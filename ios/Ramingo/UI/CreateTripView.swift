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
            Form {
                Section("Основное") {
                    TextField("Название поездки", text: $title)
                    TextField("Города через запятую", text: $cities)
                }
                Section("Даты") {
                    TextField("Начало · ГГГГ-ММ-ДД", text: $startDate)
                        .textInputAutocapitalization(.never)
                    TextField("Окончание · ГГГГ-ММ-ДД", text: $endDate)
                        .textInputAutocapitalization(.never)
                }
                Section {
                    Button {
                        Task { await save() }
                    } label: {
                        HStack {
                            Spacer()
                            if isSaving { ProgressView() }
                            Text("Сохранить поездку")
                                .fontWeight(.semibold)
                            Spacer()
                        }
                    }
                    .disabled(isSaving || cities.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
                }
                if let localError {
                    Section { Text(localError).foregroundStyle(.red) }
                }
            }
            .navigationTitle("Новая поездка")
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Отмена") { dismiss() } }
            }
        }
    }

    private func save() async {
        localError = nil
        isSaving = true
        do {
            try await model.createTrip(title: title, startDate: startDate, endDate: endDate, cities: cities)
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
        isSaving = false
    }
}
