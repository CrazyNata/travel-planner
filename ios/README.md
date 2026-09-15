# Ramingo iOS

Отдельный нативный iOS-клиент Ramingo на SwiftUI. Эта папка не переиспользует исходники веб-версии и не изменяет Android-проект в `mobile/`.

## Что уже подготовлено

- SwiftUI-приложение с общей навигацией Ramingo.
- Email/password вход и регистрация через существующий Supabase Auth.
- Google OAuth через `ASWebAuthenticationSession` и PKCE.
- Нативный Sign in with Apple с nonce-защитой и сохранением имени при первом входе.
- Native recovery-flow для сброса пароля через `ramingo://auth-callback`.
- Хранение только auth-сессии в Keychain.
- Загрузка существующих поездок и их payload из таблицы `trips`.
- Список поездок, создание новой поездки и детальный экран.
- Разделы обзора, маршрута, достопримечательностей, ресторанов, жилья, бюджета, участников, фото и pet-friendly мест.
- Карта на MapKit без дополнительного SDK и ключа.
- Чтение приватных фотографий из существующего bucket `trip-photos` через signed URL.
- Редактирование основных данных поездки, маршрута, мест, ресторанов, жилья, бюджета, участников и pet-friendly мест.
- PhotosPicker для обложек, профиля и фотографий элементов поездки; порядок и удаление сохраняются в Supabase.
- Каталоги из существующих таблиц и `restaurant-enrichment`, погода Open-Meteo и справочные курсы Frankfurter.
- Локальные напоминания о поездках и дедлайнах отмены жилья с переходом в поездку по нажатию.
- Deep link-схема `ramingo://auth-callback` для OAuth.

## Запуск на Mac

1. Установить Xcode 26+ на macOS.
2. Скопировать `Config/Secrets.xcconfig.example` в `Config/Secrets.local.xcconfig`.
3. Заполнить `SUPABASE_PUBLISHABLE_KEY`. URL проекта уже указан для текущего Supabase-проекта.
4. Открыть `Ramingo.xcodeproj` в Xcode.
5. Выбрать команду подписи и нужный iPhone Simulator или устройство.
6. В Supabase Auth → URL Configuration добавить redirect URL `ramingo://auth-callback`.

Для App Store потребуется собственный Bundle Identifier и Apple Developer Team. Локальный файл `Config/Secrets.local.xcconfig` не следует добавлять в Git.

## Сборка без Mac через GitHub Actions

Жена может работать с кодом на Windows: сборка выполняется на macOS в GitHub Actions, а ручная проверка — на iPhone. Вспомогательный Linux-компьютер для этого не требуется.

| Workflow | Результат | Назначение |
| --- | --- | --- |
| `iOS build` | Неподписанный Simulator `.app` | Проверка компиляции; на iPhone этот файл не устанавливается |
| `iOS device IPA` | Неподписанный `Ramingo.ipa` для iPhone | Установка через Sideloadly на Windows с бесплатным Apple Account |
| `iOS TestFlight` | Подписанный архив и загрузка в TestFlight | Бета-тестирование через Apple Developer Program |

### Бесплатная тестовая IPA для Windows и iPhone

1. В [GitHub Actions](https://github.com/CrazyNata/travel-planner/actions/workflows/ios-device-build.yml) открыть **iOS device IPA → Run workflow**, выбрать `main` и запустить сборку.
2. После успешного завершения открыть запуск и скачать ZIP из раздела **Artifacts** с именем `ramingo-ios-device-…`.
3. Распаковать ZIP. Для установки нужен **`Ramingo.ipa`**; файл с информацией о сборке помогает определить коммит и версию.
4. На Windows установить [Sideloadly](https://sideloadly.io/), подключить разблокированный iPhone по USB и подтвердить доверие компьютеру. Если телефон не определяется, установить компоненты iTunes/iCloud по инструкции Sideloadly.
5. В Sideloadly выбрать iPhone и `Ramingo.ipa`, войти в свой Apple Account и выполнить установку. На телефоне при необходимости включить **Настройки → Конфиденциальность и безопасность → Режим разработчика** и доверие профилю в **Основные → VPN и управление устройством**.

Нужен iPhone с iOS 17 или новее. Бесплатная подпись действует 7 дней; Sideloadly позволяет обновлять её. Для обновления приложения используйте прежний Apple Account и тот же Bundle ID. Инструкция и ограничения: [Sideloadly FAQ](https://sideloadly.io/faq).

Эта сборка не требует Apple Developer Program, App Store Connect или ключей Apple на GitHub. Сам IPA до установки не подписан: подпись создаёт Sideloadly на компьютере владельца. Apple Account вводится в Sideloadly, а не в репозиторий или чат.

Workflow использует существующий GitHub Secret `SUPABASE_PUBLISHABLE_KEY`. URL берётся из Secret `SUPABASE_URL`, затем Repository variable `VITE_SUPABASE_URL`, затем из адреса текущего проекта. Без действительного клиентского ключа сборка завершится ошибкой. В приложение встраивается только publishable/anon key; service-role и другие серверные ключи использовать нельзя. Аккаунты и данные общие с Android и вебом.

В тестовой конфигурации `SIDELOAD_BUILD` отключены кнопка Sign in with Apple и соответствующий entitlement: бесплатная подпись не поддерживает эту возможность. Вход по почте и Google остаётся доступен. Обычная конфигурация и TestFlight сохраняют Sign in with Apple. [Ограничения Apple](https://developer.apple.com/help/account/reference/supported-capabilities-ios/).

Сборка и проверка содержимого IPA ещё не подтверждают её установку, вход или работу экранов на реальном iPhone. После установки проверьте вход по почте, загрузку поездок и создание отдельной тестовой поездки, затем фотографии, карты и уведомления.

### TestFlight

Для TestFlight запустите workflow `iOS TestFlight` вручную в GitHub Actions. Он собирает Release-архив, подписывает его автоматическим provisioning и отправляет `.ipa` в TestFlight. Для этого нужны Apple Developer Program, созданное приложение с Bundle ID `com.odyssey.ramingo.ios` и App Store Connect API key.

В настройках репозитория GitHub добавьте Variables:

- `APPSTORE_ISSUER_ID` — Issuer ID из App Store Connect.
- `APPSTORE_API_KEY_ID` — Key ID API key.
- `IOS_BUNDLE_ID` — Bundle ID приложения; по умолчанию workflow использует `com.odyssey.ramingo.ios`.

Добавьте Secrets:

- `APPLE_TEAM_ID` — Team ID Apple Developer.
- `APPSTORE_API_PRIVATE_KEY` — полное содержимое файла `AuthKey_<KEY_ID>.p8`.
- `SUPABASE_PUBLISHABLE_KEY` — publishable key существующего Supabase-проекта.
- `SUPABASE_URL` — URL существующего Supabase-проекта; если не добавить, используется текущий URL проекта.

Секреты не нужно добавлять в репозиторий. Новые workflow затрагивают только iOS-сборку; существующие web- и Android-workflow не изменяются.

## Что нужно проверить на iPhone

После установки IPA или TestFlight-сборки проверить recovery/deep link, PhotosPicker, карты и уведомления на отдельной тестовой поездке. Sign in with Apple проверяется только в сборке с соответствующей платной подписью. Удаление аккаунта проверять только на специально созданном тестовом аккаунте после явного подтверждения владельца. Изменения схемы Supabase для сборки не нужны.
