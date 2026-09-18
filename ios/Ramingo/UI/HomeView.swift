import Foundation
import SwiftUI

struct HomeView: View {
    @EnvironmentObject private var model: AppModel
    @State private var filter: TripFilter = .all
    @State private var showCreateTrip = false
    @State private var showSettings = false
    @State private var editingTrip: TripSummary?
    @State private var navigationPath = NavigationPath()
    @State private var showCreateTripHint = false
    @State private var didMarkCreateTripHint = false
    @State private var restoringTripID: String?
    @State private var tripActionMessage: String?

    private var activeTrips: [TripSummary] { model.trips.filter { $0.deletedAt == nil } }
    private var deletedTrips: [TripSummary] { model.trips.filter { $0.deletedAt != nil } }

    private var visibleTrips: [TripSummary] {
        switch filter {
        case .all:
            return activeTrips
        case .upcoming:
            return activeTrips.filter { !$0.isDraft && !$0.isCompleted }
        case .drafts:
            return activeTrips.filter(\.isDraft)
        case .completed:
            return activeTrips.filter(\.isCompleted)
        case .deleted:
            return deletedTrips
        }
    }

    var body: some View {
        NavigationStack(path: $navigationPath) {
            ZStack {
                AppTheme.background.ignoresSafeArea()
                ScrollView(showsIndicators: false) {
                    LazyVStack(alignment: .leading, spacing: 16) {
                        RamingoTopBar {
                            Button { showSettings = true } label: {
                                Image(systemName: "gearshape")
                                    .font(.system(size: 22, weight: .semibold))
                                    .foregroundStyle(AppTheme.ink)
                                    .frame(width: 48, height: 48)
                            }
                            .buttonStyle(.plain)
                            .accessibilityLabel("Открыть настройки")
                        }

                        Text("Мои путешествия")
                            .font(AppTheme.font(32, .extrabold))
                            .foregroundStyle(AppTheme.ink)
                            .lineSpacing(1)
                            .padding(.top, 6)

                        filterBar

                        if let loadError = model.tripsLoadErrorMessage {
                            HStack(alignment: .top, spacing: 12) {
                                Image(systemName: "wifi.exclamationmark")
                                    .foregroundStyle(AppTheme.error)
                                VStack(alignment: .leading, spacing: 4) {
                                    Text("Не удалось загрузить путешествия")
                                        .font(AppTheme.font(13, .bold))
                                        .foregroundStyle(AppTheme.ink)
                                    Text(loadError)
                                        .font(AppTheme.font(12, .regular))
                                        .foregroundStyle(AppTheme.muted)
                                        .lineLimit(3)
                                    Button("Повторить") {
                                        Task { try? await model.reloadTrips() }
                                    }
                                    .font(AppTheme.font(13, .bold))
                                    .foregroundStyle(AppTheme.purple)
                                }
                                Spacer(minLength: 0)
                            }
                            .padding(14)
                            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                            .overlay {
                                RoundedRectangle(cornerRadius: 16, style: .continuous)
                                    .stroke(AppTheme.error.opacity(0.25), lineWidth: 1)
                            }
                        }

                        if showCreateTripHint {
                            RamingoHintCard(
                                title: "Соберите первую поездку",
                                message: "Добавьте города и даты — остальные разделы можно заполнить постепенно.",
                                icon: "wand.and.stars",
                            )
                        }

                        if visibleTrips.isEmpty {
                            EmptyTripsView(filter: filter) { showCreateTrip = true }
                        } else {
                            ForEach(visibleTrips) { trip in
                                VStack(spacing: 8) {
                                    ZStack(alignment: .topTrailing) {
                                        if trip.deletedAt == nil {
                                            NavigationLink(value: trip.id) {
                                                TripCardView(trip: trip, client: model.client)
                                            }
                                            .buttonStyle(.plain)
                                        } else {
                                            TripCardView(trip: trip, client: model.client)
                                        }

                                        if trip.canEdit && trip.deletedAt == nil {
                                            Button {
                                                editingTrip = trip
                                            } label: {
                                                Image(systemName: "ellipsis")
                                                    .font(.system(size: 18, weight: .bold))
                                                    .foregroundStyle(Color(hex: 0x46464D))
                                                    .rotationEffect(.degrees(90))
                                                    .frame(width: 36, height: 36)
                                                    .background(Color.white.opacity(0.97), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                            }
                                            .buttonStyle(.plain)
                                            .padding(12)
                                            .accessibilityLabel("Редактировать \(trip.title)")
                                        }
                                    }

                                    if trip.deletedAt != nil, trip.isOwner {
                                        Button { restoreTrip(trip) } label: {
                                            HStack(spacing: 7) {
                                                if restoringTripID == trip.id {
                                                    ProgressView().tint(.white).scaleEffect(0.8)
                                                }
                                                Text(restoringTripID == trip.id ? "Возвращаем…" : "Восстановить")
                                                    .font(AppTheme.font(12, .extrabold))
                                            }
                                            .foregroundStyle(.white)
                                            .frame(maxWidth: .infinity)
                                            .frame(height: 42)
                                            .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                        }
                                        .buttonStyle(.plain)
                                        .disabled(restoringTripID != nil)
                                    }
                                }
                            }
                        }

                        if let tripActionMessage {
                            Text(tripActionMessage)
                                .font(AppTheme.font(12, .bold))
                                .foregroundStyle(AppTheme.error)
                        }

                        NewTripCard { showCreateTrip = true }
                    }
                    .padding(.horizontal, 18)
                    .padding(.bottom, 96)
                }
                .refreshable { try? await model.reloadTrips() }
            }
            .toolbar(.hidden, for: .navigationBar)
            .navigationDestination(for: String.self) { tripID in
                TripDetailView(tripID: tripID)
            }
            .task {
                try? await model.reloadTrips()
                openPendingTrip()
                openCreateTripIfRequested()
                updateCreateTripHint()
            }
            .onChange(of: model.pendingTripID) { _, _ in openPendingTrip() }
            .onChange(of: model.shouldPresentCreateTrip) { _, _ in openCreateTripIfRequested() }
            .onChange(of: model.trips) { _, _ in updateCreateTripHint() }
            .onAppear { openCreateTripIfRequested() }
            .sheet(isPresented: $showCreateTrip) { CreateTripView() }
            .sheet(isPresented: $showSettings) { SettingsView() }
            .sheet(item: $editingTrip) { trip in
                TripSettingsSheet(trip: trip)
                    .environmentObject(model)
            }
        }
    }

    private var filterBar: some View {
        ScrollView(.horizontal, showsIndicators: false) {
            HStack(spacing: 9) {
                filterButton(.all, count: activeTrips.count)
                filterButton(.upcoming, count: activeTrips.filter { !$0.isDraft && !$0.isCompleted }.count)
                filterButton(.drafts, count: activeTrips.filter(\.isDraft).count)
                filterButton(.completed, count: activeTrips.filter(\.isCompleted).count)
                if !deletedTrips.isEmpty {
                    filterButton(.deleted, count: deletedTrips.count)
                }
            }
            .padding(.vertical, 2)
        }
        .contentMargins(.horizontal, 0, for: .scrollContent)
    }

    private func filterButton(_ value: TripFilter, count: Int) -> some View {
        let selected = filter == value
        return Button {
            withAnimation(.easeInOut(duration: 0.18)) { filter = value }
        } label: {
            Text("\(value.title) · \(count)")
                .font(AppTheme.font(13, .bold))
                .foregroundStyle(selected ? Color.white : AppTheme.ink)
                .padding(.horizontal, 15)
                .frame(height: 38)
                .background(selected ? AppTheme.purple : AppTheme.surface, in: Capsule())
                .overlay {
                    if !selected { Capsule().stroke(AppTheme.border, lineWidth: 1) }
                }
                .shadow(color: selected ? AppTheme.purple.opacity(0.22) : .clear, radius: 5, y: 3)
        }
        .buttonStyle(.plain)
    }

    private func openPendingTrip() {
        guard let tripID = model.consumePendingTripID(), !tripID.isEmpty else { return }
        navigationPath.append(tripID)
    }

    private func openCreateTripIfRequested() {
        guard model.shouldPresentCreateTrip else { return }
        model.shouldPresentCreateTrip = false
        showCreateTrip = true
    }

    private func updateCreateTripHint() {
        guard !didMarkCreateTripHint,
              model.profile.onboardingCompleted,
              !model.profile.createTripHintSeen,
              filter == .all,
              activeTrips.isEmpty
        else { return }
        showCreateTripHint = true
        didMarkCreateTripHint = true
        Task { await model.markCreateTripHintSeen() }
    }

    private func restoreTrip(_ trip: TripSummary) {
        guard restoringTripID == nil else { return }
        restoringTripID = trip.id
        tripActionMessage = nil
        Task { @MainActor in
            defer { restoringTripID = nil }
            do {
                try await model.restoreTrip(id: trip.id)
                filter = .all
            } catch {
                tripActionMessage = error.localizedDescription
            }
        }
    }
}

private enum TripFilter: String, CaseIterable, Identifiable {
    case all, upcoming, drafts, completed, deleted
    var id: String { rawValue }
    var title: String {
        switch self {
        case .all: return "Все"
        case .upcoming: return "Предстоящие"
        case .drafts: return "Черновики"
        case .completed: return "Завершённые"
        case .deleted: return "Удалённые"
        }
    }
}

private extension TripSummary {
    var isDraft: Bool { status.localizedCaseInsensitiveContains("чернов") }
    var isCompleted: Bool {
        status.localizedCaseInsensitiveContains("заверш") ||
            status.localizedCaseInsensitiveContains("прошед")
    }
}

private struct TripCardView: View {
    let trip: TripSummary
    let client: SupabaseClient

    private var isDeleted: Bool { trip.deletedAt != nil }

    var body: some View {
        VStack(alignment: .leading, spacing: 0) {
            RemotePhotoView(reference: trip.coverReference, client: client, contentMode: .fill, cornerRadius: 0)
                .frame(height: 205)
                .overlay(alignment: .topLeading) {
                    StatusPill(text: isDeleted ? "Удалено" : trip.status)
                        .padding(12)
                }

            VStack(alignment: .leading, spacing: 0) {
                Text(trip.title)
                    .font(AppTheme.font(21, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)

                Text(trip.dates.isEmpty ? "Даты не указаны" : tripDateDisplayText(trip.dates))
                .font(AppTheme.font(13, .semibold))
                .foregroundStyle(AppTheme.muted)
                .padding(.top, 7)

                GeometryReader { proxy in
                    ZStack(alignment: .leading) {
                        Capsule().fill(AppTheme.track)
                        LinearGradient(
                            colors: [AppTheme.purple, Color(hex: 0x8069EE)],
                            startPoint: .leading,
                            endPoint: .trailing
                        )
                        .frame(width: proxy.size.width * CGFloat(max(trip.progress, 3)) / 100)
                        .clipShape(Capsule())
                    }
                }
                .frame(height: 6)
                .padding(.top, 13)

                routeSummary
                    .padding(.top, 9)
            }
            .padding(.horizontal, 16)
            .padding(.top, 15)
            .padding(.bottom, 17)
        }
        .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
        .clipShape(RoundedRectangle(cornerRadius: 22, style: .continuous))
        .shadow(color: Color.black.opacity(0.08), radius: 10, x: 0, y: 5)
    }

    private var routeSummary: some View {
        Group {
            if isDeleted {
                Text("Данные поездки сохранены")
                    .font(AppTheme.font(11.5, .extrabold))
                if !trip.cities.isEmpty {
                    Text(" · \(trip.cities)")
                        .font(AppTheme.font(11.5, .semibold))
                }
            } else if trip.cities.isEmpty {
                Text("Маршрут заполнен на \(trip.progress)%")
                    .font(AppTheme.font(11.5, .extrabold))
            } else {
                Text("Маршрут заполнен на \(trip.progress)%")
                    .font(AppTheme.font(11.5, .extrabold)) +
                Text(" · \(trip.cities)")
                    .font(AppTheme.font(11.5, .semibold))
            }
        }
        .foregroundStyle(AppTheme.muted)
        .fixedSize(horizontal: false, vertical: true)
    }
}

private struct EmptyTripsView: View {
    let filter: TripFilter
    let create: () -> Void

    var body: some View {
        RamingoCard {
            VStack(spacing: 12) {
                Image(systemName: filter == .deleted ? "archivebox" : "safari")
                    .font(.system(size: 31, weight: .medium))
                    .foregroundStyle(AppTheme.purple)
                Text(filter == .deleted ? "Архив пуст" : "Здесь появятся ваши путешествия")
                    .font(AppTheme.font(18, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .multilineTextAlignment(.center)
                Text(filter == .deleted ? "Удалённые поездки появятся здесь." : "Создайте первую поездку с нуля или выберите готовый маршрут")
                    .font(AppTheme.font(13, .semibold))
                    .foregroundStyle(AppTheme.muted)
                    .multilineTextAlignment(.center)
                if filter != .deleted {
                    PrimaryActionButton(title: "Создать путешествие", action: create)
                        .padding(.top, 4)
                }
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 9)
        }
    }
}

private struct NewTripCard: View {
    let action: () -> Void

    var body: some View {
        Button(action: action) {
            VStack(spacing: 0) {
                Text("+")
                    .font(AppTheme.font(28, .semibold))
                    .foregroundStyle(AppTheme.purple)
                    .padding(.horizontal, 15)
                    .padding(.vertical, 6)
                    .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 16, style: .continuous))
                Text("Новое путешествие")
                    .font(AppTheme.font(16, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .padding(.top, 10)
                Text("С нуля или из шаблона")
                    .font(AppTheme.font(13, .medium))
                    .foregroundStyle(AppTheme.muted)
                    .padding(.top, 2)
            }
            .frame(maxWidth: .infinity)
            .padding(.vertical, 34)
            .padding(.horizontal, 20)
            .background(AppTheme.surface.opacity(0.4), in: RoundedRectangle(cornerRadius: 22, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 22, style: .continuous)
                    .stroke(AppTheme.border, style: StrokeStyle(lineWidth: 2, dash: [7, 7]))
            }
        }
        .buttonStyle(.plain)
        .accessibilityIdentifier("home.createTrip")
    }
}

private enum TripSettingsStatus: String, CaseIterable, Identifiable, Equatable {
    case upcoming = "Предстоящее"
    case draft = "Черновик"
    case past = "Прошедшее"

    var id: String { rawValue }

    var title: String {
        switch self {
        case .upcoming: return "Предстоящие"
        case .draft: return "Черновики"
        case .past: return "Прошедшие"
        }
    }

    var summaryTitle: String {
        switch self {
        case .upcoming: return "Предстоящее путешествие"
        case .draft: return "Черновик путешествия"
        case .past: return "Завершённое путешествие"
        }
    }

    init(value: String) {
        if value.localizedCaseInsensitiveContains("чернов") || value.localizedCaseInsensitiveContains("draft") {
            self = .draft
        } else if value.localizedCaseInsensitiveContains("прошед") ||
                    value.localizedCaseInsensitiveContains("заверш") ||
                    value.localizedCaseInsensitiveContains("past") ||
                    value.localizedCaseInsensitiveContains("completed") {
            self = .past
        } else {
            self = .upcoming
        }
    }
}

struct TripSettingsSheet: View {
    @EnvironmentObject private var model: AppModel
    @Environment(\.dismiss) private var dismiss

    let trip: TripSummary

    @State private var title: String
    @State private var cities: String
    @State private var dates: String
    @State private var status: TripSettingsStatus
    @State private var showDatePicker = false
    @State private var showDeleteConfirmation = false
    @State private var isSaving = false
    @State private var isDeleting = false
    @State private var alertMessage: String?

    init(trip: TripSummary) {
        self.trip = trip
        _title = State(initialValue: trip.title)
        _cities = State(initialValue: trip.cities)
        _dates = State(initialValue: trip.dates)
        _status = State(initialValue: TripSettingsStatus(value: trip.status))
    }

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 8) {
                    header
                    Divider().overlay(AppTheme.border)
                    tripSummary

                    Text("ОСНОВНОЕ")
                        .font(AppTheme.font(10, .extrabold))
                        .foregroundStyle(AppTheme.muted)
                        .tracking(0.8)
                        .padding(.top, 1)

                    TripSettingsTextField(label: "Название путешествия", text: $title)
                    TripSettingsTextField(label: "Маршрут", text: $cities, icon: "mappin")

                    VStack(alignment: .leading, spacing: 5) {
                        Text("Даты поездки")
                            .font(AppTheme.font(11, .extrabold))
                            .foregroundStyle(AppTheme.label)

                        Button { showDatePicker = true } label: {
                            HStack(spacing: 10) {
                                Image(systemName: "calendar")
                                    .font(.system(size: 17, weight: .semibold))
                                    .foregroundStyle(AppTheme.purple)
                                Text(tripDateDisplayText(dates))
                                    .font(AppTheme.font(14, .bold))
                                    .foregroundStyle(AppTheme.ink)
                                    .lineLimit(2)
                                Spacer(minLength: 0)
                                Image(systemName: "chevron.right")
                                    .font(.system(size: 13, weight: .bold))
                                    .foregroundStyle(AppTheme.muted)
                            }
                            .padding(.horizontal, 13)
                            .frame(minHeight: 52)
                            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
                            .overlay {
                                RoundedRectangle(cornerRadius: 14, style: .continuous)
                                    .stroke(AppTheme.border, lineWidth: 1)
                            }
                        }
                        .buttonStyle(.plain)
                        .accessibilityLabel("Изменить даты поездки")
                    }

                    Text("Статус")
                        .font(AppTheme.font(11, .extrabold))
                        .foregroundStyle(AppTheme.label)
                        .padding(.top, 3)

                    statusPicker
                    actionButtons

                    if trip.isOwner {
                        deleteButton
                    }
                }
                .padding(15)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 22, style: .continuous))
                .padding(.horizontal, 8)
                .padding(.vertical, 18)
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
        .interactiveDismissDisabled(isSaving || isDeleting)
        .sheet(isPresented: $showDatePicker) {
            TripDateRangeSheet(initialValue: dates) { value in
                dates = value
            }
        }
        .alert("Не удалось сохранить изменения", isPresented: Binding(
            get: { alertMessage != nil && !isDeleting },
            set: { if !$0 { alertMessage = nil } },
        )) {
            Button("ОК", role: .cancel) { alertMessage = nil }
        } message: {
            Text(alertMessage ?? "")
        }
        .alert("Удалить путешествие?", isPresented: $showDeleteConfirmation) {
            Button("Удалить", role: .destructive) { deleteTrip() }
            Button("Отмена", role: .cancel) { }
        } message: {
            Text("Путешествие будет перемещено в удалённые. Его можно будет восстановить позже.")
        }
    }

    private var header: some View {
        HStack(alignment: .top, spacing: 10) {
            VStack(alignment: .leading, spacing: 3) {
                Text("НАСТРОЙКИ ПОЕЗДКИ")
                    .font(AppTheme.font(10, .extrabold))
                    .foregroundStyle(AppTheme.muted)
                    .tracking(0.8)
                Text(title.isEmpty ? trip.title : title)
                    .font(AppTheme.font(21, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(2)
            }
            Spacer(minLength: 0)
            Button { dismiss() } label: {
                Image(systemName: "xmark")
                    .font(.system(size: 15, weight: .semibold))
                    .foregroundStyle(AppTheme.muted)
                    .frame(width: 34, height: 34)
                    .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
            }
            .buttonStyle(.plain)
            .accessibilityLabel("Закрыть")
        }
    }

    private var tripSummary: some View {
        HStack(spacing: 11) {
            Text("R")
                .font(AppTheme.font(17, .extrabold))
                .foregroundStyle(.white)
                .frame(width: 36, height: 36)
                .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 11, style: .continuous))

            VStack(alignment: .leading, spacing: 3) {
                Text(status.summaryTitle)
                    .font(AppTheme.font(14, .extrabold))
                    .foregroundStyle(AppTheme.ink)
                    .lineLimit(1)
                Text(tripDateDisplayText(dates))
                    .font(AppTheme.font(11, .bold))
                    .foregroundStyle(AppTheme.muted)
                    .lineLimit(1)
            }
            Spacer(minLength: 0)
        }
        .padding(12)
        .background(AppTheme.lavender, in: RoundedRectangle(cornerRadius: 15, style: .continuous))
    }

    private var statusPicker: some View {
        HStack(spacing: 4) {
            ForEach(TripSettingsStatus.allCases) { option in
                Button { status = option } label: {
                    Text(option.title)
                        .font(AppTheme.font(11, .extrabold))
                        .foregroundStyle(status == option ? AppTheme.ink : AppTheme.muted)
                        .lineLimit(1)
                        .frame(maxWidth: .infinity)
                        .frame(height: 39)
                        .background(status == option ? AppTheme.surface : Color.clear, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                }
                .buttonStyle(.plain)
            }
        }
        .padding(4)
        .background(AppTheme.track, in: RoundedRectangle(cornerRadius: 13, style: .continuous))
    }

    private var actionButtons: some View {
        HStack(spacing: 9) {
            Button("Отмена") { dismiss() }
                .font(AppTheme.font(14, .extrabold))
                .foregroundStyle(AppTheme.ink)
                .frame(maxWidth: .infinity)
                .frame(height: 46)
                .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                .overlay { RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(AppTheme.border, lineWidth: 1) }
                .buttonStyle(.plain)

            Button { saveTrip() } label: {
                HStack(spacing: 7) {
                    if isSaving { ProgressView().tint(.white).scaleEffect(0.8) }
                    Text(isSaving ? "Сохраняем…" : "Сохранить")
                }
                .font(AppTheme.font(14, .extrabold))
                .foregroundStyle(.white)
                .frame(maxWidth: .infinity)
                .frame(height: 46)
                .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            }
            .buttonStyle(.plain)
            .disabled(isSaving || isDeleting)
        }
    }

    private var deleteButton: some View {
        Button { showDeleteConfirmation = true } label: {
            HStack(spacing: 10) {
                Image(systemName: "trash")
                    .font(.system(size: 15, weight: .semibold))
                VStack(alignment: .leading, spacing: 2) {
                    Text("Удалить путешествие")
                        .font(AppTheme.font(13, .extrabold))
                    Text("Путешествие можно восстановить позже")
                        .font(AppTheme.font(10.5, .semibold))
                }
                Spacer(minLength: 0)
            }
            .foregroundStyle(AppTheme.error)
            .padding(.horizontal, 13)
            .frame(minHeight: 58)
            .background(AppTheme.error.opacity(0.10), in: RoundedRectangle(cornerRadius: 13, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: 13, style: .continuous).stroke(AppTheme.error.opacity(0.25), lineWidth: 1) }
        }
        .buttonStyle(.plain)
        .disabled(isSaving || isDeleting)
    }

    private func saveTrip() {
        let cleanTitle = title.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !cleanTitle.isEmpty else {
            alertMessage = "Укажите название путешествия."
            return
        }

        isSaving = true
        Task { @MainActor in
            do {
                try await model.updateTripDetails(
                    id: trip.id,
                    title: cleanTitle,
                    dates: dates.trimmingCharacters(in: .whitespacesAndNewlines),
                    cities: cities.trimmingCharacters(in: .whitespacesAndNewlines),
                )
                try await model.updateTripField(id: trip.id, key: "status", value: .string(status.rawValue))
                try await model.reloadTrips()
                isSaving = false
                dismiss()
            } catch {
                isSaving = false
                alertMessage = error.localizedDescription
            }
        }
    }

    private func deleteTrip() {
        isDeleting = true
        Task { @MainActor in
            do {
                try await model.deleteTrip(id: trip.id)
                isDeleting = false
                dismiss()
            } catch {
                isDeleting = false
                alertMessage = error.localizedDescription
            }
        }
    }
}

private struct TripSettingsTextField: View {
    let label: String
    @Binding var text: String
    var icon: String?

    var body: some View {
        VStack(alignment: .leading, spacing: 5) {
            Text(label)
                .font(AppTheme.font(11, .extrabold))
                .foregroundStyle(AppTheme.label)

            HStack(spacing: 10) {
                if let icon {
                    Image(systemName: icon)
                        .font(.system(size: 17, weight: .semibold))
                        .foregroundStyle(AppTheme.purple)
                }
                TextField(label, text: $text)
                    .font(AppTheme.font(14, .bold))
                    .foregroundStyle(AppTheme.ink)
                    .textInputAutocapitalization(.sentences)
                    .autocorrectionDisabled()
            }
            .padding(.horizontal, 13)
            .frame(minHeight: 52)
            .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 14, style: .continuous))
            .overlay {
                RoundedRectangle(cornerRadius: 14, style: .continuous)
                    .stroke(AppTheme.border, lineWidth: 1)
            }
        }
    }
}

private struct TripDateRangeSheet: View {
    @Environment(\.dismiss) private var dismiss

    let initialValue: String
    let onDone: (String) -> Void
    private let calendar: Calendar

    @State private var startDate: Date
    @State private var endDate: Date
    @State private var month: Date
    @State private var selectingEnd = false

    init(initialValue: String, onDone: @escaping (String) -> Void) {
        self.initialValue = initialValue
        self.onDone = onDone

        var calendar = Calendar(identifier: .gregorian)
        calendar.locale = Locale(identifier: "ru_RU")
        calendar.timeZone = TimeZone(secondsFromGMT: 0)!
        self.calendar = calendar

        let fallbackStart = calendar.startOfDay(for: Date())
        let range = iosTripDateRange(initialValue) ?? (fallbackStart, calendar.date(byAdding: .day, value: 1, to: fallbackStart) ?? fallbackStart)
        _startDate = State(initialValue: range.0)
        _endDate = State(initialValue: range.1)
        _month = State(initialValue: calendar.date(from: calendar.dateComponents([.year, .month], from: range.0)) ?? range.0)
    }

    var body: some View {
        ZStack {
            AppTheme.background.ignoresSafeArea()

            ScrollView(showsIndicators: false) {
                VStack(alignment: .leading, spacing: 14) {
                    HStack(alignment: .top, spacing: 10) {
                        VStack(alignment: .leading, spacing: 3) {
                            Text("ДАТЫ ПОЕЗДКИ")
                                .font(AppTheme.font(10, .extrabold))
                                .foregroundStyle(AppTheme.muted)
                                .tracking(0.8)
                            Text("Выбрать даты")
                                .font(AppTheme.font(21, .extrabold))
                                .foregroundStyle(AppTheme.ink)
                        }
                        Spacer(minLength: 0)
                        Button { dismiss() } label: {
                            Image(systemName: "xmark")
                                .font(.system(size: 15, weight: .semibold))
                                .foregroundStyle(AppTheme.muted)
                                .frame(width: 34, height: 34)
                                .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 10, style: .continuous))
                        }
                        .buttonStyle(.plain)
                    }

                    HStack(spacing: 8) {
                        rangeChip(title: "Начало", date: startDate, selected: !selectingEnd) {
                            selectingEnd = false
                        }
                        rangeChip(title: "Окончание", date: endDate, selected: selectingEnd) {
                            selectingEnd = true
                        }
                    }

                    Text(selectingEnd ? "Теперь выберите дату окончания" : "Выберите дату начала поездки")
                        .font(AppTheme.font(12, .bold))
                        .foregroundStyle(AppTheme.muted)

                    HStack {
                        calendarButton(systemName: "chevron.left", label: "Предыдущий месяц") {
                            month = calendar.date(byAdding: .month, value: -1, to: month) ?? month
                        }
                        Spacer()
                        Text(monthTitle)
                            .font(AppTheme.font(15, .extrabold))
                            .foregroundStyle(AppTheme.ink)
                        Spacer()
                        calendarButton(systemName: "chevron.right", label: "Следующий месяц") {
                            month = calendar.date(byAdding: .month, value: 1, to: month) ?? month
                        }
                    }

                    LazyVGrid(columns: Array(repeating: GridItem(.flexible(), spacing: 0), count: 7), spacing: 4) {
                        ForEach(weekdays, id: \.self) { weekday in
                            Text(weekday)
                                .font(AppTheme.font(11, .extrabold))
                                .foregroundStyle(AppTheme.muted)
                                .frame(maxWidth: .infinity)
                                .frame(height: 24)
                        }

                        ForEach(Array(calendarCells.enumerated()), id: \.offset) { _, date in
                            if let date {
                                let enabled = !selectingEnd || dateOnly(date) >= dateOnly(startDate)
                                Button { select(date) } label: {
                                    Text(String(calendar.component(.day, from: date)))
                                        .font(AppTheme.font(14, isSelected(date) ? .extrabold : .bold))
                                        .foregroundStyle(isSelected(date) ? Color.white : enabled ? AppTheme.ink : AppTheme.muted.opacity(0.35))
                                        .frame(maxWidth: .infinity)
                                        .frame(height: 42)
                                        .background(cellBackground(for: date), in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                                }
                                .buttonStyle(.plain)
                                .disabled(!enabled)
                            } else {
                                Color.clear.frame(height: 42)
                            }
                        }
                    }

                    HStack(spacing: 9) {
                        Button("Отмена") { dismiss() }
                            .font(AppTheme.font(14, .extrabold))
                            .foregroundStyle(AppTheme.ink)
                            .frame(maxWidth: .infinity)
                            .frame(height: 46)
                            .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                            .buttonStyle(.plain)
                        Button("Готово") {
                            onDone(iosStoredTripDateRange(startDate, endDate))
                            dismiss()
                        }
                        .font(AppTheme.font(14, .extrabold))
                        .foregroundStyle(.white)
                        .frame(maxWidth: .infinity)
                        .frame(height: 46)
                        .background(AppTheme.purple, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
                        .buttonStyle(.plain)
                    }
                }
                .padding(18)
                .background(AppTheme.surface, in: RoundedRectangle(cornerRadius: 24, style: .continuous))
                .padding(.horizontal, 18)
                .padding(.vertical, 22)
            }
        }
        .presentationDetents([.large])
        .presentationDragIndicator(.visible)
    }

    private var weekdays: [String] { ["ПН", "ВТ", "СР", "ЧТ", "ПТ", "СБ", "ВС"] }

    private var monthTitle: String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "LLLL yyyy"
        return formatter.string(from: month).capitalized
    }

    private var calendarCells: [Date?] {
        let firstDay = calendar.date(from: calendar.dateComponents([.year, .month], from: month)) ?? month
        let weekday = calendar.component(.weekday, from: firstDay)
        let offset = (weekday + 5) % 7
        let count = calendar.range(of: .day, in: .month, for: firstDay)?.count ?? 30
        var cells = Array(repeating: Date?.none, count: offset)
        cells.append(contentsOf: (1...count).compactMap { calendar.date(byAdding: .day, value: $0 - 1, to: firstDay) })
        return cells
    }

    private func rangeChip(title: String, date: Date, selected: Bool, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            VStack(alignment: .leading, spacing: 2) {
                Text(title)
                    .font(AppTheme.font(10, .bold))
                    .foregroundStyle(AppTheme.muted)
                Text(shortDate(date))
                    .font(AppTheme.font(13, .extrabold))
                    .foregroundStyle(AppTheme.ink)
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.horizontal, 11)
            .padding(.vertical, 8)
            .background(selected ? AppTheme.lavender : AppTheme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
            .overlay { RoundedRectangle(cornerRadius: 12, style: .continuous).stroke(selected ? AppTheme.purple.opacity(0.45) : AppTheme.border, lineWidth: 1) }
        }
        .buttonStyle(.plain)
    }

    private func calendarButton(systemName: String, label: String, action: @escaping () -> Void) -> some View {
        Button(action: action) {
            Image(systemName: systemName)
                .font(.system(size: 15, weight: .bold))
                .foregroundStyle(AppTheme.ink)
                .frame(width: 40, height: 40)
                .background(AppTheme.surface2, in: RoundedRectangle(cornerRadius: 12, style: .continuous))
        }
        .buttonStyle(.plain)
        .accessibilityLabel(label)
    }

    private func select(_ date: Date) {
        if selectingEnd {
            guard dateOnly(date) >= dateOnly(startDate) else { return }
            endDate = date
            selectingEnd = false
        } else {
            startDate = date
            if dateOnly(endDate) < dateOnly(date) { endDate = date }
            selectingEnd = true
        }
    }

    private func isSelected(_ date: Date) -> Bool {
        dateOnly(date) == dateOnly(startDate) || dateOnly(date) == dateOnly(endDate)
    }

    private func cellBackground(for date: Date) -> Color {
        let day = dateOnly(date)
        if day == dateOnly(startDate) || day == dateOnly(endDate) { return AppTheme.purple }
        if day > dateOnly(startDate) && day < dateOnly(endDate) { return AppTheme.lavender }
        return .clear
    }

    private func dateOnly(_ date: Date) -> Date {
        calendar.startOfDay(for: date)
    }

    private func shortDate(_ date: Date) -> String {
        let formatter = DateFormatter()
        formatter.locale = Locale(identifier: "ru_RU")
        formatter.dateFormat = "d MMM yyyy"
        return formatter.string(from: date).replacingOccurrences(of: ".", with: "")
    }
}

private func iosTripDateRange(_ value: String) -> (Date, Date)? {
    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(secondsFromGMT: 0)!

    let isoFormatter = DateFormatter()
    isoFormatter.locale = Locale(identifier: "en_US_POSIX")
    isoFormatter.calendar = calendar
    isoFormatter.timeZone = calendar.timeZone
    isoFormatter.dateFormat = "yyyy-MM-dd"

    let isoValues = regexMatches(#"\d{4}-\d{2}-\d{2}"#, in: value).compactMap { isoFormatter.date(from: $0) }
    if let first = isoValues.first {
        return (first, isoValues.dropFirst().first ?? first)
    }

    let dottedFormatter = DateFormatter()
    dottedFormatter.locale = Locale(identifier: "en_US_POSIX")
    dottedFormatter.calendar = calendar
    dottedFormatter.timeZone = calendar.timeZone
    dottedFormatter.dateFormat = "dd.MM.yyyy"
    let dottedValues = regexMatches(#"\d{1,2}[./]\d{1,2}[./]\d{4}"#, in: value).compactMap {
        dottedFormatter.date(from: $0.replacingOccurrences(of: "/", with: "."))
    }
    if let first = dottedValues.first {
        return (first, dottedValues.dropFirst().first ?? first)
    }

    let humanPattern = #"(\d{1,2})\s+([A-Za-zА-Яа-яЁёÄÖÜäöüß]+)\s+(\d{4})"#
    let humanValues = regexCaptureMatches(humanPattern, in: value).compactMap { match -> Date? in
        guard match.count == 4,
              let day = Int(match[1]),
              let year = Int(match[3]),
              let month = iosMonthNumber(match[2]) else { return nil }
        return calendar.date(from: DateComponents(year: year, month: month, day: day))
    }
    guard let first = humanValues.first else { return nil }
    return (first, humanValues.dropFirst().first ?? first)
}

private func iosMonthNumber(_ value: String) -> Int? {
    let month = value.lowercased()
    switch month.prefix(3) {
    case "янв", "jan", "ene": return 1
    case "фев", "feb": return 2
    case "мар", "mär": return 3
    case "апр", "apr": return 4
    case "мая", "май", "may", "mai": return 5
    case "июн", "jun": return 6
    case "июл", "jul": return 7
    case "авг", "aug", "ago": return 8
    case "сен", "сент", "sep": return 9
    case "окт", "oct", "okt": return 10
    case "ноя", "nov": return 11
    case "дек", "dec", "dez": return 12
    default: return nil
    }
}

private func iosStoredTripDateRange(_ start: Date, _ end: Date) -> String {
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "en_US_POSIX")
    formatter.timeZone = TimeZone(secondsFromGMT: 0)
    formatter.dateFormat = "yyyy-MM-dd"
    return "\(formatter.string(from: start)) — \(formatter.string(from: end))"
}

private func tripDateDisplayText(_ value: String) -> String {
    guard let (start, end) = iosTripDateRange(value) else {
        return value.isEmpty ? "Даты не указаны" : value
    }

    var calendar = Calendar(identifier: .gregorian)
    calendar.timeZone = TimeZone(secondsFromGMT: 0)!
    let formatter = DateFormatter()
    formatter.locale = Locale(identifier: "ru_RU")
    formatter.calendar = calendar
    formatter.timeZone = calendar.timeZone
    formatter.dateFormat = "d MMM yyyy"
    let startText = formatter.string(from: start).replacingOccurrences(of: ".", with: "")
    let endText = formatter.string(from: end).replacingOccurrences(of: ".", with: "")
    let days = max(calendar.dateComponents([.day], from: start, to: end).day ?? 0, 0) + 1
    return "\(startText) – \(endText) · \(days) \(iosDayWord(days))"
}

private func iosDayWord(_ count: Int) -> String {
    let mod10 = count % 10
    let mod100 = count % 100
    if mod10 == 1 && mod100 != 11 { return "день" }
    if (2...4).contains(mod10) && !(12...14).contains(mod100) { return "дня" }
    return "дней"
}

private func regexMatches(_ pattern: String, in value: String) -> [String] {
    guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
    let range = NSRange(value.startIndex..<value.endIndex, in: value)
    return regex.matches(in: value, range: range).compactMap { match in
        guard let matchRange = Range(match.range, in: value) else { return nil }
        return String(value[matchRange])
    }
}

private func regexCaptureMatches(_ pattern: String, in value: String) -> [[String]] {
    guard let regex = try? NSRegularExpression(pattern: pattern) else { return [] }
    let range = NSRange(value.startIndex..<value.endIndex, in: value)
    return regex.matches(in: value, range: range).map { match in
        (0..<match.numberOfRanges).compactMap { index in
            guard let matchRange = Range(match.range(at: index), in: value) else { return nil }
            return String(value[matchRange])
        }
    }
}
