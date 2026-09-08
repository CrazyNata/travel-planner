import Foundation

enum JSONValue: Codable, Equatable, Hashable, Sendable {
    case string(String)
    case number(Double)
    case boolean(Bool)
    case object([String: JSONValue])
    case array([JSONValue])
    case null

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()

        if container.decodeNil() {
            self = .null
        } else if let value = try? container.decode(Bool.self) {
            self = .boolean(value)
        } else if let value = try? container.decode(Int.self) {
            self = .number(Double(value))
        } else if let value = try? container.decode(Double.self) {
            self = .number(value)
        } else if let value = try? container.decode(String.self) {
            self = .string(value)
        } else if let value = try? container.decode([JSONValue].self) {
            self = .array(value)
        } else if let value = try? container.decode([String: JSONValue].self) {
            self = .object(value)
        } else {
            throw DecodingError.dataCorruptedError(
                in: container,
                debugDescription: "Unsupported JSON value",
            )
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        switch self {
        case .string(let value): try container.encode(value)
        case .number(let value): try container.encode(value)
        case .boolean(let value): try container.encode(value)
        case .object(let value): try container.encode(value)
        case .array(let value): try container.encode(value)
        case .null: try container.encodeNil()
        }
    }

    var objectValue: [String: JSONValue]? {
        guard case .object(let value) = self else { return nil }
        return value
    }

    var arrayValue: [JSONValue]? {
        guard case .array(let value) = self else { return nil }
        return value
    }

    var stringValue: String? {
        switch self {
        case .string(let value): return value
        case .number(let value): return String(value)
        case .boolean(let value): return value ? "true" : "false"
        case .object, .array, .null: return nil
        }
    }

    var doubleValue: Double? {
        switch self {
        case .number(let value): return value
        case .string(let value): return Double(value.replacingOccurrences(of: ",", with: "."))
        case .boolean, .object, .array, .null: return nil
        }
    }

    var intValue: Int? {
        guard let value = doubleValue else { return nil }
        return Int(value)
    }

    var boolValue: Bool? {
        switch self {
        case .boolean(let value): return value
        case .string(let value): return Bool(value)
        case .number(let value): return value != 0
        case .object, .array, .null: return nil
        }
    }
}

extension Dictionary where Key == String, Value == JSONValue {
    func value(_ key: String) -> JSONValue? { self[key] }

    func text(_ key: String, fallback: String = "") -> String {
        let value = self[key]?.stringValue?.trimmingCharacters(in: .whitespacesAndNewlines) ?? ""
        return value.nonEmpty ?? fallback
    }

    func number(_ key: String) -> Double? { self[key]?.doubleValue }

    func integer(_ key: String) -> Int? { self[key]?.intValue }

    func flag(_ key: String) -> Bool? { self[key]?.boolValue }

    func object(_ key: String) -> [String: JSONValue] { self[key]?.objectValue ?? [:] }

    func array(_ key: String) -> [JSONValue] { self[key]?.arrayValue ?? [] }
}

extension String {
    var nonEmpty: String? {
        isEmpty ? nil : self
    }
}
