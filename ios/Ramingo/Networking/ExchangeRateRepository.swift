import Foundation

struct ExchangeRateSnapshot: Hashable, Sendable {
    let rates: [String: Double]
    let date: String
}

private struct FrankfurterRate: Decodable {
    let date: String
    let quote: String
    let rate: Double
}

final class ExchangeRateRepository {
    private let session: URLSession

    init(session: URLSession = .shared) {
        self.session = session
    }

    func loadRubRates(quotes: Set<String>) async throws -> ExchangeRateSnapshot {
        let normalized = quotes
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines).uppercased() }
            .filter { !$0.isEmpty && $0 != "RUB" }
            .sorted()
        if normalized.isEmpty { return ExchangeRateSnapshot(rates: ["RUB": 1], date: "") }

        var components = URLComponents(string: "https://api.frankfurter.dev/v2/rates")!
        components.queryItems = [
            URLQueryItem(name: "base", value: "RUB"),
            URLQueryItem(name: "quotes", value: normalized.joined(separator: ",")),
        ]
        let (data, response) = try await session.data(from: components.url!)
        guard (response as? HTTPURLResponse)?.statusCode == 200 else { throw ExchangeRateError.requestFailed }
        let values = try JSONDecoder().decode([FrankfurterRate].self, from: data)
        var rates = ["RUB": 1.0]
        values.forEach { if $0.rate > 0 { rates[$0.quote.uppercased()] = $0.rate } }
        guard normalized.allSatisfy({ rates[$0] != nil }) else { throw ExchangeRateError.incomplete }
        return ExchangeRateSnapshot(rates: rates, date: values.map(\.date).uniqued().first ?? "")
    }
}

private enum ExchangeRateError: Error {
    case requestFailed
    case incomplete
}

private extension Array where Element: Hashable {
    func uniqued() -> [Element] {
        var seen = Set<Element>()
        return filter { seen.insert($0).inserted }
    }
}
