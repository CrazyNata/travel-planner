import SwiftUI

struct AuthView: View {
    @EnvironmentObject private var model: AppModel
    @State private var email = ""
    @State private var password = ""
    @State private var isSignUp = false
    @State private var isWorking = false
    @State private var showReset = false

    var body: some View {
        ZStack {
            AppTheme.warm.ignoresSafeArea()
            ScrollView {
                VStack(alignment: .leading, spacing: 24) {
                    RamingoLogo()
                        .padding(.top, 36)

                    VStack(alignment: .leading, spacing: 8) {
                        Text(isSignUp ? "Создайте пространство для поездок" : "С возвращением")
                            .font(.system(size: 32, weight: .heavy, design: .rounded))
                            .foregroundStyle(AppTheme.ink)
                        Text(isSignUp ? "Сохраняйте маршруты, места и воспоминания вместе." : "Войдите, чтобы продолжить планировать путешествия.")
                            .font(.body)
                            .foregroundStyle(AppTheme.muted)
                    }

                    VStack(spacing: 14) {
                        TextField("Email", text: $email)
                            .textInputAutocapitalization(.never)
                            .keyboardType(.emailAddress)
                            .textContentType(.emailAddress)
                            .authFieldStyle()
                        SecureField("Пароль", text: $password)
                            .textContentType(isSignUp ? .newPassword : .password)
                            .authFieldStyle()
                    }

                    Button {
                        Task {
                            isWorking = true
                            if isSignUp {
                                await model.signUp(email: email, password: password)
                            } else {
                                await model.signIn(email: email, password: password)
                            }
                            isWorking = false
                        }
                    } label: {
                        HStack {
                            Spacer()
                            if isWorking { ProgressView().tint(.white) }
                            Text(isSignUp ? "Зарегистрироваться" : "Войти")
                                .font(.headline.weight(.bold))
                            Spacer()
                        }
                        .padding(.vertical, 16)
                    }
                    .buttonStyle(.borderedProminent)
                    .disabled(isWorking || email.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty || password.isEmpty)

                    HStack {
                        Rectangle().fill(Color.black.opacity(0.1)).frame(height: 1)
                        Text("или")
                            .font(.caption)
                            .foregroundStyle(AppTheme.muted)
                        Rectangle().fill(Color.black.opacity(0.1)).frame(height: 1)
                    }

                    Button {
                        Task {
                            isWorking = true
                            await model.signInWithGoogle()
                            isWorking = false
                        }
                    } label: {
                        HStack {
                            Image(systemName: "globe")
                            Text("Продолжить с Google")
                                .font(.headline.weight(.semibold))
                        }
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                    }
                    .buttonStyle(.bordered)
                    .disabled(isWorking)

                    HStack {
                        Spacer()
                        Button(isSignUp ? "У меня уже есть аккаунт" : "Создать аккаунт") {
                            withAnimation(.easeInOut(duration: 0.2)) { isSignUp.toggle() }
                        }
                        Spacer()
                    }

                    if !isSignUp {
                        HStack {
                            Spacer()
                            Button("Забыли пароль?") { showReset = true }
                                .font(.subheadline.weight(.semibold))
                                .foregroundStyle(AppTheme.purpleDeep)
                            Spacer()
                        }
                    }

                    if !model.isConfigured {
                        Text("Для подключения заполните Supabase publishable key в Config/Secrets.xcconfig.")
                            .font(.footnote)
                            .foregroundStyle(.orange)
                            .padding(12)
                            .background(Color.orange.opacity(0.12), in: RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(.horizontal, 24)
                .padding(.bottom, 32)
            }
        }
        .sheet(isPresented: $showReset) {
            PasswordResetView(prefilledEmail: email)
                .environmentObject(model)
        }
    }
}

private struct PasswordResetView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var email: String
    @State private var isWorking = false
    @State private var sent = false

    init(prefilledEmail: String) {
        _email = State(initialValue: prefilledEmail)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Email") {
                    TextField("you@example.com", text: $email)
                        .textInputAutocapitalization(.never)
                        .keyboardType(.emailAddress)
                }
                Section {
                    Button {
                        Task {
                            isWorking = true
                            await model.sendPasswordReset(email: email)
                            sent = model.errorMessage == nil
                            isWorking = false
                        }
                    } label: {
                        HStack {
                            Spacer()
                            if isWorking { ProgressView() }
                            Text("Отправить ссылку")
                            Spacer()
                        }
                    }
                    .disabled(email.isEmpty || isWorking)
                }
                if sent {
                    Text("Проверьте почту: ссылка для сброса отправлена.")
                        .foregroundStyle(.green)
                }
            }
            .navigationTitle("Сброс пароля")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Закрыть") { dismiss() } } }
        }
    }
}

private extension View {
    func authFieldStyle() -> some View {
        self
            .padding(.horizontal, 16)
            .padding(.vertical, 15)
            .background(.white.opacity(0.9), in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: 16).stroke(Color.black.opacity(0.06)) }
    }
}
