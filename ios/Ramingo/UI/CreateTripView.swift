import SwiftUI

struct CreateTripView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss
    @State private var title = ""
    @State private var startDate = ""
    @State private var endDate = ""
    @State private var cityQuery = ""
    @State private var selectedCities: [IOSCityCatalogEntry] = []
    @State private var customCities: [String] = []
    @State private var isSaving = false
    @State private var localError: String?

    private var cityValues: [String] {
        var values = selectedCities.map { $0.localizedName(language: model.profile.language) } + customCities
        var seen = Set<String>()
        return values.filter { seen.insert($0.lowercased()).inserted }
    }

    private var citySuggestions: [IOSCityCatalogEntry] {
        guard !cityQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return [] }
        return model.searchCities(query: cityQuery).filter { entry in
            !selectedCities.contains(entry)
        }
    }

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
                            cityPicker
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
                            disabled: isSaving || title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                                startDate.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                                endDate.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty ||
                                cityValues.isEmpty
                        ) {
                            Task { await save() }
                        }
                        .accessibilityIdentifier("createTrip.save")
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
            guard !title.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else {
                throw SupabaseClientError.invalidInput("Укажите название путешествия.")
            }
            guard !startDate.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty,
                  !endDate.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty
            else {
                throw SupabaseClientError.invalidInput("Укажите даты начала и окончания поездки.")
            }
            let cities = cityValues.joined(separator: ", ")
            try await model.createTrip(
                title: title,
                startDate: startDate,
                endDate: endDate,
                cities: cities,
                cityCoordinates: model.resolveCityCoordinates(for: cityValues),
            )
            dismiss()
        } catch {
            localError = error.localizedDescription
        }
    }

    private var cityPicker: some View {
        VStack(alignment: .leading, spacing: 8) {
            Text("Города")
                .font(AppTheme.font(13, .bold))
                .foregroundStyle(AppTheme.label)

            HStack(spacing: 8) {
                TextField("Рим, Прага, Париж…", text: $cityQuery)
                    .font(AppTheme.font(14, .semibold))
                    .textInputAutocapitalization(.words)
                    .autocorrectionDisabled(false)
                    .accessibilityIdentifier("createTrip.cities")
                    .onSubmit { addCitiesFromDraft() }
                Button { addCitiesFromDraft() } label: {
                    Image(systemName: "plus")
                        .font(.system(size: 15, weight: .bold))
                        .foregroundStyle(AppTheme.purple)
                        .frame(width: 34, height: 34)
                        .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 10))
                }
                .buttonStyle(.plain)
                .disabled(cityQuery.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty)
            }
            .padding(.horizontal, 12)
            .frame(minHeight: 52)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(AppTheme.border, lineWidth: 1) }

            if !citySuggestions.isEmpty {
                VStack(spacing: 0) {
                    ForEach(citySuggestions.prefix(6)) { entry in
                        Button { addCity(entry) } label: {
                            HStack(spacing: 9) {
                                Text(entry.flag)
                                Text(entry.localizedName(language: model.profile.language))
                                    .font(AppTheme.font(14, .bold))
                                    .foregroundStyle(AppTheme.ink)
                                Spacer()
                                Image(systemName: "plus.circle")
                                    .foregroundStyle(AppTheme.purple)
                            }
                            .padding(.horizontal, 12)
                            .frame(height: 42)
                        }
                        .buttonStyle(.plain)
                    }
                    Button("Добавить введённый текст") { addCitiesFromDraft() }
                        .font(AppTheme.font(12, .bold))
                        .foregroundStyle(AppTheme.purple)
                        .frame(maxWidth: .infinity, alignment: .leading)
                        .padding(.horizontal, 12)
                        .frame(height: 38)
                }
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                .overlay { RoundedRectangle(cornerRadius: 14, style: .continuous).stroke(AppTheme.border, lineWidth: 1) }
            }

            if !cityValues.isEmpty {
                FlowLayout(spacing: 7) {
                    ForEach(cityValues, id: \.self) { city in
                        HStack(spacing: 5) {
                            Text(model.searchCities(query: city).first?.flag ?? "📍")
                            Text(city).font(AppTheme.font(12, .bold))
                            Button { removeCity(city) } label: {
                                Image(systemName: "xmark.circle.fill")
                                    .font(.system(size: 13))
                                    .foregroundStyle(AppTheme.muted)
                            }
                            .buttonStyle(.plain)
                        }
                        .foregroundStyle(AppTheme.ink)
                        .padding(.horizontal, 10)
                        .frame(height: 32)
                        .background(AppTheme.lavender.opacity(0.7), in: Capsule())
                    }
                }
            }
        }
    }

    private func addCitiesFromDraft() {
        let values = cityQuery
            .split(separator: ",")
            .map { $0.trimmingCharacters(in: .whitespacesAndNewlines) }
            .filter { !$0.isEmpty }
        values.forEach { value in
            if let match = model.searchCities(query: value).first(where: {
                $0.localizedName(language: model.profile.language).localizedCaseInsensitiveCompare(value) == .orderedSame ||
                    $0.aliases.contains(value.lowercased())
            }) {
                addCity(match)
            } else if !customCities.contains(where: { $0.localizedCaseInsensitiveCompare(value) == .orderedSame }) {
                customCities.append(value)
            }
        }
        cityQuery = ""
    }

    private func addCity(_ city: IOSCityCatalogEntry) {
        guard !selectedCities.contains(city) else { return }
        selectedCities.append(city)
        cityQuery = ""
    }

    private func removeCity(_ city: String) {
        selectedCities.removeAll { $0.localizedName(language: model.profile.language).localizedCaseInsensitiveCompare(city) == .orderedSame }
        customCities.removeAll { $0.localizedCaseInsensitiveCompare(city) == .orderedSame }
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
                .accessibilityIdentifier("createTrip.\(title)")
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

private struct FlowLayout: Layout {
    var spacing: CGFloat = 8

    func sizeThatFits(proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) -> CGSize {
        let maxWidth = proposal.width ?? .greatestFiniteMagnitude
        var width: CGFloat = 0
        var height: CGFloat = 0
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if width > 0, width + spacing + size.width > maxWidth {
                height += rowHeight + spacing
                width = 0
                rowHeight = 0
            }
            width += (width == 0 ? 0 : spacing) + size.width
            rowHeight = max(rowHeight, size.height)
        }
        height += rowHeight
        return CGSize(width: proposal.width ?? width, height: height)
    }

    func placeSubviews(in bounds: CGRect, proposal: ProposedViewSize, subviews: Subviews, cache: inout ()) {
        var x = bounds.minX
        var y = bounds.minY
        var rowHeight: CGFloat = 0
        for subview in subviews {
            let size = subview.sizeThatFits(.unspecified)
            if x > bounds.minX, x + size.width > bounds.maxX {
                x = bounds.minX
                y += rowHeight + spacing
                rowHeight = 0
            }
            subview.place(at: CGPoint(x: x, y: y), proposal: ProposedViewSize(size))
            x += size.width + spacing
            rowHeight = max(rowHeight, size.height)
        }
    }
}
