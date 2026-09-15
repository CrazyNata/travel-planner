import Foundation

struct TripRow: Decodable, Sendable {
    let id: String
    let payload: JSONValue
    let ownerID: String?
    let revision: Int?

    var payloadObject: [String: JSONValue] { payload.objectValue ?? [:] }

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
    let source: String
    let googlePlaceID: String
    let bookingPropertyID: String
    let externalURL: String
    let address: String
    let latitude: Double?
    let longitude: Double?
    let reviewCount: Int?
    let photoReference: String
    let website: String
    let phone: String
    let type: String
    let tripCityID: String
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
    let inputCurrencyRate: Double?
}

struct BudgetGroup: Identifiable, Hashable, Sendable {
    let id: String
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
    let dateDay: String
    let dateMonth: String
    let checkIn: String
    let checkOut: String
}

struct Sight: Identifiable, Hashable, Sendable {
    let id: String
    let name: String
    let city: String
    let category: String
    let description: String
    let photo: String
    let photos: [String]
    let rating: Double?
    let reviews: Int?
    let latitude: Double?
    let longitude: Double?
    let done: Bool
    let walkDay: Int
    let walkOrder: Int
    let link: String
}

struct SightDay: Identifiable, Hashable, Sendable {
    let id: String
    let title: String
    let photo: String
    let photoPosition: Int?
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
    let date: String
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
    let latitude: Double?
    let longitude: Double?
    let features: [String]
    let openNow: Bool?
    let mapsURL: String
    let reviewCount: Int?
    let note: String
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
    let sightNotes: [String: String]
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

    private func loadRow(id: String) async throws -> TripRow {
        let safeID = id.addingPercentEncoding(withAllowedCharacters: .urlQueryAllowed) ?? id
        let rows: [TripRow] = try await client.request(
            "rest/v1/trips?select=id,payload,owner_id,revision&id=eq.\(safeID)",
            method: "GET",
            body: nil as EmptyBody?,
            authenticated: true,
        )
        guard let row = rows.first else {
            throw SupabaseClientError.server(status: 404, message: "Путешествие не найдено.")
        }
        return row
    }

    private func patchPayload(id: String, patch: [String: JSONValue], expectedRevision: Int) async throws {
        guard !patch.isEmpty else { return }
        try await client.invokeRPC(
            "patch_trip_payload",
            body: [
                "p_trip_id": .string(id),
                "p_patch": .object(patch),
                "p_expected_revision": .number(Double(expectedRevision)),
            ],
        )
    }

    private func patchArray(
        id: String,
        section: String,
        current: TripRow,
        values: [JSONValue],
    ) async throws {
        try await patchPayload(
            id: id,
            patch: [section: .array(values)],
            expectedRevision: current.revision ?? 0,
        )
    }

    private func arrayItemID(_ value: JSONValue) -> String {
        let object = value.objectValue ?? [:]
        return object.text("id", fallback: object.text("name"))
    }

    private func itemIndex(
        in values: [JSONValue],
        itemID: String,
    ) -> Int? {
        values.firstIndex { arrayItemID($0) == itemID }
    }

    private func updateArrayItem(
        id: String,
        section: String,
        itemID: String,
        fields: [String: JSONValue],
    ) async throws {
        let current = try await loadRow(id: id)
        var values = current.payloadObject.array(section)
        guard let index = itemIndex(in: values, itemID: itemID), var object = values[index].objectValue else {
            throw SupabaseClientError.server(status: 404, message: "Элемент не найден.")
        }
        object.merge(fields) { _, new in new }
        values[index] = .object(object)
        try await patchArray(id: id, section: section, current: current, values: values)
    }

    private func removeArrayItem(
        id: String,
        section: String,
        itemID: String,
    ) async throws -> (TripRow, JSONValue?) {
        let current = try await loadRow(id: id)
        let values = current.payloadObject.array(section)
        let removed = values.first { arrayItemID($0) == itemID }
        let remaining = values.filter { arrayItemID($0) != itemID }
        guard removed != nil else {
            throw SupabaseClientError.server(status: 404, message: "Элемент не найден.")
        }
        try await patchArray(id: id, section: section, current: current, values: remaining)
        return (current, removed)
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

    func updateTripDetails(id: String, title: String, dates: String, cities: String) async throws {
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanTitle.isEmpty else {
            throw SupabaseClientError.invalidInput("Укажите название путешествия.")
        }
        let current = try await loadRow(id: id)
        let cleanDates = dates.trimmingCharacters(in: .whitespacesAndNewlines)
        var patch: [String: JSONValue] = [
            "title": .string(cleanTitle),
            "dates": .string(cleanDates),
            "cities": .string(cities.trimmingCharacters(in: .whitespacesAndNewlines)),
        ]
        let dateValues = dateTokens(in: dates)
        if !dateValues.isEmpty || cleanDates.isEmpty {
            patch["startDate"] = .string(dateValues.first ?? "")
            patch["endDate"] = .string(dateValues.dropFirst().first ?? "")
        }
        // Keep the complete existing route and all user-entered sections. The
        // editor changes only the trip-level metadata in this operation.
        try await patchPayload(id: id, patch: patch, expectedRevision: current.revision ?? 0)
    }

    func updateTripField(id: String, key: String, value: JSONValue) async throws {
        let cleanKey = key.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanKey.isEmpty else { throw SupabaseClientError.invalidInput("Поле поездки не указано.") }
        let current = try await loadRow(id: id)
        try await patchPayload(id: id, patch: [cleanKey: value], expectedRevision: current.revision ?? 0)
    }

    func addTripArrayItem(id: String, section: String, item: [String: JSONValue]) async throws {
        guard Self.editableArraySections.contains(section) else {
            throw SupabaseClientError.invalidInput("Недопустимый раздел поездки.")
        }
        let current = try await loadRow(id: id)
        var nextItem = item
        if nextItem["id"]?.stringValue?.isEmpty != false {
            nextItem["id"] = .string(UUID().uuidString.lowercased())
        }
        var values = current.payloadObject.array(section)
        values.append(.object(nextItem))
        try await patchArray(id: id, section: section, current: current, values: values)
    }

    func updateTripArrayItem(id: String, section: String, itemID: String, fields: [String: JSONValue]) async throws {
        guard Self.editableArraySections.contains(section) else {
            throw SupabaseClientError.invalidInput("Недопустимый раздел поездки.")
        }
        try await updateArrayItem(id: id, section: section, itemID: itemID, fields: fields)
    }

    func deleteTripItem(id: String, section: String, itemID: String) async throws {
        guard Self.editableArraySections.contains(section) else {
            throw SupabaseClientError.invalidInput("Недопустимый раздел поездки.")
        }
        if section == "members" {
            try await client.invokeRPC(
                "manage_trip_member",
                body: [
                    "p_trip_id": .string(id),
                    "p_member_id": .string(itemID),
                    "p_role": .null,
                    "p_delete": .boolean(true),
                ],
            )
            return
        }
        let result = try await removeArrayItem(id: id, section: section, itemID: itemID)
        if section == "coverPhotos", let reference = result.1?.objectValue?.text("image").nonEmpty {
            await client.deleteStorageReference(reference)
        }
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
        let cleanFrom = from.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanTo = to.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanFrom.isEmpty, !cleanTo.isEmpty else {
            throw SupabaseClientError.invalidInput("Укажите оба города маршрута.")
        }
        let current = try await loadRow(id: id)
        let day: [String: JSONValue] = [
            "id": .string(UUID().uuidString.lowercased()),
            "city": .string(cleanTo),
            "date": .string(date.trimmingCharacters(in: .whitespacesAndNewlines)),
            "places": .array([]),
            "roadLeg": .object([
                "from": .string(cleanFrom),
                "to": .string(cleanTo),
                "checkInFrom": .string(checkIn.trimmed),
                "checkOutFrom": .string(checkOut.trimmed),
                "notes": .string(notes.trimmed),
                "mapsUrl": .string(mapsURL.trimmed),
                "date": .string(date.trimmed),
                "dateDay": .string(dateDay.trimmed),
                "dateMonth": .string(dateMonth.trimmed),
                "weekday": .string(weekday.trimmed),
                "distance": .string(distance.trimmed),
                "travelTime": .string(travelTime.trimmed),
                "completed": .array([]),
            ]),
        ]
        var values = current.payloadObject.array("days")
        values.append(.object(day))
        try await patchArray(id: id, section: "days", current: current, values: values)
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
        let cleanFrom = from.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanTo = to.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanFrom.isEmpty, !cleanTo.isEmpty else {
            throw SupabaseClientError.invalidInput("Укажите оба города маршрута.")
        }
        let current = try await loadRow(id: id)
        let values = current.payloadObject.array("days")
        guard let index = itemIndex(in: values, itemID: dayID), var day = values[index].objectValue else {
            throw SupabaseClientError.server(status: 404, message: "Переезд не найден.")
        }
        var road = day.object("roadLeg")
        road["from"] = .string(cleanFrom)
        road["to"] = .string(cleanTo)
        road["checkInFrom"] = .string(checkIn.trimmed)
        road["checkOutFrom"] = .string(checkOut.trimmed)
        road["notes"] = .string(notes.trimmed)
        road["mapsUrl"] = .string(mapsURL.trimmed)
        road["date"] = .string(date.trimmed)
        road["dateDay"] = .string(dateDay.trimmed)
        road["dateMonth"] = .string(dateMonth.trimmed)
        road["weekday"] = .string(weekday.trimmed)
        road["distance"] = .string(distance.trimmed)
        road["travelTime"] = .string(travelTime.trimmed)
        day["city"] = .string(cleanTo)
        day["date"] = .string(date.trimmed)
        day["roadLeg"] = .object(road)
        var nextValues = values
        nextValues[index] = .object(day)
        try await patchArray(id: id, section: "days", current: current, values: nextValues)
    }

    func reorderRouteLegs(id: String, orderedDayIDs: [String]) async throws {
        guard !orderedDayIDs.isEmpty else { return }
        let current = try await loadRow(id: id)
        let days = current.payloadObject.array("days")
        let routeItems = days.filter { value in
            let road = value.objectValue?.object("roadLeg") ?? [:]
            return !road.text("from").isEmpty && !road.text("to").isEmpty
        }
        let routeIDs = routeItems.map(arrayItemID)
        guard Set(routeIDs) == Set(orderedDayIDs), routeIDs.count == orderedDayIDs.count else {
            throw SupabaseClientError.invalidInput("Маршрут изменился. Обновите экран и повторите попытку.")
        }
        let byID = Dictionary(uniqueKeysWithValues: days.map { (arrayItemID($0), $0) })
        let orderedRouteItems = orderedDayIDs.compactMap { byID[$0] }
        let routeIDSet = Set(routeIDs)
        // Preserve non-route day objects in their original positions.
        var routeIndex = 0
        let reordered = days.map { item -> JSONValue in
            guard routeIDSet.contains(arrayItemID(item)), routeIndex < orderedRouteItems.count else { return item }
            let replacement = orderedRouteItems[routeIndex]
            routeIndex += 1
            return replacement
        }
        if reordered != days {
            try await patchArray(id: id, section: "days", current: current, values: reordered)
        }
    }

    func reorderAccommodations(id: String, orderedAccommodationIDs: [String]) async throws {
        guard !orderedAccommodationIDs.isEmpty else { return }
        guard orderedAccommodationIDs.count == Set(orderedAccommodationIDs).count else {
            throw SupabaseClientError.invalidInput("Порядок жилья содержит дубликаты.")
        }
        let current = try await loadRow(id: id)
        let accommodations = current.payloadObject.array("accommodations")
        guard !accommodations.isEmpty else { return }

        let accommodationIDs = accommodations.map(arrayItemID)
        guard accommodationIDs.allSatisfy({ !$0.isEmpty }), accommodationIDs.count == Set(accommodationIDs).count,
              orderedAccommodationIDs.count == accommodationIDs.count,
              Set(orderedAccommodationIDs) == Set(accommodationIDs)
        else {
            throw SupabaseClientError.invalidInput("Список жилья изменился. Обновите экран и повторите попытку.")
        }

        let byID = Dictionary(uniqueKeysWithValues: accommodations.map { (arrayItemID($0), $0) })
        let reordered = orderedAccommodationIDs.compactMap { byID[$0] }
        let storedOrder = current.payloadObject.array("accommodationOrder").compactMap(\.stringValue)
        guard reordered != accommodations || storedOrder != orderedAccommodationIDs else { return }
        try await patchPayload(
            id: id,
            patch: [
                "accommodations": .array(reordered),
                "accommodationOrder": .array(orderedAccommodationIDs.map(JSONValue.string)),
            ],
            expectedRevision: current.revision ?? 0,
        )
    }

    private static let editableArraySections: Set<String> = [
        "days", "sights", "restaurants", "accommodations", "budgetExpenses", "members", "petPlaces", "coverPhotos",
    ]

    func addBudgetGroup(id: String, name: String, people: Int) async throws {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty, people > 0 else {
            throw SupabaseClientError.invalidInput("Укажите название группы и количество участников.")
        }
        let current = try await loadRow(id: id)
        var split = current.payloadObject.object("budgetSplit")
        var groups = split["groups"]?.arrayValue ?? []
        groups.append(.object([
            "id": .string(UUID().uuidString.lowercased()),
            "name": .string(cleanName),
            "people": .number(Double(people)),
        ]))
        split["groups"] = .array(groups)
        try await patchPayload(id: id, patch: ["budgetSplit": .object(split)], expectedRevision: current.revision ?? 0)
    }

    func updateBudgetGroup(id: String, groupID: String, name: String, people: Int) async throws {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanName.isEmpty, people > 0 else {
            throw SupabaseClientError.invalidInput("Укажите название группы и количество участников.")
        }
        let current = try await loadRow(id: id)
        var split = current.payloadObject.object("budgetSplit")
        var groups = split["groups"]?.arrayValue ?? []
        guard let index = groups.firstIndex(where: { group in
            let object = group.objectValue ?? [:]
            return object.text("id", fallback: object.text("name")) == groupID
        }), var group = groups[index].objectValue else {
            throw SupabaseClientError.server(status: 404, message: "Группа бюджета не найдена.")
        }
        group["id"] = .string(group.text("id", fallback: groupID))
        group["name"] = .string(cleanName)
        group["people"] = .number(Double(people))
        groups[index] = .object(group)
        split["groups"] = .array(groups)
        try await patchPayload(id: id, patch: ["budgetSplit": .object(split)], expectedRevision: current.revision ?? 0)
    }

    func deleteBudgetGroup(id: String, groupID: String) async throws {
        let current = try await loadRow(id: id)
        let groups = current.payloadObject.object("budgetSplit").array("groups")
        let remaining = groups.filter { group in
            let object = group.objectValue ?? [:]
            return object.text("id", fallback: object.text("name")) != groupID
        }
        guard remaining.count != groups.count else {
            throw SupabaseClientError.server(status: 404, message: "Группа бюджета не найдена.")
        }
        var split = current.payloadObject.object("budgetSplit")
        split["groups"] = .array(remaining)
        try await patchPayload(id: id, patch: ["budgetSplit": .object(split)], expectedRevision: current.revision ?? 0)
    }

    func updateMemberRole(id: String, memberID: String, role: String) async throws {
        guard role == "Редактор" || role == "Читатель" else {
            throw SupabaseClientError.invalidInput("Недопустимая роль участника.")
        }
        try await client.invokeRPC(
            "manage_trip_member",
            body: [
                "p_trip_id": .string(id),
                "p_member_id": .string(memberID),
                "p_role": .string(role),
                "p_delete": .boolean(false),
            ],
        )
    }

    func removeMember(id: String, memberID: String) async throws {
        try await client.invokeRPC(
            "manage_trip_member",
            body: [
                "p_trip_id": .string(id),
                "p_member_id": .string(memberID),
                "p_role": .null,
                "p_delete": .boolean(true),
            ],
        )
    }

    func inviteMember(id: String, name: String, email: String, role: String) async throws {
        let cleanName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        let cleanEmail = email.trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        guard !cleanName.isEmpty else { throw SupabaseClientError.invalidInput("Укажите имя участника.") }
        guard cleanEmail.contains("@") else { throw SupabaseClientError.invalidInput("Укажите корректный e-mail.") }
        guard role == "Редактор" || role == "Читатель" else {
            throw SupabaseClientError.invalidInput("Недопустимая роль участника.")
        }
        let body: [String: JSONValue] = [
            "email": .string(cleanEmail),
            "name": .string(cleanName),
            "role": .string(role),
            "tripId": .string(id),
            "redirectTo": .string("https://ramingo.online/mobile/invite?tripId=\(id)"),
        ]
        try await client.invokeFunction("send-invite", body: body)
    }

    func addCatalogItem(id: String, entry: CatalogEntry, walkDay: Int) async throws {
        let current = try await loadRow(id: id)
        let section = entry.kind.tripSection
        var item = entry.tripPayload(walkDay: max(walkDay, 1))
        var values = current.payloadObject.array(section)
        if section == "sights" {
            item["walkOrder"] = .number(Double(values.count))
        }
        if section == "petPlaces", item["type"] == nil { item["type"] = .string("shop") }
        let isDuplicate = values.contains { value in
            let object = value.objectValue ?? [:]
            let existingCatalogID = object.text("catalogId")
            let sameName = object.text("name").caseInsensitiveCompare(entry.name) == .orderedSame
            let sameCity = object.text("city").caseInsensitiveCompare(entry.city) == .orderedSame
            return (!entry.id.isEmpty && existingCatalogID == entry.id) || (sameName && sameCity)
        }
        guard !isDuplicate else { return }
        values.append(.object(item))
        try await patchArray(id: id, section: section, current: current, values: values)
    }

    func reorderSights(id: String, orderedSightIDs: [String]) async throws {
        guard !orderedSightIDs.isEmpty else { return }
        let current = try await loadRow(id: id)
        let sights = current.payloadObject.array("sights")
        let sightIDs = sights.map(arrayItemID)
        guard orderedSightIDs.count == sightIDs.count,
              orderedSightIDs.count == Set(orderedSightIDs).count,
              Set(orderedSightIDs) == Set(sightIDs)
        else {
            throw SupabaseClientError.invalidInput("Список мест изменился. Обновите экран и повторите попытку.")
        }
        let order = Dictionary(uniqueKeysWithValues: orderedSightIDs.enumerated().map { ($1, $0) })
        let next = sights.map { value -> JSONValue in
            guard var object = value.objectValue,
                  let itemID = object.text("id").nonEmpty,
                  let walkOrder = order[itemID]
            else { return value }
            object["walkOrder"] = .number(Double(walkOrder))
            return .object(object)
        }
        if next != sights {
            try await patchArray(id: id, section: "sights", current: current, values: next)
        }
    }

    func reorderSightDays(id: String, currentDayIDs: [String], orderedDayIDs: [String]) async throws {
        guard !currentDayIDs.isEmpty, !orderedDayIDs.isEmpty else { return }
        guard Set(currentDayIDs) == Set(orderedDayIDs), currentDayIDs.count == orderedDayIDs.count else {
            throw SupabaseClientError.invalidInput("Дни изменились. Обновите экран и повторите попытку.")
        }
        let current = try await loadRow(id: id)
        let dayValues = current.payloadObject.array("sightDays")
        guard !dayValues.isEmpty else { return }
        let byID = Dictionary(uniqueKeysWithValues: dayValues.enumerated().map { index, value in
            ((value.objectValue ?? [:]).text("id", fallback: "sights-day-\(index + 1)"), value)
        })
        guard currentDayIDs.allSatisfy({ byID[$0] != nil }) else { return }
        let nextDays = orderedDayIDs.compactMap { byID[$0] }
        var nextSights = current.payloadObject.array("sights")
        let dayIndexByID = Dictionary(uniqueKeysWithValues: orderedDayIDs.enumerated().map { ($1, $0 + 1) })
        let oldDayIDByIndex = Dictionary(uniqueKeysWithValues: currentDayIDs.enumerated().map { ($0 + 1, $1) })
        nextSights = nextSights.map { value in
            guard var object = value.objectValue,
                  let oldDay = object.integer("walkDay"),
                  let oldDayID = oldDayIDByIndex[oldDay],
                  let newDay = dayIndexByID[oldDayID]
            else { return value }
            object["walkDay"] = .number(Double(newDay))
            return .object(object)
        }
        try await patchPayload(
            id: id,
            patch: [
                "sightDays": .array(nextDays),
                "sightDaysVersion": .number(1),
                "sights": .array(nextSights),
            ],
            expectedRevision: current.revision ?? 0,
        )
    }

    func updateSightNotes(id: String, dayID: String, notes: String) async throws {
        guard !dayID.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
            throw SupabaseClientError.invalidInput("День достопримечательностей не указан.")
        }
        let current = try await loadRow(id: id)
        var notesByDay = current.payloadObject.object("sightNotes")
        let cleaned = notes.trimmingCharacters(in: .whitespacesAndNewlines)
        if cleaned.isEmpty {
            notesByDay.removeValue(forKey: dayID)
        } else {
            notesByDay[dayID] = .string(cleaned)
        }
        try await patchPayload(id: id, patch: ["sightNotes": .object(notesByDay)], expectedRevision: current.revision ?? 0)
    }

    func deleteSightDay(id: String, walkDay: Int) async throws {
        let dayNumber = max(walkDay, 1)
        let current = try await loadRow(id: id)
        var patch = [String: JSONValue]()
        let sights = current.payloadObject.array("sights")
        let remainingSights = sights.compactMap { value -> JSONValue? in
            guard var object = value.objectValue else { return value }
            guard let itemDay = object.integer("walkDay") else { return value }
            if itemDay == dayNumber { return nil }
            if itemDay > dayNumber { object["walkDay"] = .number(Double(itemDay - 1)) }
            return .object(object)
        }
        if remainingSights != sights { patch["sights"] = .array(remainingSights) }

        let days = current.payloadObject.array("sightDays")
        if dayNumber <= days.count {
            let removedDayID = arrayItemID(days[dayNumber - 1])
            var remainingDays = days
            remainingDays.remove(at: dayNumber - 1)
            patch["sightDays"] = .array(remainingDays)
            patch["sightDaysVersion"] = .number(1)
            if !removedDayID.isEmpty {
                var notes = current.payloadObject.object("sightNotes")
                notes.removeValue(forKey: removedDayID)
                patch["sightNotes"] = .object(notes)
            }
        }
        try await patchPayload(id: id, patch: patch, expectedRevision: current.revision ?? 0)
    }

    func addCoverPhoto(id: String, data: Data, city: String) async throws {
        guard !data.isEmpty else { throw SupabaseClientError.invalidInput("Не удалось прочитать изображение.") }
        guard let userID = client.currentUser?.id else { throw SupabaseClientError.cancelled }
        let current = try await loadRow(id: id)
        let path = "\(userID)/\(id)/covers/\(UUID().uuidString.lowercased()).jpg"
        try await client.uploadStorageObject(path: path, data: data)
        let reference = "storage://trip-photos/\(path)"
        do {
            var photos = current.payloadObject.array("coverPhotos")
            photos.append(.object([
                "id": .string(UUID().uuidString.lowercased()),
                "image": .string(reference),
                "city": .string(city.trimmed),
            ]))
            var patch: [String: JSONValue] = ["coverPhotos": .array(photos)]
            if current.payloadObject.text("coverImage").isEmpty {
                patch["coverImage"] = .string(reference)
            }
            try await patchPayload(id: id, patch: patch, expectedRevision: current.revision ?? 0)
        } catch {
            try? await client.deleteStorageObject(path: path)
            throw error
        }
    }

    func deleteCoverPhoto(id: String, photoID: String) async throws {
        let current = try await loadRow(id: id)
        let photos = current.payloadObject.array("coverPhotos")
        if photos.isEmpty, photoID == "legacy", let reference = current.payloadObject.text("coverImage").nonEmpty {
            try await patchPayload(id: id, patch: ["coverImage": .string("")], expectedRevision: current.revision ?? 0)
            await client.deleteStorageReference(reference)
            return
        }
        guard let index = itemIndex(in: photos, itemID: photoID) else {
            throw SupabaseClientError.server(status: 404, message: "Фото не найдено.")
        }
        let removed = photos[index]
        var remaining = photos
        remaining.remove(at: index)
        var patch: [String: JSONValue] = ["coverPhotos": .array(remaining)]
        if current.payloadObject.text("coverImage") == removed.objectValue?.text("image") {
            let nextCover = remaining.first.flatMap { $0.objectValue?.text("image").nonEmpty }
            patch["coverImage"] = nextCover.map(JSONValue.string) ?? .string("")
        }
        try await patchPayload(id: id, patch: patch, expectedRevision: current.revision ?? 0)
        if let reference = removed.objectValue?.text("image").nonEmpty {
            await client.deleteStorageReference(reference)
        }
    }

    func addItemPhoto(id: String, section: String, itemID: String, data: Data) async throws {
        guard section == "sights" || section == "restaurants" || section == "accommodations" else {
            throw SupabaseClientError.invalidInput("Для этого раздела нельзя добавить несколько фотографий.")
        }
        guard !data.isEmpty else { throw SupabaseClientError.invalidInput("Не удалось прочитать изображение.") }
        guard let userID = client.currentUser?.id else { throw SupabaseClientError.cancelled }
        let current = try await loadRow(id: id)
        let values = current.payloadObject.array(section)
        guard let index = itemIndex(in: values, itemID: itemID), var item = values[index].objectValue else {
            throw SupabaseClientError.server(status: 404, message: "Элемент не найден.")
        }
        let path = "\(userID)/\(id)/\(section)/\(itemID)/\(UUID().uuidString.lowercased()).jpg"
        try await client.uploadStorageObject(path: path, data: data)
        let reference = "storage://trip-photos/\(path)"
        do {
            var photos = item.array("photos")
            photos.append(.string(reference))
            item["photos"] = .array(photos)
            if section == "sights", item.text("photo").isEmpty {
                item["photo"] = .string(reference)
            }
            var nextValues = values
            nextValues[index] = .object(item)
            try await patchArray(id: id, section: section, current: current, values: nextValues)
        } catch {
            try? await client.deleteStorageObject(path: path)
            throw error
        }
    }

    func replaceItemCoverPhoto(id: String, section: String, itemID: String, data: Data) async throws {
        guard section == "sights" || section == "restaurants" || section == "accommodations" else {
            throw SupabaseClientError.invalidInput("Для этого раздела нельзя заменить фотографию.")
        }
        guard !data.isEmpty else { throw SupabaseClientError.invalidInput("Не удалось прочитать изображение.") }
        guard let userID = client.currentUser?.id else { throw SupabaseClientError.cancelled }
        let current = try await loadRow(id: id)
        let values = current.payloadObject.array(section)
        guard let index = itemIndex(in: values, itemID: itemID), var item = values[index].objectValue else {
            throw SupabaseClientError.server(status: 404, message: "Элемент не найден.")
        }
        let oldReference = section == "sights"
            ? item.text("photo").nonEmpty
            : item.array("photos").first?.stringValue?.nonEmpty
        let path = "\(userID)/\(id)/\(section)/\(itemID)/\(UUID().uuidString.lowercased()).jpg"
        try await client.uploadStorageObject(path: path, data: data)
        let reference = "storage://trip-photos/\(path)"
        do {
            if section == "sights" {
                item["photo"] = .string(reference)
                item["photoName"] = .string("")
                if item["photos"] != nil {
                    var photos = item.array("photos")
                    if photos.isEmpty { photos = [.string(reference)] } else { photos[0] = .string(reference) }
                    item["photos"] = .array(photos)
                }
            } else {
                var photos = item.array("photos")
                if photos.isEmpty { photos = [.string(reference)] } else { photos[0] = .string(reference) }
                item["photos"] = .array(photos)
            }
            var nextValues = values
            nextValues[index] = .object(item)
            try await patchArray(id: id, section: section, current: current, values: nextValues)
        } catch {
            try? await client.deleteStorageObject(path: path)
            throw error
        }
        if let oldReference, oldReference != reference {
            await client.deleteStorageReference(oldReference)
        }
    }

    func moveItemPhoto(id: String, section: String, itemID: String, photoIndex: Int, direction: Int) async throws {
        guard direction == -1 || direction == 1 else { return }
        let current = try await loadRow(id: id)
        var values = current.payloadObject.array(section)
        guard let index = itemIndex(in: values, itemID: itemID), var item = values[index].objectValue else {
            throw SupabaseClientError.server(status: 404, message: "Элемент не найден.")
        }
        var photos = item.array("photos")
        let target = photoIndex + direction
        guard photoIndex >= 0, photoIndex < photos.count, target >= 0, target < photos.count else { return }
        photos.swapAt(photoIndex, target)
        item["photos"] = .array(photos)
        values[index] = .object(item)
        try await patchArray(id: id, section: section, current: current, values: values)
    }

    func deleteItemPhoto(id: String, section: String, itemID: String, photoIndex: Int) async throws {
        let current = try await loadRow(id: id)
        var values = current.payloadObject.array(section)
        guard let index = itemIndex(in: values, itemID: itemID), var item = values[index].objectValue else {
            throw SupabaseClientError.server(status: 404, message: "Элемент не найден.")
        }
        var photos = item.array("photos")
        if section == "sights", photos.isEmpty, let reference = item.text("photo").nonEmpty {
            photos = [.string(reference)]
        }
        guard photoIndex >= 0, photoIndex < photos.count else {
            throw SupabaseClientError.server(status: 404, message: "Фото не найдено.")
        }
        let removed = photos.remove(at: photoIndex)
        item["photos"] = .array(photos)
        if section == "sights" {
            let nextPhoto = photos.first.flatMap { $0.stringValue }
            item["photo"] = nextPhoto.map(JSONValue.string) ?? .string("")
        }
        values[index] = .object(item)
        try await patchArray(id: id, section: section, current: current, values: values)
        if let reference = removed.stringValue?.nonEmpty {
            await client.deleteStorageReference(reference)
        }
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
            cityCoordinates: payload.object("cityCoordinates").compactMapValues { value in
                let object = value.objectValue ?? [:]
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
                guard let name = object.text("name").nonEmpty else { return nil }
                return BudgetGroup(
                    id: object.text("id", fallback: name),
                    name: name,
                    people: max(object.integer("people") ?? 1, 1),
                )
            },
            members: members(from: payload),
            sights: sights(from: payload),
            sightDays: payload.array("sightDays").enumerated().compactMap { index, value in
                let object = value.objectValue ?? [:]
                guard let title = object.text("title", fallback: object.text("city")).nonEmpty else { return nil }
                return SightDay(
                    id: object.text("id", fallback: "sights-day-\(index + 1)"),
                    title: title,
                    photo: object.text("photo"),
                    photoPosition: object.integer("photoPosition"),
                )
            },
            restaurants: restaurants(from: payload),
            petPlaces: pets(from: payload),
            sightNotes: payload.object("sightNotes").compactMapValues { $0.stringValue },
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
                date: day.text("date", fallback: road.text("date")),
                weekday: road.text("weekday", fallback: road.text("weekDay")),
                distance: road.text("distance", fallback: road.text("km")),
                travelTime: road.text("travelTime", fallback: road.text("hr")),
                notes: road.text("notes"),
                mapsURL: road.text("mapsUrl"),
                dateDay: road.text("dateDay", fallback: day.text("dateDay")),
                dateMonth: road.text("dateMonth", fallback: day.text("dateMonth")),
                checkIn: road.text("checkInFrom", fallback: road.text("checkIn")),
                checkOut: road.text("checkOutFrom", fallback: road.text("checkOut")),
            )
        }
    }

    private func accommodations(from payload: [String: JSONValue]) -> [Accommodation] {
        let parsed = payload.array("accommodations").compactMap { value -> Accommodation? in
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
                source: object.text("source", fallback: "manual"),
                googlePlaceID: object.text("googlePlaceId", fallback: object.text("googlePlaceID")),
                bookingPropertyID: object.text("bookingPropertyId", fallback: object.text("booking_property_id")),
                externalURL: object.text("externalUrl", fallback: object.text("externalURL")),
                address: object.text("address"),
                latitude: object.number("latitude"),
                longitude: object.number("longitude"),
                reviewCount: firstInteger(object, keys: ["reviewCount", "ratingCount", "reviews"]),
                photoReference: object.text("photoReference", fallback: object.text("photoName")),
                website: object.text("website"),
                phone: object.text("phone"),
                type: object.text("type"),
                tripCityID: object.text("tripCityId", fallback: object.text("tripCityID")),
            )
        }
        let explicitOrder = payload.array("accommodationOrder").compactMap(\.stringValue).filter { !$0.isEmpty }
        guard !explicitOrder.isEmpty else { return parsed }
        let byID = Dictionary(uniqueKeysWithValues: parsed.map { ($0.id, $0) })
        let ordered = explicitOrder.compactMap { byID[$0] }
        let orderedIDs = Set(ordered.map(\.id))
        return ordered + parsed.filter { !orderedIDs.contains($0.id) }
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
                inputCurrencyRate: object.number("inputCurrencyRate"),
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
        let parsed = payload.array("sights").enumerated().compactMap { index, value -> Sight? in
            let object = value.objectValue ?? [:]
            guard let name = object.text("name").nonEmpty else { return nil }
            let lngLat = object.array("lnglat").compactMap(\.doubleValue)
            let photos = photoReferences(object, keys: ["photos", "photoNames", "photo_names", "googlePhotos", "photo", "image", "photoUrl", "imageUrl", "photoName"])
            return Sight(
                id: object.text("id", fallback: name),
                name: name,
                city: object.text("city"),
                category: object.text("subcategory", fallback: object.text("group")),
                description: object.text("description"),
                photo: photos.first ?? "",
                photos: photos,
                rating: firstNumber(object, keys: ["rating", "googleRating", "userRating", "score"]),
                reviews: firstInteger(object, keys: ["ratingCount", "googleReviews", "reviewCount", "userRatingCount"]),
                latitude: object.number("latitude") ?? lngLat.dropFirst().first,
                longitude: object.number("longitude") ?? lngLat.first,
                done: object.flag("done") ?? false,
                walkDay: max(object.integer("walkDay") ?? 1, 1),
                walkOrder: object.integer("walkOrder") ?? index,
                link: firstText(object, keys: ["link", "mapsUrl", "externalUrl", "url"]) ?? "",
            )
        }
        return parsed.sorted {
            if $0.walkDay != $1.walkDay { return $0.walkDay < $1.walkDay }
            if $0.walkOrder != $1.walkOrder { return $0.walkOrder < $1.walkOrder }
            return $0.id < $1.id
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
                date: object.text("date"),
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
                latitude: object.number("latitude"),
                longitude: object.number("longitude"),
                features: object.array("features").compactMap(\.stringValue),
                openNow: object.flag("openNow"),
                mapsURL: object.text("mapsUrl", fallback: object.text("googleMapsUrl")),
                reviewCount: firstInteger(object, keys: ["reviewCount", "ratingCount", "reviews"]),
                note: object.text("note"),
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

private func dateTokens(in value: String) -> [String] {
    guard let regex = try? NSRegularExpression(pattern: #"\d{4}-\d{2}-\d{2}|\d{1,2}[./]\d{1,2}[./]\d{4}"#) else {
        return []
    }
    let range = NSRange(value.startIndex..<value.endIndex, in: value)
    return regex.matches(in: value, range: range).compactMap { match in
        guard let range = Range(match.range, in: value) else { return nil }
        return String(value[range])
    }
}

private extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}
