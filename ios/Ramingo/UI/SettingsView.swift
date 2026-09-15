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
    @State private var darkTheme = false
    @State private var didLoadProfile = false
    @State private var isSaving = false
    @State private var isSigningOut = false
    @State private var isDeleting = false
    @State private var showPasswordChange = false
    @State private var showDeleteConfirmation = false
    @State private var localError: String?

    var body: some View {
        NavigationStack {
            Form {
                Section("Аккаунт") {
                    HStack(spacing: 14) {
                        RemotePhotoView(reference: model.profile.avatarReference, client: model.client, contentMode: .fill, cornerRadius: 34)
                            .frame(width: 68, height: 68)
                            .overlay {
                                if model.profile.avatarReference == nil {
                                    Image(systemName: "person.fill")
                                        .font(.title2)
                                        .foregroundStyle(.white.opacity(0.85))
                                }
                            }
                        VStack(alignment: .leading, spacing: 4) {
                            Text(model.currentUser?.displayName ?? "Ramingo")
                                .font(.headline)
                            Text(model.currentUser?.email ?? "")
                                .font(.subheadline)
                                .foregroundStyle(.secondary)
                        }
                    }

                    PhotosPicker(selection: $selectedPhoto, matching: .images) {
                        Label("Изменить фотографию профиля", systemImage: "photo.badge.plus")
                    }
                    .disabled(isSaving || isDeleting)
                }

                Section("Напоминания") {
                    Toggle("Разрешить уведомления", isOn: $notificationsEnabled)
                    Toggle("Напоминать о поездках", isOn: $tripRemindersEnabled)
                        .disabled(!notificationsEnabled)
                    Toggle("Напоминать об отмене жилья", isOn: $cancellationRemindersEnabled)
                        .disabled(!notificationsEnabled)
                    Stepper(value: $reminderHour, in: 0...23) {
                        Text(String(format: "Час напоминаний: %02d:00", reminderHour))
                    }
                    .disabled(!notificationsEnabled)
                }

                Section("Внешний вид") {
                    Picker("Язык напоминаний", selection: $language) {
                        Text("Русский").tag("RU")
                        Text("English").tag("EN")
                        Text("Español").tag("ES")
                        Text("Deutsch").tag("DE")
                    }
                    Toggle("Тёмная тема", isOn: $darkTheme)
                    Button {
                        Task { await saveProfile() }
                    } label: {
                        HStack {
                            Spacer()
                            if isSaving { ProgressView() }
                            Text("Сохранить настройки")
                            Spacer()
                        }
                    }
                    .disabled(isSaving || !didLoadProfile)
                }

                Section("Приложение") {
                    LabeledContent("Версия", value: versionText)
                    LabeledContent("Хранилище данных", value: "Supabase")
                }

                Section("Документы и поддержка") {
                    Link("Политика конфиденциальности", destination: URL(string: "https://ramingo.online/#/privacy")!)
                    Link("Условия использования", destination: URL(string: "https://ramingo.online/#/terms")!)
                    Link("Написать в поддержку", destination: URL(string: "mailto:support@ramingo.online")!)
                }

                Section("Безопасность") {
                    Button("Изменить пароль") { showPasswordChange = true }
                    Button("Удалить аккаунт", role: .destructive) { showDeleteConfirmation = true }
                }

                Section {
                    Button(role: .destructive) {
                        Task {
                            isSigningOut = true
                            await model.signOut()
                            isSigningOut = false
                            dismiss()
                        }
                    } label: {
                        HStack {
                            Spacer()
                            if isSigningOut { ProgressView() }
                            Text("Выйти из аккаунта")
                            Spacer()
                        }
                    }
                    .disabled(isSigningOut || isDeleting)
                }
            }
            .navigationTitle("Настройки")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрыть") { dismiss() }
                }
            }
            .task {
                guard !didLoadProfile else { return }
                notificationsEnabled = model.profile.notificationsEnabled
                tripRemindersEnabled = model.profile.tripRemindersEnabled
                cancellationRemindersEnabled = model.profile.cancellationRemindersEnabled
                reminderHour = model.profile.reminderHour
                language = model.profile.language
                darkTheme = model.profile.darkTheme
                didLoadProfile = true
            }
            .onChange(of: selectedPhoto) { _, item in
                guard let item else { return }
                Task { @MainActor in await importPhoto(item) }
            }
            .alert("Не удалось сохранить изменения", isPresented: Binding(
                get: { localError != nil },
                set: { if !$0 { localError = nil } },
            )) {
                Button("ОК", role: .cancel) { localError = nil }
            } message: {
                Text(localError ?? "")
            }
            .confirmationDialog(
                "Удалить аккаунт?",
                isPresented: $showDeleteConfirmation,
                titleVisibility: .visible,
            ) {
                Button("Удалить аккаунт", role: .destructive) {
                    Task { await deleteAccount() }
                }
                Button("Отмена", role: .cancel) {}
            } message: {
                Text("Будут удалены профиль и связанные с ним данные. Это действие нельзя отменить.")
            }
            .sheet(isPresented: $showPasswordChange) {
                ChangePasswordView()
                    .environmentObject(model)
            }
        }
    }

    private var versionText: String {
        let version = Bundle.main.object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—"
        let build = Bundle.main.object(forInfoDictionaryKey: "CFBundleVersion") as? String ?? "—"
        return "iOS \(version) (\(build))"
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
                darkTheme: darkTheme,
                tripRemindersEnabled: tripRemindersEnabled,
                cancellationRemindersEnabled: cancellationRemindersEnabled,
                reminderHour: reminderHour,
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
            else {
                throw SettingsError.invalidPhoto
            }
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
            Form {
                Section("Новый пароль") {
                    SecureField("Минимум 6 символов", text: $password)
                        .textContentType(.newPassword)
                    SecureField("Повторите пароль", text: $confirmation)
                        .textContentType(.newPassword)
                }
                if let localError {
                    Section { Text(localError).foregroundStyle(.red) }
                }
                if isSaved {
                    Section {
                        Label("Пароль изменён.", systemImage: "checkmark.circle.fill")
                            .foregroundStyle(.green)
                        Button("Готово") { dismiss() }
                    }
                } else {
                    Section {
                        Button {
                            Task { await save() }
                        } label: {
                            HStack {
                                Spacer()
                                if isWorking { ProgressView() }
                                Text("Сохранить пароль")
                                Spacer()
                            }
                        }
                        .disabled(isWorking || password.isEmpty || confirmation.isEmpty)
                    }
                }
            }
            .navigationTitle("Изменить пароль")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрыть") { dismiss() }
                }
            }
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

private enum SettingsError: LocalizedError {
    case invalidPhoto

    var errorDescription: String? {
        "Не удалось прочитать выбранную фотографию."
    }
}
