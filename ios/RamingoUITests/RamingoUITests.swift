import XCTest

final class RamingoUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testAuthenticatedTripSectionsAndFilters() throws {
        launchAuthorizedTrip()
        XCTAssertTrue(element("trip.drawer").waitForExistence(timeout: 10), "Не найдено меню поездки")

        element("trip.drawer").tap()
        for section in ["route", "sights", "restaurants", "accommodation", "pets", "budget", "members", "photos"] {
            XCTAssertTrue(element("trip.section.\(section)").waitForExistence(timeout: 5), "В меню нет раздела \(section)")
        }

        element("trip.section.route").tap()
        XCTAssertTrue(app.staticTexts["Маршрут"].waitForExistence(timeout: 15), "Раздел маршрута не открылся")
        XCTAssertTrue(element("route.add").waitForExistence(timeout: 10), "На маршруте нет действия добавления переезда")
        element("route.add").tap()
        XCTAssertTrue(app.staticTexts["Добавить переезд"].waitForExistence(timeout: 5), "Форма добавления переезда не открылась")
        app.buttons["Отмена"].firstMatch.tap()

        if element("route.leg.edit").waitForExistence(timeout: 5) {
            element("route.leg.edit").tap()
            XCTAssertTrue(app.staticTexts["Изменить переезд"].waitForExistence(timeout: 5), "Редактор переезда не открылся")
            app.buttons["Отмена"].firstMatch.tap()
        }

        element("trip.drawer").tap()
        element("trip.section.restaurants").tap()
        XCTAssertTrue(app.staticTexts["Рестораны"].waitForExistence(timeout: 15), "Раздел ресторанов не открылся")
        XCTAssertTrue(element("restaurants.filters").waitForExistence(timeout: 10), "Фильтры ресторанов не отображаются")
        element("restaurants.filters").tap()
        XCTAssertTrue(app.staticTexts["Фильтры"].waitForExistence(timeout: 5), "Лист фильтров ресторанов не открылся")
        if app.buttons["Бар"].waitForExistence(timeout: 3) {
            app.buttons["Бар"].tap()
        }
        XCTAssertTrue(app.buttons["Показать результаты"].exists, "В фильтрах нет применения результата")
        app.buttons["Показать результаты"].tap()
        XCTAssertTrue(element("restaurants.filters").waitForExistence(timeout: 5), "После применения фильтр закрыл раздел")

        element("trip.drawer").tap()
        element("trip.settings").tap()
        XCTAssertTrue(app.staticTexts["Настройки"].waitForExistence(timeout: 10), "Настройки не открылись из меню поездки")
        app.buttons["Закрыть"].firstMatch.tap()
    }

    func testAuthenticatedTripAllSectionsAndReadOnlyEditors() throws {
        launchAuthorizedTrip()

        XCTAssertTrue(app.staticTexts["Погода по маршруту"].waitForExistence(timeout: 15), "Главный экран поездки не показал блок погоды")
        if app.buttons["На даты поездки"].waitForExistence(timeout: 5) {
            app.buttons["На даты поездки"].tap()
            XCTAssertTrue(app.staticTexts["Прогноз на даты поездки"].waitForExistence(timeout: 5), "Переключатель прогноза на даты не сработал")
            app.buttons["Сейчас"].tap()
            XCTAssertTrue(app.staticTexts["Текущая погода для городов маршрута"].waitForExistence(timeout: 5), "Переключатель текущей погоды не сработал")
        }

        if app.buttons["Редактировать главный экран"].waitForExistence(timeout: 5) {
            app.buttons["Редактировать главный экран"].tap()
            XCTAssertTrue(app.staticTexts["Зажмите блок за ⋮⋮ и перенесите его"].waitForExistence(timeout: 5), "Режим редактирования главного экрана не открылся")
            if app.buttons["Изменить карту"].waitForExistence(timeout: 3) {
                app.buttons["Изменить карту"].tap()
                XCTAssertTrue(app.staticTexts["Создать карту"].waitForExistence(timeout: 5), "Редактор карты не открылся")
                app.buttons["Отмена"].firstMatch.tap()
            }
            if app.buttons["Настроить погоду"].waitForExistence(timeout: 3) {
                app.buttons["Настроить погоду"].tap()
                XCTAssertTrue(app.staticTexts["Погода по городам"].waitForExistence(timeout: 5), "Редактор городов погоды не открылся")
                app.buttons["Отмена"].firstMatch.tap()
            }
            app.buttons["Завершить редактирование главного экрана"].tap()
        }

        openTripSection("sights", title: "Достопримечательности")
        XCTAssertTrue(app.buttons["Копировать"].waitForExistence(timeout: 10), "На экране достопримечательностей нет копирования маршрута")
        app.buttons["Копировать"].tap()
        XCTAssertTrue(app.buttons["Скопировано"].waitForExistence(timeout: 5), "Копирование маршрута достопримечательностей не сработало")

        openTripSection("restaurants", title: "Рестораны")
        XCTAssertTrue(element("restaurants.filters").waitForExistence(timeout: 10), "В разделе ресторанов нет фильтров")
        element("restaurants.filters").tap()
        XCTAssertTrue(app.staticTexts["Фильтры"].waitForExistence(timeout: 5), "Фильтры ресторанов не открылись")
        tapIfPresent("Бар")
        tapIfPresent("С собакой")
        tapIfPresent("4.5+")
        XCTAssertTrue(app.buttons["Показать результаты"].waitForExistence(timeout: 5), "В фильтрах ресторанов нет применения")
        app.buttons["Показать результаты"].tap()
        XCTAssertTrue(element("restaurants.filters").waitForExistence(timeout: 5), "После фильтрации ресторанов раздел закрылся")
        if tapIfPresent("Добавить ресторан", timeout: 5) {
            XCTAssertTrue(app.staticTexts["Редактировать поездку"].waitForExistence(timeout: 10), "Редактор ресторанов не открылся")
            app.buttons["Готово"].firstMatch.tap()
            XCTAssertTrue(app.staticTexts["Рестораны"].waitForExistence(timeout: 10), "Редактор ресторанов не закрылся без сохранения")
        }

        openTripSection("accommodation", title: "Жильё")
        XCTAssertTrue(tapIfPresent("Добавить жильё", timeout: 5), "В разделе жилья нет добавления")
        XCTAssertTrue(app.staticTexts["Выберите способ добавления"].waitForExistence(timeout: 5), "Выбор способа добавления жилья не открылся")
        XCTAssertTrue(tapIfPresentContaining("Найти в каталоге", timeout: 3), "В жилье нет добавления из каталога")
        XCTAssertTrue(app.staticTexts["Жильё"].waitForExistence(timeout: 10), "Каталог жилья не открылся")
        XCTAssertTrue(tapIfPresent("Закрыть", timeout: 5), "Каталог жилья нельзя закрыть")
        XCTAssertTrue(tapIfPresent("Добавить жильё", timeout: 5), "Повторное добавление жилья недоступно")
        XCTAssertTrue(tapIfPresentContaining("Добавить вручную", timeout: 3), "В жилье нет ручного добавления")
        XCTAssertTrue(app.staticTexts["Редактировать поездку"].waitForExistence(timeout: 10), "Ручной редактор жилья не открылся")
        app.buttons["Готово"].firstMatch.tap()
        XCTAssertTrue(app.staticTexts["Жильё"].waitForExistence(timeout: 10), "Ручной редактор жилья не закрылся")

        openTripSection("pets", title: "Питомцы")
        XCTAssertTrue(app.buttons["Зоомагазины"].waitForExistence(timeout: 10), "На экране питомцев нет вкладки зоомагазинов")
        XCTAssertTrue(app.buttons["Ветеринары"].exists, "На экране питомцев нет вкладки ветеринаров")
        app.buttons["Ветеринары"].tap()
        XCTAssertTrue(app.buttons["Ветеринары"].exists, "Переключение на ветеринаров не доступно")
        let petFilters = buttonContaining("Фильтры")
        XCTAssertTrue(petFilters.waitForExistence(timeout: 5), "В разделе питомцев нет фильтров")
        petFilters.tap()
        XCTAssertTrue(app.staticTexts["Фильтры"].waitForExistence(timeout: 5), "Фильтры питомцев не открылись")
        XCTAssertTrue(tapIfPresent("Круглосуточно"), "В фильтрах ветеринаров нет опции круглосуточной работы")
        XCTAssertTrue(tapIfPresent("★ 4.5"), "В фильтрах питомцев нет рейтинга")
        XCTAssertTrue(app.buttons["Показать места"].waitForExistence(timeout: 5), "В фильтрах питомцев нет применения")
        app.buttons["Показать места"].tap()

        openTripSection("budget", title: "Бюджет")
        XCTAssertTrue(app.staticTexts["ОБЩАЯ СУММА"].waitForExistence(timeout: 10), "Бюджет не показал общую сумму")
        XCTAssertTrue(app.staticTexts["Курс валюты"].waitForExistence(timeout: 10), "Бюджет не показал курс валюты")
        let rateEditor = app.buttons["Изменить"].firstMatch
        if rateEditor.waitForExistence(timeout: 5), rateEditor.isEnabled {
            rateEditor.tap()
            XCTAssertTrue(app.staticTexts["Изменить курс"].waitForExistence(timeout: 5), "Редактор курса валюты не открылся")
            app.buttons["Отмена"].firstMatch.tap()
        }

        openTripSection("members", title: "Участники")
        if tapIfPresent("Изменить", timeout: 5) {
            XCTAssertTrue(app.staticTexts["Редактировать поездку"].waitForExistence(timeout: 10), "Редактор участников не открылся")
            app.buttons["Готово"].firstMatch.tap()
            XCTAssertTrue(app.staticTexts["Участники"].waitForExistence(timeout: 10), "Редактор участников не закрылся")
        }
        if tapIfPresent("Покинуть поездку", timeout: 3) {
            XCTAssertTrue(app.staticTexts["Покинуть поездку?"].waitForExistence(timeout: 5), "Подтверждение выхода из поездки не открылось")
            app.buttons["Отмена"].firstMatch.tap()
        }

        openTripSection("photos", title: "Фото")
        XCTAssertTrue(app.staticTexts["Фотографии"].waitForExistence(timeout: 10), "Раздел фотографий не показал заголовок")
        XCTAssertTrue(app.buttons["Загрузить"].waitForExistence(timeout: 5), "В разделе фотографий нет загрузки")
    }

    func testAuthenticatedHomeFiltersAndSettingsSurfaces() throws {
        launchAuthorizedTrip()
        element("trip.drawer").tap()
        element("trip.home").tap()
        XCTAssertTrue(app.staticTexts["Мои путешествия"].waitForExistence(timeout: 15), "Список поездок не открылся")

        for prefix in ["Все ·", "Предстоящие ·", "Черновики ·", "Завершённые ·"] {
            let filter = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", prefix)).firstMatch
            XCTAssertTrue(filter.waitForExistence(timeout: 5), "На главном экране нет фильтра \(prefix)")
            filter.tap()
        }

        let settingsButton = app.buttons["Открыть настройки"].firstMatch
        XCTAssertTrue(settingsButton.waitForExistence(timeout: 5), "На главном экране нет перехода в настройки")
        settingsButton.tap()
        XCTAssertTrue(app.staticTexts["Настройки"].waitForExistence(timeout: 10), "Настройки из списка поездок не открылись")

        XCTAssertTrue(tapIfPresentContaining("Тема", timeout: 5), "В настройках нет выбора темы")
        XCTAssertTrue(app.staticTexts["Светлая"].waitForExistence(timeout: 5), "Выбор темы не открылся")
        XCTAssertTrue(tapIfPresentContaining("Тема", timeout: 5), "Выбор темы нельзя закрыть")
        XCTAssertTrue(tapIfPresentContaining("Языки", timeout: 5), "В настройках нет выбора языка")
        XCTAssertTrue(app.buttons["settings.language.EN"].waitForExistence(timeout: 5), "Выбор языка не открылся")
        XCTAssertTrue(app.buttons["settings.language.RU"].waitForExistence(timeout: 5), "В меню языка нет русского")
        app.buttons["settings.language.RU"].tap()

        let notificationsButton = app.buttons.matching(NSPredicate(format: "label BEGINSWITH %@", "Уведомления")).firstMatch
        XCTAssertTrue(notificationsButton.waitForExistence(timeout: 5), "В настройках нет уведомлений")
        notificationsButton.tap()
        XCTAssertTrue(app.staticTexts["Уведомления"].waitForExistence(timeout: 10), "Настройки уведомлений не открылись")
        let notificationCloseButton = app.buttons["notifications.close"]
        if !notificationCloseButton.waitForExistence(timeout: 2) {
            let closeButtons = app.buttons.matching(NSPredicate(format: "label == %@", "Закрыть"))
            XCTAssertTrue(closeButtons.lastMatch.waitForExistence(timeout: 5), "Экран уведомлений не содержит кнопки закрытия")
            closeButtons.lastMatch.tap()
        } else {
            notificationCloseButton.tap()
        }

        app.buttons["Изменить пароль"].tap()
        XCTAssertTrue(app.staticTexts["Изменить пароль"].waitForExistence(timeout: 10), "Форма смены пароля не открылась")
        app.buttons["Закрыть"].firstMatch.tap()

        app.buttons["Удалить аккаунт"].tap()
        XCTAssertTrue(app.staticTexts["Удалить аккаунт?"].waitForExistence(timeout: 5), "Подтверждение удаления аккаунта не открылось")
        app.buttons["Отмена"].firstMatch.tap()
        app.buttons["Закрыть"].firstMatch.tap()
    }

    func testUnauthenticatedAuthAndRecoverySurfaces() throws {
        app = XCUIApplication()
        app.launchArguments = ["-ramingo-qa-skip-session"]
        allowSystemPermissions()
        app.launch()

        XCTAssertTrue(app.staticTexts["С возвращением"].waitForExistence(timeout: 30), "Экран входа не открылся без сессии")
        XCTAssertTrue(app.textFields.firstMatch.waitForExistence(timeout: 5), "На экране входа нет e-mail поля")
        XCTAssertTrue(app.secureTextFields.firstMatch.waitForExistence(timeout: 5), "На экране входа нет поля пароля")

        app.buttons["Забыли пароль?"].tap()
        XCTAssertTrue(app.staticTexts["Сброс пароля"].waitForExistence(timeout: 10), "Форма восстановления пароля не открылась")
        app.buttons["Закрыть"].firstMatch.tap()

        XCTAssertTrue(buttonContaining("Зарегистрироваться").waitForExistence(timeout: 5), "На экране входа нет перехода к регистрации")
        buttonContaining("Зарегистрироваться").tap()
        XCTAssertTrue(app.staticTexts["Создать аккаунт"].waitForExistence(timeout: 5), "Переключение на регистрацию не сработало")
        buttonContaining("Войти").tap()
        XCTAssertTrue(app.staticTexts["С возвращением"].waitForExistence(timeout: 5), "Возврат к форме входа не сработал")
    }

    func testCreateTripFormValidationWithoutSaving() throws {
        let tripID = launchAuthorizedTrip()
        _ = tripID
        element("trip.drawer").tap()
        element("trip.home").tap()
        XCTAssertTrue(app.staticTexts["Мои путешествия"].waitForExistence(timeout: 15), "Список поездок не открылся")

        let createButton = element("home.createTrip")
        reveal(createButton)
        XCTAssertTrue(createButton.waitForExistence(timeout: 10), "Кнопка новой поездки не найдена")
        createButton.tap()

        let title = app.textFields["createTrip.Название"]
        let city = app.textFields["createTrip.cities"]
        let start = app.textFields["createTrip.Начало"]
        let end = app.textFields["createTrip.Окончание"]
        let save = element("createTrip.save")
        XCTAssertTrue(title.waitForExistence(timeout: 10), "Поле названия поездки не найдено")
        XCTAssertTrue(city.exists, "Поле городов поездки не найдено")
        XCTAssertTrue(start.exists && end.exists, "Поля дат поездки не найдены")
        XCTAssertTrue(save.exists && !save.isEnabled, "Пустая форма не должна позволять сохранить поездку")

        title.tap()
        title.typeText("iOS UI QA draft")
        start.tap()
        start.typeText("2031-01-01")
        end.tap()
        end.typeText("2031-01-03")
        city.tap()
        city.typeText("Прага\n")
        XCTAssertTrue(save.waitForExistence(timeout: 5) && save.isEnabled, "Заполненная обязательная форма не активирует сохранение")

        app.buttons["Отмена"].firstMatch.tap()
        XCTAssertTrue(app.staticTexts["Мои путешествия"].waitForExistence(timeout: 10), "Форма не закрылась без сохранения")
    }

    @discardableResult
    private func launchAuthorizedTrip() -> String {
        let tripID = optionalTestValue("IOS_QA_TRIP_ID") ?? ""
        XCTAssertNotNil(qaSession(), "Сессия iOS UI-теста не передана в тестовый bundle")

        app = XCUIApplication()
        app.launchArguments = qaLaunchArguments(tripID: tripID)
        allowSystemPermissions()
        app.launch()

        XCTAssertTrue(app.staticTexts["Главная"].waitForExistence(timeout: 60), "Авторизованный экран поездки не открылся")
        return tripID
    }

    private func openTripSection(_ identifier: String, title: String) {
        XCTAssertTrue(element("trip.drawer").waitForExistence(timeout: 10), "Не найдено меню поездки перед открытием \(identifier)")
        element("trip.drawer").tap()
        let row = element("trip.section.\(identifier)")
        XCTAssertTrue(row.waitForExistence(timeout: 5), "В меню нет раздела \(identifier)")
        row.tap()
        XCTAssertTrue(app.staticTexts[title].waitForExistence(timeout: 15), "Раздел \(title) не открылся")
    }

    @discardableResult
    private func tapIfPresent(_ title: String, timeout: TimeInterval = 2) -> Bool {
        let button = app.buttons[title].firstMatch
        guard button.waitForExistence(timeout: timeout) else { return false }
        button.tap()
        return true
    }

    @discardableResult
    private func tapIfPresentContaining(_ text: String, timeout: TimeInterval = 2) -> Bool {
        let button = buttonContaining(text)
        guard button.waitForExistence(timeout: timeout) else { return false }
        button.tap()
        return true
    }

    private func buttonContaining(_ text: String) -> XCUIElement {
        app.buttons.matching(NSPredicate(format: "label CONTAINS %@", text)).firstMatch
    }

    private func optionalTestValue(_ name: String) -> String? {
        if let value = ProcessInfo.processInfo.environment[name], !value.isEmpty {
            return value
        }

        for bundle in [Bundle(for: RamingoUITests.self), Bundle.main] {
            if let value = bundle.object(forInfoDictionaryKey: name) as? String, !value.isEmpty {
                return value
            }
        }

        let resourceName: String
        let resourceExtension: String
        switch name {
        case "IOS_QA_SESSION_FILE":
            resourceName = "ramingo-qa-session"
            resourceExtension = "json"
        case "IOS_QA_TRIP_ID":
            resourceName = "ramingo-qa-trip-id"
            resourceExtension = "txt"
        default:
            return nil
        }

        if let resourceURL = Bundle(for: RamingoUITests.self).url(
            forResource: resourceName,
            withExtension: resourceExtension
        ),
           let value = try? String(contentsOf: resourceURL, encoding: .utf8),
           !value.isEmpty {
            return value.trimmingCharacters(in: .whitespacesAndNewlines)
        }
        return nil
    }

    private func qaLaunchArguments(tripID: String) -> [String] {
        var arguments: [String] = []
        if !tripID.isEmpty {
            arguments += ["-ramingo-qa-trip-id", tripID]
        }
        if let session = qaSession(),
           !session.accessToken.isEmpty,
           !session.refreshToken.isEmpty {
            arguments += [
                "-ramingo-qa-access-token", session.accessToken,
                "-ramingo-qa-refresh-token", session.refreshToken,
            ]
        }
        return arguments
    }

    private func qaSession() -> QASession? {
        guard let path = optionalTestValue("IOS_QA_SESSION_FILE") else { return nil }
        let data: Data?
        if path.hasPrefix("/") {
            data = try? Data(contentsOf: URL(fileURLWithPath: path))
        } else {
            data = path.data(using: .utf8)
        }

        guard let data else { return nil }
        return try? JSONDecoder().decode(QASession.self, from: data)
    }

    private func element(_ identifier: String) -> XCUIElement {
        app.descendants(matching: .any).matching(identifier: identifier).firstMatch
    }

    private func reveal(_ target: XCUIElement) {
        let scrollView = app.scrollViews.firstMatch
        for _ in 0..<8 where !target.isHittable {
            scrollView.swipeUp()
        }
    }

    private func allowSystemPermissions() {
        addUIInterruptionMonitor(withDescription: "System permissions") { alert in
            for title in ["Allow While Using App", "Allow Once", "Разрешить при использовании приложения", "Разрешить один раз"] {
                let button = alert.buttons[title]
                if button.exists {
                    button.tap()
                    return true
                }
            }
            return false
        }
    }
}

private struct QASession: Decodable {
    let accessToken: String
    let refreshToken: String
}
