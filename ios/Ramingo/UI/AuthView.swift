import AuthenticationServices
import SwiftUI

struct AuthView: View {
    @EnvironmentObject private var model: AppModel
    @State private var name = ""
    @State private var email = ""
    @State private var password = ""
    @State private var confirmation = ""
    @State private var isSignUp = false
    @State private var rememberCredentials = true
    @State private var isWorking = false
    @State private var showReset = false
    @State private var languageCode = "RU"
    @State private var localMessage: String?
    #if !SIDELOAD_BUILD
    @State private var appleNonce: String?
    #endif

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()
            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    authHeader

                    Text(isSignUp ? "Создать аккаунт" : "С возвращением")
                        .font(AppTheme.font(30, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                        .lineSpacing(2)
                        .padding(.top, 34)

                    Text(isSignUp ? "Пара шагов — и планируем поездку" : "Войдите, чтобы продолжить планирование")
                        .font(AppTheme.font(14, .semibold))
                        .foregroundStyle(AppTheme.muted)
                        .padding(.top, 8)

                    VStack(spacing: 14) {
                        if isSignUp {
                            RamingoAuthField(title: "Имя", placeholder: "Как вас зовут", text: $name)
                        }
                        RamingoAuthField(title: "E-mail", placeholder: "you@example.com", text: $email, keyboard: .emailAddress)
                        RamingoAuthField(title: "Пароль", placeholder: "••••••••", text: $password, secure: true)
                        if isSignUp {
                            RamingoAuthField(title: "Повторите пароль", placeholder: "••••••••", text: $confirmation, secure: true)
                        }
                    }
                    .padding(.top, 28)

                    if !isSignUp {
                        Button("Забыли пароль?") { showReset = true }
                            .font(AppTheme.font(13, .extrabold))
                            .foregroundStyle(AppTheme.purple)
                            .padding(.top, 10)
                    }

                    Button {
                        rememberCredentials.toggle()
                    } label: {
                        HStack(spacing: 9) {
                            ZStack {
                                RoundedRectangle(cornerRadius: 6, style: .continuous)
                                    .fill(rememberCredentials ? AppTheme.purple : Color.clear)
                                    .overlay {
                                        RoundedRectangle(cornerRadius: 6, style: .continuous)
                                            .stroke(rememberCredentials ? AppTheme.purple : AppTheme.border, lineWidth: 1)
                                    }
                                if rememberCredentials {
                                    Image(systemName: "checkmark")
                                        .font(.system(size: 11, weight: .black))
                                        .foregroundStyle(.white)
                                }
                            }
                            .frame(width: 20, height: 20)
                            Text("Запомнить данные входа")
                                .font(AppTheme.font(13, .bold))
                                .foregroundStyle(AppTheme.muted)
                        }
                    }
                    .buttonStyle(.plain)
                    .padding(.top, 14)

                    PrimaryActionButton(
                        title: isSignUp ? "Создать аккаунт" : "Войти",
                        isLoading: isWorking,
                        disabled: isWorking,
                        action: submit
                    )
                    .padding(.top, 18)

                    if let message = localMessage ?? model.authNotice, !message.isEmpty {
                        Text(message)
                            .font(AppTheme.font(13, .bold))
                            .foregroundStyle(model.authNotice == nil ? AppTheme.error : AppTheme.success)
                            .padding(.top, 10)
                    }

                    divider
                        .padding(.vertical, 22)

                    Button {
                        Task {
                            isWorking = true
                            localMessage = nil
                            await model.signInWithGoogle(remember: rememberCredentials)
                            isWorking = false
                        }
                    } label: {
                        HStack(spacing: 10) {
                            Text("G")
                                .font(.system(size: 18, weight: .bold))
                                .foregroundStyle(Color(hex: 0x4285F4))
                            Text("Продолжить с Google")
                                .font(AppTheme.font(15, .extrabold))
                                .foregroundStyle(AppTheme.ink)
                        }
                        .frame(maxWidth: .infinity)
                        .frame(height: 53)
                        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
                        .overlay {
                            RoundedRectangle(cornerRadius: 15, style: .continuous)
                                .stroke(AppTheme.border, lineWidth: 1)
                        }
                    }
                    .buttonStyle(.plain)
                    .disabled(isWorking)

                    #if !SIDELOAD_BUILD
                    SignInWithAppleButton(.continue) { request in
                        let nonce = SupabaseClient.makeNonce()
                        appleNonce = nonce
                        request.requestedScopes = [.fullName, .email]
                        request.nonce = SupabaseClient.hashNonce(nonce)
                    } onCompletion: { result in
                        Task { @MainActor in await finishAppleSignIn(result) }
                    }
                    .signInWithAppleButtonStyle(.black)
                    .frame(height: 53)
                    .clipShape(RoundedRectangle(cornerRadius: 15, style: .continuous))
                    .padding(.top, 12)
                    .disabled(isWorking)
                    #endif

                    HStack(spacing: 0) {
                        Text(isSignUp ? "Уже есть аккаунт?" : "Нет аккаунта?")
                            .font(AppTheme.font(14, .semibold))
                            .foregroundStyle(AppTheme.muted)
                        Button(isSignUp ? " Войти" : " Зарегистрироваться") {
                            withAnimation(.easeInOut(duration: 0.2)) {
                                isSignUp.toggle()
                                password = ""
                                confirmation = ""
                                localMessage = nil
                                model.authNotice = nil
                            }
                        }
                        .font(AppTheme.font(14, .extrabold))
                        .foregroundStyle(AppTheme.purple)
                    }
                    .frame(maxWidth: .infinity)
                    .padding(.top, 26)

                    if !model.isConfigured {
                        Text("Не задан публичный ключ Supabase.")
                            .font(AppTheme.font(12, .bold))
                            .foregroundStyle(AppTheme.warning)
                            .padding(.top, 16)
                    }
                }
                .padding(.horizontal, 24)
                .padding(.top, 40)
                .padding(.bottom, 32)
            }
            .scrollDismissesKeyboard(.interactively)
        }
        .sheet(isPresented: $showReset) {
            PasswordResetView(prefilledEmail: email)
                .environmentObject(model)
        }
    }

    private var authHeader: some View {
        HStack {
            RamingoLogo()
            Spacer()
            Menu {
                languageButton("🇷🇺 Русский", code: "RU")
                languageButton("🇬🇧 English", code: "EN")
                languageButton("🇪🇸 Español", code: "ES")
                languageButton("🇩🇪 Deutsch", code: "DE")
            } label: {
                HStack(spacing: 4) {
                    Image(systemName: "globe")
                        .font(.system(size: 15, weight: .semibold))
                        .foregroundStyle(AppTheme.purple)
                    Text(languageCode)
                        .font(AppTheme.font(12, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                    Image(systemName: "chevron.down")
                        .font(.system(size: 10, weight: .bold))
                        .foregroundStyle(AppTheme.muted)
                }
                .padding(.horizontal, 8)
                .frame(height: 31)
                .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 10, style: .continuous)
                        .stroke(AppTheme.border, lineWidth: 1)
                }
            }
        }
    }

    private func languageButton(_ title: String, code: String) -> some View {
        Button {
            languageCode = code
        } label: {
            if languageCode == code {
                Label(title, systemImage: "checkmark")
            } else {
                Text(title)
            }
        }
    }

    private var divider: some View {
        HStack(spacing: 12) {
            Rectangle().fill(AppTheme.border).frame(height: 1)
            Text("или")
                .font(AppTheme.font(12, .bold))
                .foregroundStyle(AppTheme.muted)
            Rectangle().fill(AppTheme.border).frame(height: 1)
        }
    }

    private func submit() {
        localMessage = nil
        let normalizedEmail = email.trimmingCharacters(in: .whitespacesAndNewlines)
        let normalizedName = name.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !normalizedEmail.isEmpty, !password.isEmpty, !isSignUp || !normalizedName.isEmpty else {
            localMessage = "Заполните обязательные поля"
            return
        }
        if isSignUp, password != confirmation {
            localMessage = "Пароли не совпадают"
            return
        }
        if isSignUp, password.count < 6 {
            localMessage = "Пароль должен содержать минимум 6 символов"
            return
        }
        Task {
            isWorking = true
            if isSignUp {
                await model.signUp(
                    email: normalizedEmail,
                    password: password,
                    displayName: normalizedName,
                    remember: rememberCredentials
                )
            } else {
                await model.signIn(email: normalizedEmail, password: password, remember: rememberCredentials)
            }
            isWorking = false
        }
    }

    #if !SIDELOAD_BUILD
    private func finishAppleSignIn(_ result: Result<ASAuthorization, Error>) async {
        guard !isWorking else { return }
        isWorking = true
        defer { isWorking = false }
        do {
            let authorization = try result.get()
            guard let credential = authorization.credential as? ASAuthorizationAppleIDCredential,
                  let tokenData = credential.identityToken,
                  let identityToken = String(data: tokenData, encoding: .utf8),
                  let nonce = appleNonce
            else {
                localMessage = "Apple не вернул подтверждение входа. Повторите попытку."
                return
            }
            await model.signInWithApple(identityToken: identityToken, nonce: nonce)
            if model.session != nil, let fullName = credential.fullName {
                let value = [fullName.givenName, fullName.middleName, fullName.familyName]
                    .compactMap { $0?.trimmingCharacters(in: .whitespacesAndNewlines).nonEmpty }
                    .joined(separator: " ")
                if !value.isEmpty { await model.updateDisplayName(value) }
            }
        } catch let error as ASAuthorizationError where error.code == .canceled {
            return
        } catch {
            localMessage = error.localizedDescription
        }
    }
    #endif
}

private struct RamingoAuthField: View {
    let title: String
    let placeholder: String
    @Binding var text: String
    var secure = false
    var keyboard: UIKeyboardType = .default
    @State private var reveal = false

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(AppTheme.font(13, .bold))
                .foregroundStyle(AppTheme.label)
            HStack(spacing: 10) {
                Group {
                    if secure && !reveal {
                        SecureField(placeholder, text: $text)
                    } else {
                        TextField(placeholder, text: $text)
                    }
                }
                .font(AppTheme.font(15, .semibold))
                .foregroundStyle(AppTheme.ink)
                .textInputAutocapitalization(keyboard == .emailAddress ? .never : .sentences)
                .keyboardType(keyboard)
                .autocorrectionDisabled(keyboard == .emailAddress)
                if secure {
                    Button { reveal.toggle() } label: {
                        Image(systemName: reveal ? "eye.slash" : "eye")
                            .font(.system(size: 16, weight: .medium))
                            .foregroundStyle(AppTheme.muted)
                    }
                    .buttonStyle(.plain)
                }
            }
            .padding(.horizontal, 14)
            .frame(height: 52)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(AppTheme.border, lineWidth: 1)
            }
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
            ZStack {
                AppTheme.background.ignoresSafeArea()
                VStack(alignment: .leading, spacing: 20) {
                    Text("Введите e-mail, и мы отправим ссылку для восстановления.")
                        .font(AppTheme.font(14, .semibold))
                        .foregroundStyle(AppTheme.muted)
                    RamingoAuthField(title: "E-mail", placeholder: "you@example.com", text: $email, keyboard: .emailAddress)
                    PrimaryActionButton(title: "Отправить ссылку", isLoading: isWorking, disabled: email.isEmpty || isWorking) {
                        Task {
                            isWorking = true
                            sent = await model.sendPasswordReset(email: email)
                            isWorking = false
                        }
                    }
                    if sent {
                        Text("Проверьте почту: ссылка для сброса отправлена.")
                            .font(AppTheme.font(13, .bold))
                            .foregroundStyle(AppTheme.success)
                    }
                    Spacer()
                }
                .padding(24)
            }
            .navigationTitle("Сброс пароля")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Закрыть") { dismiss() } } }
        }
    }
}

struct PasswordRecoveryView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var password = ""
    @State private var confirmation = ""
    @State private var isWorking = false
    @State private var isSaved = false
    @State private var localError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.background.ignoresSafeArea()
                VStack(alignment: .leading, spacing: 18) {
                    RamingoAuthField(title: "Новый пароль", placeholder: "Минимум 6 символов", text: $password, secure: true)
                    RamingoAuthField(title: "Повторите пароль", placeholder: "••••••••", text: $confirmation, secure: true)
                    if let localError {
                        Text(localError).font(AppTheme.font(13, .bold)).foregroundStyle(AppTheme.error)
                    }
                    if isSaved {
                        Text("Пароль изменён. Теперь можно продолжить работу в Ramingo.")
                            .font(AppTheme.font(13, .bold))
                            .foregroundStyle(AppTheme.success)
                        PrimaryActionButton(title: "Продолжить", action: { dismiss() })
                    } else {
                        PrimaryActionButton(title: "Сохранить новый пароль", isLoading: isWorking, disabled: isWorking || password.isEmpty || confirmation.isEmpty) {
                            Task { await savePassword() }
                        }
                    }
                    Spacer()
                }
                .padding(24)
            }
            .navigationTitle("Новый пароль")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Закрыть") { dismiss() } } }
        }
    }

    private func savePassword() async {
        localError = nil
        guard password == confirmation else {
            localError = "Пароли не совпадают."
            return
        }
        isWorking = true
        defer { isWorking = false }
        do {
            try await model.changePassword(password)
            isSaved = true
        } catch {
            localError = error.localizedDescription
        }
    }
}
