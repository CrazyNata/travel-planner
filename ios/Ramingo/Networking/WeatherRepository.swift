import Foundation

struct WeatherSnapshot: Hashable, Sendable {
    let temperature: String
    let condition: String
    let tripTemperature: String?
    let tripCondition: String?
    let tripIsEstimate: Bool
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
        var result: [String: WeatherSnapshot] = [:]
        for city in cities.uniqued() where !city.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
            guard let coordinate = await resolveCoordinate(for: city, coordinates: coordinates) else { continue }
            if let snapshot = try? await load(city: city, coordinate: coordinate, tripDates: tripDates) {
                result[city] = snapshot
            }
        }
        return result
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
        let (data, response) = try await session.data(from: components.url!)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw WeatherError.requestFailed }
        let weather = try JSONDecoder().decode(WeatherResponse.self, from: data)
        let targetDate = tripDate(from: tripDates)
        var trip = targetDate.flatMap { tripWeather(in: weather.daily, date: $0) }
        if let targetDate, trip == nil {
            let today = Calendar(identifier: .gregorian).startOfDay(for: Date())
            if targetDate < today {
                trip = try? await loadArchivedTripWeather(coordinate: coordinate, date: targetDate)
            } else if targetDate > Calendar(identifier: .gregorian).date(byAdding: .day, value: 15, to: today) ?? today {
                trip = try? await loadClimateTripWeather(coordinate: coordinate, date: targetDate)
            }
        }
        return WeatherSnapshot(
            temperature: "\(Int(weather.current.temperature.rounded()))°C",
            condition: condition(for: weather.current.weatherCode),
            tripTemperature: trip?.temperature,
            tripCondition: trip?.condition,
            tripIsEstimate: trip?.isEstimate == true,
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
              let (data, response) = try? await session.data(from: url),
              (response as? HTTPURLResponse)?.statusCode == 200,
              let decoded = try? JSONDecoder().decode(GeocodingResponse.self, from: data),
              let result = decoded.results?.first
        else { return nil }
        return Coordinate(latitude: result.latitude, longitude: result.longitude)
    }

    private func loadArchivedTripWeather(coordinate: Coordinate, date: Date) async throws -> TripWeather {
        let day = ISO8601DateFormatter()
        day.formatOptions = [.withFullDate]
        var components = URLComponents(string: "https://archive-api.open-meteo.com/v1/archive")!
        components.queryItems = [
            URLQueryItem(name: "latitude", value: String(coordinate.latitude)),
            URLQueryItem(name: "longitude", value: String(coordinate.longitude)),
            URLQueryItem(name: "start_date", value: day.string(from: date)),
            URLQueryItem(name: "end_date", value: day.string(from: date)),
            URLQueryItem(name: "daily", value: "temperature_2m_max,weather_code"),
            URLQueryItem(name: "timezone", value: "auto"),
        ]
        let (data, response) = try await session.data(from: components.url!)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw WeatherError.requestFailed }
        let archive = try JSONDecoder().decode(DailyOnlyResponse.self, from: data)
        return tripWeather(in: archive.daily, date: date) ?? TripWeather(temperature: "—", condition: "—", isEstimate: false)
    }

    private func loadClimateTripWeather(coordinate: Coordinate, date: Date) async throws -> TripWeather {
        let day = ISO8601DateFormatter()
        day.formatOptions = [.withFullDate]
        var components = URLComponents(string: "https://climate-api.open-meteo.com/v1/climate")!
        components.queryItems = [
            URLQueryItem(name: "latitude", value: String(coordinate.latitude)),
            URLQueryItem(name: "longitude", value: String(coordinate.longitude)),
            URLQueryItem(name: "start_date", value: day.string(from: date)),
            URLQueryItem(name: "end_date", value: day.string(from: date)),
            URLQueryItem(name: "models", value: "EC_Earth3P_HR"),
            URLQueryItem(name: "daily", value: "temperature_2m_mean,precipitation_sum,cloud_cover_mean"),
            URLQueryItem(name: "timezone", value: "auto"),
        ]
        let (data, response) = try await session.data(from: components.url!)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw WeatherError.requestFailed }
        let climate = try JSONDecoder().decode(DailyOnlyResponse.self, from: data)
        guard let daily = climate.daily,
              let index = daily.time.firstIndex(of: day.string(from: date)),
              let temperature = daily.meanTemperature[safe: index]
        else { throw WeatherError.requestFailed }
        let precipitation = daily.precipitation[safe: index] ?? 0
        let cloudCover = daily.cloudCover[safe: index] ?? 0
        let condition = precipitation >= 1 ? "Дождь" : cloudCover >= 65 ? "Облачно" : "Ясно"
        return TripWeather(
            temperature: "\(Int(temperature.rounded()))°C",
            condition: condition,
            isEstimate: true,
        )
    }

    private func tripWeather(in daily: DailyWeather?, date: Date) -> TripWeather? {
        guard let daily else { return nil }
        let day = ISO8601DateFormatter()
        day.formatOptions = [.withFullDate]
        let target = day.string(from: date)
        guard let index = daily.time.firstIndex(of: target) else { return nil }
        let temperature = daily.maxTemperature[safe: index].map { "\(Int($0.rounded()))°C" }
        let condition = daily.weatherCodes[safe: index].map { self.condition(for: $0) }
        guard temperature != nil || condition != nil else { return nil }
        return TripWeather(
            temperature: temperature ?? "—",
            condition: condition ?? "—",
            isEstimate: false,
        )
    }

    private func tripDate(from value: String) -> Date? {
        let pattern = #"\d{4}-\d{2}-\d{2}|\d{1,2}[./]\d{1,2}[./]\d{4}"#
        guard let match = value.range(of: pattern, options: .regularExpression) else { return nil }
        let dateString = String(value[match])
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "en_US_POSIX")
        formatter.calendar = Calendar(identifier: .gregorian)
        for format in ["yyyy-MM-dd", "dd.MM.yyyy", "d.M.yyyy", "dd/MM/yyyy", "d/M/yyyy"] {
            formatter.dateFormat = format
            if let date = formatter.date(from: dateString) { return date }
        }
        return nil
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
