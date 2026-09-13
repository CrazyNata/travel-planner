import Foundation

struct AccountProfile: Hashable, Sendable {
    let avatarReference: String?
    let notificationsEnabled: Bool
    let language: String
    let darkTheme: Bool
    let tripRemindersEnabled: Bool
    let cancellationRemindersEnabled: Bool
    let reminderHour: Int

    static let defaults = AccountProfile(
        avatarReference: nil,
        notificationsEnabled: false,
        language: "RU",
        darkTheme: false,
        tripRemindersEnabled: true,
        cancellationRemindersEnabled: true,
        reminderHour: 9,
    )
}

private struct UserDataRow: Decodable {
    let userID: String
    let key: String
    let value: JSONValue

    enum CodingKeys: String, CodingKey {
        case userID = "user_id"
        case key
        case value
    }
}

private struct UserDataUpsert: Encodable {
    let userID: String
    let key: String
    let value: JSONValue

    enum CodingKeys: String, CodingKey {
        case userID = "user_id"
        case key
        case value
    }
}

final class AccountRepository {
    private let client: SupabaseClient

    init(client: SupabaseClient) {
        self.client = client
    }

    func loadProfile() async throws -> AccountProfile {
        guard let userID = client.currentUser?.id else { return .defaults }
        let safeUserID = userID.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? userID
        let path = "rest/v1/user_data?select=user_id,key,value&user_id=eq.\(safeUserID)&key=eq.account_profile"
        let rows: [UserDataRow] = try await client.request(
            path,
            method: "GET",
            body: nil as EmptyAccountBody?,
            authenticated: true,
        )
        guard let value = rows.first?.value.objectValue else { return .defaults }
        return AccountProfile(
            avatarReference: value.text("avatar_url").nonEmpty,
            notificationsEnabled: value.flag("notifications_enabled") ?? false,
            language: value.text("language", fallback: "RU").uppercased(),
            darkTheme: value.flag("dark_theme") ?? false,
            tripRemindersEnabled: value.flag("trip_reminders_enabled") ?? true,
            cancellationRemindersEnabled: value.flag("cancellation_reminders_enabled") ?? true,
            reminderHour: min(max(value.integer("reminder_hour") ?? 9, 0), 23),
        )
    }

    func updateProfile(
        avatarReference: String?,
        notificationsEnabled: Bool,
        language: String? = nil,
        darkTheme: Bool? = nil,
        tripRemindersEnabled: Bool? = nil,
        cancellationRemindersEnabled: Bool? = nil,
        reminderHour: Int? = nil,
    ) async throws {
        guard let userID = client.currentUser?.id else { throw SupabaseClientError.cancelled }
        let existing = try await loadProfileValue(userID: userID)
        var next = existing
        if let avatarReference { next["avatar_url"] = .string(avatarReference) }
        next["notifications_enabled"] = .boolean(notificationsEnabled)
        language.map { $0.uppercased() }.map { next["language"] = .string($0) }
        darkTheme.map { next["dark_theme"] = .boolean($0) }
        tripRemindersEnabled.map { next["trip_reminders_enabled"] = .boolean($0) }
        cancellationRemindersEnabled.map { next["cancellation_reminders_enabled"] = .boolean($0) }
        reminderHour.map { next["reminder_hour"] = .number(Double(min(max($0, 0), 23))) }
        let _: [UserDataRow] = try await client.request(
            "rest/v1/user_data?on_conflict=user_id,key",
            method: "POST",
            body: [UserDataUpsert(userID: userID, key: "account_profile", value: .object(next))],
            authenticated: true,
            extraHeaders: ["Prefer": "resolution=merge-duplicates,return=representation"],
        )
    }

    func uploadProfilePhoto(data: Data) async throws -> String {
        guard !data.isEmpty else { throw SupabaseClientError.server(status: 422, message: "Не удалось прочитать изображение.") }
        guard let userID = client.currentUser?.id else { throw SupabaseClientError.cancelled }
        let path = "\(userID)/profile/\(UUID().uuidString.lowercased()).jpg"
        try await client.uploadStorageObject(path: path, data: data)
        let reference = "storage://trip-photos/\(path)"
        do {
            let profile = try await loadProfile()
            try await updateProfile(
                avatarReference: reference,
                notificationsEnabled: profile.notificationsEnabled,
                language: profile.language,
                darkTheme: profile.darkTheme,
                tripRemindersEnabled: profile.tripRemindersEnabled,
                cancellationRemindersEnabled: profile.cancellationRemindersEnabled,
                reminderHour: profile.reminderHour,
            )
        } catch {
            try? await client.deleteStorageObject(path: path)
            throw error
        }
        return reference
    }

    func changePassword(_ password: String) async throws {
        try await client.updatePassword(password)
    }

    func deleteAccount() async throws {
        guard client.currentUser != nil else { throw SupabaseClientError.cancelled }
        try await client.invokeFunction("delete-account", body: EmptyAccountBody())
    }

    private func loadProfileValue(userID: String) async throws -> [String: JSONValue] {
        let safeUserID = userID.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? userID
        let path = "rest/v1/user_data?select=user_id,key,value&user_id=eq.\(safeUserID)&key=eq.account_profile"
        let rows: [UserDataRow] = try await client.request(
            path,
            method: "GET",
            body: nil as EmptyAccountBody?,
            authenticated: true,
        )
        return rows.first?.value.objectValue ?? [:]
    }
}

private struct EmptyAccountBody: Encodable {}
