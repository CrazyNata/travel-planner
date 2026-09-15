import CoreLocation
import Foundation
import MapKit
import SwiftUI

struct TripDetailView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let tripID: String

    @State private var overview: TripOverview?
    @State private var selectedSection: TripSection = .overview
    @State private var isLoading = true
    @State private var localError: String?
    @State private var showDrawer = false
    @State private var showEditor = false
    @State private var showSettings = false
    @State private var weatherByCity: [String: WeatherSnapshot] = [:]
    @State private var exchangeRates: ExchangeRateSnapshot?

    var body: some View {
        ZStack(alignment: .leading) {
            AppTheme.background.ignoresSafeArea()
            VStack(spacing: 0) {
                tripTopBar
                if isLoading {
                    Spacer()
                    ProgressView("Загружаем поездку…")
                        .font(AppTheme.font(13, .semibold))
                        .tint(AppTheme.purple)
                    Spacer()
                } else if let overview {
                    ScrollView(showsIndicators: false) {
                        tripSection(overview)
                            .padding(.bottom, 40)
                    }
                    .refreshable { await load() }
                } else {
                    Spacer()
                    ContentUnavailableView(
                        "Поездка не найдена",
                        systemImage: "airplane",
                        description: Text("Проверьте подключение и попробуйте ещё раз.")
                    )
                    Spacer()
                }
            }

            if showDrawer, let overview {
                Color.black.opacity(0.4)
                    .ignoresSafeArea()
                    .onTapGesture { closeDrawer() }
                    .transition(.opacity)
                TripDrawer(
                    overview: overview,
                    selection: selectedSection,
                    onSelect: { section in
                        selectedSection = section
                        closeDrawer()
                    },
                    onTrips: {
                        closeDrawer()
                        dismiss()
                    },
                    onSettings: {
                        closeDrawer()
                        showSettings = true
                    }
                )
                .transition(.move(edge: .leading))
            }
        }
        .toolbar(.hidden, for: .navigationBar)
        .task(id: tripID) { await load() }
        .sheet(isPresented: $showEditor, onDismiss: { Task { await load() } }) {
            if let overview {
                TripEditorView(tripID: tripID, initial: overview)
                    .environmentObject(model)
            }
        }
        .sheet(isPresented: $showSettings) { SettingsView() }
        .alert("Не удалось загрузить поездку", isPresented: Binding(
            get: { localError != nil },
            set: { if !$0 { localError = nil } }
        )) {
            Button("ОК", role: .cancel) { localError = nil }
        } message: {
            Text(localError ?? "")
        }
    }

    private var tripTopBar: some View {
        HStack(spacing: 0) {
            Button {
                withAnimation(.easeOut(duration: 0.2)) { showDrawer = true }
            } label: {
                Image(systemName: "line.3.horizontal")
                    .font(.system(size: 25, weight: .semibold))
                    .foregroundStyle(AppTheme.ink)
                    .frame(width: 48, height: 48)
            }
            .buttonStyle(.plain)

            Spacer(minLength: 0)
            VStack(spacing: 1) {
                Text(overview?.title ?? "Поездка")
                    .font(AppTheme.font(10, .bold))
                    .foregroundStyle(AppTheme.muted)
                    .lineLimit(1)
                Text(selectedSection.title)
                    .font(AppTheme.font(15, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(1)
            }
            .frame(maxWidth: .infinity)
            Spacer(minLength: 0)

            if selectedSection == .overview, overview?.canEdit == true {
                Button { showEditor = true } label: {
                    Image(systemName: "pencil")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(AppTheme.purpleLight)
                        .frame(width: 40, height: 40)
                        .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay { RoundedRectangle(cornerRadius: 12).stroke(AppTheme.border, lineWidth: 1) }
                }
                .frame(width: 48, height: 48)
                .buttonStyle(.plain)
            } else {
                Color.clear.frame(width: 48, height: 48)
            }
        }
        .padding(.horizontal, 16)
        .frame(height: 54)
    }

    @ViewBuilder
    private func tripSection(_ overview: TripOverview) -> some View {
        switch selectedSection {
        case .overview:
            AndroidOverviewScreen(overview: overview, weather: weatherByCity, client: model.client)
        case .route:
            AndroidRouteScreen(overview: overview, onEdit: { showEditor = true })
        case .sights:
            AndroidSightsScreen(overview: overview, client: model.client, onEdit: { showEditor = true })
        case .restaurants:
            AndroidRestaurantsScreen(overview: overview, client: model.client, onEdit: { showEditor = true })
        case .accommodation:
            AndroidAccommodationScreen(overview: overview, client: model.client, onEdit: { showEditor = true })
        case .pets:
            AndroidPetsScreen(overview: overview, client: model.client, onEdit: { showEditor = true })
        case .budget:
            AndroidBudgetScreen(overview: overview, exchangeRates: exchangeRates, onRefresh: { Task { await load() } })
        case .members:
            AndroidMembersScreen(overview: overview, onEdit: { showEditor = true })
        case .photos:
            AndroidPhotosScreen(overview: overview, client: model.client, onEdit: { showEditor = true })
        }
    }

    private func closeDrawer() {
        withAnimation(.easeIn(duration: 0.18)) { showDrawer = false }
    }

    private func load() async {
        isLoading = overview == nil
        localError = nil
        do {
            guard let fresh = try await model.overview(for: tripID) else {
                overview = nil
                weatherByCity = [:]
                exchangeRates = nil
                isLoading = false
                return
            }
            overview = fresh
            async let weather = model.weather(for: fresh)
            async let rates: ExchangeRateSnapshot? = try? await model.exchangeRates(for: fresh)
            weatherByCity = await weather
            exchangeRates = await rates
        } catch {
            localError = error.localizedDescription
        }
        isLoading = false
    }
}

private struct TripDrawer: View {
    let overview: TripOverview
    let selection: TripSection
    let onSelect: (TripSection) -> Void
    let onTrips: () -> Void
    let onSettings: () -> Void

    private let order: [TripSection] = [.overview, .route, .sights, .restaurants, .accommodation, .pets, .budget, .members, .photos]

    var body: some View {
        GeometryReader { proxy in
            VStack(spacing: 0) {
                HStack(spacing: 10) {
                    RamingoMark().frame(width: 44, height: 44)
                    Text(overview.title)
                        .font(AppTheme.font(17, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                        .lineLimit(1)
                    Spacer()
                }
                .padding(.top, proxy.safeAreaInsets.top + 22)
                .padding(.horizontal, 18)
                .padding(.bottom, 8)

                Rectangle().fill(AppTheme.border).frame(height: 1).padding(.horizontal, 18)

                VStack(spacing: 2) {
                    ForEach(order) { section in
                        DrawerRow(title: section.title, icon: section.drawerIcon, selected: section == selection) {
                            onSelect(section)
                        }
                    }
                }
                .padding(.horizontal, 18)
                .padding(.top, 12)

                Spacer(minLength: 18)
                DrawerRow(title: "Мои путешествия", icon: "arrowshape.turn.up.left", selected: false, action: onTrips)
                    .padding(.horizontal, 18)
                DrawerRow(title: "Настройки", icon: "gearshape", selected: false, action: onSettings)
                    .padding(.horizontal, 18)
                    .padding(.top, 10)
                    .padding(.bottom, proxy.safeAreaInsets.bottom + 32)
            }
            .frame(width: 310, height: proxy.size.height + proxy.safeAreaInsets.top + proxy.safeAreaInsets.bottom)
            .background(AppTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 26, style: .continuous))
            .shadow(color: .black.opacity(0.14), radius: 16, x: 6, y: 0)
            .offset(y: -proxy.safeAreaInsets.top)
        }
        .ignoresSafeArea()
    }
}

private struct DrawerRow: View {
    let title: String
    let icon: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(selected ? AppTheme.purple : Color(hex: 0x92929C))
                    .frame(width: 24)
                Text(title)
                    .font(AppTheme.font(16, selected ? .extrabold : .bold))
                    .foregroundStyle(selected ? AppTheme.purple : AppTheme.ink)
                    .lineLimit(1)
                Spacer()
            }
            .padding(.horizontal, 14)
            .frame(height: 50)
            .background(selected ? AppTheme.lavender.opacity(0.55) : .clear, in: RoundedRectangle(cornerRadius: 14))
        }
        .buttonStyle(.plain)
    }
}

private struct AndroidOverviewScreen: View {
    let overview: TripOverview
    let weather: [String: WeatherSnapshot]
    let client: SupabaseClient
    @State private var photoIndex = 0

    private var photos: [CoverPhoto] { overview.coverPhotos }
    private var activePhoto: CoverPhoto? { photos.isEmpty ? nil : photos[photoIndex % photos.count] }
    private var heroCity: String { activePhoto?.city.nonEmpty ?? overview.cities.first ?? overview.title }
    private var mapPins: [NumberedMapPin] {
        overview.cities.compactMap { city in
            overview.cityCoordinates[city].map { NumberedMapPin(title: city, coordinate: $0) }
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .bottomLeading) {
                RemotePhotoView(reference: activePhoto?.reference, client: client, contentMode: .fill, cornerRadius: 20)
                    .frame(height: 192)
                    .overlay {
                        LinearGradient(colors: [.clear, .black.opacity(0.45)], startPoint: .center, endPoint: .bottom)
                            .clipShape(RoundedRectangle(cornerRadius: 20))
                    }
                Text(heroCity)
                    .font(AppTheme.font(29, .extrabold))
                    .foregroundStyle(.white)
                    .padding(.leading, 15)
                    .padding(.bottom, 14)
                HStack {
                    carouselButton("chevron.left", delta: -1)
                    Spacer()
                    carouselButton("chevron.right", delta: 1)
                }
                .padding(.horizontal, 7)
            }
            .padding(.horizontal, 16)
            .padding(.top, 2)

            NumberedTripMap(pins: mapPins)
                .frame(height: 176)
                .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
                .padding(.horizontal, 16)
                .padding(.top, 8)
            HStack {
                Text("Общий маршрут")
                    .font(AppTheme.font(13, .semibold))
                    .foregroundStyle(AppTheme.muted)
                Spacer()
                Text("\(overview.routeLegs.count) переездов · \(overview.cities.count) городов")
                    .font(AppTheme.font(14, .extrabold))
                    .foregroundStyle(AppTheme.ink)
            }
            .padding(.horizontal, 16)
            .frame(height: 46)
            .background(AppTheme.surface)
            .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
            .padding(.horizontal, 16)

            Text("Погода по маршруту")
                .font(AppTheme.font(23, .extrabold))
                .foregroundStyle(AppTheme.ink)
                .padding(.horizontal, 16)
                .padding(.top, 13)
            Text("Текущая погода для городов маршрута")
                .font(AppTheme.font(13, .semibold))
                .foregroundStyle(AppTheme.muted)
                .padding(.horizontal, 16)
                .padding(.top, 10)
            HStack(spacing: 0) {
                Text("Сейчас")
                    .font(AppTheme.font(14, .bold))
                    .foregroundStyle(AppTheme.ink)
                    .frame(width: 72, height: 38)
                    .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 12))
                Text("На даты поездки")
                    .font(AppTheme.font(14, .bold))
                    .foregroundStyle(Color(hex: 0x9999A3))
                    .padding(.horizontal, 14)
                Spacer()
            }
            .padding(4)
            .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 13))
            .padding(.horizontal, 16)
            .padding(.top, 12)

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(overview.cities.filter { weather[$0] != nil }, id: \.self) { city in
                        if let snapshot = weather[city] {
                            AndroidWeatherCard(
                                city: city,
                                snapshot: snapshot,
                                photo: photos.first(where: { $0.city == city })?.reference ?? photos.first?.reference,
                                client: client
                            )
                        }
                    }
                }
                .padding(.horizontal, 16)
            }
            .padding(.top, 9)
        }
    }

    private func carouselButton(_ icon: String, delta: Int) -> some View {
        Button {
            guard !photos.isEmpty else { return }
            photoIndex = (photoIndex + delta + photos.count) % photos.count
        } label: {
            Image(systemName: icon)
                .font(.system(size: 21, weight: .semibold))
                .foregroundStyle(.white)
                .frame(width: 42, height: 42)
        }
        .buttonStyle(.plain)
    }
}

private struct AndroidWeatherCard: View {
    let city: String
    let snapshot: WeatherSnapshot
    let photo: String?
    let client: SupabaseClient

    var body: some View {
        ZStack(alignment: .leading) {
            RemotePhotoView(reference: photo, client: client, contentMode: .fill, cornerRadius: 17)
            LinearGradient(colors: [.black.opacity(0.08), .black.opacity(0.62)], startPoint: .top, endPoint: .bottom)
                .clipShape(RoundedRectangle(cornerRadius: 17))
            VStack(alignment: .leading) {
                Text(city).font(AppTheme.font(14, .bold))
                Spacer()
                Text(snapshot.tripTemperature ?? snapshot.temperature).font(AppTheme.font(28, .extrabold))
                Text(snapshot.tripCondition ?? snapshot.condition).font(AppTheme.font(12, .semibold))
            }
            .foregroundStyle(.white)
            .padding(11)
        }
        .frame(width: 105, height: 132)
    }
}

private struct AndroidRouteScreen: View {
    let overview: TripOverview
    let onEdit: () -> Void

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 14) {
            Text("\(tripDayCount(overview.dates)) ДНЕЙ · \(overview.cities.count) ГОРОДОВ")
                .font(AppTheme.font(12, .extrabold))
                .foregroundStyle(AppTheme.purple)
                .padding(.bottom, 4)
            if overview.routeLegs.isEmpty {
                AndroidEmptyCard(icon: "point.topleft.down.to.point.bottomright.curvepath", text: "Добавьте города и переезды")
            } else {
                ForEach(overview.routeLegs) { leg in AndroidRouteLegCard(leg: leg, onEdit: onEdit) }
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct AndroidRouteLegCard: View {
    let leg: RouteLeg
    let onEdit: () -> Void

    var body: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 12) {
                VStack(spacing: 1) {
                    Text(leg.dateDay.isEmpty ? dayFromDate(leg.date) : leg.dateDay)
                        .font(AppTheme.font(24, .extrabold)).foregroundStyle(AppTheme.purple)
                    Text((leg.dateMonth.isEmpty ? monthFromDate(leg.date) : leg.dateMonth).uppercased())
                        .font(AppTheme.font(10, .bold)).foregroundStyle(AppTheme.purpleDeep)
                }
                .frame(width: 39)
                VStack(alignment: .leading, spacing: 4) {
                    RouteStopLine(city: leg.from, muted: true, last: false)
                    RouteStopLine(city: leg.to, muted: false, last: true)
                }
                Spacer(minLength: 4)
                AndroidIconButton(icon: "doc.on.doc", action: {})
                AndroidIconButton(icon: "pencil", action: onEdit)
            }
            HStack(spacing: 12) {
                Image(systemName: "key.fill").font(.system(size: 13)).foregroundStyle(AppTheme.purple)
                Text("Заселение").font(AppTheme.font(14, .bold))
                Spacer()
                Text(routeTime(leg)).font(AppTheme.font(14, .bold))
            }
            .padding(.horizontal, 13)
            .frame(height: 40)
            .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 13))
            .padding(.top, 16)
        }
        .padding(14)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
    }
}

private struct RouteStopLine: View {
    let city: String
    let muted: Bool
    let last: Bool

    var body: some View {
        HStack(spacing: 7) {
            VStack(spacing: 0) {
                Circle().fill(last ? AppTheme.purple : AppTheme.surface)
                    .overlay(Circle().stroke(last ? AppTheme.purple : Color(hex: 0xC6BFF4), lineWidth: 1.5))
                    .frame(width: 8, height: 8)
                if !last { Rectangle().fill(Color(hex: 0xD9D4F9)).frame(width: 1.5, height: 13) }
            }
            Text(cityFlag(city)).font(.system(size: 15))
            Text(city)
                .font(AppTheme.font(last ? 16 : 13, last ? .extrabold : .semibold))
                .foregroundStyle(muted ? Color(hex: 0x9999A3) : AppTheme.ink)
                .lineLimit(1)
        }
    }
}

private struct AndroidSightsScreen: View {
    let overview: TripOverview
    let client: SupabaseClient
    let onEdit: () -> Void

    private var city: String { overview.sights.first?.city.nonEmpty ?? overview.cities.first ?? "Город" }
    private var pins: [NumberedMapPin] {
        overview.sights.compactMap { sight in
            guard let lat = sight.latitude, let lon = sight.longitude else { return nil }
            return NumberedMapPin(title: sight.name, coordinate: Coordinate(latitude: lat, longitude: lon))
        }
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 14) {
            Text("\(city.uppercased()) · ДЕНЬ 1")
                .font(AppTheme.font(12, .extrabold)).foregroundStyle(AppTheme.purple)
            HStack(spacing: 12) {
                VStack(spacing: 0) {
                    Text("1").font(AppTheme.font(20, .extrabold))
                    Text("ДЕНЬ").font(AppTheme.font(8, .bold))
                }
                .foregroundStyle(.white).frame(width: 42, height: 50)
                .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 12))
                VStack(alignment: .leading, spacing: 3) {
                    Text("ВЫБЕРИТЕ ДЕНЬ").font(AppTheme.font(10, .bold)).foregroundStyle(AppTheme.muted)
                    HStack(spacing: 8) {
                        Text(city).font(AppTheme.font(17, .extrabold))
                        Image(systemName: "pencil").foregroundStyle(AppTheme.purple)
                    }
                }
                Spacer()
                AndroidIconButton(icon: "chevron.down", action: {})
            }
            .padding(10)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
            .overlay { RoundedRectangle(cornerRadius: 20).stroke(AppTheme.border, lineWidth: 1) }

            VStack(spacing: 0) {
                NumberedTripMap(pins: pins).frame(height: 192)
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(city.uppercased()) · ДЕНЬ 1").font(AppTheme.font(10, .extrabold)).foregroundStyle(AppTheme.purple)
                        Text("\(city) · \(overview.sights.count) мест").font(AppTheme.font(13, .semibold)).foregroundStyle(AppTheme.muted)
                    }
                    Spacer()
                    Button(action: {}) {
                        Label("Копировать", systemImage: "doc.on.doc")
                            .font(AppTheme.font(13, .bold)).foregroundStyle(.white)
                            .padding(.horizontal, 13).frame(height: 40)
                            .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 11))
                    }.buttonStyle(.plain)
                }
                .padding(.horizontal, 13).frame(height: 54).background(AppTheme.surface)
            }
            .clipShape(RoundedRectangle(cornerRadius: 20))

            ForEach(overview.sights) { sight in AndroidSightCard(sight: sight, client: client, onEdit: onEdit) }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct AndroidSightCard: View {
    let sight: Sight
    let client: SupabaseClient
    let onEdit: () -> Void

    var body: some View {
        HStack(alignment: .top, spacing: 11) {
            RemotePhotoView(reference: sight.photo.nonEmpty, client: client, contentMode: .fill, cornerRadius: 12)
                .frame(width: 84, height: 105)
            VStack(alignment: .leading, spacing: 5) {
                Text((sight.category.isEmpty ? "ДОСТОПРИМЕЧАТЕЛЬНОСТИ" : sight.category).uppercased())
                    .font(AppTheme.font(9, .extrabold)).foregroundStyle(AppTheme.purple)
                Text(sight.name).font(AppTheme.font(16, .extrabold)).foregroundStyle(AppTheme.ink).lineLimit(2)
                if let rating = sight.rating {
                    Text("★ \(rating, specifier: "%.1f") · \(formatReviews(sight.reviews)) отзывов")
                        .font(AppTheme.font(11, .semibold)).foregroundStyle(AppTheme.muted)
                }
                if !sight.description.isEmpty {
                    Text(sight.description).font(AppTheme.font(11, .semibold)).foregroundStyle(AppTheme.muted).lineLimit(2)
                }
            }
            Spacer(minLength: 0)
            AndroidIconButton(icon: "pencil", action: onEdit)
        }
        .padding(8)
        .background(AppTheme.surface2.opacity(0.45), in: RoundedRectangle(cornerRadius: 19))
        .overlay { RoundedRectangle(cornerRadius: 19).stroke(AppTheme.border.opacity(0.8), lineWidth: 1) }
    }
}

private struct AndroidRestaurantsScreen: View {
    let overview: TripOverview
    let client: SupabaseClient
    let onEdit: () -> Void

    private var pins: [NumberedMapPin] {
        overview.cities.compactMap { city in overview.cityCoordinates[city].map { NumberedMapPin(title: city, coordinate: $0) } }
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            FilterHeader()
            HStack(spacing: 8) {
                CountChip("Все", count: overview.restaurants.count, selected: true)
                CountChip("Бронь", count: overview.restaurants.filter { !$0.status.isEmpty }.count)
                CountChip("Хочу", count: overview.restaurants.filter(\.priority).count)
                CountChip("Были", count: 0)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            DashedAddButton(title: "Добавить ресторан", action: onEdit)
            NumberedTripMap(pins: pins).frame(height: 146).clipShape(RoundedRectangle(cornerRadius: 20))
            ForEach(overview.restaurants) { restaurant in AndroidRestaurantCard(restaurant: restaurant, client: client) }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct FilterHeader: View {
    var body: some View {
        HStack(spacing: 10) {
            Button(action: {}) {
                HStack {
                    Image(systemName: "mappin.and.ellipse")
                    Text("Все города")
                    Spacer()
                    Image(systemName: "chevron.down")
                }
                .font(AppTheme.font(15, .extrabold)).foregroundStyle(.white)
                .padding(.horizontal, 14).frame(height: 43)
                .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 14))
            }.buttonStyle(.plain)
            Button(action: {}) {
                HStack(spacing: 9) {
                    Image(systemName: "line.3.horizontal.decrease")
                    Text("Фильтры")
                    Text("0").foregroundStyle(.white).frame(width: 27, height: 27).background(AppTheme.purple, in: Circle())
                }
                .font(AppTheme.font(15, .extrabold)).foregroundStyle(AppTheme.ink)
                .padding(.horizontal, 12).frame(height: 43)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }
            }.buttonStyle(.plain)
        }
    }
}

private struct CountChip: View {
    let title: String
    let count: Int
    var selected = false

    init(_ title: String, count: Int, selected: Bool = false) {
        self.title = title
        self.count = count
        self.selected = selected
    }

    var body: some View {
        Text("\(title) · \(count)")
            .font(AppTheme.font(12, .bold)).foregroundStyle(selected ? .white : AppTheme.ink)
            .padding(.horizontal, 13).frame(height: 31)
            .background(selected ? AppTheme.purple : AppTheme.surface2, in: Capsule())
            .overlay { if !selected { Capsule().stroke(AppTheme.border, lineWidth: 1) } }
    }
}

private struct AndroidRestaurantCard: View {
    let restaurant: Restaurant
    let client: SupabaseClient
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(spacing: 11) {
            HStack(alignment: .top, spacing: 12) {
                RemotePhotoView(reference: restaurant.photos.first, client: client, contentMode: .fill, cornerRadius: 12)
                    .frame(width: 78, height: 78)
                VStack(alignment: .leading, spacing: 4) {
                    Text(restaurant.name).font(AppTheme.font(16, .extrabold)).lineLimit(1)
                    Text(restaurant.city).font(AppTheme.font(13, .semibold)).foregroundStyle(AppTheme.muted)
                    HStack(spacing: 7) {
                        if let rating = restaurant.rating {
                            Text("★ \(rating, specifier: "%.1f")")
                                .foregroundStyle(AppTheme.ink).padding(.horizontal, 8).frame(height: 28)
                                .background(Color(hex: 0xFFF9E9), in: RoundedRectangle(cornerRadius: 8))
                        }
                        if !restaurant.price.isEmpty { Text(restaurant.price).foregroundStyle(AppTheme.purple) }
                        if !restaurant.note.isEmpty { Text(restaurant.note).lineLimit(1) }
                    }
                    .font(AppTheme.font(12, .bold))
                }
                Spacer()
                if !restaurant.link.isEmpty {
                    Button { if let url = URL(string: restaurant.link) { openURL(url) } } label: {
                        Image(systemName: "arrow.up.right.square").font(.system(size: 18, weight: .semibold)).foregroundStyle(AppTheme.purple)
                    }.buttonStyle(.plain)
                }
            }
            Divider().overlay(AppTheme.border)
            HStack {
                Text(restaurant.reviews.isEmpty ? "Нет отзывов" : "\(restaurant.reviews) отзывов")
                    .font(AppTheme.font(12, .bold)).foregroundStyle(AppTheme.muted)
                Spacer()
                Text("Забронировать").font(AppTheme.font(13, .extrabold)).foregroundStyle(AppTheme.purple)
            }
            HStack(spacing: 9) {
                AndroidOutlineAction(title: "Маршрут", action: openRestaurantLink)
                AndroidFilledAction(title: "Подробнее", action: openRestaurantLink)
            }
        }
        .padding(12)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
    }

    private func openRestaurantLink() {
        guard let url = URL(string: restaurant.link), !restaurant.link.isEmpty else { return }
        openURL(url)
    }
}

private struct AndroidAccommodationScreen: View {
    let overview: TripOverview
    let client: SupabaseClient
    let onEdit: () -> Void

    var body: some View {
        LazyVStack(spacing: 14) {
            if overview.accommodations.isEmpty {
                AndroidEmptyCard(icon: "bed.double", text: "Добавьте жильё для города поездки")
            } else {
                ForEach(overview.accommodations) { accommodation in
                    AndroidAccommodationCard(accommodation: accommodation, client: client, onEdit: onEdit)
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct AndroidAccommodationCard: View {
    let accommodation: Accommodation
    let client: SupabaseClient
    let onEdit: () -> Void
    @Environment(\.openURL) private var openURL

    private var link: String { accommodation.bookingURL.nonEmpty ?? accommodation.externalURL.nonEmpty ?? accommodation.website }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .bottom) {
                RemotePhotoView(reference: accommodation.photos.first ?? accommodation.photoReference.nonEmpty, client: client, contentMode: .fill, cornerRadius: 0)
                    .frame(height: 205)
                HStack {
                    carouselCircle("arrow.left")
                    Spacer()
                    carouselCircle("arrow.right")
                }
                .padding(.horizontal, 10).padding(.bottom, 85)
                Text("1/\(max(accommodation.photos.count, 1))")
                    .font(AppTheme.font(11, .extrabold)).foregroundStyle(.white)
                    .padding(.horizontal, 10).frame(height: 27).background(.black.opacity(0.62), in: Capsule())
                    .padding(.bottom, 10)
            }
            VStack(alignment: .leading, spacing: 10) {
                HStack(alignment: .firstTextBaseline) {
                    Text(accommodation.name).font(AppTheme.font(18, .extrabold)).lineLimit(2)
                    Spacer()
                    Text(accommodation.price).font(AppTheme.font(18, .extrabold)).foregroundStyle(AppTheme.purple)
                }
                Text("\(cityFlag(accommodation.city)) \(accommodation.city)")
                    .font(AppTheme.font(13, .semibold)).foregroundStyle(AppTheme.muted)
                if !accommodation.dates.isEmpty {
                    Label(accommodation.dates, systemImage: "calendar")
                        .font(AppTheme.font(14, .bold)).foregroundStyle(AppTheme.ink)
                }
                if let rating = accommodation.rating {
                    Text("★  \(rating, specifier: "%.1f") · \(formatReviews(accommodation.reviewCount)) отзывов")
                        .font(AppTheme.font(12, .semibold)).foregroundStyle(AppTheme.muted)
                }
                if !accommodation.deadline.isEmpty {
                    VStack(spacing: 9) {
                        HStack(spacing: 10) {
                            Image(systemName: "checkmark").font(.system(size: 23, weight: .bold)).foregroundStyle(AppTheme.success)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Бесплатная отмена").font(AppTheme.font(13, .extrabold)).foregroundStyle(AppTheme.success)
                                Text("до \(accommodation.deadline)").font(AppTheme.font(11, .semibold)).foregroundStyle(AppTheme.muted)
                            }
                            Spacer()
                        }
                        Divider().overlay(AppTheme.success.opacity(0.25))
                        HStack(spacing: 10) {
                            Image(systemName: "calendar").font(.system(size: 19, weight: .semibold)).foregroundStyle(AppTheme.purple)
                            VStack(alignment: .leading, spacing: 2) {
                                Text("Оплатить до").font(AppTheme.font(13, .extrabold)).foregroundStyle(AppTheme.purple)
                                Text(accommodation.deadline).font(AppTheme.font(11, .semibold)).foregroundStyle(AppTheme.muted)
                            }
                            Spacer()
                        }
                    }
                    .padding(11)
                    .background(AppTheme.success.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
                    .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.success.opacity(0.35), lineWidth: 1) }
                }
                HStack(spacing: 9) {
                    AndroidOutlineAction(title: "Редактировать", icon: "pencil", action: onEdit)
                    AndroidOutlineAction(title: "Открыть ссылку", icon: "arrow.up.right.square", action: openLink)
                }
            }
            .padding(15)
        }
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func carouselCircle(_ icon: String) -> some View {
        Image(systemName: icon).font(.system(size: 20, weight: .semibold)).foregroundStyle(.white)
            .frame(width: 38, height: 38).background(.black.opacity(0.55), in: Circle())
    }

    private func openLink() {
        guard let url = URL(string: link), !link.isEmpty else { return }
        openURL(url)
    }
}

private struct AndroidPetsScreen: View {
    let overview: TripOverview
    let client: SupabaseClient
    let onEdit: () -> Void
    @State private var selectedType = "shop"

    private var visiblePets: [PetPlace] {
        overview.petPlaces.filter { pet in
            selectedType == "vet" ? pet.type == "vet" || pet.type == "veterinary" : pet.type != "vet" && pet.type != "veterinary"
        }
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            FilterHeader()
            HStack(spacing: 0) {
                petTab("Зоомагазины", value: "shop")
                petTab("Ветеринары", value: "vet")
            }
            .padding(4).background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 14))

            HStack {
                Text("Поиск по каталогу").font(AppTheme.font(15, .regular)).foregroundStyle(Color(hex: 0x9999A3))
                Spacer()
            }
            .padding(.horizontal, 16).frame(height: 54)
            .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }

            Button(action: onEdit) {
                HStack(spacing: 11) {
                    Image(systemName: "plus").font(.system(size: 18, weight: .bold)).foregroundStyle(AppTheme.purple)
                        .frame(width: 36, height: 36).background(AppTheme.lavender.opacity(0.55), in: Circle())
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Добавить место вручную").font(AppTheme.font(15, .extrabold)).foregroundStyle(AppTheme.ink)
                        Text("Если его нет в каталоге").font(AppTheme.font(13, .semibold)).foregroundStyle(AppTheme.muted)
                    }
                    Spacer()
                    Image(systemName: "chevron.right").foregroundStyle(AppTheme.muted)
                }
                .padding(.horizontal, 13).frame(height: 74)
                .overlay { RoundedRectangle(cornerRadius: 17).stroke(AppTheme.border, lineWidth: 1) }
            }.buttonStyle(.plain)

            Text("Из каталога").font(AppTheme.font(19, .extrabold)).padding(.top, 4)
            if visiblePets.isEmpty {
                AndroidEmptyCard(icon: "pawprint", text: "В каталоге пока нет мест этого типа")
            } else {
                ForEach(visiblePets) { pet in AndroidPetCard(pet: pet, client: client) }
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }

    private func petTab(_ title: String, value: String) -> some View {
        Button { selectedType = value } label: {
            Text(title).font(AppTheme.font(14, .extrabold))
                .foregroundStyle(selectedType == value ? AppTheme.purple : Color(hex: 0x9999A3))
                .frame(maxWidth: .infinity).frame(height: 43)
                .background(selectedType == value ? AppTheme.surface : .clear, in: RoundedRectangle(cornerRadius: 12))
        }.buttonStyle(.plain)
    }
}

private struct AndroidPetCard: View {
    let pet: PetPlace
    let client: SupabaseClient
    @Environment(\.openURL) private var openURL

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 11) {
                RemotePhotoView(reference: pet.photo.nonEmpty, client: client, contentMode: .fill, cornerRadius: 12)
                    .frame(width: 91, height: 91)
                VStack(alignment: .leading, spacing: 5) {
                    Text(pet.type == "vet" ? "ВЕТЕРИНАР" : "ЗООМАГАЗИН")
                        .font(AppTheme.font(9, .extrabold)).foregroundStyle(AppTheme.purple)
                    Text(pet.name).font(AppTheme.font(16, .extrabold)).lineLimit(2)
                    Text(pet.address).font(AppTheme.font(11, .semibold)).foregroundStyle(AppTheme.muted).lineLimit(1)
                    if let rating = pet.rating {
                        HStack(spacing: 7) {
                            Text("★ \(rating, specifier: "%.1f")").font(AppTheme.font(12, .extrabold)).foregroundStyle(AppTheme.purple)
                            Text("(\(formatReviews(pet.reviewCount)))").font(AppTheme.font(11, .semibold)).foregroundStyle(AppTheme.muted)
                            Text(pet.openNow == true ? "Откр" : "Закр")
                                .font(AppTheme.font(11, .bold)).foregroundStyle(pet.openNow == true ? AppTheme.success : AppTheme.error)
                                .padding(.horizontal, 8).frame(height: 29)
                                .overlay { RoundedRectangle(cornerRadius: 8).stroke(pet.openNow == true ? AppTheme.success.opacity(0.5) : AppTheme.error.opacity(0.5)) }
                        }
                    }
                }
                Spacer(minLength: 0)
                VStack(spacing: 8) {
                    AndroidIconButton(icon: "mappin", action: openMap)
                    AndroidIconButton(icon: "plus", action: {})
                }
            }
            if !pet.note.isEmpty {
                Text(pet.note).font(AppTheme.font(10, .semibold)).foregroundStyle(AppTheme.muted).lineLimit(2)
            }
        }
        .padding(11)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .overlay { RoundedRectangle(cornerRadius: 20).stroke(AppTheme.border.opacity(0.8), lineWidth: 1) }
    }

    private func openMap() {
        guard let url = URL(string: pet.mapsURL), !pet.mapsURL.isEmpty else { return }
        openURL(url)
    }
}

private struct AndroidBudgetScreen: View {
    let overview: TripOverview
    let exchangeRates: ExchangeRateSnapshot?
    let onRefresh: () -> Void

    private var total: Double { overview.budgetExpenses.reduce(0) { $0 + $1.amount } }
    private var symbol: String { currencySymbol(overview.budgetCurrency) }
    private var people: Int { max(max(overview.budgetGroups.reduce(0) { $0 + $1.people }, overview.members.count), 1) }
    private var days: Int { max(tripDayCount(overview.dates), 1) }
    private var rate: Double {
        guard overview.budgetCurrency.uppercased() != "RUB",
              let raw = exchangeRates?.rates[overview.budgetCurrency.uppercased()], raw > 0 else { return 1 }
        return 1 / raw
    }
    private var categories: [(String, Color, Double)] {
        let definitions: [(String, Color, [String])] = [
            ("Жильё", AppTheme.purple, ["жиль", "hotel", "accommodation"]),
            ("Транспорт", Color(hex: 0xFFB020), ["транспорт", "transport"]),
            ("Еда и рестораны", Color(hex: 0x22B58A), ["еда", "ресторан", "food"]),
            ("Активности и билеты", Color(hex: 0x42A5E8), ["актив", "билет", "activity"]),
            ("Прочее", Color(hex: 0xEF6F9A), [])
        ]
        var used = Set<String>()
        return definitions.map { name, color, keys in
            let matches = overview.budgetExpenses.filter { expense in
                let key = expense.category.lowercased()
                let matched = keys.contains { key.contains($0) }
                if matched { used.insert(expense.id) }
                return matched
            }
            let value = keys.isEmpty
                ? overview.budgetExpenses.filter { !used.contains($0.id) }.reduce(0) { $0 + $1.amount }
                : matches.reduce(0) { $0 + $1.amount }
            return (name, color, value)
        }
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            VStack(alignment: .leading, spacing: 11) {
                Text("ОБЩАЯ СУММА").font(AppTheme.font(13, .extrabold)).foregroundStyle(.white)
                Text("\(symbol) \(formatMoney(total))").font(AppTheme.font(39, .extrabold)).foregroundStyle(.white).minimumScaleFactor(0.7)
            }
            .padding(.horizontal, 22).frame(maxWidth: .infinity, minHeight: 101, alignment: .leading)
            .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 21))

            HStack(spacing: 0) {
                ForEach(["₽", "€", "Kč", "$", "£", "zł", "Fr", "Ft"], id: \.self) { item in
                    Text(item).font(AppTheme.font(13, .extrabold))
                        .foregroundStyle(item == symbol ? AppTheme.ink : Color(hex: 0x9999A3))
                        .frame(maxWidth: .infinity).frame(height: 39)
                        .background(item == symbol ? AppTheme.surface : .clear, in: RoundedRectangle(cornerRadius: 11))
                }
            }
            .padding(4).background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 15))

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Курс валюты").font(AppTheme.font(12, .bold)).foregroundStyle(.white.opacity(0.72))
                    Spacer()
                    Text("вручную").font(AppTheme.font(11, .extrabold)).foregroundStyle(.white)
                        .padding(.horizontal, 10).frame(height: 30).background(.white.opacity(0.13), in: Capsule())
                    Text("Изменить").font(AppTheme.font(12, .extrabold)).foregroundStyle(.white)
                }
                Text("1 \(overview.budgetCurrency.uppercased()) = \(formatMoney(rate)) ₽")
                    .font(AppTheme.font(22, .extrabold)).foregroundStyle(.white)
                HStack {
                    Text("за 1 \(overview.budgetCurrency.uppercased()) · Задано вручную")
                        .font(AppTheme.font(10, .semibold)).foregroundStyle(.white.opacity(0.7))
                    Spacer()
                    Button(action: onRefresh) {
                        Label("Обновить", systemImage: "arrow.clockwise")
                            .font(AppTheme.font(11, .bold)).foregroundStyle(.white)
                            .padding(.horizontal, 12).frame(height: 35).background(.white.opacity(0.13), in: RoundedRectangle(cornerRadius: 10))
                    }.buttonStyle(.plain)
                }
            }
            .padding(18).background(AppTheme.primaryGradient, in: RoundedRectangle(cornerRadius: 20))

            HStack(spacing: 10) {
                BudgetMetric(label: "НА ЧЕЛОВЕКА", value: "\(symbol) \(formatMoney(total / Double(people)))")
                BudgetMetric(label: "В ДЕНЬ", value: "\(symbol) \(formatMoney(total / Double(days)))")
            }

            Text("По категориям").font(AppTheme.font(21, .extrabold)).padding(.top, 8)
            ForEach(Array(categories.enumerated()), id: \.offset) { _, category in
                BudgetCategory(name: category.0, color: category.1, amount: category.2, total: total, symbol: symbol)
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct BudgetMetric: View {
    let label: String
    let value: String
    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text(label).font(AppTheme.font(10, .extrabold)).foregroundStyle(AppTheme.muted)
            Text(value).font(AppTheme.font(18, .extrabold)).foregroundStyle(AppTheme.ink).minimumScaleFactor(0.75)
        }
        .padding(12).frame(maxWidth: .infinity, minHeight: 68, alignment: .leading)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 16))
        .overlay { RoundedRectangle(cornerRadius: 16).stroke(AppTheme.border, lineWidth: 1) }
    }
}

private struct BudgetCategory: View {
    let name: String
    let color: Color
    let amount: Double
    let total: Double
    let symbol: String
    private var percent: Int { total > 0 ? Int((amount / total * 100).rounded()) : 0 }

    var body: some View {
        VStack(spacing: 7) {
            HStack(spacing: 10) {
                RoundedRectangle(cornerRadius: 4).fill(color).frame(width: 11, height: 11)
                Text(name).font(AppTheme.font(14, .bold))
                Text("\(percent)%").font(AppTheme.font(13, .semibold)).foregroundStyle(AppTheme.muted)
                Spacer()
                Text("\(symbol) \(formatMoney(amount))").font(AppTheme.font(14, .extrabold))
            }
            GeometryReader { proxy in
                ZStack(alignment: .leading) {
                    Capsule().fill(AppTheme.track)
                    Capsule().fill(color).frame(width: proxy.size.width * CGFloat(percent) / 100)
                }
            }.frame(height: 8)
        }
    }
}

private struct AndroidMembersScreen: View {
    let overview: TripOverview
    let onEdit: () -> Void

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Участники").font(AppTheme.font(21, .extrabold))
                Spacer()
                Button(action: onEdit) {
                    Label("Изменить", systemImage: "pencil")
                        .font(AppTheme.font(14, .extrabold)).foregroundStyle(AppTheme.purple)
                        .padding(.horizontal, 14).frame(height: 43)
                        .background(AppTheme.surface.opacity(0.7), in: RoundedRectangle(cornerRadius: 15))
                }.buttonStyle(.plain)
            }
            .padding(.bottom, 2)

            ForEach(overview.members) { member in
                HStack(spacing: 12) {
                    Text(member.initials).font(AppTheme.font(18, .extrabold)).foregroundStyle(.white)
                        .frame(width: 44, height: 44).background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 12))
                    VStack(alignment: .leading, spacing: 4) {
                        Text(member.name).font(AppTheme.font(16, .extrabold)).lineLimit(1)
                        Text(member.email).font(AppTheme.font(13, .semibold)).foregroundStyle(AppTheme.muted).lineLimit(1)
                    }
                    Spacer()
                    Text(member.role.isEmpty ? "Участник" : member.role)
                        .font(AppTheme.font(11, .extrabold))
                        .foregroundStyle(member.role.localizedCaseInsensitiveContains("редактор") ? AppTheme.success : AppTheme.purple)
                        .padding(.horizontal, 11).frame(height: 31)
                        .background((member.role.localizedCaseInsensitiveContains("редактор") ? AppTheme.success : AppTheme.purple).opacity(0.09), in: Capsule())
                }
                .padding(.horizontal, 13).frame(height: 67)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 18))
            }
            DashedAddButton(title: "Пригласить участника", action: onEdit).padding(.top, 8)
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct AndroidPhotosScreen: View {
    let overview: TripOverview
    let client: SupabaseClient
    let onEdit: () -> Void

    private var references: [String] {
        var values = overview.coverPhotos.map(\.reference)
        values += overview.sights.flatMap(\.photos)
        values += overview.accommodations.flatMap(\.photos)
        values += overview.restaurants.flatMap(\.photos)
        values += overview.petPlaces.map(\.photo)
        var seen = Set<String>()
        return values.filter { !$0.isEmpty && seen.insert($0).inserted }
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 14) {
            HStack {
                Text("Фотографии").font(AppTheme.font(21, .extrabold))
                Spacer()
                Button(action: onEdit) {
                    Label("Загрузить", systemImage: "plus").font(AppTheme.font(13, .extrabold)).foregroundStyle(.white)
                        .padding(.horizontal, 14).frame(height: 40).background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 12))
                }.buttonStyle(.plain)
            }
            if references.isEmpty {
                AndroidEmptyCard(icon: "photo.on.rectangle", text: "Добавьте фотографии поездки")
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 10) {
                    ForEach(Array(references.enumerated()), id: \.offset) { _, reference in
                        RemotePhotoView(reference: reference, client: client, contentMode: .fill, cornerRadius: 14).frame(height: 150)
                    }
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct AndroidIconButton: View {
    let icon: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Image(systemName: icon).font(.system(size: 17, weight: .semibold)).foregroundStyle(AppTheme.purple)
                .frame(width: 36, height: 36).background(AppTheme.lavender.opacity(0.35), in: RoundedRectangle(cornerRadius: 11))
        }.buttonStyle(.plain)
    }
}

private struct AndroidOutlineAction: View {
    let title: String
    var icon: String?
    let action: () -> Void
    init(title: String, icon: String? = nil, action: @escaping () -> Void) {
        self.title = title; self.icon = icon; self.action = action
    }
    var body: some View {
        Button(action: action) {
            HStack(spacing: 7) {
                if let icon { Image(systemName: icon).foregroundStyle(AppTheme.purple) }
                Text(title)
            }
            .font(AppTheme.font(13, .extrabold)).foregroundStyle(AppTheme.ink)
            .frame(maxWidth: .infinity).frame(height: 43)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 12))
            .overlay { RoundedRectangle(cornerRadius: 12).stroke(AppTheme.border, lineWidth: 1) }
        }.buttonStyle(.plain)
    }
}

private struct AndroidFilledAction: View {
    let title: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Text(title).font(AppTheme.font(13, .extrabold)).foregroundStyle(.white)
                .frame(maxWidth: .infinity).frame(height: 43).background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 12))
        }.buttonStyle(.plain)
    }
}

private struct DashedAddButton: View {
    let title: String
    let action: () -> Void
    var body: some View {
        Button(action: action) {
            Label(title, systemImage: "plus").font(AppTheme.font(15, .extrabold)).foregroundStyle(AppTheme.purple)
                .frame(maxWidth: .infinity).frame(height: 47)
                .overlay { RoundedRectangle(cornerRadius: 16).stroke(AppTheme.border, style: StrokeStyle(lineWidth: 1.5, dash: [7, 6])) }
        }.buttonStyle(.plain)
    }
}

private struct AndroidEmptyCard: View {
    let icon: String
    let text: String
    var body: some View {
        VStack(spacing: 10) {
            Image(systemName: icon).font(.system(size: 28)).foregroundStyle(AppTheme.purple)
            Text(text).font(AppTheme.font(14, .bold)).foregroundStyle(AppTheme.muted).multilineTextAlignment(.center)
        }
        .frame(maxWidth: .infinity).padding(.vertical, 34)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
    }
}

private struct NumberedMapPin: Identifiable {
    let id = UUID()
    let title: String
    let coordinate: Coordinate
}

private struct NumberedTripMap: View {
    let pins: [NumberedMapPin]
    @State private var region: MKCoordinateRegion

    init(pins: [NumberedMapPin]) {
        self.pins = pins
        let latitude = pins.map { $0.coordinate.latitude }.reduce(0, +) / Double(max(pins.count, 1))
        let longitude = pins.map { $0.coordinate.longitude }.reduce(0, +) / Double(max(pins.count, 1))
        _region = State(initialValue: MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            span: MKCoordinateSpan(latitudeDelta: pins.count > 2 ? 14 : 0.08, longitudeDelta: pins.count > 2 ? 14 : 0.08)
        ))
    }

    var body: some View {
        if pins.isEmpty {
            ZStack {
                Color(hex: 0xE7E5EC)
                Image(systemName: "map").font(.system(size: 30)).foregroundStyle(AppTheme.muted)
            }
        } else {
            Map(coordinateRegion: $region, annotationItems: pins) { pin in
                MapAnnotation(coordinate: CLLocationCoordinate2D(latitude: pin.coordinate.latitude, longitude: pin.coordinate.longitude)) {
                    Text("\((pins.firstIndex { $0.id == pin.id } ?? 0) + 1)")
                        .font(AppTheme.font(10, .extrabold)).foregroundStyle(.white)
                        .frame(width: 24, height: 24).background(AppTheme.purple, in: Circle())
                        .overlay { Circle().stroke(.white, lineWidth: 2) }
                }
            }
        }
    }
}

private extension TripSection {
    var drawerIcon: String {
        switch self {
        case .overview: return "safari"
        case .route: return "point.3.connected.trianglepath.dotted"
        case .sights: return "mappin"
        case .restaurants: return "fork.knife"
        case .accommodation: return "bed.double"
        case .pets: return "heart"
        case .budget: return "wallet.bifold"
        case .members: return "person.2"
        case .photos: return "photo"
        }
    }
}

private func tripDayCount(_ dates: String) -> Int {
    for part in dates.components(separatedBy: "·") where part.lowercased().contains("дн") {
        let digits = part.filter(\.isNumber)
        if let value = Int(digits), value > 0 { return value }
    }
    return 1
}

private func dayFromDate(_ date: String) -> String {
    date.split(whereSeparator: { !$0.isNumber }).first.map(String.init) ?? "—"
}

private func monthFromDate(_ date: String) -> String {
    let parts = date.split(separator: " ")
    return parts.count > 1 ? String(parts[1].prefix(3)) : ""
}

private func routeTime(_ leg: RouteLeg) -> String {
    let values = [leg.checkIn, leg.checkOut].filter { !$0.isEmpty }
    return values.isEmpty ? "—" : values.joined(separator: " - ")
}

private func cityFlag(_ city: String) -> String {
    let value = city.lowercased()
    if ["праг", "praha", "prague"].contains(where: value.contains) { return "🇨🇿" }
    if ["инцел", "мюнх", "равенсбург", "berlin", "munich"].contains(where: value.contains) { return "🇩🇪" }
    if ["рим", "верон", "пиз", "милан", "вене", "флорен", "кьодж", "ravenn", "rome"].contains(where: value.contains) { return "🇮🇹" }
    if ["зальц", "vienna", "вен"].contains(where: value.contains) { return "🇦🇹" }
    return "📍"
}

private func currencySymbol(_ currency: String) -> String {
    switch currency.uppercased() {
    case "RUB": return "₽"
    case "EUR": return "€"
    case "USD": return "$"
    case "GBP": return "£"
    case "CZK": return "Kč"
    case "PLN": return "zł"
    case "CHF": return "Fr"
    case "HUF": return "Ft"
    default: return currency.uppercased()
    }
}

private func formatMoney(_ value: Double) -> String {
    value.formatted(.number.grouping(.automatic).precision(.fractionLength(value.rounded() == value ? 0 : 2)))
}

private func formatReviews(_ value: Int?) -> String {
    guard let value else { return "0" }
    return value.formatted(.number.grouping(.automatic))
}
