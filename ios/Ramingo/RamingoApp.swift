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
                    // Supabase OAuth is completed by ASWebAuthenticationSession.
                    // Keeping the handler here lets future universal links route
                    // into a trip without changing the Android or web clients.
                    _ = url
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
