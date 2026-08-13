package com.odyssey.travelplanner.data

import java.util.Locale

data class CityLocation(
    val latitude: Double,
    val longitude: Double,
)

data class CityCatalogEntry(
    val key: String,
    val russian: String,
    val english: String,
    val spanish: String,
    val german: String,
    val latitude: Double,
    val longitude: Double,
    val aliases: Set<String> = emptySet(),
    val countryName: String = "",
    val countryCode: String = "",
    val population: Long = 0L,
) {
    fun localized(language: String): String = when (language.trim().uppercase(Locale.ROOT).substringBefore('-')) {
        "EN" -> english
        "ES" -> spanish
        "DE" -> german
        else -> russian
    }

    fun selectionValue(language: String): String {
        val name = localized(language)
        return if (countryName.isBlank()) name else "$name — $countryName"
    }
}

internal fun normalizeCityAlias(value: String): String = value
    .trim()
    .lowercase(Locale.ROOT)
    .replace('ё', 'е')
    .replace(Regex("[‐‑‒–—−]"), "-")
    .replace(Regex("\\s+"), " ")

private fun city(
    key: String,
    russian: String,
    english: String,
    spanish: String,
    german: String,
    latitude: Double,
    longitude: Double,
    vararg aliases: String,
) = CityCatalogEntry(
    key = key,
    russian = russian,
    english = english,
    spanish = spanish,
    german = german,
    latitude = latitude,
    longitude = longitude,
    aliases = (setOf(key, russian, english, spanish, german) + aliases)
        .map(::normalizeCityAlias)
        .toSet(),
)

val cityCatalog: List<CityCatalogEntry> = listOf(
    city("prague", "Прага", "Prague", "Praga", "Prag", 50.0755, 14.4378),
    city("salzburg", "Зальцбург", "Salzburg", "Salzburgo", "Salzburg", 47.8095, 13.0550),
    city("verona", "Верона", "Verona", "Verona", "Verona", 45.4384, 10.9916),
    city("rome", "Рим", "Rome", "Roma", "Rom", 41.9028, 12.4964),
    city("pisa", "Пиза", "Pisa", "Pisa", "Pisa", 43.7228, 10.4017),
    city("figline valdarno", "Фильине-Вальдарно", "Figline Valdarno", "Figline Valdarno", "Figline Valdarno", 43.6190, 11.4690, "figline-valdarno", "фильине-валдарно"),
    city("san marino", "Сан-Марино", "San Marino", "San Marino", "San Marino", 43.9424, 12.4578),
    city("chioggia", "Кьоджа", "Chioggia", "Chioggia", "Chioggia", 45.2181, 12.2786),
    city("milan", "Милан", "Milan", "Milán", "Mailand", 45.4642, 9.1900),
    city("valdidentro", "Вальдидентро", "Valdidentro", "Valdidentro", "Valdidentro", 46.4890, 10.2940),
    city("ravensburg", "Равенсбург", "Ravensburg", "Ravensburg", "Ravensburg", 47.7810, 9.6110),
    city("munich", "Мюнхен", "Munich", "Múnich", "München", 48.1351, 11.5820, "muenchen"),
    city("vienna", "Вена", "Vienna", "Viena", "Wien", 48.2082, 16.3738),
    city("innsbruck", "Инсбрук", "Innsbruck", "Innsbruck", "Innsbruck", 47.2692, 11.4041),
    city("florence", "Флоренция", "Florence", "Florencia", "Florenz", 43.7696, 11.2558),
    city("venice", "Венеция", "Venice", "Venecia", "Venedig", 45.4408, 12.3155),
    city("tallinn", "Таллин", "Tallinn", "Tallin", "Tallinn", 59.4370, 24.7536),
    city("riga", "Рига", "Riga", "Riga", "Riga", 56.9496, 24.1052),
    city("vilnius", "Вильнюс", "Vilnius", "Vilna", "Vilnius", 54.6872, 25.2797),
    city("castel gandolfo", "Кастель-Гандольфо", "Castel Gandolfo", "Castel Gandolfo", "Castel Gandolfo", 41.7475, 12.6500),
    city("lake como", "Озеро Комо", "Lake Como", "Lago di Como", "Comer See", 45.8080, 9.2600, "озеро-комо"),
    city("como", "Комо", "Como", "Como", "Como", 45.8080, 9.2600, "комо (город)", "como (city)", "como (ciudad)", "como (stadt)"),
    city("bormio", "Бормио", "Bormio", "Bormio", "Bormio", 46.4670, 10.3740),
    city("val viola valley", "Долина Валь-Виола", "Val Viola Valley", "Valle de Val Viola", "Val Viola-Tal", 46.4200, 10.1900, "val viola", "валь-виола", "долина валь-виола"),
    city("stelvio", "Стельвио", "Stelvio", "Stelvio", "Stilfser Joch", 46.5286, 10.4540),
)

private val countryFlags = mapOf(
    "австрия" to "🇦🇹", "austria" to "🇦🇹", "österreich" to "🇦🇹", "osterreich" to "🇦🇹",
    "беларусь" to "🇧🇾", "belarus" to "🇧🇾",
    "бельгия" to "🇧🇪", "belgium" to "🇧🇪", "belgica" to "🇧🇪", "belgien" to "🇧🇪",
    "болгария" to "🇧🇬", "bulgaria" to "🇧🇬", "bulgarien" to "🇧🇬",
    "великобритания" to "🇬🇧", "united kingdom" to "🇬🇧", "great britain" to "🇬🇧", "uk" to "🇬🇧",
    "венгрия" to "🇭🇺", "hungary" to "🇭🇺", "hungría" to "🇭🇺", "ungarn" to "🇭🇺",
    "германия" to "🇩🇪", "germany" to "🇩🇪", "alemania" to "🇩🇪", "deutschland" to "🇩🇪",
    "греция" to "🇬🇷", "greece" to "🇬🇷", "grecia" to "🇬🇷", "griechenland" to "🇬🇷",
    "дания" to "🇩🇰", "denmark" to "🇩🇰", "dinamarca" to "🇩🇰", "dänemark" to "🇩🇰", "danemark" to "🇩🇰",
    "египет" to "🇪🇬", "egypt" to "🇪🇬", "egipto" to "🇪🇬", "ägypten" to "🇪🇬", "agypten" to "🇪🇬",
    "израиль" to "🇮🇱", "israel" to "🇮🇱",
    "индия" to "🇮🇳", "india" to "🇮🇳", "indien" to "🇮🇳",
    "ирландия" to "🇮🇪", "ireland" to "🇮🇪", "irlanda" to "🇮🇪", "irland" to "🇮🇪",
    "испания" to "🇪🇸", "spain" to "🇪🇸", "españa" to "🇪🇸", "espana" to "🇪🇸", "spanien" to "🇪🇸",
    "италия" to "🇮🇹", "italy" to "🇮🇹", "italia" to "🇮🇹", "italien" to "🇮🇹",
    "кипр" to "🇨🇾", "cyprus" to "🇨🇾", "chipre" to "🇨🇾", "zypern" to "🇨🇾",
    "китай" to "🇨🇳", "china" to "🇨🇳", "chine" to "🇨🇳",
    "латвия" to "🇱🇻", "latvia" to "🇱🇻", "letonia" to "🇱🇻", "lettland" to "🇱🇻",
    "литва" to "🇱🇹", "lithuania" to "🇱🇹", "lituania" to "🇱🇹", "litauen" to "🇱🇹",
    "мальта" to "🇲🇹", "malta" to "🇲🇹",
    "марокко" to "🇲🇦", "morocco" to "🇲🇦", "marruecos" to "🇲🇦", "marokko" to "🇲🇦",
    "нидерланды" to "🇳🇱", "netherlands" to "🇳🇱", "países bajos" to "🇳🇱", "paises bajos" to "🇳🇱", "niederlande" to "🇳🇱",
    "норвегия" to "🇳🇴", "norway" to "🇳🇴", "noruega" to "🇳🇴", "norwegen" to "🇳🇴",
    "оаэ" to "🇦🇪", "uae" to "🇦🇪", "united arab emirates" to "🇦🇪", "emiratos arabes unidos" to "🇦🇪", "vereinigte arabische emirate" to "🇦🇪",
    "польша" to "🇵🇱", "poland" to "🇵🇱", "polonia" to "🇵🇱", "polen" to "🇵🇱",
    "португалия" to "🇵🇹", "portugal" to "🇵🇹",
    "россия" to "🇷🇺", "russia" to "🇷🇺", "rusia" to "🇷🇺", "russland" to "🇷🇺",
    "румыния" to "🇷🇴", "romania" to "🇷🇴", "rumänien" to "🇷🇴", "rumanien" to "🇷🇴",
    "сербия" to "🇷🇸", "serbia" to "🇷🇸", "serbien" to "🇷🇸",
    "словакия" to "🇸🇰", "slovakia" to "🇸🇰", "eslovaquia" to "🇸🇰", "slowakei" to "🇸🇰",
    "словения" to "🇸🇮", "slovenia" to "🇸🇮", "eslovenia" to "🇸🇮", "slowenien" to "🇸🇮",
    "сша" to "🇺🇸", "usa" to "🇺🇸", "united states" to "🇺🇸", "estados unidos" to "🇺🇸", "vereinigte staaten" to "🇺🇸",
    "турция" to "🇹🇷", "turkey" to "🇹🇷", "türkiye" to "🇹🇷", "turquía" to "🇹🇷", "turkei" to "🇹🇷",
    "украина" to "🇺🇦", "ukraine" to "🇺🇦", "ucrania" to "🇺🇦",
    "франция" to "🇫🇷", "france" to "🇫🇷", "francia" to "🇫🇷", "frankreich" to "🇫🇷",
    "финляндия" to "🇫🇮", "finland" to "🇫🇮", "finlandia" to "🇫🇮", "finnland" to "🇫🇮",
    "хорватия" to "🇭🇷", "croatia" to "🇭🇷", "croacia" to "🇭🇷", "kroatien" to "🇭🇷",
    "чехия" to "🇨🇿", "czechia" to "🇨🇿", "czech republic" to "🇨🇿", "chequia" to "🇨🇿", "tschechien" to "🇨🇿",
    "швейцария" to "🇨🇭", "switzerland" to "🇨🇭", "suiza" to "🇨🇭", "schweiz" to "🇨🇭",
    "швеция" to "🇸🇪", "sweden" to "🇸🇪", "suecia" to "🇸🇪", "schweden" to "🇸🇪",
    "эстония" to "🇪🇪", "estonia" to "🇪🇪", "estland" to "🇪🇪",
    "южная корея" to "🇰🇷", "south korea" to "🇰🇷", "corea del sur" to "🇰🇷", "südkorea" to "🇰🇷", "sudkorea" to "🇰🇷",
    "япония" to "🇯🇵", "japan" to "🇯🇵", "japón" to "🇯🇵", "japon" to "🇯🇵",
    "сан-марино" to "🇸🇲", "san marino" to "🇸🇲",
)

private val cityFlags = mapOf(
    "амстердам" to "🇳🇱", "amsterdam" to "🇳🇱",
    "барселона" to "🇪🇸", "barcelona" to "🇪🇸",
    "берлин" to "🇩🇪", "berlin" to "🇩🇪",
    "болонья" to "🇮🇹", "bologna" to "🇮🇹",
    "будапешт" to "🇭🇺", "budapest" to "🇭🇺",
    "грац" to "🇦🇹", "graz" to "🇦🇹",
    "гамбург" to "🇩🇪", "hamburg" to "🇩🇪",
    "генуя" to "🇮🇹", "genoa" to "🇮🇹", "genova" to "🇮🇹",
    "кёльн" to "🇩🇪", "кельн" to "🇩🇪", "cologne" to "🇩🇪", "köln" to "🇩🇪", "koln" to "🇩🇪",
    "дубай" to "🇦🇪", "dubai" to "🇦🇪",
    "хельсинки" to "🇫🇮", "helsinki" to "🇫🇮",
    "лондон" to "🇬🇧", "london" to "🇬🇧",
    "лиссабон" to "🇵🇹", "lisbon" to "🇵🇹", "lisboa" to "🇵🇹",
    "мадрид" to "🇪🇸", "madrid" to "🇪🇸",
    "неаполь" to "🇮🇹", "naples" to "🇮🇹", "napoli" to "🇮🇹",
    "париж" to "🇫🇷", "paris" to "🇫🇷",
    "сиена" to "🇮🇹", "siena" to "🇮🇹",
    "сан-джиминьяно" to "🇮🇹", "san gimignano" to "🇮🇹",
    "стамбул" to "🇹🇷", "istanbul" to "🇹🇷", "i̇stanbul" to "🇹🇷",
    "турин" to "🇮🇹", "turin" to "🇮🇹", "torino" to "🇮🇹",
    "цюрих" to "🇨🇭", "zurich" to "🇨🇭", "zürich" to "🇨🇭",
    "лукка" to "🇮🇹", "lucca" to "🇮🇹",
    "москва" to "🇷🇺", "moscow" to "🇷🇺",
)

private val catalogFlags = mapOf(
    "prague" to "🇨🇿",
    "salzburg" to "🇦🇹",
    "verona" to "🇮🇹",
    "rome" to "🇮🇹",
    "pisa" to "🇮🇹",
    "figline valdarno" to "🇮🇹",
    "san marino" to "🇸🇲",
    "chioggia" to "🇮🇹",
    "milan" to "🇮🇹",
    "valdidentro" to "🇮🇹",
    "ravensburg" to "🇩🇪",
    "munich" to "🇩🇪",
    "vienna" to "🇦🇹",
    "innsbruck" to "🇦🇹",
    "florence" to "🇮🇹",
    "venice" to "🇮🇹",
    "tallinn" to "🇪🇪",
    "riga" to "🇱🇻",
    "vilnius" to "🇱🇹",
    "castel gandolfo" to "🇮🇹",
    "lake como" to "🇮🇹",
    "como" to "🇮🇹",
    "bormio" to "🇮🇹",
    "val viola valley" to "🇮🇹",
    "stelvio" to "🇮🇹",
)

fun cityCatalogEntry(value: String): CityCatalogEntry? {
    val cityPart = value.substringBefore(",").substringBefore(" — ")
    val normalized = normalizeCityAlias(cityPart)
    return cityCatalog.firstOrNull { normalized == it.key || normalized in it.aliases }
}

fun localizedCityCatalogName(value: String, language: String): String? = cityCatalogEntry(value)?.localized(language)

fun cityFlag(value: String): String {
    val cityPart = value.substringBefore(",").substringBefore(" — ")
    val countryPart = when {
        value.contains(",") -> value.substringAfter(",", "")
        value.contains(" — ") -> value.substringAfter(" — ", "")
        else -> ""
    }
    countryFlags[normalizeCityAlias(countryPart)]?.let { return it }

    val normalizedCity = normalizeCityAlias(cityPart)
    cityFlags[normalizedCity]?.let { return it }

    val catalogKey = cityCatalogEntry(cityPart)?.key
    return catalogFlags[catalogKey] ?: "📍"
}

fun countryFlag(value: String): String? {
    val normalizedCode = value.trim().uppercase(Locale.ROOT)
    if (normalizedCode.length == 2 && normalizedCode.all { it in 'A'..'Z' }) {
        return normalizedCode.map { letter ->
            String(Character.toChars(0x1F1E6 + letter.code - 'A'.code))
        }.joinToString("")
    }
    return countryFlags[normalizeCityAlias(value)]
}
