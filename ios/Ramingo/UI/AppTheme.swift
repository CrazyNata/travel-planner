import SwiftUI
import UIKit

enum AppTheme {
    static let purple = Color(hex: 0x6C5CE7)
    static let purpleDeep = Color(hex: 0x5A4BD4)
    static let purpleLight = Color(hex: 0x8E7BF5)
    static let success = Color(hex: 0x22B07D)
    static let error = Color(hex: 0xE0524B)
    static let warning = Color(hex: 0xF5A623)

    static let background = adaptive(light: 0xF4F4F7, dark: 0x141416)
    static let surface = adaptive(light: 0xFFFFFF, dark: 0x222531)
    static let surface2 = adaptive(light: 0xF5F5F8, dark: 0x303443)
    static let track = adaptive(light: 0xEEEEF2, dark: 0x3A3E4B)
    static let ink = adaptive(light: 0x141419, dark: 0xF7F8FC)
    static let label = adaptive(light: 0x3A3A42, dark: 0xD9DBE6)
    static let muted = adaptive(light: 0x6A6A75, dark: 0xA8ADBC)
    static let border = adaptive(light: 0xE6E6EC, dark: 0x697084)
    static let lavender = adaptive(light: 0xF1EEFE, dark: 0x332F50)
    static let warm = background

    static let primaryGradient = LinearGradient(
        colors: [purple, purpleLight],
        startPoint: .topLeading,
        endPoint: .bottomTrailing
    )

    static func font(_ size: CGFloat, _ weight: RamingoFontWeight = .regular) -> Font {
        .custom("Manrope-ExtraLight", fixedSize: size).weight(weight.swiftWeight)
    }

    private static func adaptive(light: UInt32, dark: UInt32) -> Color {
        Color(uiColor: UIColor { traits in
            UIColor(hex: traits.userInterfaceStyle == .dark ? dark : light)
        })
    }
}

enum RamingoFontWeight {
    case regular, medium, semibold, bold, extrabold

    fileprivate var swiftWeight: Font.Weight {
        switch self {
        case .regular: return .regular
        case .medium: return .medium
        case .semibold: return .semibold
        case .bold: return .bold
        case .extrabold: return .heavy
        }
    }
}

private extension UIColor {
    convenience init(hex: UInt32) {
        self.init(
            red: CGFloat((hex >> 16) & 0xFF) / 255,
            green: CGFloat((hex >> 8) & 0xFF) / 255,
            blue: CGFloat(hex & 0xFF) / 255,
            alpha: 1
        )
    }
}

extension Color {
    init(hex: UInt32) {
        self.init(
            red: Double((hex >> 16) & 0xFF) / 255,
            green: Double((hex >> 8) & 0xFF) / 255,
            blue: Double(hex & 0xFF) / 255
        )
    }
}

struct RamingoLogo: View {
    var compact = false

    var body: some View {
        HStack(spacing: compact ? 9 : 11) {
            Text("R")
                .font(AppTheme.font(compact ? 13 : 19, .extrabold))
                .foregroundStyle(.white)
                .frame(width: compact ? 26 : 42, height: compact ? 26 : 36)
                .background(AppTheme.primaryGradient, in: RoundedRectangle(cornerRadius: compact ? 8 : 12, style: .continuous))
            Text("Ramingo")
                .font(AppTheme.font(compact ? 16 : 20, .extrabold))
                .foregroundStyle(AppTheme.ink)
        }
        .accessibilityElement(children: .ignore)
        .accessibilityLabel("Ramingo")
    }
}

struct RamingoTopBar<Trailing: View>: View {
    private let trailing: Trailing

    init(@ViewBuilder trailing: () -> Trailing) {
        self.trailing = trailing()
    }

    var body: some View {
        ZStack {
            RamingoLogo(compact: true)
            HStack {
                Spacer()
                trailing
            }
        }
        .frame(height: 54)
    }
}

struct SectionHeading: View {
    let title: String
    var subtitle: String?

    init(_ title: String, subtitle: String? = nil) {
        self.title = title
        self.subtitle = subtitle
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(title)
                .font(AppTheme.font(19, .extrabold))
                .foregroundStyle(AppTheme.ink)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(AppTheme.font(13, .semibold))
                    .foregroundStyle(AppTheme.muted)
            }
        }
    }
}

struct RamingoCard<Content: View>: View {
    let content: Content
    var padding: CGFloat
    var radius: CGFloat

    init(padding: CGFloat = 16, radius: CGFloat = 22, @ViewBuilder content: () -> Content) {
        self.content = content()
        self.padding = padding
        self.radius = radius
    }

    var body: some View {
        content
            .padding(padding)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: radius, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: radius, style: .continuous)
                    .strokeBorder(AppTheme.border.opacity(0.72), lineWidth: 1)
            }
            .shadow(color: Color.black.opacity(0.07), radius: 13, x: 0, y: 8)
    }
}

struct StatusPill: View {
    let text: String

    var body: some View {
        HStack(spacing: 6) {
            Circle().fill(statusColor).frame(width: 6, height: 6)
            Text(text.isEmpty ? "Черновик" : text)
                .font(AppTheme.font(11, .bold))
                .lineLimit(1)
        }
        .foregroundStyle(AppTheme.label)
        .padding(.horizontal, 10)
        .padding(.vertical, 7)
        .background(AppTheme.surface.opacity(0.94), in: Capsule())
    }

    private var statusColor: Color {
        text.localizedCaseInsensitiveContains("чернов") ? AppTheme.warning : AppTheme.success
    }
}

struct ProgressBar: View {
    let value: Int

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(AppTheme.track)
                Capsule()
                    .fill(AppTheme.purple)
                    .frame(width: proxy.size.width * CGFloat(min(max(value, 0), 100)) / 100)
            }
        }
        .frame(height: 7)
    }
}

struct PrimaryActionButton: View {
    let title: String
    var isLoading = false
    var disabled = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 9) {
                if isLoading { ProgressView().tint(.white) }
                Text(isLoading ? "Подождите…" : title)
                    .font(AppTheme.font(16, .extrabold))
            }
            .foregroundStyle(.white)
            .frame(maxWidth: .infinity)
            .frame(height: 56)
            .background(AppTheme.primaryGradient, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
            .opacity(disabled ? 0.55 : 1)
        }
        .buttonStyle(.plain)
        .disabled(disabled)
    }
}

struct RemotePhotoView: View {
    let reference: String?
    let client: SupabaseClient
    var contentMode: ContentMode = .fill
    var cornerRadius: CGFloat = 18

    @State private var url: URL?

    var body: some View {
        ZStack {
            if let url {
                AsyncImage(url: url, transaction: Transaction(animation: .easeInOut)) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().aspectRatio(contentMode: contentMode)
                    case .failure:
                        placeholder
                    default:
                        placeholder.overlay { ProgressView().tint(.white) }
                    }
                }
            } else {
                placeholder
            }
        }
        .clipShape(RoundedRectangle(cornerRadius: cornerRadius, style: .continuous))
        .task(id: reference) {
            url = nil
            guard let reference, !reference.isEmpty else { return }
            url = try? await client.resolvePhoto(reference)
        }
    }

    private var placeholder: some View {
        Rectangle()
            .fill(LinearGradient(colors: [Color(hex: 0x7865D5), Color(hex: 0xB7A8FF)], startPoint: .topLeading, endPoint: .bottomTrailing))
            .overlay {
                Image(systemName: "mountain.2.fill")
                    .font(.system(size: 28, weight: .medium))
                    .foregroundStyle(.white.opacity(0.8))
            }
    }
}
