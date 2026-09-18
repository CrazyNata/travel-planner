# Аудит переноса Ramingo с Android на iOS

Дата аудита: 2026-09-17  
Объект аудита: C:\Users\natas\Documents\travel-planner  
Правило аудита: Android является единственным source of truth.  
Граница работ в этом шаге: создан только этот отчёт. Исходный код Android, iOS и web не изменялся.

## 1. Итог аудита

Текущая iOS-версия является частичной визуально-функциональной реконструкцией, а не переносом Android-функциональности по вертикальным срезам. Главная причина неправильного результата — перенос начался с крупного SwiftUI-экрана и визуальных оболочек до того, как были зафиксированы Android-навигация, состояния, контракты данных, модальные сценарии и правила сохранения.

В результате:

1. Android-монолит OdysseyApp.kt был приблизительно сопоставлен с несколькими крупными SwiftUI-файлами, но Android-переходы и вложенные сценарии не были перенесены один к одному.
2. В iOS появился общий TripEditorView, который заменил разные Android-сценарии редактирования собственными универсальными формами. Это меняет UX и поведение, особенно для маршрута, достопримечательностей, ресторанов, проживания и бюджета.
3. Некоторые действия доступны только через общий редактор, хотя на Android они доступны прямо на соответствующем экране. Конкретный подтверждённый пример: на iOS в экране маршрута нет Android-кнопки добавления маршрута/дня, а карандаш не открывает Android-модалку редактирования конкретного переезда.
4. iOS использует упрощённые модели и упрощённые экраны. Часть полей Android отсутствует в iOS-моделях, поэтому невозможно получить идентичные состояния и операции.
5. Android-логика была частично заменена другими реализациями: Mapbox/Google/Valhalla/OSM были смешаны с MapKit; Android-каталог городов с мировым asset заменён коротким hardcoded-списком; Android-система запоминания аккаунтов заменена более простой сессией Keychain.
6. Реальная загрузка из Supabase присутствует во многих местах iOS, но отдельные ошибки подавляются через try?, поэтому Android-состояния loading/error/empty/success не всегда различимы.
7. В текущем окружении нет xcodebuild, swiftc и swift. Реальная сборка iOS не выполнялась; существующие проверки не подтверждают компилируемость и поведение на iPhone.

Вывод: текущий перенос нельзя безопасно продолжать как «доделывание всех экранов». Сначала нужно зафиксировать Android-карту экранов и выбрать один контролируемый вертикальный срез. Первым таким срезом логично взять маршрут, потому что пользователь уже подтвердил конкретное расхождение в его поведении.

## 2. Где находится каждый клиент

### Android

Основная Android-реализация находится в:

- mobile/app/src/main/java/com/odyssey/travelplanner/MainActivity.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/ui/OdysseyApp.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/
- mobile/app/src/main/java/com/odyssey/travelplanner/notifications/ReminderScheduler.kt
- mobile/app/src/main/res/
- mobile/app/src/main/assets/city-catalog.tsv.bin
- тесты: mobile/app/src/test/

Сборочная конфигурация:

- mobile/app/build.gradle.kts
- mobile/app/src/main/AndroidManifest.xml

### iOS

Текущая iOS-реализация находится в:

- ios/Ramingo/RamingoApp.swift
- ios/Ramingo/State/AppModel.swift
- ios/Ramingo/Networking/
- ios/Ramingo/UI/
- ios/Ramingo/Notifications/ReminderScheduler.swift
- ios/Ramingo/Ramingo.xcodeproj/project.pbxproj
- ios/Ramingo/Info.plist
- ios/Ramingo/Ramingo.entitlements

### Web и дизайн-референсы

В репозитории также есть src/ и design-reference/. Они не являются Android source of truth и в этом аудите считаются вне области переноса. Их не следует использовать для исправления нативного iOS-поведения вместо Android-кода.

## 3. Архитектура Android: что реально является источником поведения

### 3.1 Навигация и UI

В Android нет отдельных Fragment, Android XML Navigation Graph, ViewModel или выделенных UseCase-классов для основных экранов. Поиск по проекту показывает:

- единственная Activity — MainActivity;
- весь основной Compose UI и Compose-навигация находятся в OdysseyApp.kt;
- переходы описаны вызовами composable(...) в OdysseyApp.kt;
- состояние экранов в основном хранится локально в Compose-функциях и передаётся через repository-операции;
- доменная и backend-логика находится в data/ и вызывается непосредственно из Compose-сценариев.

Это важно: переносить Android нельзя по принципу «один Kotlin-файл = один Swift-файл». В Android один файл содержит навигационную оболочку, пользовательские экраны, sheets, карточки, каталоги и часть состояния. Для iOS необходимо сначала разложить фактические Android-сценарии, не меняя их поведение.

### 3.2 Основные entry points Android

MainActivity.kt делает следующее:

- запускает Compose-приложение OdysseyApp;
- принимает Supabase auth deep link;
- принимает приглашение https://ramingo.online/mobile/invite?tripId=...;
- принимает reset-password deep link https://ramingo.online/mobile/reset;
- поддерживает legacy-host travelplanner.muntim.ru;
- принимает идентификатор поездки из notification intent;
- устанавливает Mapbox token перед запуском UI.

AndroidManifest.xml дополнительно объявляет:

- интернет;
- coarse/fine location;
- POST_NOTIFICATIONS;
- RECEIVE_BOOT_COMPLETED;
- SCHEDULE_EXACT_ALARM;
- verified app links для обоих host;
- receiver-ы для reminder alarm, boot, timezone, time change, package replacement и изменения exact-alarm permission.

### 3.3 Android navigation map

Фактические Compose destinations:

| Destination | Экран или сценарий |
|---|---|
| foundation | восстановление сессии и авторизация |
| onboarding/{mode} | первое обучение, повторное обучение, обучение перед созданием поездки |
| trips | список поездок |
| settings | настройки аккаунта |
| create-trip | создание поездки |
| reset-password | установка нового пароля |
| trip/{tripId} | подробности поездки с внутренними вкладками и drawer |

Внутри trip/{tripId} порядок Android-разделов такой:

1. Overview
2. Route
3. Sights
4. Restaurants
5. Accommodation
6. Pets
7. Budget
8. Members
9. Photos

В iOS этот порядок и названия должны быть проверены по Android, а не по текущей SwiftUI-композиции.

## 4. Полный перечень пользовательских Android-экранов и сценариев

Ниже перечислены не только файлы, но и фактические точки входа пользователя, вложенные sheets и сценарии, которые влияют на parity.

### ANDROID SCREEN:

Восстановление приложения / Splash

### ENTRY POINT:

Запускается из MainActivity до отображения основного состояния. В OdysseyApp.kt связан с RamingoSplash и восстановлением Supabase-сессии.

### UI AND BEHAVIOR:

Показывает стартовое состояние, пока определяется сохранённая сессия, onboarding state и pending deep link.

### DATA AND LOGIC:

Использует Supabase session manager, remembered account state и состояние pending invite/reset/trip notification.

### STATES:

Loading, authenticated, unauthenticated, pending deep link, ошибка восстановления сессии.

### IOS STATUS:

Частично реализовано в RamingoApp.swift и AppModel.swift. Точное совпадение всех pending-link и session-веток не доказано; сборка iOS не проверена.

---

### ANDROID SCREEN:

Авторизация и выбор аккаунта

### ENTRY POINT:

Destination foundation; появляется после запуска без действующей сессии, после logout или при необходимости повторной авторизации.

### UI AND BEHAVIOR:

AuthScreen поддерживает регистрацию, вход по email/password, Google, remembered accounts и переход к восстановлению пароля. RememberedAccountsPanel отображает сохранённые аккаунты.

### DATA AND LOGIC:

SupabaseProvider использует PKCE и redirect https://ramingo.online/mobile/auth. Android имеет session managers для persistent, memory и remembered account сценариев. Сохранённые credentials шифруются Android Keystore AES-GCM.

### ACTIONS:

Вход, регистрация, logout, выбор запомненного аккаунта, удаление запомненного аккаунта, Google OAuth, переход к reset password.

### STATES:

Loading, invalid credentials, network/auth error, email confirmation, remembered account picker, password visibility, remember credentials.

### IOS STATUS:

Частично реализовано в AuthView.swift и SupabaseClient.swift. Есть email, Google, Apple и reset-password request, но нет подтверждённой Android-эквивалентной системы remembered accounts с encrypted credential picker. Состояние «Запомнить данные входа» в iOS меняет persistence сессии, но не повторяет Android-семантику сохранённых аккаунтов.

---

### ANDROID SCREEN:

Восстановление и установка пароля

### ENTRY POINT:

reset-password и auth deep link из MainActivity.

### UI AND BEHAVIOR:

ResetPasswordScreen принимает auth link, позволяет задать новый пароль и завершает сценарий возвратом в приложение.

### DATA AND LOGIC:

Supabase Auth PKCE/deep link flow; legacy host также обрабатывается.

### STATES:

Ожидание deep link, invalid/expired link, ввод пароля, password mismatch, success, network error.

### IOS STATUS:

Запрос reset-письма есть в AuthView; обработчик URL есть в RamingoApp.swift/SupabaseClient.swift. Полный набор Android states и поведение после истечения ссылки не зафиксированы тестом.

---

### ANDROID SCREEN:

Onboarding tutorial

### ENTRY POINT:

onboarding/{mode} с mode first, replay или create. Вход возможен после регистрации, из настроек для повторного просмотра и перед созданием первой поездки.

### UI AND BEHAVIOR:

OnboardingTutorialScreen содержит последовательность страниц и отдельные варианты preview. Первый запуск сохраняет onboarding state в user_data; завершение направляет пользователя в список поездок или создание поездки в зависимости от mode.

### DATA AND LOGIC:

AccountRepository хранит onboarding state и web-onboarding marker. Действие пользователя влияет на destination после завершения.

### STATES:

Page index, first-run, replay, create-trip mode, skip/finish, persisted completion.

### IOS STATUS:

В RamingoApp.swift есть семистраничный onboarding. Сходство текста/визуала не доказывает совпадение Android routing и persisted state. Перенос частичный, отдельный Android state-by-state тест отсутствует.

---

### ANDROID SCREEN:

Список поездок

### ENTRY POINT:

Destination trips после успешной авторизации, завершения onboarding, создания поездки или возврата из деталей.

### UI AND BEHAVIOR:

MyTripsScreen показывает карточки поездок, фильтры и действия над активными, draft, completed и deleted trips. Отсюда доступны создание поездки, настройки, tutorial, открытие поездки и restore deleted.

### DATA AND LOGIC:

TripRepository.loadTrips, delete/restore/leave и AccountRepository. Карточка содержит cover photo, даты, города, статус, collaborators и секции, зависящие от trip payload.

### STATES:

Loading, empty, populated, deleted filter, restore in progress, repository error.

### IOS STATUS:

HomeView.swift содержит фильтры, карточки, restore, создание и переходы. Функционально это частичный перенос; точные Android card states, ошибки и lifecycle restore не сравнены экран к экрану.

---

### ANDROID SCREEN:

Настройки аккаунта

### ENTRY POINT:

Destination settings или account sheet из списка поездок.

### UI AND BEHAVIOR:

AccountSettingsScreen, AccountSettingsSheet, ThemePreferenceSelector, FeedbackBottomSheet. Содержит имя, фото профиля, язык, тему, email-настройки, уведомления, смену пароля, удаление аккаунта, onboarding replay и feedback.

### DATA AND LOGIC:

AccountRepository читает и обновляет user_data с ключом account_profile. Профильная фотография загружается в Storage bucket trip-photos. Удаление аккаунта выполняется Edge Function delete-account.

### STATES:

Loading profile, saving profile, upload progress/error, theme selection, notification settings, password change, delete-account confirmation/error/success.

### IOS STATUS:

SettingsView.swift покрывает основные пункты, но parity с Android account sheet, profile photo lifecycle и отдельными error states не подтверждён.

---

### ANDROID SCREEN:

Настройки уведомлений

### ENTRY POINT:

Account settings → notifications.

### UI AND BEHAVIOR:

NotificationSettingsScreen и профильные переключатели управляют reminder-политикой поездок, отмены, оплаты и email-поведения.

### DATA AND LOGIC:

ReminderScheduler.kt, AlarmManager, exact alarms, notification channel и broadcast receiver-ы. Уведомления восстанавливаются после boot/timezone/time change/package replacement и при изменении разрешения exact alarms.

### STATES:

Permission denied, exact-alarm unavailable, enabled/disabled per reminder type, scheduled/cancelled, reschedule after system event.

### IOS STATUS:

ios/Ramingo/Notifications/ReminderScheduler.swift планирует UNUserNotificationCenter notifications при действиях приложения. Это не эквивалент Android AlarmManager/receiver lifecycle: Android reschedule-after-boot/timezone/exact-alarm flow на iOS не найден и не доказан.

---

### ANDROID SCREEN:

Создание поездки

### ENTRY POINT:

trips → New Trip. Для первой поездки перед созданием может быть onboarding/create; для следующих — create-trip.

### UI AND BEHAVIOR:

CreateTripScreen принимает название, даты, города и другие данные поездки. Вложенные сценарии включают календарь TripDateCalendarDialog, выбор/поиск города, загрузку и назначение cover photos, gallery/photo assignment и создание payload.

### DATA AND LOGIC:

TripRepository.createTrip, CityCatalogRepository, Storage и Android city asset city-catalog.tsv.bin. Создаются overview blocks/map points, city coordinates, budget defaults, cover photos, days/route, sights, restaurants, pet places, accommodation, expenses и members.

### STATES:

Loading catalog, search, empty catalog, invalid date range, missing city/title, photo loading/upload, create in progress, create error, success route to trip/{tripId}.

### IOS STATUS:

CreateTripView.swift реализует текстовые поля, даты, города и backend create. Полный Android flow с фото/cover assignment, мировым catalog asset и всеми validation/error states не подтверждён.

---

### ANDROID SCREEN:

Каркас деталей поездки и drawer

### ENTRY POINT:

trips → tap trip → trip/{tripId}.

### UI AND BEHAVIOR:

TripOverviewScreen загружает overview, показывает top bar, title, edit pencil, drawer и внутренний раздел. Drawer содержит Overview, Route, Sights, Restaurants, Accommodation, Pets, Budget, Members, Photos.

### DATA AND LOGIC:

TripRepository.loadTripOverview, profile/permissions, weather load, reminder sync. Доступность edit/add/delete зависит от collaborator role и trip state.

### STATES:

Overview loading, weather loading, weather fallback, repository error, drawer open/closed, selected tab, editor permissions, reminder sync.

### IOS STATUS:

TripDetailView.swift содержит SwiftUI-каркас и большое число Android...Screen wrappers. Точный Android drawer/tab state и loading/error/permission parity не подтверждены; generic editor используется вместо части Android nested flows.

---

### ANDROID SCREEN:

Overview поездки

### ENTRY POINT:

Trip drawer → Overview; это default tab.

### UI AND BEHAVIOR:

OverviewContent показывает overview blocks, обложку/фотографии, weather cards, map points и редактируемые блоки. Есть reorder, add/remove/edit блоков, выбор weather city и map point, переход к подробным разделам.

### DATA AND LOGIC:

TripOverview содержит overview blocks, map points, city coordinates, weather city, cover photos и trip metadata. Сохранение идёт через patch_trip_payload с optimistic revision.

### STATES:

Loading, empty overview, edit mode, drag reorder, add block, image loading/upload, weather loading/error, map loading/error, optimistic conflict/error.

### IOS STATUS:

В TripDetailView.swift есть AndroidOverviewScreen, MapKit map, weather cards, cover photo, order/edit blocks и photo upload. Это наиболее развитая часть текущей iOS-версии, но она всё ещё не прошла сравнительный Android/iOS capture и не доказана по конфликтам revision, permission states и точным Android interaction states.

---

### ANDROID SCREEN:

Маршрут поездки

### ENTRY POINT:

Trip drawer → Route.

### UI AND BEHAVIOR:

TripRouteContent показывает список переездов/дней, расстояние и время в пути, check-in/check-out и карту/ссылку маршрута. При пустом маршруте пользователь с правом редактирования видит dashed action «＋ Добавить день». На карточке есть reorder и редактирование конкретного переезда. Редактор открывается как Android ModalBottomSheet.

### DATA AND LOGIC:

Состояние Android включает adding, editingLeg, from, to, selectedDateIso, checkIn, checkOut, notes, mapsUrl, saving, messages и order drag. Repository-операции: addRouteLeg, обновление деталей, удаление и reorderRouteLegs. RouteLeg имеет dayId, from, to, date fields, distance, travelTime, checkIn, checkOut, notes, mapsUrl и dayNumber.

### ANDROID EDIT MODAL:

По пользовательскому Android screenshot ожидаемый отдельный сценарий редактирования времени заселения выглядит так: затемнённый фон, модалка «Изменить переезд», пояснение «Обновите время заселения для этого переезда.», поле времени заселения, кнопки «Отмена» и «Сохранить». Это отличается от общего редактора всего маршрута.

### STATES:

Empty route, add day, loading, saving, invalid route data, route distance loading/error, per-leg edit, delete confirmation, reorder save/error, permission/read-only mode.

### IOS STATUS:

Подтверждённо сломан относительно Android:

- AndroidRouteScreen в TripDetailView.swift при пустом маршруте показывает только AndroidEmptyCard с текстом «Добавьте города и переезды» и не содержит Android action «Добавить день»/«Добавить маршрут».
- В route card callback onEdit передаётся без конкретного RouteLeg, поэтому карандаш ведёт в общий openFullEditor(section: "route"), а не в редактор выбранного переезда.
- TripEditorView.swift содержит кнопку «Добавить переезд» и общий RouteEditorSheet, но это доступно в другом navigation context и не повторяет Android screen flow.
- iOS-редактор маршрута показывает более широкий набор полей, тогда как пользовательский screenshot подтверждает отдельную модалку только для времени заселения.
- В iOS route distance использует отдельную MapKit/Valhalla/OSM-ветку; точное Android Mapbox/Google parsing и fallback chain не перенесены один к одному.

Это первая подтверждённая вертикальная проблема, которую нужно исправлять отдельным контролируемым шагом после утверждения baseline, а не расширением всего редактора.

---

### ANDROID SCREEN:

Достопримечательности

### ENTRY POINT:

Trip drawer → Sights.

### UI AND BEHAVIOR:

SightsContent организует достопримечательности по дням, показывает список/карту, карточки с фото/rating/location/notes и действия add/edit/delete/reorder. Вложенные сценарии: CreateDaySheet, EditDaySheet, SightCatalogSheet, AddSightSheet, SightLocationPickerSheet, notes card, card editor и full-screen photo viewer.

### DATA AND LOGIC:

Sight, SightDay, catalog tables/functions, city coordinates, Storage photos. Android Sight содержит дополнительные поля photoName, ratingCount, photoUnavailable, которых текущая iOS-модель не содержит в полном виде.

### STATES:

No days, create/edit day, catalog loading/error/empty, location picker, add/edit, photo unavailable, notes edit, reorder, delete, read-only collaborator.

### IOS STATUS:

TripDetailView.swift имеет sights screen и часть generic editor/catalog/photo flows. Это частичная реализация; отдельные Android sheets и states объединены/перенесены в общий редактор, а модель iOS неполна относительно Android.

---

### ANDROID SCREEN:

Рестораны

### ENTRY POINT:

Trip drawer → Restaurants.

### UI AND BEHAVIOR:

RestaurantsContent показывает ресторанные карточки по городам, filters/map, add from catalog, add manually, details, edit, photos и delete. Вложенные Android sheets: RestaurantAddCitySheet, RestaurantCatalogSheet, RestaurantAddSheet, RestaurantEditSheet, photo manager, city/filter sheets, card/details/map.

### DATA AND LOGIC:

Restaurant, RestaurantInput, restaurant catalog, restaurant-enrichment Edge Function, Storage photo operations, location/map integration.

### STATES:

City filter, catalog loading/error/empty, enrichment/loading, map, photo upload/delete, add/edit/delete, empty list, read-only.

### IOS STATUS:

Есть ресторанный экран, filters/map/list и callback в generic editor. Полный набор Android catalog/details/enrichment/photo flows и точное место каждого action не подтверждены.

---

### ANDROID SCREEN:

Проживание

### ENTRY POINT:

Trip drawer → Accommodation.

### UI AND BEHAVIOR:

AccommodationContent показывает карточки проживания, даты, deadline/status, ссылки, фото и детали. Вложенные сценарии: add choice, catalog, place details, add/edit, photo manager, date/calendar.

### DATA AND LOGIC:

Accommodation, AccommodationInput, catalog repository, Storage, status/deadline/date fields.

### STATES:

Empty, catalog loading/error, add/edit, date validation, status/deadline, photo loading/upload/delete, details, delete.

### IOS STATUS:

Есть базовые карточки и status/link display, а более сложные операции вынесены в TripEditorView. Это частичный перенос с другой навигацией и без доказанной parity по Android sheets/catalog/calendar/photo states.

---

### ANDROID SCREEN:

Места для питомцев

### ENTRY POINT:

Trip drawer → Pets.

### UI AND BEHAVIOR:

PetsContent поддерживает фильтры, catalog/place sheet, place cards, location/map и добавление/редактирование места.

### DATA AND LOGIC:

PetPlace, PetPlaceInput, PetCatalogRepository, DeviceLocation, расстояние до места и location permission.

### STATES:

Location permission, current location unavailable, filter, catalog loading/error/empty, place details, add/edit/delete, map.

### IOS STATUS:

Есть pets screen и MapKit/location-часть, но Android permission, distance/filter, catalog и action states не подтверждены как идентичные.

---

### ANDROID SCREEN:

Бюджет

### ENTRY POINT:

Trip drawer → Budget.

### UI AND BEHAVIOR:

BudgetContent показывает summary, currency, exchange/manual rates, metrics, categories и expenses. Вложенный add expense sheet поддерживает расходы, группы/participants, категории и редактирование.

### DATA AND LOGIC:

BudgetExpense, BudgetGroup, currency, manual rates, ExchangeRateRepository с Frankfurter, optimistic payload patch.

### STATES:

Loading rates, rate error/fallback/manual rate, empty budget, add/edit/delete expense, category/group selection, currency change, split metrics, save conflict/error.

### IOS STATUS:

Summary/currency/rates/categories присутствуют в AndroidBudgetScreen, а более полный expense/group CRUD находится в generic editor. Полный Android flow и все states на одном screen не перенесены.

---

### ANDROID SCREEN:

Участники поездки

### ENTRY POINT:

Trip drawer → Members.

### UI AND BEHAVIOR:

MembersContent показывает участников, роли, invite field и member actions. Вложенные InviteMemberField и MemberCard; поддерживаются приглашение, изменение роли, remove и leave trip.

### DATA AND LOGIC:

TripMember, trip_collaborators, send-invite Edge Function, manage_trip_member RPC, collaborator permission checks.

### STATES:

Invite sending, invalid email, pending invite, role update, remove confirmation/error, leave-trip confirmation/error, read-only role.

### IOS STATUS:

Список/invite/leave присутствуют в TripDetailView/TripEditorView, но точные Android permission checks, pending state, RPC error display и card actions не подтверждены.

---

### ANDROID SCREEN:

Фотографии поездки

### ENTRY POINT:

Trip drawer → Photos; также photo actions внутри overview, sights, restaurants и accommodation.

### UI AND BEHAVIOR:

PhotosContent показывает grid/gallery, upload/delete и full-screen photo viewer. Контекстные фото назначаются на cover, sight, restaurant или accommodation.

### DATA AND LOGIC:

Storage bucket trip-photos, trip photo metadata, signed/public URL resolution, image loading/cache. TripRepository содержит photo CRUD.

### STATES:

Loading, empty gallery, picker, upload progress, upload error, delete confirmation/error, unavailable image, full-screen viewer.

### IOS STATUS:

Есть grid/upload и отдельные photo views, но full Android gallery/context assignment/cache/error parity не подтверждены.

---

### ANDROID SCREEN:

Общий trip editor и nested modal flows

### ENTRY POINT:

Overview edit pencil, per-section edit buttons, add actions, day/leg/card actions. Это не один экран: Android использует разные sheets/panels в контексте конкретного раздела.

### UI AND BEHAVIOR:

EditTripPanel, TripDateCalendarDialog, CreateDaySheet, EditDaySheet, route editor, catalog sheets, add/edit sheets, location picker, photo viewer, budget add sheet и другие вложенные сценарии.

### DATA AND LOGIC:

Каждый sheet вызывает специализированные repository methods, сохраняет конкретную сущность и обновляет overview/revision.

### STATES:

Modal open/close, keyboard/focus, validation, save/delete progress, conflict/error, cancellation without mutation.

### IOS STATUS:

Основной перенос заменён крупным TripEditorView с секционными Form и общими sheets. Это главный архитектурный источник расхождения: iOS-контекст, entry point и объём полей часто не совпадают с конкретным Android sheet.

## 5. Android data, backend и системные зависимости

### 5.1 Repository и модели

mobile/app/src/main/java/com/odyssey/travelplanner/data/TripRepository.kt содержит модели и операции для:

- trips и trip cards;
- cover photos;
- days и route legs;
- accommodations;
- budget expenses/groups;
- trip members;
- sights и sight days;
- restaurants;
- pet places;
- photo CRUD;
- reorder days/sights/accommodations/route;
- notes, details, status и catalog additions.

Контракт RouteLeg в Android шире текущего iOS-использования: dayId, from, to, date/day/month/weekday, distance, travelTime, checkIn, checkOut, notes, mapsUrl, dayNumber.

TripOverview также включает служебные поля, использующиеся для согласования payload, включая route day count и sight-days version. Их нельзя отбрасывать без проверки всех update flows.

### 5.2 Supabase tables, Storage, RPC и Edge Functions

По Android repository:

- основная persistence — trips;
- collaboration — trip_collaborators;
- profile/settings/onboarding — user_data;
- фотографии — Storage bucket trip-photos;
- catalog — sight_catalog, restaurant_catalog и связанные catalog repositories;
- RPC — leave_trip, patch_trip_payload, manage_trip_member;
- Edge Functions — delete-account, send-invite, restaurant-enrichment.

patch_trip_payload использует optimistic revision. iOS должен сохранять ту же семантику конфликта и merge/update, а не только отправлять похожий JSON.

### 5.3 Authentication

Android:

- Supabase Kotlin client;
- PKCE;
- redirect https://ramingo.online/mobile/auth;
- persistent/memory/remembered session managers;
- Android Keystore AES-GCM для remembered credentials;
- account picker.

iOS:

- собственный REST/URLSession client в SupabaseClient.swift;
- Keychain для auth session;
- Google OAuth и Apple Sign In;
- не обнаружен полноценный эквивалент remembered account picker и Android encrypted credentials flow.

### 5.4 Catalog и города

Android CityCatalogRepository использует curated catalog и bundled compressed world catalog:

- mobile/app/src/main/assets/city-catalog.tsv.bin;
- комментарий в Android-коде указывает на большой мировой каталог примерно на 150 тысяч записей;
- поиск выполняется по локальному curated list и полному catalog.

iOS IOSCityCatalogRepository содержит короткий hardcoded список примерно из нескольких десятков городов. Это функциональное ограничение и один из ключевых источников расхождения create-trip.

### 5.5 Weather

Android WeatherRepository использует Open-Meteo:

- forecast;
- geocoding;
- archive;
- climate;
- historical/climate fallback по датам поездки;
- current и trip weather.

iOS WeatherRepository.swift также обращается к Open-Meteo, но в AppModel и UI встречается подавление ошибок через try?. Поэтому наличие того же URL не гарантирует то же состояние экрана и fallback поведение.

### 5.6 Exchange rates

Android и iOS используют Frankfurter endpoint для валютных курсов, но Android дополнительно поддерживает manual rates и budget fallback states. Эти states должны переноситься вместе с экраном бюджета.

### 5.7 Maps, Places и routing

Android:

- Mapbox SDK;
- Mapbox token из secrets;
- Google Maps URL parsing/redirect;
- route endpoint https://ramingo.online/trip-route/valhalla/route;
- Valhalla/OSM fallback;
- city coordinates;
- map usage в overview, sights, restaurants, accommodation, pets;
- DeviceLocation и Android runtime location permissions.

iOS:

- MapKit;
- CLLocationManager;
- отдельные URLSession/Valhalla/OSM вызовы в TripDetailView;
- Mapbox SDK/token в iOS target не обнаружен;
- Google Sign-In/Google Maps SDK dependency в iOS target не обнаружена.

Таким образом, iOS не является тонким переносом Android maps/routing слоя: это отдельная реализация с другой SDK и другими fallback paths.

### 5.8 Image loading

Android использует Coil Compose и network/OkHttp, а также resolution стабильных Google photo resource names через Edge Function и trip photo storage/cache.

iOS использует RemotePhotoView, URLSession и Storage resolution. Полный эквивалент Coil-кэша, стабильного lifecycle и всех Android photo-unavailable states не подтверждён.

### 5.9 Notifications and background logic

Android ReminderScheduler.kt использует AlarmManager, exact alarm, notification channel и receiver-ы на boot/timezone/time/package/exact-alarm changes.

iOS ReminderScheduler.swift использует локальные UNUserNotificationCenter notifications и синхронизацию при действиях приложения. Android background receiver lifecycle на iOS не переносится напрямую и в текущем коде эквивалентного сценария не найдено.

## 6. Состояние текущей iOS-реализации

| Область | Что уже есть | Статус относительно Android |
|---|---|---|
| App bootstrap | RamingoApp, auth/session bootstrap, URL handling | Частично; Xcode build не подтверждён |
| Auth | Email, Google, Apple, reset request | Частично; нет доказанной remembered-account parity |
| Onboarding | 7 SwiftUI pages | Частично; Android modes/routing не зафиксированы |
| Home/trips | Filters, cards, restore, create, settings | Частично |
| Create trip | Text fields, dates, city picker, backend create | Частично; нет подтверждённого world catalog/photo flow |
| Trip shell | Tabs/section wrappers, drawer-like actions | Частично; generic editor меняет Android flow |
| Overview | Cover, weather, MapKit, blocks, reorder | Частично; больше всего сделано, но не сравнено state-by-state |
| Route | Cards и generic RouteEditorSheet | Неправильно: add action и per-leg pencil flow отсутствуют на route screen |
| Sights | Screen, list/map, часть catalog/editor/photo flow | Частично |
| Restaurants | Screen, filters/map/list, generic editor callback | Частично |
| Accommodation | Cards/status/links, editor | Частично |
| Pets | Filters/catalog/map/location | Частично |
| Budget | Summary/currency/rates/categories, generic CRUD | Частично |
| Members | List/invite/leave | Частично |
| Photos | Grid/upload/photo views | Частично |
| Settings | Profile, theme, language, notifications, password, delete | Частично |
| Notifications | Local iOS notifications | Неполно относительно Android background logic |
| Maps/routing | MapKit + alternate route requests | Не 1:1 с Android Mapbox/Google/route parsing |
| Models | Swift models and REST repository | Неполно относительно Android fields/states |
| Error handling | Some alerts/fallbacks | Неполно; try? скрывает часть ошибок |
| Build verification | Static/structural checks only | Не проверено: нет Xcode toolchain в текущем окружении |

## 7. Fake, mock, hardcoded и упрощённые данные

### 7.1 Что не является fake backend

Следующие iOS части обращаются к реальным источникам и поэтому не должны автоматически называться mock:

- SupabaseClient.swift — реальный REST/Auth/Storage/RPC client;
- TripRepository.swift — реальные Supabase операции;
- CatalogRepository.swift — реальные catalog table/function calls;
- WeatherRepository.swift — реальный Open-Meteo;
- ExchangeRateRepository.swift — реальный Frankfurter;
- ReminderScheduler.swift — реальный локальный notification API iOS.

Однако реальный endpoint сам по себе не доказывает parity поведения: важны payload, fields, ошибки и states.

### 7.2 Найденные hardcoded или упрощённые места

1. ios/Ramingo/Networking/CatalogRepository.swift: короткий hardcoded IOSCityCatalogRepository вместо Android city-catalog.tsv.bin и полного поиска городов. Это реальное функциональное ограничение, а не просто UI fixture.
2. ios/Ramingo/Networking/SupabaseClient.swift: fallback Supabase URL. Это конфигурационный fallback, не fake data, но production configuration следует проверять отдельно.
3. ios/Ramingo/UI/TripDetailView.swift: route/map logic использует отдельную iOS-ветку MapKit/Valhalla/OSM вместо переноса Android RouteDistance и Google/Mapbox URL parsing. Это не mock, но это другая логика.
4. ios/Ramingo/UI/TripDetailView.swift и ios/Ramingo/State/AppModel.swift: ряд сетевых/фото/weather/rate операций завершается через try? или fallback. Внешне это может выглядеть как пустой/успешный экран при реальной ошибке.
5. ios/Ramingo/UI/TripDetailView.swift: префикс Android...Screen обозначает попытку визуальной оболочки, но не подтверждает перенос Android поведения. В ряде случаев это собственные SwiftUI views с другими entry points.
6. design-reference/ios-local-preview.html: это локальный web preview, где pencil может быть только preview/toast interaction. Этот файл не является нативной iOS реализацией и не должен использоваться как источник функциональной истины.

Не найдено доказательств, что основные trip cards или backend payload целиком заменены статическим mock-массивом. Основная проблема — неполная модель, hardcoded city catalog, другая routing implementation и подавленные ошибки, а не один общий mock backend.

## 8. Подтверждённая причина проблемы с карандашом и добавлением маршрута

Это сравнение сделано по Android-коду, текущему iOS-коду и пользовательскому screenshot.

### Android

TripRouteContent:

- хранит editingLeg конкретного переезда;
- по пустому маршруту показывает действие добавления дня, если пользователь может редактировать;
- по pencil передаёт выбранный leg в RouteLegEditorSheet;
- после сохранения вызывает специализированную route repository operation;
- отдельный screenshot показывает короткое редактирование времени заселения с заголовком «Изменить переезд».

### iOS

В TripDetailView.swift:

- AndroidRouteScreen получает только onEdit: () -> Void;
- route card вызывает этот callback без передачи выбранного RouteLeg;
- empty state не содержит Android add action;
- callback из trip detail вызывает openFullEditor(section: "route");
- TripEditorView.swift содержит кнопку «Добавить переезд», поэтому add action спрятан в другом экране;
- общий RouteEditorSheet редактирует более широкий объект и не является Android-модалкой времени заселения.

Следствие: нажатие карандаша не может открыть модалку именно того переезда и именно в том контексте, который показан на Android. Это не мелкая проблема hit-test или icon button; причина в неправильной архитектуре entry point и callback contract.

## 9. Какие Android-функции отсутствуют или не подтверждены на iOS

Список ниже означает «не найдено полного подтверждения 1:1 в текущем iOS-коде», а не утверждение, что каждое действие абсолютно отсутствует:

- route-screen add action при пустом списке;
- per-leg route edit с передачей выбранного leg;
- Android modal для изменения времени заселения;
- полноценный Android route reorder/save/error flow;
- мировой city catalog из bundled asset;
- Android create-trip photo/cover assignment flow;
- точные Android collaborator permissions для всех add/edit/delete actions;
- remembered accounts и encrypted credentials/account picker semantics;
- Android Mapbox/Google route parsing и тот же fallback order;
- runtime location permission and distance states for pets;
- Android catalog sheets/details/enrichment для restaurants;
- Android catalog/date/photo sheets для accommodation;
- Android day/sight/catalog/location/photo viewer sheets;
- полный budget expenses/groups CRUD в контексте budget screen;
- полная members invite/role/remove/leave state machine;
- полный photo gallery/context assignment/viewer/cache/error flow;
- boot/timezone/package/exact-alarm rescheduling;
- доказанная parity для revision conflict and patch_trip_payload;
- доказанная parity для every loading/empty/error/success state;
- реальная iOS build/run verification on iPhone simulator/device.

## 10. Какие iOS-экраны сделаны без достаточного подтверждения Android-кода

По структуре и текущим callbacks видно, что следующие части были собраны как самостоятельные SwiftUI-экраны до полной фиксации Android contract:

1. Большой TripDetailView.swift с AndroidOverviewScreen, AndroidRouteScreen, AndroidSightsScreen, AndroidRestaurantsScreen, AndroidAccommodationScreen, AndroidPetsScreen, AndroidBudgetScreen, AndroidMembersScreen, AndroidPhotosScreen.
2. Универсальный TripEditorView.swift, который объединяет разные Android sheets в один Form/editor.
3. iOS route implementation с callback без RouteLeg и с add action, доступным только внутри общего редактора.
4. iOS city picker с коротким hardcoded catalog вместо Android bundled-world search.
5. iOS MapKit route/map implementation, созданная как аналог, но без подтверждения идентичного Android SDK/fallback contract.
6. Упрощённая auth persistence semantics, где UI copy про запоминание не соответствует Android remembered account picker.
7. iOS section views, где loading/error/empty states часто скрыты общим fallback/try?, поэтому визуальный экран может выглядеть завершённым при отсутствии Android state machine.

Это не означает, что эти экраны нужно удалить. Это означает, что их нельзя считать verified parity и нельзя дальше масштабировать тем же способом до фиксации Android contract.

## 11. Почему предыдущая стратегия дала неправильный результат

### Причина A: неверная единица переноса

Переносилась визуальная секция или SwiftUI-файл, а не пользовательский вертикальный сценарий:

entry point → state → action → repository call → response/error → updated screen.

Для маршрута это проявилось как красивый список карточек, но без Android add action и без per-leg edit contract.

### Причина B: универсальный редактор заменил контекстные Android sheets

Android использует небольшие sheets в контексте текущего экрана. TripEditorView объединил их в Form и изменил:

- место, из которого открывается действие;
- набор полей;
- размер и тип модалки;
- передачу выбранной сущности;
- обработку отмены/сохранения;
- видимость permission-dependent действий.

### Причина C: модели iOS были сужены

Если iOS-модель не содержит Android fields, UI не сможет повторить Android states. Подтверждённый пример — дополнительные Sight fields и служебные overview/route fields.

### Причина D: backend parity была принята за UI parity

Наличие Supabase client и похожих методов не гарантирует одинаковый payload, revision handling, error mapping, Storage lifecycle и permission checks.

### Причина E: разные внешние SDK и fallback paths

MapKit вместо Android Mapbox/Google, другая route implementation и другое image-loading поведение дают иной результат даже при похожем UI.

### Причина F: отсутствует цикл device verification

В Windows-окружении нет Xcode toolchain. Пока не сделаны Android/iOS captures в одинаковом состоянии на macOS/simulator, нельзя объявлять screen parity завершённой.

## 12. Что сейчас нельзя считать проверенным

Ни один iOS-экран не должен иметь статус «1:1 verified». Причины:

- не выполнена сборка iOS через Xcode;
- не выполнен запуск iOS simulator/device;
- нет парных captures Android и iOS для одного состояния;
- не проверены реальные Supabase permissions/error responses на iOS;
- не проверен deep link/reset/invite lifecycle на iOS;
- часть текущих изменений в рабочем дереве уже была сделана до этого аудита, но не является доказанным baseline.

В текущем окружении команды xcodebuild, swiftc и swift не обнаружены, поэтому этот отчёт не выдаёт предположение за результат iOS compilation.

## 13. Безопасный следующий порядок миграции

Это план после аудита; в рамках текущего шага код не менялся.

### Шаг 0. Зафиксировать baseline

- сохранить этот audit;
- не трогать web и Android;
- получить на Android точный screenshot/video одного route сценария;
- зафиксировать trip payload, collaborator role и начальное состояние;
- зафиксировать ожидаемые tap targets и тексты modal.

### Шаг 1. Один vertical slice: Route

Сначала переносится только:

- TripRouteContent;
- empty route add action;
- route card;
- per-leg pencil;
- Android modal «Изменить переезд»;
- add/edit/delete/reorder;
- loading/empty/error/success;
- конкретные Android repository/API contracts.

Нельзя одновременно менять overview, auth, backend, schema или остальные разделы.

### Шаг 2. Проверка

Для одного и того же состояния:

- Android capture;
- iOS simulator capture;
- сравнение entry point, modal, fields, action, payload и error state;
- исправление только route slice;
- повторная проверка.

### Шаг 3. Следующие срезы

Только после подтверждения route:

1. Overview.
2. Sights.
3. Restaurants.
4. Accommodation.
5. Pets.
6. Budget.
7. Members.
8. Photos.
9. Auth/settings/background behavior.

Порядок может быть изменён только по конкретному Android сценарному приоритету, но не по визуальному удобству текущей iOS-структуры.

## 14. Файлы, на которых основан аудит

### Android

- mobile/app/src/main/java/com/odyssey/travelplanner/MainActivity.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/ui/OdysseyApp.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/TripRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/AccountRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/CityCatalogRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/SightCatalogRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/RestaurantCatalogRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/AccommodationCatalogRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/PetCatalogRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/WeatherRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/ExchangeRateRepository.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/RouteDistance.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/data/DeviceLocation.kt
- mobile/app/src/main/java/com/odyssey/travelplanner/notifications/ReminderScheduler.kt
- mobile/app/src/main/AndroidManifest.xml
- mobile/app/build.gradle.kts
- mobile/app/src/main/assets/city-catalog.tsv.bin

### iOS

- ios/Ramingo/RamingoApp.swift
- ios/Ramingo/State/AppModel.swift
- ios/Ramingo/Networking/SupabaseClient.swift
- ios/Ramingo/Networking/TripRepository.swift
- ios/Ramingo/Networking/AccountRepository.swift
- ios/Ramingo/Networking/CatalogRepository.swift
- ios/Ramingo/Networking/WeatherRepository.swift
- ios/Ramingo/Networking/ExchangeRateRepository.swift
- ios/Ramingo/UI/AuthView.swift
- ios/Ramingo/UI/HomeView.swift
- ios/Ramingo/UI/CreateTripView.swift
- ios/Ramingo/UI/SettingsView.swift
- ios/Ramingo/UI/TripDetailView.swift
- ios/Ramingo/UI/TripEditorView.swift
- ios/Ramingo/Notifications/ReminderScheduler.swift
- ios/Ramingo/Ramingo.xcodeproj/project.pbxproj
- ios/Ramingo/Info.plist
- ios/Ramingo/Ramingo.entitlements

## 15. Финальный статус аудита

Аудит выполнен на уровне структуры репозитория, Android Compose navigation, Android data/repository contracts и текущей iOS реализации.

Главный подтверждённый дефект для ближайшего контролируемого шага:

> iOS route screen не повторяет Android entry point для добавления маршрута и редактирования конкретного переезда; карандаш открывает общий редактор вместо Android-модалки выбранного переезда.

До отдельного согласованного route-среза не следует продолжать массовый перенос, менять backend/schema, расширять generic editor или редактировать web/Android.
