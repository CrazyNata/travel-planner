package com.odyssey.travelplanner.ui.i18n

import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.contentDescription
import java.util.Locale
import com.odyssey.travelplanner.ui.theme.LocalLanguage

internal fun mapLocale(language: String): Locale = when (normalizeLanguage(language)) {
    "EN" -> Locale.ENGLISH
    "ES" -> Locale("es", "ES")
    "DE" -> Locale.GERMAN
    else -> Locale("ru", "RU")
}

internal fun labelMapboxAccessibility(view: View, attributionDescription: String) {
    when {
        view.javaClass.name.endsWith("LogoViewImpl") -> view.contentDescription = "Mapbox"
        view.javaClass.name.endsWith("AttributionViewImpl") -> view.contentDescription = attributionDescription
    }
    if (view is ViewGroup) {
        repeat(view.childCount) { index -> labelMapboxAccessibility(view.getChildAt(index), attributionDescription) }
    }
}

@Composable
internal fun localized(ru: String, en: String, es: String, de: String): String = localized(LocalLanguage.current, ru, en, es, de)

internal fun normalizeLanguage(value: String): String = when (value.trim().uppercase(Locale.ROOT).substringBefore('-')) {
    "EN", "ENGLISH" -> "EN"
    "ES", "SPANISH" -> "ES"
    "DE", "GERMAN" -> "DE"
    else -> "RU"
}

internal fun localized(language: String, ru: String, en: String, es: String, de: String): String = when (normalizeLanguage(language)) {
    "EN" -> en
    "ES" -> es
    "DE" -> de
    else -> ru
}

internal fun localizedCountWord(
    count: Int,
    language: String,
    ruOne: String,
    ruFew: String,
    ruMany: String,
    enOne: String,
    enMany: String,
    esOne: String,
    esMany: String,
    deOne: String,
    deMany: String,
): String = when (normalizeLanguage(language)) {
    "EN" -> if (count == 1) enOne else enMany
    "ES" -> if (count == 1) esOne else esMany
    "DE" -> if (count == 1) deOne else deMany
    else -> when {
        count % 100 in 11..14 -> ruMany
        count % 10 == 1 -> ruOne
        count % 10 in 2..4 -> ruFew
        else -> ruMany
    }
}

internal fun localizedRouteSummary(tripDays: Int?, cityCount: Int, language: String): String {
    val parts = buildList {
        tripDays?.let {
            add(
                "$it ${localizedCountWord(it, language, "ДЕНЬ", "ДНЯ", "ДНЕЙ", "DAY", "DAYS", "DÍA", "DÍAS", "TAG", "TAGE")}",
            )
        }
        add(
            "$cityCount ${localizedCountWord(cityCount, language, "ГОРОД", "ГОРОДА", "ГОРОДОВ", "CITY", "CITIES", "CIUDAD", "CIUDADES", "STADT", "STÄDTE")}",
        )
    }
    return parts.joinToString(" · ")
}

internal fun localizedLegsAndCitiesSummary(legsCount: Int, cityCount: Int, language: String): String =
    "$legsCount ${localizedCountWord(legsCount, language, "переезд", "переезда", "переездов", "leg", "legs", "trayecto", "trayectos", "Etappe", "Etappen")} · " +
        "$cityCount ${localizedCountWord(cityCount, language, "город", "города", "городов", "city", "cities", "ciudad", "ciudades", "Stadt", "Städte")}"

@Composable
internal fun localizedBudgetCategory(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "жильё", "жилье", "проживание" -> localized("Жильё", "Lodging", "Alojamiento", "Unterkunft")
    "транспорт" -> localized("Транспорт", "Transport", "Transporte", "Transport")
    "еда и рестораны", "питание", "еда" -> localized("Еда и рестораны", "Food & restaurants", "Comida y restaurantes", "Essen & Restaurants")
    "активности и билеты", "развлечения", "активности" -> localized("Активности и билеты", "Activities & tickets", "Actividades y entradas", "Aktivitäten & Tickets")
    "прочее" -> localized("Прочее", "Other", "Otros", "Sonstiges")
    else -> value
}

