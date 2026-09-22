import Foundation

struct WeatherSnapshot: Hashable, Sendable {
    let temperature: String
    let condition: String
    let tripTemperature: String?
    let tripCondition: String?
    let tripIsEstimate: Bool
    let tripDays: [String: WeatherDaySnapshot]
}

struct WeatherDaySnapshot: Hashable, Sendable {
    let temperature: String?
    let condition: String?
    let isEstimate: Bool
}

private struct TripWeather {
    let temperature: String
    let condition: String
    let isEstimate: Bool
}

private struct WeatherResponse: Decodable {
    let current: CurrentWeather
    let daily: DailyWeather?
}

private struct CurrentWeather: Decodable {
    let temperature: Double
    let weatherCode: Int

    enum CodingKeys: String, CodingKey {
        case temperature = "temperature_2m"
        case weatherCode = "weather_code"
    }
}

private struct DailyWeather: Decodable {
    let time: [String]
    let maxTemperature: [Double]
    let meanTemperature: [Double]
    let weatherCodes: [Int]
    let precipitation: [Double]
    let cloudCover: [Double]

    enum CodingKeys: String, CodingKey {
        case time
        case maxTemperature = "temperature_2m_max"
        case meanTemperature = "temperature_2m_mean"
        case weatherCodes = "weather_code"
        case precipitation = "precipitation_sum"
        case cloudCover = "cloud_cover_mean"
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.container(keyedBy: CodingKeys.self)
        time = try container.decodeIfPresent([String].self, forKey: .time) ?? []
        maxTemperature = try container.decodeIfPresent([Double].self, forKey: .maxTemperature) ?? []
        meanTemperature = try container.decodeIfPresent([Double].self, forKey: .meanTemperature) ?? []
        weatherCodes = try container.decodeIfPresent([Int].self, forKey: .weatherCodes) ?? []
        precipitation = try container.decodeIfPresent([Double].self, forKey: .precipitation) ?? []
        cloudCover = try container.decodeIfPresent([Double].self, forKey: .cloudCover) ?? []
    }
}

private struct DailyOnlyResponse: Decodable {
    let daily: DailyWeather?
}

private struct GeocodingResponse: Decodable {
    let results: [GeocodingResult]?
}

private struct GeocodingResult: Decodable {
    let latitude: Double
    let longitude: Double
}

final class WeatherRepository {
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func loadCurrent(
        cities: [String],
        tripDates: String,
        coordinates: [String: Coordinate],
    ) async -> [String: WeatherSnapshot] {
        let uniqueCities = cities.reduce(into: [String]()) { result, city in
            let value = city.trimmingCharacters(in: .whitespacesAndNewlines)
            guard !value.isEmpty,
                  !result.contains(where: { $0.caseInsensitiveCompare(value) == .orderedSame })
            else { return }
            result.append(value)
        }

        return await withTaskGroup(of: (String, WeatherSnapshot)?.self) { group in
            for city in uniqueCities {
                group.addTask {
                    guard let coordinate = await self.resolveCoordinate(for: city, coordinates: coordinates),
                          let snapshot = try? await self.load(city: city, coordinate: coordinate, tripDates: tripDates)
                    else { return nil }
                    return (city, snapshot)
                }
            }

            var result: [String: WeatherSnapshot] = [:]
            for await item in group {
                if let item {
                    result[item.0] = item.1
                }
            }
            return result
        }
    }

    func resolveCoordinates(
        cities: [String],
        known: [String: Coordinate],
    ) async -> [String: Coordinate] {
        var result = known
        for city in cities.uniqued() where !city.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            if result[city] != nil { continue }
            if let coordinate = await resolveCoordinate(for: city, coordinates: result) {
                result[city] = coordinate
            }
        }
        return result
    }

    private func load(city: String, coordinate: Coordinate, tripDates: String) async throws -> WeatherSnapshot {
        var components = URLComponents(string: "https://api.open-meteo.com/v1/forecast")!
        components.queryItems = [
            URLQueryItem(name: "latitude", value: String(coordinate.latitude)),
            URLQueryItem(name: "longitude", value: String(coordinate.longitude)),
            URLQueryItem(name: "current", value: "temperature_2m,weather_code"),
            URLQueryItem(name: "daily", value: "temperature_2m_max,weather_code"),
            URLQueryItem(name: "forecast_days", value: "16"),
            URLQueryItem(name: "timezone", value: "auto"),
        ]
        let (data, response) = try await requestData(from: components.url!)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw WeatherError.requestFailed }
        let weather = try JSONDecoder().decode(WeatherResponse.self, from: data)
        let tripDateRange = iosWeatherTripDateRange(tripDates)
        let targetDate = tripDateRange?.0
        let tripDatesToLoad = tripDateRange.map { iosWeatherTripDates(tripDates) } ?? []
        var tripDays = weatherTripDays(from: weather.daily)
        let missingTripDates = tripDatesToLoad.filter { tripDays[iosWeatherISODate($0)] == nil }
        var calendar = Calendar(identifier: .gregorian)
        calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
        let today = calendar.startOfDay(for: Date())
        let forecastEnd = calendar.date(byAdding: .day, value: 15, to: today) ?? today

        let archivedDates = missingTripDates.filter { $0 < today }
        if !archivedDates.isEmpty,
           let archived = try? await loadArchivedTripWeather(
               coordinate: coordinate,
               startDate: archivedDates[0],
               endDate: archivedDates[archivedDates.count - 1],
           ) {
            for (date, trip) in archived {
                tripDays[date] = WeatherDaySnapshot(
                    temperature: trip.temperature,
                    condition: trip.condition,
                    isEstimate: trip.isEstimate,
                )
            }
        }

        let climateDates = missingTripDates.filter { $0 > forecastEnd }
        if !climateDates.isEmpty,
           let climate = try? await loadClimateTripWeather(
               coordinate: coordinate,
               startDate: climateDates[0],
               endDate: climateDates[climateDates.count - 1],
           ) {
            for (date, trip) in climate {
                tripDays[date] = WeatherDaySnapshot(
                    temperature: trip.temperature,
                    condition: trip.condition,
                    isEstimate: trip.isEstimate,
                )
            }
        }

        let firstTripDay = targetDate.flatMap { tripDays[iosWeatherISODate($0)] }
        return WeatherSnapshot(
            temperature: "\(Int(weather.current.temperature.rounded()))°C",
            condition: condition(for: weather.current.weatherCode),
            tripTemperature: firstTripDay?.temperature,
            tripCondition: firstTripDay?.condition,
            tripIsEstimate: firstTripDay?.isEstimate == true,
            tripDays: tripDays,
        )
    }

    private func resolveCoordinate(for city: String, coordinates: [String: Coordinate]) async -> Coordinate? {
        if let exact = coordinates[city] { return exact }
        if let matching = coordinates.first(where: { $0.key.caseInsensitiveCompare(city) == .orderedSame })?.value {
            return matching
        }
        var components = URLComponents(string: "https://geocoding-api.open-meteo.com/v1/search")!
        components.queryItems = [
            URLQueryItem(name: "name", value: city.replacingOccurrences(of: " — ", with: ", ")),
            URLQueryItem(name: "count", value: "1"),
            URLQueryItem(name: "language", value: "en"),
            URLQueryItem(name: "format", value: "json"),
        ]
        guard let url = components.url,
              let (data, response) = try? await requestData(from: url),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let decoded = try? JSONDecoder().decode(GeocodingResponse.self, from: data),
              let result = decoded.results?.first
        else { return nil }
        return Coordinate(latitude: result.latitude, longitude: result.longitude)
    }

    private func loadArchivedTripWeather(
        coordinate: Coordinate,
        startDate: Date,
        endDate: Date,
    ) async throws -> [String: TripWeather] {
        let day = ISO8601DateFormatter()
        day.formatOptions = [.withFullDate]
        var components = URLComponents(string: "https://archive-api.open-meteo.com/v1/archive")!
        components.queryItems = [
            URLQueryItem(name: "latitude", value: String(coordinate.latitude)),
            URLQueryItem(name: "longitude", value: String(coordinate.longitude)),
            URLQueryItem(name: "start_date", value: day.string(from: startDate)),
            URLQueryItem(name: "end_date", value: day.string(from: endDate)),
            URLQueryItem(name: "daily", value: "temperature_2m_max,weather_code"),
            URLQueryItem(name: "timezone", value: "auto"),
        ]
        let (data, response) = try await requestData(from: components.url!)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw WeatherError.requestFailed }
        let archive = try JSONDecoder().decode(DailyOnlyResponse.self, from: data)
        return tripWeatherDays(in: archive.daily)
    }

    private func loadClimateTripWeather(
        coordinate: Coordinate,
        startDate: Date,
        endDate: Date,
    ) async throws -> [String: TripWeather] {
        let day = ISO8601DateFormatter()
        day.formatOptions = [.withFullDate]
        var components = URLComponents(string: "https://climate-api.open-meteo.com/v1/climate")!
        components.queryItems = [
            URLQueryItem(name: "latitude", value: String(coordinate.latitude)),
            URLQueryItem(name: "longitude", value: String(coordinate.longitude)),
            URLQueryItem(name: "start_date", value: day.string(from: startDate)),
            URLQueryItem(name: "end_date", value: day.string(from: endDate)),
            URLQueryItem(name: "models", value: "EC_Earth3P_HR"),
            URLQueryItem(name: "daily", value: "temperature_2m_mean,precipitation_sum,cloud_cover_mean"),
            URLQueryItem(name: "timezone", value: "auto"),
        ]
        let (data, response) = try await requestData(from: components.url!)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw WeatherError.requestFailed }
        let climate = try JSONDecoder().decode(DailyOnlyResponse.self, from: data)
        guard let daily = climate.daily else { throw WeatherError.requestFailed }
        var result: [String: TripWeather] = [:]
        for (index, date) in daily.time.enumerated() {
            let temperature = daily.meanTemperature[safe: index].map { "\(Int($0.rounded()))°C" }
            let precipitation = daily.precipitation[safe: index] ?? 0
            let cloudCover = daily.cloudCover[safe: index] ?? 0
            let condition = precipitation >= 1 ? "Дождь" : cloudCover >= 65 ? "Облачно" : "Ясно"
            if temperature != nil || !condition.isEmpty {
                result[date] = TripWeather(
                    temperature: temperature ?? "—",
                    condition: condition,
                    isEstimate: true,
                )
            }
        }
        return result
    }

    private func tripWeatherDays(in daily: DailyWeather?) -> [String: TripWeather] {
        guard let daily else { return [:] }
        var result: [String: TripWeather] = [:]
        for (index, date) in daily.time.enumerated() {
            let temperature = daily.maxTemperature[safe: index].map { "\(Int($0.rounded()))°C" }
            let condition = daily.weatherCodes[safe: index].map { self.condition(for: $0) }
            if temperature != nil || condition != nil {
                result[date] = TripWeather(
                    temperature: temperature ?? "—",
                    condition: condition ?? "—",
                    isEstimate: false,
                )
            }
        }
        return result
    }

    private func weatherTripDays(from daily: DailyWeather?) -> [String: WeatherDaySnapshot] {
        guard let daily else { return [:] }
        var result: [String: WeatherDaySnapshot] = [:]
        for (index, date) in daily.time.enumerated() {
            let temperature = daily.maxTemperature[safe: index].map { "\(Int($0.rounded()))°C" }
            let condition = daily.weatherCodes[safe: index].map { self.condition(for: $0) }
            if temperature != nil || condition != nil {
                result[date] = WeatherDaySnapshot(temperature: temperature, condition: condition, isEstimate: false)
            }
        }
        return result
    }

    private func condition(for code: Int) -> String {
        switch code {
        case 0: return "Ясно"
        case 1, 2, 3: return "Облачно"
        case 45, 48: return "Туман"
        case 51, 53, 55, 56, 57: return "Морось"
        case 61, 63, 65, 66, 67, 80, 81, 82: return "Дождь"
        case 71, 73, 75, 77, 85, 86: return "Снег"
        case 95, 96, 99: return "Гроза"
        default: return "—"
        }
    }

    private func requestData(from url: URL) async throws -> (Data, URLResponse) {
        var request = URLRequest(url: url)
        request.timeoutInterval = 15
        return try await session.data(for: request)
    }
}

func iosWeatherTripDateRange(_ value: String) -> (Date, Date)? {
    let normalizedValue = value
        .replacingOccurrences(of: "\u{00A0}", with: " ")
        .replacingOccurrences(of: "\u{202F}", with: " ")
        .replacingOccurrences(of: "\u{2009}", with: " ")
        .trimmingCharacters(in: .whitespacesAndNewlines)
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current

    let isoFormatter = DateFormatter()
    isoFormatter.locale = Locale(identifier: "en_US_POSIX")
    isoFormatter.calendar = calendar
    isoFormatter.timeZone = calendar.timeZone
    isoFormatter.dateFormat = "yyyy-MM-dd"
    let isoDates = weatherRegexMatches(#"(?<!\d)\d{4}-\d{2}-\d{2}(?!\d)"#, in: normalizedValue)
        .compactMap { isoFormatter.date(from: $0) }
    if let first = isoDates.first {
        return (first, isoDates.dropFirst().first ?? first)
    }

    let dottedFormatter = DateFormatter()
    dottedFormatter.locale = Locale(identifier: "en_US_POSIX")
    dottedFormatter.calendar = calendar
    dottedFormatter.timeZone = calendar.timeZone
    dottedFormatter.dateFormat = "dd.MM.yyyy"
    let dottedDates = weatherRegexMatches(#"(?<!\d)\d{1,2}[./]\d{1,2}[./]\d{4}(?!\d)"#, in: normalizedValue)
        .compactMap { dottedFormatter.date(from: $0.replacingOccurrences(of: "/", with: ".")) }
    if let first = dottedDates.first {
        return (first, dottedDates.dropFirst().first ?? first)
    }

    let humanRangePattern = #"(?<!\d)(\d{1,2})\s*[–—-]\s*(\d{1,2})\s+([A-Za-zА-Яа-яЁёÄÖÜäöüß]+)\s+(\d{4})(?!\d)"#
    if let match = weatherRegexCaptureMatches(humanRangePattern, in: normalizedValue).first,
       match.count == 5,
       let startDay = Int(match[1]),
       let endDay = Int(match[2]),
       let year = Int(match[4]),
       let month = iosWeatherMonthNumber(match[3]),
       let start = calendar.date(from: DateComponents(year: year, month: month, day: startDay)),
       let end = calendar.date(from: DateComponents(year: year, month: month, day: endDay)) {
        return (start, end)
    }

    let humanPattern = #"(?<!\d)(\d{1,2})\s+([A-Za-zА-Яа-яЁёÄÖÜäöüß]+)\s+(\d{4})(?!\d)"#
    let humanDates = weatherRegexCaptureMatches(humanPattern, in: normalizedValue).compactMap { match -> Date? in
        guard match.count == 4,
              let day = Int(match[1]),
              let year = Int(match[3]),
              let month = iosWeatherMonthNumber(match[2])
        else { return nil }
        return calendar.date(from: DateComponents(year: year, month: month, day: day))
    }
    guard let first = humanDates.first else { return nil }
    return (first, humanDates.dropFirst().first ?? first)
}

func iosWeatherTripDates(_ value: String, maxDays: Int = 366) -> [Date] {
    guard let (start, end) = iosWeatherTripDateRange(value) else { return [] }
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(secondsFromGMT: 0) ?? .current
    let dayCount = calendar.dateComponents([.day], from: start, to: end).day ?? 0
    guard dayCount >= 0, maxDays > 0 else { return [] }
    let lastOffset = min(dayCount, maxDays - 1)
    return (0...lastOffset).compactMap {
        calendar.date(byAdding: .day, value: $0, to: start)
    }
}

func iosWeatherISODate(_ date: Date) -> String {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.calendar = Calendar(identifier: .gregorian)
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.dateFormat = "yyyy-MM-dd"
    return formatter.string(from: date)
}

private func iosWeatherMonthNumber(_ value: String) -> Int? {
    switch value.trimmingCharacters(in: .whitespacesAndNewlines).lowercased().prefix(3) {
    case "янв", "jan", "ene": return 1
    case "фев", "feb": return 2
    case "мар", "mär": return 3
    case "апр", "apr": return 4
    case "мая", "май", "may", "mai": return 5
    case "июн", "jun": return 6
    case "июл", "jul": return 7
    case "авг", "aug", "ago": return 8
    case "сен", "сент", "sep": return 9
    case "окт", "oct", "okt": return 10
    case "ноя", "nov": return 11
    case "дек", "dec", "dez": return 12
    default: return nil
    }
}

private func weatherRegexMatches(_ pattern: String, in value: String) -> [String] {
    guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
    let range = NSRange(value.startIndex..., in: value)
    return regex.matches(in: value, range: range).compactMap { match in
        guard let matchRange = Range(match.range, in: value) else { return nil }
        return String(value[matchRange])
    }
}

private func weatherRegexCaptureMatches(_ pattern: String, in value: String) -> [[String]] {
    guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
    let range = NSRange(value.startIndex..., in: value)
    return regex.matches(in: value, range: range).map { match in
        (0..<match.numberOfRanges).compactMap { index in
            guard let matchRange = Range(match.range(at: index), in: value) else { return nil }
            return String(value[matchRange])
        }
    }
}

private enum WeatherError: Error {
    case requestFailed
}

private extension Array {
    subscript(safe index: Int) -> Element? {
        indices.contains(index) ? self[index] : nil
    }
}

private extension Array where Element: Hashable {
    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}
