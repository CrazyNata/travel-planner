import SwiftUI

@main
struct RamingoApp: App {
    @StateObject private var model = AppModel()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(model)
                .tint(AppTheme.purple)
                .task { await model.bootstrap() }
                .onOpenURL { url in
                    model.handleDeepLink(url)
                }
                .preferredColorScheme(model.profile.darkTheme ? .dark : nil)
                .sheet(isPresented: $model.isShowingPasswordRecovery) {
                    PasswordRecoveryView()
                        .environmentObject(model)
                }
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
                HomeView()
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
