import SwiftUI

struct CreateTripView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var startDate = ""
    @State private var endDate = ""
    @State private var cities = ""
    @State private var isSaving = false
    @State private var localError: String?

    var body: some View {
        NavigationStack {
            ZStack {
                AppTheme.background.ignoresSafeArea()
                ScrollView(showsIndicators: false) {
                    VStack(alignment: .leading, spacing: 0) {
                        Text("Новое путешествие")
                            .font(AppTheme.font(30, .extrabold))
                            .foregroundStyle(AppTheme.ink)
                        Text("Добавьте основу — детали можно заполнить позже")
                            .font(AppTheme.font(14, .semibold))
                            .foregroundStyle(AppTheme.muted)
                            .padding(.top, 8)

                        VStack(spacing: 14) {
                            TripTextField(title: "Название", placeholder: "Например, Рождественская Италия", text: $title)
                            TripTextField(title: "Города", placeholder: "Рим, Флоренция, Милан", text: $cities)
                        }
                        .padding(.top, 28)

                        Text("ДАТЫ")
                            .font(AppTheme.font(11, .extrabold))
                            .tracking(1.2)
                            .foregroundStyle(AppTheme.purple)
                            .padding(.top, 25)

                        HStack(spacing: 10) {
                            TripTextField(title: "Начало", placeholder: "ГГГГ-ММ-ДД", text: $startDate)
                            TripTextField(title: "Окончание", placeholder: "ГГГГ-ММ-ДД", text: $endDate)
                        }
                        .padding(.top, 12)

                        if let localError {
                            Text(localError)
                                .font(AppTheme.font(13, .bold))
                                .foregroundStyle(AppTheme.error)
                                .padding(.top, 12)
                        }

                        PrimaryActionButton(
                            title: "Создать путешествие",
                            isLoading: isSaving,
                            disabled: isSaving || cities.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
                        ) {
                            Task { await save() }
                        }
                        .padding(.top, 24)

                        Text("После создания откроются маршрут, места, жильё, бюджет и остальные разделы.")
                            .font(AppTheme.font(12, .semibold))
                            .foregroundStyle(AppTheme.muted)
                            .multilineTextAlignment(.center)
                            .frame(maxWidth: .infinity)
                            .padding(.top, 13)
                    }
                    .padding(.horizontal, 24)
                    .padding(.top, 28)
                    .padding(.bottom, 40)
                }
                .scrollDismissesKeyboard(.interactively)
            }
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Отмена") { dismiss() }
                        .font(AppTheme.font(15, .bold))
                }
            }
            .navigationBarTitleDisplayMode(.inline)
        }
    }

    private func save() async {
        localError = nil
        isSaving = true
        defer { isSaving = false }
        do {
            try await model.createTrip(title: title, startDate: startDate, endDate: endDate, cities: cities)
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
    }
}

struct TripTextField: View {
    let title: String
    let placeholder: String
    @Binding var text: String
    var axis: Axis = .horizontal

    var body: some View {
        VStack(alignment: .leading, spacing: 7) {
            Text(title)
                .font(AppTheme.font(13, .bold))
                .foregroundStyle(AppTheme.label)
            TextField(placeholder, text: $text, axis: axis)
                .font(AppTheme.font(14, .semibold))
                .foregroundStyle(AppTheme.ink)
                .textInputAutocapitalization(.sentences)
                .padding(.horizontal, 14)
                .frame(minHeight: 52)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay {
                    RoundedRectangle(cornerRadius: 14, style: .continuous)
                        .stroke(AppTheme.border, lineWidth: 1)
                }
        }
        .frame(maxWidth: .infinity)
    }
}
