package com.odyssey.travelplanner.ui.i18n

import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.runtime.setValue
import java.util.Locale

@Composable
internal fun localizedRestaurantNote(value: String): String {
    return when (value.trim().lowercase(Locale.ROOT)) {
        "пицца", "пиццерия" -> localized("Пицца", "Pizza", "Pizza", "Pizza")
        "рыба", "рыба и морепродукты", "морепродукты" -> localized("Рыба и морепродукты", "Seafood", "Mariscos", "Fisch & Meeresfrüchte")
        "итальянская", "итальянская кухня" -> localized("Итальянская кухня", "Italian cuisine", "Cocina italiana", "Italienische Küche")
        "европейская", "европейская кухня" -> localized("Европейская кухня", "European cuisine", "Cocina europea", "Europäische Küche")
        "бар" -> localized("Бар", "Bar", "Bar", "Bar")
        "кафе" -> localized("Кафе", "Cafe", "Café", "Café")
        "ресторан с террасой и видом на озеро альбано. хорош на день вылазки из рима." -> localized("Ресторан с террасой и видом на озеро Альбано. Хорош на день вылазки из Рима.", "Restaurant with a terrace overlooking Lake Albano, ideal for a day trip from Rome.", "Restaurante con terraza y vistas al lago Albano, ideal para una excursión de un día desde Roma.", "Restaurant mit Terrasse und Blick auf den Albaner See, ideal für einen Tagesausflug von Rom.")
        "простая траттория рядом с домом: миланские, тосканские и калабрийские блюда, большие порции." -> localized("Простая траттория рядом с домом: миланские, тосканские и калабрийские блюда, большие порции.", "Simple neighborhood trattoria serving Milanese, Tuscan, and Calabrian dishes in generous portions.", "Trattoria sencilla del barrio con platos milaneses, toscanos y calabreses en porciones generosas.", "Einfache Trattoria in der Nachbarschaft mit Mailänder, toskanischen und kalabrischen Gerichten und großen Portionen.")
        "slow food остерия в старом железнодорожном клубе; миланская классика — ризотто, котолетта." -> localized("Slow Food остерия в старом железнодорожном клубе; миланская классика — ризотто, котолетта.", "Slow Food osteria in a former railway club; Milanese classics include risotto and cotoletta.", "Osteria Slow Food en un antiguo club ferroviario; clásicos milaneses como risotto y cotoletta.", "Slow-Food-Osteria in einem ehemaligen Eisenbahnclub mit Mailänder Klassikern wie Risotto und Cotoletta.")
        "историческая траттория в центре с 1930-х: котолетта, ризотто по-милански, оссобуко." -> localized("Историческая траттория в центре с 1930-х: котолетта, ризотто по-милански, оссобуко.", "Historic trattoria in the center since the 1930s, serving cotoletta, Milanese risotto, and ossobuco.", "Trattoria histórica del centro desde los años 30, con cotoletta, risotto a la milanesa y ossobuco.", "Historische Trattoria im Zentrum seit den 1930er-Jahren mit Cotoletta, Mailänder Risotto und Ossobuco.")
        "классика миланской кухни у брера: оссобуко с ризотто, котолетта, кассоэла." -> localized("Классика миланской кухни у Брера: оссобуко с ризотто, котолетта, кассоэла.", "Milanese classics near Brera: ossobuco with risotto, cotoletta, and cassoeula.", "Clásicos de la cocina milanesa cerca de Brera: ossobuco con risotto, cotoletta y cassoeula.", "Mailänder Küche nahe Brera: Ossobuco mit Risotto, Cotoletta und Cassoeula.")
        "историческая семейная траттория (с 1921): образцовая миланская и ломбардская кухня." -> localized("Историческая семейная траттория (с 1921): образцовая миланская и ломбардская кухня.", "Historic family trattoria since 1921, known for classic Milanese and Lombard cuisine.", "Trattoria familiar histórica desde 1921, con cocina milanesa y lombarda ejemplar.", "Historische Familientrattoria seit 1921 mit klassischer Mailänder und lombardischer Küche.")
        "простая семейная траттория в читта-студи: домашняя паста и миланские блюда." -> localized("Простая семейная траттория в Читта-Студи: домашняя паста и миланские блюда.", "Simple family trattoria in Città Studi with homemade pasta and Milanese dishes.", "Trattoria familiar sencilla en Città Studi con pasta casera y platos milaneses.", "Einfache Familientrattoria in Città Studi mit hausgemachter Pasta und Mailänder Gerichten.")
        "знаменитая высокая миланская пицца al trancio; удобно взять кусок на прогулке." -> localized("Знаменитая высокая миланская пицца al trancio; удобно взять кусок на прогулке.", "Famous thick Milanese pizza al trancio; easy to grab a slice for a walk.", "Famosa pizza milanesa alta al trancio; ideal para llevar un trozo durante el paseo.", "Berühmte dicke Mailänder Pizza al trancio; ideal für ein Stück unterwegs.")
        "легендарные горячие панцеротти рядом с дуомо; возможна очередь, лучше взять навынос." -> localized("Легендарные горячие панцеротти рядом с Дуомо; возможна очередь, лучше взять навынос.", "Legendary hot panzerotti near the Duomo; there may be a queue, so takeaway is best.", "Legendarios panzerotti calientes cerca del Duomo; puede haber cola, mejor pedir para llevar.", "Legendäre heiße Panzerotti nahe dem Dom; es kann eine Warteschlange geben, am besten zum Mitnehmen.")
        "небольшая пиццерия у via torino: пицца, простое меню и быстрый обед в центре." -> localized("Небольшая пиццерия у Via Torino: пицца, простое меню и быстрый обед в центре.", "Small pizzeria near Via Torino with pizza, a simple menu, and a quick lunch in the center.", "Pequeña pizzería cerca de Via Torino con pizza, menú sencillo y almuerzo rápido en el centro.", "Kleine Pizzeria an der Via Torino mit Pizza, einfacher Karte und schnellem Mittagessen im Zentrum.")
        "китайские паровые пельмени на улице паоло сарпи: очень бюджетный перекус на ходу." -> localized("Китайские паровые пельмени на улице Паоло Сарпи: очень бюджетный перекус на ходу.", "Chinese steamed dumplings on Paolo Sarpi Street, a very affordable snack on the go.", "Dumplings chinos al vapor en la calle Paolo Sarpi, un tentempié muy económico para llevar.", "Chinesische Teigtaschen auf der Paolo-Sarpi-Straße, ein sehr günstiger Snack für unterwegs.")
        "неаполитанская пицца в нескольких минутах от дуомо; популярное место, в пиковые часы бывает очередь." -> localized("Неаполитанская пицца в нескольких минутах от Дуомо; популярное место, в пиковые часы бывает очередь.", "Neapolitan pizza a few minutes from the Duomo; popular, with queues at peak times.", "Pizza napolitana a pocos minutos del Duomo; lugar popular, con colas en horas punta.", "Neapolitanische Pizza wenige Minuten vom Dom entfernt; beliebter Ort, zu Stoßzeiten mit Warteschlange.")
        "классическая миланская траттория у навильи: ризотто, котолетта и домашняя атмосфера." -> localized("Классическая миланская траттория у Навильи: ризотто, котолетта и домашняя атмосфера.", "Classic Milanese trattoria by the Navigli: risotto, cotoletta, and a homely atmosphere.", "Trattoria clásica milanesa junto a Navigli: risotto, cotoletta y ambiente casero.", "Klassische Mailänder Trattoria an den Navigli mit Risotto, Cotoletta und familiärer Atmosphäre.")
        "рыбная кухня в кьодже, рядом с пляжем соттомарина." -> localized(
            "Рыбная кухня в Кьодже, рядом с пляжем Соттомарина.",
            "Seafood in Chioggia, near Sottomarina beach.",
            "Mariscos en Chioggia, cerca de la playa de Sottomarina.",
            "Fischküche in Chioggia, nahe dem Strand von Sottomarina.",
        )
        "местное пиво из деревянных бочек и классика кухни." -> localized(
            "Местное пиво из деревянных бочек и классика кухни.",
            "Local beer from wooden barrels and classic Bavarian dishes.",
            "Cerveza local de barriles de madera y clásicos de la cocina bávara.",
            "Lokales Bier aus Holzfässern und klassische bayerische Küche.",
        )
        "знаменит кнедлями и домашней баварской кухней." -> localized(
            "Знаменит кнедлями и домашней баварской кухней.",
            "Known for dumplings and homestyle Bavarian cuisine.",
            "Famoso por sus knödel y su cocina bávara casera.",
            "Bekannt für Knödel und hausgemachte bayerische Küche.",
        )
        "ресторан в подвале ратуши: удобен после прогулки по центру." -> localized(
            "Ресторан в подвале ратуши: удобен после прогулки по центру.",
            "Restaurant in the town hall cellar, convenient after a walk through the center.",
            "Restaurante en el sótano del ayuntamiento, práctico después de pasear por el centro.",
            "Restaurant im Rathauskeller, ideal nach einem Spaziergang durch die Innenstadt.",
        )
        "классическое заведение у оперы; лучше бронировать." -> localized(
            "Классическое заведение у оперы; лучше бронировать.",
            "Classic restaurant by the opera; reservations are recommended.",
            "Restaurante clásico junto a la ópera; se recomienda reservar.",
            "Klassisches Lokal an der Oper; Reservierung empfohlen.",
        )
        else -> value
    }
}

@Composable
internal fun localizedRestaurantCuisine(value: String): String {
    val tokens = value
        .split('·')
        .map { it.trim() }
        .filter(String::isNotBlank)
    if (tokens.isEmpty()) return ""

    val normalizedTokens = tokens.map { token ->
        token.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
    }
    val hasSpecificRestaurantType = normalizedTokens.any {
        it != "restaurant" && it.endsWith(" restaurant")
    }
    val visibleTokens = tokens.zip(normalizedTokens)
        .filterNot { (_, normalized) -> hasSpecificRestaurantType && normalized == "restaurant" }
        .map { (token, _) -> localizedRestaurantCuisineToken(token) }
        .distinct()
    return visibleTokens.joinToString(" · ")
}

@Composable
internal fun localizedRestaurantCuisineToken(value: String): String = when (
    value.lowercase(Locale.ROOT).replace(Regex("\\s+"), " ").trim()
) {
    "italian restaurant", "italian" -> localized("Итальянская кухня", "Italian cuisine", "Cocina italiana", "Italienische Küche")
    "fast food restaurant", "fast food" -> localized("Фастфуд", "Fast food", "Comida rápida", "Fast Food")
    "korean restaurant", "korean" -> localized("Корейская кухня", "Korean cuisine", "Cocina coreana", "Koreanische Küche")
    "japanese restaurant", "japanese" -> localized("Японская кухня", "Japanese cuisine", "Cocina japonesa", "Japanische Küche")
    "sushi restaurant", "sushi" -> localized("Суши", "Sushi", "Sushi", "Sushi")
    "chinese restaurant", "chinese" -> localized("Китайская кухня", "Chinese cuisine", "Cocina china", "Chinesische Küche")
    "indian restaurant", "indian" -> localized("Индийская кухня", "Indian cuisine", "Cocina india", "Indische Küche")
    "thai restaurant", "thai" -> localized("Тайская кухня", "Thai cuisine", "Cocina tailandesa", "Thailändische Küche")
    "french restaurant", "french" -> localized("Французская кухня", "French cuisine", "Cocina francesa", "Französische Küche")
    "german restaurant", "german" -> localized("Немецкая кухня", "German cuisine", "Cocina alemana", "Deutsche Küche")
    "bavarian restaurant", "bavarian" -> localized("Баварская кухня", "Bavarian cuisine", "Cocina bávara", "Bayerische Küche")
    "seafood restaurant", "seafood", "fish restaurant", "fish" -> localized("Рыба и морепродукты", "Seafood", "Mariscos", "Fisch und Meeresfrüchte")
    "pizza restaurant", "pizza", "pizzeria" -> localized("Пицца", "Pizza", "Pizza", "Pizza")
    "steak house", "steakhouse", "steak restaurant" -> localized("Стейки", "Steakhouse", "Parrilla", "Steakhaus")
    "barbecue restaurant", "barbecue", "bbq" -> localized("Барбекю", "Barbecue", "Barbacoa", "Barbecue")
    "vegetarian restaurant", "vegetarian" -> localized("Вегетарианская кухня", "Vegetarian cuisine", "Cocina vegetariana", "Vegetarische Küche")
    "vegan restaurant", "vegan" -> localized("Веганская кухня", "Vegan cuisine", "Cocina vegana", "Vegane Küche")
    "restaurant" -> localized("Ресторан", "Restaurant", "Restaurante", "Restaurant")
    "cafe", "café" -> localized("Кафе", "Café", "Café", "Café")
    "coffee shop", "coffee" -> localized("Кофейня", "Coffee shop", "Cafetería", "Café")
    "bar", "wine bar", "cocktail bar" -> localized("Бар", "Bar", "Bar", "Bar")
    "bakery" -> localized("Пекарня", "Bakery", "Panadería", "Bäckerei")
    "dessert shop", "dessert" -> localized("Десерты", "Desserts", "Postres", "Desserts")
    "meal takeaway", "takeaway" -> localized("Еда навынос", "Takeaway", "Comida para llevar", "Essen zum Mitnehmen")
    "food court" -> localized("Фуд-корт", "Food court", "Zona de restauración", "Food-Court")
    "regional" -> localized("Региональная кухня", "Regional cuisine", "Cocina regional", "Regionale Küche")
    else -> value
}

