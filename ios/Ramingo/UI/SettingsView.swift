import PhotosUI
import SwiftUI
import UIKit
import UserNotifications

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    @State private var selectedPhoto: PhotosPickerItem?
    @State private var notificationsEnabled = false
    @State private var tripRemindersEnabled = true
    @State private var cancellationRemindersEnabled = true
    @State private var paymentRemindersEnabled = true
    @State private var emailNotificationsEnabled = true
    @State private var emailPaymentRemindersEnabled = true
    @State private var emailRecipient: String?
    @State private var reminderHour = 9
    @State private var language = "RU"
    @State private var themePreference: ThemePreference = .system
    @State private var themePickerOpen = false
    @State private var didLoadProfile = false
    @State private var isSaving = false
    @State private var isSigningOut = false
    @State private var isDeleting = false
    @State private var showNotificationSettings = false
    @State private var isPhotoPickerPresented = false
    @State private var showPasswordChange = false
    @State private var showOnboardingReplay = false
    @State private var showDeleteConfirmation = false
    @State private var localError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                if showNotificationSettings {
                    NotificationSettingsView(onClose: {
                        showNotificationSettings = false
                    })
                    .environmentObject(model)
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    AppTheme.background.ignoresSafeArea()
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        profileCard
                        settingsSection("ВНЕШНИЙ ВИД") {
                            SettingsButtonRow(icon: "paintpalette", title: "Тема", value: themeTitle) {
                                themePickerOpen.toggle()
                            }
                            if themePickerOpen {
                                ThemePreferenceSelector(selection: $themePreference) { selected in
                                    let previous = themePreference
                                    themePreference = selected
                                    themePickerOpen = false
                                    Task { await saveAppearance(previousLanguage: language, previousTheme: previous) }
                                }
                            }
                        }

                        settingsSection("НАСТРОЙКИ АККАУНТА") {
                            LanguageSettingsRow(
                                language: $language,
                                languageTitle: languageTitle,
                                onSelect: { selected in
                                    guard selected.uppercased() != language.uppercased() else { return }
                                    let previous = language
                                    language = selected
                                    Task { await saveAppearance(previousLanguage: previous, previousTheme: themePreference) }
                                }
                            )
                            SettingsDivider()
                            SettingsButtonRow(icon: "bell", title: "Уведомления", value: notificationTitle) {
                                showNotificationSettings = true
                            }
                            SettingsDivider()
                            SettingsButtonRow(icon: "key", title: "Изменить пароль") { showPasswordChange = true }
                            SettingsDivider()
                            SettingsButtonRow(icon: "photo", title: "Сменить фото") { isPhotoPickerPresented = true }
                            SettingsDivider()
                            SettingsButtonRow(icon: "trash", title: "Удалить аккаунт", destructive: true) {
                                showDeleteConfirmation = true
                            }
                        }

                        settingsSection("ПРИЛОЖЕНИЕ") {
                            SettingsButtonRow(icon: "book.pages", title: "Повторить обучение") {
                                showOnboardingReplay = true
                            }
                            SettingsDivider()
                            SettingsRowLabel(icon: "info.circle", title: "Версия", value: versionText)
                        }

                        settingsSection("ДОКУМЕНТЫ И ПОДДЕРЖКА") {
                            SettingsLinkRow(icon: "hand.raised", title: "Политика конфиденциальности", url: "https://ramingo.online/#/privacy")
                            SettingsDivider()
                            SettingsLinkRow(icon: "doc.text", title: "Условия использования", url: "https://ramingo.online/#/terms")
                            SettingsDivider()
                            SettingsLinkRow(icon: "envelope", title: "Написать в поддержку", url: "mailto:support@ramingo.online")
                        }

                        Button {
                            Task {
                                isSigningOut = true
                                await model.signOut()
                                isSigningOut = false
                                dismiss()
                            }
                        } label: {
                            HStack(spacing: 8) {
                                if isSigningOut { ProgressView().tint(AppTheme.error) }
                                Image(systemName: "rectangle.portrait.and.arrow.right")
                                Text("Выйти из аккаунта")
                            }
                            .font(AppTheme.font(15, .extrabold))
                            .foregroundStyle(AppTheme.error)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
                            .overlay { RoundedRectangle(cornerRadius: 15).stroke(AppTheme.border, lineWidth: 1) }
                        }
                        .buttonStyle(.plain)
                        .disabled(isSigningOut || isDeleting)
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 34)
                }
                }
            }
            .navigationTitle("Настройки")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрыть") { dismiss() }
                        .font(AppTheme.font(15, .bold))
                }
            }
            .task { loadProfileState() }
            .photosPicker(isPresented: $isPhotoPickerPresented, selection: $selectedPhoto, matching: .images)
            .onChange(of: model.profile) { _, next in
                guard didLoadProfile else { return }
                notificationsEnabled = next.notificationsEnabled
                tripRemindersEnabled = next.tripRemindersEnabled
                cancellationRemindersEnabled = next.cancellationRemindersEnabled
                paymentRemindersEnabled = next.paymentRemindersEnabled
                emailNotificationsEnabled = next.emailNotificationsEnabled
                emailPaymentRemindersEnabled = next.emailPaymentRemindersEnabled
                emailRecipient = next.emailRecipient
                reminderHour = next.reminderHour
                language = next.language
                themePreference = next.themePreference
            }
            .onChange(of: selectedPhoto) { _, item in
                guard let item else { return }
                Task { @MainActor in await importPhoto(item) }
            }
            .alert("Не удалось сохранить изменения", isPresented: Binding(
                get: { localError != nil },
                set: { if !$0 { localError = nil } }
            )) {
                Button("ОК", role: .cancel) { localError = nil }
            } message: {
                Text(localError ?? "")
            }
            .confirmationDialog("Удалить аккаунт?", isPresented: $showDeleteConfirmation, titleVisibility: .visible) {
                Button("Удалить аккаунт", role: .destructive) { Task { await deleteAccount() } }
                Button("Отмена", role: .cancel) {}
            } message: {
                Text("Будут удалены профиль и связанные с ним данные. Это действие нельзя отменить.")
            }
            .fullScreenCover(isPresented: $showPasswordChange) {
                ChangePasswordView().environmentObject(model)
            }
            .fullScreenCover(isPresented: $showOnboardingReplay) {
                OnboardingView(replay: true)
                    .environmentObject(model)
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private var profileCard: some View {
        RamingoCard(padding: 18) {
            HStack(spacing: 15) {
                RemotePhotoView(reference: model.profile.avatarReference, client: model.client, contentMode: .fill, cornerRadius: 34)
                    .frame(width: 68, height: 68)
                    .overlay {
                        if model.profile.avatarReference == nil {
                            Text(initials)
                                .font(AppTheme.font(20, .extrabold))
                                .foregroundStyle(.white)
                        }
                    }
                VStack(alignment: .leading, spacing: 4) {
                    Text(model.currentUser?.displayName ?? "Ramingo")
                        .font(AppTheme.font(18, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                    Text(model.currentUser?.email ?? "")
                        .font(AppTheme.font(12, .semibold))
                        .foregroundStyle(AppTheme.muted)
                        .lineLimit(1)
                }
                Spacer()
            }
        }
    }

    private func settingsSection<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            Text(title)
                .font(AppTheme.font(11, .extrabold))
                .tracking(1.15)
                .foregroundStyle(AppTheme.purple)
                .padding(.leading, 4)
            VStack(spacing: 0) { content() }
                .padding(.horizontal, 15)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 18, style: .continuous))
                .overlay { RoundedRectangle(cornerRadius: 18).stroke(AppTheme.border.opacity(0.75), lineWidth: 1) }
        }
    }

    private var initials: String {
        String((model.currentUser?.displayName ?? "R").prefix(2)).uppercased()
    }

    private var themeTitle: String {
        switch themePreference {
        case .system: return "Системная"
        case .light: return "Светлая"
        case .dark: return "Тёмная"
        }
    }

    private var languageTitle: String {
        switch language.uppercased() {
        case "EN": return "English"
        case "ES": return "Español"
        case "DE": return "Deutsch"
        default: return "Русский"
        }
    }

    private var notificationTitle: String {
        notificationsEnabled ? "Включены" : "Выключены"
    }

    private var versionText: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "—"
        return "iOS \(version) (\(build))"
    }

    private func loadProfileState() {
        guard !didLoadProfile else { return }
        notificationsEnabled = model.profile.notificationsEnabled
        tripRemindersEnabled = model.profile.tripRemindersEnabled
        cancellationRemindersEnabled = model.profile.cancellationRemindersEnabled
        paymentRemindersEnabled = model.profile.paymentRemindersEnabled
        emailNotificationsEnabled = model.profile.emailNotificationsEnabled
        emailPaymentRemindersEnabled = model.profile.emailPaymentRemindersEnabled
        emailRecipient = model.profile.emailRecipient
        reminderHour = model.profile.reminderHour
        language = model.profile.language
        themePreference = model.profile.themePreference
        didLoadProfile = true
    }

    private func saveAppearance(previousLanguage: String, previousTheme: ThemePreference) async {
        guard didLoadProfile else { return }
        localError = nil
        isSaving = true
        defer { isSaving = false }
        do {
            let next = AccountProfile(
                avatarReference: model.profile.avatarReference,
                notificationsEnabled: notificationsEnabled,
                language: language,
                themePreference: themePreference,
                tripRemindersEnabled: tripRemindersEnabled,
                cancellationRemindersEnabled: cancellationRemindersEnabled,
                paymentRemindersEnabled: paymentRemindersEnabled,
                emailNotificationsEnabled: emailNotificationsEnabled,
                emailPaymentRemindersEnabled: emailPaymentRemindersEnabled,
                emailRecipient: emailRecipient,
                reminderHour: reminderHour,
                onboardingCompleted: model.profile.onboardingCompleted,
                createTripHintSeen: model.profile.createTripHintSeen,
                addPlaceHintSeen: model.profile.addPlaceHintSeen,
                hasStoredProfile: model.profile.hasStoredProfile,
            )
            try await model.updateProfile(next)
        } catch {
            language = previousLanguage
            themePreference = previousTheme
            localError = error.localizedDescription
        }
    }

    private func saveProfile() async {
        localError = nil
        isSaving = true
        defer { isSaving = false }
        do {
            if notificationsEnabled != model.profile.notificationsEnabled {
                try await model.setNotificationsEnabled(notificationsEnabled)
            }
            let next = AccountProfile(
                avatarReference: model.profile.avatarReference,
                notificationsEnabled: notificationsEnabled,
                language: language,
                themePreference: themePreference,
                tripRemindersEnabled: tripRemindersEnabled,
                cancellationRemindersEnabled: cancellationRemindersEnabled,
                paymentRemindersEnabled: paymentRemindersEnabled,
                emailNotificationsEnabled: emailNotificationsEnabled,
                emailPaymentRemindersEnabled: emailPaymentRemindersEnabled,
                emailRecipient: emailRecipient,
                reminderHour: reminderHour,
                onboardingCompleted: model.profile.onboardingCompleted,
                createTripHintSeen: model.profile.createTripHintSeen,
                addPlaceHintSeen: model.profile.addPlaceHintSeen,
                hasStoredProfile: model.profile.hasStoredProfile,
            )
            try await model.updateProfile(next)
        } catch {
            if notificationsEnabled && error is AppModelError {
                notificationsEnabled = model.profile.notificationsEnabled
            }
            localError = error.localizedDescription
        }
    }

    private func importPhoto(_ item: PhotosPickerItem) async {
        isSaving = true
        defer {
            isSaving = false
            selectedPhoto = nil
        }
        do {
            guard let source = try await item.loadTransferable(type: Data.self),
                  let image = UIImage(data: source),
                  let data = image.jpegData(compressionQuality: 0.86)
            else { throw SettingsError.invalidPhoto }
            try await model.uploadProfilePhoto(data: data)
        } catch {
            localError = error.localizedDescription
        }
    }

    private func deleteAccount() async {
        localError = nil
        isDeleting = true
        defer { isDeleting = false }
        do {
            try await model.deleteAccount()
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
    }
}

private struct NotificationSettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    private let onClose: (() -> Void)?

    @State private var notificationsEnabled = false
    @State private var tripRemindersEnabled = true
    @State private var cancellationRemindersEnabled = true
    @State private var paymentRemindersEnabled = true
    @State private var emailNotificationsEnabled = true
    @State private var emailPaymentRemindersEnabled = true
    @State private var emailRecipient = ""
    @State private var reminderHour = 9
    @State private var permissionGranted = false
    @State private var didLoad = false
    @State private var isSaving = false
    @State private var message: String?
    @State private var messageIsError = false
    @State private var selectedPreset = 0
    @State private var helpOpen = false
    @State private var emailEditorOpen = false
    @State private var timePickerOpen = false
    @State private var pickerDate = Date()

    init(onClose: (() -> Void)? = nil) {
        self.onClose = onClose
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            AppTheme.background.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 0) {
                    header
                    permissionCard

                    notificationGroup
                        .padding(.top, 14)
                    emailGroup
                        .padding(.top, 14)
                    deliveryTime
                    quickPreset
                }
                .padding(.horizontal, 20)
                .padding(.top, 8)
                .padding(.bottom, 118)
            }
            .safeAreaPadding(.top, 4)

            saveFooter
        }
        .toolbar(.hidden, for: .navigationBar)
        .navigationBarBackButtonHidden(true)
        .task { await loadState() }
        .sheet(isPresented: $emailEditorOpen) {
            EmailRecipientEditorView(initialValue: emailRecipient) { value in
                guard let normalized = normalizeNotificationEmail(value) else {
                    return "Укажите корректный e-mail"
                }
                emailRecipient = normalized
                return nil
            }
            .presentationDetents([.height(310)])
        }
        .sheet(isPresented: $timePickerOpen) {
            TimePickerView(date: $pickerDate) {
                reminderHour = Calendar.current.component(.hour, from: pickerDate)
                selectedPreset = -1
                timePickerOpen = false
            }
            .presentationDetents([.height(350)])
        }
        .alert("О напоминаниях", isPresented: $helpOpen) {
            Button("Понятно", role: .cancel) {}
        } message: {
            Text("Ramingo напомнит о начале поездки, оплате жилья и дедлайнах бесплатной отмены. E-mail-настройки применяются и в веб-версии. Все параметры сохраняются в профиле аккаунта.")
        }
    }

    private var header: some View {
        HStack(alignment: .center, spacing: 10) {
            Button { onClose?() ?? dismiss() } label: {
                Image(systemName: "arrow.left")
                    .font(.system(size: 22, weight: .medium))
                    .foregroundStyle(AppTheme.ink)
                    .frame(width: 42, height: 42)
            }
            .buttonStyle(.plain)
            .accessibilityElement(children: .ignore)
            .accessibilityLabel("Закрыть")
            .accessibilityIdentifier("notifications.close")
            VStack(alignment: .leading, spacing: 2) {
                Text("Уведомления")
                    .font(AppTheme.font(24, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                Text("Настройте напоминания под себя")
                    .font(AppTheme.font(13, .medium))
                    .foregroundStyle(AppTheme.muted)
            }
            Spacer(minLength: 8)
            Button { helpOpen = true } label: {
                Image(systemName: "questionmark")
                    .font(.system(size: 17, weight: .heavy))
                    .foregroundStyle(AppTheme.purple)
                    .frame(width: 36, height: 42)
            }
            .buttonStyle(.plain)
            .accessibilityLabel("О напоминаниях")
        }
    }

    private var permissionCard: some View {
        Button {
            Task { await requestPermission() }
        } label: {
            HStack(spacing: 14) {
                Image(systemName: permissionGranted ? "checkmark" : "bell")
                    .font(.system(size: 20, weight: .semibold))
                    .foregroundStyle(permissionGranted ? AppTheme.success : Color(hex: 0xB97828))
                    .frame(width: 54, height: 54)
                    .background(
                        (permissionGranted ? AppTheme.success : Color(hex: 0xFFDFAC)).opacity(0.18),
                        in: RoundedRectangle(cornerRadius: 16, style: .continuous),
                    )
                VStack(alignment: .leading, spacing: 5) {
                    Text(permissionGranted ? "Разрешение телефона включено" : "Разрешите уведомления телефона")
                        .font(AppTheme.font(14, .bold))
                        .foregroundStyle(permissionGranted ? AppTheme.success : AppTheme.ink)
                        .multilineTextAlignment(.leading)
                    Text(permissionGranted ? "Ramingo сможет напоминать о важных датах." : "Нажмите, чтобы открыть системное разрешение.")
                        .font(AppTheme.font(12, .medium))
                        .foregroundStyle(AppTheme.muted)
                        .multilineTextAlignment(.leading)
                }
                Spacer(minLength: 0)
            }
            .padding(14)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(
                (permissionGranted ? AppTheme.success : AppTheme.warning).opacity(permissionGranted ? 0.10 : 0.12),
                in: RoundedRectangle(cornerRadius: 20, style: .continuous),
            )
        }
        .buttonStyle(.plain)
        .padding(.top, 14)
        .accessibilityHint(permissionGranted ? "Уведомления разрешены" : "Открыть системные настройки уведомлений")
    }

    private var notificationGroup: some View {
        VStack(spacing: 0) {
            NotificationSettingsRow(
                icon: "bell",
                title: "Все уведомления",
                detail: "Поездки, жильё и задачи",
                isOn: $notificationsEnabled,
                onToggle: { selectedPreset = -1 },
            )
            SettingsDivider()
            NotificationSettingsRow(
                icon: "calendar",
                title: "До начала поездки",
                detail: "30, 14, 7, 3 и 1 день",
                isOn: $tripRemindersEnabled,
                enabled: notificationsEnabled,
                onToggle: { selectedPreset = -1 },
            )
            SettingsDivider()
            NotificationSettingsRow(
                icon: "bed.double",
                title: "Бесплатная отмена",
                detail: "7, 3, 1 день и день дедлайна",
                isOn: $cancellationRemindersEnabled,
                enabled: notificationsEnabled,
                onToggle: { selectedPreset = -1 },
            )
            SettingsDivider()
            NotificationSettingsRow(
                icon: "wallet.pass",
                title: "Оплата жилья",
                detail: "За 3 дня и в день дедлайна",
                isOn: $paymentRemindersEnabled,
                enabled: notificationsEnabled,
                onToggle: { selectedPreset = -1 },
            )
        }
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(AppTheme.border.opacity(0.82), lineWidth: 1) }
    }

    private var emailGroup: some View {
        VStack(spacing: 0) {
            HStack(alignment: .top, spacing: 14) {
                settingsIcon("bell")
                VStack(alignment: .leading, spacing: 6) {
                    HStack(alignment: .firstTextBaseline, spacing: 8) {
                        Text("Email-уведомления")
                            .font(AppTheme.font(15, .bold))
                            .foregroundStyle(AppTheme.ink)
                            .lineLimit(1)
                            .minimumScaleFactor(0.82)
                        Spacer(minLength: 4)
                        Button("Изменить") { emailEditorOpen = true }
                            .font(AppTheme.font(13, .extrabold))
                            .foregroundStyle(AppTheme.purple)
                            .buttonStyle(.plain)
                            .fixedSize()
                    }
                    Text("Письма будут приходить на")
                        .font(AppTheme.font(13, .medium))
                        .foregroundStyle(AppTheme.muted)
                    Text(emailTarget)
                        .font(AppTheme.font(13, .medium))
                        .foregroundStyle(AppTheme.muted)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 16)

            SettingsDivider()
            NotificationSettingsRow(
                icon: "bell",
                title: "Все письма от Ramingo",
                detail: "Разрешить отправку писем",
                isOn: $emailNotificationsEnabled,
                onToggle: { selectedPreset = -1 },
            )
            SettingsDivider()
            NotificationSettingsRow(
                icon: "wallet.pass",
                title: "Оплата жилья по e-mail",
                detail: "За 3 дня и в день дедлайна",
                isOn: $emailPaymentRemindersEnabled,
                enabled: emailNotificationsEnabled,
                onToggle: { selectedPreset = -1 },
            )
        }
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
        .overlay { RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(AppTheme.border.opacity(0.82), lineWidth: 1) }
    }

    private var deliveryTime: some View {
        VStack(spacing: 0) {
            HStack(alignment: .firstTextBaseline) {
                Text("Время отправки")
                    .font(AppTheme.font(17, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                Spacer()
                Text("Часовой пояс: авто")
                    .font(AppTheme.font(13, .extrabold))
                    .foregroundStyle(AppTheme.purple)
            }
            .padding(.top, 22)
            .padding(.bottom, 10)

            Button {
                pickerDate = pickerDateForHour
                timePickerOpen = true
            } label: {
                HStack(spacing: 14) {
                    settingsIcon("clock")
                    VStack(alignment: .leading, spacing: 5) {
                        Text("Ежедневное время")
                            .font(AppTheme.font(15, .bold))
                            .foregroundStyle(AppTheme.ink)
                        Text("Напоминания не будут приходить ночью")
                            .font(AppTheme.font(13, .medium))
                            .foregroundStyle(AppTheme.muted)
                            .lineLimit(1)
                            .minimumScaleFactor(0.78)
                    }
                    Spacer(minLength: 4)
                    Text(String(format: "%02d:00", reminderHour))
                        .font(AppTheme.font(16, .extrabold))
                        .foregroundStyle(AppTheme.purple)
                        .padding(.horizontal, 14)
                        .padding(.vertical, 12)
                        .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                }
                .padding(14)
                .frame(maxWidth: .infinity, alignment: .leading)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
                .overlay { RoundedRectangle(cornerRadius: 20, style: .continuous).stroke(AppTheme.border.opacity(0.82), lineWidth: 1) }
            }
            .buttonStyle(.plain)
        }
    }

    private var quickPreset: some View {
        VStack(alignment: .leading, spacing: 10) {
            Text("Быстрый пресет")
                .font(AppTheme.font(17, .extrabold))
                .foregroundStyle(AppTheme.ink)
            HStack(spacing: 8) {
                presetButton("Сбалансированный", index: 0)
                presetButton("Только важное", index: 1)
                presetButton("Всё", index: 2)
            }
        }
        .padding(.top, 22)
    }

    private func presetButton(_ title: String, index: Int) -> some View {
        Button {
            selectedPreset = index
            notificationsEnabled = true
            if index == 1 {
                tripRemindersEnabled = false
                cancellationRemindersEnabled = true
                paymentRemindersEnabled = true
            } else {
                tripRemindersEnabled = true
                cancellationRemindersEnabled = true
                paymentRemindersEnabled = true
            }
        } label: {
            Text(title)
                .font(AppTheme.font(12, .extrabold))
                .foregroundStyle(selectedPreset == index ? AppTheme.purple : AppTheme.muted)
                .lineLimit(1)
                .minimumScaleFactor(0.72)
                .frame(maxWidth: .infinity)
                .frame(height: 42)
                .background(
                    selectedPreset == index ? AppTheme.lavender : AppTheme.surface,
                    in: RoundedRectangle(cornerRadius: 13, style: .continuous),
                )
                .overlay {
                    RoundedRectangle(cornerRadius: 13, style: .continuous)
                        .stroke(selectedPreset == index ? AppTheme.purpleLight.opacity(0.65) : AppTheme.border, lineWidth: 1)
                }
        }
        .buttonStyle(.plain)
    }

    private var saveFooter: some View {
        VStack(spacing: 7) {
            if let message {
                Text(message)
                    .font(AppTheme.font(12, .bold))
                    .foregroundStyle(messageIsError ? AppTheme.error : AppTheme.success)
                    .lineLimit(1)
            }
            Button {
                Task { await save() }
            } label: {
                HStack(spacing: 8) {
                    if isSaving { ProgressView().tint(.white) }
                    Text(isSaving ? "Сохраняем…" : "Сохранить настройки")
                        .font(AppTheme.font(16, .extrabold))
                }
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 56)
                .background(AppTheme.primaryGradient, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(isSaving || !didLoad)
        }
        .padding(.horizontal, 20)
        .padding(.top, 9)
        .padding(.bottom, 8)
        .background(AppTheme.background)
    }

    private var emailTarget: String {
        let configured = emailRecipient.trimmingCharacters(in: .whitespacesAndNewlines)
        return configured.isEmpty ? (model.currentUser?.email ?? "email вашего аккаунта") : configured
    }

    private var pickerDateForHour: Date {
        Calendar.current.date(bySettingHour: reminderHour, minute: 0, second: 0, of: Date()) ?? Date()
    }

    private func settingsIcon(_ systemName: String) -> some View {
        Image(systemName: systemName)
            .font(.system(size: 19, weight: .semibold))
            .foregroundStyle(AppTheme.purple)
            .frame(width: 42, height: 42)
            .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
    }

    private func loadState() async {
        guard !didLoad else { return }
        notificationsEnabled = model.profile.notificationsEnabled
        tripRemindersEnabled = model.profile.tripRemindersEnabled
        cancellationRemindersEnabled = model.profile.cancellationRemindersEnabled
        paymentRemindersEnabled = model.profile.paymentRemindersEnabled
        emailNotificationsEnabled = model.profile.emailNotificationsEnabled
        emailPaymentRemindersEnabled = model.profile.emailPaymentRemindersEnabled
        emailRecipient = model.profile.emailRecipient ?? model.currentUser?.email ?? ""
        reminderHour = model.profile.reminderHour
        selectedPreset = !tripRemindersEnabled && cancellationRemindersEnabled ? 1 : 0
        let settings = await ReminderScheduler.notificationSettings()
        permissionGranted = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
        didLoad = true
    }

    private func requestPermission() async {
        do {
            let granted = try await ReminderScheduler.requestAuthorization()
            permissionGranted = granted
            if !granted {
                messageIsError = true
                message = "Разрешите уведомления в настройках iPhone"
            }
        } catch {
            messageIsError = true
            message = error.localizedDescription
        }
    }

    private func save() async {
        message = nil
        messageIsError = false
        guard let normalizedEmail = normalizeNotificationEmail(emailRecipient) else {
            emailEditorOpen = true
            return
        }
        isSaving = true
        defer { isSaving = false }
        do {
            try await model.updateNotificationSettings(
                enabled: notificationsEnabled,
                tripRemindersEnabled: tripRemindersEnabled,
                cancellationRemindersEnabled: cancellationRemindersEnabled,
                paymentRemindersEnabled: paymentRemindersEnabled,
                emailNotificationsEnabled: emailNotificationsEnabled,
                emailPaymentRemindersEnabled: emailPaymentRemindersEnabled,
                emailRecipient: normalizedEmail,
                reminderHour: reminderHour,
            )
            let settings = await ReminderScheduler.notificationSettings()
            permissionGranted = settings.authorizationStatus == .authorized || settings.authorizationStatus == .provisional
            message = "Настройки сохранены"
        } catch {
            messageIsError = true
            message = error.localizedDescription
        }
    }
}

private struct NotificationSettingsRow: View {
    let icon: String
    let title: String
    let detail: String
    @Binding var isOn: Bool
    var enabled = true
    let onToggle: () -> Void

    var body: some View {
        Button {
            guard enabled else { return }
            isOn.toggle()
            onToggle()
        } label: {
            HStack(spacing: 14) {
                Image(systemName: icon)
                    .font(.system(size: 19, weight: .semibold))
                    .foregroundStyle(AppTheme.purple)
                    .frame(width: 42, height: 42)
                    .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
                VStack(alignment: .leading, spacing: 5) {
                    Text(title)
                        .font(AppTheme.font(15, .bold))
                        .foregroundStyle(AppTheme.ink)
                        .lineLimit(1)
                    Text(detail)
                        .font(AppTheme.font(13, .medium))
                        .foregroundStyle(AppTheme.muted)
                        .lineLimit(1)
                        .minimumScaleFactor(0.72)
                }
                Spacer(minLength: 4)
                RamingoSwitch(isOn: isOn, enabled: enabled)
            }
            .padding(.horizontal, 14)
            .padding(.vertical, 15)
            .contentShape(Rectangle())
        }
        .buttonStyle(.plain)
        .opacity(enabled ? 1 : 0.45)
        .accessibilityElement(children: .combine)
        .accessibilityLabel(title)
        .accessibilityValue(isOn ? "Включено" : "Выключено")
    }
}

private struct RamingoSwitch: View {
    let isOn: Bool
    let enabled: Bool

    var body: some View {
        ZStack(alignment: isOn ? .trailing : .leading) {
            Capsule()
                .fill(isOn ? AppTheme.purple.opacity(enabled ? 1 : 0.35) : AppTheme.border)
                .frame(width: 52, height: 32)
            Circle()
                .fill(Color.white)
                .frame(width: 28, height: 28)
                .shadow(color: .black.opacity(0.12), radius: 2, y: 1)
                .padding(2)
        }
        .animation(.easeInOut(duration: 0.16), value: isOn)
    }
}

private struct EmailRecipientEditorView: View {
    @Environment(\.dismiss) private var dismiss
    let initialValue: String
    let onSave: (String) -> String?
    @State private var value: String
    @State private var error: String?

    init(initialValue: String, onSave: @escaping (String) -> String?) {
        self.initialValue = initialValue
        self.onSave = onSave
        _value = State(initialValue: initialValue)
    }

    var body: some View {
        NavigationStack {
            VStack(alignment: .leading, spacing: 16) {
                Text("Укажите адрес, на который будут приходить напоминания Ramingo.")
                    .font(AppTheme.font(14, .medium))
                    .foregroundStyle(AppTheme.muted)
                TextField("you@example.com", text: $value)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                    .autocorrectionDisabled()
                    .ramingoSettingsField()
                if let error {
                    Text(error)
                        .font(AppTheme.font(12, .bold))
                        .foregroundStyle(AppTheme.error)
                }
                Spacer()
            }
            .padding(20)
            .background(AppTheme.background.ignoresSafeArea())
            .navigationTitle("Почта для писем")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена") { dismiss() }
                }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Готово") {
                        if let error = onSave(value) {
                            self.error = error
                        } else {
                            dismiss()
                        }
                    }
                    .font(AppTheme.font(14, .extrabold))
                }
            }
        }
    }
}

private struct TimePickerView: View {
    @Environment(\.dismiss) private var dismiss
    @Binding var date: Date
    let onDone: () -> Void

    var body: some View {
        NavigationStack {
            DatePicker("Ежедневное время", selection: $date, displayedComponents: .hourAndMinute)
                .datePickerStyle(.wheel)
                .labelsHidden()
                .frame(maxWidth: .infinity, maxHeight: .infinity)
                .background(AppTheme.background.ignoresSafeArea())
                .navigationTitle("Время отправки")
                .navigationBarTitleDisplayMode(.inline)
                .toolbar {
                    ToolbarItem(placement: .cancellationAction) {
                        Button("Отмена") { dismiss() }
                    }
                    ToolbarItem(placement: .confirmationAction) {
                        Button("Готово") { onDone() }
                            .font(AppTheme.font(14, .extrabold))
                    }
                }
        }
    }
}

private struct SettingsDivider: View {
    var body: some View { Rectangle().fill(AppTheme.border.opacity(0.8)).frame(height: 1) }
}

private struct SettingsRowLabel: View {
    let icon: String
    let title: String
    var value: String? = nil

    var body: some View {
        HStack(spacing: 12) {
            Image(systemName: icon)
                .font(.system(size: 16, weight: .semibold))
                .foregroundStyle(AppTheme.purple)
                .frame(width: 22)
            Text(title)
                .font(AppTheme.font(14, .bold))
                .foregroundStyle(AppTheme.ink)
            Spacer()
            if let value {
                Text(value)
                    .font(AppTheme.font(12, .semibold))
                    .foregroundStyle(AppTheme.muted)
            }
        }
        .frame(minHeight: 52)
    }
}

private struct SettingsToggleRow: View {
    let icon: String
    let title: String
    @Binding var isOn: Bool

    var body: some View {
        Toggle(isOn: $isOn) {
            SettingsRowLabel(icon: icon, title: title)
        }
        .toggleStyle(.switch)
        .tint(AppTheme.purple)
    }
}

private struct ThemePreferenceSelector: View {
    @Binding var selection: ThemePreference
    let onSelect: (ThemePreference) -> Void

    var body: some View {
        VStack(spacing: 4) {
            ForEach([ThemePreference.system, .light, .dark], id: \.self) { preference in
                Button {
                    onSelect(preference)
                } label: {
                    HStack {
                        Text(title(for: preference))
                            .font(AppTheme.font(12, .bold))
                            .foregroundStyle(selection == preference ? .white : AppTheme.ink)
                        Spacer()
                        if selection == preference {
                            Image(systemName: "checkmark")
                                .font(.system(size: 13, weight: .bold))
                                .foregroundStyle(.white)
                        }
                    }
                    .padding(.horizontal, 11)
                    .frame(minHeight: 42)
                    .background(selection == preference ? AppTheme.purple : Color.clear, in: RoundedRectangle(cornerRadius: 9, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .padding(.bottom, 10)
    }

    private func title(for preference: ThemePreference) -> String {
        switch preference {
        case .system: return "Системная"
        case .light: return "Светлая"
        case .dark: return "Тёмная"
        }
    }
}

private struct LanguageSettingsRow: View {
    @Binding var language: String
    let languageTitle: String
    let onSelect: (String) -> Void
    private let options = ["RU", "EN", "ES", "DE"]

    var body: some View {
        Menu {
            ForEach(options, id: \.self) { code in
                Button {
                    onSelect(code)
                } label: {
                    HStack {
                        Text(code)
                        if language.uppercased() == code {
                            Image(systemName: "checkmark")
                        }
                    }
                }
                .accessibilityIdentifier("settings.language.\(code)")
            }
        } label: {
            HStack(spacing: 12) {
                Image(systemName: "globe")
                    .frame(width: 22)
                Text("Языки")
                Spacer()
                Text(languageTitle)
                    .font(AppTheme.font(12, .semibold))
                    .foregroundStyle(AppTheme.muted)
                Image(systemName: "chevron.right")
                    .font(.system(size: 12, weight: .bold))
            }
            .font(AppTheme.font(14, .bold))
            .foregroundStyle(AppTheme.ink)
            .frame(maxWidth: .infinity, minHeight: 52, alignment: .leading)
        }
        .menuStyle(.borderlessButton)
        .accessibilityIdentifier("settings.language.row")
    }
}

private struct SettingsButtonRow: View {
    let icon: String
    let title: String
    var value: String? = nil
    var destructive = false
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            HStack(spacing: 12) {
                Image(systemName: icon).frame(width: 22)
                Text(title)
                Spacer()
                if let value {
                    Text(value)
                        .font(AppTheme.font(12, .semibold))
                        .foregroundStyle(AppTheme.muted)
                }
                Image(systemName: "chevron.right").font(.system(size: 12, weight: .bold))
            }
            .font(AppTheme.font(14, .bold))
            .foregroundStyle(destructive ? AppTheme.error : AppTheme.ink)
            .frame(minHeight: 52)
        }
        .buttonStyle(.plain)
    }
}

private struct SettingsLinkRow: View {
    let icon: String
    let title: String
    let url: String

    var body: some View {
        Link(destination: URL(string: url)!) {
            HStack(spacing: 12) {
                Image(systemName: icon).foregroundStyle(AppTheme.purple).frame(width: 22)
                Text(title).foregroundStyle(AppTheme.ink)
                Spacer()
                Image(systemName: "arrow.up.right").foregroundStyle(AppTheme.muted).font(.system(size: 12, weight: .bold))
            }
            .font(AppTheme.font(14, .bold))
            .frame(minHeight: 52)
        }
    }
}

struct ChangePasswordView: View {
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
                    SecureField("Новый пароль · минимум 6 символов", text: $password)
                        .ramingoSettingsField()
                        .textContentType(.newPassword)
                        .autocorrectionDisabled()
                    SecureField("Повторите пароль", text: $confirmation)
                        .ramingoSettingsField()
                        .textContentType(.newPassword)
                        .autocorrectionDisabled()
                    if let localError {
                        Text(localError).font(AppTheme.font(13, .bold)).foregroundStyle(AppTheme.error)
                    }
                    if isSaved {
                        Text("Пароль изменён.")
                            .font(AppTheme.font(13, .bold))
                            .foregroundStyle(AppTheme.success)
                        PrimaryActionButton(title: "Готово", action: { dismiss() })
                    } else {
                        PrimaryActionButton(title: "Сохранить пароль", isLoading: isWorking, disabled: isWorking || password.isEmpty || confirmation.isEmpty) {
                            Task { await save() }
                        }
                    }
                    Spacer()
                }
                .padding(24)
            }
            .navigationTitle("Изменить пароль")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Закрыть") { dismiss() } } }
        }
    }

    private func save() async {
        localError = nil
        guard password == confirmation else {
            localError = "Пароли не совпадают."
            return
        }
        guard password.count >= 6 else {
            localError = "Пароль должен содержать минимум 6 символов."
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

private extension View {
    func ramingoSettingsField() -> some View {
        self
            .font(AppTheme.font(14, .semibold))
            .padding(.horizontal, 14)
            .frame(height: 52)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }
    }
}

private enum SettingsError: LocalizedError {
    case invalidPhoto
    var errorDescription: String? { "Не удалось прочитать выбранную фотографию." }
}
