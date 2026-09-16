import PhotosUI
import SwiftUI
import UIKit

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    @State private var selectedPhoto: PhotosPickerItem?
    @State private var notificationsEnabled = false
    @State private var tripRemindersEnabled = true
    @State private var cancellationRemindersEnabled = true
    @State private var reminderHour = 9
    @State private var language = "RU"
    @State private var themePreference: ThemePreference = .system
    @State private var themePickerOpen = false
    @State private var languagePickerOpen = false
    @State private var didLoadProfile = false
    @State private var isSaving = false
    @State private var isSigningOut = false
    @State private var isDeleting = false
    @State private var showPasswordChange = false
    @State private var showDeleteConfirmation = false
    @State private var localError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.background.ignoresSafeArea()
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 18) {
                        profileCard
                        settingsSection("НАПОМИНАНИЯ") {
                            SettingsToggleRow(icon: "bell", title: "Разрешить уведомления", isOn: $notificationsEnabled)
                            SettingsDivider()
                            SettingsToggleRow(icon: "airplane", title: "Напоминать о поездках", isOn: $tripRemindersEnabled)
                                .disabled(!notificationsEnabled)
                            SettingsDivider()
                            SettingsToggleRow(icon: "bed.double", title: "Напоминать об отмене жилья", isOn: $cancellationRemindersEnabled)
                                .disabled(!notificationsEnabled)
                            SettingsDivider()
                            Stepper(value: $reminderHour, in: 0...23) {
                                SettingsRowLabel(icon: "clock", title: "Время напоминаний", value: String(format: "%02d:00", reminderHour))
                            }
                            .disabled(!notificationsEnabled)
                        }

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
                            SettingsButtonRow(icon: "globe", title: "Языки", value: languageTitle) {
                                languagePickerOpen.toggle()
                            }
                            if languagePickerOpen {
                                LanguageSelector(selection: $language) { selected in
                                    let previous = language
                                    language = selected
                                    languagePickerOpen = false
                                    Task { await saveAppearance(previousLanguage: previous, previousTheme: themePreference) }
                                }
                            }
                        }

                        settingsSection("ПРИЛОЖЕНИЕ") {
                            SettingsRowLabel(icon: "info.circle", title: "Версия", value: versionText)
                        }

                        settingsSection("ДОКУМЕНТЫ И ПОДДЕРЖКА") {
                            SettingsLinkRow(icon: "hand.raised", title: "Политика конфиденциальности", url: "https://ramingo.online/#/privacy")
                            SettingsDivider()
                            SettingsLinkRow(icon: "doc.text", title: "Условия использования", url: "https://ramingo.online/#/terms")
                            SettingsDivider()
                            SettingsLinkRow(icon: "envelope", title: "Написать в поддержку", url: "mailto:support@ramingo.online")
                        }

                        settingsSection("БЕЗОПАСНОСТЬ") {
                            SettingsButtonRow(icon: "key", title: "Изменить пароль") { showPasswordChange = true }
                            SettingsDivider()
                            SettingsButtonRow(icon: "trash", title: "Удалить аккаунт", destructive: true) {
                                showDeleteConfirmation = true
                            }
                        }

                        PrimaryActionButton(title: "Сохранить настройки", isLoading: isSaving, disabled: isSaving || !didLoadProfile) {
                            Task { await saveProfile() }
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
            .navigationTitle("Профиль и настройки")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрыть") { dismiss() }
                        .font(AppTheme.font(15, .bold))
                }
            }
            .task { loadProfileState() }
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
            .sheet(isPresented: $showPasswordChange) {
                ChangePasswordView().environmentObject(model)
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
                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Text("Изменить фото")
                            .font(AppTheme.font(12, .extrabold))
                            .foregroundStyle(AppTheme.purple)
                    }
                    .disabled(isSaving || isDeleting)
                    .padding(.top, 2)
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
                reminderHour: reminderHour,
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
                reminderHour: reminderHour
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

private struct LanguageSelector: View {
    @Binding var selection: String
    let onSelect: (String) -> Void
    private let options = ["RU", "EN", "ES", "DE"]

    var body: some View {
        HStack(spacing: 5) {
            ForEach(options, id: \.self) { code in
                Button {
                    onSelect(code)
                } label: {
                    Text(code)
                        .font(AppTheme.font(12, .extrabold))
                        .foregroundStyle(selection.uppercased() == code ? .white : AppTheme.muted)
                        .frame(maxWidth: .infinity)
                        .frame(height: 42)
                        .background(selection.uppercased() == code ? AppTheme.purple : Color.clear, in: RoundedRectangle(cornerRadius: 8, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        .padding(.bottom, 10)
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
                    SecureField("Повторите пароль", text: $confirmation)
                        .ramingoSettingsField()
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
