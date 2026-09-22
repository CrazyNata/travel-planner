import Foundation
import PhotosUI
import SwiftUI
import UIKit

struct TripEditorView: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    let tripID: String
    let initialItemID: String?
    let initialCreateSection: String?

    @State private var overview: TripOverview
    @State private var selectedSection: EditorSection = .details
    @State private var title: String
    @State private var dates: String
    @State private var cities: String
    @State private var budgetCurrency: String
    @State private var coverCity: String
    @State private var isSaving = false
    @State private var alertMessage: String?
    @State private var routeEditor: RouteDraft?
    @State private var itemEditor: EditableItem?
    @State private var groupEditor: GroupDraft?
    @State private var inviteEditor = false
    @State private var pendingDelete: DeleteRequest?
    @State private var photoEditor: PhotoEditorRequest?
    @State private var catalogRequest: CatalogEditorRequest?
    @State private var sightNotesDraft: [String: String]
    @State private var selectedPhoto: PhotosPickerItem?
    @State private var didOpenInitialItem = false

    init(
        tripID: String,
        initial: TripOverview,
        initialSection: String = "details",
        initialItemID: String? = nil,
        initialCreateSection: String? = nil,
    ) {
        self.tripID = tripID
        self.initialItemID = initialItemID
        self.initialCreateSection = initialCreateSection
        _overview = State(initialValue: initial)
        _selectedSection = State(initialValue: EditorSection(rawValue: initialSection) ?? .details)
        _title = State(initialValue: initial.title)
        _dates = State(initialValue: initial.dates)
        _cities = State(initialValue: initial.cities.joined(separator: ", "))
        _budgetCurrency = State(initialValue: initial.budgetCurrency)
        _coverCity = State(initialValue: initial.cities.first ?? "")
        _sightNotesDraft = State(initialValue: initial.sightNotes)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    Picker("Раздел", selection: $selectedSection) {
                        ForEach(EditorSection.allCases) { section in
                            Label(section.title, systemImage: section.icon).tag(section)
                        }
                    }
                }

                switch selectedSection {
                case .details:
                    detailsSection
                case .route:
                    routeSection
                case .sights:
                    sightSection
                case .restaurants:
                    itemSection(kind: .restaurant, items: restaurantItems)
                case .accommodation:
                    itemSection(kind: .accommodation, items: accommodationItems)
                case .budget:
                    budgetSection
                case .members:
                    membersSection
                case .photos:
                    photosSection
                case .pets:
                    itemSection(kind: .pet, items: petItems)
                }
            }
            .font(AppTheme.font(14, .semibold))
            .scrollContentBackground(.hidden)
            .background(AppTheme.background)
            .tint(AppTheme.purple)
            .navigationTitle("Редактировать поездку")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Готово") { dismiss() }
                }
                if selectedSection == .route || selectedSection == .sights || selectedSection == .accommodation {
                    ToolbarItem(placement: .primaryAction) { EditButton() }
                }
            }
            .onAppear { openInitialItemIfNeeded() }
            .overlay {
                if isSaving {
                    ProgressView("Сохраняем…")
                        .padding(22)
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                }
            }
            .alert("Не удалось сохранить изменения", isPresented: Binding(
                get: { alertMessage != nil },
                set: { if !$0 { alertMessage = nil } },
            )) {
                Button("ОК", role: .cancel) { alertMessage = nil }
            } message: {
                Text(alertMessage ?? "")
            }
            .alert("Удалить элемент?", isPresented: Binding(
                get: { pendingDelete != nil },
                set: { if !$0 { pendingDelete = nil } },
            )) {
                Button("Удалить", role: .destructive) {
                    guard let request = pendingDelete else { return }
                    pendingDelete = nil
                    Task { await delete(request) }
                }
                Button("Отмена", role: .cancel) { pendingDelete = nil }
            } message: {
                Text(pendingDelete?.message ?? "Действие нельзя отменить.")
            }
            .sheet(item: $routeEditor) { draft in
                RouteEditorSheet(initial: draft) { updated in
                    routeEditor = nil
                    Task { await saveRoute(updated) }
                }
            }
            .sheet(item: $itemEditor) { item in
                ItemEditorSheet(
                    initial: item,
                    onCancel: { itemEditor = nil },
                    onSave: { updated in
                        itemEditor = nil
                        Task { await saveItem(updated) }
                    },
                )
            }
            .sheet(item: $groupEditor) { draft in
                GroupEditorSheet(initial: draft) { updated in
                    groupEditor = nil
                    Task { await saveGroup(updated) }
                }
            }
            .sheet(isPresented: $inviteEditor) {
                MemberInviteSheet { name, email, role in
                    inviteEditor = false
                    Task { await invite(name: name, email: email, role: role) }
                }
            }
            .sheet(item: $photoEditor) { request in
                ItemPhotoManagerSheet(request: request) { action in
                    photoEditor = nil
                    Task { await handlePhotoAction(request, action: action) }
                }
                .environmentObject(model)
            }
            .sheet(item: $catalogRequest) { request in
                CatalogPickerSheet(
                    kind: request.kind,
                    cities: catalogCities,
                    defaultCity: request.city,
                ) { entry, walkDay in
                    catalogRequest = nil
                    Task { await addCatalogEntry(entry, walkDay: walkDay) }
                }
                .environmentObject(model)
            }
        }
    }

    private var detailsSection: some View {
        Section("Основное") {
            TextField("Название поездки", text: $title)
            TextField("Даты", text: $dates)
            TextField("Города через запятую", text: $cities, axis: .vertical)
                .lineLimit(2...4)
            Button("Сохранить основные данные") {
                Task { await saveDetails() }
            }
            .disabled(isSaving)
        }
    }

    private var routeSection: some View {
        Section {
            if overview.routeLegs.isEmpty {
                Text("Переездов пока нет.").foregroundStyle(.secondary)
            }
            ForEach(overview.routeLegs) { leg in
                Button {
                    routeEditor = RouteDraft(leg: leg)
                } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text("\(leg.from) → \(leg.to)").font(.headline)
                        Text([leg.date, leg.travelTime, leg.distance].filter { !$0.isEmpty }.joined(separator: " · "))
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                }
                .foregroundStyle(.primary)
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button(role: .destructive) {
                        pendingDelete = DeleteRequest(section: "days", itemID: leg.id, message: "Переезд «\(leg.from) → \(leg.to)» будет удалён.")
                    } label: {
                        Label("Удалить", systemImage: "trash")
                    }
                }
            }
            .onMove { source, destination in
                var orderedIDs = overview.routeLegs.map(\.id)
                orderedIDs.move(fromOffsets: source, toOffset: destination)
                Task { await reorderRoute(orderedIDs) }
            }
            Button {
                routeEditor = RouteDraft.new()
            } label: {
                Label("Добавить переезд", systemImage: "plus")
            }
        } header: {
            Text("Маршрут")
        } footer: {
            Text("Редактор сохраняет дополнительные поля переезда, которые уже есть в поездке.")
        }
    }

    private var sightSection: some View {
        Group {
            sightDaysSection
            itemSection(kind: .sight, items: sightItems)
        }
    }

    private var sightDaysSection: some View {
        Section {
            if sightDayItems.isEmpty {
                Text("Дни для планирования мест пока не созданы.").foregroundStyle(.secondary)
            } else {
                ForEach(Array(sightDayItems.enumerated()), id: \.element.id) { index, day in
                    VStack(alignment: .leading, spacing: 8) {
                        HStack {
                            Text("День \(index + 1)").font(.caption.weight(.semibold)).foregroundStyle(.secondary)
                            Text(day.title).font(.headline)
                            Spacer()
                        }
                        TextField("Заметки на этот день", text: Binding(
                            get: { sightNotesDraft[day.id] ?? "" },
                            set: { sightNotesDraft[day.id] = $0 },
                        ), axis: .vertical)
                        .lineLimit(2...4)
                        Button("Сохранить заметки") {
                            Task { await saveSightNotes(dayID: day.id) }
                        }
                        .font(.caption.weight(.semibold))
                    }
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            pendingDelete = DeleteRequest(
                                section: "sightDay",
                                itemID: String(index + 1),
                                message: "Все места этого дня будут удалены из поездки.",
                            )
                        } label: {
                            Label("Удалить день", systemImage: "trash")
                        }
                    }
                }
                .onMove { source, destination in
                    var orderedIDs = sightDayItems.map(\.id)
                    orderedIDs.move(fromOffsets: source, toOffset: destination)
                    Task { await reorderSightDays(currentIDs: sightDayItems.map(\.id), orderedIDs: orderedIDs) }
                }
            }
        } header: {
            Text("Планирование по дням")
        } footer: {
            Text("Порядок дней и заметки синхронизируются с Android и веб-клиентом.")
        }
    }

    private var budgetSection: some View {
        Group {
            Section("Валюта") {
                TextField("Например, EUR", text: $budgetCurrency)
                    .textInputAutocapitalization(.characters)
                Button("Сохранить валюту") {
                    Task { await saveBudgetCurrency() }
                }
            }

            itemSection(kind: .expense, items: expenseItems)

            Section("Группы бюджета") {
                if overview.budgetGroups.isEmpty {
                    Text("Групп пока нет.").foregroundStyle(.secondary)
                }
                ForEach(overview.budgetGroups) { group in
                    Button {
                        groupEditor = GroupDraft(group: group)
                    } label: {
                        HStack {
                            Text(group.name)
                            Spacer()
                            Text("\(group.people)")
                                .foregroundStyle(.secondary)
                        }
                    }
                    .foregroundStyle(.primary)
                    .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                        Button(role: .destructive) {
                            pendingDelete = DeleteRequest(section: "budgetGroup", itemID: group.id, message: "Группа «\(group.name)» будет удалена.")
                        } label: {
                            Label("Удалить", systemImage: "trash")
                        }
                    }
                }
                Button {
                    groupEditor = GroupDraft.new()
                } label: {
                    Label("Добавить группу", systemImage: "plus")
                }
            }
        }
    }

    private var membersSection: some View {
        Section {
            ForEach(overview.members) { member in
                HStack(spacing: 12) {
                    Circle()
                        .fill(AppTheme.lavender)
                        .frame(width: 36, height: 36)
                        .overlay {
                            Text(member.initials)
                                .font(.caption.weight(.bold))
                                .foregroundStyle(AppTheme.purpleDeep)
                        }
                    VStack(alignment: .leading, spacing: 2) {
                        Text(member.name).font(.headline)
                        if !member.email.isEmpty { Text(member.email).font(.caption).foregroundStyle(.secondary) }
                    }
                    Spacer()
                    if canManageMembers, member.id != model.currentUser?.id {
                        Menu {
                            Button("Редактор") { Task { await changeRole(member, to: "Редактор") } }
                            Button("Читатель") { Task { await changeRole(member, to: "Читатель") } }
                        } label: {
                            Text(member.role.isEmpty ? "Роль" : member.role)
                                .font(.caption.weight(.semibold))
                        }
                    } else {
                        Text(member.role).font(.caption).foregroundStyle(.secondary)
                    }
                }
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    if canManageMembers, member.id != model.currentUser?.id {
                        Button(role: .destructive) {
                            pendingDelete = DeleteRequest(section: "members", itemID: member.id, message: "Участник «\(member.name)» будет удалён из поездки.")
                        } label: {
                            Label("Удалить", systemImage: "person.badge.minus")
                        }
                    }
                }
            }
            if canManageMembers {
                Button { inviteEditor = true } label: {
                    Label("Пригласить участника", systemImage: "person.badge.plus")
                }
            }
        } header: {
            Text("Участники")
        } footer: {
            Text(canManageMembers ? "Роли и удаление участников изменяются владельцем поездки." : "Управление участниками доступно только владельцу поездки.")
        }
    }

    private var photosSection: some View {
        Section {
            TextField("Город для нового фото", text: $coverCity)
            if overview.coverPhotos.isEmpty {
                Text("Фото поездки пока нет.").foregroundStyle(.secondary)
            }
            ForEach(overview.coverPhotos) { photo in
                HStack {
                    RemotePhotoView(reference: photo.reference, client: model.client, contentMode: .fill, cornerRadius: 10)
                        .frame(width: 58, height: 44)
                    VStack(alignment: .leading, spacing: 2) {
                        Text(photo.city.isEmpty ? "Фото поездки" : photo.city)
                            .font(.headline)
                        Text("Обложка").font(.caption).foregroundStyle(.secondary)
                    }
                }
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button(role: .destructive) {
                        pendingDelete = DeleteRequest(section: "coverPhotos", itemID: photo.id, message: "Это фото будет удалено из обложек поездки.")
                    } label: {
                        Label("Удалить", systemImage: "trash")
                    }
                }
            }
            PhotosPicker(selection: $selectedPhoto, matching: .images) {
                Label("Добавить фото", systemImage: "photo.badge.plus")
            }
            .onChange(of: selectedPhoto) { _, item in
                guard let item else { return }
                Task { await importPhoto(item) }
            }
        } header: {
            Text("Фото поездки")
        } footer: {
            Text("Изображение загрузится в Supabase Storage и будет доступно остальным участникам поездки.")
        }
    }

    @ViewBuilder
    private func itemSection(kind: EditorItemKind, items: [EditableItem]) -> some View {
        Section {
            if items.isEmpty {
                Text("Элементов пока нет.").foregroundStyle(.secondary)
            }
            ForEach(items) { item in
                Button { itemEditor = item } label: {
                    VStack(alignment: .leading, spacing: 4) {
                        Text(item.name).font(.headline)
                        let subtitle = [item.city, item.category, item.status, item.amountText]
                            .filter { !$0.isEmpty }
                            .joined(separator: " · ")
                        if !subtitle.isEmpty { Text(subtitle).font(.caption).foregroundStyle(.secondary) }
                    }
                }
                .foregroundStyle(.primary)
                .swipeActions(edge: .trailing, allowsFullSwipe: false) {
                    Button(role: .destructive) {
                        pendingDelete = DeleteRequest(section: kind.section, itemID: item.id, message: "«\(item.name)» будет удалено.")
                    } label: {
                        Label("Удалить", systemImage: "trash")
                    }
                }
                .contextMenu {
                    if kind.supportsPhotos {
                        Button {
                            photoEditor = PhotoEditorRequest(item: item)
                        } label: {
                            Label("Фотографии", systemImage: "photo.on.rectangle")
                        }
                    }
                }
            }
            .onMove { source, destination in
                guard kind == .accommodation || kind == .sight else { return }
                var orderedIDs = items.map(\.id)
                orderedIDs.move(fromOffsets: source, toOffset: destination)
                Task {
                    if kind == .accommodation {
                        await reorderAccommodations(orderedIDs)
                    } else {
                        await reorderSights(orderedIDs)
                    }
                }
            }
            if let catalogCategory = kind.catalogCategory {
                Button {
                    catalogRequest = CatalogEditorRequest(
                        kind: catalogCategory,
                        city: catalogCities.first ?? "",
                    )
                } label: {
                    Label("Добавить из каталога", systemImage: "books.vertical")
                }
            }
            Button { itemEditor = EditableItem.new(kind: kind) } label: {
                Label("Добавить", systemImage: "plus")
            }
        } header: {
            Text(kind.title)
        }
    }

    private var canManageMembers: Bool {
        overview.currentUserRole == "Владелец"
    }

    private var sightItems: [EditableItem] { overview.sights.map(EditableItem.init) }
    private var restaurantItems: [EditableItem] { overview.restaurants.map(EditableItem.init) }
    private var accommodationItems: [EditableItem] { overview.accommodations.map(EditableItem.init) }
    private var expenseItems: [EditableItem] { overview.budgetExpenses.map(EditableItem.init) }
    private var petItems: [EditableItem] { overview.petPlaces.map(EditableItem.init) }

    private var catalogCities: [String] {
        (
            overview.cities +
                overview.routeLegs.flatMap { [$0.from, $0.to] } +
                overview.sights.map(\.city) +
                overview.restaurants.map(\.city) +
                overview.accommodations.map(\.city) +
                overview.petPlaces.map(\.city)
        )
        .flatMap { $0.split(separator: ",").map { $0.trimmingCharacters(in: .whitespacesAndNewlines) } }
        .filter { !$0.isEmpty }
        .reduce(into: [String]()) { values, city in
            if !values.contains(where: { $0.caseInsensitiveCompare(city) == .orderedSame }) {
                values.append(city)
            }
        }
    }

    private var sightDayItems: [SightDay] {
        if !overview.sightDays.isEmpty { return overview.sightDays }
        return overview.routeLegs.enumerated().map { index, leg in
            SightDay(id: "route-day-\(index + 1)", title: leg.to, photo: "", photoPosition: nil)
        }
    }

    private func saveDetails() async {
        beginSave()
        do {
            try await model.updateTripDetails(id: tripID, title: title, dates: dates, cities: cities)
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func saveBudgetCurrency() async {
        beginSave()
        do {
            let value = budgetCurrency.trimmingCharacters(in: .whitespacesAndNewlines).uppercased()
            guard !value.isEmpty else { throw EditorError.validation("Укажите валюту бюджета.") }
            try await model.updateTripField(id: tripID, key: "budgetCurrency", value: .string(value))
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func saveRoute(_ draft: RouteDraft) async {
        beginSave()
        do {
            if draft.isNew {
                try await model.addRouteLeg(
                    id: tripID,
                    from: draft.from,
                    to: draft.to,
                    checkIn: draft.checkIn,
                    checkOut: draft.checkOut,
                    notes: draft.notes,
                    mapsURL: draft.mapsURL,
                    date: draft.date,
                    dateDay: draft.dateDay,
                    dateMonth: draft.dateMonth,
                    weekday: draft.weekday,
                    distance: draft.distance,
                    travelTime: draft.travelTime,
                )
            } else {
                try await model.updateRouteLegDetails(
                    id: tripID,
                    dayID: draft.id,
                    from: draft.from,
                    to: draft.to,
                    checkIn: draft.checkIn,
                    checkOut: draft.checkOut,
                    notes: draft.notes,
                    mapsURL: draft.mapsURL,
                    date: draft.date,
                    dateDay: draft.dateDay,
                    dateMonth: draft.dateMonth,
                    weekday: draft.weekday,
                    distance: draft.distance,
                    travelTime: draft.travelTime,
                )
            }
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func reorderRoute(_ orderedIDs: [String]) async {
        beginSave()
        do {
            try await model.reorderRouteLegs(id: tripID, orderedDayIDs: orderedIDs)
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func reorderAccommodations(_ orderedIDs: [String]) async {
        beginSave()
        do {
            try await model.reorderAccommodations(id: tripID, orderedAccommodationIDs: orderedIDs)
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func reorderSights(_ orderedIDs: [String]) async {
        beginSave()
        do {
            try await model.reorderSights(id: tripID, orderedSightIDs: orderedIDs)
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func reorderSightDays(currentIDs: [String], orderedIDs: [String]) async {
        beginSave()
        do {
            try await model.reorderSightDays(id: tripID, currentDayIDs: currentIDs, orderedDayIDs: orderedIDs)
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func saveSightNotes(dayID: String) async {
        beginSave()
        do {
            try await model.updateSightNotes(id: tripID, dayID: dayID, notes: sightNotesDraft[dayID] ?? "")
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func saveItem(_ item: EditableItem) async {
        beginSave()
        do {
            guard item.isValid else { throw EditorError.validation(item.validationMessage) }
            if item.isNew {
                try await model.addTripArrayItem(id: tripID, section: item.kind.section, item: item.insertObject)
            } else {
                try await model.updateTripArrayItem(id: tripID, section: item.kind.section, itemID: item.id, fields: item.fields)
            }
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func addCatalogEntry(_ entry: CatalogEntry, walkDay: Int) async {
        beginSave()
        do {
            try await model.addCatalogItem(
                id: tripID,
                entry: entry,
                walkDay: walkDay,
            )
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func saveGroup(_ draft: GroupDraft) async {
        beginSave()
        do {
            guard draft.isValid else { throw EditorError.validation("Укажите название группы и количество участников.") }
            if draft.isNew {
                try await model.addBudgetGroup(id: tripID, name: draft.name, people: draft.people)
            } else {
                try await model.updateBudgetGroup(id: tripID, groupID: draft.id, name: draft.name, people: draft.people)
            }
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func changeRole(_ member: TripMember, to role: String) async {
        beginSave()
        do {
            try await model.updateMemberRole(id: tripID, memberID: member.id, role: role)
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func invite(name: String, email: String, role: String) async {
        beginSave()
        do {
            try await model.inviteMember(id: tripID, name: name, email: email, role: role)
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func importPhoto(_ item: PhotosPickerItem) async {
        beginSave()
        do {
            guard let data = try await item.loadTransferable(type: Data.self) else {
                throw EditorError.validation("Не удалось прочитать изображение.")
            }
            try await model.addCoverPhoto(id: tripID, data: normalizedImageData(data), city: coverCity)
            selectedPhoto = nil
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        selectedPhoto = nil
        endSave()
    }

    private func handlePhotoAction(_ request: PhotoEditorRequest, action: PhotoAction) async {
        beginSave()
        do {
            switch action {
            case .add(let data):
                try await model.addItemPhoto(id: tripID, section: request.section, itemID: request.itemID, data: normalizedImageData(data))
            case .replace(let data):
                try await model.replaceItemCoverPhoto(id: tripID, section: request.section, itemID: request.itemID, data: normalizedImageData(data))
            case .move(let index, let direction):
                try await model.moveItemPhoto(id: tripID, section: request.section, itemID: request.itemID, photoIndex: index, direction: direction)
            case .delete(let index):
                try await model.deleteItemPhoto(id: tripID, section: request.section, itemID: request.itemID, photoIndex: index)
            }
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func normalizedImageData(_ data: Data) -> Data {
        UIImage(data: data)?.jpegData(compressionQuality: 0.88) ?? data
    }

    private func delete(_ request: DeleteRequest) async {
        beginSave()
        do {
            switch request.section {
            case "budgetGroup":
                try await model.deleteBudgetGroup(id: tripID, groupID: request.itemID)
            case "members":
                try await model.removeMember(id: tripID, memberID: request.itemID)
            case "coverPhotos":
                try await model.deleteCoverPhoto(id: tripID, photoID: request.itemID)
            case "sightDay":
                try await model.deleteSightDay(id: tripID, walkDay: Int(request.itemID) ?? 1)
            default:
                try await model.deleteTripItem(id: tripID, section: request.section, itemID: request.itemID)
            }
            try await reload()
        } catch {
            alertMessage = error.localizedDescription
        }
        endSave()
    }

    private func reload() async throws {
        guard let fresh = try await model.overview(for: tripID) else {
            throw EditorError.validation("Путешествие не найдено.")
        }
        overview = fresh
        title = fresh.title
        dates = fresh.dates
        cities = fresh.cities.joined(separator: ", ")
        budgetCurrency = fresh.budgetCurrency
        sightNotesDraft = fresh.sightNotes
        if coverCity.isEmpty { coverCity = fresh.cities.first ?? "" }
    }

    private func openInitialItemIfNeeded() {
        guard !didOpenInitialItem else { return }
        didOpenInitialItem = true

        if let initialCreateSection,
           let kind = editorItemKind(for: initialCreateSection) {
            itemEditor = EditableItem.new(kind: kind)
            return
        }

        guard let initialItemID, !initialItemID.isEmpty else { return }

        switch selectedSection {
        case .route:
            guard let leg = overview.routeLegs.first(where: { $0.id == initialItemID }) else { return }
            routeEditor = RouteDraft(leg: leg)
        case .sights:
            guard let sight = overview.sights.first(where: { $0.id == initialItemID }) else { return }
            itemEditor = EditableItem(sight)
        case .restaurants:
            guard let restaurant = overview.restaurants.first(where: { $0.id == initialItemID }) else { return }
            itemEditor = EditableItem(restaurant)
        case .accommodation:
            guard let accommodation = overview.accommodations.first(where: { $0.id == initialItemID }) else { return }
            itemEditor = EditableItem(accommodation)
        case .pets:
            guard let pet = overview.petPlaces.first(where: { $0.id == initialItemID }) else { return }
            itemEditor = EditableItem(pet)
        default:
            break
        }
    }

    private func editorItemKind(for section: String) -> EditorItemKind? {
        switch section {
        case "sights": return .sight
        case "restaurants": return .restaurant
        case "accommodation": return .accommodation
        case "budgetExpenses": return .expense
        case "pets": return .pet
        default: return nil
        }
    }

    private func beginSave() {
        isSaving = true
        alertMessage = nil
    }

    private func endSave() {
        isSaving = false
    }
}

private enum EditorSection: String, CaseIterable, Identifiable, Hashable {
    case details
    case route
    case sights
    case restaurants
    case accommodation
    case budget
    case members
    case photos
    case pets

    var id: String { rawValue }

    var title: String {
        switch self {
        case .details: return "Основное"
        case .route: return "Маршрут"
        case .sights: return "Места"
        case .restaurants: return "Рестораны"
        case .accommodation: return "Жильё"
        case .budget: return "Бюджет"
        case .members: return "Участники"
        case .photos: return "Фото"
        case .pets: return "Питомцы"
        }
    }

    var icon: String {
        switch self {
        case .details: return "pencil"
        case .route: return "point.topleft.down.to.point.bottomright.curvepath"
        case .sights: return "building.columns"
        case .restaurants: return "fork.knife"
        case .accommodation: return "bed.double"
        case .budget: return "creditcard"
        case .members: return "person.2"
        case .photos: return "photo.on.rectangle"
        case .pets: return "pawprint"
        }
    }

}

private enum EditorItemKind: String, Equatable {
    case sight
    case restaurant
    case accommodation
    case expense
    case pet

    var section: String {
        switch self {
        case .sight: return "sights"
        case .restaurant: return "restaurants"
        case .accommodation: return "accommodations"
        case .expense: return "budgetExpenses"
        case .pet: return "petPlaces"
        }
    }

    var title: String {
        switch self {
        case .sight: return "Места"
        case .restaurant: return "Рестораны"
        case .accommodation: return "Жильё"
        case .expense: return "Расходы"
        case .pet: return "Питомцы"
        }
    }

    var editorTitle: String {
        switch self {
        case .sight: return "место"
        case .restaurant: return "ресторан"
        case .accommodation: return "жильё"
        case .expense: return "расход"
        case .pet: return "место"
        }
    }

    var supportsPhotos: Bool {
        self == .sight || self == .restaurant || self == .accommodation
    }

    var catalogCategory: CatalogCategory? {
        switch self {
        case .sight: return .sight
        case .restaurant: return .restaurant
        case .accommodation: return .accommodation
        case .pet: return .pet
        case .expense: return nil
        }
    }
}

private struct EditableItem: Identifiable {
    let id: String
    let kind: EditorItemKind
    var isNew: Bool
    var name: String
    var city: String
    var category: String = ""
    var description: String = ""
    var link: String = ""
    var walkDayText: String = "1"
    var latitudeText: String = ""
    var longitudeText: String = ""
    var status: String = ""
    var note: String = ""
    var price: String = ""
    var dates: String = ""
    var details: String = ""
    var bookingURL: String = ""
    var deadline: String = ""
    var source: String = ""
    var googlePlaceID: String = ""
    var bookingPropertyID: String = ""
    var externalURL: String = ""
    var address: String = ""
    var reviewCountText: String = ""
    var photoReference: String = ""
    var type: String = ""
    var tripCityID: String = ""
    var amountText: String = ""
    var inputCurrencyRateText: String = ""
    var scope: String = ""
    var paidBy: String = ""
    var date: String = ""
    var inputCurrency: String = ""
    var phone: String = ""
    var website: String = ""
    var mapsURL: String = ""
    var noteForPet: String = ""
    var featuresText: String = ""
    var photos: [String] = []
    var priority = false
    var done = false

    init(id: String, kind: EditorItemKind, isNew: Bool, name: String, city: String) {
        self.id = id
        self.kind = kind
        self.isNew = isNew
        self.name = name
        self.city = city
    }

    init(_ sight: Sight) {
        self.init(id: sight.id, kind: .sight, isNew: false, name: sight.name, city: sight.city)
        category = sight.category
        description = sight.description
        link = sight.link
        walkDayText = String(sight.walkDay)
        latitudeText = sight.latitude.map { String($0) } ?? ""
        longitudeText = sight.longitude.map { String($0) } ?? ""
        photos = sight.photos
        done = sight.done
    }

    init(_ restaurant: Restaurant) {
        self.init(id: restaurant.id, kind: .restaurant, isNew: false, name: restaurant.name, city: restaurant.city)
        status = restaurant.status
        note = restaurant.note
        price = restaurant.price
        link = restaurant.link
        date = restaurant.date
        photos = restaurant.photos
        priority = restaurant.priority
    }

    init(_ accommodation: Accommodation) {
        self.init(id: accommodation.id, kind: .accommodation, isNew: false, name: accommodation.name, city: accommodation.city)
        dates = accommodation.dates
        price = accommodation.price
        status = accommodation.status
        details = accommodation.details
        bookingURL = accommodation.bookingURL
        deadline = accommodation.deadline
        source = accommodation.source
        googlePlaceID = accommodation.googlePlaceID
        bookingPropertyID = accommodation.bookingPropertyID
        externalURL = accommodation.externalURL
        address = accommodation.address
        latitudeText = accommodation.latitude.map { String($0) } ?? ""
        longitudeText = accommodation.longitude.map { String($0) } ?? ""
        reviewCountText = accommodation.reviewCount.map(String.init) ?? ""
        photoReference = accommodation.photoReference
        website = accommodation.website
        phone = accommodation.phone
        type = accommodation.type
        tripCityID = accommodation.tripCityID
        photos = accommodation.photos
    }

    init(_ expense: BudgetExpense) {
        self.init(id: expense.id, kind: .expense, isNew: false, name: expense.name, city: "")
        amountText = String(expense.amount)
        category = expense.category
        scope = expense.scope
        paidBy = expense.paidBy
        date = expense.date
        inputCurrency = expense.inputCurrency
        inputCurrencyRateText = expense.inputCurrencyRate.map { String($0) } ?? ""
    }

    init(_ pet: PetPlace) {
        self.init(id: pet.id, kind: .pet, isNew: false, name: pet.name, city: pet.city)
        category = pet.type
        details = pet.address
        phone = pet.phone
        mapsURL = pet.mapsURL
        website = pet.website
        latitudeText = pet.latitude.map { String($0) } ?? ""
        longitudeText = pet.longitude.map { String($0) } ?? ""
        reviewCountText = pet.reviewCount.map(String.init) ?? ""
        noteForPet = pet.note
        featuresText = pet.features.joined(separator: ", ")
    }

    static func new(kind: EditorItemKind) -> EditableItem {
        var item = EditableItem(id: UUID().uuidString.lowercased(), kind: kind, isNew: true, name: "", city: "")
        switch kind {
        case .restaurant:
            item.status = "хочу"
            item.price = "€€"
        case .accommodation:
            item.status = "хочу"
        case .pet:
            item.category = "shop"
        case .expense:
            item.amountText = "0"
        case .sight:
            break
        }
        return item
    }

    var isValid: Bool {
        guard !name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty else { return false }
        if kind == .expense { return Double(amountText.replacingOccurrences(of: ",", with: ".")) ?? 0 > 0 }
        return true
    }

    var validationMessage: String {
        if name.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty { return "Укажите название." }
        if kind == .expense, (Double(amountText.replacingOccurrences(of: ",", with: ".")) ?? 0) <= 0 { return "Укажите сумму больше нуля." }
        return "Проверьте заполненные поля."
    }

    var fields: [String: JSONValue] {
        var result: [String: JSONValue] = [
            "name": .string(name.trimmed),
            "city": .string(city.trimmed),
        ]
        switch kind {
        case .sight:
            result["subcategory"] = .string(category.trimmed)
            result["description"] = .string(description.trimmed)
            result["link"] = .string(link.trimmed)
            result["done"] = .boolean(done)
            result["walkDay"] = .number(Double(max(Int(walkDayText) ?? 1, 1)))
            if let latitude = Double(latitudeText.replacingOccurrences(of: ",", with: ".")),
               let longitude = Double(longitudeText.replacingOccurrences(of: ",", with: ".")) {
                result["lnglat"] = .array([.number(longitude), .number(latitude)])
            } else {
                result.removeValue(forKey: "lnglat")
            }
        case .restaurant:
            result["status"] = .string(status.trimmed)
            result["note"] = .string(note.trimmed)
            result["price"] = .string(price.trimmed)
            result["link"] = .string(link.trimmed)
            result["date"] = .string(date.trimmed)
            result["priority"] = .boolean(priority)
        case .accommodation:
            result["dates"] = .string(dates.trimmed)
            result["price"] = .string(price.trimmed)
            result["status"] = .string(status.trimmed)
            result["details"] = .string(details.trimmed)
            result["bookingUrl"] = .string(bookingURL.trimmed)
            result["deadline"] = .string(deadline.trimmed)
            result["source"] = .string(source.trimmed)
            result["googlePlaceId"] = .string(googlePlaceID.trimmed)
            result["bookingPropertyId"] = .string(bookingPropertyID.trimmed)
            result["externalUrl"] = .string(externalURL.trimmed)
            result["address"] = .string(address.trimmed)
            result["photoReference"] = .string(photoReference.trimmed)
            result["website"] = .string(website.trimmed)
            result["phone"] = .string(phone.trimmed)
            result["type"] = .string(type.trimmed)
            result["tripCityId"] = .string(tripCityID.trimmed)
            if let latitude = Double(latitudeText.replacingOccurrences(of: ",", with: ".")),
               let longitude = Double(longitudeText.replacingOccurrences(of: ",", with: ".")) {
                result["latitude"] = .number(latitude)
                result["longitude"] = .number(longitude)
            }
            if let reviewCount = Int(reviewCountText) { result["reviewCount"] = .number(Double(reviewCount)) }
        case .expense:
            result["amount"] = .number(Double(amountText.replacingOccurrences(of: ",", with: ".")) ?? 0)
            result["category"] = .string(category.trimmed)
            result["scope"] = .string(scope.trimmed)
            result["paidBy"] = .string(paidBy.trimmed)
            result["date"] = .string(date.trimmed)
            result["inputCurrency"] = .string(inputCurrency.trimmed)
            if let rate = Double(inputCurrencyRateText.replacingOccurrences(of: ",", with: ".")), rate > 0 {
                result["inputCurrencyRate"] = .number(rate)
            }
        case .pet:
            result["type"] = .string(category.trimmed)
            result["address"] = .string(details.trimmed)
            result["phone"] = .string(phone.trimmed)
            result["mapsUrl"] = .string(mapsURL.trimmed)
            result["website"] = .string(website.trimmed)
            result["note"] = .string(noteForPet.trimmed)
            result["features"] = .array(featuresText.split(separator: ",").map { .string(String($0).trimmed) }.filter { $0.stringValue?.isEmpty == false })
            if let latitude = Double(latitudeText.replacingOccurrences(of: ",", with: ".")),
               let longitude = Double(longitudeText.replacingOccurrences(of: ",", with: ".")) {
                result["latitude"] = .number(latitude)
                result["longitude"] = .number(longitude)
            }
        }
        return result
    }

    var insertObject: [String: JSONValue] {
        var result = fields
        result["id"] = .string(id)
        if kind == .restaurant || kind == .accommodation { result["photos"] = .array([]) }
        return result
    }
}

private struct RouteDraft: Identifiable {
    let id: String
    let isNew: Bool
    var from: String
    var to: String
    var checkIn: String
    var checkOut: String
    var notes: String
    var mapsURL: String
    var date: String
    var dateDay: String
    var dateMonth: String
    var weekday: String
    var distance: String
    var travelTime: String

    init(leg: RouteLeg) {
        id = leg.id
        isNew = false
        from = leg.from
        to = leg.to
        checkIn = leg.checkIn
        checkOut = leg.checkOut
        notes = leg.notes
        mapsURL = leg.mapsURL
        date = leg.date
        dateDay = leg.dateDay
        dateMonth = leg.dateMonth
        weekday = leg.weekday
        distance = leg.distance
        travelTime = leg.travelTime
    }

    private init(id: String, isNew: Bool) {
        self.id = id
        self.isNew = isNew
        from = ""
        to = ""
        checkIn = ""
        checkOut = ""
        notes = ""
        mapsURL = ""
        date = ""
        dateDay = ""
        dateMonth = ""
        weekday = ""
        distance = ""
        travelTime = ""
    }

    static func new() -> RouteDraft { RouteDraft(id: UUID().uuidString.lowercased(), isNew: true) }
}

private struct GroupDraft: Identifiable {
    let id: String
    let isNew: Bool
    var name: String
    var peopleText: String

    init(group: BudgetGroup) {
        id = group.id
        isNew = false
        name = group.name
        peopleText = String(group.people)
    }

    private init(id: String, isNew: Bool) {
        self.id = id
        self.isNew = isNew
        name = ""
        peopleText = "1"
    }

    static func new() -> GroupDraft { GroupDraft(id: UUID().uuidString.lowercased(), isNew: true) }

    var people: Int { Int(peopleText) ?? 0 }
    var isValid: Bool { !name.trimmed.isEmpty && people > 0 }
}

private struct DeleteRequest {
    let section: String
    let itemID: String
    let message: String
}

private struct CatalogEditorRequest: Identifiable {
    let kind: CatalogCategory
    let city: String

    var id: String { "\(kind.rawValue)-\(city)" }
}

private enum EditorError: LocalizedError {
    case validation(String)

    var errorDescription: String? {
        switch self {
        case .validation(let message): return message
        }
    }
}

private struct RouteEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var draft: RouteDraft
    let onSave: (RouteDraft) -> Void

    init(initial: RouteDraft, onSave: @escaping (RouteDraft) -> Void) {
        _draft = State(initialValue: initial)
        self.onSave = onSave
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Города") {
                    TextField("Откуда", text: $draft.from)
                    TextField("Куда", text: $draft.to)
                }
                Section("Дата и время") {
                    TextField("Дата", text: $draft.date)
                    TextField("Число", text: $draft.dateDay).keyboardType(.numberPad)
                    TextField("Месяц", text: $draft.dateMonth)
                    TextField("День недели", text: $draft.weekday)
                    TextField("Время отправления", text: $draft.checkIn)
                    TextField("Время прибытия", text: $draft.checkOut)
                }
                Section("Детали") {
                    TextField("Расстояние", text: $draft.distance)
                    TextField("Время в пути", text: $draft.travelTime)
                    TextField("Ссылка на карту", text: $draft.mapsURL)
                        .keyboardType(.URL)
                        .textInputAutocapitalization(.never)
                    TextField("Заметки", text: $draft.notes, axis: .vertical)
                        .lineLimit(3...6)
                }
            }
            .navigationTitle(draft.isNew ? "Новый переезд" : "Переезд")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Отмена") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Сохранить") {
                        onSave(draft)
                        dismiss()
                    }
                }
            }
        }
    }
}

private struct ItemEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var item: EditableItem
    let onCancel: () -> Void
    let onSave: (EditableItem) -> Void

    init(initial: EditableItem, onCancel: @escaping () -> Void, onSave: @escaping (EditableItem) -> Void) {
        _item = State(initialValue: initial)
        self.onCancel = onCancel
        self.onSave = onSave
    }

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                HStack(alignment: .center, spacing: 12) {
                    Text(item.isNew ? "Добавить \(item.kind.editorTitle)" : "Редактировать \(item.kind.editorTitle)")
                        .font(AppTheme.font(23, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                    Spacer(minLength: 0)
                    Button { onCancel(); dismiss() } label: {
                        Image(systemName: "xmark")
                            .font(.system(size: 17, weight: .bold))
                            .foregroundStyle(AppTheme.muted)
                            .frame(width: 37, height: 37)
                            .background(AppTheme.surface2, in: Circle())
                    }
                    .buttonStyle(.plain)
                }

                editorField("Название *", text: $item.name, placeholder: namePlaceholder)
                editorField("Город *", text: $item.city, placeholder: "Выберите город")
                fields

                HStack(spacing: 10) {
                    Button("Отмена") { onCancel(); dismiss() }
                        .font(AppTheme.font(14, .extrabold))
                        .foregroundStyle(AppTheme.ink)
                        .frame(maxWidth: .infinity)
                        .frame(height: 54)
                        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                        .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }

                    Button("Сохранить") {
                        onSave(item)
                    }
                    .font(AppTheme.font(14, .extrabold))
                    .foregroundStyle(.white)
                    .frame(maxWidth: .infinity)
                    .frame(height: 54)
                    .background(
                        LinearGradient(colors: [AppTheme.purple, AppTheme.purpleLight], startPoint: .topLeading, endPoint: .bottomTrailing),
                        in: RoundedRectangle(cornerRadius: 14),
                    )
                    .shadow(color: AppTheme.purple.opacity(0.28), radius: 8, y: 4)
                }
            }
            .padding(.horizontal, 12)
            .padding(.top, 7)
            .padding(.bottom, 14)
        }
        .scrollDismissesKeyboard(.interactively)
        .background(AppTheme.background.ignoresSafeArea())
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private var namePlaceholder: String {
        switch item.kind {
        case .restaurant: return "Название места"
        case .accommodation: return "Название жилья"
        case .pet: return "Название места"
        case .sight: return "Название достопримечательности"
        case .expense: return "Например, билеты"
        }
    }

    @ViewBuilder
    private var fields: some View {
        switch item.kind {
        case .sight:
            editorSection("МЕСТО") {
                editorField("Категория", text: $item.category, placeholder: "Достопримечательность")
                editorField("Описание", text: $item.description, placeholder: "Короткое описание", multiline: true)
                editorField("Ссылка", text: $item.link, placeholder: "https://...", keyboard: .URL)
                HStack(spacing: 12) {
                    editorField("День маршрута", text: $item.walkDayText, keyboard: .numberPad)
                    editorField("Широта", text: $item.latitudeText, keyboard: .decimalPad)
                }
                editorToggle("Посещено", isOn: $item.done, icon: "checkmark.circle")
            }
        case .restaurant:
            editorSection("СТАТУС") {
                editorChoiceRow(
                    ["хочу", "бронь", "были"],
                    selected: item.status.isEmpty ? "хочу" : item.status,
                    labels: ["хочу", "бронь", "были"],
                ) { value in item.status = value }
                editorSectionLabel("СРЕДНИЙ ЧЕК")
                editorSegmentRow(["€", "€€", "€€€", "€€€€"], selected: item.price) { item.price = $0 }
                editorField("Кухня или заметка", text: $item.note, placeholder: "Например, итальянская кухня", multiline: true)
                editorField("Дата или время брони", text: $item.date, placeholder: "Не указано")
                editorField("Ссылка", text: $item.link, placeholder: "https://...", keyboard: .URL)
                editorToggle("В приоритете", isOn: $item.priority, icon: "flame.fill")
            }
        case .accommodation:
            editorSection("СТАТУС") {
                editorChoiceRow(
                    ["хочу", "бронь", "оплачено", "пожили"],
                    selected: item.status,
                    labels: ["хочу", "бронь", "оплачено", "пожили"],
                ) { value in item.status = value }
                editorField("Даты", text: $item.dates, placeholder: "дд.мм.гггг – дд.мм.гггг")
                HStack(spacing: 12) {
                    editorField("Цена", text: $item.price, placeholder: "€90")
                    editorField("Тип", text: $item.type, placeholder: "Отель")
                }
                editorField("Описание или адрес", text: $item.details, placeholder: "Адрес и детали брони", multiline: true)
                editorField("Бесплатная отмена до", text: $item.deadline, placeholder: "дд.мм.гггг")
                editorField("Ссылка на бронирование", text: $item.bookingURL, placeholder: "https://...", keyboard: .URL)
                editorField("Сайт", text: $item.website, placeholder: "https://...", keyboard: .URL)
                editorField("Телефон", text: $item.phone, keyboard: .phonePad)
            }
        case .expense:
            editorSection("РАСХОД") {
                HStack(spacing: 12) {
                    editorField("Сумма *", text: $item.amountText, placeholder: "0", keyboard: .decimalPad)
                    editorField("Валюта", text: $item.inputCurrency, placeholder: "EUR")
                }
                editorField("Категория", text: $item.category, placeholder: "Транспорт")
                editorField("Общий или личный", text: $item.scope, placeholder: "Общий")
                editorField("Кто оплатил", text: $item.paidBy, placeholder: "Участник")
                editorField("Дата", text: $item.date, placeholder: "дд.мм.гггг")
                editorField("Курс ввода", text: $item.inputCurrencyRateText, keyboard: .decimalPad)
            }
        case .pet:
            editorSection("МЕСТО ДЛЯ ПИТОМЦЕВ") {
                editorChoiceRow(
                    ["shop", "vet"],
                    selected: normalizedPetType,
                    labels: ["Зоомагазин", "Ветеринар"],
                ) { value in item.category = value }
                editorField("Адрес", text: $item.details, placeholder: "Адрес", multiline: true)
                editorField("Телефон", text: $item.phone, keyboard: .phonePad)
                editorField("Ссылка на Google Карты", text: $item.mapsURL, placeholder: "https://maps.google.com/...", keyboard: .URL)
                editorField("Сайт", text: $item.website, placeholder: "https://...", keyboard: .URL)
                editorField("Заметка", text: $item.noteForPet, placeholder: "Дополнительная информация", multiline: true)
                editorField("Особенности через запятую", text: $item.featuresText, placeholder: "Корм, груминг")
            }
        }
    }

    private var normalizedPetType: String {
        let value = item.category.lowercased()
        return value.contains("вет") || value == "vet" ? "vet" : "shop"
    }

    @ViewBuilder
    private func editorSection<Content: View>(_ title: String, @ViewBuilder content: () -> Content) -> some View {
        VStack(alignment: .leading, spacing: 10) {
            editorSectionLabel(title)
            content()
        }
    }

    private func editorSectionLabel(_ title: String) -> some View {
        Text(title)
            .font(AppTheme.font(11, .extrabold))
            .tracking(0.45)
            .foregroundStyle(AppTheme.muted)
            .padding(.top, 3)
    }

    @ViewBuilder
    private func editorField(
        _ label: String,
        text: Binding<String>,
        placeholder: String = "",
        keyboard: UIKeyboardType = .default,
        multiline: Bool = false,
    ) -> some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(label)
                .font(AppTheme.font(12, .extrabold))
                .foregroundStyle(AppTheme.ink)
            if multiline {
                TextField(placeholder, text: text, axis: .vertical)
                    .lineLimit(2...5)
                    .font(AppTheme.font(14, .semibold))
                    .keyboardType(keyboard)
                    .textInputAutocapitalization(keyboard == .URL ? .never : .sentences)
                    .autocorrectionDisabled(keyboard == .URL)
                    .padding(.horizontal, 13)
                    .padding(.vertical, 13)
                    .frame(maxWidth: .infinity, minHeight: 76, alignment: .topLeading)
                    .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                    .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }
            } else {
                TextField(placeholder, text: text)
                    .font(AppTheme.font(14, .semibold))
                    .keyboardType(keyboard)
                    .textInputAutocapitalization(keyboard == .URL ? .never : .sentences)
                    .autocorrectionDisabled(keyboard == .URL)
                    .padding(.horizontal, 13)
                    .frame(maxWidth: .infinity)
                    .frame(height: 50)
                    .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14))
                    .overlay { RoundedRectangle(cornerRadius: 14).stroke(AppTheme.border, lineWidth: 1) }
            }
        }
    }

    private func editorChoiceRow(_ values: [String], selected: String, labels: [String], action: @escaping (String) -> Void) -> some View {
        HStack(spacing: 9) {
            ForEach(Array(values.enumerated()), id: \.offset) { index, value in
                Button { action(value) } label: {
                    Text(labels[index])
                        .font(AppTheme.font(12, .extrabold))
                        .foregroundStyle(selected == value ? .white : AppTheme.muted)
                        .lineLimit(1)
                        .minimumScaleFactor(0.72)
                        .frame(maxWidth: .infinity)
                        .frame(height: 41)
                        .background(
                            selected == value ? AnyShapeStyle(AppTheme.purple) : AnyShapeStyle(AppTheme.surface),
                            in: RoundedRectangle(cornerRadius: 12, style: .continuous),
                        )
                        .overlay { RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(selected == value ? AppTheme.purple : AppTheme.border, lineWidth: 1) }
                }
                .buttonStyle(.plain)
            }
        }
    }

    private func editorSegmentRow(_ values: [String], selected: String, action: @escaping (String) -> Void) -> some View {
        HStack(spacing: 4) {
            ForEach(values, id: \.self) { value in
                Button { action(value) } label: {
                    Text(value)
                        .font(AppTheme.font(13, .bold))
                        .foregroundStyle(selected == value ? AppTheme.ink : AppTheme.muted)
                        .frame(maxWidth: .infinity)
                        .frame(height: 38)
                        .background(selected == value ? AppTheme.surface : .clear, in: RoundedRectangle(cornerRadius: 11, style: .continuous))
                        .shadow(color: selected == value ? Color.black.opacity(0.08) : .clear, radius: 2, y: 1)
                }
                .buttonStyle(.plain)
            }
        }
        .padding(5)
        .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
    }

    private func editorToggle(_ title: String, isOn: Binding<Bool>, icon: String) -> some View {
        Button { isOn.wrappedValue.toggle() } label: {
            HStack(spacing: 12) {
                Image(systemName: icon)
                    .font(.system(size: 17, weight: .semibold))
                    .foregroundStyle(isOn.wrappedValue ? AppTheme.purple : AppTheme.muted)
                    .frame(width: 38, height: 38)
                    .background(AppTheme.lavender.opacity(0.35), in: Circle())
                Text(title)
                    .font(AppTheme.font(14, .semibold))
                    .foregroundStyle(AppTheme.ink)
                Spacer()
                Image(systemName: isOn.wrappedValue ? "checkmark.circle.fill" : "circle")
                    .font(.system(size: 22, weight: .semibold))
                    .foregroundStyle(isOn.wrappedValue ? AppTheme.purple : AppTheme.border)
            }
            .padding(.horizontal, 14)
            .frame(height: 58)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: 16, style: .continuous).stroke(AppTheme.border, lineWidth: 1) }
        }
        .buttonStyle(.plain)
    }
}

private struct PhotoEditorRequest: Identifiable {
    let section: String
    let itemID: String
    let itemName: String
    let photos: [String]

    var id: String { "\(section)-\(itemID)" }

    init(item: EditableItem) {
        section = item.kind.section
        itemID = item.id
        itemName = item.name
        photos = item.photos
    }
}

private enum PhotoAction {
    case add(Data)
    case replace(Data)
    case move(Int, Int)
    case delete(Int)
}

private struct ItemPhotoManagerSheet: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    let request: PhotoEditorRequest
    let onAction: (PhotoAction) -> Void
    @State private var addSelection: PhotosPickerItem?
    @State private var replaceSelection: PhotosPickerItem?

    init(request: PhotoEditorRequest, onAction: @escaping (PhotoAction) -> Void) {
        self.request = request
        self.onAction = onAction
        _addSelection = State(initialValue: nil)
        _replaceSelection = State(initialValue: nil)
    }

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    if request.photos.isEmpty {
                        Text("Фотографий пока нет.").foregroundStyle(.secondary)
                    } else {
                        ForEach(Array(request.photos.enumerated()), id: \.offset) { index, reference in
                            HStack(spacing: 10) {
                                RemotePhotoView(reference: reference, client: model.client, contentMode: .fill, cornerRadius: 10)
                                    .frame(width: 72, height: 58)
                                VStack(alignment: .leading, spacing: 3) {
                                    Text(index == 0 ? "Обложка" : "Фото \(index + 1)")
                                        .font(.headline)
                                    Text(reference.hasPrefix("storage://") ? "Supabase Storage" : "Ссылка каталога")
                                        .font(.caption)
                                        .foregroundStyle(.secondary)
                                }
                                Spacer()
                                Menu {
                                    if index > 0 {
                                        Button("Переместить выше") { onAction(.move(index, -1)) }
                                    }
                                    if index + 1 < request.photos.count {
                                        Button("Переместить ниже") { onAction(.move(index, 1)) }
                                    }
                                    Button("Удалить", role: .destructive) { onAction(.delete(index)) }
                                } label: {
                                    Image(systemName: "ellipsis.circle")
                                        .font(.title3)
                                }
                            }
                        }
                    }
                } header: {
                    Text(request.itemName)
                }

                if request.section != "sights" {
                    Section {
                        PhotosPicker(selection: $addSelection, matching: .images) {
                            Label("Добавить фотографию", systemImage: "photo.badge.plus")
                        }
                        if !request.photos.isEmpty {
                            PhotosPicker(selection: $replaceSelection, matching: .images) {
                                Label("Заменить обложку", systemImage: "arrow.triangle.2.circlepath")
                            }
                        }
                    }
                } else {
                    Section {
                        PhotosPicker(selection: $replaceSelection, matching: .images) {
                            Label(request.photos.isEmpty ? "Добавить фотографию" : "Заменить фотографию", systemImage: "photo.badge.plus")
                        }
                    }
                }
            }
            .navigationTitle("Фотографии")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрыть") { dismiss() }
                }
            }
            .onChange(of: addSelection) { _, item in
                guard let item else { return }
                Task { @MainActor in
                    if let data = try? await item.loadTransferable(type: Data.self) {
                        onAction(.add(data))
                    }
                    addSelection = nil
                }
            }
            .onChange(of: replaceSelection) { _, item in
                guard let item else { return }
                Task { @MainActor in
                    if let data = try? await item.loadTransferable(type: Data.self) {
                        if request.section == "sights" && request.photos.isEmpty {
                            onAction(.add(data))
                        } else {
                            onAction(.replace(data))
                        }
                    }
                    replaceSelection = nil
                }
            }
        }
    }
}

private struct GroupEditorSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var draft: GroupDraft
    let onSave: (GroupDraft) -> Void

    init(initial: GroupDraft, onSave: @escaping (GroupDraft) -> Void) {
        _draft = State(initialValue: initial)
        self.onSave = onSave
    }

    var body: some View {
        NavigationStack {
            Form {
                TextField("Название группы", text: $draft.name)
                TextField("Количество участников", text: $draft.peopleText).keyboardType(.numberPad)
            }
            .navigationTitle(draft.isNew ? "Новая группа" : "Группа бюджета")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Отмена") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Сохранить") {
                        onSave(draft)
                        dismiss()
                    }
                }
            }
        }
    }
}

private struct MemberInviteSheet: View {
    @Environment(\.dismiss) private var dismiss
    @State private var name = ""
    @State private var email = ""
    @State private var role = "Читатель"
    let onSave: (String, String, String) -> Void

    var body: some View {
        NavigationStack {
            Form {
                TextField("Имя", text: $name)
                TextField("E-mail", text: $email)
                    .keyboardType(.emailAddress)
                    .textInputAutocapitalization(.never)
                Picker("Роль", selection: $role) {
                    Text("Читатель").tag("Читатель")
                    Text("Редактор").tag("Редактор")
                }
            }
            .navigationTitle("Пригласить участника")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) { Button("Отмена") { dismiss() } }
                ToolbarItem(placement: .confirmationAction) {
                    Button("Отправить") {
                        onSave(name, email, role)
                        dismiss()
                    }
                }
            }
        }
    }
}

struct CatalogPickerSheet: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    let kind: CatalogCategory
    let cities: [String]
    let defaultCity: String
    let onSelect: (CatalogEntry, Int) -> Void

    @State private var city: String
    @State private var query = ""
    @State private var petType = "shop"
    @State private var walkDay = 1
    @State private var entries: [CatalogEntry] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    init(
        kind: CatalogCategory,
        cities: [String],
        defaultCity: String,
        onSelect: @escaping (CatalogEntry, Int) -> Void,
    ) {
        self.kind = kind
        self.cities = cities
        self.defaultCity = defaultCity
        self.onSelect = onSelect
        _city = State(initialValue: defaultCity)
    }

    private var language: String { model.profile.language }

    private var searchKey: String {
        [kind.rawValue, city, query, petType, language].joined(separator: "|")
    }

    private var displayedEntries: [CatalogEntry] {
        guard kind == .accommodation else { return entries }
        return entries.sorted {
            let leftRating = $0.rating ?? -1
            let rightRating = $1.rating ?? -1
            if leftRating != rightRating { return leftRating > rightRating }
            let leftReviews = $0.reviewCount ?? 0
            let rightReviews = $1.reviewCount ?? 0
            if leftReviews != rightReviews { return leftReviews > rightReviews }
            return $0.name.localizedCaseInsensitiveCompare($1.name) == .orderedAscending
        }
    }

    var body: some View {
        NavigationStack {
            Form {
                Section("Поиск") {
                    if !cities.isEmpty {
                        Picker("Город поездки", selection: $city) {
                            ForEach(cities, id: \.self) { option in
                                Text(option).tag(option)
                            }
                        }
                    }
                    TextField("Город", text: $city)
                        .textInputAutocapitalization(.words)
                    TextField("Название или запрос", text: $query)
                        .textInputAutocapitalization(.never)
                    if kind == .pet {
                        Picker("Тип", selection: $petType) {
                            Text("Зоомагазины").tag("shop")
                            Text("Ветеринарные клиники").tag("vet")
                        }
                    }
                    if kind == .sight {
                        Stepper("День маршрута: \(walkDay)", value: $walkDay, in: 1...99)
                    }
                }

                if isLoading {
                    Section { ProgressView("Загружаем каталог…") }
                }

                if let errorMessage {
                    Section {
                        Text(errorMessage).foregroundStyle(.secondary)
                    }
                }

                Section(kind.title) {
                    if entries.isEmpty && !isLoading && errorMessage == nil {
                        Text("Ничего не найдено. Попробуйте изменить город или запрос.")
                            .foregroundStyle(.secondary)
                    }
                    ForEach(displayedEntries) { entry in
                        Button {
                            onSelect(entry, walkDay)
                            dismiss()
                        } label: {
                            CatalogEntryRow(entry: entry, client: model.client)
                        }
                        .buttonStyle(.plain)
                    }
                }
            }
            .navigationTitle(kind.title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .cancellationAction) {
                    Button("Закрыть") { dismiss() }
                }
            }
            .task(id: searchKey) {
                await loadCatalog()
            }
        }
    }

    private func loadCatalog() async {
        do {
            try await Task.sleep(nanoseconds: 220_000_000)
        } catch {
            return
        }
        let selectedCity = city.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !selectedCity.isEmpty else {
            entries = []
            errorMessage = "Укажите город поездки."
            isLoading = false
            return
        }

        isLoading = true
        errorMessage = nil
        do {
            entries = try await model.searchCatalog(
                kind: kind,
                city: selectedCity,
                query: query,
                language: language,
                petType: petType,
            )
        } catch is CancellationError {
            return
        } catch {
            entries = []
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }
}

private struct CatalogEntryRow: View {
    let entry: CatalogEntry
    let client: SupabaseClient

    var body: some View {
        HStack(alignment: .top, spacing: 12) {
            RemotePhotoView(reference: entry.photoReference, client: client, contentMode: .fill, cornerRadius: 12)
                .frame(width: 72, height: 72)
            VStack(alignment: .leading, spacing: 5) {
                Text(entry.name)
                    .font(.headline)
                    .foregroundStyle(.primary)
                let details = [entry.category, entry.type, entry.priceLabel]
                    .filter { !$0.isEmpty }
                    .joined(separator: " · ")
                if !details.isEmpty {
                    Text(details)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
                if let rating = entry.ratingLabel {
                    Label(rating, systemImage: "star.fill")
                        .font(.caption.weight(.semibold))
                        .foregroundStyle(.orange)
                }
                if !entry.address.isEmpty {
                    Text(entry.address)
                        .font(.caption)
                        .foregroundStyle(.secondary)
                        .lineLimit(2)
                }
            }
            Spacer(minLength: 0)
        }
        .padding(.vertical, 4)
    }
}

private extension String {
    var trimmed: String { trimmingCharacters(in: .whitespacesAndNewlines) }
}
