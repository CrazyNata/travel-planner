# Ramingo iOS

Отдельный нативный iOS-клиент Ramingo на SwiftUI. Эта папка не переиспользует исходники веб-версии и не изменяет Android-проект в `mobile/`.

## Что уже подготовлено

- SwiftUI-приложение с общей навигацией Ramingo.
- Email/password вход и регистрация через существующий Supabase Auth.
- Google OAuth через `ASWebAuthenticationSession` и PKCE.
- Хранение только auth-сессии в Keychain.
- Загрузка существующих поездок и их payload из таблицы `trips`.
- Список поездок, создание новой поездки и детальный экран.
- Разделы обзора, маршрута, достопримечательностей, ресторанов, жилья, бюджета, участников, фото и pet-friendly мест.
- Карта на MapKit без дополнительного SDK и ключа.
- Чтение приватных фотографий из существующего bucket `trip-photos` через signed URL.
- Deep link-схема `ramingo://auth-callback` для OAuth.

## Запуск на Mac

1. Установить Xcode 15+ на macOS.
2. Скопировать `Config/Secrets.xcconfig.example` в `Config/Secrets.local.xcconfig`.
3. Заполнить `SUPABASE_PUBLISHABLE_KEY`. URL проекта уже указан для текущего Supabase-проекта.
4. Открыть `Ramingo.xcodeproj` в Xcode.
5. Выбрать команду подписи и нужный iPhone Simulator или устройство.
6. В Supabase Auth → URL Configuration добавить redirect URL `ramingo://auth-callback`.

Для App Store потребуется собственный Bundle Identifier и Apple Developer Team. Локальный файл `Config/Secrets.local.xcconfig` не следует добавлять в Git.

## Сборка без Mac через GitHub Actions

Если Xcode нет, исходный код можно собрать на облачном macOS runner GitHub Actions. После отправки изменений в GitHub workflow `iOS build` автоматически проверит компиляцию и сохранит unsigned Simulator `.app` как artifact. Такой artifact предназначен для проверки сборки и не устанавливается на обычный iPhone.

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

## Ограничения текущего первого iOS-клиента

Сейчас реализован безопасный первый вертикальный срез: авторизация, создание поездки, загрузка данных и просмотр всех основных разделов. Сложные редакторы Android (перетаскивание, каталоги, расширенные формы, локальное планирование уведомлений) оставлены для следующих итераций после проверки базового iOS UX на реальном устройстве. База данных и её структура для этого модуля не изменялись.
