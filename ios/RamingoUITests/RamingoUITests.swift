import XCTest

final class RamingoUITests: XCTestCase {
    private var app: XCUIApplication!

    override func setUpWithError() throws {
        continueAfterFailure = false
    }

    func testAuthenticatedTripSectionsAndFilters() throws {
        let accessToken = try requiredEnvironment("IOS_QA_ACCESS_TOKEN")
        let refreshToken = try requiredEnvironment("IOS_QA_REFRESH_TOKEN")
        let tripID = ProcessInfo.processInfo.environment["IOS_QA_TRIP_ID"] ?? ""

        app = XCUIApplication()
        app.launchArguments = [
            "-ramingo-qa-access-token", accessToken,
            "-ramingo-qa-refresh-token", refreshToken,
            "-ramingo-qa-trip-id", tripID,
        ]
        allowSystemPermissions()
        app.launch()

        XCTAssertTrue(app.staticTexts["Главная"].waitForExistence(timeout: 60), "Авторизованный экран поездки не открылся")
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
        element("trip.section.settings").tap()
        XCTAssertTrue(app.staticTexts["Настройки"].waitForExistence(timeout: 10), "Настройки не открылись из меню поездки")
        app.buttons["Закрыть"].firstMatch.tap()
    }

    func testCreateTripFormValidationWithoutSaving() throws {
        let accessToken = try requiredEnvironment("IOS_QA_ACCESS_TOKEN")
        let refreshToken = try requiredEnvironment("IOS_QA_REFRESH_TOKEN")
        let tripID = ProcessInfo.processInfo.environment["IOS_QA_TRIP_ID"] ?? ""

        app = XCUIApplication()
        app.launchArguments = [
            "-ramingo-qa-access-token", accessToken,
            "-ramingo-qa-refresh-token", refreshToken,
            "-ramingo-qa-trip-id", tripID,
        ]
        allowSystemPermissions()
        app.launch()

        XCTAssertTrue(app.staticTexts["Главная"].waitForExistence(timeout: 60), "Авторизованный экран поездки не открылся")
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

    private func requiredEnvironment(_ name: String) throws -> String {
        guard let value = ProcessInfo.processInfo.environment[name], !value.isEmpty else {
            throw XCTSkip("Не задана переменная \(name); live iOS UI smoke пропущен")
        }
        return value
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
