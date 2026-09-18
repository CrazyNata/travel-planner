import Foundation
import SwiftUI

@main
struct RamingoApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .environmentObject(model)
                .tint(AppTheme.purple)
                .task { await model.bootstrap() }
                .onOpenURL { url in
                    model.handleDeepLink(url)
                }
                .onContinueUserActivity(NSUserActivityTypeBrowsingWeb) { activity in
                    if let url = activity.webpageURL {
                        model.handleDeepLink(url)
                    }
                }
                .preferredColorScheme(colorScheme(for: model.profile.themePreference))
                .sheet(isPresented: $model.isShowingPasswordRecovery) {
                    PasswordRecoveryView()
                        .environmentObject(model)
                }
        }
    }

    private func colorScheme(for preference: ThemePreference) -> ColorScheme? {
        switch preference {
        case .system: return nil
        case .light: return .light
        case .dark: return .dark
        }
    }
}

struct RootView: View {
    @EnvironmentObject private var model: AppModel

    var body: some View {
        Group {
            if model.isBootstrapping {
                SplashView()
            } else if model.session != nil {
                if !model.isReadyForSession {
                    SplashView()
                } else if !model.profile.onboardingCompleted {
                    OnboardingView()
                } else {
                    HomeView()
                }
            } else {
                AuthView()
            }
        }
        .alert("Не удалось выполнить запрос", isPresented: Binding(
            get: { model.errorMessage != nil },
            set: { if !$0 { model.errorMessage = nil } },
        )) {
            Button("ОК", role: .cancel) { model.errorMessage = nil }
        } message: {
            Text(model.errorMessage ?? "")
        }
    }
}

private struct SplashView: View {
    var body: some View {
        ZStack {
            AppTheme.warm.ignoresSafeArea()
            VStack(spacing: 18) {
                RamingoLogo()
                ProgressView()
                    .tint(AppTheme.purple)
            }
        }
    }
}

struct OnboardingView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    let replay: Bool
    @State private var page = 0
    @State private var isCompleting = false

    init(replay: Bool = false) {
        self.replay = replay
    }

    private let pages: [OnboardingPage] = [
        OnboardingPage(
            icon: "plus.app.fill",
            title: "Создайте поездку",
            body: "Соберите города, даты и обложку в одном понятном плане.",
            accent: "Начните с маршрута",
        ),
        OnboardingPage(
            icon: "point.3.connected.trianglepath.dotted",
            title: "Планируйте дни",
            body: "Маршрутные дни, переезды, заселение и заметки всегда под рукой.",
            accent: "Планируйте день, а не километры",
        ),
        OnboardingPage(
            icon: "building.columns.fill",
            title: "Сохраняйте места",
            body: "Добавляйте достопримечательности из каталога или вручную.",
            accent: "Каталог или ручной ввод",
        ),
        OnboardingPage(
            icon: "bed.double.fill",
            title: "Выбирайте жильё",
            body: "Храните цены, ссылки бронирования, фотографии и дедлайны отмены.",
            accent: "Важные даты не потеряются",
        ),
        OnboardingPage(
            icon: "fork.knife",
            title: "Собирайте находки",
            body: "Рестораны, бронирования, приоритеты и заметки — в разделе поездки.",
            accent: "Сохраняйте свои находки",
        ),
        OnboardingPage(
            icon: "pawprint.fill",
            title: "Путешествуйте с питомцем",
            body: "Находите ветклиники, магазины и места с нужными удобствами.",
            accent: "Каталог или своё место",
        ),
        OnboardingPage(
            icon: "creditcard.fill",
            title: "Контролируйте бюджет",
            body: "Расходы, группы, валюты и ручные курсы помогают видеть общую картину.",
            accent: "Всё путешествие — в одном месте",
        ),
    ]

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            VStack(spacing: 0) {
                HStack {
                    RamingoMark().frame(width: 42, height: 42)
                    Spacer()
                    Button("Пропустить") { finish(openCreateTrip: false) }
                        .font(AppTheme.font(14, .bold))
                        .foregroundStyle(AppTheme.muted)
                        .disabled(isCompleting)
                }
                .padding(.horizontal, 22)
                .padding(.top, 14)

                TabView(selection: $page) {
                    ForEach(Array(pages.enumerated()), id: \.offset) { index, page in
                        OnboardingPageCard(page: page, index: index + 1, total: pages.count)
                            .tag(index)
                            .padding(.horizontal, 22)
                    }
                }
                .tabViewStyle(.page(indexDisplayMode: .never))
                .animation(.easeInOut(duration: 0.22), value: page)

                HStack(spacing: 7) {
                    ForEach(pages.indices, id: \.self) { index in
                        Capsule()
                            .fill(index == page ? AppTheme.purple : AppTheme.border)
                            .frame(width: index == page ? 24 : 8, height: 8)
                            .animation(.easeInOut(duration: 0.18), value: page)
                    }
                }
                .padding(.bottom, 18)

                HStack(spacing: 12) {
                    if page > 0 {
                        Button {
                            withAnimation { page -= 1 }
                        } label: {
                            Image(systemName: "chevron.left")
                                .font(.system(size: 16, weight: .bold))
                                .foregroundStyle(AppTheme.purple)
                                .frame(width: 52, height: 52)
                                .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 16))
                        }
                        .buttonStyle(.plain)
                    }

                    Button {
                        if page == pages.count - 1 {
                            finish(openCreateTrip: !replay)
                        } else {
                            withAnimation { page += 1 }
                        }
                    } label: {
                        HStack(spacing: 8) {
                            if isCompleting { ProgressView().tint(.white) }
                            Text(page == pages.count - 1 ? (replay ? "Готово" : "Создать путешествие") : "Далее")
                        }
                        .font(AppTheme.font(15, .extrabold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 52)
                        .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 16))
                    }
                    .buttonStyle(.plain)
                    .disabled(isCompleting)
                }
                .padding(.horizontal, 22)
                .padding(.bottom, 22)
            }
        }
    }

    private func finish(openCreateTrip: Bool) {
        guard !isCompleting else { return }
        isCompleting = true
        Task {
            await model.completeOnboarding(openCreateTrip: openCreateTrip)
            isCompleting = false
            if replay {
                dismiss()
            }
        }
    }
}

private struct OnboardingPage: Hashable {
    let icon: String
    let title: String
    let body: String
    let accent: String
}

private struct OnboardingPageCard: View {
    let page: OnboardingPage
    let index: Int
    let total: Int

    var body: some View {
        VStack(spacing: 0) {
            Spacer(minLength: 18)
            ZStack {
                Circle()
                    .fill(AppTheme.lavender)
                    .frame(width: 176, height: 176)
                Circle()
                    .stroke(AppTheme.purpleLight.opacity(0.35), lineWidth: 1.5)
                    .frame(width: 204, height: 204)
                Image(systemName: page.icon)
                    .font(.system(size: 62, weight: .semibold))
                    .foregroundStyle(AppTheme.purple)
            }
            .padding(.top, 22)
            Text("Ramingo · \(index)/\(total)")
                .font(AppTheme.font(12, .extrabold))
                .foregroundStyle(AppTheme.purple)
                .padding(.top, 34)
            Text(page.title)
                .font(AppTheme.font(31, .extrabold))
                .foregroundStyle(AppTheme.ink)
                .multilineTextAlignment(.center)
                .padding(.top, 12)
            Text(page.body)
                .font(AppTheme.font(16, .semibold))
                .foregroundStyle(AppTheme.muted)
                .multilineTextAlignment(.center)
                .lineSpacing(4)
                .padding(.horizontal, 20)
                .padding(.top, 13)
            Text(page.accent)
                .font(AppTheme.font(13, .extrabold))
                .foregroundStyle(AppTheme.purpleDeep)
                .padding(.horizontal, 15)
                .frame(minHeight: 36)
                .background(AppTheme.lavender.opacity(0.7), in: Capsule())
                .padding(.top, 24)
            Spacer()
        }
        .frame(maxWidth: .infinity)
    }
}
