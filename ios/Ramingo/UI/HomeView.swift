import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    @State private var filter: TripFilter = .all
    @State private var showCreateTrip = false
    @State private var showSettings = false
    @State private var navigationPath = NavigationPath()

    private var activeTrips: [TripSummary] { model.trips.filter { $0.deletedAt == nil } }
    private var deletedTrips: [TripSummary] { model.trips.filter { $0.deletedAt != nil } }

    private var visibleTrips: [TripSummary] {
        switch filter {
        case .all:
            return activeTrips
        case .upcoming:
            return activeTrips.filter { !$0.isDraft && !$0.isCompleted }
        case .drafts:
            return activeTrips.filter(\.isDraft)
        case .completed:
            return activeTrips.filter(\.isCompleted)
        case .deleted:
            return deletedTrips
        }
    }

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ZStack {
                AppTheme.background.ignoresSafeArea()
                ScrollView(showsIndicators: false) {
                    LazyVStack(alignment: .leading, spacing: 16) {
                        RamingoTopBar {
                            Button { showSettings = true } label: {
                                Image(systemName: "gearshape")
                                    .font(.system(size: 22, weight: .semibold))
                                    .foregroundStyle(AppTheme.ink)
                                    .frame(width: 48, height: 48)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Открыть настройки")
                        }

                        Text("Мои путешествия")
                            .font(AppTheme.font(32, .extrabold))
                            .foregroundStyle(AppTheme.ink)
                            .lineSpacing(1)
                            .padding(.top, 6)

                        filterBar

                        if visibleTrips.isEmpty {
                            EmptyTripsView(filter: filter) { showCreateTrip = true }
                        } else {
                            ForEach(visibleTrips) { trip in
                                NavigationLink(value: trip.id) {
                                    TripCardView(trip: trip, client: model.client)
                                }
                                .buttonStyle(.plain)
                            }
                        }

                        NewTripCard { showCreateTrip = true }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 96)
                }
                .refreshable { try? await model.reloadTrips() }
            }
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: String.self) { tripID in
                TripDetailView(tripID: tripID)
            }
            .task {
                try? await model.reloadTrips()
                openPendingTrip()
            }
            .onChange(of: model.pendingTripID) { _, _ in openPendingTrip() }
            .sheet(isPresented: $showCreateTrip) { CreateTripView() }
            .sheet(isPresented: $showSettings) { SettingsView() }
        }
    }

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 9) {
                filterButton(.all, count: activeTrips.count)
                filterButton(.upcoming, count: activeTrips.filter { !$0.isDraft && !$0.isCompleted }.count)
                filterButton(.drafts, count: activeTrips.filter(\.isDraft).count)
                filterButton(.completed, count: activeTrips.filter(\.isCompleted).count)
                if !deletedTrips.isEmpty {
                    filterButton(.deleted, count: deletedTrips.count)
                }
            }
            .padding(.vertical, 2)
        }
        .contentMargins(.horizontal, 0, for: .scrollContent)
    }

    private func filterButton(_ value: TripFilter, count: Int) -> some View {
        let selected = filter == value
        return Button {
            withAnimation(.easeInOut(duration: 0.18)) { filter = value }
        } label: {
            Text("\(value.title) · \(count)")
                .font(AppTheme.font(13, .bold))
                .foregroundStyle(selected ? Color.white : AppTheme.ink)
                .padding(.horizontal, 15)
                .frame(height: 38)
                .background(selected ? AppTheme.purple : AppTheme.surface, in: Capsule())
                .overlay {
                    if !selected { Capsule().stroke(AppTheme.border, lineWidth: 1) }
                }
                .shadow(color: selected ? AppTheme.purple.opacity(0.22) : .clear, radius: 5, y: 3)
        }
        .buttonStyle(.plain)
    }

    private func openPendingTrip() {
        guard let tripID = model.consumePendingTripID(), !tripID.isEmpty else { return }
        navigationPath.append(tripID)
    }
}

private enum TripFilter: String, CaseIterable, Identifiable {
    case all, upcoming, drafts, completed, deleted
    var id: String { rawValue }
    var title: String {
        switch self {
        case .all: return "Все"
        case .upcoming: return "Предстоящие"
        case .drafts: return "Черновики"
        case .completed: return "Завершённые"
        case .deleted: return "Удалённые"
        }
    }
}

private extension TripSummary {
    var isDraft: Bool { status.localizedCaseInsensitiveContains("чернов") }
    var isCompleted: Bool {
        status.localizedCaseInsensitiveContains("заверш") ||
            status.localizedCaseInsensitiveContains("прошед")
    }
}

private struct TripCardView: View {
    let trip: TripSummary
    let client: SupabaseClient

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            RemotePhotoView(reference: trip.coverReference, client: client, contentMode: .fill, cornerRadius: 0)
                .frame(height: 178)
                .overlay {
                    LinearGradient(colors: [.clear, .black.opacity(0.2)], startPoint: .center, endPoint: .bottom)
                }
                .overlay(alignment: .topLeading) {
                    StatusPill(text: trip.status)
                        .padding(12)
                }
                .overlay(alignment: .topTrailing) {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 16, weight: .bold))
                        .foregroundStyle(Color(hex: 0x32323A))
                        .frame(width: 32, height: 32)
                        .background(Color.white.opacity(0.94), in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                        .padding(12)
                }

            VStack(alignment: .leading, spacing: 10) {
                Text(trip.title)
                    .font(AppTheme.font(20, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)

                HStack(spacing: 7) {
                    Image(systemName: "calendar")
                    Text(trip.dates.isEmpty ? "Даты не указаны" : trip.dates)
                }
                .font(AppTheme.font(13, .semibold))
                .foregroundStyle(AppTheme.muted)

                if !trip.cities.isEmpty {
                    HStack(spacing: 7) {
                        Image(systemName: "mappin.and.ellipse")
                        Text(trip.cities).lineLimit(1)
                    }
                    .font(AppTheme.font(13, .semibold))
                    .foregroundStyle(AppTheme.muted)
                }

                HStack(spacing: 10) {
                    ProgressBar(value: trip.progress)
                    Text("\(trip.progress)%")
                        .font(AppTheme.font(12, .extrabold))
                        .foregroundStyle(AppTheme.purple)
                        .frame(width: 38, alignment: .trailing)
                }
                .padding(.top, 2)
            }
            .padding(16)
        }
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 22, style: .continuous)
                .stroke(AppTheme.border.opacity(0.65), lineWidth: 1)
        }
        .shadow(color: Color.black.opacity(0.07), radius: 13, x: 0, y: 8)
    }
}

private struct EmptyTripsView: View {
    let filter: TripFilter
    let create: () -> Void

    var body: some View {
        RamingoCard {
            VStack(spacing: 12) {
                Image(systemName: filter == .deleted ? "archivebox" : "safari")
                    .font(.system(size: 31, weight: .medium))
                    .foregroundStyle(AppTheme.purple)
                Text(filter == .deleted ? "Архив пуст" : "Здесь появятся ваши путешествия")
                    .font(AppTheme.font(18, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .multilineTextAlignment(.center)
                Text(filter == .deleted ? "Удалённые поездки появятся здесь." : "Создайте первую поездку с нуля или выберите готовый маршрут")
                    .font(AppTheme.font(13, .semibold))
                    .foregroundStyle(AppTheme.muted)
                    .multilineTextAlignment(.center)
                if filter != .deleted {
                    PrimaryActionButton(title: "Создать путешествие", action: create)
                        .padding(.top, 4)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 9)
        }
    }
}

private struct NewTripCard: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 13) {
                Image(systemName: "plus")
                    .font(.system(size: 19, weight: .bold))
                    .foregroundStyle(.white)
                    .frame(width: 42, height: 42)
                    .background(AppTheme.primaryGradient, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                VStack(alignment: .leading, spacing: 3) {
                    Text("Новое путешествие")
                        .font(AppTheme.font(16, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                    Text("Добавить маршрут, места и жильё")
                        .font(AppTheme.font(12, .semibold))
                        .foregroundStyle(AppTheme.muted)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .font(.system(size: 13, weight: .bold))
                    .foregroundStyle(AppTheme.purple)
            }
            .padding(15)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 20, style: .continuous)
                    .stroke(AppTheme.purple.opacity(0.35), style: StrokeStyle(lineWidth: 1, dash: [6, 5]))
            }
        }
        .buttonStyle(.plain)
    }
}
