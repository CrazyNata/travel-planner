import Foundation

enum CatalogCategory: String, CaseIterable, Identifiable, Hashable, Sendable {
    case sight
    case restaurant
    case accommodation
    case pet

    var id: String { rawValue }

    var title: String {
        switch self {
        case .sight: return "Достопримечательности"
        case .restaurant: return "Рестораны"
        case .accommodation: return "Жильё"
        case .pet: return "Места для питомцев"
        }
    }

    var icon: String {
        switch self {
        case .sight: return "building.columns"
        case .restaurant: return "fork.knife"
        case .accommodation: return "bed.double"
        case .pet: return "pawprint"
        }
    }

    var tripSection: String {
        switch self {
        case .sight: return "sights"
        case .restaurant: return "restaurants"
        case .accommodation: return "accommodations"
        case .pet: return "petPlaces"
        }
    }
}

struct CatalogEntry: Identifiable, Hashable, Sendable {
    let id: String
    let kind: CatalogCategory
    let name: String
    let city: String
    let category: String
    let description: String
    let address: String
    let type: String
    let mapURL: String
    let website: String
    let phone: String
    let latitude: Double?
    let longitude: Double?
    let rating: Double?
    let reviewCount: Int?
    let priceLevel: Int?
    let photoURL: String?
    let photoName: String
    let photoNames: [String]
    let photoAttribution: String?
    let source: String
    let googlePlaceID: String
    let isLive: Bool
    let openNow: Bool?

    var photoReference: String? {
        photoURL?.nonEmpty ?? photoName.nonEmpty ?? photoNames.first?.nonEmpty
    }

    var priceLabel: String {
        guard let priceLevel, priceLevel > 0 else { return "" }
        return String(repeating: "$", count: min(max(priceLevel, 1), 4))
    }

    var ratingLabel: String? {
        guard let rating else { return nil }
        if let reviewCount {
            return String(format: "%.1f · %d отзывов", rating, reviewCount)
        }
        return String(format: "%.1f", rating)
    }

    func tripPayload(walkDay: Int = 1) -> [String: JSONValue] {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanCity = city.trimmingCharacters(in: .whitespacesAndNewlines)
        let primaryPhoto = photoReference
        var payload: [String: JSONValue] = [
            "id": .string(UUID().uuidString.lowercased()),
            "catalogId": .string(id),
            "name": .string(cleanName),
            "city": .string(cleanCity),
        ]

        if let primaryPhoto, !primaryPhoto.isEmpty {
            payload["photo"] = .string(primaryPhoto)
        }
        if !photoName.isEmpty {
            payload["photoName"] = .string(photoName)
        }
        if let rating {
            payload["rating"] = .number(rating)
        }
        if let reviewCount {
            payload["ratingCount"] = .number(Double(reviewCount))
        }
        if !googlePlaceID.isEmpty {
            payload["googlePlaceId"] = .string(googlePlaceID)
        }
        if !source.isEmpty {
            payload["source"] = .string(source)
        }
        if !photoNames.isEmpty {
            payload["photoNames"] = .array(photoNames.map(JSONValue.string))
        }
        if let photoAttribution, !photoAttribution.isEmpty {
            payload["photoAttribution"] = .string(photoAttribution)
        }
        if !mapURL.isEmpty {
            payload["link"] = .string(mapURL)
        }
        if let latitude, let longitude {
            payload["latitude"] = .number(latitude)
            payload["longitude"] = .number(longitude)
            payload["lnglat"] = .array([.number(longitude), .number(latitude)])
        }

        switch kind {
        case .sight:
            payload["subcategory"] = .string(category)
            payload["description"] = .string(description)
            payload["walkDay"] = .number(Double(max(walkDay, 1)))
            payload["walkOrder"] = .number(0)
            payload["done"] = .boolean(false)
        case .restaurant:
            payload["status"] = .string("хочу")
            payload["note"] = .string(category.isEmpty ? description : category)
            payload["price"] = .string(priceLabel)
            payload["date"] = .string("")
            payload["priority"] = .boolean(false)
            payload["photos"] = .array(photoReferences.map(JSONValue.string))
        case .accommodation:
            payload["dates"] = .string("")
            payload["price"] = .string(priceLabel)
            payload["status"] = .string("хочу")
            payload["details"] = .string(address)
            payload["bookingUrl"] = .string("")
            payload["externalUrl"] = .string(website.isEmpty ? mapURL : website)
            payload["address"] = .string(address)
            payload["type"] = .string(type.isEmpty ? category : type)
            payload["reviewCount"] = reviewCount.map { .number(Double($0)) } ?? .null
            payload["photoReference"] = .string(photoName)
            payload["website"] = .string(website)
            payload["phone"] = .string(phone)
            payload["photos"] = .array(photoReferences.map(JSONValue.string))
            payload["tripCityId"] = .string(city)
        case .pet:
            let normalizedType = type.lowercased() == "vet" || type.lowercased() == "veterinary" ? "vet" : "shop"
            payload["type"] = .string(normalizedType)
            payload["address"] = .string(address)
            payload["phone"] = .string(phone)
            payload["mapsUrl"] = .string(mapURL)
            payload["website"] = .string(website)
            payload["note"] = .string("")
            payload["features"] = .array([])
            if let openNow { payload["openNow"] = .boolean(openNow) }
        }
        return payload
    }

    private var photoReferences: [String] {
        var values = [String]()
        if let photoURL, !photoURL.isEmpty { values.append(photoURL) }
        values.append(contentsOf: photoNames.filter { !$0.isEmpty })
        if !photoName.isEmpty, !values.contains(photoName) { values.append(photoName) }
        return values
    }
}

private struct CatalogSightRow: Decodable {
    let id: String?
    let cityKey: String?
    let cityNameRu: String?
    let cityNameEn: String?
    let cityNameEs: String?
    let cityNameDe: String?
    let nameRu: String?
    let nameEn: String?
    let nameEs: String?
    let nameDe: String?
    let descriptionRu: String?
    let descriptionEn: String?
    let descriptionEs: String?
    let descriptionDe: String?
    let category: String?
    let latitude: Double?
    let longitude: Double?
    let mapURL: String?
    let searchText: String?
    let sortOrder: Int?
    let photoURL: String?
    let photoName: String?
    let photoAttribution: String?
    let rating: Double?
    let ratingCount: Int?
    let ratingPlaceURL: String?

    enum CodingKeys: String, CodingKey {
        case id
        case cityKey = "city_key"
        case cityNameRu = "city_name_ru"
        case cityNameEn = "city_name_en"
        case cityNameEs = "city_name_es"
        case cityNameDe = "city_name_de"
        case nameRu = "name_ru"
        case nameEn = "name_en"
        case nameEs = "name_es"
        case nameDe = "name_de"
        case descriptionRu = "description_ru"
        case descriptionEn = "description_en"
        case descriptionEs = "description_es"
        case descriptionDe = "description_de"
        case category
        case latitude
        case longitude
        case mapURL = "map_url"
        case searchText = "search_text"
        case sortOrder = "sort_order"
        case photoURL = "photo_url"
        case photoName = "photo_name"
        case photoAttribution = "photo_attribution"
        case rating
        case ratingCount = "rating_count"
        case ratingPlaceURL = "rating_place_url"
    }
}

private struct CatalogRestaurantRow: Decodable {
    let id: String?
    let cityKey: String?
    let cityNameRu: String?
    let cityNameEn: String?
    let cityNameEs: String?
    let cityNameDe: String?
    let nameRu: String?
    let nameEn: String?
    let nameEs: String?
    let nameDe: String?
    let cuisine: String?
    let address: String?
    let website: String?
    let phone: String?
    let latitude: Double?
    let longitude: Double?
    let openNow: Bool?
    let mapURL: String?
    let searchText: String?
    let sortOrder: Int?

    enum CodingKeys: String, CodingKey {
        case id
        case cityKey = "city_key"
        case cityNameRu = "city_name_ru"
        case cityNameEn = "city_name_en"
        case cityNameEs = "city_name_es"
        case cityNameDe = "city_name_de"
        case nameRu = "name_ru"
        case nameEn = "name_en"
        case nameEs = "name_es"
        case nameDe = "name_de"
        case cuisine
        case address
        case website
        case phone
        case latitude
        case longitude
        case openNow = "open_now"
        case mapURL = "map_url"
        case searchText = "search_text"
        case sortOrder = "sort_order"
    }
}

private struct CatalogPlace: Decodable {
    let placeID: String?
    let name: String?
    let address: String?
    let category: String?
    let cuisine: String?
    let description: String?
    let type: String?
    let rating: Double?
    let ratingCount: Int?
    let priceLevel: Int?
    let photoURL: String?
    let photoName: String?
    let photoNames: [String]?
    let photoAttribution: String?
    let mapURL: String?
    let website: String?
    let phone: String?
    let latitude: Double?
    let longitude: Double?
    let openNow: Bool?

    enum CodingKeys: String, CodingKey {
        case placeID = "place_id"
        case name
        case address
        case category
        case cuisine
        case description
        case type
        case rating
        case ratingCount = "rating_count"
        case priceLevel = "price_level"
        case photoURL = "photo_url"
        case photoName = "photo_name"
        case photoNames = "photo_names"
        case photoAttribution = "photo_attribution"
        case mapURL = "google_maps_url"
        case website
        case phone
        case latitude
        case longitude
        case openNow = "open_now"
    }
}

private struct CatalogEnrichmentResponse: Decodable {
    let sights: [CatalogPlace]?
    let restaurants: [CatalogPlace]?
    let accommodations: [CatalogPlace]?
    let petPlaces: [CatalogPlace]?
}

private struct LiveCatalogRequest: Encodable {
    let category: String?
    let petType: String?
    let city: String
    let query: String
    let languageCode: String
    let limit: Int

    enum CodingKeys: String, CodingKey {
        case category
        case petType
        case city
        case query
        case languageCode
        case limit
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.container(keyedBy: CodingKeys.self)
        try container.encodeIfPresent(category, forKey: .category)
        try container.encodeIfPresent(petType, forKey: .petType)
        try container.encode(city, forKey: .city)
        try container.encode(query, forKey: .query)
        try container.encode(languageCode, forKey: .languageCode)
        try container.encode(limit, forKey: .limit)
    }
}

private struct CatalogEmptyBody: Encodable {}

final class CatalogRepository {
    private let client: SupabaseClient

    init(client: SupabaseClient) {
        self.client = client
    }

    func search(
        kind: CatalogCategory,
        city: String,
        query: String,
        language: String,
        petType: String = "shop",
    ) async throws -> [CatalogEntry] {
        let selectedCity = catalogCityName(city)
        guard !selectedCity.isEmpty else { return [] }

        switch kind {
        case .sight:
            let fallback = try? await searchStaticSights(city: selectedCity, query: query, language: language)
            let live = try? await searchLive(kind: kind, city: selectedCity, query: query, language: language, petType: petType)
            if let live, !live.isEmpty {
                return mergeSightEntries(live: live, fallback: fallback ?? [])
            }
            if let fallback { return fallback }
            throw CatalogError.unavailable
        case .restaurant:
            let fallback = try? await searchStaticRestaurants(city: selectedCity, query: query, language: language)
            let live = try? await searchLive(kind: kind, city: selectedCity, query: query, language: language, petType: petType)
            if let live, !live.isEmpty { return live }
            if let fallback { return fallback }
            throw CatalogError.unavailable
        case .accommodation, .pet:
            return try await searchLive(kind: kind, city: selectedCity, query: query, language: language, petType: petType)
        }
    }

    private func searchStaticSights(city: String, query: String, language: String) async throws -> [CatalogEntry] {
        let rows: [CatalogSightRow] = try await loadRows(table: "sight_catalog", city: city)
        let normalizedQuery = normalizeCatalogText(query)
        return rows.compactMap { row in
            let name = localizedName(
                language: language,
                ru: row.nameRu,
                en: row.nameEn,
                es: row.nameEs,
                de: row.nameDe,
            )
            let searchValues = [
                row.nameRu,
                row.nameEn,
                row.nameEs,
                row.nameDe,
                row.category,
                row.searchText,
            ].compactMap { $0 }
            guard let id = row.id?.nonEmpty, let name = name.nonEmpty else { return nil }
            guard normalizedQuery.isEmpty || searchValues.contains(where: { normalizeCatalogText($0).contains(normalizedQuery) }) else { return nil }
            return CatalogEntry(
                id: id,
                kind: .sight,
                name: name,
                city: city,
                category: row.category ?? "",
                description: localizedName(language: language, ru: row.descriptionRu, en: row.descriptionEn, es: row.descriptionEs, de: row.descriptionDe),
                address: "",
                type: "",
                mapURL: row.mapURL ?? "",
                website: "",
                phone: "",
                latitude: row.latitude,
                longitude: row.longitude,
                rating: row.rating,
                reviewCount: row.ratingCount,
                priceLevel: nil,
                photoURL: row.photoURL,
                photoName: row.photoName ?? "",
                photoNames: row.photoName?.nonEmpty.map { [$0] } ?? [],
                photoAttribution: row.photoAttribution,
                source: "catalog",
                googlePlaceID: "",
                isLive: false,
                openNow: nil,
            )
        }.sorted {
            if normalizedQuery.isEmpty { return ($0.id) < ($1.id) }
            let leftStarts = normalizeCatalogText($0.name).hasPrefix(normalizedQuery)
            let rightStarts = normalizeCatalogText($1.name).hasPrefix(normalizedQuery)
            if leftStarts != rightStarts { return leftStarts }
            return normalizeCatalogText($0.name) < normalizeCatalogText($1.name)
        }
    }

    private func searchStaticRestaurants(city: String, query: String, language: String) async throws -> [CatalogEntry] {
        let rows: [CatalogRestaurantRow] = try await loadRows(table: "restaurant_catalog", city: city)
        let normalizedQuery = normalizeCatalogText(query)
        return rows.compactMap { row in
            let name = localizedName(language: language, ru: row.nameRu, en: row.nameEn, es: row.nameEs, de: row.nameDe)
            let searchValues = [row.nameRu, row.nameEn, row.nameEs, row.nameDe, row.cuisine, row.address, row.website, row.searchText]
                .compactMap { $0 }
            guard let id = row.id?.nonEmpty, let name = name.nonEmpty else { return nil }
            guard normalizedQuery.isEmpty || searchValues.contains(where: { normalizeCatalogText($0).contains(normalizedQuery) }) else { return nil }
            return CatalogEntry(
                id: id,
                kind: .restaurant,
                name: name,
                city: city,
                category: row.cuisine ?? "",
                description: "",
                address: row.address ?? "",
                type: "",
                mapURL: row.mapURL ?? "",
                website: row.website ?? "",
                phone: row.phone ?? "",
                latitude: row.latitude,
                longitude: row.longitude,
                rating: nil,
                reviewCount: nil,
                priceLevel: nil,
                photoURL: nil,
                photoName: "",
                photoNames: [],
                photoAttribution: nil,
                source: "catalog",
                googlePlaceID: "",
                isLive: false,
                openNow: nil,
            )
        }.sorted {
            if normalizedQuery.isEmpty { return ($0.id) < ($1.id) }
            let leftStarts = normalizeCatalogText($0.name).hasPrefix(normalizedQuery)
            let rightStarts = normalizeCatalogText($1.name).hasPrefix(normalizedQuery)
            if leftStarts != rightStarts { return leftStarts }
            return normalizeCatalogText($0.name) < normalizeCatalogText($1.name)
        }
    }

    private func loadRows<T: Decodable>(table: String, city: String) async throws -> [T] {
        let encodedCity = queryComponent(city)
        let clauses = ["city_name_ru", "city_name_en", "city_name_es", "city_name_de"]
            .map { "\($0).eq.\(encodedCity)" }
            .joined(separator: ",")
        let path = "rest/v1/\(table)?select=*&or=(\(clauses))&limit=200"
        return try await client.request(
            path,
            method: "GET",
            body: nil as CatalogEmptyBody?,
            authenticated: true,
        )
    }

    private func searchLive(
        kind: CatalogCategory,
        city: String,
        query: String,
        language: String,
        petType: String,
    ) async throws -> [CatalogEntry] {
        let normalizedPetType = petType.lowercased() == "vet" || petType.lowercased() == "veterinary" ? "vet" : "shop"
        let request = LiveCatalogRequest(
            category: kind == .restaurant ? nil : kind.rawValue,
            petType: kind == .pet ? normalizedPetType : nil,
            city: city,
            query: String(query.trimmingCharacters(in: .whitespacesAndNewlines).prefix(80)),
            languageCode: placesLanguageCode(language),
            limit: 60,
        )
        let response: CatalogEnrichmentResponse = try await client.invokeFunctionResponse("restaurant-enrichment", body: request)
        let places: [CatalogPlace]
        switch kind {
        case .sight: places = response.sights ?? []
        case .restaurant: places = response.restaurants ?? []
        case .accommodation: places = response.accommodations ?? []
        case .pet: places = response.petPlaces ?? []
        }
        return places.enumerated().compactMap { index, place in
            liveEntry(place: place, kind: kind, city: city, petType: normalizedPetType, index: index)
        }
    }

    private func liveEntry(place: CatalogPlace, kind: CatalogCategory, city: String, petType: String, index: Int) -> CatalogEntry? {
        let name = place.name?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        guard !name.isEmpty else { return nil }
        let photoName = place.photoName?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        let photoNames = (place.photoNames ?? []).filter { !$0.isEmpty }
        let hasPhoto = place.photoURL?.isEmpty == false || !photoName.isEmpty || !photoNames.isEmpty
        if (kind == .sight || kind == .restaurant) && (place.rating == nil || !hasPhoto) { return nil }
        let stableID = place.placeID?.nonEmpty ?? "\(kind.rawValue):\(city):\(index):\(name)"
        let primaryType = place.type?.nonEmpty ?? place.category?.nonEmpty ?? ""
        let category: String
        switch kind {
        case .sight: category = place.category?.nonEmpty ?? primaryType
        case .restaurant: category = place.cuisine?.nonEmpty ?? place.category?.nonEmpty ?? primaryType
        case .accommodation, .pet: category = primaryType
        }
        return CatalogEntry(
            id: "google:\(stableID)",
            kind: kind,
            name: name,
            city: city,
            category: category,
            description: place.description ?? "",
            address: place.address ?? "",
            type: kind == .pet ? petType : primaryType,
            mapURL: place.mapURL ?? "",
            website: place.website ?? "",
            phone: place.phone ?? "",
            latitude: place.latitude,
            longitude: place.longitude,
            rating: place.rating,
            reviewCount: place.ratingCount,
            priceLevel: place.priceLevel,
            photoURL: place.photoURL,
            photoName: photoName,
            photoNames: photoNames.isEmpty && !photoName.isEmpty ? [photoName] : photoNames,
            photoAttribution: place.photoAttribution,
            source: "google",
            googlePlaceID: place.placeID ?? "",
            isLive: true,
            openNow: place.openNow,
        )
    }

    private func mergeSightEntries(live: [CatalogEntry], fallback: [CatalogEntry]) -> [CatalogEntry] {
        var result = live
        for entry in fallback {
            let entryName = normalizeCatalogText(entry.name)
            let alreadyPresent = live.contains { liveEntry in
                let liveName = normalizeCatalogText(liveEntry.name)
                return liveName == entryName || liveName.contains(entryName) || entryName.contains(liveName)
            }
            if !alreadyPresent { result.append(entry) }
        }
        return result
    }
}

private enum CatalogError: LocalizedError {
    case unavailable

    var errorDescription: String? {
        "Каталог временно недоступен. Попробуйте ещё раз."
    }
}

private func localizedName(language: String, ru: String?, en: String?, es: String?, de: String?) -> String {
    let languageCode = language.trimmingCharacters(in: .whitespacesAndNewlines).uppercased().prefix(2)
    switch languageCode {
    case "EN": return en?.nonEmpty ?? ru?.nonEmpty ?? es?.nonEmpty ?? de ?? ""
    case "ES": return es?.nonEmpty ?? en?.nonEmpty ?? ru?.nonEmpty ?? de ?? ""
    case "DE": return de?.nonEmpty ?? en?.nonEmpty ?? ru?.nonEmpty ?? es ?? ""
    default: return ru?.nonEmpty ?? en?.nonEmpty ?? es?.nonEmpty ?? de ?? ""
    }
}

private func catalogCityName(_ value: String) -> String {
    value
        .components(separatedBy: ",").first?
        .components(separatedBy: " — ").first?
        .trimmingCharacters(in: .whitespacesAndNewlines) ?? value.trimmingCharacters(in: .whitespacesAndNewlines)
}

private func normalizeCatalogText(_ value: String) -> String {
    value
        .trimmingCharacters(in: .whitespacesAndNewlines)
        .lowercased()
        .replacingOccurrences(of: "ё", with: "е")
        .replacingOccurrences(of: "\n", with: " ")
        .split(whereSeparator: { $0.isWhitespace })
        .joined(separator: " ")
}

private func queryComponent(_ value: String) -> String {
    var allowed = CharacterSet.alphanumerics
    allowed.insert(charactersIn: "-._~")
    return value.addingPercentEncoding(withAllowedCharacters: allowed) ?? value
}

private func placesLanguageCode(_ language: String) -> String {
    switch language.trimmingCharacters(in: .whitespacesAndNewlines).uppercased().prefix(2) {
    case "EN": return "en"
    case "ES": return "es"
    case "DE": return "de"
    default: return "ru"
    }
}

// MARK: - City catalog parity

/// The Android client keeps a curated city layer in front of its large world
/// catalog. Keeping the canonical layer in Swift gives iOS deterministic
/// aliases, coordinates and country flags while still allowing a user to type
/// an arbitrary free-form city name in a trip.
struct IOSCityCatalogEntry: Identifiable, Hashable, Sendable {
    let id: String
    let russian: String
    let english: String
    let spanish: String
    let german: String
    let latitude: Double
    let longitude: Double
    let flag: String
    let aliases: Set<String>

    func localizedName(language: String) -> String {
        switch language.trimmingCharacters(in: .whitespacesAndNewlines).uppercased().prefix(2) {
        case "EN": return english
        case "ES": return spanish
        case "DE": return german
        default: return russian
        }
    }

    var coordinate: Coordinate { Coordinate(latitude: latitude, longitude: longitude) }
}

final class IOSCityCatalogRepository {
    private let entries: [IOSCityCatalogEntry] = [
        Self.city("prague", "Прага", "Prague", "Praga", "Prag", 50.0755, 14.4378, "🇨🇿", "прага", "praha"),
        Self.city("salzburg", "Зальцбург", "Salzburg", "Salzburgo", "Salzburg", 47.8095, 13.0550, "🇦🇹"),
        Self.city("verona", "Верона", "Verona", "Verona", "Verona", 45.4384, 10.9916, "🇮🇹"),
        Self.city("rome", "Рим", "Rome", "Roma", "Rom", 41.9028, 12.4964, "🇮🇹"),
        Self.city("pisa", "Пиза", "Pisa", "Pisa", "Pisa", 43.7228, 10.4017, "🇮🇹"),
        Self.city("san-marino", "Сан-Марино", "San Marino", "San Marino", "San Marino", 43.9424, 12.4578, "🇸🇲"),
        Self.city("chioggia", "Кьоджа", "Chioggia", "Chioggia", "Chioggia", 45.2181, 12.2786, "🇮🇹"),
        Self.city("milan", "Милан", "Milan", "Milán", "Mailand", 45.4642, 9.1900, "🇮🇹"),
        Self.city("munich", "Мюнхен", "Munich", "Múnich", "München", 48.1351, 11.5820, "🇩🇪", "muenchen"),
        Self.city("vienna", "Вена", "Vienna", "Viena", "Wien", 48.2082, 16.3738, "🇦🇹"),
        Self.city("innsbruck", "Инсбрук", "Innsbruck", "Innsbruck", "Innsbruck", 47.2692, 11.4041, "🇦🇹"),
        Self.city("florence", "Флоренция", "Florence", "Florencia", "Florenz", 43.7696, 11.2558, "🇮🇹"),
        Self.city("venice", "Венеция", "Venice", "Venecia", "Venedig", 45.4408, 12.3155, "🇮🇹"),
        Self.city("tallinn", "Таллин", "Tallinn", "Tallin", "Tallinn", 59.4370, 24.7536, "🇪🇪"),
        Self.city("riga", "Рига", "Riga", "Riga", "Riga", 56.9496, 24.1052, "🇱🇻"),
        Self.city("vilnius", "Вильнюс", "Vilnius", "Vilna", "Vilnius", 54.6872, 25.2797, "🇱🇹"),
        Self.city("como", "Комо", "Como", "Como", "Como", 45.8080, 9.2600, "🇮🇹", "озеро комо", "lake como"),
        Self.city("bormio", "Бормио", "Bormio", "Bormio", "Bormio", 46.4670, 10.3740, "🇮🇹"),
        Self.city("berlin", "Берлин", "Berlin", "Berlín", "Berlin", 52.5200, 13.4050, "🇩🇪"),
        Self.city("amsterdam", "Амстердам", "Amsterdam", "Ámsterdam", "Amsterdam", 52.3676, 4.9041, "🇳🇱"),
        Self.city("barcelona", "Барселона", "Barcelona", "Barcelona", "Barcelona", 41.3874, 2.1686, "🇪🇸"),
        Self.city("madrid", "Мадрид", "Madrid", "Madrid", "Madrid", 40.4168, -3.7038, "🇪🇸"),
        Self.city("lisbon", "Лиссабон", "Lisbon", "Lisboa", "Lissabon", 38.7223, -9.1393, "🇵🇹"),
        Self.city("paris", "Париж", "Paris", "París", "Paris", 48.8566, 2.3522, "🇫🇷"),
        Self.city("london", "Лондон", "London", "Londres", "London", 51.5074, -0.1278, "🇬🇧"),
        Self.city("budapest", "Будапешт", "Budapest", "Budapest", "Budapest", 47.4979, 19.0402, "🇭🇺"),
        Self.city("istanbul", "Стамбул", "Istanbul", "Estambul", "Istanbul", 41.0082, 28.9784, "🇹🇷", "стамбул"),
        Self.city("zurich", "Цюрих", "Zurich", "Zúrich", "Zürich", 47.3769, 8.5417, "🇨🇭"),
        Self.city("helsinki", "Хельсинки", "Helsinki", "Helsinki", "Helsinki", 60.1699, 24.9384, "🇫🇮"),
        Self.city("dubai", "Дубай", "Dubai", "Dubái", "Dubai", 25.2048, 55.2708, "🇦🇪"),
        Self.city("tokyo", "Токио", "Tokyo", "Tokio", "Tokio", 35.6762, 139.6503, "🇯🇵"),
        Self.city("new-york", "Нью-Йорк", "New York", "Nueva York", "New York", 40.7128, -74.0060, "🇺🇸", "new york"),
    ]

    func search(query: String, language: String, limit: Int = 36) -> [IOSCityCatalogEntry] {
        let normalizedQuery = Self.normalize(query)
        return entries
            .compactMap { entry -> (entry: IOSCityCatalogEntry, score: Int)? in
                let candidates = entry.aliases
                let score = candidates.compactMap { candidate -> Int? in
                    let value = Self.normalize(candidate)
                    if normalizedQuery.isEmpty { return 0 }
                    if value == normalizedQuery { return 300 }
                    if value.hasPrefix(normalizedQuery) { return 200 }
                    if value.contains(normalizedQuery) { return 100 }
                    if normalizedQuery.count >= 4 && Self.editDistance(value, normalizedQuery) <= (normalizedQuery.count <= 6 ? 1 : 2) {
                        return 50
                    }
                    return nil
                }.max()
                guard let score else { return nil }
                return (entry, score)
            }
            .sorted {
                if $0.score != $1.score { return $0.score > $1.score }
                return $0.entry.localizedName(language: language) < $1.entry.localizedName(language: language)
            }
            .prefix(limit)
            .map { $0.entry }
    }

    func resolve(_ value: String) -> IOSCityCatalogEntry? {
        let normalized = Self.normalize(value.components(separatedBy: ",").first ?? value)
        return entries.first { $0.aliases.contains(normalized) }
    }

    private static func city(
        _ id: String,
        _ russian: String,
        _ english: String,
        _ spanish: String,
        _ german: String,
        _ latitude: Double,
        _ longitude: Double,
        _ flag: String,
        _ aliases: String...
    ) -> IOSCityCatalogEntry {
        let names = [id, russian, english, spanish, german] + aliases
        return IOSCityCatalogEntry(
            id: id,
            russian: russian,
            english: english,
            spanish: spanish,
            german: german,
            latitude: latitude,
            longitude: longitude,
            flag: flag,
            aliases: Set(names.map(Self.normalize)),
        )
    }

    private static func normalize(_ value: String) -> String {
        value
            .trimmingCharacters(in: .whitespacesAndNewlines)
            .lowercased()
            .replacingOccurrences(of: "ё", with: "е")
            .replacingOccurrences(of: "‑", with: "-")
            .split(whereSeparator: { $0.isWhitespace })
            .joined(separator: " ")
    }

    private static func editDistance(_ lhs: String, _ rhs: String) -> Int {
        let right = Array(rhs)
        var previous = Array(0...right.count)
        for (row, leftCharacter) in Array(lhs).enumerated() {
            var current = [row + 1]
            for (column, rightCharacter) in right.enumerated() {
                let substitution = previous[column] + (leftCharacter == rightCharacter ? 0 : 1)
                let insertion = current[column] + 1
                let deletion = previous[column + 1] + 1
                current.append(min(substitution, insertion, deletion))
            }
            previous = current
        }
        return previous.last ?? rhs.count
    }
}
