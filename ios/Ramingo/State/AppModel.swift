import Foundation
import SwiftUI

@MainActor
final class AppModel: ObservableObject {
    let client: SupabaseClient
    private let repository: TripRepository

    @Published private(set) var session: AuthSession?
    @Published private(set) var trips: [TripSummary] = []
    @Published private(set) var isBootstrapping = true
    @Published var errorMessage: String?

    init() {
        let client = SupabaseClient(configuration: .load())
        self.client = client
        self.repository = TripRepository(client: client)
        self.session = client.session
    }

    var isConfigured: Bool { client.configuration.isConfigured }

    var currentUser: AuthUser? { session?.user ?? client.currentUser }

    func bootstrap() async {
        defer { isBootstrapping = false }
        guard isConfigured else { return }
        do {
            session = try await client.restoreSession()
            if session != nil { try await reloadTrips() }
        } catch {
            session = nil
            errorMessage = error.localizedDescription
        }
    }

    func signIn(email: String, password: String) async {
        await self.runAuth {
            self.session = try await self.client.signIn(email: email, password: password)
            try await self.reloadTrips()
        }
    }

    func signUp(email: String, password: String) async {
        await self.runAuth {
            let result = try await self.client.signUp(email: email, password: password)
            self.session = result ?? self.client.session
            if self.session != nil { try await self.reloadTrips() }
        }
    }

    func signInWithGoogle() async {
        await self.runAuth {
            self.session = try await self.client.signInWithGoogle()
            try await self.reloadTrips()
        }
    }

    func sendPasswordReset(email: String) async {
        do {
            try await client.sendPasswordReset(email: email)
            errorMessage = nil
        } catch {
            errorMessage = error.localizedDescription
        }
    }

    func signOut() async {
        await client.signOut()
        session = nil
        trips = []
    }

    func reloadTrips() async throws {
        try await ensureSession()
        trips = try await repository.loadTrips()
    }

    func overview(for tripID: String) async throws -> TripOverview? {
        try await ensureSession()
        return try await repository.loadOverview(id: tripID)
    }

    func createTrip(title: String, startDate: String, endDate: String, cities: String) async throws {
        try await ensureSession()
        _ = try await repository.createTrip(title: title, startDate: startDate, endDate: endDate, cities: cities)
        try await reloadTrips()
    }

    func resolvePhoto(_ reference: String) async -> URL? {
        try? await client.resolvePhoto(reference)
    }

    private func ensureSession() async throws {
        if client.session?.isExpired == true {
            session = try await client.refreshSession()
        }
        guard session != nil else { throw SupabaseClientError.cancelled }
    }

    private func runAuth(_ operation: @escaping () async throws -> Void) async {
        errorMessage = nil
        do {
            try await operation()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}
