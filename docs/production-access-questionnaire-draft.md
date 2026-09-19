# Production access questionnaire — честный черновик

Статус: рабочий черновик, не отправлять без финальной проверки. Релиз `0.2.72` (version code `10210`) отправлен в закрытое тестирование; при последней проверке Play Console показывал 12 участников и 13 дней непрерывного тестирования. Заявка на production access пока заблокирована до выполнения 14 дней.

PDF с вопросами содержит шаблонные ответы. Ниже они превращены в проверяемый черновик без утверждений, которые нельзя подтвердить по репозиторию или Play Console.

## Ответы

### 1. How did you recruit testers?

Черновик: `We recruited testers through our personal network of friends and acquaintances. We invited people who were willing to install the app, join the closed test and evaluate the app from a user's perspective.`

Проверить: точные даты набора и число приглашённых участников. Не добавлять неподтверждённые сведения о платном наборе.

### 2. How easy or difficult was it to recruit testers?

Черновик: `Recruitment was difficult. It required personal communication and follow-up because testers needed to install the app, opt in to the closed test and remain participants for the required period. We reached the required number of testers through our personal network.`

Проверить: фактическое количество приглашённых и принявших тестеров.

### 3. How did testers engage with your app?

Черновик: `Testers used the app to check its main functionality and ease of use. They navigated through the main screens, reviewed the trip-planning flow and evaluated whether the interface and actions were clear. We recorded the issues and requests reported by testers and used them to prioritise improvements.`

Проверить: при необходимости добавить конкретные сценарии, которые каждый тестер действительно прошёл.

### 4. What feedback did you receive?

Черновик: `We received useful feedback throughout the testing period. Testers sent comments through messaging apps and email. The main reports concerned dark-theme behaviour, Google authentication, unexpected sign-outs while creating a trip without a clear explanation, inconsistent weather display, maps not being generated correctly, inability to reorder attractions, and inability to delete a trip. We reviewed these reports and used them to prioritise fixes and improve reliability.`

Проверить: точное количество сообщений, если оно известно, и отметить, какие проблемы были закрыты в текущем релизе.

### 5. Who is your app intended for?

Черновик: `Ramingo is intended for travelers planning solo trips or trips with friends and other collaborators. It helps them keep routes, cities, dates, places, accommodation, expenses, photos, notes, and weather in one trip plan.`

### 6. How does your app provide value to users?

Черновик: `The app turns a collection of travel details into a shared, editable itinerary. Users can organize a route by day and city, save places and photos, track expenses, keep notes, review accommodation details, and return to the plan through their account.`

### 7. What is the expected install volume?

Черновик: `Our initial estimate is [VERIFY RANGE].`

Не копировать диапазон `10,000–100,000` из шаблона без бизнес-обоснования и данных.

### 8. What changes did you make as a result of testing?

Черновик: `During the testing and improvement cycle, we introduced and refined the first-time onboarding flow, improved Android sign-in and session persistence, fixed dark-theme behaviour, improved weather loading and selection for trip dates, improved map and route handling, added recoverable trip deletion, and added mobile reordering for route items. We also improved notification delivery and settings, accessibility labels and the Android trip-planning flows. The current release includes the verified fix for displaying weather for each trip day and navigation for longer trips.`

Проверить: финальная версия каждого исправления на текущем Play-сборке перед отправкой production access.

### 9. Why are you ready for production?

Черновик: `The closed test helped us identify and prioritise issues with authentication, session handling, themes, weather, maps, trip deletion and attraction ordering. We addressed these issues in successive test builds, ran Android unit tests and build verification, and sent the current signed build to Google Play closed testing. Before applying for production access, we will complete a final smoke check of Google sign-in, session persistence, dark theme, maps, weather, trip deletion and reordering on the current Play build, and review the available tester feedback and Play Console stability information.`

Не отправлять этот ответ, пока финальная smoke-проверка не подтвердит перечисленные core flows.

### 10. What makes your app different?

Черновик: `Ramingo combines collaborative itinerary planning with route-by-day organisation, places, accommodation, expenses, notes, photos and weather in one trip workspace. The first-run onboarding explains the main product flows and helps a new user understand how to plan a trip.`

## Данные, которые нужно собрать перед отправкой

На 15 сентября 2026 уже подтверждено в Play Console: 12 участников закрытого теста и 10 дней непрерывного тестирования. Остальные поля ниже нельзя заполнять предположениями.

- [ ] даты начала и окончания закрытого теста;
- [ ] число приглашённых, принявших участие и активных тестеров;
- [ ] список реальных моделей устройств, версий Android и языков;
- [ ] сценарии, которые тестеры прошли;
- [ ] каналы обратной связи и число полученных сообщений;
- [ ] повторяющиеся проблемы и соответствующие исправления;
- [ ] Android Vitals/стабильность за период теста;
- [ ] итоговая карточка Play Store, privacy policy, Data safety и account deletion;
- [ ] подтверждение целевого диапазона установок.
