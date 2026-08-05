package com.odyssey.travelplanner.data

import java.util.Locale

data class CityCatalogEntry(
    val key: String,
    val russian: String,
    val english: String,
    val spanish: String,
    val german: String,
    val latitude: Double,
    val longitude: Double,
    val aliases: Set<String> = emptySet(),
) {
    fun localized(language: String): String = when (language.trim().uppercase(Locale.ROOT).substringBefore('-')) {
        "EN" -> english
        "ES" -> spanish
        "DE" -> german
        else -> russian
    }
}

private fun normalizeCityAlias(value: String): String = value
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

fun cityCatalogEntry(value: String): CityCatalogEntry? {
    val cityPart = value.substringBefore(",")
    val normalized = normalizeCityAlias(cityPart)
    return cityCatalog.firstOrNull { normalized == it.key || normalized in it.aliases }
}

fun localizedCityCatalogName(value: String, language: String): String? = cityCatalogEntry(value)?.localized(language)
