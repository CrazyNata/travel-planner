import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    @State private var filter: TripFilter = .active
    @State private var showCreateTrip = false
    @State private var showSettings = false

    private var visibleTrips: [TripSummary] {
        model.trips.filter { filter == .active ? $0.deletedAt == nil : $0.deletedAt != nil }
    }

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.background.ignoresSafeArea()
                ScrollView {
                    VStack(alignment: .leading, spacing: 20) {
                        HStack(alignment: .top) {
                            VStack(alignment: .leading, spacing: 6) {
                                Text("Твои путешествия")
                                    .font(.system(size: 30, weight: .heavy, design: .rounded))
                                    .foregroundStyle(AppTheme.ink)
                                Text(greeting)
                                    .font(.subheadline)
                                    .foregroundStyle(AppTheme.muted)
                            }
                            Spacer()
                            Button { showSettings = true } label: {
                                Image(systemName: "person.crop.circle")
                                    .font(.system(size: 27))
                                    .foregroundStyle(AppTheme.purpleDeep)
                            }
                            .accessibilityLabel("Настройки аккаунта")
                        }

                        Picker("Фильтр поездок", selection: $filter) {
                            ForEach(TripFilter.allCases) { value in
                                Text(value.title).tag(value)
                            }
                        }
                        .pickerStyle(.segmented)

                        if visibleTrips.isEmpty {
                            EmptyTripsView(filter: filter) { showCreateTrip = true }
                        } else {
                            LazyVStack(spacing: 14) {
                                ForEach(visibleTrips) { trip in
                                    NavigationLink(value: trip.id) {
                                        TripCardView(trip: trip, client: model.client)
                                    }
                                    .buttonStyle(.plain)
                                }
                            }
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.top, 18)
                    .padding(.bottom, 30)
                }
            }
            .navigationDestination(for: String.self) { tripID in
                TripDetailView(tripID: tripID)
            }
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { showCreateTrip = true } label: {
                        Image(systemName: "plus")
                    }
                    .accessibilityLabel("Новая поездка")
                }
            }
            .refreshable { try? await model.reloadTrips() }
            .task { try? await model.reloadTrips() }
            .sheet(isPresented: $showCreateTrip) { CreateTripView() }
            .sheet(isPresented: $showSettings) { SettingsView() }
        }
    }

    private var greeting: String {
        let name = model.currentUser?.displayName ?? ""
        return name.isEmpty ? "Планируй легко — путешествуй с удовольствием." : "Привет, \(name)!"
    }
}

private enum TripFilter: String, CaseIterable, Identifiable {
    case active
    case deleted

    var id: String { rawValue }
    var title: String { self == .active ? "Активные" : "Архив" }
}

private struct TripCardView: View {
    let trip: TripSummary
    let client: SupabaseClient

    var body: some View {
        RamingoCard {
            VStack(alignment: .leading, spacing: 14) {
                RemotePhotoView(reference: trip.coverReference, client: client, contentMode: .fill, cornerRadius: 16)
                    .frame(height: 150)
                    .overlay(alignment: .bottomLeading) {
                        LinearGradient(colors: [.clear, .black.opacity(0.55)], startPoint: .top, endPoint: .bottom)
                            .clipShape(RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay(alignment: .bottomLeading) {
                                Text(trip.cities.isEmpty ? "Новая поездка" : trip.cities)
                                    .font(.caption.weight(.semibold))
                                    .foregroundStyle(.white)
                                    .padding(12)
                            }
                    }

                HStack(alignment: .top) {
                    VStack(alignment: .leading, spacing: 5) {
                        Text(trip.title)
                            .font(.title3.weight(.bold))
                            .foregroundStyle(AppTheme.ink)
                            .lineLimit(2)
                        Text(trip.dates.isEmpty ? "Даты не указаны" : trip.dates)
                            .font(.subheadline)
                            .foregroundStyle(AppTheme.muted)
                    }
                    Spacer()
                    StatusPill(text: trip.status)
                }

                HStack(spacing: 10) {
                    ProgressBar(value: trip.progress)
                    Text("\(trip.progress)%")
                        .font(.caption.weight(.bold))
                        .foregroundStyle(AppTheme.purpleDeep)
                        .frame(width: 38, alignment: .trailing)
                }
            }
        }
    }
}

private struct EmptyTripsView: View {
    let filter: TripFilter
    let create: () -> Void

    var body: some View {
        RamingoCard {
            VStack(spacing: 14) {
                Image(systemName: filter == .active ? "airplane" : "archivebox")
                    .font(.system(size: 32, weight: .medium))
                    .foregroundStyle(AppTheme.purple)
                Text(filter == .active ? "Пока нет поездок" : "Архив пуст")
                    .font(.headline.weight(.bold))
                Text(filter == .active ? "Создай первую поездку и собери всё важное в одном месте." : "Удалённые поездки появятся здесь.")
                    .multilineTextAlignment(.center)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.muted)
                if filter == .active {
                    Button("Создать поездку", action: create)
                        .buttonStyle(.borderedProminent)
                }
            }
            .frame(maxWidth: .infinity)
        }
    }
}
