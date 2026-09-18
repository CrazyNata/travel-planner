# Ramingo iOS migration plan

Дата аудита: 2026-09-17
Источник функционального паритета: `mobile/` (нативный Android-клиент)
Целевой код: `ios/` (нативный SwiftUI-клиент)

> В рамках этого аудита Android и web были только прочитаны и этим аудитом не изменялись. Обнаруженные до аудита незакоммиченные изменения в `mobile/`, `src/`, `design-reference/` и прочих артефактах сохранены как есть; существующие изменения пользователя в iOS также сохранены.

## 1. Границы и правила реализации

- Реализовать нативное iOS-приложение на Swift/SwiftUI, используя существующий проект `ios/Ramingo`.
- Android остаётся read-only источником экранов, состояний, действий и контрактов API; Android-код не переписывается и не форматируется.
- Web остаётся вне области работ; responsive/web-реализация не является заменой native iOS.
- Supabase остаётся единственным постоянным хранилищем данных приложения.
- Не менять схему Supabase: таблицы, колонки, связи, индексы, RPC, Edge Functions, RLS, миграции и прочие schema objects.
- Не использовать `UserDefaults`, `localStorage`, `sessionStorage`, IndexedDB, cookies или browser cache для данных приложения, черновиков или UI-state. Keychain разрешён только для auth-сессии/учётных данных согласно текущей политике приложения.
- При обновлении существующей поездки не удалять и не заменять пользовательские данные; изменения шаблонов применять только к новым поездкам.
- Каждый экран должен иметь состояния idle/loading/loaded/empty/error и retry там, где операция повторяема.
- В таблицах ниже `IMPLEMENTED` означает наличие кода, а `TESTED` — только подтверждённую сборкой и runtime-проверкой на macOS/iPhone. На дату аудита полноценная iOS-сборка и device QA с Windows не подтверждены.

## 2. Что найдено в Android-источнике

### Архитектура и маршруты

Android реализован в `mobile/app/src/main/java/com/odyssey/travelplanner/` на Kotlin/Compose. Основной граф навигации собран в `ui/OdysseyApp.kt`; крупные экраны пока находятся в одном Compose-файле, а работа с данными вынесена в `data/`.

Основные маршруты:

| Route | Экран | Назначение |
| --- | --- | --- |
| `foundation` | Auth | вход, регистрация, восстановление, OAuth |
| `onboarding/{mode}` | Onboarding tutorial | первое знакомство, повторный просмотр, create-flow подсказки |
| `trips` | My trips | список, фильтры, удалённые/черновики/завершённые поездки |
| `settings` | Account settings | профиль, тема, язык, уведомления, пароль, удаление аккаунта |
| `create-trip` | Create trip | создание поездки, даты, города, фотографии и подсказки |
| `reset-password` | Reset password | установка нового пароля из deep link |
| `trip/{tripId}` | Trip overview | обзор и все разделы поездки |

Внутри поездки Android поддерживает разделы в следующей логике: Overview, Route, Sights, Restaurants, Accommodation, Pets, Budget, Members, Photos.

### Android-функции, которые должны быть отражены на iOS

- локализация RU/EN/ES/DE, включая названия городов, достопримечательностей, ресторанов и погодные статусы;
- светлая/тёмная/системная тема и единая типографика Manrope;
- поиск городов из локального каталога с aliases, флагами, координатами и fallback-геокодированием;
- создание и редактирование поездки с датами, городами, координатами, обложкой и галереей;
- обзор маршрута с городами, картой, погодой, прогрессом и подсказками;
- маршрутные дни, порядок legs, дата/день, расстояние, время в пути, check-in/check-out, заметки, ссылки на карты;
- каталог и ручное добавление достопримечательностей, ресторанов, жилья и pet places;
- фото для поездки и объектов, cover photo, перемещение/удаление/замена обложки;
- бюджет: валюта, ручные курсы, группы, расходы, категории, scopes и участники оплаты;
- участники: роли, приглашения, изменение роли, удаление и выход из поездки;
- pets: типы мест, фильтры по городу/радиусу/рейтингу/open now/features;
- локальные напоминания о начале поездки, бесплатной отмене, оплате жилья и email-настройках;
- deep links для приглашений, восстановления пароля и открытия поездки после notification tap;
- Mapbox-карта с маркерами, polyline маршрута и текущей геопозицией; внешние ссылки на Google Maps;
- loading/empty/error/success состояния и повтор операции.

### Обнаруженные Android-контракты

`data/TripRepository.kt` содержит модели и действия для всех основных секций. Среди них `TripOverview`, `TripSummary`, `RouteLeg`, `Sight`, `SightDay`, `Restaurant`, `Accommodation`, `BudgetExpense`, `BudgetGroup`, `TripMember`, `PetPlace`, `CoverPhoto`, а также операции создания, soft-delete/restore, выхода, patch/update, сортировки, фото и каталогов.

Дополнительные Android-компоненты:

- `AccountRepository.kt` — профиль и настройки в `user_data`;
- `CityCatalog.kt` и `CityCatalogRepository.kt` — локальный каталог городов и aliases;
- `WeatherRepository.kt` — Open-Meteo forecast/geocoding/archive/climate;
- `ExchangeRateRepository.kt` — Frankfurter;
- `CatalogRepository`-аналоги для sights/restaurants/accommodation/pets — Supabase catalog + live enrichment;
- `TripPhotoStorage.kt` — signed URL, canonical `storage://` references и cache signed URLs;
- `TripRouteSchedule.kt`, `RouteDistance.kt`, `TripProgress.kt` — календарь route days, дистанции и прогресс;
- `notifications/ReminderScheduler.kt` — alarms, boot/timezone reschedule и notification tap.

## 3. Текущее состояние iOS

### Уже есть в проекте

Проект `ios/Ramingo` уже содержит SwiftUI-клиент с `NavigationStack`, email/password auth, Google PKCE, Sign in with Apple, Keychain auth-сессией, списком поездок, созданием поездки, всеми девятью секциями поездки, MapKit, signed photo URLs, Supabase catalogs/enrichment, Open-Meteo, Frankfurter и локальными напоминаниями.

Основные файлы:

- `RamingoApp.swift` — bootstrap, auth/root routing, recovery sheet и deep links;
- `State/AppModel.swift` — `@MainActor` state, auth, trips, overview, profile, reminders и orchestration;
- `Networking/SupabaseClient.swift` — REST/Auth/Storage/Edge Function/RPC клиент и Keychain;
- `Networking/TripRepository.swift` — Codable-модели, PostgREST и optimistic revision patch;
- `Networking/AccountRepository.swift` — `user_data` и account profile;
- `Networking/CatalogRepository.swift` — sights/restaurants/accommodation/pets;
- `Networking/WeatherRepository.swift`, `ExchangeRateRepository.swift` — внешние data providers;
- `Notifications/ReminderScheduler.swift` — iOS local notifications;
- `UI/HomeView.swift`, `CreateTripView.swift`, `TripDetailView.swift`, `TripEditorView.swift`, `SettingsView.swift`, `AuthView.swift` — основной UI;
- `UI/AppTheme.swift` — цвета, Manrope и общие UI-компоненты.

Текущие незакоммиченные изменения в iOS-коде не являются частью этого аудита и должны сохраняться при дальнейшей работе.

### Важные ограничения текущей iOS-версии

- Нет iOS test target и автоматических iOS-тестов.
- На Windows нельзя выполнить `xcodebuild`; сборку следует проверять в GitHub Actions или на macOS.
- Текущий `TripDetailView` использует MapKit с numbered city pins и прямым polyline между городами. Это ещё не полный Android-паритет с route geometry, sight markers и current location.
- В iOS-коде нет отдельного `CityCatalogRepository` с Android-поведением aliases/локальным каталогом.
- В iOS-коде не найден отдельный flow `leaveTrip`, хотя Android поддерживает `leave_trip`.
- Onboarding/tutorial из Android (7 страниц и сохранённые флаги) в текущем iOS UI не найден.
- Основной iOS UI пока преимущественно русскоязычный; полная RU/EN/ES/DE локализация — отдельная задача.
- `Info.plist` сейчас содержит photo usage description, но не location usage description; запрашивать геолокацию следует только вместе с реальной iOS-реализацией соответствующего Android-сценария.
- Auth/recovery/deep-link, Apple sign-in, уведомления, catalog enrichment, storage signed URLs и item editing требуют runtime QA, даже если соответствующий код уже присутствует.

## 4. Экранный и feature-паритет

| Android screen/flow | Модели и API | iOS-состояние на аудите | Что проверить или доделать |
| --- | --- | --- | --- |
| Auth / Foundation | Supabase Auth password, signup, Google PKCE, Apple, recovery | `IMPLEMENTED`, QA pending | Error mapping, validation, registration notice, OAuth callbacks, expired session |
| Onboarding tutorial | account profile flags in `user_data`; 7 pages; create-flow hints | `TODO` | Native paged flow, skip/replay, first-run flag, create-trip/add-place hints, localization |
| My trips | `trips`, `trip_collaborators`; load/filter/soft-delete/restore | `IMPLEMENTED`, QA pending | Empty/loading/error, deleted filter, deep-link opening, dates/status/progress |
| Create trip | insert `trips`; cities, coordinates, dates, initial payload | `IMPLEMENTED`, parity gap | City catalog/aliases/flags, date validation, cover/gallery, create-flow hints, preservation rules |
| Trip overview | `loadOverview`, revision, weather, rates, photos | `IMPLEMENTED`, QA pending | Refresh/error/not-found, progress, map parity, weather/rates failure states |
| Overview | overview blocks, cities, weather, map points | `IMPLEMENTED`, partial parity | Android-style edit mode, reorder persistence, map/weather selectors, richer map, current location, route geometry, localization |
| Route | route legs, `patch_trip_payload`, route schedule/distance providers | `IMPLEMENTED`, partial parity | Network distance providers, approximate fallback labeling, route days, reorder, check-in/out, map links |
| Sights | `sights`, `sightDays`, catalog/enrichment/photo resolve | `IMPLEMENTED`, QA pending | Catalog filters, day assignment, notes, rich edit, photo states, reorder persistence |
| Restaurants | `restaurants`, enrichment/photo resolve, filters | `IMPLEMENTED`, QA pending | Filter parity, reservation fields, status/details/photos, empty/error/retry |
| Accommodation | `accommodations`, enrichment/photo resolve, deadlines/status | `IMPLEMENTED`, QA pending | Deadline/payment reminder semantics, catalog fields, reorder, photos, booking links |
| Budget | budget currency/manual rates, expenses, groups, member scopes | `IMPLEMENTED`, QA pending | Conversion/error states, categories, edit/delete, group split, formatting and locale |
| Members | `trip_collaborators`, `manage_trip_member`, `send-invite` | `IMPLEMENTED`, partial parity | Invite redirect, role changes, remove member, leave-trip flow, permission gating |
| Photos | Storage `trip-photos`, signed URLs, cover/item photos | `IMPLEMENTED`, QA pending | Upload/delete/replace/move, authorization errors, cache expiry, Photos permission |
| Pets | `petPlaces`, pet catalog/enrichment/photo resolve | `IMPLEMENTED`, partial parity | City/radius/rating/open-now/features filters, types, map/details, photo attribution |
| Account settings | `user_data` profile and notification fields | `IMPLEMENTED`, QA pending | Profile photo, language/theme, password, account delete, persistence and restart |
| Notification settings | `user_data` flags + `ReminderScheduler` | `IMPLEMENTED`, QA pending | Permission denial, scheduling/rescheduling, cancellation/payment/email settings, tap routing |
| Reset password / deep links | Auth recovery, invite, notification trip ID | `IMPLEMENTED`, verification required | Test custom `ramingo://` and HTTPS `/mobile/*` routes, cold start and already-running app |

## 5. API, storage и data contracts

### Supabase

| Сервис | Контракт | iOS implementation target |
| --- | --- | --- |
| Auth | email/password, signup, password recovery, Google PKCE, Sign in with Apple | `SupabaseClient.swift`, `AppModel.swift`, `AuthView.swift` |
| `trips` | load/create trip rows, payload JSON, owner | `TripRepository.loadTrips/createTrip` |
| `trip_collaborators` | collaborators and roles | `TripRepository`, `AppModel` |
| `user_data` | key `account_profile`; language/theme/notifications/onboarding flags | `AccountRepository`, `SettingsView` |
| RPC `patch_trip_payload` | `p_trip_id`, `p_patch`, `p_expected_revision` | optimistic revision mutation in `TripRepository` |
| RPC `leave_trip` | `p_trip_id` | add iOS `leaveTrip` wrapper and UI |
| RPC `manage_trip_member` | trip/member/delete/role operation | member role/remove operations |
| Edge Function `send-invite` | `{tripId,email,name,role,redirectTo}` | invite form and deep-link verification |
| Edge Function `delete-account` | authenticated account deletion | Settings delete-account flow |
| Edge Function `restaurant-enrichment` | category, city, query, language, limit and pet type where needed | unified catalog service |
| Storage bucket `trip-photos` | upload/delete signed URLs; canonical `storage://trip-photos/<path>` | `TripPhotoStorage` behavior in `SupabaseClient`/repository |

Все mutation-запросы должны сохранять optimistic revision semantics Android-клиента и корректно показывать conflict/error, не затирая чужие изменения.

### Внешние сервисы

| Сервис | Использование |
| --- | --- |
| Open-Meteo forecast | current temperature/code и daily forecast, timezone города |
| Open-Meteo geocoding | поиск/уточнение города |
| Open-Meteo archive | историческая погода для прошедших дат |
| Open-Meteo climate | климатический fallback, если он нужен сценарию Android |
| Frankfurter | курсы валют, включая базовую RUB-логику Android |
| Valhalla | `https://ramingo.online/trip-route/valhalla/route` и публичный fallback |
| OpenStreetMap routing | foot/bike/car route fallback |
| Mapbox Directions | последний routing fallback при наличии token |
| Google/Yandex/Apple Maps links | внешнее открытие места/маршрута; URL parsing для координат |

Никакой production-функционал не должен подменяться mock-данными. Если внешний сервис недоступен, UI показывает error/approximate state с retry и не делает вид, что данные точные.

## 6. Persistence, cache и permissions

### Постоянные данные

- поездки, payload, участники, профиль, настройки, фотографии и все пользовательские изменения сохраняются только через Supabase;
- auth-сессия и при необходимости remembered credentials — только через текущий защищённый Keychain-механизм;
- transient UI state хранится в SwiftUI/AppModel memory и восстанавливается через загрузку из Supabase, а не через browser/device storage;
- signed photo URL можно кэшировать только как временный runtime cache с TTL, не как пользовательские данные;
- ревизия поездки обязательна для patch/update.

### Изображения

- поддержать external URL без повторной загрузки;
- для Supabase photos использовать canonical storage reference, signed URL, загрузку/удаление/замену cover и item photo;
- обрабатывать permission denied, network error, oversized/unsupported image и expired signed URL;
- использовать `PhotosPicker`/нативный Photos permission на iOS; не хранить черновик изображения в browser storage.

### Permissions

- Notifications: запрашивать только при включении уведомлений и показывать состояние denied/settings;
- Photos: запрашивать через нативный picker/upload flow;
- Location: добавить `NSLocationWhenInUseUsageDescription` и `CoreLocation` flow только при реализации current-location/map behavior, затем отдельно проверить privacy text;
- Sign in with Apple, camera/location/photos и notifications должны иметь QA сценарии cold start, denial и повторного разрешения в Settings.

## 7. Deep links, maps и background behavior

### Deep links

Android принимает:

- `https://ramingo.online/mobile/invite?tripId=...`;
- `https://ramingo.online/mobile/reset`;
- legacy `https://travelplanner.muntim.ru/mobile/...`;
- notification intent с trip ID.

iOS уже поддерживает custom scheme `ramingo://auth-callback` и recovery handling. План проверки:

1. сохранить auth recovery callback;
2. обработать invite link и открыть конкретную поездку после завершения auth;
3. обработать reset link на cold start и при уже запущенном приложении;
4. обработать notification tap и отложенное открытие trip ID после восстановления сессии;
5. проверить HTTPS/legacy redirect compatibility с `send-invite` и Supabase Auth.

### Maps и geolocation

Минимальный iOS-паритет:

- MapKit map для overview и route;
- numbered city markers, sight/accommodation/restaurant/pet markers;
- route polyline от routing provider, а не только straight line между городами;
- fit/zoom/selected marker и внешнее открытие карты;
- current location по явному permission и понятному fallback без location;
- обработка отсутствующих координат и approximate route.

Текущий `NumberedTripMap` в `TripDetailView.swift` — только базовый city-pin/straight-polyline вариант, поэтому считать его полностью протестированным нельзя.

### Уведомления и фон

iOS `ReminderScheduler` должен покрывать Android-сценарии:

- напоминание о начале поездки;
- deadline бесплатной отмены жилья;
- payment deadline жилья;
- email notification settings;
- пересоздание после изменения поездки/настроек;
- корректный tap payload с trip ID;
- permission denied и отсутствие exact scheduling capability.

На iOS не переносить Android alarm receiver буквально: использовать нативный `UNUserNotificationCenter`, а ограничения фонового выполнения явно отражать в UX и QA.

## 8. План работ по фазам

### Phase 1 — audit и contract freeze

- завершить этот документ и зафиксировать screen/API/model matrix;
- сравнить Android payload codec, revision patch и существующие Codable-модели iOS;
- составить набор ручных QA сценариев для каждого loading/empty/error/success состояния;
- не менять backend schema, Android или web.

### Phase 2 — устранение parity gaps

Порядок реализации:

1. iOS onboarding/tutorial с account flags и replay/create-flow hint;
2. `CityCatalogRepository` с локальным каталогом/aliases/flags/coordinates и тем же search behavior;
3. `leaveTrip` и permission-aware member flow;
4. deep-link matrix invite/reset/notification на cold и warm start;
5. routing provider chain и approximate fallback label;
6. MapKit markers/polyline/current-location flow с permission handling;
7. полная localization layer RU/EN/ES/DE;
8. missing climate/route/photo edge cases и единые retry/error components.

Каждый пункт проверять только в `ios/`; Android используется для сверки поведения.

### Phase 3 — feature QA и automated coverage

- добавить iOS unit tests для payload decoding/encoding, route schedule, progress, budget conversions, filter predicates, deep-link parsing и reminder dates;
- добавить repository tests для revision conflict, soft-delete/restore, leave/member mutations;
- проверить catalog/photo URL resolution и external URL passthrough;
- пройти manual flow на iPhone Simulator и физическом iPhone: auth, create trip, each section, edit/delete/restore, invite, photos, reminders, account deletion;
- отдельно проверить уже заполненную существующую поездку и убедиться, что данные не перезаписываются.

### Phase 4 — build, release and parity sign-off

- выполнить `xcodebuild`/архив на macOS или в GitHub Actions;
- проверить deployment target iOS 17, entitlements, privacy manifest, export compliance и App Store metadata;
- собрать screenshots/review notes и обновить `IOS_RELEASE_BUGS.md` фактическими результатами;
- поддерживать `ANDROID_IOS_PARITY.md` как текущую parity-матрицу и после macOS/iPhone QA перевести все подтверждённые строки в финальные статусы;
- перед релизом убедиться, что git diff содержит изменения только в `ios/` и разрешённых migration/release docs.

## 9. Критерии готовности

Работа считается завершённой, когда:

- все Android routes и девять trip sections доступны в native iOS UI;
- auth, recovery, invite, notification tap и session restore подтверждены на cold/warm start;
- существующие populated trips не теряют данные при загрузке, patch, refresh, restore или добавлении новых функций;
- все Supabase mutations используют существующие API и revision contract без schema changes;
- catalogs, weather, rates, routing, photos и notifications имеют loading/empty/error/retry состояния;
- onboarding, city aliases, leave-trip, maps/geolocation и localization gaps закрыты либо явно документированы как release blocker;
- есть unit/runtime QA и подтверждённая macOS/iPhone сборка;
- `ANDROID_IOS_PARITY.md` содержит финальное сопоставление Android/iOS с доказательством тестирования;
- web и Android остаются нетронутыми.

## 10. Прогресс реализации

После аудита в iOS добавлены следующие parity-блоки:

- native onboarding/tutorial на 7 шагов с серверными флагами `account_profile`, совместимым `web-onboarding` marker и безопасной миграцией старых аккаунтов с уже существующими поездками;
- `create_trip_hint_seen` и `add_place_hint_seen` с transient UI-подсказками на Home и Trip Detail;
- iOS city catalog с curated aliases, RU/EN/ES/DE names, country flags, координатами, ограниченным fuzzy search и Open-Meteo geocoding fallback для произвольного введённого города;
- передача `cityCoordinates` в новый payload поездки без изменения схемы Supabase;
- `leave_trip` repository/AppModel wrapper и native «Покинуть поездку» flow для не-владельца;
- Associated Domains для `ramingo.online` и legacy `travelplanner.muntim.ru`, сохранённый `ramingo://auth-callback`, notification trip routing;
- `NSLocationWhenInUseUsageDescription`, MapKit user-location permission handling, Valhalla → OpenStreetMap → Apple road-routing chain с approximate straight-line fallback и расстоянием маршрута в Overview;
- обновлённый порядок drawer-разделов, совпадающий с Android: Pets → Budget → Members → Photos.
- Android-style Overview edit mode: top pencil/check toggle, persisted `overviewBlocks`, map/weather city selectors and cover-photo upload.

Эти изменения требуют macOS/iPhone QA и пока не переводят release blockers в `TESTED`: на Windows Xcode не установлен. Следующие обязательные блоки после этой итерации — полная localization layer для уже существующих русских UI-строк, расширение city catalog до полного Android world asset, проверка HTTPS universal links/Apple provisioning, unit tests и runtime QA. Текущая матрица зафиксирована в `ANDROID_IOS_PARITY.md` и должна быть обновлена после этих проверок.
