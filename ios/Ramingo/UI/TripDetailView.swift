import CoreLocation
import Foundation
import MapKit
import PhotosUI
import SwiftUI
import UIKit

struct TripDetailView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let tripID: String

    @State private var overview: TripOverview?
    @State private var selectedSection: TripSection = .overview
    @State private var isLoading = true
    @State private var localError: String?
    @State private var showDrawer = false
    @State private var overviewEditMode = false
    @State private var showFullEditor = false
    @State private var fullEditorSection = "details"
    @State private var fullEditorItemID: String?
    @State private var routeCheckInEditor: RouteLeg?
    @State private var isSavingRouteCheckIn = false
    @State private var routeCheckInError: String?
    @State private var showRouteAddEditor = false
    @State private var isAddingRoute = false
    @State private var routeAddError: String?
    @State private var restaurantStatusSavingID: String?
    @State private var showSettings = false
    @State private var showLeaveConfirmation = false
    @State private var isLeaving = false
    @State private var showAddPlaceHint = false
    @State private var showAccommodationAddChoice = false
    @State private var showAccommodationCatalog = false
    @State private var isAddingAccommodation = false
    @State private var weatherByCity: [String: WeatherSnapshot] = [:]
    @State private var isWeatherLoading = false
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
                        if showAddPlaceHint {
                            RamingoHintCard(
                                title: "Добавьте первое место",
                                message: "Откройте раздел достопримечательностей, ресторанов или питомцев и выберите каталог либо ручной ввод.",
                                icon: "mappin.and.ellipse",
                            )
                            .padding(.horizontal, 18)
                            .padding(.top, 14)
                        }
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
                        if section != .overview { overviewEditMode = false }
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
        .sheet(isPresented: $showFullEditor, onDismiss: { Task { await load() } }) {
            if let overview {
                TripEditorView(
                    tripID: overview.id,
                    initial: overview,
                    initialSection: fullEditorSection,
                    initialItemID: fullEditorItemID,
                )
                    .environmentObject(model)
            }
        }
        .sheet(item: $routeCheckInEditor) { leg in
            RouteCheckInEditorSheet(
                leg: leg,
                isSaving: isSavingRouteCheckIn,
                errorMessage: routeCheckInError,
                onCancel: {
                    routeCheckInEditor = nil
                    routeCheckInError = nil
                },
                onSave: { checkIn in
                    Task { await saveRouteCheckIn(leg, value: checkIn) }
                },
            )
        }
        .sheet(isPresented: $showRouteAddEditor) {
            RouteAddEditorSheet(
                isSaving: isAddingRoute,
                errorMessage: routeAddError,
                onCancel: {
                    showRouteAddEditor = false
                    routeAddError = nil
                },
                onSave: { draft in
                    Task { await addRoute(draft) }
                },
            )
        }
        .sheet(isPresented: $showSettings) { SettingsView() }
        .sheet(isPresented: $showAccommodationAddChoice) {
            IOSAccommodationAddChoiceSheet(
                onManual: {
                    Task { @MainActor in
                        await Task.yield()
                        openFullEditor(section: "accommodation")
                    }
                },
                onCatalog: {
                    Task { @MainActor in
                        await Task.yield()
                        showAccommodationCatalog = true
                    }
                },
            )
        }
        .sheet(isPresented: $showAccommodationCatalog, onDismiss: { Task { await load() } }) {
            if let overview {
                CatalogPickerSheet(
                    kind: .accommodation,
                    cities: accommodationCatalogCities(for: overview),
                    defaultCity: overview.cities.first ?? overview.accommodations.first?.city ?? "",
                ) { entry, _ in
                    Task { await addCatalogAccommodation(entry) }
                }
                .environmentObject(model)
            }
        }
        .confirmationDialog("Покинуть поездку?", isPresented: $showLeaveConfirmation, titleVisibility: .visible) {
            Button("Покинуть поездку", role: .destructive) {
                Task { await leaveTrip() }
            }
            Button("Отмена", role: .cancel) {}
        } message: {
            Text("Вы потеряете доступ к этой поездке и её общим данным.")
        }
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
            .accessibilityIdentifier("trip.drawer")

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
                Button {
                    withAnimation(.easeOut(duration: 0.16)) { overviewEditMode.toggle() }
                } label: {
                    Image(systemName: overviewEditMode ? "checkmark" : "pencil")
                        .font(.system(size: 18, weight: .semibold))
                        .foregroundStyle(overviewEditMode ? .white : AppTheme.purpleLight)
                        .frame(width: 40, height: 40)
                        .background(overviewEditMode ? AppTheme.purple : AppTheme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .overlay { RoundedRectangle(cornerRadius: 12).stroke(overviewEditMode ? AppTheme.purple : AppTheme.border, lineWidth: 1) }
                }
                .frame(width: 48, height: 48)
                .buttonStyle(.plain)
                .accessibilityLabel(overviewEditMode ? "Завершить редактирование главного экрана" : "Редактировать главный экран")
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
            AndroidOverviewScreen(
                overview: overview,
                weather: weatherByCity,
                client: model.client,
                editMode: overviewEditMode,
                weatherLoading: isWeatherLoading,
                onChanged: { Task { await load() } },
            )
        case .route:
            AndroidRouteScreen(
                overview: overview,
                onEdit: openRouteCheckInEditor,
                onAdd: {
                    routeAddError = nil
                    showRouteAddEditor = true
                },
            )
        case .sights:
            AndroidSightsScreen(overview: overview, client: model.client, onEdit: { itemID in openFullEditor(section: "sights", itemID: itemID) })
        case .restaurants:
            AndroidRestaurantsScreen(
                overview: overview,
                client: model.client,
                onEdit: { itemID in openFullEditor(section: "restaurants", itemID: itemID) },
                onStatusChange: { restaurant, status in
                    Task { await updateRestaurantStatus(restaurant, to: status) }
                },
                savingRestaurantID: restaurantStatusSavingID,
            )
        case .accommodation:
            AndroidAccommodationScreen(
                overview: overview,
                client: model.client,
                onEdit: { itemID in openFullEditor(section: "accommodation", itemID: itemID) },
                onAdd: { showAccommodationAddChoice = true },
            )
        case .pets:
            AndroidPetsScreen(
                overview: overview,
                client: model.client,
                onEdit: { openFullEditor(section: "pets") },
                onPetChanged: { Task { await load() } },
            )
        case .budget:
            AndroidBudgetScreen(overview: overview, exchangeRates: exchangeRates, onRefresh: { Task { await load() } })
        case .members:
            AndroidMembersScreen(
                overview: overview,
                onEdit: { openFullEditor(section: "members") },
                onLeave: { showLeaveConfirmation = true },
            )
        case .photos:
            AndroidPhotosScreen(overview: overview, client: model.client, onPhotosChanged: { Task { await load() } })
        }
    }

    private func closeDrawer() {
        withAnimation(.easeIn(duration: 0.18)) { showDrawer = false }
    }

    private func openFullEditor(section: String, itemID: String? = nil) {
        fullEditorSection = section
        fullEditorItemID = itemID
        showFullEditor = true
    }

    private func openRouteCheckInEditor(_ leg: RouteLeg) {
        routeCheckInError = nil
        routeCheckInEditor = leg
    }

    private func saveRouteCheckIn(_ leg: RouteLeg, value: String) async {
        guard !isSavingRouteCheckIn else { return }
        isSavingRouteCheckIn = true
        routeCheckInError = nil
        defer { isSavingRouteCheckIn = false }

        do {
            try await model.updateRouteLegDetails(
                id: tripID,
                dayID: leg.id,
                from: leg.from,
                to: leg.to,
                checkIn: value.trimmingCharacters(in: .whitespacesAndNewlines),
                checkOut: leg.checkOut,
                notes: leg.notes,
                mapsURL: leg.mapsURL,
                date: leg.date,
                dateDay: leg.dateDay,
                dateMonth: leg.dateMonth,
                weekday: leg.weekday,
                distance: leg.distance,
                travelTime: leg.travelTime,
            )
            routeCheckInEditor = nil
            await load()
        } catch {
            routeCheckInError = error.localizedDescription
        }
    }

    private func addRoute(_ draft: RouteAddDraft) async {
        guard !isAddingRoute else { return }
        isAddingRoute = true
        routeAddError = nil
        defer { isAddingRoute = false }

        do {
            let date = draft.date.trimmingCharacters(in: .whitespacesAndNewlines)
            try await model.addRouteLeg(
                id: tripID,
                from: draft.from,
                to: draft.to,
                checkIn: draft.checkIn,
                checkOut: draft.checkOut,
                notes: draft.notes,
                mapsURL: draft.mapsURL,
                date: date,
                dateDay: date.isEmpty ? "" : dayFromDate(date),
                dateMonth: monthFromDate(date),
                weekday: "",
                distance: draft.distance,
                travelTime: draft.travelTime,
            )
            showRouteAddEditor = false
            await load()
        } catch {
            routeAddError = error.localizedDescription
        }
    }

    private func updateRestaurantStatus(_ restaurant: Restaurant, to status: String) async {
        guard restaurantStatusSavingID == nil else { return }
        restaurantStatusSavingID = restaurant.id
        defer { restaurantStatusSavingID = nil }

        do {
            try await model.updateTripArrayItem(
                id: tripID,
                section: "restaurants",
                itemID: restaurant.id,
                fields: ["status": .string(status)],
            )
            await load()
            if status == "бронь" {
                await MainActor.run {
                    openFullEditor(section: "restaurants", itemID: restaurant.id)
                }
            }
        } catch {
            await MainActor.run { localError = error.localizedDescription }
        }
    }

    private func accommodationCatalogCities(for overview: TripOverview) -> [String] {
        (
            overview.cities +
                overview.routeLegs.flatMap { [$0.from, $0.to] } +
                overview.sights.map(\.city) +
                overview.restaurants.map(\.city) +
                overview.accommodations.map(\.city) +
                overview.petPlaces.map(\.city)
        )
        .flatMap { $0.split(separator: ",").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) } }
        .filter { !$0.isEmpty }
        .reduce(into: [String]()) { values, city in
            if !values.contains(where: { iosFilterCityKey($0) == iosFilterCityKey(city) }) {
                values.append(city)
            }
        }
    }

    private func addCatalogAccommodation(_ entry: CatalogEntry) async {
        guard !isAddingAccommodation else { return }
        isAddingAccommodation = true
        defer { isAddingAccommodation = false }
        do {
            try await model.addCatalogItem(id: tripID, entry: entry, walkDay: 1)
            await load()
        } catch {
            localError = error.localizedDescription
        }
    }

    private func leaveTrip() async {
        guard !isLeaving else { return }
        isLeaving = true
        defer { isLeaving = false }
        do {
            try await model.leaveTrip(id: tripID)
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
    }

    private func load() async {
        isLoading = overview == nil
        localError = nil
        isWeatherLoading = true
        defer {
            isLoading = false
            isWeatherLoading = false
        }
        do {
            guard let fresh = try await model.overview(for: tripID) else {
                overview = nil
                weatherByCity = [:]
                exchangeRates = nil
                return
            }
            overview = fresh
            if model.profile.onboardingCompleted,
               !model.profile.addPlaceHintSeen,
               fresh.sights.isEmpty,
               fresh.restaurants.isEmpty,
               fresh.petPlaces.isEmpty {
                showAddPlaceHint = true
                Task { await model.markAddPlaceHintSeen() }
            } else {
                showAddPlaceHint = false
            }
            async let weather = model.weather(for: fresh)
            async let rates: ExchangeRateSnapshot? = try? await model.exchangeRates(for: fresh)
            weatherByCity = await weather
            exchangeRates = await rates
        } catch {
            localError = error.localizedDescription
        }
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
                        .accessibilityIdentifier("trip.section.\(section.rawValue)")
                    }
                }
                .padding(.horizontal, 18)
                .padding(.top, 12)

                Spacer(minLength: 18)
                DrawerRow(title: "Мои путешествия", icon: "arrowshape.turn.up.left", selected: false, action: onTrips)
                    .accessibilityIdentifier("trip.home")
                    .padding(.horizontal, 18)
                DrawerRow(title: "Настройки", icon: "gearshape", selected: false, action: onSettings)
                    .accessibilityIdentifier("trip.settings")
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

private enum OverviewEditSheet: String, Identifiable {
    case map
    case weather

    var id: String { rawValue }
}

private struct AndroidOverviewScreen: View {
    @EnvironmentObject private var model: AppModel
    let overview: TripOverview
    let weather: [String: WeatherSnapshot]
    let client: SupabaseClient
    let editMode: Bool
    let weatherLoading: Bool
    let onChanged: () -> Void
    @State private var photoIndex = 0
    @State private var showTripWeather = false
    @State private var selectedTripDate: Date?
    @State private var selectedWeatherCity: String?
    @State private var roadRoute: [Coordinate] = []
    @State private var roadRouteDistanceMeters: CLLocationDistance?
    @State private var orderedBlocks: [String] = ["photo", "map", "weather"]
    @State private var selectedMapCities: [String] = []
    @State private var selectedWeatherCities: [String] = []
    @State private var editSheet: OverviewEditSheet?
    @State private var isSavingSettings = false
    @State private var actionMessage: String?
    @State private var selectedPhoto: PhotosPickerItem?

    private var photos: [CoverPhoto] { overview.coverPhotos }
    private var activePhoto: CoverPhoto? { photos.isEmpty ? nil : photos[photoIndex % photos.count] }
    private var activePhotoIndex: Int { photos.isEmpty ? 0 : photoIndex % photos.count }
    private var heroCity: String { activePhoto?.city.nonEmpty ?? overview.cities.first ?? overview.title }
    private var mapPins: [NumberedMapPin] {
        mapCities.compactMap { city in
            let coordinate = overview.cityCoordinates[city]
                ?? overview.cityCoordinates.first(where: { $0.key.caseInsensitiveCompare(city) == .orderedSame })?.value
                ?? model.resolveCityCoordinates(for: [city])[city]
            return coordinate.map { NumberedMapPin(title: city, coordinate: $0) }
        }
    }

    private var routeCities: [String] {
        (overview.cities + overview.overviewMapPoints + overview.routeLegs.flatMap { [$0.from, $0.to] })
            .filter { !$0.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty }
            .uniqued(by: iosFilterCityKey)
    }

    private var mapCities: [String] {
        let values = overview.overviewMapPoints.isEmpty ? routeCities : overview.overviewMapPoints
        return values.uniqued(by: iosFilterCityKey)
    }

    private var weatherCities: [String] {
        let values: [String]
        if !overview.overviewWeatherCities.isEmpty {
            values = overview.overviewWeatherCities
        } else if !overview.cities.isEmpty {
            values = overview.cities
        } else {
            values = mapCities
        }
        return values.uniqued(by: iosFilterCityKey)
    }

    private var tripDates: [Date] {
        iosWeatherTripDates(overview.dates)
    }

    private var tripCityByDate: [Date: String] {
        let dates = tripDates
        guard !dates.isEmpty else { return [:] }

        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current

        let transitions = overview.routeLegs.enumerated().compactMap { index, leg -> (Date, String, String, Int)? in
            let date = iosWeatherTripDateRange(leg.date)?.0 ?? dates.first.flatMap { calendar.date(byAdding: .day, value: index, to: $0) }
            guard let date else { return nil }
            return (date, leg.from, leg.to, index)
        }
        .sorted { lhs, rhs in
            if lhs.0 != rhs.0 { return lhs.0 < rhs.0 }
            return lhs.3 < rhs.3
        }

        var currentCity = transitions.first?.1 ?? overview.accommodations.first?.city ?? ""
        var transitionIndex = 0
        var result: [Date: String] = [:]

        for date in dates {
            if let accommodationCity = overview.accommodations.first(where: { accommodation in
                guard let range = iosWeatherTripDateRange(accommodation.dates) else { return false }
                if range.0 == range.1 {
                    return calendar.isDate(date, inSameDayAs: range.0) && !accommodation.city.isEmpty
                }
                return date >= range.0 && date < range.1 && !accommodation.city.isEmpty
            })?.city {
                result[date] = accommodationCity
                continue
            }

            while transitionIndex < transitions.count && transitions[transitionIndex].0 <= date {
                currentCity = transitions[transitionIndex].2
                transitionIndex += 1
            }
            if !currentCity.isEmpty { result[date] = currentCity }
        }
        return result
    }

    private var weatherSubtitle: String {
        guard showTripWeather, let selectedTripDate else {
            return "Текущая погода для городов маршрута"
        }
        return "Погода на \(iosWeatherDateLabel(selectedTripDate))"
    }

    private var selectedForecastAvailable: Bool {
        guard showTripWeather, let selectedTripDate else { return true }
        let key = iosWeatherISODate(selectedTripDate)
        return weatherCities.contains { city in
            weatherSnapshot(for: city)?.tripDays[key].map { $0.temperature != nil || $0.condition != nil } == true
        }
    }

    private var defaultWeatherCityForTrip: String? {
        guard let firstDate = tripDates.first,
              let firstCity = tripCityByDate[firstDate]
        else { return weatherCities.first }
        return weatherCities.first(where: { iosFilterCityKey($0) == iosFilterCityKey(firstCity) }) ?? weatherCities.first
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            if editMode {
                HStack(spacing: 7) {
                    Image(systemName: "hand.draw")
                        .font(.system(size: 15, weight: .semibold))
                    Text("Зажмите блок за ⋮⋮ и перенесите его")
                        .font(AppTheme.font(11, .bold))
                    Spacer(minLength: 0)
                }
                .foregroundStyle(AppTheme.purple)
                .padding(.horizontal, 12)
                .frame(minHeight: 39)
                .background(AppTheme.lavender.opacity(0.65), in: RoundedRectangle(cornerRadius: 12))
                .padding(.top, 9)
            }

            if let actionMessage {
                Text(actionMessage)
                    .font(AppTheme.font(12, .bold))
                    .foregroundStyle(AppTheme.error)
                    .padding(.top, 8)
            }

            VStack(alignment: .leading, spacing: 8) {
                ForEach(orderedBlocks, id: \.self) { block in
                    editableBlock(block)
                }
            }
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(.horizontal, 18)
        .onAppear {
            syncEditorState()
            syncWeatherState()
        }
        .onChange(of: overview.overviewBlocks) { _, _ in syncEditorState() }
        .onChange(of: overview.overviewMapPoints) { _, _ in syncEditorState() }
        .onChange(of: overview.overviewWeatherCities) { _, _ in syncEditorState() }
        .onChange(of: overview.dates) { _, _ in syncWeatherState() }
        .onChange(of: weatherCities) { _, _ in syncWeatherState() }
        .onChange(of: editMode) { _, next in
            if next { syncEditorState() }
        }
        .onChange(of: selectedPhoto) { _, item in
            guard let item else { return }
            Task { await importPhoto(item) }
        }
        .sheet(item: $editSheet) { sheet in
            switch sheet {
            case .map:
                OverviewCitySelectionSheet(
                    title: "Создать карту",
                    description: "Выберите города, которые должны быть на карте маршрута.",
                    cities: routeCities,
                    selectedCities: $selectedMapCities,
                    allowAddingCities: false,
                    isSaving: isSavingSettings,
                    onDismiss: { editSheet = nil },
                    onSave: { Task { await saveCities(selectedMapCities, key: "overviewMapPoints") } },
                )
            case .weather:
                OverviewCitySelectionSheet(
                    title: "Погода по городам",
                    description: "Добавьте или уберите города в блоке погоды на главном экране.",
                    cities: routeCities,
                    selectedCities: $selectedWeatherCities,
                    allowAddingCities: true,
                    isSaving: isSavingSettings,
                    onDismiss: { editSheet = nil },
                    onSave: { Task { await saveCities(selectedWeatherCities, key: "overviewWeatherCities") } },
                )
            }
        }
        .task(id: overview.cities + overview.overviewMapPoints + overview.overviewWeatherCities) {
            let route = await IOSMapRouteService.load(stops: mapPins.map(\.coordinate))
            roadRoute = route.coordinates
            roadRouteDistanceMeters = route.distanceMeters
        }
    }

    @ViewBuilder
    private func editableBlock(_ block: String) -> some View {
        VStack(alignment: .leading, spacing: 0) {
            if editMode {
                HStack(spacing: 6) {
                    Text("⋮⋮")
                        .font(AppTheme.font(17, .extrabold))
                    Text(blockTitle(block))
                        .font(AppTheme.font(12, .extrabold))
                    Spacer(minLength: 0)
                    blockAction(block)
                    if let index = orderedBlocks.firstIndex(of: block) {
                        Button { moveBlock(at: index, by: -1) } label: {
                            Image(systemName: "chevron.up")
                                .font(.system(size: 12, weight: .bold))
                        }
                        .disabled(index == 0 || isSavingSettings)
                        Button { moveBlock(at: index, by: 1) } label: {
                            Image(systemName: "chevron.down")
                                .font(.system(size: 12, weight: .bold))
                        }
                        .disabled(index == orderedBlocks.count - 1 || isSavingSettings)
                    }
                }
                .foregroundStyle(AppTheme.purple)
                .padding(.horizontal, 8)
                .padding(.bottom, 6)
            }
            blockContent(block)
        }
        .padding(editMode ? 7 : 0)
        .overlay {
            if editMode {
                RoundedRectangle(cornerRadius: 21, style: .continuous)
                    .stroke(AppTheme.purple, lineWidth: 1)
            }
        }
        .contentShape(Rectangle())
        .draggable(editMode ? block : "")
        .dropDestination(for: String.self) { items, _ in
            guard editMode, let source = items.first, source != block else { return false }
            moveBlock(source: source, before: block)
            return true
        }
    }

    @ViewBuilder
    private func blockAction(_ block: String) -> some View {
        switch block {
        case "photo":
            PhotosPicker(selection: $selectedPhoto, matching: .images) {
                editActionLabel("Изменить фото")
            }
            .disabled(isSavingSettings)
        case "map":
            Button {
                selectedMapCities = overview.overviewMapPoints.isEmpty ? mapCities : overview.overviewMapPoints
                editSheet = .map
            } label: {
                editActionLabel("Изменить карту")
            }
            .disabled(isSavingSettings)
        default:
            Button {
                selectedWeatherCities = overview.overviewWeatherCities.isEmpty ? weatherCities : overview.overviewWeatherCities
                editSheet = .weather
            } label: {
                editActionLabel("Настроить погоду")
            }
            .disabled(isSavingSettings)
        }
    }

    private func editActionLabel(_ title: String) -> some View {
        Text(title)
            .font(AppTheme.font(11, .extrabold))
            .foregroundStyle(AppTheme.purple)
            .padding(.horizontal, 8)
            .frame(minHeight: 30)
            .background(AppTheme.lavender.opacity(0.55), in: Capsule())
    }

    @ViewBuilder
    private func blockContent(_ block: String) -> some View {
        switch block {
        case "photo": photoBlock
        case "map": mapBlock
        default: weatherBlock
        }
    }

    private var photoBlock: some View {
        ZStack(alignment: .bottomLeading) {
            RemotePhotoView(reference: activePhoto?.reference, client: client, contentMode: .fill, cornerRadius: 20)
                .frame(height: 270)
                .overlay {
                    LinearGradient(colors: [.clear, .black.opacity(0.45)], startPoint: .center, endPoint: .bottom)
                        .clipShape(RoundedRectangle(cornerRadius: 20))
                }
            Text(heroCity)
                .font(AppTheme.font(24, .extrabold))
                .foregroundStyle(.white)
                .padding(.leading, 15)
                .padding(.bottom, 14)
            if photos.count > 1 {
                HStack(spacing: 5) {
                    ForEach(photos.indices, id: \.self) { index in
                        Capsule()
                            .fill(index == activePhotoIndex ? Color.white : Color.white.opacity(0.6))
                            .frame(width: index == activePhotoIndex ? 18 : 6, height: 6)
                    }
                }
                .padding(14)
                .frame(maxWidth: .infinity, maxHeight: .infinity, alignment: .topTrailing)

                HStack {
                    carouselButton("chevron.left", delta: -1)
                    Spacer()
                    carouselButton("chevron.right", delta: 1)
                }
                .padding(.horizontal, 7)
            }
        }
        .padding(.top, 2)
    }

    private var mapBlock: some View {
        VStack(spacing: 0) {
            NumberedTripMap(pins: mapPins, routeCoordinates: roadRoute, showsUserLocation: true)
                .frame(height: 200)
            HStack {
                Text("Общий маршрут")
                    .font(AppTheme.font(13, .semibold))
                    .foregroundStyle(AppTheme.muted)
                Spacer()
                Text(routeSummary)
                    .font(AppTheme.font(14, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(1)
                    .minimumScaleFactor(0.72)
            }
            .padding(.horizontal, 16)
            .frame(height: 46)
            .background(AppTheme.surface)
        }
        .background(AppTheme.surface)
        .clipShape(RoundedRectangle(cornerRadius: 20, style: .continuous))
    }

    private var weatherBlock: some View {
        VStack(alignment: .leading, spacing: 0) {
            Text("Погода по маршруту")
                .font(AppTheme.font(20, .extrabold))
                .foregroundStyle(AppTheme.ink)
                .padding(.top, 2)
            Text(weatherSubtitle)
                .font(AppTheme.font(12, .semibold))
                .foregroundStyle(AppTheme.muted)
                .padding(.top, 8)
            HStack(spacing: 0) {
                weatherModeButton("Сейчас", trip: false)
                weatherModeButton("На даты поездки", trip: true)
            }
            .padding(4)
                .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 13))
                .padding(.top, 8)

            if showTripWeather, selectedTripDate != nil, !weatherLoading, !selectedForecastAvailable {
                Text("Для выбранной даты точный прогноз появится примерно за 16 дней до поездки.")
                    .font(AppTheme.font(11, .bold))
                    .foregroundStyle(AppTheme.purple)
                    .frame(maxWidth: .infinity, alignment: .leading)
                    .padding(.horizontal, 12)
                    .padding(.vertical, 10)
                    .background(AppTheme.lavender.opacity(0.55), in: RoundedRectangle(cornerRadius: 12))
                    .padding(.top, 8)
            }

            if !weatherLoading && !weatherCities.isEmpty && weather.isEmpty {
                HStack(spacing: 8) {
                    Image(systemName: "wifi.exclamationmark")
                        .foregroundStyle(AppTheme.purple)
                    Text("Не удалось загрузить погоду. Проверьте интернет.")
                        .font(AppTheme.font(11, .bold))
                        .foregroundStyle(AppTheme.muted)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    Button("Повторить", action: onChanged)
                        .font(AppTheme.font(11, .extrabold))
                }
                .padding(.horizontal, 12)
                .frame(minHeight: 42)
                .background(AppTheme.lavender.opacity(0.55), in: RoundedRectangle(cornerRadius: 12))
                .padding(.top, 8)
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    ForEach(weatherCities, id: \.self) { city in
                        Button {
                            selectedWeatherCity = city
                        } label: {
                            AndroidWeatherCard(
                                city: city,
                                snapshot: weatherSnapshot(for: city),
                                isLoading: weatherLoading,
                                isSelected: showTripWeather && iosFilterCityKey(selectedWeatherCity ?? "") == iosFilterCityKey(city),
                                showTripWeather: showTripWeather,
                                tripDate: showTripWeather ? selectedTripDate : nil,
                                photo: photos.first(where: { iosFilterCityKey($0.city) == iosFilterCityKey(city) })?.reference ?? photos.first?.reference,
                                client: client
                            )
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .padding(.top, 8)

            if showTripWeather, !tripDates.isEmpty {
                IOSWeatherTripDayPanel(
                    dates: tripDates,
                    selectedDate: selectedTripDate ?? tripDates[0],
                    selectedCity: selectedWeatherCity ?? weatherCities.first,
                    cityByDate: tripCityByDate,
                    weather: weather,
                    onDateSelected: { selectedTripDate = $0 },
                )
                .padding(.top, 8)
            }
        }
    }

    private func blockTitle(_ block: String) -> String {
        switch block {
        case "photo": return "Фото"
        case "map": return "Карта"
        default: return "Погода"
        }
    }

    private func syncEditorState() {
        let allowed = ["photo", "map", "weather"]
        let storedBlocks = overview.overviewBlocks.filter { allowed.contains($0) }
        orderedBlocks = storedBlocks.isEmpty ? allowed : storedBlocks
        selectedMapCities = overview.overviewMapPoints.isEmpty ? mapCities : overview.overviewMapPoints
        selectedWeatherCities = overview.overviewWeatherCities.isEmpty ? weatherCities : overview.overviewWeatherCities
    }

    private func syncWeatherState() {
        let dates = tripDates
        if let selectedTripDate, dates.contains(selectedTripDate) {
            // Keep the selected day when the weather response refreshes.
        } else {
            selectedTripDate = dates.first
        }

        if let selectedWeatherCity,
           weatherCities.contains(where: { iosFilterCityKey($0) == iosFilterCityKey(selectedWeatherCity) }) {
            // Keep the selected city when the overview is refreshed.
        } else {
            selectedWeatherCity = defaultWeatherCityForTrip
        }
    }

    private func moveBlock(at index: Int, by offset: Int) {
        guard orderedBlocks.indices.contains(index), offset != 0 else { return }
        var next = orderedBlocks
        let source = next.remove(at: index)
        let destination = max(0, min(next.count, index + offset))
        next.insert(source, at: destination)
        orderedBlocks = next
        Task { await persistBlocks() }
    }

    private func moveBlock(source: String, before target: String) {
        guard source != target,
              let sourceIndex = orderedBlocks.firstIndex(of: source),
              let targetIndex = orderedBlocks.firstIndex(of: target)
        else { return }
        var next = orderedBlocks
        next.remove(at: sourceIndex)
        let adjustedTarget = sourceIndex < targetIndex ? targetIndex - 1 : targetIndex
        next.insert(source, at: adjustedTarget)
        orderedBlocks = next
        Task { await persistBlocks() }
    }

    private func persistBlocks() async {
        guard !isSavingSettings else { return }
        isSavingSettings = true
        defer { isSavingSettings = false }
        do {
            try await model.updateTripField(
                id: overview.id,
                key: "overviewBlocks",
                value: .array(orderedBlocks.map(JSONValue.string)),
            )
            actionMessage = nil
            onChanged()
        } catch {
            actionMessage = error.localizedDescription
            syncEditorState()
        }
    }

    private func saveCities(_ cities: [String], key: String) async {
        guard !isSavingSettings else { return }
        isSavingSettings = true
        defer { isSavingSettings = false }
        do {
            try await model.updateTripField(
                id: overview.id,
                key: key,
                value: .array(cities.map(JSONValue.string)),
            )
            actionMessage = nil
            editSheet = nil
            onChanged()
        } catch {
            actionMessage = error.localizedDescription
        }
    }

    private func importPhoto(_ item: PhotosPickerItem) async {
        defer { selectedPhoto = nil }
        guard let data = try? await item.loadTransferable(type: Data.self), !data.isEmpty else {
            actionMessage = "Не удалось прочитать изображение."
            return
        }
        isSavingSettings = true
        defer { isSavingSettings = false }
        do {
            let uploadData = UIImage(data: data)?.jpegData(compressionQuality: 0.88) ?? data
            try await model.addCoverPhoto(id: overview.id, data: uploadData, city: activePhoto?.city ?? overview.cities.first ?? "")
            actionMessage = nil
            onChanged()
        } catch {
            actionMessage = error.localizedDescription
        }
    }

    private var routeDistanceLabel: String? {
        if let roadRouteDistanceMeters, roadRouteDistanceMeters > 0 {
            return "\(Int((roadRouteDistanceMeters / 1_000).rounded())) км"
        }
        let fallback = zip(mapPins.map(\.coordinate), mapPins.dropFirst().map(\.coordinate)).reduce(0.0) { total, pair in
            total + CLLocation(
                latitude: pair.0.latitude,
                longitude: pair.0.longitude,
            ).distance(from: CLLocation(latitude: pair.1.latitude, longitude: pair.1.longitude))
        }
        guard fallback > 0 else { return nil }
        return "≈ \(Int((fallback / 1_000).rounded())) км"
    }

    private var routeSummary: String {
        let base = "\(overview.routeLegs.count) переездов · \(overview.cities.count) городов"
        guard let routeDistanceLabel else { return base }
        return "\(base) · \(routeDistanceLabel)"
    }

    private func weatherModeButton(_ title: String, trip: Bool) -> some View {
        Button {
            withAnimation(.easeOut(duration: 0.16)) { showTripWeather = trip }
        } label: {
            Text(title)
                .font(AppTheme.font(13, .bold))
                .foregroundStyle(showTripWeather == trip ? AppTheme.ink : Color(hex: 0x9999A3))
                .padding(.horizontal, 14)
                .frame(height: 32)
                .background(showTripWeather == trip ? AppTheme.surface : .clear, in: RoundedRectangle(cornerRadius: 12))
        }
        .buttonStyle(.plain)
    }

    private func weatherSnapshot(for city: String) -> WeatherSnapshot? {
        weather[city]
            ?? weather.first(where: { iosFilterCityKey($0.key) == iosFilterCityKey(city) })?.value
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

private struct OverviewCitySelectionSheet: View {
    let title: String
    let description: String
    let cities: [String]
    @Binding var selectedCities: [String]
    let allowAddingCities: Bool
    let isSaving: Bool
    let onDismiss: () -> Void
    let onSave: () -> Void

    @State private var newCity = ""

    private var visibleCities: [String] {
        (cities + selectedCities).uniqued(by: iosFilterCityKey)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Text(description)
                        .font(AppTheme.font(13, .semibold))
                        .foregroundStyle(AppTheme.muted)
                }

                Section("Города") {
                    ForEach(visibleCities, id: \.self) { city in
                        Button {
                            toggle(city)
                        } label: {
                            HStack {
                                Text(city)
                                    .font(AppTheme.font(15, .bold))
                                    .foregroundStyle(AppTheme.ink)
                                Spacer()
                                Image(systemName: isSelected(city) ? "checkmark.circle.fill" : "circle")
                                    .foregroundStyle(isSelected(city) ? AppTheme.purple : AppTheme.muted)
                            }
                        }
                        .buttonStyle(.plain)
                    }
                }

                if allowAddingCities {
                    Section("Добавить город") {
                        HStack(spacing: 8) {
                            TextField("Например, Берлин", text: $newCity)
                                .textInputAutocapitalization(.words)
                            Button("Добавить") {
                                addCity()
                            }
                            .font(AppTheme.font(13, .extrabold))
                            .disabled(newCity.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                        }
                    }
                }
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена") { onDismiss() }
                        .disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? "Сохраняем…" : "Сохранить") { onSave() }
                        .font(AppTheme.font(14, .bold))
                        .disabled(isSaving)
                }
            }
        }
    }

    private func isSelected(_ city: String) -> Bool {
        selectedCities.contains { iosFilterCityKey($0) == iosFilterCityKey(city) }
    }

    private func toggle(_ city: String) {
        if isSelected(city) {
            selectedCities.removeAll { iosFilterCityKey($0) == iosFilterCityKey(city) }
        } else {
            selectedCities.append(city)
        }
    }

    private func addCity() {
        let city = newCity.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !city.isEmpty, !isSelected(city) else { return }
        selectedCities.append(city)
        newCity = ""
    }
}

private struct AndroidWeatherCard: View {
    let city: String
    let snapshot: WeatherSnapshot?
    let isLoading: Bool
    let isSelected: Bool
    let showTripWeather: Bool
    let tripDate: Date?
    let photo: String?
    let client: SupabaseClient

    var body: some View {
        ZStack(alignment: .leading) {
            RemotePhotoView(reference: photo, client: client, contentMode: .fill, cornerRadius: 16)
            LinearGradient(colors: [.black.opacity(0.08), .black.opacity(0.62)], startPoint: .top, endPoint: .bottom)
                .clipShape(RoundedRectangle(cornerRadius: 16))
            VStack(alignment: .leading) {
                Text(city).font(AppTheme.font(13, .bold))
                Spacer()
                if isLoading && snapshot == nil {
                    ProgressView()
                        .tint(.white)
                        .scaleEffect(0.8)
                    Text("Загружаем…")
                        .font(AppTheme.font(11, .semibold))
                } else {
                    Text(displayedTemperature)
                        .font(AppTheme.font(26, .extrabold))
                    Text(displayedCondition)
                        .font(AppTheme.font(11, .semibold))
                }
            }
            .foregroundStyle(.white)
            .padding(12)
        }
        .frame(width: 120, height: 150)
        .overlay {
            if isSelected {
                RoundedRectangle(cornerRadius: 16, style: .continuous)
                    .stroke(AppTheme.purple, lineWidth: 3)
            }
        }
    }

    private var selectedTripDay: WeatherDaySnapshot? {
        guard let tripDate, let snapshot else { return nil }
        return snapshot.tripDays[iosWeatherISODate(tripDate)]
    }

    private var displayedTemperature: String {
        if showTripWeather {
            return selectedTripDay?.temperature ?? (tripDate == nil ? snapshot?.tripTemperature : nil) ?? "—"
        }
        return snapshot?.temperature ?? "—"
    }

    private var displayedCondition: String {
        if showTripWeather {
            return selectedTripDay?.condition ?? (tripDate == nil ? snapshot?.tripCondition : nil) ?? "Нет прогноза"
        }
        return snapshot?.condition ?? "Нет данных"
    }
}

private struct IOSWeatherTripDayPanel: View {
    let dates: [Date]
    let selectedDate: Date
    let selectedCity: String?
    let cityByDate: [Date: String]
    let weather: [String: WeatherSnapshot]
    let onDateSelected: (Date) -> Void

    @State private var visibleDateStart = 0

    private let pageSize = 8

    private var visibleDates: [Date] {
        dates.dropFirst(visibleDateStart).prefix(pageSize).map { $0 }
    }

    private var nextDateStart: Int {
        min(visibleDateStart + pageSize, dates.count)
    }

    private var previousDateStart: Int {
        max(visibleDateStart - pageSize, 0)
    }

    private var selectedWeather: WeatherDaySnapshot? {
        guard let selectedCity,
              let snapshot = snapshot(for: selectedCity)
        else { return nil }
        return snapshot.tripDays[iosWeatherISODate(selectedDate)]
    }

    private var selectedCityDateCount: Int {
        guard let selectedCity else { return 0 }
        return dates.filter {
            iosFilterCityKey(cityByDate[$0] ?? "") == iosFilterCityKey(selectedCity)
        }.count
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 10) {
            HStack(alignment: .top) {
                VStack(alignment: .leading, spacing: 2) {
                    Text("Каждый день поездки")
                        .font(AppTheme.font(15, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                    Text(
                        selectedCityDateCount > 0
                            ? "Дни в \(selectedCity ?? "городе") подсвечены"
                            : "Выбранный день"
                    )
                    .font(AppTheme.font(11, .semibold))
                    .foregroundStyle(AppTheme.muted)
                }

                Spacer(minLength: 8)

                HStack(spacing: 2) {
                    if visibleDateStart > 0 {
                        Button {
                            onDateSelected(dates[previousDateStart])
                        } label: {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundStyle(AppTheme.purple)
                                .frame(width: 30, height: 32)
                        }
                        .buttonStyle(.plain)
                    }
                    Text("\(dates.count) \(iosWeatherDayWord(dates.count))")
                        .font(AppTheme.font(11, .extrabold))
                        .foregroundStyle(AppTheme.purple)
                        .padding(.horizontal, 10)
                        .frame(height: 32)
                        .background(AppTheme.lavender.opacity(0.45), in: Capsule())
                    if nextDateStart < dates.count {
                        Button {
                            onDateSelected(dates[nextDateStart])
                        } label: {
                            Image(systemName: "chevron.right")
                                .font(.system(size: 17, weight: .bold))
                                .foregroundStyle(AppTheme.purple)
                                .frame(width: 30, height: 32)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 7) {
                    ForEach(visibleDates, id: \.self) { date in
                        dateTile(date)
                    }
                    if nextDateStart < dates.count {
                        Button {
                            onDateSelected(dates[nextDateStart])
                        } label: {
                            VStack(spacing: 2) {
                                Text("+\(dates.count - nextDateStart)")
                                    .font(AppTheme.font(14, .extrabold))
                                Text("дней")
                                    .font(AppTheme.font(10, .bold))
                                    .foregroundStyle(AppTheme.muted)
                            }
                            .frame(width: 64, height: 70)
                            .foregroundStyle(AppTheme.ink)
                            .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }
                }
            }

            Rectangle()
                .fill(AppTheme.border)
                .frame(height: 1)

            HStack(alignment: .center, spacing: 10) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("\(iosWeatherDateLabel(selectedDate)) · \(selectedCity ?? "Города маршрута")")
                        .font(AppTheme.font(12, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                        .lineLimit(1)
                        .minimumScaleFactor(0.78)
                    Text(
                        selectedWeather.flatMap { $0.condition }.map { "\($0) · хороший день для прогулки" } ?? "Прогноз появится позже"
                    )
                    .font(AppTheme.font(11, .semibold))
                    .foregroundStyle(AppTheme.muted)
                    .lineLimit(2)
                }
                Spacer(minLength: 6)
                Text(weatherDayTemperature(selectedWeather?.temperature, isEstimate: selectedWeather?.isEstimate == true))
                    .font(AppTheme.font(27, .extrabold))
                    .foregroundStyle(AppTheme.ink)
            }
        }
        .padding(14)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 17, style: .continuous))
        .overlay {
            RoundedRectangle(cornerRadius: 17, style: .continuous)
                .stroke(AppTheme.border, lineWidth: 1)
        }
        .onAppear { alignVisibleDatePage() }
        .onChange(of: selectedDate) { _, _ in alignVisibleDatePage() }
        .onChange(of: dates) { _, _ in alignVisibleDatePage() }
    }

    @ViewBuilder
    private func dateTile(_ date: Date) -> some View {
        let selected = calendar.isDate(date, inSameDayAs: selectedDate)
        let isSelectedCityDate = selectedCity != nil &&
            iosFilterCityKey(cityByDate[date] ?? "") == iosFilterCityKey(selectedCity ?? "")
        let dayWeather = selectedCity.flatMap { snapshot(for: $0)?.tripDays[iosWeatherISODate(date)] }

        Button {
            onDateSelected(date)
        } label: {
            VStack(alignment: .leading, spacing: 0) {
                Text(calendar.component(.day, from: date).description)
                    .font(AppTheme.font(14, .extrabold))
                Text(iosWeatherShortMonth(date))
                    .font(AppTheme.font(10, .bold))
                    .padding(.top, 2)
                Spacer(minLength: 0)
                Text(weatherDayTemperature(dayWeather?.temperature, isEstimate: dayWeather?.isEstimate == true))
                    .font(AppTheme.font(12, .extrabold))
            }
            .foregroundStyle(selected ? Color(hex: 0x315C7C) : AppTheme.ink)
            .padding(.horizontal, 9)
            .padding(.vertical, 7)
            .frame(width: 66, height: 78, alignment: .leading)
            .background(
                selected ? Color(hex: 0xDCE9F3) : isSelectedCityDate ? Color(hex: 0xE8F2F8) : AppTheme.surface2,
                in: RoundedRectangle(cornerRadius: 13, style: .continuous),
            )
            .overlay {
                RoundedRectangle(cornerRadius: 13, style: .continuous)
                    .stroke(
                        selected || isSelectedCityDate ? AppTheme.purple : .clear,
                        lineWidth: selected ? 1 : 2,
                    )
            }
        }
        .buttonStyle(.plain)
    }

    private var calendar: Calendar {
        var value = Calendar(identifier: .gregorian)
        value.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
        return value
    }

    private func alignVisibleDatePage() {
        guard let selectedIndex = dates.firstIndex(where: { calendar.isDate($0, inSameDayAs: selectedDate) }) else { return }
        visibleDateStart = (selectedIndex / pageSize) * pageSize
    }

    private func snapshot(for city: String) -> WeatherSnapshot? {
        weather[city]
            ?? weather.first(where: { iosFilterCityKey($0.key) == iosFilterCityKey(city) })?.value
    }
}

private func iosWeatherDayWord(_ count: Int) -> String {
    switch count % 100 {
    case 11...14: return "дней"
    default:
        switch count % 10 {
        case 1: return "день"
        case 2...4: return "дня"
        default: return "дней"
        }
    }
}

private func iosWeatherShortMonth(_ date: Date) -> String {
    let months = ["янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"]
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
    return months[calendar.component(.month, from: date) - 1]
}

private func iosWeatherDateLabel(_ date: Date) -> String {
    let months = ["января", "февраля", "марта", "апреля", "мая", "июня", "июля", "августа", "сентября", "октября", "ноября", "декабря"]
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
    return "\(calendar.component(.day, from: date)) \(months[calendar.component(.month, from: date) - 1])"
}

private func weatherDayTemperature(_ value: String?, isEstimate: Bool) -> String {
    guard let value, !value.isEmpty else { return "—" }
    let normalized = value
        .replacingOccurrences(of: "°C", with: "°")
    return "\(isEstimate ? "≈ " : "")\(normalized)"
}

private struct AndroidRouteScreen: View {
    let overview: TripOverview
    let onEdit: (RouteLeg) -> Void
    let onAdd: () -> Void

    private var routeCityCount: Int {
        let values = overview.overviewMapPoints.isEmpty
            ? overview.routeLegs.flatMap { [$0.from, $0.to] }
            : overview.overviewMapPoints
        return values
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
            .uniqued(by: iosFilterCityKey)
            .count
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 14) {
            Text("\(tripDayCount(overview.dates)) ДНЕЙ · \(routeCityCount) ГОРОДОВ")
                .font(AppTheme.font(12, .extrabold))
                .foregroundStyle(AppTheme.purple)
                .padding(.bottom, 4)
            if overview.routeLegs.isEmpty {
                AndroidEmptyCard(icon: "point.topleft.down.to.point.bottomright.curvepath", text: "Добавьте города и переезды")
            } else {
                ForEach(Array(overview.routeLegs.enumerated()), id: \.element.id) { index, leg in
                    AndroidRouteLegCard(
                        leg: leg,
                        tripDates: overview.dates,
                        dayIndex: index,
                        onEdit: { onEdit(leg) },
                    )
                }
            }
            if overview.canEdit {
                DashedAddButton(title: "Добавить переезд", action: onAdd)
                    .accessibilityIdentifier("route.add")
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 18)
    }
}

private struct AndroidRouteLegCard: View {
    let leg: RouteLeg
    let tripDates: String
    let dayIndex: Int
    let onEdit: () -> Void
    @State private var copied = false

    var body: some View {
        let dateParts = routeDateParts(for: leg)
        let timing = routeTiming(leg)

        VStack(spacing: 13) {
            HStack(alignment: .top, spacing: 12) {
                VStack(spacing: 1) {
                    Text(dateParts.day)
                        .font(AppTheme.font(24, .extrabold)).foregroundStyle(AppTheme.purple)
                    Text(dateParts.month)
                        .font(AppTheme.font(10, .bold)).foregroundStyle(AppTheme.purpleDeep)
                }
                .frame(width: 39)
                VStack(alignment: .leading, spacing: 4) {
                    RouteStopLine(city: leg.from, muted: true, last: false)
                    RouteStopLine(city: leg.to, muted: false, last: true)
                }
                Spacer(minLength: 4)
                AndroidIconButton(icon: copied ? "checkmark" : "doc.on.doc", action: copyLeg)
                AndroidIconButton(icon: "pencil", action: onEdit)
                    .accessibilityIdentifier("route.leg.edit")
            }
            HStack(spacing: 12) {
                Image(systemName: "key.fill").font(.system(size: 13)).foregroundStyle(AppTheme.purple)
                Text(timing.label).font(AppTheme.font(14, .bold))
                Spacer()
                Text(timing.value).font(AppTheme.font(14, .bold))
            }
            .padding(.horizontal, 13)
            .padding(.vertical, 11)
            .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 13))
            .overlay { RoundedRectangle(cornerRadius: 13).stroke(AppTheme.border, lineWidth: 1) }
        }
        .padding(.horizontal, 16)
        .padding(.top, 16)
        .padding(.bottom, 14)
        .frame(minHeight: 158, alignment: .top)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
    }

    private func copyLeg() {
        let timing = routeTiming(leg)
        UIPasteboard.general.string = "\(leg.date) · \(leg.from) → \(leg.to) · \(timing.label): \(timing.value)"
        withAnimation(.easeOut(duration: 0.15)) { copied = true }
        Task {
            try? await Task.sleep(for: .seconds(1.5))
            await MainActor.run { withAnimation(.easeOut(duration: 0.15)) { copied = false } }
        }
    }

    private func routeDateParts(for leg: RouteLeg) -> (day: String, month: String) {
        if let date = iosWeatherTripDateRange(leg.date)?.0 {
            return iosRouteDateParts(date)
        }
        if leg.date.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
           (!leg.dateDay.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                !leg.dateMonth.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty) {
            return (
                leg.dateDay.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? "—",
                leg.dateMonth.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty?.uppercased() ?? "—",
            )
        }
        if let date = iosWeatherTripDates(tripDates).dropFirst(dayIndex).first {
            return iosRouteDateParts(date)
        }
        return (
            leg.dateDay.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? "—",
            leg.dateMonth.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty?.uppercased() ?? "—",
        )
    }

    private func iosRouteDateParts(_ date: Date) -> (day: String, month: String) {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
        let months = ["ЯНВ", "ФЕВ", "МАР", "АПР", "МАЙ", "ИЮН", "ИЮЛ", "АВГ", "СЕН", "ОКТ", "НОЯ", "ДЕК"]
        return (
            String(calendar.component(.day, from: date)),
            months[calendar.component(.month, from: date) - 1],
        )
    }
}

private struct RouteCheckInEditorSheet: View {
    let leg: RouteLeg
    let isSaving: Bool
    let errorMessage: String?
    let onCancel: () -> Void
    let onSave: (String) -> Void
    @State private var checkIn: String

    init(
        leg: RouteLeg,
        isSaving: Bool,
        errorMessage: String?,
        onCancel: @escaping () -> Void,
        onSave: @escaping (String) -> Void,
    ) {
        self.leg = leg
        self.isSaving = isSaving
        self.errorMessage = errorMessage
        self.onCancel = onCancel
        self.onSave = onSave
        _checkIn = State(initialValue: leg.checkIn)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(alignment: .top, spacing: 12) {
                VStack(alignment: .leading, spacing: 5) {
                    Text("Изменить переезд")
                        .font(AppTheme.font(23, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                    Text("Обновите время заселения для этого переезда.")
                        .font(AppTheme.font(13, .semibold))
                        .foregroundStyle(AppTheme.muted)
                }
                Spacer(minLength: 0)
                Button(action: onCancel) {
                    Image(systemName: "xmark")
                        .font(.system(size: 14, weight: .bold))
                        .foregroundStyle(AppTheme.muted)
                        .frame(width: 36, height: 36)
                        .background(AppTheme.surface2, in: Circle())
                }
                .buttonStyle(.plain)
                .disabled(isSaving)
            }

            VStack(alignment: .leading, spacing: 7) {
                Text("Время заселения")
                    .font(AppTheme.font(12, .extrabold))
                    .foregroundStyle(AppTheme.muted)
                TextField("23:00", text: $checkIn)
                    .font(AppTheme.font(17, .bold))
                    .keyboardType(.numbersAndPunctuation)
                    .textInputAutocapitalization(.never)
                    .padding(.horizontal, 13)
                    .frame(height: 52)
                    .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                    .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.purple, lineWidth: 1) }
            }

            if let errorMessage, !errorMessage.isEmpty {
                Text(errorMessage)
                    .font(AppTheme.font(12, .semibold))
                    .foregroundStyle(AppTheme.error)
            }

            HStack(spacing: 10) {
                Button("Отмена", action: onCancel)
                    .font(AppTheme.font(14, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .frame(maxWidth: .infinity)
                    .frame(height: 48)
                    .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                    .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }
                    .disabled(isSaving)

                Button(isSaving ? "Сохраняем…" : "Сохранить") {
                    onSave(checkIn)
                }
                .font(AppTheme.font(14, .extrabold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 14))
                .disabled(isSaving)
            }
        }
        .padding(18)
        .background(AppTheme.background)
        .presentationDetents([.height(300)])
        .presentationDragIndicator(.visible)
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
    let onEdit: (String) -> Void
    @State private var copied = false
    @State private var selectedDay = 1

    private var dayOptions: [(number: Int, title: String)] {
        if overview.sightDays.isEmpty { return [(1, overview.sights.first?.city.nonEmpty ?? overview.cities.first ?? "Город")] }
        return overview.sightDays.enumerated().map { index, day in (index + 1, day.title) }
    }

    private var visibleSights: [Sight] {
        let result = overview.sights.filter { $0.walkDay == selectedDay }
        return result.isEmpty && selectedDay == 1 ? overview.sights : result
    }

    private var city: String { visibleSights.first?.city.nonEmpty ?? dayOptions.first?.title ?? overview.cities.first ?? "Город" }
    private var pins: [NumberedMapPin] {
        visibleSights.compactMap { sight in
            guard let lat = sight.latitude, let lon = sight.longitude else { return nil }
            return NumberedMapPin(title: sight.name, coordinate: Coordinate(latitude: lat, longitude: lon))
        }
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 14) {
            Text("\(city.uppercased()) · ДЕНЬ \(selectedDay)")
                .font(AppTheme.font(12, .extrabold)).foregroundStyle(AppTheme.purple)
            HStack(spacing: 12) {
                VStack(spacing: 0) {
                    Text("\(selectedDay)").font(AppTheme.font(20, .extrabold))
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
                Menu {
                ForEach(Array(dayOptions.enumerated()), id: \.offset) { item in
                    let option = item.element
                    Button {
                        selectedDay = option.number
                        } label: {
                            if option.number == selectedDay {
                                Label("День \(option.number) · \(option.title)", systemImage: "checkmark")
                            } else {
                                Text("День \(option.number) · \(option.title)")
                            }
                        }
                    }
                } label: {
                    Image(systemName: "chevron.down")
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(AppTheme.purple)
                        .frame(width: 36, height: 36)
                        .background(AppTheme.lavender.opacity(0.35), in: RoundedRectangle(cornerRadius: 11))
                }
                .buttonStyle(.plain)
            }
            .padding(10)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
            .overlay { RoundedRectangle(cornerRadius: 20).stroke(AppTheme.border, lineWidth: 1) }

            VStack(spacing: 0) {
                NumberedTripMap(pins: pins).frame(height: 192)
                HStack {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(city.uppercased()) · ДЕНЬ \(selectedDay)").font(AppTheme.font(10, .extrabold)).foregroundStyle(AppTheme.purple)
                        Text("\(city) · \(visibleSights.count) мест").font(AppTheme.font(13, .semibold)).foregroundStyle(AppTheme.muted)
                    }
                    Spacer()
                    Button(action: copyRoute) {
                        Label(copied ? "Скопировано" : "Копировать", systemImage: copied ? "checkmark" : "doc.on.doc")
                            .font(AppTheme.font(13, .bold)).foregroundStyle(.white)
                            .padding(.horizontal, 13).frame(height: 40)
                            .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 11))
                    }.buttonStyle(.plain)
                }
                .padding(.horizontal, 13).frame(height: 54).background(AppTheme.surface)
            }
            .clipShape(RoundedRectangle(cornerRadius: 20))

            ForEach(visibleSights) { sight in
                AndroidSightCard(sight: sight, client: client, onEdit: { onEdit(sight.id) })
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }

    private func copyRoute() {
        let route = overview.sights
            .filter { $0.walkDay == selectedDay }
            .sorted { ($0.walkDay, $0.walkOrder) < ($1.walkDay, $1.walkOrder) }
            .map(\.name)
            .joined(separator: " → ")
        UIPasteboard.general.string = route.isEmpty ? city : "\(city): \(route)"
        withAnimation(.easeOut(duration: 0.15)) { copied = true }
        Task {
            try? await Task.sleep(for: .seconds(1.5))
            await MainActor.run { withAnimation(.easeOut(duration: 0.15)) { copied = false } }
        }
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
    let onEdit: (String?) -> Void
    let onStatusChange: (Restaurant, String) -> Void
    let savingRestaurantID: String?
    @State private var detailsRestaurant: Restaurant?

    @State private var selectedCity = "Все города"
    @State private var selectedStatus = "all"
    @State private var showFilters = false
    @State private var selectedType = "Ресторан"
    @State private var selectedFeatures = Set<String>()
    @State private var selectedPrice = ""
    @State private var minimumRating: Double? = nil

    private var cityOptions: [String] {
        var values = ["Все города"]
        for city in overview.cities + overview.restaurants.map(\.city) {
            let clean = city.trimmingCharacters(in: .whitespacesAndNewlines)
            if !clean.isEmpty && !values.contains(where: { iosFilterCityKey($0) == iosFilterCityKey(clean) }) {
                values.append(clean)
            }
        }
        return values
    }

    private var restaurantsInSelectedCity: [Restaurant] {
        overview.restaurants.filter { restaurant in
            selectedCity == "Все города" || iosFilterCityKey(restaurant.city) == iosFilterCityKey(selectedCity)
        }
    }

    private var visibleRestaurants: [Restaurant] {
        overview.restaurants.filter { restaurant in
            let note = restaurant.note.lowercased()
            let statusMatches = selectedStatus == "all" || iosRestaurantStatusKey(restaurant.status) == selectedStatus
            let cityMatches = selectedCity == "Все города" || iosFilterCityKey(restaurant.city) == iosFilterCityKey(selectedCity)
            let typeMatches: Bool
            switch selectedType {
            case "Бар": typeMatches = note.contains("бар") || note.contains("bar")
            case "Кафе": typeMatches = note.contains("кафе") || note.contains("cafe")
            default: typeMatches = true
            }
            let featureMatches = selectedFeatures.allSatisfy { feature in
                switch feature {
                case "priority": return restaurant.priority || note.contains("приоритет") || note.contains("priority")
                case "dog": return note.contains("с собакой") || note.contains("dog")
                case "reservation": return iosRestaurantStatusKey(restaurant.status) == "бронь" || note.contains("бронь") || note.contains("reserv")
                case "vegan": return note.contains("веган") || note.contains("vegan")
                default: return true
                }
            }
            let priceMatches = selectedPrice.isEmpty || restaurant.price == selectedPrice
            let ratingMatches = minimumRating == nil || (restaurant.rating ?? 0) >= (minimumRating ?? 0)
            return cityMatches && statusMatches && typeMatches && featureMatches && priceMatches && ratingMatches
        }
    }

    private var statusCounts: [String: Int] {
        [
            "all": restaurantsInSelectedCity.count,
            "бронь": restaurantsInSelectedCity.count { iosRestaurantStatusKey($0.status) == "бронь" },
            "хочу": restaurantsInSelectedCity.count { iosRestaurantStatusKey($0.status) == "хочу" },
            "были": restaurantsInSelectedCity.count { iosRestaurantStatusKey($0.status) == "были" },
        ]
    }

    private var filterCount: Int {
        (selectedType != "Ресторан" ? 1 : 0) +
            (!selectedPrice.isEmpty ? 1 : 0) +
            (minimumRating == nil ? 0 : 1) +
            selectedFeatures.count
    }

    private var pins: [NumberedMapPin] {
        let visibleCities = Set(visibleRestaurants.map { iosFilterCityKey($0.city) })
        return overview.cities.compactMap { city in
            guard selectedCity == "Все города" || visibleCities.contains(iosFilterCityKey(city)) else { return nil }
            return overview.cityCoordinates[city].map { NumberedMapPin(title: city, coordinate: $0) }
        }
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            FilterHeader(
                cities: cityOptions,
                selectedCity: $selectedCity,
                filterCount: filterCount,
                onFilters: { showFilters = true },
            )
            ScrollView(.horizontal, showsIndicators: false) {
                HStack(spacing: 8) {
                    CountChip("Все", count: statusCounts["all"] ?? 0, selected: selectedStatus == "all") { selectedStatus = "all" }
                    CountChip("Бронь", count: statusCounts["бронь"] ?? 0, selected: selectedStatus == "бронь") { selectedStatus = "бронь" }
                    CountChip("Хочу", count: statusCounts["хочу"] ?? 0, selected: selectedStatus == "хочу") { selectedStatus = "хочу" }
                    CountChip("Были", count: statusCounts["были"] ?? 0, selected: selectedStatus == "были") { selectedStatus = "были" }
                }
            }
            DashedAddButton(title: "Добавить ресторан", action: { onEdit(nil) })
            NumberedTripMap(pins: pins).frame(height: 146).clipShape(RoundedRectangle(cornerRadius: 20))
            if visibleRestaurants.isEmpty {
                AndroidEmptyCard(icon: "fork.knife", text: overview.restaurants.isEmpty ? "Рестораны пока не добавлены" : "Ничего не найдено")
            } else {
                ForEach(visibleRestaurants) { restaurant in
                    AndroidRestaurantCard(
                        restaurant: restaurant,
                        client: client,
                        onEdit: { onEdit(restaurant.id) },
                        onDetails: { detailsRestaurant = restaurant },
                        onStatusChange: { status in onStatusChange(restaurant, status) },
                        isSavingStatus: savingRestaurantID == restaurant.id,
                    )
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
        .sheet(isPresented: $showFilters) {
            AndroidRestaurantFilterSheet(
                initialType: selectedType,
                initialFeatures: selectedFeatures,
                initialPrice: selectedPrice,
                initialMinimumRating: minimumRating,
                onApply: { type, features, price, rating in
                    selectedType = type
                    selectedFeatures = features
                    selectedPrice = price
                    minimumRating = rating
                },
            )
        }
        .sheet(item: $detailsRestaurant) { restaurant in
            IOSRestaurantDetailsSheet(
                restaurant: restaurant,
                client: client,
                onClose: { detailsRestaurant = nil },
                onEdit: {
                    detailsRestaurant = nil
                    Task { @MainActor in
                        await Task.yield()
                        onEdit(restaurant.id)
                    }
                },
            )
        }
    }
}

private struct FilterHeader: View {
    let cities: [String]
    @Binding var selectedCity: String
    let filterCount: Int
    let onFilters: () -> Void

    var body: some View {
        HStack(spacing: 10) {
            Menu {
                ForEach(cities, id: \.self) { city in
                    Button {
                        selectedCity = city
                    } label: {
                        if city == selectedCity {
                            Label(city, systemImage: "checkmark")
                        } else {
                            Text(city)
                        }
                    }
                }
            } label: {
                HStack {
                    Image(systemName: "mappin.and.ellipse")
                    Text(selectedCity).lineLimit(1)
                    Spacer()
                    Image(systemName: "chevron.down")
                }
                .font(AppTheme.font(15, .extrabold)).foregroundStyle(.white)
                .padding(.horizontal, 14).frame(height: 43)
                .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 14))
            }
            .buttonStyle(.plain)
            .frame(maxWidth: .infinity)
            Button(action: onFilters) {
                HStack(spacing: 9) {
                    Image(systemName: "line.3.horizontal.decrease")
                    Text("Фильтры")
                    Text("\(filterCount)").foregroundStyle(.white).frame(width: 27, height: 27).background(AppTheme.purple, in: Circle())
                }
                .font(AppTheme.font(15, .extrabold)).foregroundStyle(AppTheme.ink)
                .padding(.horizontal, 12).frame(height: 43)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }
            }
            .buttonStyle(.plain)
            .frame(maxWidth: .infinity)
            .accessibilityIdentifier("restaurants.filters")
        }
    }
}

private struct CountChip: View {
    let title: String
    let count: Int
    let selected: Bool
    let action: () -> Void

    init(_ title: String, count: Int, selected: Bool = false, action: @escaping () -> Void = {}) {
        self.title = title
        self.count = count
        self.selected = selected
        self.action = action
    }

    var body: some View {
        Button(action: action) {
            Text("\(title) · \(count)")
                .font(AppTheme.font(12, .bold)).foregroundStyle(selected ? .white : AppTheme.ink)
                .padding(.horizontal, 13).frame(height: 31)
                .background(selected ? AppTheme.purple : AppTheme.surface2, in: Capsule())
                .overlay { if !selected { Capsule().stroke(AppTheme.border, lineWidth: 1) } }
        }
        .buttonStyle(.plain)
    }
}

private struct AndroidRestaurantFilterSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var type: String
    @State private var features: Set<String>
    @State private var price: String
    @State private var rating: Double?
    let onApply: (String, Set<String>, String, Double?) -> Void

    init(
        initialType: String,
        initialFeatures: Set<String>,
        initialPrice: String,
        initialMinimumRating: Double?,
        onApply: @escaping (String, Set<String>, String, Double?) -> Void,
    ) {
        _type = State(initialValue: initialType)
        _features = State(initialValue: initialFeatures)
        _price = State(initialValue: initialPrice)
        _rating = State(initialValue: initialMinimumRating)
        self.onApply = onApply
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                HStack(alignment: .firstTextBaseline) {
                    Text("Фильтры")
                        .font(AppTheme.font(22, .extrabold))
                    Spacer()
                    Button("Сбросить") {
                        type = "Ресторан"
                        features = []
                        price = ""
                        rating = nil
                    }
                    .font(AppTheme.font(14, .extrabold))
                    .foregroundStyle(AppTheme.purple)
                }

                filterSection("Тип заведения") {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                        restaurantTypeChoice("Ресторан", icon: "fork.knife") { type = "Ресторан" }
                        restaurantTypeChoice("Бар", icon: "wineglass") { type = "Бар" }
                        restaurantTypeChoice("Кафе", icon: "cup.and.saucer") { type = "Кафе" }
                    }
                }
                filterSection("Особенности") {
                    LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 8) {
                        featureChoice("Приоритет", key: "priority", icon: "star")
                        featureChoice("С собакой", key: "dog", icon: "pawprint")
                        featureChoice("Есть бронь", key: "reservation", icon: "calendar.badge.checkmark")
                        featureChoice("Веган", key: "vegan", icon: "leaf")
                    }
                }
                filterSection("Средний чек") {
                    HStack(spacing: 4) {
                        ForEach(["", "€", "€€", "€€€", "€€€€"], id: \.self) { value in
                            restaurantSegment(value.isEmpty ? "Любой" : value, selected: price == value) { price = value }
                        }
                    }
                    .padding(4)
                    .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 14))
                }
                filterSection("Рейтинг от") {
                    HStack(spacing: 4) {
                        restaurantSegment("Любой", selected: rating == nil) { rating = nil }
                        restaurantSegment("4.0+", selected: rating == 4) { rating = 4 }
                        restaurantSegment("4.5+", selected: rating == 4.5) { rating = 4.5 }
                        restaurantSegment("4.8+", selected: rating == 4.8) { rating = 4.8 }
                    }
                    .padding(4)
                    .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 14))
                }
                Button("Показать результаты") {
                    onApply(type, features, price, rating)
                    dismiss()
                }
                .font(AppTheme.font(15, .extrabold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity).frame(height: 56)
                .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 16))
                .buttonStyle(.plain)
            }
            .padding(18)
        }
        .background(AppTheme.background)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    @ViewBuilder
    private func filterSection<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(title).font(AppTheme.font(14, .extrabold)).foregroundStyle(AppTheme.muted)
            content()
        }
    }

    private func restaurantTypeChoice(_ title: String, icon: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(spacing: 5) {
                Image(systemName: icon).font(.system(size: 19, weight: .semibold))
                Text(title).font(AppTheme.font(12, .bold)).lineLimit(1).minimumScaleFactor(0.75)
            }
            .foregroundStyle(type == title ? AppTheme.purple : AppTheme.ink)
            .frame(maxWidth: .infinity).frame(height: 68)
            .background(type == title ? AppTheme.lavender.opacity(0.7) : AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
            .overlay { RoundedRectangle(cornerRadius: 14).stroke(type == title ? AppTheme.purple : AppTheme.border, lineWidth: 1) }
        }
        .buttonStyle(.plain)
    }

    private func featureChoice(_ title: String, key: String, icon: String) -> some View {
        Button {
            if features.contains(key) { features.remove(key) } else { features.insert(key) }
        } label: {
            HStack(spacing: 8) {
                Image(systemName: icon).font(.system(size: 16, weight: .semibold))
                    .foregroundStyle(features.contains(key) ? AppTheme.purple : AppTheme.muted)
                    .frame(width: 24)
                Text(title).font(AppTheme.font(12, .bold)).lineLimit(1).minimumScaleFactor(0.75)
                Spacer(minLength: 2)
                Image(systemName: features.contains(key) ? "checkmark.circle.fill" : "circle")
                    .foregroundStyle(features.contains(key) ? AppTheme.purple : AppTheme.border)
            }
            .foregroundStyle(AppTheme.ink)
            .padding(.horizontal, 10).frame(maxWidth: .infinity).frame(height: 50)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
            .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }
        }
        .buttonStyle(.plain)
    }

    private func restaurantSegment(_ title: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Text(title).font(AppTheme.font(12, .bold)).lineLimit(1).minimumScaleFactor(0.7)
                .foregroundStyle(selected ? AppTheme.ink : AppTheme.muted)
                .frame(maxWidth: .infinity).frame(height: 38)
                .background(selected ? AppTheme.surface : .clear, in: RoundedRectangle(cornerRadius: 10))
        }
        .buttonStyle(.plain)
    }
}

private struct AndroidRestaurantCard: View {
    let restaurant: Restaurant
    let client: SupabaseClient
    let onEdit: () -> Void
    let onDetails: () -> Void
    let onStatusChange: (String) -> Void
    let isSavingStatus: Bool
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
                    Button { openRestaurantLink() } label: {
                        Image(systemName: "arrow.up.right.square").font(.system(size: 18, weight: .semibold)).foregroundStyle(AppTheme.purple)
                    }.buttonStyle(.plain)
                }
                AndroidIconButton(icon: "pencil", action: onEdit)
            }
            Divider().overlay(AppTheme.border)
            HStack {
                Text(restaurant.reviews.isEmpty ? "Нет отзывов" : "\(restaurant.reviews) отзывов")
                    .font(AppTheme.font(12, .bold)).foregroundStyle(AppTheme.muted)
                Spacer()
                Button {
                    onStatusChange(nextStatus)
                } label: {
                    Text(isSavingStatus ? "Сохраняем…" : statusActionTitle)
                        .font(AppTheme.font(13, .extrabold)).foregroundStyle(AppTheme.purple)
                }
                .buttonStyle(.plain)
                .disabled(isSavingStatus)
            }
            HStack(spacing: 9) {
                AndroidOutlineAction(title: "Маршрут", action: openRestaurantLink)
                AndroidFilledAction(title: "Подробнее", action: onDetails)
            }
        }
        .padding(12)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
    }

    private func openRestaurantLink() {
        guard let url = iosPlaceURL(name: restaurant.name, city: restaurant.city, link: restaurant.link, latitude: nil, longitude: nil) else { return }
        openURL(url)
    }

    private var normalizedStatus: String { iosRestaurantStatusKey(restaurant.status) }

    private var nextStatus: String {
        switch normalizedStatus {
        case "хочу": return "бронь"
        case "бронь": return "были"
        default: return "хочу"
        }
    }

    private var statusActionTitle: String {
        switch normalizedStatus {
        case "бронь": return "Отметить посещённым"
        case "были": return "Запланировать снова"
        default: return "Забронировать"
        }
    }
}

private struct IOSRestaurantDetailsSheet: View {
    let restaurant: Restaurant
    let client: SupabaseClient
    let onClose: () -> Void
    let onEdit: () -> Void
    @Environment(\.openURL) private var openURL

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 14) {
                HStack(spacing: 10) {
                    Text("Подробнее")
                        .font(AppTheme.font(23, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                    Spacer()
                    AndroidIconButton(icon: "pencil", action: onEdit)
                    AndroidIconButton(icon: "xmark", action: onClose)
                }

                RemotePhotoView(
                    reference: restaurant.photos.first,
                    client: client,
                    contentMode: .fill,
                    cornerRadius: 20,
                )
                .frame(maxWidth: .infinity)
                .frame(height: 196)

                Text(restaurant.name)
                    .font(AppTheme.font(24, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                Text(restaurant.city.isEmpty ? "Город не указан" : restaurant.city)
                    .font(AppTheme.font(14, .bold))
                    .foregroundStyle(AppTheme.muted)

                HStack(spacing: 8) {
                    if let rating = restaurant.rating {
                        Text("★ \(rating, specifier: "%.1f")")
                            .foregroundStyle(Color(hex: 0xD7942D))
                    }
                    if !restaurant.reviews.isEmpty {
                        Text(restaurant.reviews)
                            .foregroundStyle(AppTheme.muted)
                    }
                    if !restaurant.price.isEmpty {
                        Text(restaurant.price)
                            .foregroundStyle(AppTheme.purple)
                    }
                }
                .font(AppTheme.font(13, .bold))

                HStack(spacing: 9) {
                    Image(systemName: "calendar")
                        .foregroundStyle(AppTheme.purple)
                    Text(restaurantStatusTitle)
                        .font(AppTheme.font(14, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                    Spacer()
                    if !restaurant.date.isEmpty {
                        Text(restaurant.date)
                            .font(AppTheme.font(12, .bold))
                            .foregroundStyle(AppTheme.muted)
                    }
                }
                .padding(.horizontal, 14)
                .frame(minHeight: 48)
                .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 13))

                if !restaurant.note.isEmpty && !restaurant.note.localizedCaseInsensitiveContains("http") {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("Кухня и заметка")
                            .font(AppTheme.font(12, .extrabold))
                            .foregroundStyle(AppTheme.muted)
                        Text(restaurant.note)
                            .font(AppTheme.font(14, .semibold))
                            .foregroundStyle(AppTheme.ink)
                    }
                }

                if let url = iosPlaceURL(name: restaurant.name, city: restaurant.city, link: restaurant.link, latitude: nil, longitude: nil) {
                    Button {
                        openURL(url)
                    } label: {
                        Label("Открыть маршрут", systemImage: "arrow.up.right.square")
                            .font(AppTheme.font(14, .extrabold))
                            .foregroundStyle(AppTheme.ink)
                            .frame(maxWidth: .infinity)
                            .frame(height: 46)
                            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 13))
                            .overlay { RoundedRectangle(cornerRadius: 13).stroke(AppTheme.border, lineWidth: 1) }
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 18)
            .padding(.top, 10)
            .padding(.bottom, 28)
        }
        .background(AppTheme.background.ignoresSafeArea())
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private var restaurantStatusTitle: String {
        switch iosRestaurantStatusKey(restaurant.status) {
        case "бронь": return "Бронь подтверждена"
        case "были": return "Посещено"
        default: return "Запланировано"
        }
    }
}

private struct RouteAddDraft {
    var from = ""
    var to = ""
    var date = ""
    var checkIn = ""
    var checkOut = ""
    var notes = ""
    var mapsURL = ""
    var distance = ""
    var travelTime = ""
}

private struct RouteAddEditorSheet: View {
    let isSaving: Bool
    let errorMessage: String?
    let onCancel: () -> Void
    let onSave: (RouteAddDraft) -> Void
    @State private var draft = RouteAddDraft()

    var body: some View {
        NavigationStack {
            Form {
                Section("Города") {
                    TextField("Откуда", text: $draft.from)
                    TextField("Куда", text: $draft.to)
                }
                Section("Дата и время") {
                    TextField("Дата", text: $draft.date)
                    TextField("Заселение", text: $draft.checkIn)
                    TextField("Выселение", text: $draft.checkOut)
                }
                Section("Детали") {
                    TextField("Расстояние", text: $draft.distance)
                    TextField("Время в пути", text: $draft.travelTime)
                    TextField("Ссылка на карту", text: $draft.mapsURL)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                    TextField("Заметки", text: $draft.notes, axis: .vertical)
                        .lineLimit(3...6)
                }
                if let errorMessage, !errorMessage.isEmpty {
                    Section { Text(errorMessage).foregroundStyle(AppTheme.error) }
                }
            }
            .navigationTitle("Новый переезд")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена", action: onCancel).disabled(isSaving)
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button(isSaving ? "Сохраняем…" : "Сохранить") {
                        onSave(draft)
                    }
                    .disabled(isSaving)
                }
            }
        }
    }
}

private struct AndroidAccommodationScreen: View {
    let overview: TripOverview
    let client: SupabaseClient
    let onEdit: (String) -> Void
    let onAdd: () -> Void

    var body: some View {
        LazyVStack(spacing: 14) {
            if overview.accommodations.isEmpty {
                AndroidEmptyCard(icon: "bed.double", text: "Добавьте жильё для города поездки")
            } else {
                ForEach(overview.accommodations) { accommodation in
                    AndroidAccommodationCard(
                        accommodation: accommodation,
                        client: client,
                        onEdit: { onEdit(accommodation.id) },
                    )
                }
            }
            if overview.canEdit {
                DashedAddButton(title: "Добавить жильё", action: onAdd)
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct IOSAccommodationAddChoiceSheet: View {
    @Environment(\.dismiss) private var dismiss
    let onManual: () -> Void
    let onCatalog: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            HStack(alignment: .firstTextBaseline) {
                VStack(alignment: .leading, spacing: 3) {
                    Text("Добавить жильё")
                        .font(AppTheme.font(23, .extrabold))
                    Text("Выберите способ добавления")
                        .font(AppTheme.font(12, .semibold))
                        .foregroundStyle(AppTheme.muted)
                }
                Spacer()
                Button {
                    dismiss()
                } label: {
                    Image(systemName: "xmark")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(AppTheme.muted)
                        .frame(width: 36, height: 36)
                        .background(AppTheme.surface2, in: Circle())
                }
                .buttonStyle(.plain)
            }

            accommodationChoice(
                icon: "bed.double",
                title: "Найти в каталоге",
                subtitle: "Поиск по городу, названию или типу жилья",
                action: {
                    dismiss()
                    onCatalog()
                },
            )
            accommodationChoice(
                icon: "plus",
                title: "Добавить вручную",
                subtitle: "Сохранить свою бронь, даты, цену и ссылку",
                action: {
                    dismiss()
                    onManual()
                },
            )
        }
        .padding(18)
        .background(AppTheme.background)
        .presentationDetents([.height(260)])
        .presentationDragIndicator(.visible)
    }

    private func accommodationChoice(icon: String, title: String, subtitle: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 13) {
                Image(systemName: icon)
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(AppTheme.purple)
                    .frame(width: 45, height: 45)
                    .background(AppTheme.lavender.opacity(0.6), in: RoundedRectangle(cornerRadius: 14))
                VStack(alignment: .leading, spacing: 3) {
                    Text(title)
                        .font(AppTheme.font(15, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                    Text(subtitle)
                        .font(AppTheme.font(12, .semibold))
                        .foregroundStyle(AppTheme.muted)
                        .multilineTextAlignment(.leading)
                }
                Spacer()
                Image(systemName: "chevron.right")
                    .foregroundStyle(AppTheme.muted)
            }
            .padding(13)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 17))
            .overlay { RoundedRectangle(cornerRadius: 17).stroke(AppTheme.border, lineWidth: 1) }
        }
        .buttonStyle(.plain)
    }
}

private struct AndroidAccommodationCard: View {
    let accommodation: Accommodation
    let client: SupabaseClient
    let onEdit: () -> Void
    @EnvironmentObject private var model: AppModel
    @Environment(\.openURL) private var openURL
    @State private var photoIndex = 0

    private var link: String { accommodation.bookingURL.nonEmpty ?? accommodation.externalURL.nonEmpty ?? accommodation.website }
    private var photoReferences: [String] {
        var values = accommodation.photos.filter { !$0.isEmpty }
        if values.isEmpty, let reference = accommodation.photoReference.nonEmpty { values = [reference] }
        return values
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            ZStack(alignment: .bottom) {
                RemotePhotoView(
                    reference: photoReferences.indices.contains(photoIndex) ? photoReferences[photoIndex] : nil,
                    client: client,
                    contentMode: .fill,
                    cornerRadius: 0,
                )
                    .frame(maxWidth: .infinity)
                    .frame(height: 210)
                    .clipped()
                HStack {
                    carouselButton("arrow.left", delta: -1)
                    Spacer()
                    carouselButton("arrow.right", delta: 1)
                }
                .padding(.horizontal, 10).padding(.bottom, 87)
                Text("\(photoReferences.isEmpty ? 1 : photoIndex + 1)/\(max(photoReferences.count, 1))")
                    .font(AppTheme.font(11, .extrabold)).foregroundStyle(.white)
                    .padding(.horizontal, 10).frame(height: 27).background(.black.opacity(0.62), in: Capsule())
                    .padding(.bottom, 10)
            }
            .frame(maxWidth: .infinity)
            .frame(height: 210)
            .clipped()

            VStack(alignment: .leading, spacing: 0) {
                HStack(alignment: .firstTextBaseline) {
                    Text(accommodation.name)
                        .font(AppTheme.font(16, .extrabold))
                        .lineLimit(1)
                        .frame(maxWidth: .infinity, alignment: .leading)
                    if !accommodation.price.isEmpty {
                        Text(accommodation.price)
                            .font(AppTheme.font(15, .extrabold))
                            .foregroundStyle(AppTheme.purple)
                            .lineLimit(1)
                    }
                }
                Text("\(cityFlag(accommodation.city)) \(accommodation.city)")
                    .font(AppTheme.font(12, .semibold))
                    .foregroundStyle(AppTheme.muted)
                    .lineLimit(1)
                    .padding(.top, 4)
                if !accommodation.dates.isEmpty {
                    Label(formatAccommodationDatesIOS(accommodation.dates), systemImage: "calendar")
                        .font(AppTheme.font(12.5, .bold))
                        .foregroundStyle(AppTheme.ink)
                        .lineLimit(1)
                        .padding(.top, 7)
                }
                if let rating = accommodation.rating {
                    Text("★  \(rating, specifier: "%.1f") · \(formatReviews(accommodation.reviewCount)) отзывов")
                        .font(AppTheme.font(11, .semibold))
                        .foregroundStyle(AppTheme.muted)
                        .padding(.top, 11)
                }
                if !accommodation.deadline.isEmpty || !accommodation.paymentDeadline.isEmpty {
                    IOSAccommodationDeadlineCard(
                        deadline: accommodation.deadline,
                        paymentDeadline: accommodation.paymentDeadline,
                        remindersEnabled: model.profile.notificationsEnabled,
                    )
                    .padding(.top, 10)
                }
                HStack(spacing: 9) {
                    AndroidOutlineAction(title: "Редактировать", icon: "pencil", action: onEdit)
                    AndroidOutlineAction(title: "Открыть ссылку", icon: "arrow.up.right.square", action: openLink)
                }
                .padding(.top, accommodation.deadline.isEmpty && accommodation.paymentDeadline.isEmpty ? 12 : 15)
            }
            .padding(.horizontal, 15)
            .padding(.top, 13)
            .padding(.bottom, 15)
            .background(AppTheme.surface)
        }
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .clipShape(RoundedRectangle(cornerRadius: 20))
    }

    private func carouselButton(_ icon: String, delta: Int) -> some View {
        Button {
            guard !photoReferences.isEmpty else { return }
            photoIndex = (photoIndex + delta + photoReferences.count) % photoReferences.count
        } label: {
            Image(systemName: icon).font(.system(size: 19, weight: .semibold)).foregroundStyle(.white)
                .frame(width: 36, height: 36).background(.black.opacity(0.55), in: Circle())
        }
        .buttonStyle(.plain)
    }

    private func openLink() {
        guard let url = URL(string: link), !link.isEmpty else { return }
        openURL(url)
    }
}

private struct IOSAccommodationDeadlineCard: View {
    let deadline: String
    let paymentDeadline: String
    let remindersEnabled: Bool

    private var daysRemaining: Int? {
        guard let date = accommodationDateIOS(deadline) else { return nil }
        let calendar = Calendar.current
        return calendar.dateComponents(
            [.day],
            from: calendar.startOfDay(for: Date()),
            to: calendar.startOfDay(for: date),
        ).day
    }

    private var accent: Color {
        guard let daysRemaining else { return AppTheme.success }
        if daysRemaining < 0 { return AppTheme.error }
        if daysRemaining <= 3 { return AppTheme.warning }
        return AppTheme.success
    }

    var body: some View {
        VStack(spacing: 8) {
            if !deadline.isEmpty {
                HStack(spacing: 10) {
                    Text(daysRemaining.map { $0 >= 0 ? "✓" : "!" } ?? "✓")
                        .font(AppTheme.font(19, .extrabold))
                        .foregroundStyle(accent)
                        .frame(width: 23)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Бесплатная отмена")
                            .font(AppTheme.font(12.5, .extrabold))
                            .foregroundStyle(accent)
                        Text("до \(formatAccommodationDeadlineDetailIOS(deadline))")
                            .font(AppTheme.font(9.5, .semibold))
                            .foregroundStyle(AppTheme.muted)
                            .lineLimit(2)
                    }
                    Spacer(minLength: 8)
                    if let daysRemaining {
                        VStack(alignment: .trailing, spacing: 0) {
                            Text(daysRemaining >= 0 ? "\(daysRemaining)" : "—")
                                .font(AppTheme.font(21, .extrabold))
                                .foregroundStyle(daysRemaining >= 0 ? AppTheme.purple : accent)
                            Text(daysRemaining >= 0 ? "дней осталось" : "истёк")
                                .font(AppTheme.font(8.5, .extrabold))
                                .foregroundStyle(AppTheme.muted)
                        }
                    }
                }
            }

            if !deadline.isEmpty && !paymentDeadline.isEmpty {
                Rectangle()
                    .fill(accent.opacity(0.25))
                    .frame(height: 1)
                    .padding(.leading, 23)
            }

            if !paymentDeadline.isEmpty {
                HStack(spacing: 10) {
                    Image(systemName: "calendar")
                        .font(.system(size: 20, weight: .semibold))
                        .foregroundStyle(AppTheme.purple)
                        .frame(width: 23)
                    VStack(alignment: .leading, spacing: 2) {
                        Text("Оплатить до")
                            .font(AppTheme.font(12.5, .extrabold))
                            .foregroundStyle(AppTheme.purple)
                        Text(formatAccommodationDeadlineDetailIOS(paymentDeadline))
                            .font(AppTheme.font(9.5, .semibold))
                            .foregroundStyle(AppTheme.muted)
                            .lineLimit(2)
                    }
                    Spacer(minLength: 6)
                    Text(remindersEnabled ? "✓ Напоминания включены" : "Напоминания выключены")
                        .font(AppTheme.font(8.5, .extrabold))
                        .foregroundStyle(remindersEnabled ? AppTheme.success : AppTheme.muted)
                        .multilineTextAlignment(.trailing)
                        .lineLimit(2)
                }
            }
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 9)
        .background(accent.opacity(0.08), in: RoundedRectangle(cornerRadius: 14))
        .overlay { RoundedRectangle(cornerRadius: 14).stroke(accent.opacity(0.28), lineWidth: 1) }
    }
}

private struct AndroidPetsScreen: View {
    @EnvironmentObject private var model: AppModel
    let overview: TripOverview
    let client: SupabaseClient
    let onEdit: () -> Void
    let onPetChanged: () -> Void
    @State private var selectedType = "shop"
    @State private var searchText = ""
    @State private var selectedCity = "Все города"
    @State private var showFilters = false
    @State private var selectedRadius = 10.0
    @State private var minimumRating: Double? = nil
    @State private var openNowOnly = false
    @State private var selectedFeatures = Set<String>()
    @State private var catalogEntries: [CatalogEntry] = []
    @State private var isLoadingCatalog = false
    @State private var catalogMessage: String?
    @State private var addingCatalogIDs = Set<String>()

    private var cityOptions: [String] {
        var values = ["Все города"]
        let sourceCities = overview.cities +
            overview.routeLegs.flatMap { [$0.from, $0.to] } +
            overview.petPlaces.map(\.city)
        for rawCity in sourceCities {
            for city in rawCity.split(separator: ",") {
                let clean = city.trimmingCharacters(in: .whitespacesAndNewlines)
                if !clean.isEmpty && !values.contains(where: { iosFilterCityKey($0) == iosFilterCityKey(clean) }) {
                    values.append(clean)
                }
            }
        }
        return values
    }

    private var filterCount: Int {
        (selectedRadius == 10 ? 0 : 1) + (minimumRating == nil ? 0 : 1) + (openNowOnly ? 1 : 0) + selectedFeatures.count
    }

    private var catalogTaskKey: String {
        "\(selectedType)|\(selectedCity)|\(searchText.trimmingCharacters(in: .whitespacesAndNewlines))"
    }

    private func distanceFromCity(_ city: String, latitude: Double?, longitude: Double?) -> Double? {
        guard
            let origin = overview.cityCoordinates.first(where: { iosFilterCityKey($0.key) == iosFilterCityKey(city) })?.value,
            let latitude,
            let longitude
        else { return nil }
        let earthRadius = 6_371.0
        let latitudeDelta = (latitude - origin.latitude) * .pi / 180
        let longitudeDelta = (longitude - origin.longitude) * .pi / 180
        let originLatitude = origin.latitude * .pi / 180
        let placeLatitude = latitude * .pi / 180
        let haversine = sin(latitudeDelta / 2) * sin(latitudeDelta / 2) +
            cos(originLatitude) * cos(placeLatitude) * sin(longitudeDelta / 2) * sin(longitudeDelta / 2)
        return earthRadius * 2 * atan2(sqrt(haversine), sqrt(max(0, 1 - haversine)))
    }

    private func withinRadius(city: String, latitude: Double?, longitude: Double?) -> Bool {
        guard let distance = distanceFromCity(city, latitude: latitude, longitude: longitude) else { return true }
        return distance <= selectedRadius
    }

    private var visiblePets: [PetPlace] {
        overview.petPlaces.filter { pet in
            let isVet = iosPetIsVet(pet.type)
            let matchesType = selectedType == "vet" ? isVet : !isVet
            let query = searchText.trimmingCharacters(in: .whitespacesAndNewlines)
            let matchesCity = selectedCity == "Все города" || iosFilterCityKey(pet.city) == iosFilterCityKey(selectedCity)
            let matchesRadius = withinRadius(city: pet.city, latitude: pet.latitude, longitude: pet.longitude)
            let matchesRating = minimumRating == nil || (pet.rating ?? 0) >= (minimumRating ?? 0)
            let matchesOpen = !openNowOnly || pet.openNow == true
            let petText = ([pet.type, pet.note] + pet.features).joined(separator: " ").lowercased()
            let matchesFeatures = selectedFeatures.allSatisfy { feature in
                switch feature {
                case "phone": return !pet.phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                case "24/7": return petText.contains("24") || petText.contains("кругл") || petText.contains("24/7")
                case "emergency": return petText.contains("экстр") || petText.contains("emergency")
                case "walk-in": return petText.contains("без записи") || petText.contains("walk-in") || petText.contains("walk in")
                case "dog": return petText.contains("собак") || petText.contains("dog")
                case "cat": return petText.contains("кош") || petText.contains("cat")
                case "medicine": return petText.contains("лекар") || petText.contains("medic")
                case "grooming": return petText.contains("грум") || petText.contains("groom")
                default: return true
                }
            }
            return matchesType && matchesCity && matchesRadius && matchesRating && matchesOpen && matchesFeatures &&
                (query.isEmpty || pet.name.localizedCaseInsensitiveContains(query) || pet.address.localizedCaseInsensitiveContains(query))
        }
    }

    private var visibleCatalog: [CatalogEntry] {
        catalogEntries.filter { entry in
            let text = [entry.category, entry.description, entry.name].joined(separator: " ").lowercased()
            let cityMatches = selectedCity == "Все города" || iosFilterCityKey(entry.city) == iosFilterCityKey(selectedCity)
            let radiusMatches = withinRadius(city: entry.city, latitude: entry.latitude, longitude: entry.longitude)
            let ratingMatches = minimumRating == nil || (entry.rating ?? 0) >= (minimumRating ?? 0)
            let openMatches = !openNowOnly || entry.openNow == true
            let featureMatches = selectedFeatures.allSatisfy { feature in
                switch feature {
                case "phone": return !entry.phone.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                case "24/7": return text.contains("24") || text.contains("кругл") || text.contains("emergency")
                case "emergency": return text.contains("экстр") || text.contains("emergency")
                case "walk-in": return text.contains("без записи") || text.contains("walk-in") || text.contains("walk in")
                case "dog": return text.contains("собак") || text.contains("dog")
                case "cat": return text.contains("кош") || text.contains("cat")
                case "medicine": return text.contains("лекар") || text.contains("medic")
                case "grooming": return text.contains("грум") || text.contains("groom")
                default: return true
                }
            }
            return cityMatches && radiusMatches && ratingMatches && openMatches && featureMatches
        }
    }

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 12) {
            FilterHeader(
                cities: cityOptions,
                selectedCity: $selectedCity,
                filterCount: filterCount,
                onFilters: { showFilters = true },
            )
            HStack(spacing: 0) {
                petTab("Зоомагазины", value: "shop")
                petTab("Ветеринары", value: "vet")
            }
            .padding(4).background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 14))

            HStack(spacing: 10) {
                Image(systemName: "magnifyingglass").foregroundStyle(AppTheme.muted)
                TextField("Поиск по каталогу", text: $searchText)
                    .font(AppTheme.font(15, .regular))
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                if !searchText.isEmpty {
                    Button { searchText = "" } label: {
                        Image(systemName: "xmark.circle.fill").foregroundStyle(AppTheme.muted)
                    }.buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 16).frame(height: 54)
            .background(AppTheme.surface.opacity(0.45), in: RoundedRectangle(cornerRadius: 14))
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

            if !visiblePets.isEmpty {
                Text("Мои места").font(AppTheme.font(19, .extrabold)).padding(.top, 4)
                ForEach(visiblePets) { pet in AndroidPetCard(pet: pet, client: client) }
            }
            Text("Из каталога").font(AppTheme.font(19, .extrabold)).padding(.top, 4)
            if isLoadingCatalog {
                ProgressView().tint(AppTheme.purple).frame(maxWidth: .infinity).padding(.vertical, 30)
            } else if visibleCatalog.isEmpty {
                AndroidEmptyCard(icon: "pawprint", text: catalogMessage ?? "В каталоге пока нет мест этого типа")
            } else {
                ForEach(visibleCatalog) { entry in
                    AndroidPetCatalogCard(
                        entry: entry,
                        client: client,
                        added: isCatalogEntryAdded(entry),
                        saving: addingCatalogIDs.contains(entry.id),
                        onMap: { openCatalogMap(entry) },
                        onAdd: { addCatalogEntry(entry) },
                    )
                }
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
        .sheet(isPresented: $showFilters) {
            AndroidPetFilterSheet(
                type: selectedType,
                initialRadius: selectedRadius,
                initialMinimumRating: minimumRating,
                initialOpenNow: openNowOnly,
                initialFeatures: selectedFeatures,
                onApply: { radius, rating, openNow, features in
                    selectedRadius = radius
                    minimumRating = rating
                    openNowOnly = openNow
                    selectedFeatures = features
                },
            )
        }
        .task(id: catalogTaskKey) { await loadCatalog() }
    }

    private func petTab(_ title: String, value: String) -> some View {
        Button { selectedType = value } label: {
            Text(title).font(AppTheme.font(14, .extrabold))
                .foregroundStyle(selectedType == value ? AppTheme.purple : Color(hex: 0x9999A3))
                .frame(maxWidth: .infinity).frame(height: 43)
                .background(selectedType == value ? AppTheme.surface : .clear, in: RoundedRectangle(cornerRadius: 12))
        }.buttonStyle(.plain)
    }

    private func loadCatalog() async {
        // Keep the first screen fast and match Android: search several cities
        // concurrently instead of waiting for every city one after another.
        let cities = selectedCity == "Все города"
            ? Array(cityOptions.dropFirst().prefix(6))
            : [selectedCity]
        catalogEntries = []
        catalogMessage = nil
        guard !cities.isEmpty else {
            isLoadingCatalog = false
            catalogMessage = "Добавьте город в маршрут, чтобы найти места для питомцев."
            return
        }
        isLoadingCatalog = true
        var loaded: [CatalogEntry] = []
        var errors: [String] = []

        await withTaskGroup(of: ([CatalogEntry], String?).self) { group in
            for city in cities {
                group.addTask {
                    do {
                        let entries = try await model.searchCatalog(
                            kind: .pet,
                            city: city,
                            query: searchText,
                            language: model.profile.language,
                            petType: selectedType,
                        )
                        return (entries, nil)
                    } catch {
                        return ([], error.localizedDescription)
                    }
                }
            }

            for await (entries, errorMessage) in group {
                loaded += entries
                if let errorMessage { errors.append(errorMessage) }

                var seen = Set<String>()
                let uniqueEntries = loaded.filter { seen.insert($0.id).inserted }
                if !uniqueEntries.isEmpty {
                    catalogEntries = uniqueEntries
                    isLoadingCatalog = false
                }
            }
        }

        if Task.isCancelled {
            isLoadingCatalog = false
            return
        }

        var seen = Set<String>()
        catalogEntries = loaded.filter { seen.insert($0.id).inserted }
        if catalogEntries.isEmpty, let firstError = errors.first {
            catalogMessage = firstError
        }
        isLoadingCatalog = false
    }

    private func isCatalogEntryAdded(_ entry: CatalogEntry) -> Bool {
        addingCatalogIDs.contains("added:\(entry.id)") || overview.petPlaces.contains {
            iosFilterCityKey($0.city) == iosFilterCityKey(entry.city) &&
                $0.name.localizedCaseInsensitiveCompare(entry.name) == .orderedSame
        }
    }

    private func addCatalogEntry(_ entry: CatalogEntry) {
        guard !isCatalogEntryAdded(entry), !addingCatalogIDs.contains(entry.id) else { return }
        addingCatalogIDs.insert(entry.id)
        Task {
            do {
                try await model.addCatalogItem(id: overview.id, entry: entry, walkDay: 1)
                addingCatalogIDs.remove(entry.id)
                addingCatalogIDs.insert("added:\(entry.id)")
                onPetChanged()
            } catch {
                addingCatalogIDs.remove(entry.id)
                catalogMessage = error.localizedDescription
            }
        }
    }

    private func openCatalogMap(_ entry: CatalogEntry) {
        guard let url = iosPlaceURL(name: entry.name, city: entry.city, link: entry.mapURL, latitude: entry.latitude, longitude: entry.longitude) else { return }
        UIApplication.shared.open(url)
    }
}

private struct AndroidPetFilterSheet: View {
    @Environment(\.dismiss) private var dismiss
    let type: String
    @State private var radius: Double
    @State private var rating: Double?
    @State private var openNow: Bool
    @State private var features: Set<String>
    let onApply: (Double, Double?, Bool, Set<String>) -> Void

    init(
        type: String,
        initialRadius: Double,
        initialMinimumRating: Double?,
        initialOpenNow: Bool,
        initialFeatures: Set<String>,
        onApply: @escaping (Double, Double?, Bool, Set<String>) -> Void,
    ) {
        self.type = type
        _radius = State(initialValue: initialRadius)
        _rating = State(initialValue: initialMinimumRating)
        _openNow = State(initialValue: initialOpenNow)
        _features = State(initialValue: initialFeatures)
        self.onApply = onApply
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                HStack(alignment: .firstTextBaseline) {
                    Text("Фильтры")
                        .font(AppTheme.font(22, .extrabold))
                    Spacer()
                    Button("Сбросить") {
                        radius = 10
                        rating = nil
                        openNow = false
                        features = []
                    }
                    .font(AppTheme.font(14, .extrabold))
                    .foregroundStyle(AppTheme.purple)
                }

                filterSection("Радиус поиска") {
                    HStack(spacing: 8) {
                        ForEach([1.0, 5.0, 10.0, 25.0], id: \.self) { value in
                            AndroidPetFilterChoice(
                                title: "\(Int(value)) км",
                                selected: radius == value,
                            ) { radius = value }
                        }
                    }
                }

                filterSection("Рейтинг от") {
                    HStack(spacing: 8) {
                        AndroidPetFilterChoice(title: "Любой", selected: rating == nil) { rating = nil }
                        AndroidPetFilterChoice(title: "★ 4.0", selected: rating == 4) { rating = 4 }
                        AndroidPetFilterChoice(title: "★ 4.5", selected: rating == 4.5) { rating = 4.5 }
                        AndroidPetFilterChoice(title: "★ 4.8", selected: rating == 4.8) { rating = 4.8 }
                    }
                }

                filterSection("Дополнительно") {
                    petToggleRow("Открыто сейчас", icon: "calendar", selected: openNow) {
                        openNow.toggle()
                    }
                    ForEach(featureOptions) { option in
                        petToggleRow(option.title, icon: option.icon, selected: features.contains(option.key)) {
                            toggle(option.key)
                        }
                    }
                }

                Button("Показать места") {
                    onApply(radius, rating, openNow, features)
                    dismiss()
                }
                .font(AppTheme.font(15, .extrabold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 16))
                .buttonStyle(.plain)
            }
            .padding(18)
        }
        .background(AppTheme.background)
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private var featureOptions: [AndroidPetFilterOption] {
        if type == "vet" {
            return [
                AndroidPetFilterOption(key: "24/7", title: "Круглосуточно", icon: "calendar"),
                AndroidPetFilterOption(key: "emergency", title: "Экстренная помощь", icon: "cross.case"),
                AndroidPetFilterOption(key: "walk-in", title: "Приём без записи", icon: "person.badge.clock"),
                AndroidPetFilterOption(key: "phone", title: "Есть телефон", icon: "phone"),
            ]
        }
        return [
            AndroidPetFilterOption(key: "dog", title: "Корм для собак", icon: "heart"),
            AndroidPetFilterOption(key: "cat", title: "Корм для кошек", icon: "heart"),
            AndroidPetFilterOption(key: "medicine", title: "Лекарства", icon: "cross.case"),
            AndroidPetFilterOption(key: "grooming", title: "Груминг", icon: "scissors"),
            AndroidPetFilterOption(key: "phone", title: "Есть телефон", icon: "phone"),
        ]
    }

    @ViewBuilder
    private func filterSection<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 9) {
            Text(title)
                .font(AppTheme.font(15, .extrabold))
                .foregroundStyle(AppTheme.ink)
            content()
        }
    }

    private func petToggleRow(_ title: String, icon: String, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(selected ? AppTheme.purple : AppTheme.muted)
                    .frame(width: 38, height: 38)
                    .background(AppTheme.lavender.opacity(selected ? 0.75 : 0.35), in: Circle())
                Text(title)
                    .font(AppTheme.font(15, .regular))
                    .foregroundStyle(AppTheme.ink)
                Spacer()
                Image(systemName: selected ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 25, weight: .regular))
                    .foregroundStyle(selected ? AppTheme.purple : AppTheme.border)
            }
            .padding(.horizontal, 14)
            .frame(height: 58)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 16))
            .overlay { RoundedRectangle(cornerRadius: 16).stroke(AppTheme.border, lineWidth: 1) }
        }
        .buttonStyle(.plain)
        .accessibilityLabel(title)
        .accessibilityValue(selected ? "Выбрано" : "Не выбрано")
    }

    private func toggle(_ key: String) {
        if features.contains(key) { features.remove(key) } else { features.insert(key) }
    }
}

private struct AndroidPetFilterOption: Identifiable {
    let key: String
    let title: String
    let icon: String

    var id: String { key }
}

private struct AndroidPetFilterChoice: View {
    let title: String
    let selected: Bool
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            Text(title)
                .font(AppTheme.font(13, .bold))
                .foregroundStyle(selected ? .white : AppTheme.ink)
                .lineLimit(1)
                .minimumScaleFactor(0.75)
                .frame(maxWidth: .infinity)
                .frame(height: 48)
                .background(selected ? AppTheme.purple : AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                .overlay { RoundedRectangle(cornerRadius: 14).stroke(selected ? AppTheme.purple : AppTheme.border, lineWidth: 1) }
        }
        .buttonStyle(.plain)
    }
}

private struct AndroidPetCatalogCard: View {
    let entry: CatalogEntry
    let client: SupabaseClient
    let added: Bool
    let saving: Bool
    let onMap: () -> Void
    let onAdd: () -> Void

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            HStack(alignment: .top, spacing: 11) {
                RemotePhotoView(reference: entry.photoReference, client: client, contentMode: .fill, cornerRadius: 12)
                    .frame(width: 91, height: 91)
                VStack(alignment: .leading, spacing: 5) {
                    Text(iosPetIsVet(entry.type) ? "ВЕТЕРИНАР" : "ЗООМАГАЗИН")
                        .font(AppTheme.font(9, .extrabold)).foregroundStyle(AppTheme.purple)
                    Text(entry.name).font(AppTheme.font(16, .extrabold)).lineLimit(2)
                    Text(entry.address).font(AppTheme.font(11, .semibold)).foregroundStyle(AppTheme.muted).lineLimit(1)
                    HStack(spacing: 7) {
                        if let rating = entry.rating {
                            Text("★ \(rating, specifier: "%.1f")")
                                .font(AppTheme.font(12, .extrabold)).foregroundStyle(AppTheme.purple)
                        }
                        if let reviewCount = entry.reviewCount {
                            Text("(\(formatReviews(reviewCount)))").font(AppTheme.font(11, .semibold)).foregroundStyle(AppTheme.muted)
                        }
                        if let openNow = entry.openNow {
                            Text(openNow ? "Открыто" : "Закрыто")
                                .font(AppTheme.font(11, .bold)).foregroundStyle(openNow ? AppTheme.success : AppTheme.error)
                                .padding(.horizontal, 8).frame(height: 29)
                                .overlay { RoundedRectangle(cornerRadius: 8).stroke(openNow ? AppTheme.success.opacity(0.5) : AppTheme.error.opacity(0.5)) }
                        }
                    }
                }
                Spacer(minLength: 0)
                VStack(spacing: 8) {
                    AndroidIconButton(icon: "mappin", action: onMap)
                    AndroidIconButton(icon: added ? "checkmark" : "plus", action: onAdd)
                        .opacity(saving ? 0.5 : 1)
                }
            }
            if let attribution = entry.photoAttribution?.nonEmpty {
                Text(attribution)
                    .font(AppTheme.font(10, .semibold)).foregroundStyle(AppTheme.muted).lineLimit(1)
            }
        }
        .padding(11)
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20))
        .overlay { RoundedRectangle(cornerRadius: 20).stroke(AppTheme.border.opacity(0.8), lineWidth: 1) }
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
                    Text(iosPetIsVet(pet.type) ? "ВЕТЕРИНАР" : "ЗООМАГАЗИН")
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
                AndroidIconButton(icon: "mappin", action: openMap)
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
        guard let url = iosPlaceURL(name: pet.name, city: pet.city, link: pet.mapsURL, latitude: pet.latitude, longitude: pet.longitude) else { return }
        openURL(url)
    }
}

private struct AndroidBudgetScreen: View {
    @EnvironmentObject private var model: AppModel
    let overview: TripOverview
    let exchangeRates: ExchangeRateSnapshot?
    let onRefresh: () -> Void
    @State private var selectedCurrencyCode: String?
    @State private var manualRateOverride: Double?
    @State private var showRateEditor = false
    @State private var rateInput = ""
    @State private var rateError: String?
    @State private var isSavingRate = false

    private var total: Double { overview.budgetExpenses.reduce(0) { $0 + displayedAmount($1) } }
    private var budgetCurrency: String { selectedCurrencyCode ?? overview.budgetCurrency }
    private var symbol: String { currencySymbol(budgetCurrency) }
    private var people: Int { max(max(overview.budgetGroups.reduce(0) { $0 + $1.people }, overview.members.count), 1) }
    private var days: Int { max(tripDayCount(overview.dates), 1) }
    private var rate: Double {
        let perRub = currencyPerRubRate(budgetCurrency)
        return perRub > 0 ? 1 / perRub : 1
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
                ? overview.budgetExpenses.filter { !used.contains($0.id) }.reduce(0) { $0 + displayedAmount($1) }
                : matches.reduce(0) { $0 + displayedAmount($1) }
            return (name, color, value)
        }
    }

    private var normalizedBudgetRate: Double {
        let code = budgetCurrency.uppercased()
        switch code {
        case "EUR": return 1
        case "RUB": return 100
        case "CZK": return 25
        default:
            let eurPerRub = currencyPerRubRate("EUR", includeOverride: false)
            guard eurPerRub > 0 else { return 1 }
            return currencyPerRubRate(code) / eurPerRub
        }
    }

    private func currencyPerRubRate(_ code: String, includeOverride: Bool = true) -> Double {
        let normalized = code.uppercased()
        if normalized == "RUB" { return 1 }
        if includeOverride, normalized == budgetCurrency.uppercased(), let manualRateOverride, manualRateOverride > 0 {
            return 1 / manualRateOverride
        }
        if let stored = overview.budgetManualRates[normalized], stored > 0 { return stored }
        if let online = exchangeRates?.rates[normalized], online > 0 { return online }
        return fallbackCurrencyPerRubRate(normalized)
    }

    private func displayedAmount(_ expense: BudgetExpense) -> Double {
        let inputValue = expense.inputCurrency.trimmingCharacters(in: .whitespacesAndNewlines)
        let inputCode = currencyCode(for: inputValue) ?? inputValue.uppercased()
        if let storedRate = expense.inputCurrencyRate, storedRate > 0, !inputCode.isEmpty {
            return expense.amount * (inputCode == budgetCurrency.uppercased() ? storedRate : currencyPerRubRate(budgetCurrency))
        }
        return expense.amount * normalizedBudgetRate
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
                    Button { selectCurrency(item) } label: {
                        Text(item).font(AppTheme.font(13, .extrabold))
                            .foregroundStyle(item == symbol ? AppTheme.ink : Color(hex: 0x9999A3))
                            .frame(maxWidth: .infinity).frame(height: 39)
                            .background(item == symbol ? AppTheme.surface : .clear, in: RoundedRectangle(cornerRadius: 11))
                    }
                    .buttonStyle(.plain)
                    .disabled(!overview.canEdit)
                }
            }
            .padding(4).background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 15))

            VStack(alignment: .leading, spacing: 12) {
                HStack {
                    Text("Курс валюты").font(AppTheme.font(12, .bold)).foregroundStyle(.white.opacity(0.72))
                    Spacer()
                    Text("вручную").font(AppTheme.font(11, .extrabold)).foregroundStyle(.white)
                        .padding(.horizontal, 10).frame(height: 30).background(.white.opacity(0.13), in: Capsule())
                    Button("Изменить") { openRateEditor() }
                        .font(AppTheme.font(12, .extrabold))
                        .foregroundStyle(.white)
                        .buttonStyle(.plain)
                        .disabled(!overview.canEdit || budgetCurrency.uppercased() == "RUB")
                }
                Text("1 \(budgetCurrency.uppercased()) = \(formatMoney(rate)) ₽")
                    .font(AppTheme.font(22, .extrabold)).foregroundStyle(.white)
                HStack {
                    Text("за 1 \(budgetCurrency.uppercased()) · Задано вручную")
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
        .sheet(isPresented: $showRateEditor) {
            NavigationStack {
                Form {
                    Section("Курс валюты") {
                        TextField("Рубли за единицу", text: $rateInput)
                            .keyboardType(.decimalPad)
                        if let rateError {
                            Text(rateError).font(.caption).foregroundStyle(AppTheme.error)
                        }
                    }
                }
                .navigationTitle("Изменить курс")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Отмена") { showRateEditor = false }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Сохранить") { saveRate() }.disabled(isSavingRate)
                    }
                }
            }
        }
    }

    private func selectCurrency(_ value: String) {
        guard overview.canEdit, let code = currencyCode(for: value), code != budgetCurrency.uppercased() else { return }
        selectedCurrencyCode = code
        Task {
            do {
                try await model.updateTripField(id: overview.id, key: "budgetCurrency", value: .string(code))
                await MainActor.run { onRefresh() }
            } catch {
                await MainActor.run { selectedCurrencyCode = nil }
            }
        }
    }

    private func openRateEditor() {
        rateError = nil
        rateInput = formatMoney(rate)
        showRateEditor = true
    }

    private func saveRate() {
        guard let value = Double(rateInput.replacingOccurrences(of: " ", with: "").replacingOccurrences(of: ",", with: ".")), value > 0,
              let code = budgetCurrency.uppercased().nonEmpty, code != "RUB" else {
            rateError = "Введите курс больше нуля"
            return
        }
        let storedValue = 1 / value
        var updated = overview.budgetManualRates
        updated[code] = storedValue
        isSavingRate = true
        rateError = nil
        Task {
            do {
                let payload = JSONValue.object(updated.mapValues { .number($0) })
                try await model.updateTripField(id: overview.id, key: "budgetManualRates", value: payload)
                await MainActor.run {
                    manualRateOverride = value
                    isSavingRate = false
                    showRateEditor = false
                    onRefresh()
                }
            } catch {
                await MainActor.run {
                    isSavingRate = false
                    rateError = error.localizedDescription
                }
            }
        }
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
    let onLeave: () -> Void

    var body: some View {
        LazyVStack(alignment: .leading, spacing: 10) {
            HStack {
                Text("Участники").font(AppTheme.font(21, .extrabold))
                Spacer()
                if overview.canEdit {
                    Button(action: onEdit) {
                        Label("Изменить", systemImage: "pencil")
                            .font(AppTheme.font(14, .extrabold)).foregroundStyle(AppTheme.purple)
                            .padding(.horizontal, 14).frame(height: 43)
                            .background(AppTheme.surface.opacity(0.7), in: RoundedRectangle(cornerRadius: 15))
                    }.buttonStyle(.plain)
                }
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
            if overview.canEdit {
                DashedAddButton(title: "Пригласить участника", action: onEdit).padding(.top, 8)
            }
            if !overview.currentUserRole.localizedCaseInsensitiveContains("владел") &&
                !overview.currentUserRole.localizedCaseInsensitiveContains("owner") {
                Button(action: onLeave) {
                    Label("Покинуть поездку", systemImage: "rectangle.portrait.and.arrow.right")
                        .font(AppTheme.font(14, .extrabold))
                        .foregroundStyle(AppTheme.error)
                        .frame(maxWidth: .infinity)
                        .frame(height: 44)
                        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 13))
                        .overlay { RoundedRectangle(cornerRadius: 13).stroke(AppTheme.error.opacity(0.25), lineWidth: 1) }
                }
                .buttonStyle(.plain)
                .padding(.top, 2)
            }
        }
        .padding(.horizontal, 18)
        .padding(.top, 24)
    }
}

private struct AndroidPhotosScreen: View {
    @EnvironmentObject private var model: AppModel
    let overview: TripOverview
    let client: SupabaseClient
    let onPhotosChanged: () -> Void
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var isUploading = false
    @State private var errorMessage: String?

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
                PhotosPicker(selection: $selectedPhoto, matching: .images) {
                    Group {
                        if isUploading {
                            ProgressView().tint(.white)
                        } else {
                            Label("Загрузить", systemImage: "plus")
                        }
                    }
                    .font(AppTheme.font(13, .extrabold)).foregroundStyle(.white)
                    .padding(.horizontal, 14).frame(height: 40)
                    .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 12))
                }
                .disabled(isUploading)
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
        .onChange(of: selectedPhoto) { _, item in
            guard let item else { return }
            Task { await importPhoto(item) }
        }
        .alert("Не удалось загрузить фото", isPresented: Binding(
            get: { errorMessage != nil },
            set: { if !$0 { errorMessage = nil } },
        )) {
            Button("ОК", role: .cancel) { errorMessage = nil }
        } message: {
            Text(errorMessage ?? "")
        }
    }

    private func importPhoto(_ item: PhotosPickerItem) async {
        isUploading = true
        defer {
            isUploading = false
            selectedPhoto = nil
        }
        do {
            guard let source = try await item.loadTransferable(type: Data.self),
                  let image = UIImage(data: source),
                  let data = image.jpegData(compressionQuality: 0.86)
            else {
                throw TripPhotoImportError.invalidPhoto
            }
            try await model.addCoverPhoto(id: overview.id, data: data, city: overview.cities.first ?? "")
            onPhotosChanged()
        } catch {
            errorMessage = error.localizedDescription
        }
    }
}

private enum TripPhotoImportError: LocalizedError {
    case invalidPhoto

    var errorDescription: String? { "Не удалось прочитать изображение." }
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

private struct NumberedTripMap: UIViewRepresentable {
    let pins: [NumberedMapPin]
    var routeCoordinates: [Coordinate] = []
    var showsUserLocation = false

    func makeCoordinator() -> Coordinator {
        Coordinator()
    }

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView(frame: .zero)
        map.delegate = context.coordinator
        map.showsCompass = false
        map.showsScale = false
        map.showsUserLocation = false
        map.pointOfInterestFilter = .excludingAll
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        map.removeAnnotations(map.annotations)
        map.removeOverlays(map.overlays)
        context.coordinator.mapView = map
        if showsUserLocation {
            switch context.coordinator.locationManager.authorizationStatus {
            case .authorizedAlways, .authorizedWhenInUse:
                map.showsUserLocation = true
            case .notDetermined:
                context.coordinator.locationManager.requestWhenInUseAuthorization()
            default:
                map.showsUserLocation = false
            }
        } else {
            map.showsUserLocation = false
        }

        if pins.isEmpty {
            map.setRegion(
                MKCoordinateRegion(
                    center: CLLocationCoordinate2D(latitude: 45.7, longitude: 10.8),
                    span: MKCoordinateSpan(latitudeDelta: 8, longitudeDelta: 8)
                ),
                animated: false
            )
            return
        }

        let annotations = pins.enumerated().map { index, pin in
            NumberedPointAnnotation(number: index + 1, title: pin.title, coordinate: CLLocationCoordinate2D(
                latitude: pin.coordinate.latitude,
                longitude: pin.coordinate.longitude
            ))
        }
        map.addAnnotations(annotations)

        let coordinates = annotations.map(\.coordinate)
        let route = routeCoordinates.map { CLLocationCoordinate2D(latitude: $0.latitude, longitude: $0.longitude) }
        let lineCoordinates = route.count > 1 ? route : coordinates
        if lineCoordinates.count > 1 {
            map.addOverlay(MKPolyline(coordinates: lineCoordinates, count: lineCoordinates.count), level: .aboveRoads)
        }

        if coordinates.count == 1, let coordinate = coordinates.first {
            map.setRegion(
                MKCoordinateRegion(center: coordinate, span: MKCoordinateSpan(latitudeDelta: 0.08, longitudeDelta: 0.08)),
                animated: false
            )
        } else if let overlay = map.overlays.first {
            map.setVisibleMapRect(
                overlay.boundingMapRect,
                edgePadding: UIEdgeInsets(top: 28, left: 28, bottom: 28, right: 28),
                animated: false
            )
        }
    }

    final class Coordinator: NSObject, MKMapViewDelegate, CLLocationManagerDelegate {
        let locationManager = CLLocationManager()
        weak var mapView: MKMapView?

        override init() {
            super.init()
            locationManager.delegate = self
        }

        func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
            guard let mapView else { return }
            mapView.showsUserLocation = manager.authorizationStatus == .authorizedAlways ||
                manager.authorizationStatus == .authorizedWhenInUse
        }

        func mapView(_ mapView: MKMapView, viewFor annotation: MKAnnotation) -> MKAnnotationView? {
            guard let annotation = annotation as? NumberedPointAnnotation else { return nil }
            let identifier = "RamingoNumberedPin"
            let marker = (mapView.dequeueReusableAnnotationView(withIdentifier: identifier) as? MKMarkerAnnotationView)
                ?? MKMarkerAnnotationView(annotation: annotation, reuseIdentifier: identifier)
            marker.annotation = annotation
            marker.markerTintColor = UIColor(red: 0.42, green: 0.36, blue: 0.91, alpha: 1)
            marker.glyphTintColor = .white
            marker.glyphText = String(annotation.number)
            marker.titleVisibility = .hidden
            marker.subtitleVisibility = .hidden
            marker.displayPriority = .required
            return marker
        }

        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            guard let polyline = overlay as? MKPolyline else { return MKOverlayRenderer(overlay: overlay) }
            let renderer = MKPolylineRenderer(polyline: polyline)
            renderer.strokeColor = UIColor(red: 0.42, green: 0.36, blue: 0.91, alpha: 1)
            renderer.lineWidth = 5
            renderer.lineJoin = .round
            renderer.lineCap = .round
            return renderer
        }
    }
}

private enum IOSMapRouteService {
    struct Result: Sendable {
        let coordinates: [Coordinate]
        let distanceMeters: CLLocationDistance?
    }

    static func load(stops: [Coordinate]) async -> Result {
        guard stops.count > 1 else { return Result(coordinates: [], distanceMeters: nil) }
        var result = [Coordinate]()
        var distanceMeters = 0.0
        let providerDistanceMeters = await IOSRoutingProvider.loadDistanceMeters(stops: stops)
        for index in 0..<(stops.count - 1) {
            let from = stops[index]
            let to = stops[index + 1]
            let request = MKDirections.Request()
            request.source = MKMapItem(placemark: MKPlacemark(coordinate: CLLocationCoordinate2D(latitude: from.latitude, longitude: from.longitude)))
            request.destination = MKMapItem(placemark: MKPlacemark(coordinate: CLLocationCoordinate2D(latitude: to.latitude, longitude: to.longitude)))
            request.transportType = .automobile
            do {
                let response = try await MKDirections(request: request).calculate()
                guard let route = response.routes.first else { continue }
                distanceMeters += route.distance
                let polyline = route.polyline
                let points = polyline.points()
                let coordinates = (0..<polyline.pointCount).map { index -> Coordinate in
                    let value = points[index].coordinate
                    return Coordinate(latitude: value.latitude, longitude: value.longitude)
                }
                if result.isEmpty { result.append(contentsOf: coordinates) }
                else { result.append(contentsOf: coordinates.dropFirst()) }
            } catch {
                // The map falls back to the ordered city polyline when Apple
                // routing is unavailable or the device is offline.
                continue
            }
        }
        return Result(
            coordinates: result.count > 1 ? result : [],
            distanceMeters: providerDistanceMeters ?? (distanceMeters > 0 ? distanceMeters : nil),
        )
    }
}

private enum IOSRoutingProvider {
    static func loadDistanceMeters(stops: [Coordinate]) async -> CLLocationDistance? {
        guard stops.count > 1 else { return nil }
        let locations = stops.map { "{\"lat\":\($0.latitude),\"lon\":\($0.longitude)}" }.joined(separator: ",")
        let body = "{\"locations\":[\(locations)],\"costing\":\"auto\",\"units\":\"kilometers\"}"
        for endpoint in [
            "https://ramingo.online/trip-route/valhalla/route",
            "https://valhalla1.openstreetmap.de/route",
        ] {
            if let length = await postValhalla(body: body, endpoint: endpoint) { return length }
        }
        return await loadOpenStreetMapDistance(stops: stops)
    }

    private static func postValhalla(body: String, endpoint: String) async -> CLLocationDistance? {
        guard let url = URL(string: endpoint) else { return nil }
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.timeoutInterval = 8
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("text/plain", forHTTPHeaderField: "Content-Type")
        request.httpBody = Data(body.utf8)
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              let httpResponse = response as? HTTPURLResponse,
              (200..<300).contains(httpResponse.statusCode),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let trip = object["trip"] as? [String: Any],
              let summary = trip["summary"] as? [String: Any],
              let kilometers = summary["length"] as? NSNumber
        else { return nil }
        return kilometers.doubleValue * 1_000
    }

    private static func loadOpenStreetMapDistance(stops: [Coordinate]) async -> CLLocationDistance? {
        let path = stops.map { "\($0.longitude),\($0.latitude)" }.joined(separator: ";")
        var components = URLComponents(string: "https://routing.openstreetmap.de/routed-car/route/v1/driving/\(path)")
        components?.queryItems = [
            URLQueryItem(name: "overview", value: "false"),
            URLQueryItem(name: "alternatives", value: "false"),
            URLQueryItem(name: "steps", value: "false"),
        ]
        guard let url = components?.url else { return nil }
        var request = URLRequest(url: url)
        request.timeoutInterval = 8
        guard let (data, response) = try? await URLSession.shared.data(for: request),
              let httpResponse = response as? HTTPURLResponse,
              (200..<300).contains(httpResponse.statusCode),
              let object = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let routes = object["routes"] as? [[String: Any]],
              let meters = routes.first?["distance"] as? NSNumber
        else { return nil }
        return meters.doubleValue
    }
}

private final class NumberedPointAnnotation: NSObject, MKAnnotation {
    let number: Int
    let title: String?
    dynamic var coordinate: CLLocationCoordinate2D

    init(number: Int, title: String, coordinate: CLLocationCoordinate2D) {
        self.number = number
        self.title = title
        self.coordinate = coordinate
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
    let parsedDateCount = iosWeatherTripDates(dates).count
    if parsedDateCount > 0 {
        return parsedDateCount
    }
    for part in dates.components(separatedBy: "·") where part.lowercased().contains("дн") {
        let digits = part.filter(\.isNumber)
        if let value = Int(digits), value > 0 { return value }
    }
    return 1
}

private func dayFromDate(_ date: String) -> String {
    if let parsed = iosWeatherTripDateRange(date)?.0 {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
        return String(calendar.component(.day, from: parsed))
    }
    date.split(whereSeparator: { !$0.isNumber }).first.map(String.init) ?? "—"
}

private func monthFromDate(_ date: String) -> String {
    if let parsed = iosWeatherTripDateRange(date)?.0 {
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
        return ["янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"][calendar.component(.month, from: parsed) - 1]
    }
    let parts = date.split(separator: " ")
    return parts.count > 1 ? String(parts[1].prefix(3)) : ""
}

private extension Array where Element == String {
    func uniqued(by key: (String) -> String) -> [String] {
        var seen = Set<String>()
        return filter { seen.insert(key($0)).inserted }
    }
}

private func routeTime(_ leg: RouteLeg) -> String {
    routeTiming(leg).value
}

private func routeTiming(_ leg: RouteLeg) -> (label: String, value: String) {
    let checkIn = leg.checkIn.trimmingCharacters(in: .whitespacesAndNewlines)
    let checkOut = leg.checkOut.trimmingCharacters(in: .whitespacesAndNewlines)
    if !checkIn.isEmpty { return ("Заселение", checkIn) }
    if !checkOut.isEmpty { return ("Выселение", checkOut) }
    return ("Заселение", "—")
}

private func cityFlag(_ city: String) -> String {
    let value = city.lowercased()
    if ["праг", "praha", "prague"].contains(where: value.contains) { return "🇨🇿" }
    if ["инцел", "мюнх", "равенсбург", "berlin", "munich"].contains(where: value.contains) { return "🇩🇪" }
    if ["рим", "верон", "пиз", "милан", "вене", "флорен", "кьодж", "ravenn", "rome"].contains(where: value.contains) { return "🇮🇹" }
    if ["зальц", "vienna", "вен"].contains(where: value.contains) { return "🇦🇹" }
    return "📍"
}

private func iosFilterCityKey(_ city: String) -> String {
    city.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
}

private func iosRestaurantStatusKey(_ status: String) -> String {
    let value = status.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    if value.contains("брон") || value.contains("reserv") || value.contains("book") { return "бронь" }
    if value.contains("хоч") || value.contains("want") { return "хочу" }
    if value.contains("был") || value.contains("visit") { return "были" }
    return value
}

private func iosPetIsVet(_ type: String) -> Bool {
    let value = type.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
    return value.contains("vet") || value.contains("вет") || value.contains("clinic") || value.contains("клиник") || value.contains("tierarzt")
}

private func iosPlaceURL(
    name: String,
    city: String,
    link: String,
    latitude: Double?,
    longitude: Double?,
) -> URL? {
    if let url = URL(string: link.trimmingCharacters(in: .whitespacesAndNewlines)), !link.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
        return url
    }
    var allowed = CharacterSet.urlQueryAllowed
    allowed.remove(charactersIn: "+&")
    if let latitude, let longitude {
        let query = "\(name), \(city)".addingPercentEncoding(withAllowedCharacters: allowed) ?? "\(name), \(city)"
        return URL(string: "http://maps.apple.com/?ll=\(latitude),\(longitude)&q=\(query)")
    }
    let query = "\(name), \(city)".addingPercentEncoding(withAllowedCharacters: allowed) ?? "\(name), \(city)"
    return URL(string: "http://maps.apple.com/?q=\(query)")
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

private func currencyCode(for symbol: String) -> String? {
    switch symbol {
    case "₽": return "RUB"
    case "€": return "EUR"
    case "Kč": return "CZK"
    case "$": return "USD"
    case "£": return "GBP"
    case "zł": return "PLN"
    case "Fr": return "CHF"
    case "Ft": return "HUF"
    default: return nil
    }
}

private func fallbackCurrencyPerRubRate(_ code: String) -> Double {
    switch code.uppercased() {
    case "EUR": return 1 / 100
    case "CZK": return 1 / 4
    case "USD": return 1 / 90
    case "GBP": return 1 / 115
    case "PLN": return 1 / 25
    case "CHF": return 1 / 105
    case "HUF": return 1 / 0.25
    default: return 1
    }
}

private func formatMoney(_ value: Double) -> String {
    value.formatted(.number.grouping(.automatic).precision(.fractionLength(value.rounded() == value ? 0 : 2)))
}

private struct IOSAccommodationDateParts {
    let year: Int
    let month: Int
    let day: Int
}

private let iosAccommodationMonths = [
    "янв", "фев", "мар", "апр", "май", "июн",
    "июл", "авг", "сен", "окт", "ноя", "дек",
]

private func accommodationDatePartsIOS(_ value: String) -> IOSAccommodationDateParts? {
    let pattern = #"(\d{4})-(\d{2})-(\d{2})"#
    guard let match = value.range(of: pattern, options: .regularExpression) else { return nil }
    let value = String(value[match])
    let parts = value.split(separator: "-").compactMap { Int($0) }
    guard parts.count == 3, (1...12).contains(parts[1]), (1...31).contains(parts[2]) else { return nil }
    return IOSAccommodationDateParts(year: parts[0], month: parts[1], day: parts[2])
}

private func accommodationDateIOS(_ value: String) -> Date? {
    guard let parts = accommodationDatePartsIOS(value) else { return nil }
    var components = DateComponents()
    components.calendar = Calendar(identifier: .gregorian)
    components.timeZone = .current
    components.year = parts.year
    components.month = parts.month
    components.day = parts.day
    return components.date
}

private func formatAccommodationDatesIOS(_ value: String) -> String {
    let raw = value.trimmingCharacters(in: .whitespacesAndNewlines)
    let parts = raw.components(separatedBy: " – ").count == 2
        ? raw.components(separatedBy: " – ")
        : raw.components(separatedBy: " - ")
    guard parts.count == 2,
          let start = accommodationDatePartsIOS(parts[0]),
          let end = accommodationDatePartsIOS(parts[1])
    else { return raw }

    if start.month == end.month {
        return "\(start.day)–\(end.day) \(iosAccommodationMonths[end.month - 1])"
    }
    return "\(start.day) \(iosAccommodationMonths[start.month - 1]) – \(end.day) \(iosAccommodationMonths[end.month - 1])"
}

private func formatAccommodationDeadlineDetailIOS(_ value: String) -> String {
    guard let parts = accommodationDatePartsIOS(value) else {
        return value.trimmingCharacters(in: .whitespacesAndNewlines)
    }
    let date = "\(parts.day) \(iosAccommodationMonths[parts.month - 1]) \(parts.year)"
    let time = value.range(of: #"\b\d{1,2}:\d{2}\b"#, options: .regularExpression).map { String(value[$0]) }
    if let time { return "\(date) · \(time) по местному времени" }
    return date
}

private func formatReviews(_ value: Int?) -> String {
    guard let value else { return "0" }
    return value.formatted(.number.grouping(.automatic))
}
