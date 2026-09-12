# Production access questionnaire — честный черновик

Статус: не отправлять как готовый ответ. Релиз `0.2.60` (version code `10069`) находится в закрытом тестировании; фактические даты, состав тестеров и метрики нужно взять из Play Console перед заполнением формы.

PDF с вопросами содержит шаблонные ответы. Ниже они превращены в проверяемый черновик без утверждений, которые нельзя подтвердить по репозиторию или Play Console.

## Ответы

### 1. How did you recruit testers?

Черновик: `We recruited closed-test participants who represent people planning solo and group trips. [VERIFY the actual recruitment channel and whether a paid testing provider was used.]`

Проверить: канал набора, платный/бесплатный набор, даты и количество участников.

### 2. How easy or difficult was it to recruit testers?

Черновик: `Recruitment was [EASY / MODERATE / DIFFICULT] because [ADD the concrete reason and evidence].`

Проверить: фактическое количество приглашённых и принявших тестеров.

### 3. How did testers engage with your app?

Черновик: `Testers were asked to create or edit a trip, add cities and dates, review the route and weather, and use the main trip-planning sections. We reviewed whether these core flows could be completed and recorded the issues and requests reported by testers.`

Проверить: какие сценарии действительно прошли тестеры, а также ссылки на feedback/заметки.

### 4. What feedback did you receive?

Черновик: `Feedback covered onboarding, clarity of the first-trip flow, route planning, weather, theme settings, and account support actions. We used it to verify the first-run walkthrough and shared trip-weather behavior. [ADD the actual feedback channels and summarize recurring reports.]`

Проверить: канал feedback, количество сообщений, повторяющиеся проблемы и что исправлено в каком релизе.

### 5. Who is your app intended for?

Черновик: `Ramingo is intended for travelers planning solo trips or trips with friends and other collaborators. It helps them keep routes, cities, dates, places, accommodation, expenses, photos, notes, and weather in one trip plan.`

### 6. How does your app provide value to users?

Черновик: `The app turns a collection of travel details into a shared, editable itinerary. Users can organize a route by day and city, save places and photos, track expenses, keep notes, review accommodation details, and return to the plan through their account.`

### 7. What is the expected install volume?

Черновик: `Our initial estimate is [VERIFY RANGE].`

Не копировать диапазон `10,000–100,000` из шаблона без бизнес-обоснования и данных.

### 8. What changes did you make as a result of testing?

Черновик: `We improved the first-run onboarding so a user with no trips is guided directly into creating the first trip, kept the tutorial available from the profile, and aligned Android weather selection with the shared web trip cities. We also provide system-theme selection, in-app feedback, and a rate-app action.`

Проверить: какие из этих изменений были доступны тестерам и какие даты/версии указать.

### 9. Why are you ready for production?

Черновик: `We will request production access after the required closed-testing period. Before submitting, we will verify the core sign-up, first-trip creation, shared-trip, weather, account, feedback, and deletion flows on the target devices, and review all tester feedback and Play Console vitals.`

Не утверждать отсутствие критических ошибок без фактической истории теста и Android Vitals.

### 10. What makes your app different?

Черновик: `Ramingo combines collaborative itinerary planning with route-by-day organization, places, accommodation, expenses, notes, photos, and weather in one trip workspace. The first-run walkthrough explains the product before the user creates their first trip.`

## Данные, которые нужно собрать перед отправкой

- [ ] даты начала и окончания закрытого теста;
- [ ] число приглашённых, принявших участие и активных тестеров;
- [ ] список реальных моделей устройств, версий Android и языков;
- [ ] сценарии, которые тестеры прошли;
- [ ] каналы обратной связи и число полученных сообщений;
- [ ] повторяющиеся проблемы и соответствующие исправления;
- [ ] Android Vitals/стабильность за период теста;
- [ ] итоговая карточка Play Store, privacy policy, Data safety и account deletion;
- [ ] подтверждение целевого диапазона установок.
