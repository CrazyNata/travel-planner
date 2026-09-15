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
                .frame(height: 205)
                .overlay(alignment: .topLeading) {
                    StatusPill(text: trip.status)
                        .padding(12)
                }
                .overlay(alignment: .topTrailing) {
                    Image(systemName: "ellipsis")
                        .font(.system(size: 20, weight: .bold))
                        .foregroundStyle(Color(hex: 0x46464D))
                        .rotationEffect(.degrees(90))
                        .frame(width: 36, height: 36)
                        .background(Color.white.opacity(0.97), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .padding(12)
                }

            VStack(alignment: .leading, spacing: 0) {
                Text(trip.title)
                    .font(AppTheme.font(21, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)

                Text(trip.dates.isEmpty ? "Даты не указаны" : trip.dates)
                .font(AppTheme.font(13, .semibold))
                .foregroundStyle(AppTheme.muted)
                .padding(.top, 7)

                GeometryReader { proxy in
                    ZStack(alignment: .leading) {
                        Capsule().fill(AppTheme.track)
                        LinearGradient(
                            colors: [AppTheme.purple, Color(hex: 0x8069EE)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                        .frame(width: proxy.size.width * CGFloat(max(trip.progress, 3)) / 100)
                        .clipShape(Capsule())
                    }
                }
                .frame(height: 6)
                .padding(.top, 13)

                routeSummary
                    .padding(.top, 9)
            }
            .padding(.horizontal, 16)
            .padding(.top, 15)
            .padding(.bottom, 17)
        }
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color.black.opacity(0.08), radius: 10, x: 0, y: 5)
    }

    private var routeSummary: some View {
        Group {
            if trip.cities.isEmpty {
                Text("Маршрут заполнен на \(trip.progress)%")
                    .font(AppTheme.font(11.5, .extrabold))
            } else {
                Text("Маршрут заполнен на \(trip.progress)%")
                    .font(AppTheme.font(11.5, .extrabold)) +
                Text(" · \(trip.cities)")
                    .font(AppTheme.font(11.5, .semibold))
            }
        }
        .foregroundStyle(AppTheme.muted)
        .fixedSize(horizontal: false, vertical: true)
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
            VStack(spacing: 0) {
                Text("+")
                    .font(AppTheme.font(28, .semibold))
                    .foregroundStyle(AppTheme.purple)
                    .padding(.horizontal, 15)
                    .padding(.vertical, 6)
                    .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                Text("Новое путешествие")
                    .font(AppTheme.font(16, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .padding(.top, 10)
                Text("С нуля или из шаблона")
                    .font(AppTheme.font(13, .medium))
                    .foregroundStyle(AppTheme.muted)
                    .padding(.top, 2)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 34)
            .padding(.horizontal, 20)
            .background(AppTheme.surface.opacity(0.4), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(AppTheme.border, style: StrokeStyle(lineWidth: 2, dash: [7, 7]))
            }
        }
        .buttonStyle(.plain)
    }
}
