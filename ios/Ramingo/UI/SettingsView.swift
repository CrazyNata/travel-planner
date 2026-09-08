import SwiftUI

struct SettingsView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var isSigningOut = false

    var body: some View {
        NavigationStack {
            Form {
                Section("Аккаунт") {
                    LabeledContent("Имя", value: model.currentUser?.displayName ?? "Ramingo")
                    LabeledContent("Email", value: model.currentUser?.email ?? "")
                }
                Section("Приложение") {
                    LabeledContent("Версия", value: "iOS 0.1.0")
                    LabeledContent("Хранилище данных", value: "Supabase")
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
                    .disabled(isSigningOut)
                }
            }
            .navigationTitle("Настройки")
            .toolbar { ToolbarItem(placement: .cancellationAction) { Button("Закрыть") { dismiss() } } }
        }
    }
}
