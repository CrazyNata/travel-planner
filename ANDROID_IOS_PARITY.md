# Android → iOS parity

Дата текущего сравнения: 2026-09-17
Источник: `mobile/` Android-клиент, read-only
Результат: native iPhone implementation in `ios/`

> В этой итерации изменялись только iOS-код и migration/release-документация. Android и web этим implementation pass не изменялись. `TESTED` здесь означает подтверждение на macOS/iPhone; на Windows такая проверка невозможна.

## Статусы

- `IMPLEMENTED` — сценарий есть в iOS-коде и использует production API.
- `PARTIAL` — основной flow есть, но отсутствует часть Android-поведения или полный каталог.
- `QA_PENDING` — код есть, но нужна macOS/iPhone runtime-проверка.
- `BLOCKED` — требуется внешний Apple/Supabase/App Store доступ.

## Навигация и экраны

| Android route/section | iOS implementation | Status | Notes |
| --- | --- | --- | --- |
| `foundation` / Auth | `AuthView.swift`, `SupabaseClient.swift`, `AppModel.swift` | QA_PENDING | Email/password, registration, Google PKCE, Apple, recovery |
| `onboarding/{mode}` | `RamingoApp.swift` `OnboardingView` | IMPLEMENTED / QA_PENDING | Seven native SwiftUI pages, server flags, skip/replay-compatible state, create-trip handoff |
| `trips` / My trips | `HomeView.swift` | QA_PENDING | Filters, empty/loading, soft-delete/restore, pending notification/invite navigation |
| `create-trip` | `CreateTripView.swift`, `TripRepository.swift` | IMPLEMENTED / QA_PENDING | Required fields, city aliases, coordinates, geocoding fallback, Supabase insert |
| `trip/{tripId}` / Overview | `TripDetailView.swift` | QA_PENDING | Loading/error/not-found/refresh, weather, rates, progress, map, Android-style overview edit mode |
| Route | `AndroidRouteScreen`, `TripEditorView.swift` | QA_PENDING | Legs, schedule fields, reorder/edit, routing distance fallback |
| Sights | `AndroidSightsScreen`, `CatalogRepository.swift`, editor | QA_PENDING | Catalog/manual add, days, notes, reorder, photos |
| Restaurants | `AndroidRestaurantsScreen`, catalog/editor | QA_PENDING | Filters, reservations, status, photos, map links |
| Accommodation | `AndroidAccommodationScreen`, catalog/editor | QA_PENDING | Deadlines, payment status, booking fields, photos |
| Budget | `AndroidBudgetScreen`, editor, exchange repository | QA_PENDING | Currency, manual rates, expenses, groups and scopes |
| Members | `AndroidMembersScreen`, editor, RPC/function wrappers | QA_PENDING | Invite, roles, remove, owner gating, leave-trip |
| Photos | `AndroidPhotosScreen`, `TripPhotoStorage` behavior | QA_PENDING | Signed URLs, upload/delete/cover/item photos |
| Pets | `AndroidPetsScreen`, pet catalog/editor | QA_PENDING | Radius/rating/open-now/features/type filters, map and details |
| Settings | `SettingsView.swift`, `AccountRepository.swift` | QA_PENDING | Profile, theme, language, notifications, password, account deletion |

## Backend and external service parity

| Android contract | iOS mapping | Status |
| --- | --- | --- |
| Supabase Auth | REST auth client, Keychain session, OAuth callbacks | QA_PENDING |
| `trips` / `trip_collaborators` | `TripRepository` PostgREST load/create/overview | QA_PENDING |
| `user_data.account_profile` | `AccountRepository`, including onboarding/hint flags | QA_PENDING |
| `patch_trip_payload` + expected revision | All iOS trip mutations through repository patch helpers | QA_PENDING |
| `manage_trip_member` | Role/remove wrappers | QA_PENDING |
| `leave_trip` | `TripRepository.leaveTrip` + non-owner UI flow | QA_PENDING |
| `send-invite` | Edge Function with HTTPS invite redirect | QA_PENDING |
| `delete-account` | Settings account deletion | QA_PENDING |
| `restaurant-enrichment` | Unified catalog repository for sights/restaurants/accommodation/pets | QA_PENDING |
| `trip-photos` Storage | Signed URL, canonical `storage://` references, upload/delete | QA_PENDING |
| Open-Meteo forecast/geocoding/archive/climate | `WeatherRepository`, plus create-trip geocoding fallback | QA_PENDING |
| Frankfurter rates | `ExchangeRateRepository` | QA_PENDING |
| Valhalla → OSM → Mapbox/Apple fallback | iOS Valhalla → OSM distance + Apple MapKit geometry + straight-line fallback | QA_PENDING |
| Android alarm scheduler | iOS `UNUserNotificationCenter` local reminders | QA_PENDING |

## Native behavior parity

| Capability | iOS state | Notes |
| --- | --- | --- |
| Persistent application data | IMPLEMENTED | Supabase only; no localStorage/sessionStorage/IndexedDB/browser cache |
| Auth persistence | IMPLEMENTED | Keychain session store; remember-me controls persistence |
| City aliases and coordinates | PARTIAL | Curated RU/EN/ES/DE layer plus fuzzy search and Open-Meteo fallback; Android full world asset still requires porting/QA |
| Map markers and route | PARTIAL / QA_PENDING | MapKit pins, road polyline, route provider distance, user location permission and fallback; overview city selection persists |
| Universal links | QA_PENDING | Associated domains added for `ramingo.online` and legacy host; AASA/provisioning/device verification remains |
| Auth custom scheme | QA_PENDING | `ramingo://auth-callback` retained for recovery/OAuth |
| Notification tap routing | QA_PENDING | Trip ID retained through session restore and opened from Home |
| Photos permission | QA_PENDING | Native `PhotosPicker` flow |
| Location permission | QA_PENDING | `NSLocationWhenInUseUsageDescription`, requested from overview map only |
| Loading/empty/error/retry | QA_PENDING | Present across existing screens; requires device pass through |
| Localization | PARTIAL | Catalog/onboarding support RU/EN/ES/DE; legacy UI strings remain primarily Russian |
| Unit/UI tests | OPEN | Existing iOS target has no test bundle; add on macOS before release sign-off |

## Release gates still open

1. Build and run `xcodebuild` on macOS/GitHub Actions.
2. Test auth, recovery, Apple, invite, universal links and notification tap on cold/warm start.
3. Test location/photos/notifications denied and re-enabled from Settings.
4. Verify populated trips are not overwritten during create/edit/refresh/restore/leave flows.
5. Port or otherwise validate the full Android world city catalog.
6. Complete localization of existing iOS UI strings.
7. Add iOS tests and update `ios/IOS_RELEASE_BUGS.md` with observed results.
8. Resolve Apple provisioning, privacy, export compliance and App Store metadata blockers.
