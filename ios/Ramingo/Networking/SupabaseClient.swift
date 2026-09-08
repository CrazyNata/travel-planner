import AuthenticationServices
import CryptoKit
import Foundation
import Security
import UIKit

struct SupabaseConfiguration: Sendable {
    let endpoint: URL
    let publishableKey: String

    static func load(bundle: Bundle = .main) -> SupabaseConfiguration {
        let fallbackURL = URL(string: "https://hxcavgtlucyoqudbrgse.supabase.co")!
        let rawURL = bundle.object(forInfoDictionaryKey: "SUPABASE_URL") as? String
        let endpoint = URL(string: rawURL?.trimmingCharacters(in: .whitespacesAndNewlines) ?? "") ?? fallbackURL
        let key = (bundle.object(forInfoDictionaryKey: "SUPABASE_PUBLISHABLE_KEY") as? String ?? "")
            .trimmingCharacters(in: .whitespacesAndNewlines)
        return SupabaseConfiguration(endpoint: endpoint, publishableKey: key)
    }

    var isConfigured: Bool {
        !publishableKey.isEmpty && !publishableKey.contains("REPLACE_WITH") && !publishableKey.contains("CHANGE_ME")
    }
}

struct AuthUser: Codable, Hashable, Sendable {
    let id: String
    let email: String?
    let userMetadata: [String: JSONValue]?

    enum CodingKeys: String, CodingKey {
        case id
        case email
        case userMetadata = "user_metadata"
    }

    var displayName: String {
        let metadataName = userMetadata?["full_name"]?.stringValue?.trimmingCharacters(in: .whitespacesAndNewlines)
        if let metadataName, let value = metadataName.nonEmpty { return value }
        if let emailName = email?.split(separator: "@").first.map(String.init), let value = emailName.nonEmpty { return value }
        return "Ramingo"
    }
}

struct AuthSession: Codable, Hashable, Sendable {
    let accessToken: String
    let refreshToken: String
    let expiresAt: Date?
    let user: AuthUser?

    var isExpired: Bool {
        guard let expiresAt else { return false }
        return expiresAt.timeIntervalSinceNow < 60
    }
}

private struct AuthResponse: Decodable {
    let accessToken: String?
    let refreshToken: String?
    let expiresAt: TimeInterval?
    let expiresIn: TimeInterval?
    let user: AuthUser?

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case refreshToken = "refresh_token"
        case expiresAt = "expires_at"
        case expiresIn = "expires_in"
        case user
    }

    var session: AuthSession {
        let absoluteExpiry: Date?
        if let expiresAt {
            absoluteExpiry = Date(timeIntervalSince1970: expiresAt)
        } else if let expiresIn {
            absoluteExpiry = Date(timeIntervalSinceNow: expiresIn)
        } else {
            absoluteExpiry = nil
        }
        return AuthSession(
            accessToken: accessToken ?? "",
            refreshToken: refreshToken ?? "",
            expiresAt: absoluteExpiry,
            user: user,
        )
    }
}

private struct RefreshRequest: Encodable {
    let refreshToken: String
    enum CodingKeys: String, CodingKey { case refreshToken = "refresh_token" }
}

private struct PasswordRequest: Encodable {
    let email: String
    let password: String
}

private struct RecoveryRequest: Encodable {
    let email: String
    let redirectTo: String
    enum CodingKeys: String, CodingKey { case email; case redirectTo = "redirect_to" }
}

private struct PKCERequest: Encodable {
    let authCode: String
    let codeVerifier: String
    enum CodingKeys: String, CodingKey {
        case authCode = "auth_code"
        case codeVerifier = "code_verifier"
    }
}

private struct EmptyBody: Encodable {}

enum SupabaseClientError: LocalizedError {
    case notConfigured
    case malformedURL
    case server(status: Int, message: String)
    case cancelled
    case oauthCallbackMissingCode

    var errorDescription: String? {
        switch self {
        case .notConfigured:
            return "Добавьте Supabase publishable key в Config/Secrets.xcconfig."
        case .malformedURL:
            return "Не удалось сформировать адрес запроса."
        case .server(_, let message):
            return message
        case .cancelled:
            return "Авторизация отменена."
        case .oauthCallbackMissingCode:
            return "Supabase не вернул код авторизации."
        }
    }
}

final class KeychainSessionStore {
    private let service = "com.odyssey.ramingo.ios"
    private let account = "supabase.auth.session"

    func read() -> AuthSession? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        var result: CFTypeRef?
        guard SecItemCopyMatching(query as CFDictionary, &result) == errSecSuccess,
              let data = result as? Data
        else { return nil }
        return try? JSONDecoder().decode(AuthSession.self, from: data)
    }

    func write(_ session: AuthSession) {
        guard let data = try? JSONEncoder().encode(session) else { return }
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        let update: [String: Any] = [kSecValueData as String: data]
        if SecItemUpdate(query as CFDictionary, update as CFDictionary) != errSecSuccess {
            var item = query
            item[kSecValueData as String] = data
            item[kSecAttrAccessible as String] = kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
            SecItemAdd(item as CFDictionary, nil)
        }
    }

    func clear() {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
        ]
        SecItemDelete(query as CFDictionary)
    }
}

final class SupabaseClient {
    let configuration: SupabaseConfiguration
    private let sessionStore = KeychainSessionStore()
    private let urlSession: URLSession
    private(set) var session: AuthSession?

    init(configuration: SupabaseConfiguration, urlSession: URLSession = .shared) {
        self.configuration = configuration
        self.urlSession = urlSession
        self.session = sessionStore.read()
    }

    var currentUser: AuthUser? { session?.user }

    func restoreSession() async throws -> AuthSession? {
        guard let current = session else { return nil }
        if !current.isExpired { return current }
        return try await refreshSession()
    }

    @discardableResult
    func signIn(email: String, password: String) async throws -> AuthSession {
        guard configuration.isConfigured else { throw SupabaseClientError.notConfigured }
        let response: AuthResponse = try await send(
            "auth/v1/token?grant_type=password",
            method: "POST",
            body: PasswordRequest(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password),
            authenticated: false,
        )
        guard !response.session.accessToken.isEmpty, !response.session.refreshToken.isEmpty else {
            throw SupabaseClientError.server(status: 401, message: "Supabase не вернул активную сессию.")
        }
        return save(response.session)
    }

    @discardableResult
    func signUp(email: String, password: String) async throws -> AuthSession? {
        guard configuration.isConfigured else { throw SupabaseClientError.notConfigured }
        let response: AuthResponse = try await send(
            "auth/v1/signup",
            method: "POST",
            body: PasswordRequest(email: email.trimmingCharacters(in: .whitespacesAndNewlines), password: password),
            authenticated: false,
        )
        guard !response.session.accessToken.isEmpty, !response.session.refreshToken.isEmpty else { return nil }
        return save(response.session)
    }

    func sendPasswordReset(email: String) async throws {
        guard configuration.isConfigured else { throw SupabaseClientError.notConfigured }
        try await sendVoid(
            "auth/v1/recover",
            method: "POST",
            body: RecoveryRequest(email: email, redirectTo: "https://ramingo.online/mobile/reset"),
            authenticated: false,
        )
    }

    @discardableResult
    func refreshSession() async throws -> AuthSession {
        guard let refreshToken = session?.refreshToken else { throw SupabaseClientError.cancelled }
        let response: AuthResponse = try await send(
            "auth/v1/token?grant_type=refresh_token",
            method: "POST",
            body: RefreshRequest(refreshToken: refreshToken),
            authenticated: false,
        )
        guard !response.session.accessToken.isEmpty, !response.session.refreshToken.isEmpty else {
            throw SupabaseClientError.server(status: 401, message: "Supabase не обновил активную сессию.")
        }
        return save(response.session)
    }

    func signOut() async {
        if session != nil {
            try? await sendVoid("auth/v1/logout", method: "POST", body: EmptyBody(), authenticated: true)
        }
        session = nil
        sessionStore.clear()
    }

    @MainActor
    @discardableResult
    func signInWithGoogle() async throws -> AuthSession {
        guard configuration.isConfigured else { throw SupabaseClientError.notConfigured }
        let verifier = Self.makeCodeVerifier()
        let challenge = Self.makeCodeChallenge(verifier)
        var components = URLComponents(url: configuration.endpoint.appendingPathComponent("auth/v1/authorize"), resolvingAgainstBaseURL: false)
        components?.queryItems = [
            URLQueryItem(name: "provider", value: "google"),
            URLQueryItem(name: "redirect_to", value: "ramingo://auth-callback"),
            URLQueryItem(name: "code_challenge", value: challenge),
            URLQueryItem(name: "code_challenge_method", value: "s256"),
        ]
        guard let authorizeURL = components?.url else { throw SupabaseClientError.malformedURL }
        let callback = try await GoogleAuthCoordinator().start(url: authorizeURL)
        guard let code = URLComponents(url: callback, resolvingAgainstBaseURL: false)?.queryItems?.first(where: { $0.name == "code" })?.value else {
            throw SupabaseClientError.oauthCallbackMissingCode
        }
        let response: AuthResponse = try await send(
            "auth/v1/token?grant_type=pkce",
            method: "POST",
            body: PKCERequest(authCode: code, codeVerifier: verifier),
            authenticated: false,
        )
        guard !response.session.accessToken.isEmpty, !response.session.refreshToken.isEmpty else {
            throw SupabaseClientError.server(status: 401, message: "Supabase не вернул активную сессию Google.")
        }
        return save(response.session)
    }

    func resolvePhoto(_ reference: String) async throws -> URL? {
        let value = reference.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !value.isEmpty else { return nil }
        if let url = URL(string: value), url.scheme == "http" || url.scheme == "https" {
            if let path = Self.storagePath(from: value) {
                return try await signStoragePath(path)
            }
            return url
        }
        guard let path = Self.storagePath(from: value) else { return nil }
        return try await signStoragePath(path)
    }

    func request<T: Decodable, Body: Encodable>(
        _ path: String,
        method: String,
        body: Body?,
        authenticated: Bool,
        extraHeaders: [String: String] = [:],
    ) async throws -> T {
        let data = try await perform(
            path,
            method: method,
            body: body,
            authenticated: authenticated,
            extraHeaders: extraHeaders,
        )
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw SupabaseClientError.server(status: 200, message: "Supabase вернул неожиданный ответ.")
        }
    }

    private func signStoragePath(_ path: String) async throws -> URL? {
        let encodedPath = path.addingPercentEncoding(withAllowedCharacters: .urlPathAllowed) ?? path
        struct SignRequest: Encodable { let expiresIn: Int; enum CodingKeys: String, CodingKey { case expiresIn = "expiresIn" } }
        struct SignResponse: Decodable { let signedURL: String; enum CodingKeys: String, CodingKey { case signedURL = "signedURL" } }
        let response: SignResponse = try await send(
            "storage/v1/object/sign/trip-photos/\(encodedPath)",
            method: "POST",
            body: SignRequest(expiresIn: 86_400),
            authenticated: true,
        )
        let signed = response.signedURL
        if let url = URL(string: signed), url.scheme != nil { return url }
        let relative = signed.hasPrefix("/") ? signed : "/\(signed)"
        let storageRelative = relative.hasPrefix("/storage/v1/") ? relative : "/storage/v1\(relative)"
        return URL(string: baseURLString + storageRelative)
    }

    private func save(_ value: AuthSession) -> AuthSession {
        session = value
        sessionStore.write(value)
        return value
    }

    private var baseURLString: String {
        configuration.endpoint.absoluteString.trimmingCharacters(in: CharacterSet(charactersIn: "/"))
    }

    private func makeURL(_ path: String) -> URL? {
        URL(string: "\(baseURLString)/\(path)")
    }

    private func send<T: Decodable, Body: Encodable>(
        _ path: String,
        method: String,
        body: Body?,
        authenticated: Bool,
    ) async throws -> T {
        let data = try await perform(path, method: method, body: body, authenticated: authenticated)
        do {
            return try JSONDecoder().decode(T.self, from: data)
        } catch {
            throw SupabaseClientError.server(status: 200, message: "Supabase вернул неожиданный ответ.")
        }
    }

    private func sendVoid<Body: Encodable>(
        _ path: String,
        method: String,
        body: Body?,
        authenticated: Bool,
    ) async throws {
        _ = try await perform(path, method: method, body: body, authenticated: authenticated)
    }

    private func perform<Body: Encodable>(
        _ path: String,
        method: String,
        body: Body?,
        authenticated: Bool,
        extraHeaders: [String: String] = [:],
    ) async throws -> Data {
        guard configuration.isConfigured else { throw SupabaseClientError.notConfigured }
        guard let url = makeURL(path) else { throw SupabaseClientError.malformedURL }
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue(configuration.publishableKey, forHTTPHeaderField: "apikey")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if body != nil { request.setValue("application/json", forHTTPHeaderField: "Content-Type") }
        if authenticated, let accessToken = session?.accessToken {
            request.setValue("Bearer \(accessToken)", forHTTPHeaderField: "Authorization")
        }
        extraHeaders.forEach { request.setValue($1, forHTTPHeaderField: $0) }
        if let body { request.httpBody = try JSONEncoder().encode(body) }

        let (data, response) = try await urlSession.data(for: request)
        guard let httpResponse = response as? HTTPURLResponse else {
            throw SupabaseClientError.server(status: 0, message: "Не удалось получить ответ Supabase.")
        }
        guard (200..<300).contains(httpResponse.statusCode) else {
            throw SupabaseClientError.server(status: httpResponse.statusCode, message: Self.errorMessage(data))
        }
        return data
    }

    private static func errorMessage(_ data: Data) -> String {
        if let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any] {
            for key in ["msg", "message", "error_description", "error"] {
                if let value = object[key] as? String, !value.isEmpty { return value }
            }
        }
        return "Запрос к Supabase завершился ошибкой."
    }

    private static func storagePath(from value: String) -> String? {
        if value.hasPrefix("trip-photos/") { return String(value.dropFirst("trip-photos/".count)) }
        if let url = URL(string: value), let range = url.path.range(of: "/trip-photos/") {
            return String(url.path[range.upperBound...])
        }
        if value.contains("/"), !value.hasPrefix("places/") { return value }
        return nil
    }

    private static func makeCodeVerifier() -> String {
        let bytes = (0..<32).map { _ in UInt8.random(in: 0...255) }
        return Data(bytes).base64URLEncodedString()
    }

    private static func makeCodeChallenge(_ verifier: String) -> String {
        Data(SHA256.hash(data: Data(verifier.utf8))).base64URLEncodedString()
    }
}

private extension Data {
    func base64URLEncodedString() -> String {
        base64EncodedString()
            .replacingOccurrences(of: "+", with: "-")
            .replacingOccurrences(of: "/", with: "_")
            .replacingOccurrences(of: "=", with: "")
    }
}

private final class GoogleAuthCoordinator: NSObject, ASWebAuthenticationPresentationContextProviding {
    private var session: ASWebAuthenticationSession?

    func start(url: URL) async throws -> URL {
        try await withCheckedThrowingContinuation { continuation in
            let webSession = ASWebAuthenticationSession(url: url, callbackURLScheme: "ramingo") { [weak self] callback, error in
                self?.session = nil
                if let callback {
                    continuation.resume(returning: callback)
                } else if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(throwing: SupabaseClientError.cancelled)
                }
            }
            session = webSession
            webSession.presentationContextProvider = self
            webSession.prefersEphemeralWebBrowserSession = false
            guard webSession.start() else {
                self.session = nil
                continuation.resume(throwing: SupabaseClientError.cancelled)
                return
            }
        }
    }

    func presentationAnchor(for session: ASWebAuthenticationSession) -> ASPresentationAnchor {
        UIApplication.shared.connectedScenes
            .compactMap { $0 as? UIWindowScene }
            .flatMap(\.windows)
            .first(where: { $0.isKeyWindow }) ?? UIWindow(frame: .zero)
    }
}
