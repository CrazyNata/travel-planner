import Foundation
import CoreLocation
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
    @State private var showEditor = false
    @State private var weatherByCity: [String: WeatherSnapshot] = [:]
    @State private var exchangeRates: ExchangeRateSnapshot?

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            VStack(spacing: 0) {
                detailTopBar
                    .padding(.horizontal, 10)
                if isLoading {
                    Spacer()
                    ProgressView("Загружаем поездку…")
                        .font(AppTheme.font(13, .semibold))
                        .tint(AppTheme.purple)
                    Spacer()
                } else if let overview {
                    ScrollView(showsIndicators: false) {
                        VStack(alignment: .leading, spacing: 18) {
                            TripHeroView(overview: overview, client: model.client)
                            SectionPicker(selectedSection: $selectedSection)
                            tripSection(overview)
                        }
                        .padding(.horizontal, 18)
                        .padding(.top, 10)
                        .padding(.bottom, 42)
                    }
                    .refreshable { await load() }
                } else {
                    Spacer()
                    ContentUnavailableView("Поездка не найдена", systemImage: "airplane", description: Text("Проверьте подключение и попробуйте ещё раз."))
                    Spacer()
                }
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
        .alert("Не удалось загрузить поездку", isPresented: Binding(
            get: { localError != nil },
            set: { if !$0 { localError = nil } },
        )) {
            Button("ОК", role: .cancel) { localError = nil }
        } message: {
            Text(localError ?? "")
        }
    }

    private var detailTopBar: some View {
        HStack(spacing: 8) {
            Button { dismiss() } label: {
                Image(systemName: "chevron.left")
                    .font(.system(size: 18, weight: .bold))
                    .foregroundStyle(AppTheme.purple)
                    .frame(width: 42, height: 42)
            }
            .buttonStyle(.plain)

            Spacer(minLength: 4)
            Menu {
                ForEach(TripSection.allCases) { section in
                    Button {
                        selectedSection = section
                    } label: {
                        Label(section.title, systemImage: section.icon)
                    }
                }
            } label: {
                VStack(spacing: 1) {
                    Text(overview?.title ?? "Поездка")
                        .font(AppTheme.font(10, .bold))
                        .foregroundStyle(AppTheme.muted)
                        .lineLimit(1)
                    HStack(spacing: 5) {
                        Text(selectedSection.title)
                            .font(AppTheme.font(15, .extrabold))
                            .foregroundStyle(AppTheme.ink)
                        Image(systemName: "chevron.down")
                            .font(.system(size: 10, weight: .bold))
                            .foregroundStyle(AppTheme.purple)
                    }
                }
                .frame(maxWidth: .infinity)
            }

            Spacer(minLength: 4)
            Button { showEditor = true } label: {
                Image(systemName: "pencil")
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(overview?.canEdit == true ? AppTheme.ink : Color.clear)
                    .frame(width: 42, height: 42)
            }
            .buttonStyle(.plain)
            .disabled(overview?.canEdit != true)
        }
        .frame(height: 54)
    }

    @ViewBuilder
    private func tripSection(_ overview: TripOverview) -> some View {
        switch selectedSection {
        case .overview:
            OverviewSection(overview: overview, weather: weatherByCity)
        case .route:
            RouteSection(legs: overview.routeLegs)
        case .sights:
            SightsSection(sights: overview.sights, client: model.client)
        case .restaurants:
            RestaurantsSection(restaurants: overview.restaurants, client: model.client)
        case .accommodation:
            AccommodationSection(accommodations: overview.accommodations, client: model.client)
        case .budget:
            BudgetSection(overview: overview, exchangeRates: exchangeRates)
        case .members:
            MembersSection(members: overview.members)
        case .photos:
            PhotosSection(overview: overview, client: model.client)
        case .pets:
            PetsSection(pets: overview.petPlaces, client: model.client)
        }
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

private struct TripHeroView: View {
    let overview: TripOverview
    let client: SupabaseClient

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            RemotePhotoView(reference: overview.coverPhotos.first?.reference, client: client, contentMode: .fill, cornerRadius: 22)
                .frame(height: 204)
                .overlay(alignment: .bottomLeading) {
                    LinearGradient(colors: [.clear, .black.opacity(0.64)], startPoint: .top, endPoint: .bottom)
                        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
                        .overlay(alignment: .bottomLeading) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(overview.title)
                                    .font(AppTheme.font(24, .extrabold))
                                    .foregroundStyle(.white)
                                    .lineLimit(2)
                                if !overview.cities.isEmpty {
                                    Text(overview.cities.joined(separator: " · "))
                                        .font(AppTheme.font(13, .semibold))
                                        .foregroundStyle(.white.opacity(0.9))
                                }
                            }
                            .padding(16)
                        }
                }

            HStack {
                VStack(alignment: .leading, spacing: 4) {
                    Text(overview.dates.isEmpty ? "Даты не указаны" : overview.dates)
                        .font(AppTheme.font(13, .bold))
                        .foregroundStyle(AppTheme.ink)
                    Text(overview.currentUserRole.isEmpty ? overview.status : "\(overview.status) · \(overview.currentUserRole)")
                        .font(AppTheme.font(11, .semibold))
                        .foregroundStyle(AppTheme.muted)
                }
                Spacer()
                Text("\(overview.progress)%")
                    .font(AppTheme.font(18, .extrabold))
                    .foregroundStyle(AppTheme.purple)
            }
            ProgressBar(value: overview.progress)
        }
    }
}

private struct SectionPicker: View {
    @Binding var selectedSection: TripSection

    var body: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 9) {
                ForEach(TripSection.allCases) { section in
                    Button {
                        withAnimation(.easeInOut(duration: 0.18)) { selectedSection = section }
                    } label: {
                        Label(section.title, systemImage: section.icon)
                            .font(AppTheme.font(12, .bold))
                            .foregroundStyle(selectedSection == section ? .white : AppTheme.purpleDeep)
                            .padding(.horizontal, 13)
                            .frame(height: 38)
                            .background(selectedSection == section ? AppTheme.purple : AppTheme.lavender, in: Capsule())
                            .overlay {
                                if selectedSection != section {
                                    Capsule().stroke(AppTheme.border, lineWidth: 1)
                                }
                            }
                    }
                    .buttonStyle(.plain)
                }
            }
        }
    }
}

private struct OverviewSection: View {
    let overview: TripOverview
    let weather: [String: WeatherSnapshot]

    var body: some View {
        VStack(alignment: .leading, spacing: 16) {
            HStack(spacing: 10) {
                StatTile(value: "\(overview.cities.count)", label: "города", icon: "mappin.and.ellipse")
                StatTile(value: "\(overview.routeLegs.count)", label: "переезда", icon: "car.fill")
                StatTile(value: "\(overview.sights.count)", label: "места", icon: "building.columns.fill")
            }

            if !weather.isEmpty {
                SectionHeading("Погода", subtitle: "Сейчас и на даты поездки")
                ScrollView(.horizontal, showsIndicators: false) {
                    HStack(spacing: 10) {
                        ForEach(overview.cities.filter { weather[$0] != nil }, id: \.self) { city in
                            if let snapshot = weather[city] {
                                WeatherCard(city: city, snapshot: snapshot)
                            }
                        }
                    }
                }
            }

            if !overview.cityCoordinates.isEmpty {
                SectionHeading("Карта маршрута", subtitle: "Города поездки")
                TripMapView(coordinates: overview.cityCoordinates)
            }

            if !overview.routeLegs.isEmpty {
                SectionHeading("Ближайшие переезды")
                ForEach(overview.routeLegs.prefix(3)) { leg in RouteRow(leg: leg) }
            }

            if !overview.accommodations.isEmpty || !overview.restaurants.isEmpty {
                HStack(spacing: 10) {
                    StatTile(value: "\(overview.accommodations.count)", label: "жильё", icon: "bed.double.fill")
                    StatTile(value: "\(overview.restaurants.count)", label: "ресторана", icon: "fork.knife")
                }
            }
        }
    }
}

private struct WeatherCard: View {
    let city: String
    let snapshot: WeatherSnapshot

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(city)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
            Text(snapshot.tripTemperature ?? snapshot.temperature)
                .font(.title3.weight(.heavy))
            Text(snapshot.tripCondition ?? snapshot.condition)
                .font(.caption)
                .foregroundStyle(AppTheme.muted)
            if snapshot.tripIsEstimate {
                Text("ориентировочно")
                    .font(.caption2)
                    .foregroundStyle(.orange)
            }
        }
        .frame(width: 138, alignment: .leading)
        .padding(14)
        .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

private struct RouteSection: View {
    let legs: [RouteLeg]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeading("Маршрут", subtitle: legs.isEmpty ? "Переезды пока не добавлены" : "\(legs.count) участков")
            if legs.isEmpty {
                EmptySection(icon: "point.topleft.down.to.point.bottomright.curvepath", text: "Добавьте города и переезды в редакторе поездки.")
            } else {
                ForEach(legs) { leg in RouteRow(leg: leg, expanded: true) }
            }
        }
    }
}

private struct RouteRow: View {
    let leg: RouteLeg
    var expanded = false

    var body: some View {
        RamingoCard {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .top, spacing: 12) {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(leg.from)
                            .font(.headline.weight(.bold))
                        Image(systemName: "arrow.down")
                            .font(.caption.weight(.bold))
                            .foregroundStyle(AppTheme.purple)
                        Text(leg.to)
                            .font(.headline.weight(.bold))
                    }
                    Spacer()
                    if !leg.date.isEmpty {
                        Text(leg.date)
                            .font(.caption.weight(.semibold))
                            .foregroundStyle(AppTheme.muted)
                    }
                }
                if !leg.distance.isEmpty || !leg.travelTime.isEmpty {
                    HStack(spacing: 14) {
                        if !leg.distance.isEmpty { Label(leg.distance, systemImage: "ruler") }
                        if !leg.travelTime.isEmpty { Label(leg.travelTime, systemImage: "clock") }
                        Spacer()
                    }
                    .font(.caption)
                    .foregroundStyle(AppTheme.muted)
                }
                if expanded, !leg.notes.isEmpty {
                    Text(leg.notes)
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.muted)
                }
                if expanded, let url = URL(string: leg.mapsURL), !leg.mapsURL.isEmpty {
                    Link(destination: url) { Label("Открыть на карте", systemImage: "arrow.up.right.square") }
                        .font(.caption.weight(.semibold))
                }
            }
        }
    }
}

private struct SightsSection: View {
    let sights: [Sight]
    let client: SupabaseClient

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeading("Достопримечательности", subtitle: "\(sights.count) мест в поездке")
            if sights.isEmpty {
                EmptySection(icon: "building.columns", text: "В поездке пока нет достопримечательностей.")
            } else {
                ForEach(sights) { sight in
                    RamingoCard {
                        HStack(alignment: .top, spacing: 12) {
                            RemotePhotoView(reference: sight.photo.nonEmpty, client: client, contentMode: .fill, cornerRadius: 14)
                                .frame(width: 92, height: 106)
                            VStack(alignment: .leading, spacing: 6) {
                                HStack(alignment: .top) {
                                    Text(sight.name)
                                        .font(.headline.weight(.bold))
                                        .foregroundStyle(AppTheme.ink)
                                    Spacer()
                                    if sight.done { Image(systemName: "checkmark.circle.fill").foregroundStyle(.green) }
                                }
                                if !sight.city.isEmpty { Text(sight.city).font(.caption.weight(.semibold)).foregroundStyle(AppTheme.purpleDeep) }
                                if !sight.category.isEmpty { Text(sight.category.uppercased()).font(.caption2.weight(.bold)).foregroundStyle(AppTheme.muted) }
                                if let rating = sight.rating {
                                    Label(String(format: "%.1f", rating), systemImage: "star.fill")
                                        .font(.caption.weight(.semibold))
                                        .foregroundStyle(.orange)
                                }
                                if !sight.description.isEmpty {
                                    Text(sight.description).font(.caption).foregroundStyle(AppTheme.muted).lineLimit(3)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

private struct RestaurantsSection: View {
    let restaurants: [Restaurant]
    let client: SupabaseClient

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeading("Рестораны", subtitle: "\(restaurants.count) сохранённых мест")
            if restaurants.isEmpty {
                EmptySection(icon: "fork.knife", text: "Сохраните ресторан, чтобы он появился здесь.")
            } else {
                ForEach(restaurants) { restaurant in
                    RamingoCard {
                        HStack(alignment: .top, spacing: 12) {
                            RemotePhotoView(reference: restaurant.photos.first, client: client, contentMode: .fill, cornerRadius: 14)
                                .frame(width: 82, height: 82)
                            VStack(alignment: .leading, spacing: 6) {
                                HStack {
                                    Text(restaurant.name).font(.headline.weight(.bold))
                                    if restaurant.priority { Image(systemName: "bookmark.fill").foregroundStyle(AppTheme.purple) }
                                }
                                Text([restaurant.city, restaurant.status].filter { !$0.isEmpty }.joined(separator: " · "))
                                    .font(.caption).foregroundStyle(AppTheme.muted)
                                HStack(spacing: 10) {
                                    if let rating = restaurant.rating { Label(String(format: "%.1f", rating), systemImage: "star.fill").foregroundStyle(.orange) }
                                    if !restaurant.price.isEmpty { Text(restaurant.price) }
                                }
                                .font(.caption.weight(.semibold))
                                if !restaurant.note.isEmpty { Text(restaurant.note).font(.caption).foregroundStyle(AppTheme.muted).lineLimit(2) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private struct AccommodationSection: View {
    let accommodations: [Accommodation]
    let client: SupabaseClient

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeading("Жильё", subtitle: "\(accommodations.count) вариантов")
            if accommodations.isEmpty {
                EmptySection(icon: "bed.double", text: "Добавьте жильё для города поездки.")
            } else {
                ForEach(accommodations) { accommodation in
                    RamingoCard {
                        VStack(alignment: .leading, spacing: 12) {
                            HStack(alignment: .top, spacing: 12) {
                                RemotePhotoView(reference: accommodation.photos.first, client: client, contentMode: .fill, cornerRadius: 14)
                                    .frame(width: 92, height: 92)
                                VStack(alignment: .leading, spacing: 5) {
                                    Text(accommodation.name).font(.headline.weight(.bold))
                                    if !accommodation.city.isEmpty { Text(accommodation.city).font(.caption.weight(.semibold)).foregroundStyle(AppTheme.purpleDeep) }
                                    if !accommodation.dates.isEmpty { Label(accommodation.dates, systemImage: "calendar").font(.caption).foregroundStyle(AppTheme.muted) }
                                    if !accommodation.price.isEmpty { Text(accommodation.price).font(.subheadline.weight(.bold)) }
                                }
                            }
                            HStack {
                                if !accommodation.status.isEmpty { StatusPill(text: accommodation.status) }
                                if !accommodation.deadline.isEmpty { Label("Отмена до \(accommodation.deadline)", systemImage: "clock").font(.caption).foregroundStyle(.orange) }
                                Spacer()
                                if let rating = accommodation.rating { Label(String(format: "%.1f", rating), systemImage: "star.fill").font(.caption.weight(.semibold)).foregroundStyle(.orange) }
                            }
                            if !accommodation.details.isEmpty { Text(accommodation.details).font(.caption).foregroundStyle(AppTheme.muted).lineLimit(3) }
                        }
                    }
                }
            }
        }
    }
}

private struct BudgetSection: View {
    let overview: TripOverview
    let exchangeRates: ExchangeRateSnapshot?

    private var total: Double { overview.budgetExpenses.reduce(0) { $0 + $1.amount } }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeading("Бюджет", subtitle: "Валюта поездки · \(overview.budgetCurrency)")
            RamingoCard {
                VStack(alignment: .leading, spacing: 8) {
                    Text("Всего запланировано")
                        .font(.subheadline)
                        .foregroundStyle(AppTheme.muted)
                    Text(total.formatted(.number.precision(.fractionLength(2))) + " " + overview.budgetCurrency)
                        .font(.system(size: 30, weight: .heavy, design: .rounded))
                        .foregroundStyle(AppTheme.ink)
                    Text("\(overview.budgetExpenses.count) трат")
                        .font(.caption)
                        .foregroundStyle(AppTheme.muted)
                }
            }
            if let exchangeRates,
               overview.budgetCurrency.uppercased() != "RUB",
               let rubPerUnit = exchangeRates.rates[overview.budgetCurrency.uppercased()],
               rubPerUnit > 0 {
                RamingoCard {
                    HStack(spacing: 12) {
                        Image(systemName: "arrow.left.arrow.right")
                            .foregroundStyle(AppTheme.purple)
                        VStack(alignment: .leading, spacing: 3) {
                            Text("Онлайн-курс")
                                .font(.subheadline.weight(.semibold))
                            Text("1 \(overview.budgetCurrency.uppercased()) ≈ \(String(format: "%.2f", 1 / rubPerUnit)) ₽")
                                .font(.caption)
                                .foregroundStyle(AppTheme.muted)
                            if !exchangeRates.date.isEmpty {
                                Text("Frankfurter · \(exchangeRates.date)")
                                    .font(.caption2)
                                    .foregroundStyle(AppTheme.muted)
                            }
                        }
                        Spacer()
                    }
                }
            }
            if overview.budgetExpenses.isEmpty {
                EmptySection(icon: "creditcard", text: "Траты ещё не добавлены.")
            } else {
                ForEach(overview.budgetExpenses) { expense in
                    RamingoCard {
                        HStack {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(expense.name).font(.subheadline.weight(.semibold))
                                Text([expense.category, expense.paidBy].filter { !$0.isEmpty }.joined(separator: " · "))
                                    .font(.caption).foregroundStyle(AppTheme.muted)
                            }
                            Spacer()
                            Text(expense.amount.formatted(.number.precision(.fractionLength(2))) + " " + (expense.inputCurrency.isEmpty ? overview.budgetCurrency : expense.inputCurrency))
                                .font(.subheadline.weight(.bold))
                        }
                    }
                }
            }
        }
    }
}

private struct MembersSection: View {
    let members: [TripMember]

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeading("Участники", subtitle: "\(members.count) человек")
            if members.isEmpty {
                EmptySection(icon: "person.2", text: "Участники ещё не добавлены.")
            } else {
                ForEach(members) { member in
                    RamingoCard {
                        HStack(spacing: 12) {
                            Text(member.initials)
                                .font(.headline.weight(.bold))
                                .foregroundStyle(.white)
                                .frame(width: 44, height: 44)
                                .background(AppTheme.purple, in: Circle())
                            VStack(alignment: .leading, spacing: 4) {
                                Text(member.name).font(.headline.weight(.semibold))
                                if !member.email.isEmpty { Text(member.email).font(.caption).foregroundStyle(AppTheme.muted) }
                            }
                            Spacer()
                            Text(member.role.isEmpty ? "Участник" : member.role)
                                .font(.caption.weight(.semibold))
                                .foregroundStyle(AppTheme.purpleDeep)
                        }
                    }
                }
            }
        }
    }
}

private struct PhotosSection: View {
    let overview: TripOverview
    let client: SupabaseClient

    private var references: [String] {
        var values = overview.coverPhotos.map(\.reference)
        values += overview.sights.flatMap(\.photos)
        values += overview.accommodations.flatMap(\.photos)
        values += overview.restaurants.flatMap(\.photos)
        values += overview.petPlaces.map(\.photo)
        return values.filter { !$0.isEmpty }.uniqued()
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeading("Фото", subtitle: "Фотографии поездки")
            if references.isEmpty {
                EmptySection(icon: "photo.on.rectangle", text: "Фото появятся здесь после добавления в поездке.")
            } else {
                LazyVGrid(columns: [GridItem(.flexible()), GridItem(.flexible())], spacing: 12) {
                    ForEach(Array(references.enumerated()), id: \.offset) { _, reference in
                        RemotePhotoView(reference: reference, client: client, contentMode: .fill, cornerRadius: 16)
                            .frame(height: 150)
                    }
                }
            }
        }
    }
}

private struct PetsSection: View {
    let pets: [PetPlace]
    let client: SupabaseClient

    var body: some View {
        VStack(alignment: .leading, spacing: 14) {
            SectionHeading("Питомцы", subtitle: "Pet-friendly места")
            if pets.isEmpty {
                EmptySection(icon: "pawprint", text: "В поездке пока нет мест для питомцев.")
            } else {
                ForEach(pets) { pet in
                    RamingoCard {
                        HStack(alignment: .top, spacing: 12) {
                            RemotePhotoView(reference: pet.photo.nonEmpty, client: client, contentMode: .fill, cornerRadius: 14)
                                .frame(width: 84, height: 84)
                            VStack(alignment: .leading, spacing: 5) {
                                Text(pet.name).font(.headline.weight(.bold))
                                Text([pet.city, pet.type == "vet" ? "Ветеринар" : "Магазин"].filter { !$0.isEmpty }.joined(separator: " · "))
                                    .font(.caption).foregroundStyle(AppTheme.muted)
                                if let rating = pet.rating { Label(String(format: "%.1f", rating), systemImage: "star.fill").font(.caption.weight(.semibold)).foregroundStyle(.orange) }
                                if !pet.address.isEmpty { Text(pet.address).font(.caption).foregroundStyle(AppTheme.muted).lineLimit(2) }
                            }
                        }
                    }
                }
            }
        }
    }
}

private struct TripMapPin: Identifiable {
    let id: String
    let title: String
    let coordinate: CLLocationCoordinate2D
}

private struct TripMapView: View {
    let pins: [TripMapPin]
    @State private var region: MKCoordinateRegion

    init(coordinates: [String: Coordinate]) {
        let sorted = coordinates.sorted { $0.key < $1.key }
        pins = sorted.map { name, coordinate in
            TripMapPin(id: name, title: name, coordinate: CLLocationCoordinate2D(latitude: coordinate.latitude, longitude: coordinate.longitude))
        }
        let latitude = sorted.map { $0.value.latitude }.reduce(0, +) / Double(max(sorted.count, 1))
        let longitude = sorted.map { $0.value.longitude }.reduce(0, +) / Double(max(sorted.count, 1))
        _region = State(initialValue: MKCoordinateRegion(
            center: CLLocationCoordinate2D(latitude: latitude, longitude: longitude),
            span: MKCoordinateSpan(latitudeDelta: 8, longitudeDelta: 8),
        ))
    }

    var body: some View {
        Map(coordinateRegion: $region, annotationItems: pins) { pin in
            MapMarker(coordinate: pin.coordinate, tint: AppTheme.purple)
        }
        .frame(height: 230)
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .allowsHitTesting(true)
    }
}

private struct StatTile: View {
    let value: String
    let label: String
    let icon: String

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Image(systemName: icon).foregroundStyle(AppTheme.purple)
            Text(value).font(.title3.weight(.heavy)).foregroundStyle(AppTheme.ink)
            Text(label).font(.caption).foregroundStyle(AppTheme.muted)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding(13)
        .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
    }
}

private struct EmptySection: View {
    let icon: String
    let text: String

    var body: some View {
        RamingoCard {
            VStack(spacing: 10) {
                Image(systemName: icon).font(.title2).foregroundStyle(AppTheme.purple)
                Text(text).font(.subheadline).foregroundStyle(AppTheme.muted).multilineTextAlignment(.center)
            }
            .frame(maxWidth: .infinity)
        }
    }
}

private extension Array where Element: Hashable {
    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}
