import Foundation

enum ThemePreference: String, Hashable, Sendable {
    case system
    case light
    case dark

    init(storageValue: String?) {
        switch storageValue?.trimmingCharacters(in: .whitespacesAndNewlines).lowercased() {
        case "light": self = .light
        case "dark": self = .dark
        default: self = .system
        }
    }

    var storageValue: String { rawValue }
}

struct AccountProfile: Hashable, Sendable {
    let avatarReference: String?
    let notificationsEnabled: Bool
    let language: String
    let themePreference: ThemePreference
    let tripRemindersEnabled: Bool
    let cancellationRemindersEnabled: Bool
    let paymentRemindersEnabled: Bool
    let emailNotificationsEnabled: Bool
    let emailPaymentRemindersEnabled: Bool
    let emailRecipient: String?
    let reminderHour: Int
    let onboardingCompleted: Bool = false
    let createTripHintSeen: Bool = false
    let addPlaceHintSeen: Bool = false
    let hasStoredProfile: Bool = false

    init(
        avatarReference: String?,
        notificationsEnabled: Bool,
        language: String,
        themePreference: ThemePreference,
        tripRemindersEnabled: Bool,
        cancellationRemindersEnabled: Bool,
        paymentRemindersEnabled: Bool,
        emailNotificationsEnabled: Bool,
        emailPaymentRemindersEnabled: Bool,
        emailRecipient: String?,
        reminderHour: Int,
        onboardingCompleted: Bool = false,
        createTripHintSeen: Bool = false,
        addPlaceHintSeen: Bool = false,
        hasStoredProfile: Bool = false,
    ) {
        self.avatarReference = avatarReference
        self.notificationsEnabled = notificationsEnabled
        self.language = language
        self.themePreference = themePreference
        self.tripRemindersEnabled = tripRemindersEnabled
        self.cancellationRemindersEnabled = cancellationRemindersEnabled
        self.paymentRemindersEnabled = paymentRemindersEnabled
        self.emailNotificationsEnabled = emailNotificationsEnabled
        self.emailPaymentRemindersEnabled = emailPaymentRemindersEnabled
        self.emailRecipient = emailRecipient
        self.reminderHour = reminderHour
        self.onboardingCompleted = onboardingCompleted
        self.createTripHintSeen = createTripHintSeen
        self.addPlaceHintSeen = addPlaceHintSeen
        self.hasStoredProfile = hasStoredProfile
    }

    var darkTheme: Bool { themePreference == .dark }

    static let defaults = AccountProfile(
        avatarReference: nil,
        notificationsEnabled: false,
        language: "RU",
        themePreference: .system,
        tripRemindersEnabled: true,
        cancellationRemindersEnabled: true,
        paymentRemindersEnabled: true,
        emailNotificationsEnabled: true,
        emailPaymentRemindersEnabled: true,
        emailRecipient: nil,
        reminderHour: 9,
        onboardingCompleted: false,
        createTripHintSeen: false,
        addPlaceHintSeen: false,
        hasStoredProfile: false,
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
        let themePreference: ThemePreference
        if let storedTheme = value.text("theme").nonEmpty {
            themePreference = ThemePreference(storageValue: storedTheme)
        } else if value.flag("dark_theme") == true {
            themePreference = .dark
        } else {
            // Android treats an existing legacy profile without a theme as light.
            themePreference = .light
        }
        return AccountProfile(
            avatarReference: value.text("avatar_url").nonEmpty,
            notificationsEnabled: value.flag("notifications_enabled") ?? false,
            language: value.text("language", fallback: "RU").uppercased(),
            themePreference: themePreference,
            tripRemindersEnabled: value.flag("trip_reminders_enabled") ?? true,
            cancellationRemindersEnabled: value.flag("cancellation_reminders_enabled") ?? true,
            paymentRemindersEnabled: value.flag("payment_reminders_enabled") ?? true,
            emailNotificationsEnabled: value.flag("email_notifications_enabled") ?? true,
            emailPaymentRemindersEnabled: value.flag("email_payment_reminders_enabled") ?? true,
            emailRecipient: value.text("email_recipient").nonEmpty.flatMap(normalizeNotificationEmail),
            reminderHour: min(max(value.integer("reminder_hour") ?? 9, 0), 23),
            onboardingCompleted: value.flag("onboarding_completed") ?? false,
            createTripHintSeen: value.flag("create_trip_hint_seen") ?? false,
            addPlaceHintSeen: value.flag("add_place_hint_seen") ?? false,
            hasStoredProfile: true,
        )
    }

    func updateProfile(
        avatarReference: String?,
        notificationsEnabled: Bool,
        language: String? = nil,
        darkTheme: Bool? = nil,
        themePreference: ThemePreference? = nil,
        tripRemindersEnabled: Bool? = nil,
        cancellationRemindersEnabled: Bool? = nil,
        paymentRemindersEnabled: Bool? = nil,
        emailNotificationsEnabled: Bool? = nil,
        emailPaymentRemindersEnabled: Bool? = nil,
        emailRecipient: String? = nil,
        reminderHour: Int? = nil,
        onboardingCompleted: Bool? = nil,
        createTripHintSeen: Bool? = nil,
        addPlaceHintSeen: Bool? = nil,
    ) async throws {
        guard let userID = client.currentUser?.id else { throw SupabaseClientError.cancelled }
        let existing = try await loadProfileValue(userID: userID)
        var next = existing
        if let avatarReference { next["avatar_url"] = .string(avatarReference) }
        next["notifications_enabled"] = .boolean(notificationsEnabled)
        language.map { $0.uppercased() }.map { next["language"] = .string($0) }
        if let themePreference {
            next["theme"] = .string(themePreference.storageValue)
            next["dark_theme"] = .boolean(themePreference == .dark)
        } else if let darkTheme {
            next["dark_theme"] = .boolean(darkTheme)
            if next["theme"] == nil {
                next["theme"] = .string(darkTheme ? ThemePreference.dark.storageValue : ThemePreference.light.storageValue)
            }
        }
        tripRemindersEnabled.map { next["trip_reminders_enabled"] = .boolean($0) }
        cancellationRemindersEnabled.map { next["cancellation_reminders_enabled"] = .boolean($0) }
        paymentRemindersEnabled.map { next["payment_reminders_enabled"] = .boolean($0) }
        emailNotificationsEnabled.map { next["email_notifications_enabled"] = .boolean($0) }
        emailPaymentRemindersEnabled.map { next["email_payment_reminders_enabled"] = .boolean($0) }
        if let emailRecipient {
            guard let normalized = normalizeNotificationEmail(emailRecipient) else {
                throw SupabaseClientError.invalidInput("Укажите корректный e-mail")
            }
            next["email_recipient"] = .string(normalized)
        }
        reminderHour.map { next["reminder_hour"] = .number(Double(min(max($0, 0), 23))) }
        onboardingCompleted.map { next["onboarding_completed"] = .boolean($0) }
        createTripHintSeen.map { next["create_trip_hint_seen"] = .boolean($0) }
        addPlaceHintSeen.map { next["add_place_hint_seen"] = .boolean($0) }
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
                themePreference: profile.themePreference,
                tripRemindersEnabled: profile.tripRemindersEnabled,
                cancellationRemindersEnabled: profile.cancellationRemindersEnabled,
                paymentRemindersEnabled: profile.paymentRemindersEnabled,
                emailNotificationsEnabled: profile.emailNotificationsEnabled,
                emailPaymentRemindersEnabled: profile.emailPaymentRemindersEnabled,
                emailRecipient: profile.emailRecipient,
                reminderHour: profile.reminderHour,
                onboardingCompleted: profile.onboardingCompleted,
                createTripHintSeen: profile.createTripHintSeen,
                addPlaceHintSeen: profile.addPlaceHintSeen,
            )
            if let previousReference = profile.avatarReference, previousReference != reference {
                await client.deleteStorageReference(previousReference)
            }
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

    func loadWebOnboardingCompleted() async throws -> Bool? {
        guard let userID = client.currentUser?.id else { return nil }
        let safeUserID = userID.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? userID
        let path = "rest/v1/user_data?select=user_id,key,value&user_id=eq.\(safeUserID)&key=eq.web-onboarding"
        let rows: [UserDataRow] = try await client.request(
            path,
            method: "GET",
            body: nil as EmptyAccountBody?,
            authenticated: true,
        )
        guard let value = rows.first?.value.objectValue else { return nil }
        return value.flag("completed")
    }

    func updateWebOnboardingState(completed: Bool) async throws {
        guard let userID = client.currentUser?.id else { throw SupabaseClientError.cancelled }
        let value: JSONValue = .object([
            "completed": .boolean(completed),
            "completedAt": .string(ISO8601DateFormatter().string(from: Date())),
        ])
        let _: [UserDataRow] = try await client.request(
            "rest/v1/user_data?on_conflict=user_id,key",
            method: "POST",
            body: [UserDataUpsert(userID: userID, key: "web-onboarding", value: value)],
            authenticated: true,
            extraHeaders: ["Prefer": "resolution=merge-duplicates,return=representation"],
        )
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

private let notificationEmailPattern = try! Regex(#"^[^\s@]+@[^\s@]+\.[^\s@]+$"#)

func normalizeNotificationEmail(_ value: String) -> String? {
    let normalized = value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    guard normalized.count <= 254, normalized.wholeMatch(of: notificationEmailPattern) != nil else { return nil }
    return normalized
}

private struct EmptyAccountBody: Encodable {}
