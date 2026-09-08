import Foundation

struct TripRow: Decodable, Sendable {
    let id: String
    let payload: JSONValue
    let ownerID: String?
    let revision: Int?

    enum CodingKeys: String, CodingKey {
        case id
        case payload
        case ownerID = "owner_id"
        case revision
    }
}

struct Coordinate: Hashable, Sendable {
    let latitude: Double
    let longitude: Double
}

struct CoverPhoto: Identifiable, Hashable, Sendable {
    let id: String
    let reference: String
    let city: String
}

struct Accommodation: Identifiable, Hashable, Sendable {
    let id: String
    let city: String
    let name: String
    let dates: String
    let price: String
    let status: String
    let details: String
    let photos: [String]
    let bookingURL: String
    let deadline: String
    let rating: Double?
}

struct BudgetExpense: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let amount: Double
    let category: String
    let scope: String
    let paidBy: String
    let date: String
    let inputCurrency: String
}

struct BudgetGroup: Hashable, Sendable {
    let name: String
    let people: Int
}

struct TripMember: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let email: String
    let role: String
    let initials: String
    let tone: String
}

struct RouteLeg: Identifiable, Hashable, Sendable {
    let id: String
    let from: String
    let to: String
    let date: String
    let weekday: String
    let distance: String
    let travelTime: String
    let notes: String
    let mapsURL: String
}

struct Sight: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let city: String
    let category: String
    let description: String
    let photo: String
    let rating: Double?
    let reviews: Int?
    let latitude: Double?
    let longitude: Double?
    let done: Bool
}

struct SightDay: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let photo: String
}

struct Restaurant: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let city: String
    let status: String
    let photos: [String]
    let rating: Double?
    let reviews: String
    let price: String
    let note: String
    let link: String
    let priority: Bool
}

struct PetPlace: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let city: String
    let type: String
    let address: String
    let phone: String
    let website: String
    let photo: String
    let rating: Double?
    let features: [String]
    let openNow: Bool?
}

struct TripSummary: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let dates: String
    let status: String
    let progress: Int
    let cities: String
    let coverReference: String?
    let isOwner: Bool
    let canEdit: Bool
    let deletedAt: String?
}

enum TripSection: String, CaseIterable, Identifiable {
    case overview
    case route
    case sights
    case restaurants
    case accommodation
    case budget
    case members
    case photos
    case pets

    var id: String { rawValue }

    var title: String {
        switch self {
        case .overview: return "Обзор"
        case .route: return "Маршрут"
        case .sights: return "Места"
        case .restaurants: return "Рестораны"
        case .accommodation: return "Жильё"
        case .budget: return "Бюджет"
        case .members: return "Участники"
        case .photos: return "Фото"
        case .pets: return "Питомцы"
        }
    }

    var icon: String {
        switch self {
        case .overview: return "sparkles"
        case .route: return "point.topleft.down.to.point.bottomright.curvepath"
        case .sights: return "building.columns"
        case .restaurants: return "fork.knife"
        case .accommodation: return "bed.double"
        case .budget: return "creditcard"
        case .members: return "person.2"
        case .photos: return "photo.on.rectangle"
        case .pets: return "pawprint"
        }
    }
}

struct TripOverview: Identifiable, Sendable {
    let id: String
    let title: String
    let dates: String
    let status: String
    let progress: Int
    let cities: [String]
    let cityCoordinates: [String: Coordinate]
    let coverPhotos: [CoverPhoto]
    let routeLegs: [RouteLeg]
    let accommodations: [Accommodation]
    let budgetCurrency: String
    let budgetExpenses: [BudgetExpense]
    let budgetGroups: [BudgetGroup]
    let members: [TripMember]
    let sights: [Sight]
    let sightDays: [SightDay]
    let restaurants: [Restaurant]
    let petPlaces: [PetPlace]
    let canEdit: Bool
    let currentUserRole: String
}

private struct TripInsert: Encodable {
    let id: String
    let ownerID: String
    let payload: [String: JSONValue]

    enum CodingKeys: String, CodingKey {
        case id
        case ownerID = "owner_id"
        case payload
    }
}

final class TripRepository {
    private let client: SupabaseClient

    init(client: SupabaseClient) {
        self.client = client
    }

    func loadTrips() async throws -> [TripSummary] {
        let rows: [TripRow] = try await client.request(
            "rest/v1/trips?select=id,payload,owner_id,revision",
            method: "GET",
            body: nil as EmptyBody?,
            authenticated: true,
        )
        return rows.map(makeSummary)
    }

    func loadOverview(id: String) async throws -> TripOverview? {
        let safeID = id.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? id
        let rows: [TripRow] = try await client.request(
            "rest/v1/trips?select=id,payload,owner_id,revision&id=eq.\(safeID)",
            method: "GET",
            body: nil as EmptyBody?,
            authenticated: true,
        )
        return rows.first.map(makeOverview)
    }

    func createTrip(title: String, startDate: String, endDate: String, cities: String) async throws -> TripSummary {
        guard let owner = client.currentUser else { throw SupabaseClientError.cancelled }
        let tripID = UUID().uuidString.lowercased()
        let cityList = cities.split(separator: ",").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty }
        let dates = [startDate, endDate].filter { !$0.isEmpty }.joined(separator: " — ")
        let ownerName = owner.displayName
        let payload: [String: JSONValue] = [
            "id": .string(tripID),
            "title": .string(title.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? "Без названия"),
            "startDate": .string(startDate),
            "endDate": .string(endDate),
            "dates": .string(dates),
            "cities": .string(cities.trimmingCharacters(in: .whitespacesAndNewlines)),
            "status": .string("Черновик"),
            "isDraft": .boolean(true),
            "tone": .string("purple"),
            "overviewBlocks": .array([.string("photo"), .string("map"), .string("weather")]),
            "overviewMapPoints": .array(cityList.map(JSONValue.string)),
            "cityCoordinates": .object([:]),
            "budgetCurrency": .string("EUR"),
            "budgetManualRates": .object([:]),
            "budgetSplit": .object(["groups": .array([])]),
            "coverPhotos": .array([]),
            "sights": .array([]),
            "restaurants": .array([]),
            "petPlaces": .array([]),
            "accommodations": .array([]),
            "budgetExpenses": .array([]),
            "members": .array([
                .object([
                    "id": .string(owner.id),
                    "name": .string(ownerName),
                    "email": .string(owner.email ?? ""),
                    "role": .string("Владелец"),
                    "initials": .string(String(ownerName.prefix(2)).uppercased()),
                    "tone": .string("purple"),
                ]),
            ]),
            "days": .array(cityList.dropFirst().enumerated().map { index, city in
                .object([
                    "id": .string(UUID().uuidString.lowercased()),
                    "city": .string(city),
                    "date": .string(""),
                    "places": .array([]),
                    "roadLeg": .object([
                        "from": .string(cityList[index]),
                        "to": .string(city),
                        "checkInFrom": .string(""),
                        "checkInTo": .string(""),
                        "checkOutFrom": .string(""),
                        "checkOutTo": .string(""),
                        "notes": .string(""),
                        "mapsUrl": .string(""),
                        "dateDay": .string(""),
                        "dateMonth": .string(""),
                        "weekday": .string(""),
                        "distance": .string(""),
                        "travelTime": .string(""),
                        "completed": .array([]),
                    ]),
                    "dayNumber": .number(Double(index + 1)),
                ])
            }),
            "progress": .number(0),
        ]
        let _: [TripRow] = try await client.request(
            "rest/v1/trips",
            method: "POST",
            body: [TripInsert(id: tripID, ownerID: owner.id, payload: payload)],
            authenticated: true,
            extraHeaders: ["Prefer": "return=representation"],
        )
        return TripSummary(
            id: tripID,
            title: payload["title"]?.stringValue ?? "Без названия",
            dates: dates,
            status: "Черновик",
            progress: 0,
            cities: cities,
            coverReference: nil,
            isOwner: true,
            canEdit: true,
            deletedAt: nil,
        )
    }

    private func makeSummary(_ row: TripRow) -> TripSummary {
        let payload = row.payload.objectValue ?? [:]
        let role = role(for: payload, row: row)
        return TripSummary(
            id: row.id,
            title: payload.text("title", fallback: "Путешествие"),
            dates: payload.text("dates", fallback: [payload.text("startDate"), payload.text("endDate")].filter { !$0.isEmpty }.joined(separator: " — ")),
            status: payload.text("status", fallback: "Черновик"),
            progress: min(max(payload.integer("progress") ?? 0, 0), 100),
            cities: payload.text("cities"),
            coverReference: coverReferences(from: payload).first,
            isOwner: row.ownerID == client.currentUser?.id,
            canEdit: role == "Владелец" || role == "Редактор",
            deletedAt: payload.text("deletedAt").nonEmpty,
        )
    }

    private func makeOverview(_ row: TripRow) -> TripOverview {
        let payload = row.payload.objectValue ?? [:]
        let role = role(for: payload, row: row)
        let covers = payload.array("coverPhotos").compactMap { value -> CoverPhoto? in
            let object = value.objectValue ?? [:]
            guard let reference = firstText(object, keys: ["image", "imageUrl", "photo", "photoUrl", "url"])?.nonEmpty else { return nil }
            return CoverPhoto(id: object.text("id", fallback: reference), reference: reference, city: object.text("city"))
        }
        let coverPhotos = covers.isEmpty && !payload.text("coverImage").isEmpty
            ? [CoverPhoto(id: "legacy", reference: payload.text("coverImage"), city: "")]
            : covers

        return TripOverview(
            id: row.id,
            title: payload.text("title", fallback: "Путешествие"),
            dates: payload.text("dates"),
            status: payload.text("status", fallback: "Черновик"),
            progress: min(max(payload.integer("progress") ?? 0, 0), 100),
            cities: payload.text("cities").split(separator: ",").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }.filter { !$0.isEmpty },
            cityCoordinates: payload.object("cityCoordinates").compactMapValues { object in
                guard let latitude = object.number("latitude"), let longitude = object.number("longitude") else { return nil }
                return Coordinate(latitude: latitude, longitude: longitude)
            },
            coverPhotos: coverPhotos,
            routeLegs: routeLegs(from: payload),
            accommodations: accommodations(from: payload),
            budgetCurrency: payload.text("budgetCurrency", fallback: "EUR"),
            budgetExpenses: expenses(from: payload),
            budgetGroups: payload.object("budgetSplit").array("groups").compactMap { value in
                let object = value.objectValue ?? [:]
                let name = object.text("name").nonEmpty else { return nil }
                return BudgetGroup(name: name, people: max(object.integer("people") ?? 1, 1))
            },
            members: members(from: payload),
            sights: sights(from: payload),
            sightDays: payload.array("sightDays").enumerated().compactMap { index, value in
                let object = value.objectValue ?? [:]
                let title = object.text("title", fallback: object.text("city")).nonEmpty else { return nil }
                return SightDay(id: object.text("id", fallback: "sights-day-\(index + 1)"), title: title, photo: object.text("photo"))
            },
            restaurants: restaurants(from: payload),
            petPlaces: pets(from: payload),
            canEdit: role == "Владелец" || role == "Редактор",
            currentUserRole: role,
        )
    }

    private func role(for payload: [String: JSONValue], row: TripRow) -> String {
        if row.ownerID == client.currentUser?.id { return "Владелец" }
        let email = client.currentUser?.email?.lowercased() ?? ""
        return payload.array("members").compactMap(\.objectValue).first(where: {
            $0.text("userId") == client.currentUser?.id || (!email.isEmpty && $0.text("email").lowercased() == email)
        })?.text("role") ?? "Читатель"
    }

    private func coverReferences(from payload: [String: JSONValue]) -> [String] {
        var values = [payload.text("coverImage")]
        values += payload.array("coverPhotos").compactMap { firstText($0.objectValue ?? [:], keys: ["image", "imageUrl", "photo", "photoUrl", "url"]) }
        return values.filter { !$0.isEmpty }
    }

    private func routeLegs(from payload: [String: JSONValue]) -> [RouteLeg] {
        payload.array("days").enumerated().compactMap { index, value in
            let day = value.objectValue ?? [:]
            let road = day.object("roadLeg")
            guard let from = road.text("from").nonEmpty, let to = road.text("to").nonEmpty else { return nil }
            return RouteLeg(
                id: day.text("id", fallback: "route-\(index + 1)"),
                from: from,
                to: to,
                date: day.text("date"),
                weekday: road.text("weekday", fallback: road.text("weekDay")),
                distance: road.text("distance", fallback: road.text("km")),
                travelTime: road.text("travelTime", fallback: road.text("hr")),
                notes: road.text("notes"),
                mapsURL: road.text("mapsUrl"),
            )
        }
    }

    private func accommodations(from payload: [String: JSONValue]) -> [Accommodation] {
        payload.array("accommodations").compactMap { value in
            let object = value.objectValue ?? [:]
            guard let name = object.text("name").nonEmpty else { return nil }
            return Accommodation(
                id: object.text("id", fallback: name),
                city: object.text("city"),
                name: name,
                dates: object.text("dates"),
                price: object.text("price"),
                status: object.text("status"),
                details: object.text("details", fallback: object.text("address")),
                photos: photoReferences(object, keys: ["photos", "photoNames", "photo_names", "googlePhotos", "photo", "image", "imageUrl", "photoUrl"]),
                bookingURL: object.text("bookingUrl", fallback: object.text("externalUrl")),
                deadline: object.text("deadline"),
                rating: firstNumber(object, keys: ["rating", "hotelRating", "googleRating", "userRating", "score"]),
            )
        }
    }

    private func expenses(from payload: [String: JSONValue]) -> [BudgetExpense] {
        payload.array("budgetExpenses").compactMap { value in
            let object = value.objectValue ?? [:]
            guard let name = object.text("name").nonEmpty else { return nil }
            return BudgetExpense(
                id: object.text("id", fallback: name),
                name: name,
                amount: object.number("amount") ?? 0,
                category: object.text("category", fallback: "Прочее"),
                scope: object.text("scope"),
                paidBy: object.text("paidBy"),
                date: object.text("date"),
                inputCurrency: object.text("inputCurrency"),
            )
        }
    }

    private func members(from payload: [String: JSONValue]) -> [TripMember] {
        payload.array("members").compactMap { value in
            let object = value.objectValue ?? [:]
            guard let name = object.text("name").nonEmpty else { return nil }
            return TripMember(
                id: object.text("id", fallback: name),
                name: name,
                email: object.text("email"),
                role: object.text("role"),
                initials: object.text("initials", fallback: String(name.prefix(2)).uppercased()),
                tone: object.text("tone"),
            )
        }
    }

    private func sights(from payload: [String: JSONValue]) -> [Sight] {
        payload.array("sights").compactMap { value in
            let object = value.objectValue ?? [:]
            guard let name = object.text("name").nonEmpty else { return nil }
            let lngLat = object.array("lnglat").compactMap(\.doubleValue)
            return Sight(
                id: object.text("id", fallback: name),
                name: name,
                city: object.text("city"),
                category: object.text("subcategory", fallback: object.text("group")),
                description: object.text("description"),
                photo: firstText(object, keys: ["photo", "image", "photoUrl", "imageUrl"]) ?? firstPhotoReference(object),
                rating: firstNumber(object, keys: ["rating", "googleRating", "userRating", "score"]),
                reviews: firstInteger(object, keys: ["ratingCount", "googleReviews", "reviewCount", "userRatingCount"]),
                latitude: object.number("latitude") ?? lngLat.dropFirst().first,
                longitude: object.number("longitude") ?? lngLat.first,
                done: object.flag("done") ?? false,
            )
        }
    }

    private func restaurants(from payload: [String: JSONValue]) -> [Restaurant] {
        payload.array("restaurants").compactMap { value in
            let object = value.objectValue ?? [:]
            guard let name = object.text("name").nonEmpty else { return nil }
            return Restaurant(
                id: object.text("id", fallback: name),
                name: name,
                city: object.text("city"),
                status: object.text("status"),
                photos: photoReferences(object, keys: ["photos", "photoNames", "photo_names", "googlePhotos", "photo", "image", "imageUrl", "photoUrl"]),
                rating: firstNumber(object, keys: ["googleRating", "rating", "userRating", "score"]),
                reviews: firstText(object, keys: ["googleReviews", "reviews", "ratingCount", "reviewCount", "userRatingCount"]) ?? "",
                price: object.text("price", fallback: object.text("priceLevel")),
                note: object.text("note", fallback: object.text("cuisine")),
                link: firstText(object, keys: ["link", "address", "mapsUrl", "googleMapsUrl", "url"]) ?? "",
                priority: object.flag("priority") ?? false,
            )
        }
    }

    private func pets(from payload: [String: JSONValue]) -> [PetPlace] {
        payload.array("petPlaces").compactMap { value in
            let object = value.objectValue ?? [:]
            guard let name = object.text("name").nonEmpty else { return nil }
            return PetPlace(
                id: object.text("id", fallback: name),
                name: name,
                city: object.text("city"),
                type: object.text("type", fallback: "shop"),
                address: object.text("address"),
                phone: object.text("phone"),
                website: object.text("website"),
                photo: firstText(object, keys: ["photoUrl", "photo", "imageUrl", "image"]) ?? firstPhotoReference(object),
                rating: firstNumber(object, keys: ["rating", "googleRating", "userRating", "score"]),
                features: object.array("features").compactMap(\.stringValue),
                openNow: object.flag("openNow"),
            )
        }
    }

    private func firstText(_ object: [String: JSONValue], keys: [String]) -> String? {
        for key in keys {
            if let value = object[key]?.stringValue?.trimmingCharacters(in: .whitespacesAndNewlines), let nonEmpty = value.nonEmpty {
                return nonEmpty
            }
        }
        return nil
    }

    private func firstNumber(_ object: [String: JSONValue], keys: [String]) -> Double? {
        keys.lazy.compactMap { object[$0]?.doubleValue }.first
    }

    private func firstInteger(_ object: [String: JSONValue], keys: [String]) -> Int? {
        keys.lazy.compactMap { object[$0]?.intValue }.first
    }

    private func firstPhotoReference(_ object: [String: JSONValue]) -> String {
        photoReferences(object, keys: ["photos", "photoNames", "photo_names", "googlePhotos"]).first ?? ""
    }

    private func photoReferences(_ object: [String: JSONValue], keys: [String]) -> [String] {
        keys.flatMap { references(from: object[$0]) }.filter { !$0.isEmpty }.uniqued()
    }

    private func references(from value: JSONValue?) -> [String] {
        guard let value else { return [] }
        switch value {
        case .string(let string): return [string.trimmingCharacters(in: .whitespacesAndNewlines)].filter { !$0.isEmpty }
        case .number, .boolean, .null: return []
        case .array(let values): return values.flatMap(references)
        case .object(let object): return ["url", "image", "photo", "imageUrl", "photoUrl", "image_url", "photo_url", "src", "path", "photoName", "photo_name", "name"].flatMap { references(from: object[$0]) }
        }
    }
}

private extension Array where Element: Hashable {
    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}

private struct EmptyBody: Encodable {}
