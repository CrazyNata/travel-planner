import Foundation
import UserNotifications

struct ReminderScheduler {
    static let reminderHour = 9

    private static let reminderPrefix = "ramingo.reminder."
    private static let tripReminderDays = [30, 14, 7, 3, 1]
    private static let cancellationReminderDays = [7, 3, 1, 0]

    private struct Event {
        let identifier: String
        let tripID: String
        let triggerDate: Date
        let title: String
        let body: String
    }

    static func requestAuthorization() async throws -> Bool {
        try await withCheckedThrowingContinuation { continuation in
            UNUserNotificationCenter.current().requestAuthorization(options: [.alert, .badge, .sound]) { granted, error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: granted)
                }
            }
        }
    }

    static func sync(
        trips: [TripOverview],
        profile: AccountProfile,
        now: Date = Date(),
    ) async {
        let center = UNUserNotificationCenter.current()
        let pending = await pendingRequests(center)
        let identifiers = pending
            .map(\.identifier)
            .filter { $0.hasPrefix(reminderPrefix) }
        if !identifiers.isEmpty {
            center.removePendingNotificationRequests(withIdentifiers: identifiers)
        }

        guard profile.notificationsEnabled else { return }
        let settings = await notificationSettings(center)
        guard settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional else { return }

        let events = trips
            .filter { $0.status.isFinished == false }
            .flatMap {
                planEvents(
                    for: $0,
                    language: profile.language,
                    reminderHour: profile.reminderHour,
                    now: now,
                    tripRemindersEnabled: profile.tripRemindersEnabled,
                    cancellationRemindersEnabled: profile.cancellationRemindersEnabled,
                )
            }

        for event in events {
            let content = UNMutableNotificationContent()
            content.title = event.title
            content.body = event.body
            content.sound = .default
            content.userInfo = ["tripID": event.tripID]
            let components = Calendar.current.dateComponents([.calendar, .timeZone, .year, .month, .day, .hour, .minute], from: event.triggerDate)
            let request = UNNotificationRequest(
                identifier: "\(reminderPrefix)\(event.identifier)",
                content: content,
                trigger: UNCalendarNotificationTrigger(dateMatching: components, repeats: false),
            )
            try? await add(request, to: center)
        }
    }

    static func cancelAll() async {
        let center = UNUserNotificationCenter.current()
        let pending = await pendingRequests(center)
        let identifiers = pending
            .map(\.identifier)
            .filter { $0.hasPrefix(reminderPrefix) }
        if !identifiers.isEmpty {
            center.removePendingNotificationRequests(withIdentifiers: identifiers)
        }
    }

    private static func planEvents(
        for trip: TripOverview,
        language: String,
        reminderHour: Int,
        now: Date,
        tripRemindersEnabled: Bool,
        cancellationRemindersEnabled: Bool,
    ) -> [Event] {
        let calendar = Calendar.current
        let dates = extractDates(from: trip.dates)
        guard let start = dates.first else {
            return cancellationRemindersEnabled
                ? cancellationEvents(for: trip, language: language, reminderHour: reminderHour, now: now)
                : []
        }

        var events = [Event]()
        let title = trip.title.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty ?? localized(language, ru: "Путешествие", en: "Trip", es: "Viaje", de: "Reise")
        if tripRemindersEnabled {
            for daysBefore in tripReminderDays {
                guard let target = calendar.date(byAdding: .day, value: -daysBefore, to: start),
                      let trigger = reminderDate(on: target, hour: reminderHour, calendar: calendar),
                      trigger > now else { continue }
                let body = localized(
                    language,
                    ru: "До путешествия «\(title)» осталось \(daysBefore) \(russianDayWord(daysBefore))",
                    en: "\(daysBefore) \(daysBefore == 1 ? "day" : "days") left until «\(title)»",
                    es: "Faltan \(daysBefore) \(daysBefore == 1 ? "día" : "días") para «\(title)»",
                    de: "Noch \(daysBefore) \(daysBefore == 1 ? "Tag" : "Tage") bis «\(title)»",
                )
                events.append(Event(
                    identifier: "trip-\(trip.id)-\(daysBefore)",
                    tripID: trip.id,
                    triggerDate: trigger,
                    title: localized(language, ru: "Скоро путешествие", en: "Trip reminder", es: "Recordatorio del viaje", de: "Reiseerinnerung"),
                    body: body,
                ))
            }
        }
        if cancellationRemindersEnabled {
            events.append(contentsOf: cancellationEvents(for: trip, language: language, reminderHour: reminderHour, now: now))
        }
        return events
    }

    private static func cancellationEvents(
        for trip: TripOverview,
        language: String,
        reminderHour: Int,
        now: Date,
    ) -> [Event] {
        let calendar = Calendar.current
        return trip.accommodations
            .filter { $0.status.isStayed == false }
            .flatMap { accommodation -> [Event] in
                guard let deadline = extractDates(from: accommodation.deadline).first else { return [] }
                let name = [accommodation.name, accommodation.city]
                    .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
                    .filter { !$0.isEmpty }
                    .joined(separator: " · ")
                return cancellationReminderDays.compactMap { daysBefore -> Event? in
                    guard let target = calendar.date(byAdding: .day, value: -daysBefore, to: deadline),
                          let trigger = reminderDate(on: target, hour: reminderHour, calendar: calendar),
                          trigger > now else { return nil }
                    let body: String
                    if daysBefore == 0 {
                        body = localized(
                            language,
                            ru: "Сегодня заканчивается бесплатная отмена «\(name)»",
                            en: "Free cancellation for «\(name)» ends today",
                            es: "La cancelación gratuita de «\(name)» termina hoy",
                            de: "Die kostenlose Stornierung für «\(name)» endet heute",
                        )
                    } else {
                        body = localized(
                            language,
                            ru: "До конца бесплатной отмены «\(name)» осталось \(daysBefore) \(russianDayWord(daysBefore))",
                            en: "\(daysBefore) \(daysBefore == 1 ? "day" : "days") left to cancel «\(name)» for free",
                            es: "Quedan \(daysBefore) \(daysBefore == 1 ? "día" : "días") para cancelar «\(name)» gratis",
                            de: "Noch \(daysBefore) \(daysBefore == 1 ? "Tag" : "Tage"), um «\(name)» kostenlos zu stornieren",
                        )
                    }
                    return Event(
                        identifier: "cancellation-\(trip.id)-\(accommodation.id)-\(daysBefore)",
                        tripID: trip.id,
                        triggerDate: trigger,
                        title: localized(language, ru: "Срок бесплатной отмены", en: "Free cancellation deadline", es: "Fecha límite de cancelación gratuita", de: "Frist für kostenlose Stornierung"),
                        body: body,
                    )
                }
            }
    }

    private static func reminderDate(on date: Date, hour: Int, calendar: Calendar) -> Date? {
        var components = calendar.dateComponents([.year, .month, .day], from: date)
        components.hour = min(max(hour, 0), 23)
        components.minute = 0
        components.second = 0
        return calendar.date(from: components)
    }

    private static func extractDates(from value: String) -> [Date] {
        let source = value.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !source.isEmpty else { return [] }
        var results = [Date]()
        let calendar = Calendar.current

        if let regex = try? NSRegularExpression(pattern: #"(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)"#) {
            let range = NSRange(source.startIndex..<source.endIndex, in: source)
            for match in regex.matches(in: source, range: range) {
                guard let year = integer(match, in: source, group: 1),
                      let month = integer(match, in: source, group: 2),
                      let day = integer(match, in: source, group: 3),
                      let date = makeDate(year: year, month: month, day: day, calendar: calendar)
                else { continue }
                results.append(date)
            }
        }
        if !results.isEmpty { return results }

        if let regex = try? NSRegularExpression(pattern: #"(?<!\d)(\d{1,2})[./](\d{1,2})[./](\d{4})(?!\d)"#) {
            let range = NSRange(source.startIndex..<source.endIndex, in: source)
            for match in regex.matches(in: source, range: range) {
                guard let day = integer(match, in: source, group: 1),
                      let month = integer(match, in: source, group: 2),
                      let year = integer(match, in: source, group: 3),
                      let date = makeDate(year: year, month: month, day: day, calendar: calendar)
                else { continue }
                results.append(date)
            }
        }
        if !results.isEmpty { return results }

        guard let regex = try? NSRegularExpression(pattern: #"(?i)(?<!\d)(\d{1,2})\s+([A-Za-zА-Яа-яЁёÄÖÜäöüßÑñ]+)\s+(\d{4})(?!\d)"#) else { return [] }
        let range = NSRange(source.startIndex..<source.endIndex, in: source)
        for match in regex.matches(in: source, range: range) {
            guard let day = integer(match, in: source, group: 1),
                  let monthName = string(match, in: source, group: 2),
                  let year = integer(match, in: source, group: 3),
                  let month = monthNumber(monthName),
                  let date = makeDate(year: year, month: month, day: day, calendar: calendar)
            else { continue }
            results.append(date)
        }
        return results
    }

    private static func makeDate(year: Int, month: Int, day: Int, calendar: Calendar) -> Date? {
        calendar.date(from: DateComponents(year: year, month: month, day: day))
    }

    private static func integer(_ match: NSTextCheckingResult, in value: String, group: Int) -> Int? {
        string(match, in: value, group: group).flatMap(Int.init)
    }

    private static func string(_ match: NSTextCheckingResult, in value: String, group: Int) -> String? {
        guard let range = Range(match.range(at: group), in: value) else { return nil }
        return String(value[range])
    }

    private static func monthNumber(_ value: String) -> Int? {
        let key = value.lowercased()
        return [
            "января": 1, "январь": 1, "янв": 1,
            "февраля": 2, "февраль": 2, "фев": 2,
            "марта": 3, "март": 3, "мар": 3,
            "апреля": 4, "апрель": 4, "апр": 4,
            "мая": 5, "май": 5,
            "июня": 6, "июнь": 6, "июн": 6,
            "июля": 7, "июль": 7, "июл": 7,
            "августа": 8, "август": 8, "авг": 8,
            "сентября": 9, "сентябрь": 9, "сен": 9, "сент": 9,
            "октября": 10, "октябрь": 10, "окт": 10,
            "ноября": 11, "ноябрь": 11, "ноя": 11,
            "декабря": 12, "декабрь": 12, "дек": 12,
            "january": 1, "jan": 1, "enero": 1, "ene": 1, "januar": 1,
            "february": 2, "feb": 2, "febrero": 2, "februar": 2,
            "march": 3, "marzo": 3, "märz": 3, "maerz": 3,
            "april": 4, "apr": 4, "abril": 4,
            "may": 5, "mayo": 5, "mai": 5,
            "june": 6, "jun": 6, "junio": 6, "juni": 6,
            "july": 7, "jul": 7, "julio": 7, "juli": 7,
            "august": 8, "aug": 8, "agosto": 8,
            "september": 9, "sep": 9, "septiembre": 9,
            "october": 10, "oct": 10, "octubre": 10, "oktober": 10,
            "november": 11, "nov": 11, "noviembre": 11,
            "december": 12, "dec": 12, "diciembre": 12, "dezember": 12,
        ][key]
    }

    private static func pendingRequests(_ center: UNUserNotificationCenter) async -> [UNNotificationRequest] {
        await withCheckedContinuation { continuation in
            center.getPendingNotificationRequests { continuation.resume(returning: $0) }
        }
    }

    static func notificationSettings(_ center: UNUserNotificationCenter = .current()) async -> UNNotificationSettings {
        await withCheckedContinuation { continuation in
            center.getNotificationSettings { continuation.resume(returning: $0) }
        }
    }

    private static func add(_ request: UNNotificationRequest, to center: UNUserNotificationCenter) async throws {
        try await withCheckedThrowingContinuation { (continuation: CheckedContinuation<Void, Error>) in
            center.add(request) { error in
                if let error {
                    continuation.resume(throwing: error)
                } else {
                    continuation.resume(returning: ())
                }
            }
        }
    }

    private static func localized(_ language: String, ru: String, en: String, es: String, de: String) -> String {
        switch language.trimmingCharacters(in: .whitespacesAndNewlines).uppercased().prefix(2) {
        case "EN": return en
        case "ES": return es
        case "DE": return de
        default: return ru
        }
    }

    private static func russianDayWord(_ value: Int) -> String {
        if value % 10 == 1 && value % 100 != 11 { return "день" }
        if (2...4).contains(value % 10) && !(12...14).contains(value % 100) { return "дня" }
        return "дней"
    }
}

private extension String {
    var isFinished: Bool {
        let value = trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return ["заверш", "прошед", "completed", "past", "finished"].contains { value.contains($0) }
    }

    var isStayed: Bool {
        let value = trimmingCharacters(in: .whitespacesAndNewlines).lowercased()
        return ["пожил", "stayed", "visited", "past"].contains { value.contains($0) }
    }
}
