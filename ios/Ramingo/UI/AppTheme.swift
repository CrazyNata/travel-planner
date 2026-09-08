import SwiftUI

enum AppTheme {
    static let ink = Color(red: 0.12, green: 0.10, blue: 0.18)
    static let muted = Color(red: 0.40, green: 0.37, blue: 0.48)
    static let purple = Color(red: 0.42, green: 0.27, blue: 0.88)
    static let purpleDeep = Color(red: 0.27, green: 0.16, blue: 0.65)
    static let lavender = Color(red: 0.95, green: 0.93, blue: 1.0)
    static let warm = Color(red: 1.0, green: 0.97, blue: 0.92)
    static let surface = Color(uiColor: .secondarySystemBackground)
    static let background = Color(uiColor: .systemGroupedBackground)
}

struct RamingoLogo: View {
    var compact = false

    var body: some View {
        HStack(spacing: compact ? 8 : 12) {
            ZStack {
                Circle()
                    .fill(AppTheme.purple)
                Image(systemName: "airplane.departure")
                    .font(.system(size: compact ? 14 : 19, weight: .bold))
                    .foregroundStyle(.white)
                    .rotationEffect(.degrees(-12))
            }
            .frame(width: compact ? 30 : 42, height: compact ? 30 : 42)
            Text("Ramingo")
                .font(.system(size: compact ? 21 : 30, weight: .heavy, design: .rounded))
                .foregroundStyle(AppTheme.ink)
        }
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
        VStack(alignment: .leading, spacing: 4) {
            Text(title)
                .font(.title3.weight(.bold))
                .foregroundStyle(AppTheme.ink)
            if let subtitle, !subtitle.isEmpty {
                Text(subtitle)
                    .font(.subheadline)
                    .foregroundStyle(AppTheme.muted)
            }
        }
    }
}

struct RamingoCard<Content: View>: View {
    let content: Content

    init(@ViewBuilder content: () -> Content) {
        self.content = content()
    }

    var body: some View {
        content
            .padding(16)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .strokeBorder(Color.black.opacity(0.04))
            }
    }
}

struct StatusPill: View {
    let text: String

    var body: some View {
        Text(text.isEmpty ? "Черновик" : text)
            .font(.caption.weight(.semibold))
            .foregroundStyle(AppTheme.purpleDeep)
            .padding(.horizontal, 10)
            .padding(.vertical, 6)
            .background(AppTheme.lavender, in: Capsule())
    }
}

struct ProgressBar: View {
    let value: Int

    var body: some View {
        GeometryReader { proxy in
            ZStack(alignment: .leading) {
                Capsule().fill(AppTheme.lavender)
                Capsule()
                    .fill(AppTheme.purple)
                    .frame(width: proxy.size.width * CGFloat(min(max(value, 0), 100)) / 100)
            }
        }
        .frame(height: 8)
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
            guard let reference, !reference.isEmpty else { return }
            url = await client.resolvePhoto(reference)
        }
    }

    private var placeholder: some View {
        Rectangle()
            .fill(LinearGradient(colors: [AppTheme.purpleDeep, AppTheme.purple], startPoint: .topLeading, endPoint: .bottomTrailing))
            .overlay {
                Image(systemName: "mountain.2.fill")
                    .font(.system(size: 28, weight: .medium))
                    .foregroundStyle(.white.opacity(0.75))
            }
    }
}
