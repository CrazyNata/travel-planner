import Foundation
import OSLog
import SwiftUI
import UserNotifications

@MainActor
final class AppModel: NSObject, ObservableObject, UNUserNotificationCenterDelegate {
    let client: SupabaseClient

    private let repository: TripRepository
    private let accountRepository: AccountRepository
    private let catalogRepository: CatalogRepository
    private let cityCatalogRepository = IOSCityCatalogRepository()
    private let weatherRepository: WeatherRepository
    private let exchangeRateRepository: ExchangeRateRepository
    private let logger = Logger(subsystem: "com.odyssey.ramingo.ios", category: "bootstrap")

    @Published private(set) var session: AuthSession?
    @Published private(set) var trips: [TripSummary] = []
    @Published private(set) var profile = AccountProfile.defaults
    @Published private(set) var isBootstrapping = true
    @Published private(set) var isReadyForSession = false
    @Published private(set) var tripsLoadErrorMessage: String?
    @Published var errorMessage: String?
    @Published var authNotice: String?
    @Published var pendingTripID: String?
    @Published var isShowingPasswordRecovery = false
    @Published var shouldPresentCreateTrip = false

    override init() {
        let client = SupabaseClient(configuration: .load())
        self.client = client
        self.repository = TripRepository(client: client)
        self.accountRepository = AccountRepository(client: client)
        self.catalogRepository = CatalogRepository(client: client)
        self.weatherRepository = WeatherRepository()
        self.exchangeRateRepository = ExchangeRateRepository()
        self.session = client.session
        super.init()
        UNUserNotificationCenter.current().delegate = self
    }

    var isConfigured: Bool { client.configuration.isConfigured }

    var currentUser: AuthUser? { session?.user ?? client.currentUser }

    func bootstrap() async {
        defer { isBootstrapping = false }
        guard isConfigured else {
            isReadyForSession = session == nil
            return
        }
        do {
#if DEBUG
            if Self.hasLaunchArgument("-ramingo-qa-skip-session") {
                session = nil
                isReadyForSession = false
                return
            }
            if let tripID = Self.launchArgument("-ramingo-qa-trip-id"), !tripID.isEmpty {
                pendingTripID = tripID
            }
            if let accessToken = Self.launchArgument("-ramingo-qa-access-token"),
               let refreshToken = Self.launchArgument("-ramingo-qa-refresh-token"),
               !accessToken.isEmpty,
               !refreshToken.isEmpty {
                session = try await client.establishSession(
                    accessToken: accessToken,
                    refreshToken: refreshToken,
                    expiresIn: 3_600,
                )
            } else {
                session = try await client.restoreSession()
            }
#else
            session = try await client.restoreSession()
#endif
        } catch {
            session = nil
            isReadyForSession = false
            profile = .defaults
            errorMessage = error.localizedDescription
            logger.error("Session bootstrap failed: \(error.localizedDescription, privacy: .public)")
            return
        }

        guard session != nil else {
            isReadyForSession = false
            return
        }

        isReadyForSession = false
        await finishAuthenticatedBootstrap()
    }

    func signIn(email: String, password: String, remember: Bool = true) async {
        await runAuth {
            self.client.setSessionPersistence(remember)
            self.session = try await self.client.signIn(email: email, password: password)
            self.isReadyForSession = false
            await self.finishAuthenticatedBootstrap()
        }
    }

    func signUp(email: String, password: String, displayName: String = "", remember: Bool = true) async {
        await runAuth {
            self.client.setSessionPersistence(remember)
            let result = try await self.client.signUp(email: email, password: password, displayName: displayName)
            self.session = result ?? self.client.session
            if self.session != nil {
                self.isReadyForSession = false
                await self.finishAuthenticatedBootstrap()
            } else {
                let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
                self.authNotice = "Мы отправили письмо для подтверждения на \(normalizedEmail). Подтвердите e-mail, затем войдите в приложение."
            }
        }
    }

    func signInWithGoogle(remember: Bool = true) async {
        await runAuth {
            self.client.setSessionPersistence(remember)
            self.session = try await self.client.signInWithGoogle()
            self.isReadyForSession = false
            await self.finishAuthenticatedBootstrap()
        }
    }

    func signInWithApple(identityToken: String, nonce: String) async {
        await runAuth {
            self.session = try await self.client.signInWithApple(identityToken: identityToken, nonce: nonce)
            self.isReadyForSession = false
            await self.finishAuthenticatedBootstrap()
        }
    }

    func updateDisplayName(_ name: String) async {
        guard let user = try? await client.updateUserDisplayName(name) else { return }
        if let current = session {
            session = AuthSession(
                accessToken: current.accessToken,
                refreshToken: current.refreshToken,
                expiresAt: current.expiresAt,
                user: user,
            )
        }
    }

    @discardableResult
    func sendPasswordReset(email: String) async -> Bool {
        do {
            try await client.sendPasswordReset(email: email)
            errorMessage = nil
            authNotice = "Ссылка для сброса пароля отправлена на указанный e-mail. Откройте её на этом iPhone."
            return true
        } catch {
            errorMessage = error.localizedDescription
            return false
        }
    }

    func changePassword(_ password: String) async throws {
        try await accountRepository.changePassword(password)
    }

    func signOut() async {
        await ReminderScheduler.cancelAll()
        await client.signOut()
        session = nil
        profile = .defaults
        trips = []
        tripsLoadErrorMessage = nil
        isReadyForSession = false
        pendingTripID = nil
        isShowingPasswordRecovery = false
        shouldPresentCreateTrip = false
    }

    func deleteAccount() async throws {
        try await accountRepository.deleteAccount()
        await signOut()
    }

    func loadProfile() async {
        guard session != nil else {
            profile = .defaults
            return
        }
        profile = (try? await accountRepository.loadProfile()) ?? .defaults
    }

    func completeOnboarding(openCreateTrip: Bool = false) async {
        let next = profileCopy(
            onboardingCompleted: true,
            createTripHintSeen: profile.createTripHintSeen,
            addPlaceHintSeen: profile.addPlaceHintSeen,
        )
        // Onboarding is a navigation gate, not a reason to block the app when
        // the optional profile sync is temporarily unavailable. Update the
        // in-memory state first so both Skip and Create can continue normally.
        profile = next
        shouldPresentCreateTrip = openCreateTrip

        // Persist the flag in the background. Supabase remains the source of
        // truth, but a transient transport error must not strand the user on
        // the tutorial or show a blocking global alert.
        Task { [weak self] in
            guard let self else { return }
            do {
                try await self.ensureSession()
                try await self.accountRepository.updateProfile(
                    avatarReference: next.avatarReference,
                    notificationsEnabled: next.notificationsEnabled,
                    language: next.language,
                    themePreference: next.themePreference,
                    tripRemindersEnabled: next.tripRemindersEnabled,
                    cancellationRemindersEnabled: next.cancellationRemindersEnabled,
                    paymentRemindersEnabled: next.paymentRemindersEnabled,
                    emailNotificationsEnabled: next.emailNotificationsEnabled,
                    emailPaymentRemindersEnabled: next.emailPaymentRemindersEnabled,
                    emailRecipient: next.emailRecipient,
                    reminderHour: next.reminderHour,
                    onboardingCompleted: true,
                    createTripHintSeen: next.createTripHintSeen,
                    addPlaceHintSeen: next.addPlaceHintSeen,
                )
                try? await self.accountRepository.updateWebOnboardingState(completed: true)
            } catch is CancellationError {
                return
            } catch {
                self.logger.error("Onboarding sync deferred: \(error.localizedDescription, privacy: .public)")
            }
        }
    }

    func markCreateTripHintSeen() async {
        guard !profile.createTripHintSeen else { return }
        let next = profileCopy(
            onboardingCompleted: profile.onboardingCompleted,
            createTripHintSeen: true,
            addPlaceHintSeen: profile.addPlaceHintSeen,
        )
        do {
            try await updateProfile(next)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func markAddPlaceHintSeen() async {
        guard !profile.addPlaceHintSeen else { return }
        let next = profileCopy(
            onboardingCompleted: profile.onboardingCompleted,
            createTripHintSeen: profile.createTripHintSeen,
            addPlaceHintSeen: true,
        )
        do {
            try await updateProfile(next)
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    private func finishAuthenticatedBootstrap() async {
        await loadProfile()
        do {
            try await reloadTrips()
            tripsLoadErrorMessage = nil
        } catch {
            tripsLoadErrorMessage = error.localizedDescription
            logger.error("Authenticated data bootstrap failed: \(error.localizedDescription, privacy: .public)")
        }
        await reconcileOnboardingState()
        isReadyForSession = true
    }

    private func reconcileOnboardingState() async {
        guard session != nil, !profile.onboardingCompleted else { return }
        if let webCompleted = try? await accountRepository.loadWebOnboardingCompleted(), webCompleted == true {
            await completeOnboarding()
            return
        }
        // Older accounts may have trips but no account_profile onboarding flag.
        // Do not show a first-run tutorial to those users.
        guard !profile.hasStoredProfile, !trips.isEmpty else { return }
        await completeOnboarding()
    }

    private func profileCopy(
        onboardingCompleted: Bool,
        createTripHintSeen: Bool,
        addPlaceHintSeen: Bool,
    ) -> AccountProfile {
        AccountProfile(
            avatarReference: profile.avatarReference,
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
            onboardingCompleted: onboardingCompleted,
            createTripHintSeen: createTripHintSeen,
            addPlaceHintSeen: addPlaceHintSeen,
            hasStoredProfile: true,
        )
    }

    func updateProfile(_ next: AccountProfile) async throws {
        try await accountRepository.updateProfile(
            avatarReference: next.avatarReference,
            notificationsEnabled: next.notificationsEnabled,
            language: next.language,
            themePreference: next.themePreference,
            tripRemindersEnabled: next.tripRemindersEnabled,
            cancellationRemindersEnabled: next.cancellationRemindersEnabled,
            paymentRemindersEnabled: next.paymentRemindersEnabled,
            emailNotificationsEnabled: next.emailNotificationsEnabled,
            emailPaymentRemindersEnabled: next.emailPaymentRemindersEnabled,
            emailRecipient: next.emailRecipient,
            reminderHour: next.reminderHour,
            onboardingCompleted: next.onboardingCompleted,
            createTripHintSeen: next.createTripHintSeen,
            addPlaceHintSeen: next.addPlaceHintSeen,
        )
        profile = next
        await syncReminders()
    }

    func setNotificationsEnabled(_ enabled: Bool) async throws {
        try await updateNotificationSettings(
            enabled: enabled,
            tripRemindersEnabled: profile.tripRemindersEnabled,
            cancellationRemindersEnabled: profile.cancellationRemindersEnabled,
            paymentRemindersEnabled: profile.paymentRemindersEnabled,
            emailNotificationsEnabled: profile.emailNotificationsEnabled,
            emailPaymentRemindersEnabled: profile.emailPaymentRemindersEnabled,
            emailRecipient: profile.emailRecipient,
            reminderHour: profile.reminderHour,
        )
    }

    func updateNotificationSettings(
        enabled: Bool,
        tripRemindersEnabled: Bool,
        cancellationRemindersEnabled: Bool,
        paymentRemindersEnabled: Bool,
        emailNotificationsEnabled: Bool,
        emailPaymentRemindersEnabled: Bool,
        emailRecipient: String?,
        reminderHour: Int,
    ) async throws {
        if enabled {
            let settings = await ReminderScheduler.notificationSettings()
            let granted: Bool
            switch settings.authorizationStatus {
            case .authorized, .provisional:
                granted = true
            case .notDetermined:
                granted = try await ReminderScheduler.requestAuthorization()
            default:
                granted = false
            }
            guard granted else { throw AppModelError.notificationsDenied }
        }
        let next = AccountProfile(
            avatarReference: profile.avatarReference,
            notificationsEnabled: enabled,
            language: profile.language,
            themePreference: profile.themePreference,
            tripRemindersEnabled: tripRemindersEnabled,
            cancellationRemindersEnabled: cancellationRemindersEnabled,
            paymentRemindersEnabled: paymentRemindersEnabled,
            emailNotificationsEnabled: emailNotificationsEnabled,
            emailPaymentRemindersEnabled: emailPaymentRemindersEnabled,
            emailRecipient: emailRecipient,
            reminderHour: min(max(reminderHour, 0), 23),
            onboardingCompleted: profile.onboardingCompleted,
            createTripHintSeen: profile.createTripHintSeen,
            addPlaceHintSeen: profile.addPlaceHintSeen,
            hasStoredProfile: profile.hasStoredProfile,
        )
        try await updateProfile(next)
        if !enabled { await ReminderScheduler.cancelAll() }
    }

    func uploadProfilePhoto(data: Data) async throws {
        let reference = try await accountRepository.uploadProfilePhoto(data: data)
        profile = AccountProfile(
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
            hasStoredProfile: profile.hasStoredProfile,
        )
    }

    func reloadTrips() async throws {
        do {
            try await ensureSession()
            trips = try await repository.loadTrips()
            tripsLoadErrorMessage = nil
            await syncReminders()
        } catch {
            tripsLoadErrorMessage = error.localizedDescription
            throw error
        }
    }

    func overview(for tripID: String) async throws -> TripOverview? {
        try await ensureSession()
        return try await repository.loadOverview(id: tripID)
    }

    func weather(for overview: TripOverview) async -> [String: WeatherSnapshot] {
        let fallbackCities = overview.overviewMapPoints.isEmpty
            ? overview.routeLegs.flatMap { [$0.from, $0.to] }
            : overview.overviewMapPoints
        let candidates = overview.overviewWeatherCities.isEmpty
            ? (overview.cities.isEmpty ? fallbackCities : overview.cities)
            : overview.overviewWeatherCities
        var seen = Set<String>()
        let cities = candidates.filter { city in
            let value = city.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !value.isEmpty else { return false }
            return seen.insert(value.lowercased()).inserted
        }
        let catalogCoordinates = resolveCityCoordinates(for: cities)
        let coordinates = overview.cityCoordinates.merging(catalogCoordinates) { existing, _ in existing }
        return await weatherRepository.loadCurrent(
            cities: cities,
            tripDates: overview.dates,
            coordinates: coordinates,
        )
    }

    func exchangeRates(for overview: TripOverview) async throws -> ExchangeRateSnapshot {
        let currencies = Set(
            [overview.budgetCurrency] + overview.budgetExpenses.map(\.inputCurrency),
        )
        return try await exchangeRateRepository.loadRubRates(quotes: currencies)
    }

    func createTrip(
        title: String,
        startDate: String,
        endDate: String,
        cities: String,
        cityCoordinates: [String: Coordinate] = [:],
    ) async throws {
        try await ensureSession()
        let cityList = cities
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        let resolvedCoordinates = await weatherRepository.resolveCoordinates(
            cities: cityList,
            known: cityCoordinates,
        )
        _ = try await repository.createTrip(
            title: title,
            startDate: startDate,
            endDate: endDate,
            cities: cities,
            cityCoordinates: resolvedCoordinates,
        )
        try await reloadTrips()
    }

    func searchCities(query: String) -> [IOSCityCatalogEntry] {
        cityCatalogRepository.search(query: query, language: profile.language)
    }

    func resolveCityCoordinates(for cities: [String]) -> [String: Coordinate] {
        cities.reduce(into: [String: Coordinate]()) { result, city in
            if let entry = cityCatalogRepository.resolve(city) {
                result[city] = entry.coordinate
            }
        }
    }

    func updateTripDetails(id: String, title: String, dates: String, cities: String) async throws {
        try await ensureSession()
        try await repository.updateTripDetails(id: id, title: title, dates: dates, cities: cities)
    }

    func deleteTrip(id: String) async throws {
        try await ensureSession()
        try await repository.deleteTrip(id: id)
        try await reloadTrips()
    }

    func restoreTrip(id: String) async throws {
        try await ensureSession()
        try await repository.restoreTrip(id: id)
        try await reloadTrips()
    }

    func updateTripField(id: String, key: String, value: JSONValue) async throws {
        try await ensureSession()
        try await repository.updateTripField(id: id, key: key, value: value)
    }

    func addTripArrayItem(id: String, section: String, item: [String: JSONValue]) async throws {
        try await ensureSession()
        try await repository.addTripArrayItem(id: id, section: section, item: item)
    }

    func updateTripArrayItem(id: String, section: String, itemID: String, fields: [String: JSONValue]) async throws {
        try await ensureSession()
        try await repository.updateTripArrayItem(id: id, section: section, itemID: itemID, fields: fields)
    }

    func deleteTripItem(id: String, section: String, itemID: String) async throws {
        try await ensureSession()
        try await repository.deleteTripItem(id: id, section: section, itemID: itemID)
    }

    func addRouteLeg(
        id: String,
        from: String,
        to: String,
        checkIn: String,
        checkOut: String,
        notes: String,
        mapsURL: String,
        date: String,
        dateDay: String,
        dateMonth: String,
        weekday: String,
        distance: String,
        travelTime: String,
    ) async throws {
        try await ensureSession()
        try await repository.addRouteLeg(
            id: id,
            from: from,
            to: to,
            checkIn: checkIn,
            checkOut: checkOut,
            notes: notes,
            mapsURL: mapsURL,
            date: date,
            dateDay: dateDay,
            dateMonth: dateMonth,
            weekday: weekday,
            distance: distance,
            travelTime: travelTime,
        )
    }

    func updateRouteLegDetails(
        id: String,
        dayID: String,
        from: String,
        to: String,
        checkIn: String,
        checkOut: String,
        notes: String,
        mapsURL: String,
        date: String,
        dateDay: String,
        dateMonth: String,
        weekday: String,
        distance: String,
        travelTime: String,
    ) async throws {
        try await ensureSession()
        try await repository.updateRouteLegDetails(
            id: id,
            dayID: dayID,
            from: from,
            to: to,
            checkIn: checkIn,
            checkOut: checkOut,
            notes: notes,
            mapsURL: mapsURL,
            date: date,
            dateDay: dateDay,
            dateMonth: dateMonth,
            weekday: weekday,
            distance: distance,
            travelTime: travelTime,
        )
    }

    func reorderRouteLegs(id: String, orderedDayIDs: [String]) async throws {
        try await ensureSession()
        try await repository.reorderRouteLegs(id: id, orderedDayIDs: orderedDayIDs)
    }

    func reorderAccommodations(id: String, orderedAccommodationIDs: [String]) async throws {
        try await ensureSession()
        try await repository.reorderAccommodations(id: id, orderedAccommodationIDs: orderedAccommodationIDs)
    }

    func reorderSights(id: String, orderedSightIDs: [String]) async throws {
        try await ensureSession()
        try await repository.reorderSights(id: id, orderedSightIDs: orderedSightIDs)
    }

    func reorderSightDays(id: String, currentDayIDs: [String], orderedDayIDs: [String]) async throws {
        try await ensureSession()
        try await repository.reorderSightDays(id: id, currentDayIDs: currentDayIDs, orderedDayIDs: orderedDayIDs)
    }

    func updateSightNotes(id: String, dayID: String, notes: String) async throws {
        try await ensureSession()
        try await repository.updateSightNotes(id: id, dayID: dayID, notes: notes)
    }

    func deleteSightDay(id: String, walkDay: Int) async throws {
        try await ensureSession()
        try await repository.deleteSightDay(id: id, walkDay: walkDay)
    }

    func addCatalogItem(id: String, entry: CatalogEntry, walkDay: Int) async throws {
        try await ensureSession()
        try await repository.addCatalogItem(id: id, entry: entry, walkDay: walkDay)
    }

    func searchCatalog(
        kind: CatalogCategory,
        city: String,
        query: String,
        language: String,
        petType: String,
    ) async throws -> [CatalogEntry] {
        try await ensureSession()
        return try await catalogRepository.search(kind: kind, city: city, query: query, language: language, petType: petType)
    }

    func addBudgetGroup(id: String, name: String, people: Int) async throws {
        try await ensureSession()
        try await repository.addBudgetGroup(id: id, name: name, people: people)
    }

    func updateBudgetGroup(id: String, groupID: String, name: String, people: Int) async throws {
        try await ensureSession()
        try await repository.updateBudgetGroup(id: id, groupID: groupID, name: name, people: people)
    }

    func deleteBudgetGroup(id: String, groupID: String) async throws {
        try await ensureSession()
        try await repository.deleteBudgetGroup(id: id, groupID: groupID)
    }

    func updateMemberRole(id: String, memberID: String, role: String) async throws {
        try await ensureSession()
        try await repository.updateMemberRole(id: id, memberID: memberID, role: role)
    }

    func removeMember(id: String, memberID: String) async throws {
        try await ensureSession()
        try await repository.removeMember(id: id, memberID: memberID)
    }

    func leaveTrip(id: String) async throws {
        try await ensureSession()
        try await repository.leaveTrip(id: id)
        trips.removeAll { $0.id == id }
    }

    func inviteMember(id: String, name: String, email: String, role: String) async throws {
        try await ensureSession()
        try await repository.inviteMember(id: id, name: name, email: email, role: role)
    }

    func addCoverPhoto(id: String, data: Data, city: String) async throws {
        try await ensureSession()
        try await repository.addCoverPhoto(id: id, data: data, city: city)
    }

    func deleteCoverPhoto(id: String, photoID: String) async throws {
        try await ensureSession()
        try await repository.deleteCoverPhoto(id: id, photoID: photoID)
    }

    func addItemPhoto(id: String, section: String, itemID: String, data: Data) async throws {
        try await ensureSession()
        try await repository.addItemPhoto(id: id, section: section, itemID: itemID, data: data)
    }

    func replaceItemCoverPhoto(id: String, section: String, itemID: String, data: Data) async throws {
        try await ensureSession()
        try await repository.replaceItemCoverPhoto(id: id, section: section, itemID: itemID, data: data)
    }

    func moveItemPhoto(id: String, section: String, itemID: String, photoIndex: Int, direction: Int) async throws {
        try await ensureSession()
        try await repository.moveItemPhoto(id: id, section: section, itemID: itemID, photoIndex: photoIndex, direction: direction)
    }

    func deleteItemPhoto(id: String, section: String, itemID: String, photoIndex: Int) async throws {
        try await ensureSession()
        try await repository.deleteItemPhoto(id: id, section: section, itemID: itemID, photoIndex: photoIndex)
    }

    func resolvePhoto(_ reference: String) async -> URL? {
        try? await client.resolvePhoto(reference)
    }

    func consumePendingTripID() -> String? {
        defer { pendingTripID = nil }
        return pendingTripID
    }

    func handleDeepLink(_ url: URL) {
        let absolute = url.absoluteString.lowercased()
        let containsSession = absolute.contains("access_token=") && absolute.contains("refresh_token=")
        if !containsSession, let tripID = Self.tripID(from: url), !tripID.isEmpty {
            pendingTripID = tripID
        }
        guard containsSession else { return }
        Task { [weak self] in
            guard let self else { return }
            do {
                guard let result = try await self.client.handleAuthCallback(url) else { return }
                self.session = result.session
                self.isReadyForSession = false
                await self.finishAuthenticatedBootstrap()
                if let tripID = result.tripID, !tripID.isEmpty { self.pendingTripID = tripID }
                if result.isRecovery { self.isShowingPasswordRecovery = true }
            } catch {
                if self.session != nil { self.isReadyForSession = true }
                self.errorMessage = error.localizedDescription
            }
        }
    }

    private func ensureSession() async throws {
        if client.session?.isExpired == true {
            session = try await client.refreshSession()
        }
        guard session != nil else { throw SupabaseClientError.cancelled }
    }

    private func syncReminders() async {
        guard session != nil else { return }
        guard profile.notificationsEnabled else {
            await ReminderScheduler.cancelAll()
            return
        }
        let activeTrips = trips.filter { $0.deletedAt == nil }
        var overviews = [TripOverview]()
        for trip in activeTrips {
            if let overview = try? await repository.loadOverview(id: trip.id) {
                overviews.append(overview)
            }
        }
        await ReminderScheduler.sync(trips: overviews, profile: profile)
    }

    private func runAuth(_ operation: @escaping () async throws -> Void) async {
        errorMessage = nil
        authNotice = nil
        do {
            try await operation()
        } catch {
            if session != nil { isReadyForSession = true }
            errorMessage = error.localizedDescription
        }
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void,
    ) {
        completionHandler([.banner, .list, .sound])
    }

    nonisolated func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void,
    ) {
        let tripID = response.notification.request.content.userInfo["tripID"] as? String
        Task { @MainActor [weak self] in
            if let tripID, !tripID.isEmpty { self?.pendingTripID = tripID }
        }
        completionHandler()
    }

    private static func tripID(from url: URL) -> String? {
        let components = URLComponents(url: url, resolvingAgainstBaseURL: false)
        let queryValue = components?.queryItems?.first(where: { ["tripID", "tripId", "trip_id"].contains($0.name) })?.value
        if let queryValue { return queryValue }
        guard let fragment = components?.fragment,
              let fragmentComponents = URLComponents(string: "?\(fragment)")
        else { return nil }
        return fragmentComponents.queryItems?.first(where: { ["tripID", "tripId", "trip_id"].contains($0.name) })?.value
    }

#if DEBUG
    private static func hasLaunchArgument(_ name: String) -> Bool {
        ProcessInfo.processInfo.arguments.contains(name)
    }

    private static func launchArgument(_ name: String) -> String? {
        let arguments = ProcessInfo.processInfo.arguments
        guard let index = arguments.firstIndex(of: name), arguments.index(after: index) < arguments.endIndex else {
            return nil
        }
        return arguments[arguments.index(after: index)]
    }
#endif
}

enum AppModelError: LocalizedError {
    case notificationsDenied

    var errorDescription: String? {
        switch self {
        case .notificationsDenied:
            return "Уведомления запрещены в настройках iPhone. Разрешите их для Ramingo и повторите попытку."
        }
    }
}
