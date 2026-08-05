package com.odyssey.travelplanner.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.Hotel
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.DirectionsCar
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.FavoriteBorder
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.annotation.generated.PolylineAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.CircleAnnotationOptions
import com.mapbox.maps.plugin.annotation.generated.PointAnnotationOptions
import com.mapbox.maps.plugin.annotation.annotations
import com.mapbox.maps.plugin.annotation.generated.createPolylineAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createCircleAnnotationManager
import com.mapbox.maps.plugin.annotation.generated.createPointAnnotationManager
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.extension.style.layers.properties.generated.TextAnchor
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil3.compose.AsyncImage
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.odyssey.travelplanner.R
import com.odyssey.travelplanner.BuildConfig
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.AccountRepository
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripCard
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.WeatherRepository
import com.odyssey.travelplanner.data.WeatherSnapshot
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

private val OdysseyPurple = Color(0xFF6C5CE7)
private val OdysseyBackground = Color(0xFFF4F4F7)
private val OdysseySurface = Color(0xFFFFFFFF)
private val OdysseySurface2 = Color(0xFFF5F5F8)
private val OdysseyTrack = Color(0xFFEEEEF2)
private val OdysseyText = Color(0xFF141419)
private val OdysseyLabel = Color(0xFF3A3A42)
private val OdysseySubtext = Color(0xFF8A8A95)
private val OdysseyBorder = Color(0xFFE6E6EC)
private val OdysseyTint = Color(0xFFF1EEFE)
private val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.W400),
    Font(R.font.manrope_medium, FontWeight.W500),
    Font(R.font.manrope_semibold, FontWeight.W600),
    Font(R.font.manrope_bold, FontWeight.W700),
    Font(R.font.manrope_extrabold, FontWeight.W800),
)
private val OdysseyNoFontPadding = PlatformTextStyle(includeFontPadding = false)
private val LocalDarkTheme = staticCompositionLocalOf { false }
private val LocalLanguage = staticCompositionLocalOf { "RU" }

@Composable
private fun localized(ru: String, en: String, es: String, de: String): String = localized(LocalLanguage.current, ru, en, es, de)

private fun normalizeLanguage(value: String): String = when (value.trim().uppercase(Locale.ROOT).substringBefore('-')) {
    "EN", "ENGLISH" -> "EN"
    "ES", "SPANISH" -> "ES"
    "DE", "GERMAN" -> "DE"
    else -> "RU"
}

private fun localizedDatePickerContext(context: Context, language: String): Context {
    val locale = when (normalizeLanguage(language)) {
        "EN" -> Locale.ENGLISH
        "ES" -> Locale.forLanguageTag("es")
        "DE" -> Locale.GERMAN
        else -> Locale.forLanguageTag("ru")
    }
    return context.createConfigurationContext(Configuration(context.resources.configuration).apply { setLocale(locale) })
}

private fun localized(language: String, ru: String, en: String, es: String, de: String): String = when (normalizeLanguage(language)) {
    "EN" -> en
    "ES" -> es
    "DE" -> de
    else -> ru
}

@Composable
private fun localizedBudgetCategory(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "жильё", "жилье", "проживание" -> localized("Жильё", "Lodging", "Alojamiento", "Unterkunft")
    "транспорт" -> localized("Транспорт", "Transport", "Transporte", "Transport")
    "еда и рестораны", "питание", "еда" -> localized("Еда и рестораны", "Food & restaurants", "Comida y restaurantes", "Essen & Restaurants")
    "активности и билеты", "развлечения", "активности" -> localized("Активности и билеты", "Activities & tickets", "Actividades y entradas", "Aktivitäten & Tickets")
    "прочее" -> localized("Прочее", "Other", "Otros", "Sonstiges")
    else -> value
}

@Composable
private fun localizedSightNameByTerms(value: String): String {
    val replacements = when (normalizeLanguage(LocalLanguage.current)) {
        "EN" -> listOf(
            "рождественские ярмарочные домики" to "Christmas market stalls",
            "рождественская иллюминация" to "Christmas lights",
            "главная рождественская ёлка" to "Main Christmas tree",
            "рождественская ёлка" to "Christmas tree",
            "рождественский вертеп" to "Christmas nativity scene",
            "панорамные виды" to "Panoramic views",
            "смотровая площадка" to "Viewpoint",
            "кафедральный собор" to "Cathedral",
            "пешеходная улица" to "Pedestrian street",
            "торговая улица" to "Shopping street",
            "городские ворота" to "City gates",
            "новая ратуша" to "New Town Hall",
            "ратуша" to "Town Hall",
            "рождественская деревня" to "Christmas village",
            "резиденция" to "Residence",
            "дворец" to "Palace",
            "сад" to "Garden",
            "башня" to "Tower",
            "статуя" to "Statue",
            "театр" to "Theatre",
            "парк" to "Park",
            "рождественская" to "Christmas",
            "рождественский" to "Christmas",
            "рождественские" to "Christmas",
            "главная" to "Main",
            "площадь" to "Square",
            "площади" to "Square",
            "набережная" to "Waterfront",
            "мостики" to "Bridges",
            "мост" to "Bridge",
            "собор" to "Cathedral",
            "церковь" to "Church",
            "базилика" to "Basilica",
            "фонтан" to "Fountain",
            "колонна" to "Column",
            "храм" to "Temple",
            "арка" to "Arch",
            "рынок" to "Market",
            "порт" to "Port",
            "пляж" to "Beach",
            "прогулка" to "Walk",
            "остров" to "Island",
            "район" to "District",
            "улочки" to "Lanes",
            "рыбацкие домики" to "Fishing cottages",
            "пришвартованные лодки" to "Moored boats",
            "на дамбе" to "on the dike",
            "дамба" to "Dike",
            "римский форум" to "Roman Forum",
            "римский" to "Roman",
            "старый город" to "Old Town",
            "старого города" to "Old Town",
            "мюнхена" to "Munich",
            "вероны" to "Verona",
            "ватикана" to "the Vatican",
            "святого петра" to "St. Peter",
            "святого марка" to "St. Mark",
            "кьоджи" to "Chioggia",
            "риальто" to "Rialto",
            "гранд-канала" to "Grand Canal",
        )
        "ES" -> listOf(
            "рождественские ярмарочные домики" to "Casetas del mercado navideño",
            "рождественская иллюминация" to "Iluminación navideña",
            "главная рождественская ёлка" to "Árbol de Navidad principal",
            "рождественская ёлка" to "Árbol de Navidad",
            "рождественский вертеп" to "Belén navideño",
            "панорамные виды" to "Vistas panorámicas",
            "смотровая площадка" to "Mirador",
            "кафедральный собор" to "Catedral",
            "пешеходная улица" to "Calle peatonal",
            "торговая улица" to "Calle comercial",
            "городские ворота" to "Puertas de la ciudad",
            "новая ратуша" to "Ayuntamiento nuevo",
            "ратуша" to "Ayuntamiento",
            "рождественская деревня" to "Pueblo navideño",
            "резиденция" to "Residencia",
            "дворец" to "Palacio",
            "сад" to "Jardín",
            "башня" to "Torre",
            "статуя" to "Estatua",
            "театр" to "Teatro",
            "парк" to "Parque",
            "рождественская" to "Navideña",
            "рождественский" to "Navideño",
            "рождественские" to "Navideños",
            "главная" to "Principal",
            "площадь" to "Plaza",
            "площади" to "Plaza",
            "набережная" to "Paseo",
            "мостики" to "Puentes",
            "мост" to "Puente",
            "собор" to "Catedral",
            "церковь" to "Iglesia",
            "базилика" to "Basílica",
            "фонтан" to "Fuente",
            "колонна" to "Columna",
            "храм" to "Templo",
            "арка" to "Arco",
            "рынок" to "Mercado",
            "порт" to "Puerto",
            "пляж" to "Playa",
            "прогулка" to "Paseo",
            "остров" to "Isla",
            "район" to "Barrio",
            "улочки" to "Calles",
            "рыбацкие домики" to "Casas de pescadores",
            "пришвартованные лодки" to "Barcos amarrados",
            "римский форум" to "Foro Romano",
            "римский" to "Romano",
            "старый город" to "casco antiguo",
            "старого города" to "casco antiguo",
            "мюнхена" to "Múnich",
            "вероны" to "Verona",
            "ватикана" to "del Vaticano",
            "святого петра" to "San Pedro",
            "святого марка" to "San Marcos",
            "кьоджи" to "Chioggia",
            "риальто" to "Rialto",
            "гранд-канала" to "Gran Canal",
        )
        "DE" -> listOf(
            "рождественские ярмарочные домики" to "Weihnachtsmarktbuden",
            "рождественская иллюминация" to "Weihnachtsbeleuchtung",
            "главная рождественская ёлка" to "Hauptweihnachtsbaum",
            "рождественская ёлка" to "Weihnachtsbaum",
            "рождественский вертеп" to "Weihnachtskrippe",
            "панорамные виды" to "Panoramablick",
            "смотровая площадка" to "Aussichtspunkt",
            "кафедральный собор" to "Kathedrale",
            "пешеходная улица" to "Fußgängerstraße",
            "торговая улица" to "Einkaufsstraße",
            "городские ворота" to "Stadttor",
            "новая ратуша" to "Neues Rathaus",
            "ратуша" to "Rathaus",
            "рождественская деревня" to "Weihnachtsdorf",
            "резиденция" to "Residenz",
            "дворец" to "Palast",
            "сад" to "Garten",
            "башня" to "Turm",
            "статуя" to "Statue",
            "театр" to "Theater",
            "парк" to "Park",
            "рождественская" to "Weihnachts",
            "рождественский" to "Weihnachts",
            "рождественские" to "Weihnachts",
            "главная" to "Haupt",
            "площадь" to "Platz",
            "площади" to "Platz",
            "набережная" to "Uferpromenade",
            "мостики" to "Brücken",
            "мост" to "Brücke",
            "собор" to "Dom",
            "церковь" to "Kirche",
            "базилика" to "Basilika",
            "фонтан" to "Brunnen",
            "колонна" to "Säule",
            "храм" to "Tempel",
            "арка" to "Bogen",
            "рынок" to "Markt",
            "порт" to "Hafen",
            "пляж" to "Strand",
            "прогулка" to "Spaziergang",
            "остров" to "Insel",
            "район" to "Viertel",
            "улочки" to "Gassen",
            "рыбацкие домики" to "Fischerhäuser",
            "пришвартованные лодки" to "vertäute Boote",
            "римский форум" to "Forum Romanum",
            "римский" to "Römisch",
            "старый город" to "Altstadt",
            "старого города" to "Altstadt",
            "мюнхена" to "München",
            "вероны" to "Verona",
            "ватикана" to "Vatikan",
            "святого петра" to "St. Peter",
            "святого марка" to "St. Markus",
            "кьоджи" to "Chioggia",
            "риальто" to "Rialto",
            "гранд-канала" to "Canal Grande",
        )
        else -> emptyList()
    }
    return replacements.fold(value) { result, (source, target) ->
        result.replace(
            Regex("(?i)(?<![\\p{L}])${Regex.escape(source)}(?![\\p{L}])"),
            target,
        )
    }
}

@Composable
private fun localizedBudgetScope(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "общий", "общее" -> localized("общий", "shared", "compartido", "gemeinsam")
    "семья" -> localized("семья", "family", "familia", "Familie")
    "личный" -> localized("личный", "personal", "personal", "privat")
    else -> value
}

@Composable
private fun localizedCityFilter(value: String): String = if (value.trim().equals("Все города", ignoreCase = true)) {
    localized("Все города", "All cities", "Todas las ciudades", "Alle Städte")
} else {
    localizedCityName(value)
}

private fun localizedCityName(value: String, language: String): String {
    val parts = value.trim().split(Regex("\\s*,\\s*"), limit = 2)
    val city = when (parts.firstOrNull()?.lowercase(Locale.ROOT)) {
        "прага" -> localized(language, "Прага", "Prague", "Praga", "Prag")
        "мюнхен" -> localized(language, "Мюнхен", "Munich", "Múnich", "München")
        "верона" -> localized(language, "Верона", "Verona", "Verona", "Verona")
        "милан" -> localized(language, "Милан", "Milan", "Milán", "Mailand")
        "венеция" -> localized(language, "Венеция", "Venice", "Venecia", "Venedig")
        "рим" -> localized(language, "Рим", "Rome", "Roma", "Rom")
        "флоренция" -> localized(language, "Флоренция", "Florence", "Florencia", "Florenz")
        "пиза" -> localized(language, "Пиза", "Pisa", "Pisa", "Pisa")
        "кьоджа" -> localized(language, "Кьоджа", "Chioggia", "Chioggia", "Chioggia")
        "фильине-вальдарно" -> localized(language, "Фильине-Вальдарно", "Figline Valdarno", "Figline Valdarno", "Figline Valdarno")
        "равенсбург" -> localized(language, "Равенсбург", "Ravensburg", "Ravensburg", "Ravensburg")
        "сан-марино" -> localized(language, "Сан-Марино", "San Marino", "San Marino", "San Marino")
        "вальдидентро" -> localized(language, "Вальдидентро", "Valdidentro", "Valdidentro", "Valdidentro")
        "инсбрук" -> localized(language, "Инсбрук", "Innsbruck", "Innsbruck", "Innsbruck")
        "зальцбург" -> localized(language, "Зальцбург", "Salzburg", "Salzburgo", "Salzburg")
        "вена" -> localized(language, "Вена", "Vienna", "Viena", "Wien")
        "таллин" -> localized(language, "Таллин", "Tallinn", "Tallin", "Tallinn")
        "рига" -> localized(language, "Рига", "Riga", "Riga", "Riga")
        "вильнюс" -> localized(language, "Вильнюс", "Vilnius", "Vilna", "Vilnius")
        "кастель-гандольфо" -> localized(language, "Кастель-Гандольфо", "Castel Gandolfo", "Castel Gandolfo", "Castel Gandolfo")
        "озеро комо" -> localized(language, "Озеро Комо", "Lake Como", "Lago di Como", "Comer See")
        "стельвио" -> localized(language, "Стельвио", "Stelvio", "Stelvio", "Stilfser Joch")
        else -> parts.firstOrNull().orEmpty()
    }
    if (parts.size == 1) return city
    val country = when (parts[1].trim().lowercase(Locale.ROOT)) {
        "италия" -> localized(language, "Италия", "Italy", "Italia", "Italien")
        "германия" -> localized(language, "Германия", "Germany", "Alemania", "Deutschland")
        "австрия" -> localized(language, "Австрия", "Austria", "Austria", "Österreich")
        "чехия" -> localized(language, "Чехия", "Czechia", "Chequia", "Tschechien")
        "латвия" -> localized(language, "Латвия", "Latvia", "Letonia", "Lettland")
        "литва" -> localized(language, "Литва", "Lithuania", "Lituania", "Litauen")
        "эстония" -> localized(language, "Эстония", "Estonia", "Estonia", "Estland")
        else -> parts[1].trim()
    }
    return "$city, $country"
}

@Composable
private fun localizedCityName(value: String): String = localizedCityName(value, LocalLanguage.current)

private fun localizedCityList(value: String, language: String): String {
    val separator = when {
        value.contains(" → ") -> " → "
        value.contains(" · ") -> " · "
        value.contains(",") -> ", "
        else -> return localizedCityName(value, language)
    }
    return value.split(separator).joinToString(separator) { localizedCityName(it, language) }
}

private fun splitStoredCityList(value: String): List<String> {
    val trimmed = value.trim()
    if (trimmed.isBlank()) return emptyList()
    return when {
        trimmed.contains(" · ") -> trimmed.split(" · ")
        trimmed.contains(" → ") -> trimmed.split(" → ")
        trimmed.count { it == ',' } >= 2 -> trimmed.split(",")
        else -> listOf(trimmed)
    }
}

@Composable
private fun localizedTripStatus(value: String): String = when {
    value.contains("чернов", ignoreCase = true) -> localized("Черновик", "Draft", "Borrador", "Entwurf")
    value.contains("предст", ignoreCase = true) -> localized("Предстоящее", "Upcoming", "Próximo", "Bevorstehend")
    value.contains("заверш", ignoreCase = true) -> localized("Завершено", "Completed", "Completado", "Abgeschlossen")
    value.contains("прошед", ignoreCase = true) -> localized("Прошедшее", "Past", "Pasado", "Vergangen")
    else -> value
}

private fun localizedTripDateText(value: String, language: String, multilineDuration: Boolean = false): String {
    if (value.isBlank()) return value
    val monthNames = when (normalizeLanguage(language)) {
        "EN" -> mapOf("января" to "Jan", "январь" to "Jan", "февраля" to "Feb", "февраль" to "Feb", "марта" to "Mar", "март" to "Mar", "апреля" to "Apr", "апрель" to "Apr", "мая" to "May", "май" to "May", "июня" to "Jun", "июнь" to "Jun", "июля" to "Jul", "июль" to "Jul", "августа" to "Aug", "август" to "Aug", "сентября" to "Sep", "сентябрь" to "Sep", "октября" to "Oct", "октябрь" to "Oct", "ноября" to "Nov", "ноябрь" to "Nov", "декабря" to "Dec", "декабрь" to "Dec")
        "ES" -> mapOf("января" to "ene", "январь" to "ene", "февраля" to "feb", "февраль" to "feb", "марта" to "mar", "март" to "mar", "апреля" to "abr", "апрель" to "abr", "мая" to "may", "май" to "may", "июня" to "jun", "июнь" to "jun", "июля" to "jul", "июль" to "jul", "августа" to "ago", "август" to "ago", "сентября" to "sep", "сентябрь" to "sep", "октября" to "oct", "октябрь" to "oct", "ноября" to "nov", "ноябрь" to "nov", "декабря" to "dic", "декабрь" to "dic")
        "DE" -> mapOf("января" to "Jan", "январь" to "Jan", "февраля" to "Feb", "февраль" to "Feb", "марта" to "Mär", "март" to "Mär", "апреля" to "Apr", "апрель" to "Apr", "мая" to "Mai", "май" to "Mai", "июня" to "Jun", "июнь" to "Jun", "июля" to "Jul", "июль" to "Jul", "августа" to "Aug", "август" to "Aug", "сентября" to "Sep", "сентябрь" to "Sep", "октября" to "Okt", "октябрь" to "Okt", "ноября" to "Nov", "ноябрь" to "Nov", "декабря" to "Dez", "декабрь" to "Dez")
        else -> mapOf("января" to "янв", "январь" to "янв", "февраля" to "фев", "февраль" to "фев", "марта" to "мар", "март" to "мар", "апреля" to "апр", "апрель" to "апр", "мая" to "май", "май" to "май", "июня" to "июн", "июнь" to "июн", "июля" to "июл", "июль" to "июл", "августа" to "авг", "август" to "авг", "сентября" to "сен", "сентябрь" to "сен", "октября" to "окт", "октябрь" to "окт", "ноября" to "ноя", "ноябрь" to "ноя", "декабря" to "дек", "декабрь" to "дек")
    }
    var result = value
    val monthPattern = Regex("(?i)(января|январь|февраля|февраль|марта|март|апреля|апрель|мая|май|июня|июнь|июля|июль|августа|август|сентября|сентябрь|октября|октябрь|ноября|ноябрь|декабря|декабрь)")
    result = monthPattern.replace(result) { match ->
        monthNames[match.value.lowercase(Locale.ROOT)] ?: match.value
    }
    val durationWord = when (normalizeLanguage(language)) {
        "EN" -> "days"
        "ES" -> "días"
        "DE" -> "Tage"
        else -> "дней"
    }
    result = result.replace(Regex("(\\d+)\\s+дн(?:ей|я|ень)", RegexOption.IGNORE_CASE)) { "${it.groupValues[1]} $durationWord" }
    val dateJoiner = when (normalizeLanguage(language)) {
        "EN" -> " and "
        "ES" -> " y "
        "DE" -> " und "
        else -> " и "
    }
    result = result.replace(" и ", dateJoiner)
    return if (multilineDuration) {
        result.replace(Regex("\\s+·\\s+(\\d+\\s+\\S+)"), " ·\n$1")
    } else {
        result
    }
}

@Composable
private fun localizedWeatherCondition(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when {
        normalized.contains("ясно") || normalized.contains("clear") -> localized("Ясно", "Clear", "Despejado", "Klar")
        normalized.contains("облачно") || normalized.contains("cloud") -> localized("Облачно", "Cloudy", "Nublado", "Bewölkt")
        normalized.contains("туман") || normalized.contains("fog") -> localized("Туман", "Fog", "Niebla", "Nebel")
        normalized.contains("морось") || normalized.contains("drizzle") -> localized("Морось", "Drizzle", "Llovizna", "Nieselregen")
        normalized.contains("дождь") || normalized.contains("rain") -> localized("Дождь", "Rain", "Lluvia", "Regen")
        normalized.contains("снег") || normalized.contains("snow") -> localized("Снег", "Snow", "Nieve", "Schnee")
        normalized.contains("гроза") || normalized.contains("thunder") -> localized("Гроза", "Thunderstorm", "Tormenta", "Gewitter")
        else -> value
    }
}

@Composable
private fun localizedTripTitle(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "рождественская италия" -> localized("Рождественская Италия", "Christmas Italy", "Italia navideña", "Weihnachtliches Italien")
    "италия с семьёй", "италия с семьей" -> localized("Италия с семьёй", "Italy with family", "Italia en familia", "Italien mit Familie")
    else -> value
}

@Composable
private fun localizedSightCategory(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "достопримечательности", "достопримечательность", "места", "место" -> localized("Достопримечательности", "Sights", "Lugares", "Sehenswürdigkeiten")
    "главная достопримечательность" -> localized("Главная достопримечательность", "Main sight", "Lugar principal", "Hauptsehenswürdigkeit")
    "природа" -> localized("Природа", "Nature", "Naturaleza", "Natur")
    else -> value
}

@Composable
private fun localizedSightName(value: String): String = when (value.trim().lowercase(Locale.ROOT)) {
    "арена ди верона" -> localized("Арена ди Верона", "Verona Arena", "Arena de Verona", "Arena von Verona")
    "пьяцца бра" -> localized("Пьяцца Бра", "Piazza Bra", "Piazza Bra", "Piazza Bra")
    "дом джульетты" -> localized("Дом Джульетты", "Juliet's House", "Casa de Julieta", "Julias Haus")
    "пьяцца делле эрбе" -> localized("Пьяцца делле Эрбе", "Piazza delle Erbe", "Piazza delle Erbe", "Piazza delle Erbe")
    "большой цирк" -> localized("Большой цирк", "Circus Maximus", "Circo Máximo", "Circus Maximus")
    "термы каракаллы" -> localized("Термы Каракаллы", "Baths of Caracalla", "Termas de Caracalla", "Caracalla-Thermen")
    "уста истины" -> localized("Уста Истины", "Mouth of Truth", "Boca de la Verdad", "Mund der Wahrheit")
    "район монти", "district монти" -> localized("Район Монти", "Monti district", "Barrio de Monti", "Viertel Monti")
    "кастель-гандольфо" -> localized("Кастель-Гандольфо", "Castel Gandolfo", "Castel Gandolfo", "Castel Gandolfo")
    "озеро альбано" -> localized("Озеро Альбано", "Lake Albano", "Lago Albano", "Lago Albano")
    "антико спедале серристори" -> localized("Антико Спедале Серристори", "Antico Spedale Serristori", "Antico Spedale Serristori", "Antico Spedale Serristori")
    "пьяцца марсилио фичино" -> localized("Пьяцца Марсилио Фичино", "Piazza Marsilio Ficino", "Piazza Marsilio Ficino", "Piazza Marsilio Ficino")
    "палаццо преторио" -> localized("Палаццо Преторио", "Palazzo Pretorio", "Palazzo Pretorio", "Palazzo Pretorio")
    "коллегиата санта-мария" -> localized("Коллегиата Санта-Мария", "Collegiate Church of Santa Maria", "Colegiata de Santa Maria", "Stiftskirche Santa Maria")
    "корсо-дель-пополо" -> localized("Корсо-дель-Пополо", "Corso del Popolo", "Corso del Popolo", "Corso del Popolo")
    "собор санта-мария-ассунта", "кафедральный собор santa maria assunta" -> localized("Собор Санта-Мария-Ассунта", "Santa Maria Assunta Cathedral", "Catedral de Santa Maria Assunta", "Kathedrale Santa Maria Assunta")
    "канал вена" -> localized("Канал Вена", "Vena Canal", "Canal Vena", "Vena-Kanal")
    "палаццо гранайо" -> localized("Палаццо Гранайо", "Palazzo Granaio", "Palazzo Granaio", "Palazzo Granaio")
    "дуомо (миланский собор)", "дуомо (миланский cathedral)" -> localized("Дуомо (Миланский собор)", "Milan Cathedral (Duomo)", "Catedral de Milán (Duomo)", "Mailänder Dom (Duomo)")
    "галерея виктора эммануила ii" -> localized("Галерея Виктора Эммануила II", "Galleria Vittorio Emanuele II", "Galería Vittorio Emanuele II", "Galleria Vittorio Emanuele II")
    "театр ла скала", "theatre ла скала" -> localized("Театр Ла Скала", "La Scala Theatre", "Teatro alla Scala", "Teatro alla Scala")
    "пинакотека брера" -> localized("Пинакотека Брера", "Brera Gallery", "Pinacoteca de Brera", "Pinacoteca di Brera")
    "монументальное кладбище" -> localized("Монументальное кладбище", "Monumental Cemetery", "Cementerio Monumental", "Monumentalfriedhof")
    "площадь гае ауленти", "square гае ауленти" -> localized("Площадь Гае Ауленти", "Piazza Gae Aulenti", "Piazza Gae Aulenti", "Piazza Gae Aulenti")
    "боско вертикале" -> localized("Боско Вертикале", "Bosco Verticale", "Bosco Verticale", "Bosco Verticale")
    "центральный вокзал милана" -> localized("Центральный вокзал Милана", "Milan Central Station", "Estación Central de Milán", "Mailänder Hauptbahnhof")
    "комо (город)" -> localized("Комо (город)", "Como (city)", "Como (ciudad)", "Como (Stadt)")
    "черноббио (вилла д’эсте)" -> localized("Черноббио (Вилла д’Эсте)", "Cernobbio (Villa d’Este)", "Cernobbio (Villa d’Este)", "Cernobbio (Villa d’Este)")
    "вилла бальбьянелло (ленно)" -> localized("Вилла Бальбьянелло (Ленно)", "Villa del Balbianello (Lenno)", "Villa del Balbianello (Lenno)", "Villa del Balbianello (Lenno)")
    "вилла карлотта (тремеццо)" -> localized("Вилла Карлотта (Тремеццо)", "Villa Carlotta (Tremezzo)", "Villa Carlotta (Tremezzo)", "Villa Carlotta (Tremezzo)")
    "арнога" -> localized("Арнога", "Arnoga", "Arnoga", "Arnoga")
    "долина валь-виола" -> localized("Долина Валь-Виола", "Val Viola Valley", "Valle de Val Viola", "Val Viola-Tal")
    "башни фраэле" -> localized("Башни Фраэле", "Fraele Towers", "Torres Fraele", "Fraele-Türme")
    "озеро делле-скале" -> localized("Озеро делле-Скале", "Lake delle Scale", "Lago delle Scale", "Lago delle Scale")
    "арнога — старт стельвио" -> localized("Арнога — старт Стельвио", "Arnoga — Stelvio start", "Arnoga — inicio del Stelvio", "Arnoga — Stelvio-Start")
    "бормио — старый город", "бормио — old town" -> localized("Бормио — старый город", "Bormio — Old Town", "Bormio — casco antiguo", "Bormio — Altstadt")
    "баньи-веки — панорама бормио" -> localized("Баньи-Векки — панорама Бормио", "Bagni Vecchi — Bormio panorama", "Bagni Vecchi — panorama de Bormio", "Bagni Vecchi — Panorama von Bormio")
    "перевал стельвио" -> localized("Перевал Стельвио", "Stelvio Pass", "Paso del Stelvio", "Stilfser Joch")
    "мариенплац и новая ратуша", "мариенплац и new town hall" -> localized("Мариенплац и Новая ратуша", "Marienplatz and New Town Hall", "Marienplatz y el Ayuntamiento Nuevo", "Marienplatz und Neues Rathaus")
    "виктуалиенмаркт" -> localized("Виктуалиенмаркт", "Viktualienmarkt", "Viktualienmarkt", "Viktualienmarkt")
    "одеонсплац" -> localized("Одеонсплац", "Odeonsplatz", "Odeonsplatz", "Odeonsplatz")
    "хофгартен" -> localized("Хофгартен", "Hofgarten", "Hofgarten", "Hofgarten")
    "староместская площадь и часы орлой", "староместская square и часы орлой" -> localized("Староместская площадь и часы Орлой", "Old Town Square and Orloj", "Plaza de la Ciudad Vieja y el Orloj", "Altstädter Ring und Orloj")
    "клементинум" -> localized("Клементинум", "Klementinum", "Klementinum", "Klementinum")
    "карлов мост", "карлов bridge" -> localized("Карлов мост", "Charles Bridge", "Puente de Carlos", "Karlsbrücke")
    "малостранская площадь", "малостранская square" -> localized("Малостранская площадь", "Malá Strana Square", "Plaza de Malá Strana", "Kleinseitner Ring")
    "арена вероны (arena di verona)" -> localized("Арена Вероны (Arena di Verona)", "Verona Arena (Arena di Verona)", "Arena de Verona (Arena di Verona)", "Arena von Verona (Arena di Verona)")
    "рождественская звезда rigoletto" -> localized("Рождественская звезда Rigoletto", "Rigoletto Christmas star", "Estrella navideña Rigoletto", "Weihnachtsstern Rigoletto")
    "набережная реки адидже" -> localized("Набережная реки Адидже", "Adige riverfront", "Paseo del río Adigio", "Uferpromenade am Etsch")
    "фонтан четырех рек" -> localized("Фонтан Четырёх рек", "Fountain of the Four Rivers", "Fuente de los Cuatro Ríos", "Brunnen der Vier Flüsse")
    "церковь sant'agnese in agone" -> localized("Церковь Sant'Agnese in Agone", "Sant'Agnese in Agone Church", "Iglesia de Sant'Agnese in Agone", "Kirche Sant'Agnese in Agone")
    "храм адриана" -> localized("Храм Адриана", "Temple of Hadrian", "Templo de Adriano", "Tempel des Hadrian")
    "колонна марка аврелия" -> localized("Колонна Марка Аврелия", "Column of Marcus Aurelius", "Columna de Marco Aurelio", "Säule des Marc Aurel")
    "фонтан треви" -> localized("Фонтан Треви", "Trevi Fountain", "Fontana di Trevi", "Trevi-Brunnen")
    "испанская лестница" -> localized("Испанская лестница", "Spanish Steps", "Escalinata de España", "Spanische Treppe")
    "рождественская ёлка на piazza di spagna" -> localized("Рождественская ёлка на Piazza di Spagna", "Christmas tree at Piazza di Spagna", "Árbol de Navidad en Piazza di Spagna", "Weihnachtsbaum an der Piazza di Spagna")
    "колизей" -> localized("Колизей", "Colosseum", "Coliseo", "Kolosseum")
    "арка константина" -> localized("Арка Константина", "Arch of Constantine", "Arco de Constantino", "Konstantinsbogen")
    "римский форум" -> localized("Римский форум", "Roman Forum", "Forum Romanum", "Forum Romanum")
    "палатинский холм" -> localized("Палатинский холм", "Palatine Hill", "Monte Palatino", "Palatin")
    "смотровая площадка на форум" -> localized("Смотровая площадка на Форум", "Forum viewpoint", "Mirador del Foro", "Aussichtspunkt auf das Forum")
    "капитолийская площадь" -> localized("Капитолийская площадь", "Capitoline Square", "Plaza del Campidoglio", "Kapitolsplatz")
    "площадь святого петра" -> localized("Площадь Святого Петра", "St. Peter's Square", "Plaza de San Pedro", "Petersplatz")
    "собор святого петра" -> localized("Собор Святого Петра", "St. Peter's Basilica", "Basílica de San Pedro", "Petersdom")
    "главная рождественская ёлка ватикана" -> localized("Главная рождественская ёлка Ватикана", "Vatican's main Christmas tree", "Árbol de Navidad principal del Vaticano", "Hauptweihnachtsbaum des Vatikans")
    "рождественский вертеп" -> localized("Рождественский вертеп", "Christmas nativity scene", "Belén navideño", "Weihnachtskrippe")
    "мост скальци" -> localized("Мост Скальци", "Scalzi Bridge", "Puente de los Descalzos", "Scalzi-Brücke")
    "прогулка вдоль гранд-канала" -> localized("Прогулка вдоль Гранд-канала", "Grand Canal walk", "Paseo por el Gran Canal", "Spaziergang am Canal Grande")
    "вапоретто по гранд-каналу" -> localized("Вапоретто по Гранд-каналу", "Vaporetto along the Grand Canal", "Vaporetto por el Gran Canal", "Vaporetto auf dem Canal Grande")
    "мост риальто" -> localized("Мост Риальто", "Rialto Bridge", "Puente de Rialto", "Rialtobrücke")
    "рынок риальто" -> localized("Рынок Риальто", "Rialto Market", "Mercado de Rialto", "Rialto-Markt")
    "улочки района сан-поло" -> localized("Улочки района Сан-Поло", "San Polo's lanes", "Calles del barrio de San Polo", "Gassen im Viertel San Polo")
    "базилика санта-мария-глориоза-деи-фрари" -> localized("Базилика Санта-Мария-Глориоза-деи-Фрари", "Basilica of Santa Maria Gloriosa dei Frari", "Basílica de Santa Maria Gloriosa dei Frari", "Basilika Santa Maria Gloriosa dei Frari")
    "мост вздохов" -> localized("Мост Вздохов", "Bridge of Sighs", "Puente de los Suspiros", "Seufzerbrücke")
    "площадь сан-марко" -> localized("Площадь Сан-Марко", "St. Mark's Square", "Plaza de San Marcos", "Markusplatz")
    "собор святого марка" -> localized("Собор Святого Марка", "St. Mark's Basilica", "Basílica de San Marcos", "Markusdom")
    "колонна святого марка" -> localized("Колонна Святого Марка", "Column of St. Mark", "Columna de San Marcos", "Säule des heiligen Markus")
    "канал vena" -> localized("Канал Vena", "Vena Canal", "Canal Vena", "Vena-Kanal")
    "кафедральный собор santa maria assunta" -> localized("Кафедральный собор Santa Maria Assunta", "Santa Maria Assunta Cathedral", "Catedral de Santa Maria Assunta", "Kathedrale Santa Maria Assunta")
    "церковь sant'andrea" -> localized("Церковь Sant'Andrea", "Sant'Andrea Church", "Iglesia de Sant'Andrea", "Kirche Sant'Andrea")
    "мостики через канал vena" -> localized("Мостики через канал Vena", "Bridges over Vena Canal", "Puentes sobre el canal Vena", "Brücken über den Vena-Kanal")
    "рыбацкие домики и пришвартованные лодки" -> localized("Рыбацкие домики и пришвартованные лодки", "Fishing cottages and moored boats", "Casas de pescadores y barcos amarrados", "Fischerhäuser und vertäute Boote")
    "набережная лагуны" -> localized("Набережная лагуны", "Lagoon waterfront", "Paseo de la laguna", "Lagunenpromenade")
    "порт кьоджи" -> localized("Порт Кьоджи", "Chioggia port", "Puerto de Chioggia", "Hafen von Chioggia")
    "прогулка по дамбе diga sottomarina" -> localized("Прогулка по дамбе Diga Sottomarina", "Diga Sottomarina dike walk", "Paseo por el dique Diga Sottomarina", "Spaziergang auf dem Damm Diga Sottomarina")
    "панорамные виды на лагуну" -> localized("Панорамные виды на лагуну", "Panoramic lagoon views", "Vistas panorámicas de la laguna", "Panoramablick auf die Lagune")
    "главная рождественская ёлка города" -> localized("Главная рождественская ёлка города", "City's main Christmas tree", "Árbol de Navidad principal de la ciudad", "Hauptweihnachtsbaum der Stadt")
    "рождественские ярмарочные домики" -> localized("Рождественские ярмарочные домики", "Christmas market stalls", "Casetas del mercado navideño", "Weihnachtsmarktbuden")
    else -> localizedSightNameByTerms(value)
}

@Composable
private fun localizedSightDescription(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    val known = when {
        normalized.contains("античная мраморная маска") -> localized("Античная мраморная маска в портике церкви Санта-Мария-ин-Космедин — по легенде откусит руку лжецу.", "Ancient marble mask at the portico of Santa Maria in Cosmedin; legend says it bites the hand of a liar.", "Máscara de mármol antigua en el pórtico de Santa Maria in Cosmedin; según la leyenda, muerde la mano del mentiroso.", "Antike Marmormaske im Portikus von Santa Maria in Cosmedin; der Legende nach beißt sie die Hand eines Lügners.")
        normalized.contains("атмосферный старинный район") -> localized("Атмосферный старинный район у Форума: ремесленные лавки, винные бары и вечерняя жизнь.", "Atmospheric historic district by the Forum, with artisan shops, wine bars, and lively evenings.", "Barrio histórico con ambiente junto al Foro, tiendas de artesanía, bares de vino y vida nocturna.", "Stimmungsvolles historisches Viertel am Forum mit Handwerksläden, Weinbars und regem Abendleben.")
        normalized.contains("однодневная поездка из рима") -> localized("Однодневная поездка из Рима: исторический центр и Апостольский дворец.", "A day trip from Rome: historic center and Apostolic Palace.", "Excursión de un día desde Roma: centro histórico y Palacio Apostólico.", "Tagesausflug von Rom: historisches Zentrum und Apostolischer Palast.")
        normalized.contains("вулканическое озеро") -> localized("Вулканическое озеро рядом с Кастель-Гандольфо.", "Volcanic lake near Castel Gandolfo.", "Lago volcánico cerca de Castel Gandolfo.", "Vulkanischer See bei Castel Gandolfo.")
        normalized.contains("средневековый госпиталь") -> localized("Средневековый госпиталь, основанный семьёй Серристори в XIV веке. Сохранил свою церковь и алтарь XV века; сегодня — культурный центр и музей аптечной посуды.", "Medieval hospital founded by the Serristori family in the 14th century. It retains its church and 15th-century altar and is now a cultural center and museum of apothecary ceramics.", "Hospital medieval fundado por la familia Serristori en el siglo XIV. Conserva su iglesia y un altar del siglo XV; hoy es un centro cultural y museo de cerámica farmacéutica.", "Mittelalterliches Hospital, im 14. Jahrhundert von der Familie Serristori gegründet. Mit eigener Kirche und Altar aus dem 15. Jahrhundert ist es heute Kulturzentrum und Museum für Apothekenkeramik.")
        normalized.contains("сердце старого города") -> localized("Сердце Старого города — одна из самых больших средневековых площадей Тосканы, окружённая портиками. По воскресеньям здесь антикварный рынок. Названа в честь философа Марсилио Фичино, родившегося в Фильине в 1433 году.", "The heart of the Old Town: one of Tuscany's largest medieval squares, lined with arcades. An antiques market is held here on Sundays. It is named after philosopher Marsilio Ficino, born in Figline in 1433.", "El corazón del casco antiguo: una de las plazas medievales más grandes de la Toscana, rodeada de soportales. Los domingos acoge un mercado de antigüedades. Lleva el nombre del filósofo Marsilio Ficino, nacido en Figline en 1433.", "Das Herz der Altstadt: einer der größten mittelalterlichen Plätze der Toskana, von Arkaden gesäumt. Sonntags findet hier ein Antiquitätenmarkt statt. Benannt ist er nach dem Philosophen Marsilio Ficino, der 1433 in Figline geboren wurde.")
        normalized.contains("историческая резиденция подеста") -> localized("Историческая резиденция подеста в центре города; фасад украшен гербами прежних правителей.", "Historic residence of the podestà in the city center; its façade is decorated with the coats of arms of former rulers.", "Residencia histórica del podestà en el centro; su fachada está decorada con los escudos de antiguos gobernantes.", "Historische Residenz des Podestà im Stadtzentrum; die Fassade ist mit den Wappen früherer Herrscher geschmückt.")
        normalized.contains("главная церковь города") -> localized("Главная церковь города. Хранит алтарный образ «Мадонна с Младенцем на троне» кисти Мастера из Фильине (после 1317 года).", "The city's main church. It houses the altarpiece Madonna and Child Enthroned by the Master of Figline, painted after 1317.", "La iglesia principal de la ciudad. Conserva el retablo Madonna con el Niño entronizada, obra del Maestro de Figline, posterior a 1317.", "Die Hauptkirche der Stadt. Sie beherbergt das Altarbild Madonna mit Kind auf dem Thron des Meisters von Figline aus der Zeit nach 1317.")
        normalized.contains("парадная главная улица") -> localized("Парадная главная улица-«салотто» Кьоджи, вытянутая через весь остров: дворцы, кафе и вечернее гулянье горожан.", "Chioggia's grand main boulevard, a salon-like street stretching across the island with palaces, cafés, and evening strolls.", "La gran calle principal de Chioggia, un paseo tipo salón que recorre la isla entre palacios, cafés y paseos al atardecer.", "Chioggias prächtige Hauptstraße, eine salonartige Flaniermeile über die Insel mit Palästen, Cafés und abendlichen Spaziergängen.")
        normalized.contains("кафедральный собор xvii века") -> localized("Кафедральный собор XVII века, перестроенный Бальдассаре Лонгеной, с отдельно стоящей колокольней XIV века.", "A 17th-century cathedral rebuilt by Baldassare Longhena, with a freestanding 14th-century bell tower.", "Catedral del siglo XVII reconstruida por Baldassare Longhena, con un campanario independiente del siglo XIV.", "Kathedrale aus dem 17. Jahrhundert, von Baldassare Longhena umgebaut, mit freistehendem Glockenturm aus dem 14. Jahrhundert.")
        normalized.contains("живописный главный канал") -> localized("Живописный главный канал с рыбацкими лодками и старыми мостами — за это Кьоджу зовут «маленькой Венецией».", "Scenic main canal with fishing boats and old bridges — why Chioggia is called Little Venice.", "Canal principal pintoresco con barcos pesqueros y puentes antiguos; por eso Chioggia recibe el nombre de Pequeña Venecia.", "Malerischer Hauptkanal mit Fischerbooten und alten Brücken — deshalb wird Chioggia Klein-Venedig genannt.")
        normalized.contains("городская житница") -> localized("Городская житница 1322 года на канале Вена; сегодня внизу — рыбный рынок и туристический офис.", "The city's 1322 granary on Vena Canal; today its ground floor houses a fish market and tourist office.", "Granero municipal de 1322 junto al canal Vena; hoy alberga un mercado de pescado y una oficina de turismo.", "Städtischer Getreidespeicher von 1322 am Vena-Kanal; heute befinden sich im Erdgeschoss ein Fischmarkt und ein Touristenbüro.")
        normalized.contains("готический собор из белого мрамора") -> localized("Готический собор из белого мрамора — символ Милана; можно подняться на крышу к шпилям и «Мадоннине».", "Gothic cathedral of white marble and symbol of Milan; climb to the rooftop spires and the Madonnina.", "Catedral gótica de mármol blanco y símbolo de Milán; se puede subir a la azotea, a las agujas y a la Madonnina.", "Gotischer Dom aus weißem Marmor und Wahrzeichen Mailands; auf dem Dach gelangt man zu den Türmen und der Madonnina.")
        normalized.contains("роскошная стеклянная галерея") -> localized("Роскошная стеклянная галерея XIX века рядом с собором — «гостиная Милана» с кафе и бутиками.", "A lavish 19th-century glass arcade beside the cathedral, known as Milan's salon, with cafés and boutiques.", "Galería acristalada del siglo XIX junto a la catedral, el salón de Milán, con cafés y boutiques.", "Prunkvolle Glasgalerie aus dem 19. Jahrhundert neben dem Dom, Mailands Salon mit Cafés und Boutiquen.")
        normalized.contains("легендарный оперный театр") -> localized("Легендарный оперный театр Ла Скала; при нём — музей театра.", "Legendary La Scala opera house with its own theater museum.", "Legendario teatro de ópera La Scala, con su propio museo teatral.", "Das legendäre Opernhaus La Scala mit eigenem Theatermuseum.")
        normalized.contains("одна из лучших картинных галерей италии") -> localized("Одна из лучших картинных галерей Италии в квартале Брера (Рафаэль, Караваджо, Мантенья).", "One of Italy's finest art galleries in the Brera district, with works by Raphael, Caravaggio, and Mantegna.", "Una de las mejores galerías de arte de Italia en el barrio de Brera, con obras de Rafael, Caravaggio y Mantegna.", "Eine der besten Kunstgalerien Italiens im Viertel Brera mit Werken von Raffael, Caravaggio und Mantegna.")
        normalized.contains("музей скульптуры под открытым небом") -> localized("Музей скульптуры под открытым небом: фамильные усыпальницы, модерн и надгробия-шедевры.", "Open-air sculpture museum with family tombs, Art Nouveau works, and masterpiece monuments.", "Museo de escultura al aire libre con mausoleos familiares, obras modernistas y monumentos funerarios.", "Skulpturenmuseum unter freiem Himmel mit Familiengrabmälern, Jugendstil und meisterhaften Grabdenkmälern.")
        normalized.contains("современная площадь порта-нуова") -> localized("Современная площадь Порта-Нуова с небоскрёбами, фонтанами и панорамой делового Милана.", "Modern Piazza Gae Aulenti in Porta Nuova, with skyscrapers, fountains, and views over Milan's business district.", "Moderna Piazza Gae Aulenti en Porta Nuova, con rascacielos, fuentes y vistas del distrito financiero de Milán.", "Moderner Platz Gae Aulenti in Porta Nuova mit Wolkenkratzern, Brunnen und Blick auf Mailands Geschäftsviertel.")
        normalized.contains("вертикальный лес") -> localized("«Вертикальный лес» — башни-небоскрёбы с деревьями на балконах в квартале Порта-Нуова.", "The Vertical Forest: skyscraper towers covered with trees on their balconies in Porta Nuova.", "El Bosque Vertical: torres de rascacielos con árboles en los balcones de Porta Nuova.", "Der Vertikale Wald: Wolkenkratzer mit Bäumen auf den Balkonen im Viertel Porta Nuova.")
        normalized.contains("монументальный вокзал") -> localized("Монументальный вокзал Milano Centrale с парадным фасадом, огромными сводчатыми залами и архитектурой в духе ар-деко.", "Monumental Milano Centrale station with a grand façade, vast vaulted halls, and Art Deco architecture.", "Monumental estación Milano Centrale, con una fachada grandiosa, enormes salas abovedadas y arquitectura art déco.", "Der monumentale Bahnhof Milano Centrale mit prächtiger Fassade, riesigen Gewölbehallen und Art-déco-Architektur.")
        normalized.contains("элегантный город у южного берега") -> localized("Элегантный город у южного берега: романский собор, набережная Пьяцца-Кавур и фуникулёр в Брунате с видом на озеро.", "Elegant city on the southern shore: a Romanesque cathedral, Piazza Cavour waterfront, and the funicular to Brunate overlooking the lake.", "Elegante ciudad en la orilla sur: catedral románica, paseo marítimo de Piazza Cavour y funicular a Brunate con vistas al lago.", "Elegante Stadt am Südufer mit romanischem Dom, Uferpromenade an der Piazza Cavour und Standseilbahn nach Brunate mit Seeblick.")
        normalized.contains("первый городок к северу") -> localized("Первый городок к северу от Комо; знаменита Вилла д’Эсте и живописная набережная.", "The first town north of Como, known for Villa d’Este and its scenic waterfront.", "El primer pueblo al norte de Como, famoso por Villa d’Este y su pintoresco paseo marítimo.", "Der erste Ort nördlich von Como, bekannt für die Villa d’Este und seine malerische Uferpromenade.")
        normalized.contains("романтическая вилла на мысу") -> localized("Романтическая вилла на мысу с террасными садами (снималась в «Звёздных войнах» и «Казино Рояль»); от Ленно — пешком или катером.", "Romantic villa on a promontory with terraced gardens, featured in Star Wars and Casino Royale; reachable from Lenno on foot or by boat.", "Villa romántica en un promontorio con jardines en terrazas, escenario de Star Wars y Casino Royale; desde Lenno se llega a pie o en barco.", "Romantische Villa auf einer Landzunge mit Terrassengärten, Drehort von Star Wars und Casino Royale; von Lenno zu Fuß oder per Boot erreichbar.")
        normalized.contains("вилла-музей со знаменитым ботаническим") -> localized("Вилла-музей со знаменитым ботаническим садом (азалии, рододендроны) и скульптурами.", "Villa museum with a famous botanical garden of azaleas and rhododendrons, plus sculptures.", "Museo-villa con un famoso jardín botánico de azaleas y rododendros, además de esculturas.", "Villa-Museum mit berühmtem botanischem Garten aus Azaleen und Rhododendren sowie Skulpturen.")
        normalized.contains("старт горной дороги") -> localized("Старт горной дороги в Валь-Виолу. Перед выездом проверьте погоду и статус высокогорных дорог.", "Start of the mountain road into Val Viola. Check the weather and high-mountain road status before departure.", "Inicio de la carretera de montaña hacia Val Viola. Compruebe el tiempo y el estado de las carreteras de alta montaña antes de salir.", "Beginn der Bergstraße ins Val Viola. Prüfen Sie vor der Abfahrt Wetter und Zustand der Hochgebirgsstraßen.")
        normalized.contains("широкая альпийская долина") -> localized("Широкая альпийская долина с лёгкими прогулками по грунтовой дороге и видами на вершины.", "Wide Alpine valley with easy walks along a dirt road and views of the peaks.", "Amplio valle alpino con paseos sencillos por un camino de tierra y vistas a las cumbres.", "Weites Alpental mit leichten Spaziergängen auf einer Schotterstraße und Blick auf die Gipfel.")
        normalized.contains("две средневековые башни") -> localized("Две средневековые башни над дорогой к озёрам Канкано; короткая остановка с панорамой долины.", "Two medieval towers above the road to the Cancano lakes; a short stop with a panoramic valley view.", "Dos torres medievales sobre la carretera a los lagos de Cancano; breve parada con vistas panorámicas del valle.", "Zwei mittelalterliche Türme über der Straße zu den Cancano-Seen; kurzer Halt mit Panoramablick ins Tal.")
        normalized.contains("высокогорное водохранилище") -> localized("Высокогорное водохранилище у Канкано, окружённое светлыми склонами и тропами.", "High-mountain reservoir near Cancano, surrounded by pale slopes and trails.", "Embalse de alta montaña cerca de Cancano, rodeado de laderas claras y senderos.", "Hochgebirgsstausee bei Cancano, umgeben von hellen Hängen und Wanderwegen.")
        normalized.contains("s.s. 301") -> localized("Старт: S.S. 301, Località Arnoga. В октябре утром обязательно проверьте открытие перевалов Стельвио и Умбраиль: снегопад может закрыть дорогу.", "Start: S.S. 301, Località Arnoga. In October, check early in the morning that the Stelvio and Umbrail passes are open: snowfall can close the road.", "Inicio: S.S. 301, Località Arnoga. En octubre, compruebe por la mañana que los puertos de Stelvio y Umbrail estén abiertos: la nieve puede cerrar la carretera.", "Start: S.S. 301, Località Arnoga. Prüfen Sie im Oktober morgens unbedingt, ob die Pässe Stilfser Joch und Umbrail geöffnet sind: Schneefall kann die Straße sperren.")
        normalized.contains("короткая остановка на кофе") -> localized("Короткая остановка на кофе и прогулку по старому центру перед подъёмом к перевалу.", "A short coffee stop and walk through the Old Town before climbing to the pass.", "Breve parada para tomar café y pasear por el casco antiguo antes de subir al puerto.", "Kurzer Kaffeestopp und Spaziergang durch die Altstadt vor dem Anstieg zum Pass.")
        normalized.contains("смотровая точка над бормио") -> localized("Смотровая точка над Бормио у исторических терм; вид на долину Адды.", "Viewpoint above Bormio by the historic baths, overlooking the Adda valley.", "Mirador sobre Bormio junto a las termas históricas, con vistas al valle del Adda.", "Aussichtspunkt über Bormio bei den historischen Thermen mit Blick ins Adda-Tal.")
        normalized.contains("один из самых высоких перевалов альп") -> localized("Один из самых высоких перевалов Альп (2757 м): серпантины, ледниковые склоны и большая обзорная площадка.", "One of the highest Alpine passes (2,757 m), with hairpin bends, glacial slopes, and a large viewpoint.", "Uno de los puertos alpinos más altos (2757 m), con curvas cerradas, laderas glaciares y un gran mirador.", "Einer der höchsten Alpenpässe (2757 m) mit Serpentinen, Gletscherhängen und großem Aussichtspunkt.")
        normalized.contains("сердце старого города: готическая") -> localized("Сердце Старого города: готическая ратуша и знаменитые астрономические часы.", "The heart of the Old Town: Gothic town hall and famous astronomical clock.", "El corazón del casco antiguo: ayuntamiento gótico y famoso reloj astronómico.", "Das Herz der Altstadt: gotisches Rathaus und berühmte astronomische Uhr.")
        normalized.contains("исторический иезуитский комплекс") -> localized("Исторический иезуитский комплекс с барочной библиотекой и башней с видом на центр.", "Historic Jesuit complex with a Baroque library and a tower overlooking the city center.", "Complejo jesuita histórico con biblioteca barroca y torre con vistas al centro.", "Historischer Jesuitenkomplex mit barocker Bibliothek und Turm mit Blick auf das Stadtzentrum.")
        normalized.contains("каменный мост xiv века") -> localized("Каменный мост XIV века со статуями и панорамой Влтавы.", "14th-century stone bridge with statues and views of the Vltava.", "Puente de piedra del siglo XIV con estatuas y vistas del Moldava.", "Steinbrücke aus dem 14. Jahrhundert mit Statuen und Blick auf die Moldau.")
        normalized.contains("барочная площадь в малой стране") -> localized("Барочная площадь в Малой Стране у подножия Пражского града.", "Baroque square in Malá Strana below Prague Castle.", "Plaza barroca en Malá Strana, al pie del Castillo de Praga.", "Barocker Platz in der Kleinseite am Fuß der Prager Burg.")
        else -> null
    }
    if (known != null) return known
    return when (normalized) {
    "оживлённая площадь у западного входа в исторический центр мюнхена." -> localized("Оживлённая площадь у западного входа в исторический центр Мюнхена.", "Lively square at the western entrance to Munich's historic center.", "Plaza animada en la entrada oeste del centro histórico de Múnich.", "Belebter Platz am westlichen Eingang zur Münchner Altstadt.")
    "пешеходная улица с рождественскими витринами, гирляндами и праздничными украшениями." -> localized("Пешеходная улица с рождественскими витринами, гирляндами и праздничными украшениями.", "Pedestrian street with Christmas shop windows, garlands, and festive decorations.", "Calle peatonal con escaparates navideños, guirnaldas y adornos festivos.", "Fußgängerzone mit weihnachtlichen Schaufenstern, Girlanden und festlicher Dekoration.")
    "средневековые городские ворота, открывающие путь в старый город." -> localized("Средневековые городские ворота, открывающие путь в Старый город.", "Medieval city gates leading to the Old Town.", "Puertas medievales que conducen al casco antiguo.", "Mittelalterliches Stadttor zum Eingang in die Altstadt.")
    "главная площадь мюнхена и сердце праздничного старого города." -> localized("Главная площадь Мюнхена и сердце праздничного Старого города.", "Munich's main square and the heart of the festive Old Town.", "La plaza principal de Múnich y el corazón del casco antiguo festivo.", "Münchens Hauptplatz und das Herz der festlichen Altstadt.")
    "неоготическая ратуша с башней, часами и знаменитым глокеншпилем." -> localized("Неоготическая ратуша с башней, часами и знаменитым Глокеншпилем.", "Neo-Gothic town hall with a tower, clocks, and the famous Glockenspiel.", "Ayuntamiento neogótico con torre, relojes y el famoso Glockenspiel.", "Neugotisches Rathaus mit Turm, Uhr und dem berühmten Glockenspiel.")
    "главная рождественская ярмарка города с ремесленными лавками и баварскими угощениями." -> localized("Главная рождественская ярмарка города с ремесленными лавками и баварскими угощениями.", "The city's main Christmas market with craft stalls and Bavarian treats.", "El principal mercado navideño de la ciudad, con puestos de artesanía y especialidades bávaras.", "Der wichtigste Weihnachtsmarkt der Stadt mit Handwerksständen und bayerischen Spezialitäten.")
    "кафедральный собор и один из главных архитектурных символов мюнхена." -> localized("Кафедральный собор и один из главных архитектурных символов Мюнхена.", "A cathedral and one of Munich's main architectural landmarks.", "Catedral y uno de los principales símbolos arquitectónicos de Múnich.", "Kathedrale und eines der wichtigsten architektonischen Wahrzeichen Münchens.")
    "праздничная торговая улица, особенно красивая в вечерней подсветке." -> localized("Праздничная торговая улица, особенно красивая в вечерней подсветке.", "Festive shopping street, especially beautiful in the evening lights.", "Calle comercial festiva, especialmente bonita con la iluminación nocturna.", "Festliche Einkaufsstraße, besonders schön in der Abendbeleuchtung.")
    "уютная рождественская деревня во дворе мюнхенской резиденции." -> localized("Уютная рождественская деревня во дворе Мюнхенской резиденции.", "Cozy Christmas village in the courtyard of the Munich Residence.", "Pueblo navideño acogedor en el patio de la Residencia de Múnich.", "Behagliches Weihnachtsdorf im Innenhof der Münchner Residenz.")
    "парадная площадь перед баварской государственной оперой и резиденцией." -> localized("Парадная площадь перед Баварской государственной оперой и Резиденцией.", "Grand square in front of the Bavarian State Opera and the Residence.", "Plaza monumental frente a la Ópera Estatal de Baviera y la Residencia.", "Prachtplatz vor der Bayerischen Staatsoper und der Residenz.")
    "монументальная площадь на границе старого города и дворцового квартала." -> localized("Монументальная площадь на границе Старого города и дворцового квартала.", "Monumental square on the edge of the Old Town and palace district.", "Plaza monumental entre el casco antiguo y el barrio de los palacios.", "Monumentaler Platz am Rand der Altstadt und des Residenzviertels.")
    "аркада xix века, вдохновлённая флорентийской лоджией ланци." -> localized("Аркада XIX века, вдохновлённая флорентийской Лоджией Ланци.", "19th-century arcade inspired by Florence's Loggia dei Lanzi.", "Galería del siglo XIX inspirada en la Loggia dei Lanzi de Florencia.", "Arkade aus dem 19. Jahrhundert, inspiriert von der florentinischen Loggia dei Lanzi.")
    "барочная церковь с выразительным жёлтым фасадом и красивой вечерней подсветкой." -> localized("Барочная церковь с выразительным жёлтым фасадом и красивой вечерней подсветкой.", "Baroque church with a striking yellow façade and beautiful evening lighting.", "Iglesia barroca con una llamativa fachada amarilla y una hermosa iluminación nocturna.", "Barocke Kirche mit markanter gelber Fassade und schöner Abendbeleuchtung.")
    "спокойный придворный сад рядом с резиденцией, завершающий прогулку." -> localized("Спокойный придворный сад рядом с Резиденцией, завершающий прогулку.", "Peaceful court garden next to the Residence, the perfect end to the walk.", "Jardín cortesano tranquilo junto a la Residencia, un cierre perfecto para el paseo.", "Ruhiger Hofgarten neben der Residenz als schöner Abschluss des Spaziergangs.")
    else -> localizedSightNameByTerms(value)
}
}

@Composable
private fun localizedSightInfo(description: String, category: String): String {
    return if (description.isBlank()) localizedSightCategory(category) else localizedSightDescription(description)
}

@Composable
private fun localizedRestaurantNote(value: String): String {
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

private data class PhotoDateRange(val start: LocalDate, val end: LocalDate)

private val PhotoMonthNames = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")

private fun photoCityKey(city: String): String = city.substringBefore(',').trim().lowercase(Locale.ROOT)

private fun samePhotoCity(left: String, right: String): Boolean = photoCityKey(left) == photoCityKey(right)

private fun parsePhotoDateRange(value: String): PhotoDateRange? {
    val dates = Regex("""\d{4}-\d{2}-\d{2}""").findAll(value).mapNotNull { match ->
        runCatching { LocalDate.parse(match.value) }.getOrNull()
    }.toList()
    val start = dates.firstOrNull() ?: return null
    return PhotoDateRange(start, dates.getOrElse(1) { start })
}

private fun parsePhotoTripStart(value: String): LocalDate? {
    val iso = Regex("""\d{4}-\d{2}-\d{2}""").find(value)?.value
    if (iso != null) return runCatching { LocalDate.parse(iso) }.getOrNull()
    val match = Regex("""(\d{1,2})\s+([A-Za-zА-Яа-яЁё]+)\s+(\d{4})""").find(value) ?: return null
    val month = when (match.groupValues[2].lowercase(Locale.ROOT).take(4)) {
        "янва" -> 1
        "февр" -> 2
        "март" -> 3
        "апре" -> 4
        "мая", "май" -> 5
        "июн" -> 6
        "июл" -> 7
        "авгу" -> 8
        "сент" -> 9
        "октя" -> 10
        "нояб" -> 11
        "дека" -> 12
        else -> return null
    }
    return runCatching { LocalDate.of(match.groupValues[3].toInt(), month, match.groupValues[1].toInt()) }.getOrNull()
}

private fun photoGroupDay(city: String, overview: TripOverview, fallback: Int): Int {
    val route = overview.routeLegs.withIndex().firstOrNull { (_, leg) ->
        samePhotoCity(leg.from, city) || samePhotoCity(leg.to, city)
    }
    val routeDay = route?.value?.dayId?.filter { it in '0'..'9' }?.toIntOrNull()
        ?.takeIf { it in 1..99 }
        ?: route?.index?.plus(1)
    val sightDay = overview.sights.filter { samePhotoCity(it.city, city) && it.walkDay > 0 }
        .minOfOrNull { it.walkDay }
    return routeDay ?: sightDay ?: fallback
}

private fun photoGroupDateRange(city: String, overview: TripOverview, fallbackDay: Int): PhotoDateRange? {
    val stayRanges = overview.accommodations
        .filter { samePhotoCity(it.city, city) }
        .mapNotNull { parsePhotoDateRange(it.dates) }
    if (stayRanges.isNotEmpty()) {
        return PhotoDateRange(stayRanges.minOf { it.start }, stayRanges.maxOf { it.end })
    }
    val tripStart = parsePhotoTripStart(overview.dates) ?: return null
    val day = photoGroupDay(city, overview, fallbackDay).coerceAtLeast(1)
    val date = tripStart.plusDays((day - 1).toLong())
    return PhotoDateRange(date, date)
}

private fun formatPhotoDateRange(range: PhotoDateRange, language: String): String {
    val monthNames = when (normalizeLanguage(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> PhotoMonthNames
    }
    val startMonth = monthNames[range.start.monthValue - 1]
    val endMonth = monthNames[range.end.monthValue - 1]
    return when {
        range.start == range.end -> "${range.start.dayOfMonth} $startMonth"
        range.start.year == range.end.year && range.start.monthValue == range.end.monthValue -> "${range.start.dayOfMonth}–${range.end.dayOfMonth} $startMonth"
        else -> "${range.start.dayOfMonth} $startMonth – ${range.end.dayOfMonth} $endMonth"
    }
}

@Composable
private fun OdysseyBackArrow(iconSize: Dp = 22.dp, color: Color = Color(0xFF1B1B22)) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.2.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(color, point(19f, 12f), point(5f, 12f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, point(12f, 19f), point(5f, 12f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, point(12f, 5f), point(5f, 12f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OdysseyChevronDown(iconSize: Dp, color: Color = OdysseyPurple) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.8.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(color, point(6f, 9f), point(12f, 15f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, point(12f, 15f), point(18f, 9f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OdysseyChevronUp(iconSize: Dp, color: Color = OdysseyPurple) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.8.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(color, point(6f, 15f), point(12f, 9f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, point(12f, 9f), point(18f, 15f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OdysseyPlusIcon(iconSize: Dp = 17.dp, color: Color = OdysseyPurple) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.2.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(color, point(12f, 5f), point(12f, 19f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, point(5f, 12f), point(19f, 12f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OdysseyLocationIcon(iconSize: Dp = 15.dp, color: Color = OdysseyText) {
    Canvas(Modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val path = Path().apply {
            moveTo(20f * sx, 10f * sy)
            cubicTo(20f * sx, 16f * sy, 12f * sx, 22f * sy, 12f * sx, 22f * sy)
            cubicTo(12f * sx, 22f * sy, 4f * sx, 16f * sy, 4f * sx, 10f * sy)
            cubicTo(4f * sx, 5.6f * sy, 7.6f * sx, 2f * sy, 12f * sx, 2f * sy)
            cubicTo(16.4f * sx, 2f * sy, 20f * sx, 5.6f * sy, 20f * sx, 10f * sy)
            close()
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, radius = 3f * sx, center = Offset(12f * sx, 10f * sy), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
private fun OdysseyFilterIcon(iconSize: Dp = 15.dp, color: Color = OdysseyLabel) {
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(color, point(4f, 6f), point(20f, 6f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, point(7f, 12f), point(17f, 12f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, point(10f, 18f), point(14f, 18f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OdysseyUtensilsIcon(iconSize: Dp = 15.dp, color: Color = OdysseyPurple) {
    Canvas(Modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val path = Path().apply {
            moveTo(3f * sx, 2f * sy)
            lineTo(3f * sx, 9f * sy)
            cubicTo(3f * sx, 10.1f * sy, 3.9f * sx, 11f * sy, 5f * sx, 11f * sy)
            cubicTo(6.1f * sx, 11f * sy, 7f * sx, 10.1f * sy, 7f * sx, 9f * sy)
            lineTo(7f * sx, 2f * sy)
            moveTo(5f * sx, 2f * sy)
            lineTo(5f * sx, 22f * sy)
            moveTo(17f * sx, 2f * sy)
            lineTo(17f * sx, 12f * sy)
            cubicTo(19f * sx, 12f * sy, 21f * sx, 10.5f * sy, 21f * sx, 7f * sy)
            cubicTo(21f * sx, 3.5f * sy, 19f * sx, 2f * sy, 17f * sx, 2f * sy)
            lineTo(17f * sx, 12f * sy)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
    }
}

@Composable
private fun OdysseyExternalLinkIcon(iconSize: Dp = 17.dp, color: Color = OdysseyPurple, modifier: Modifier = Modifier) {
    Canvas(modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = 2.2.dp.toPx()
        val frame = Path().apply {
            moveTo(18f * sx, 13f * sy)
            lineTo(18f * sx, 19f * sy)
            cubicTo(18f * sx, 20.1f * sy, 17.1f * sx, 21f * sy, 16f * sx, 21f * sy)
            lineTo(5f * sx, 21f * sy)
            cubicTo(3.9f * sx, 21f * sy, 3f * sx, 20.1f * sy, 3f * sx, 19f * sy)
            lineTo(3f * sx, 8f * sy)
            cubicTo(3f * sx, 6.9f * sy, 3.9f * sx, 6f * sy, 5f * sx, 6f * sy)
            lineTo(11f * sx, 6f * sy)
        }
        drawPath(frame, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawLine(color, Offset(15f * sx, 3f * sy), Offset(21f * sx, 3f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(21f * sx, 3f * sy), Offset(21f * sx, 9f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(10f * sx, 14f * sy), Offset(21f * sx, 3f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OdysseyEditIcon(iconSize: Dp = 15.dp, color: Color = OdysseyPurple, modifier: Modifier = Modifier) {
    Canvas(modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = 2.1.dp.toPx()
        drawLine(
            color,
            Offset(12f * sx, 20f * sy),
            Offset(21f * sx, 20f * sy),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val pencil = Path().apply {
            moveTo(16.5f * sx, 3.5f * sy)
            cubicTo(17.3f * sx, 2.7f * sy, 18.7f * sx, 2.7f * sy, 19.5f * sx, 3.5f * sy)
            lineTo(20.5f * sx, 4.5f * sy)
            cubicTo(21.3f * sx, 5.3f * sy, 21.3f * sx, 6.7f * sy, 20.5f * sx, 7.5f * sy)
            lineTo(7f * sx, 21f * sy)
            lineTo(3f * sx, 22f * sy)
            lineTo(4f * sx, 18f * sy)
            close()
        }
        drawPath(pencil, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
private fun OdysseyCalendarIcon(iconSize: Dp = 14.dp, color: Color = OdysseySubtext) {
    Canvas(Modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = 2.2.dp.toPx()
        val frame = Path().apply {
            moveTo(5f * sx, 4f * sy)
            lineTo(19f * sx, 4f * sy)
            cubicTo(20.1f * sx, 4f * sy, 21f * sx, 4.9f * sy, 21f * sx, 6f * sy)
            lineTo(21f * sx, 20f * sy)
            cubicTo(21f * sx, 21.1f * sy, 20.1f * sx, 22f * sy, 19f * sx, 22f * sy)
            lineTo(5f * sx, 22f * sy)
            cubicTo(3.9f * sx, 22f * sy, 3f * sx, 21.1f * sy, 3f * sx, 20f * sy)
            lineTo(3f * sx, 6f * sy)
            cubicTo(3f * sx, 4.9f * sy, 3.9f * sx, 4f * sy, 5f * sx, 4f * sy)
        }
        drawPath(frame, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawLine(color, Offset(8f * sx, 2f * sy), Offset(8f * sx, 6f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(16f * sx, 2f * sy), Offset(16f * sx, 6f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(3f * sx, 10f * sy), Offset(21f * sx, 10f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun OdysseyExpandIcon(iconSize: Dp = 15.dp, color: Color = OdysseyText) {
    Canvas(Modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = 2.2.dp.toPx()
        drawLine(color, Offset(15f * sx, 3f * sy), Offset(21f * sx, 3f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(21f * sx, 3f * sy), Offset(21f * sx, 9f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(9f * sx, 21f * sy), Offset(3f * sx, 21f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(3f * sx, 21f * sy), Offset(3f * sx, 15f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(21f * sx, 3f * sy), Offset(14f * sx, 10f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(color, Offset(3f * sx, 21f * sy), Offset(10f * sx, 14f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
private fun contentTextColor() = if (LocalDarkTheme.current) Color(0xFFF5F6FA) else OdysseyText

@Composable
private fun secondaryTextColor() = if (LocalDarkTheme.current) Color(0xFFBEC1CC) else OdysseySubtext

@Composable
private fun cardSurfaceColor() = if (LocalDarkTheme.current) Color(0xFF20222E) else Color.White

@Composable
private fun secondarySurfaceColor() = if (LocalDarkTheme.current) Color(0xFF2B2E3B) else Color(0xFFF0F0F4)

@Composable
private fun SurfaceEmptyMedia(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.background(if (LocalDarkTheme.current) Color(0xFF303342) else Color(0xFFEDEBF3)),
    ) {
        Icon(icon, contentDescription = null, tint = if (LocalDarkTheme.current) Color(0xFF9D96C9) else Color(0xFFAAA5B9), modifier = Modifier.size(28.dp))
    }
}

@Composable
fun OdysseyApp() {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    var darkTheme by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("RU") }
    var authReady by remember { mutableStateOf(false) }
    var hasSession by remember { mutableStateOf(false) }
    var rememberSession by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        hasSession = SupabaseProvider.restorePersistentSession()
        authReady = true
        if (hasSession) {
            runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadProfile() }.getOrNull()?.let { profile ->
                darkTheme = profile.darkTheme
                language = normalizeLanguage(profile.language)
            }
        }
    }

    LaunchedEffect(authReady, hasSession, currentRoute) {
        if (!authReady || currentRoute == null) return@LaunchedEffect
        if (hasSession && currentRoute == "foundation") {
            navController.navigate("trips") { popUpTo("foundation") { inclusive = true } }
        } else if (!hasSession && currentRoute != "foundation") {
            navController.navigate("foundation") { popUpTo(0) { inclusive = true } }
        }
    }

    LaunchedEffect(authReady) {
        if (!authReady) return@LaunchedEffect
        val authClients = listOf(SupabaseProvider.sessionOnlyClient, SupabaseProvider.persistentClient)
        authClients.forEach { client ->
            launch {
                client.auth.sessionStatus.collect { status ->
                    val isActiveClient = SupabaseProvider.clientForCurrentAuthFlow() === client
                    val sessionLost = isActiveClient && (
                        status is SessionStatus.RefreshFailure ||
                            status is SessionStatus.NotAuthenticated
                        )
                    if (sessionLost) {
                        hasSession = false
                    }
                }
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme, LocalLanguage provides language) {
    MaterialTheme {
        Surface(color = if (darkTheme) Color(0xFF141416) else OdysseyBackground) {
            if (!authReady) {
                TripOverviewLoading()
            } else NavHost(navController = navController, startDestination = "foundation") {
                composable("foundation") {
                    AuthScreen(
                        rememberSession = rememberSession,
                        onRememberSessionChange = { rememberSession = it },
                        onAuthenticated = {
                        hasSession = true
                        navController.navigate("trips")
                        },
                    )
                }
                composable("trips") {
                    MyTripsScreen(
                        onTripClick = { navController.navigate("trip/$it") },
                        onNewTrip = { navController.navigate("create-trip") },
                        onLogout = {
                            hasSession = false
                            navController.navigate("foundation") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onCatalog = { navController.navigate("catalog") },
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme },
                        onThemeSet = { darkTheme = it },
                        language = language,
                        onLanguageChange = { language = normalizeLanguage(it) },
                    )
                }
                composable("catalog") { RouteCatalogScreen(onBack = { navController.popBackStack() }, onUseTemplate = { navController.navigate("create-trip/$it") }) }
                composable("create-trip") {
                    CreateTripScreen(
                        onBack = { navController.popBackStack() },
                        onCreated = {
                            navController.navigate("trips") {
                                popUpTo("trips") { inclusive = true }
                            }
                        },
                    )
                }
                composable("create-trip/{template}") { entry ->
                    CreateTripScreen(
                        template = entry.arguments?.getString("template"),
                        onBack = { navController.popBackStack() },
                        onCreated = { navController.navigate("trips") { popUpTo("trips") { inclusive = true } } },
                    )
                }
                composable("trip/{tripId}") { entry ->
                    TripOverviewScreen(
                        tripId = entry.arguments?.getString("tripId").orEmpty(),
                        onBack = { navController.popBackStack() },
                    )
                }
            }
        }
    }
    }
}

@Composable
private fun AuthScreen(
    rememberSession: Boolean,
    onRememberSessionChange: (Boolean) -> Unit,
    onAuthenticated: () -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    val context = LocalContext.current
    val language = LocalLanguage.current
    var isRegistration by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    fun messageText(ru: String, en: String, es: String, de: String) = localized(language, ru, en, es, de)

    fun submit() {
        if (email.isBlank() || password.isBlank() || (isRegistration && name.isBlank())) {
            message = messageText("Заполните обязательные поля", "Complete the required fields", "Complete los campos obligatorios", "Füllen Sie die Pflichtfelder aus")
            return
        }
        if (isRegistration && password != repeatPassword) {
            message = messageText("Пароли не совпадают", "Passwords do not match", "Las contraseñas no coinciden", "Passwörter stimmen nicht überein")
            return
        }
        scope.launch {
            isLoading = true
            message = null
            runCatching {
                SupabaseProvider.selectSessionPersistence(rememberSession)
                val auth = SupabaseProvider.clientForCurrentAuthFlow().auth
                if (isRegistration) {
                    auth.signUpWith(Email) {
                        this.email = email.trim()
                        this.password = password
                        data = buildJsonObject { put("full_name", name.trim()) }
                    }
                } else {
                    auth.signInWith(Email) {
                        this.email = email.trim()
                        this.password = password
                    }
                }
            }.onSuccess {
                if (isRegistration) {
                    message = messageText("Проверьте e-mail для подтверждения", "Check your email to confirm", "Revise su correo para confirmar", "Prüfen Sie Ihre E-Mail zur Bestätigung")
                } else {
                    onAuthenticated()
                }
            }.onFailure {
                message = messageText("Не удалось выполнить запрос", "Could not complete the request", "No se pudo completar la solicitud", "Anfrage konnte nicht ausgeführt werden")
            }
            isLoading = false
        }
    }

    fun signInWithGoogle() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            message = messageText("Google OAuth не настроен", "Google OAuth is not configured", "Google OAuth no está configurado", "Google OAuth ist nicht eingerichtet")
            return
        }
        scope.launch {
            isLoading = true
            message = null
            runCatching {
                SupabaseProvider.selectSessionPersistence(rememberSession)
                val option = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(BuildConfig.GOOGLE_WEB_CLIENT_ID)
                    .setAutoSelectEnabled(false)
                    .build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = CredentialManager.create(context).getCredential(context, request).credential
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                SupabaseProvider.clientForCurrentAuthFlow().auth.signInWith(IDToken) {
                    idToken = googleCredential.idToken
                    provider = Google
                }
            }.onSuccess { onAuthenticated() }.onFailure {
                message = messageText("Не удалось войти через Google", "Google sign-in failed", "No se pudo iniciar sesión con Google", "Google-Anmeldung fehlgeschlagen")
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (darkTheme) Color(0xFF141416) else OdysseyBackground)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .padding(start = 24.dp, top = 40.dp, end = 24.dp, bottom = 28.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = "O",
                color = Color.White,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 19.sp,
                modifier = Modifier
                    .background(
                        Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF8E7BF5))),
                        RoundedCornerShape(12.dp),
                    )
                    .padding(horizontal = 12.dp, vertical = 7.dp),
            )
            Text(
                text = "Одиссея",
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 20.sp,
                modifier = Modifier.padding(start = 11.dp),
            )
        }

        Spacer(Modifier.height(34.dp))
        Text(
            text = if (isRegistration) localized("Создать аккаунт", "Create account", "Crear cuenta", "Konto erstellen") else localized("С возвращением", "Welcome back", "Bienvenido de nuevo", "Willkommen zurück"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 30.sp,
            lineHeight = 32.sp,
        )
        Text(
            text = if (isRegistration) localized("Пара шагов - и планируем поездку", "A few steps and you can plan your trip", "Unos pasos y podrá planificar su viaje", "Noch ein paar Schritte bis zur Reiseplanung") else localized("Войдите, чтобы продолжить планирование", "Sign in to continue planning", "Inicie sesión para continuar planificando", "Melden Sie sich an, um weiterzuplanen"),
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp),
        )

        Spacer(Modifier.height(28.dp))
        if (isRegistration) {
            AuthField(localized("Имя", "Name", "Nombre", "Name"), localized("Как вас зовут", "What is your name", "Cómo se llama", "Wie heißen Sie"), name) { name = it }
            Spacer(Modifier.height(14.dp))
        }
        AuthField(localized("E-mail", "E-mail", "Correo electrónico", "E-Mail"), "you@example.com", email) { email = it }
        Spacer(Modifier.height(14.dp))
        AuthField(localized("Пароль", "Password", "Contraseña", "Passwort"), "••••••••", password, password = true) { password = it }
        if (isRegistration) {
            Spacer(Modifier.height(14.dp))
            AuthField(localized("Повторите пароль", "Repeat password", "Repita la contraseña", "Passwort wiederholen"), "••••••••", repeatPassword, password = true) { repeatPassword = it }
        } else {
            Text(
                text = localized("Забыли пароль?", "Forgot password?", "¿Olvidó su contraseña?", "Passwort vergessen?"),
                color = OdysseyPurple,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable {
                        if (email.isBlank()) {
                            message = messageText("Введите e-mail для восстановления", "Enter your email to reset the password", "Introduzca su e-mail para restablecer la contraseña", "Geben Sie Ihre E-Mail zum Zurücksetzen des Passworts ein")
                        } else {
                            scope.launch {
                                isLoading = true
                                runCatching {
                                    SupabaseProvider.clientForCurrentAuthFlow().auth.resetPasswordForEmail(email.trim(), redirectUrl = "https://travelplanner.muntim.ru")
                                }.onSuccess {
                                    message = messageText("Письмо для восстановления отправлено", "Password reset email sent", "Correo de restablecimiento enviado", "E-Mail zum Zurücksetzen gesendet")
                                }.onFailure {
                                    message = it.message ?: messageText("Не удалось отправить письмо", "Could not send reset email", "No se pudo enviar el correo", "E-Mail konnte nicht gesendet werden")
                                }
                                isLoading = false
                            }
                        }
                    },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 14.dp).clickable { onRememberSessionChange(!rememberSession) },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(20.dp).background(if (rememberSession) OdysseyPurple else Color.Transparent, RoundedCornerShape(6.dp)).drawBehind {
                    if (!rememberSession) drawRoundRect(Color(0xFFBDBCC6), style = Stroke(width = 1.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                },
            ) {
                if (rememberSession) Text("✓", color = Color.White, fontWeight = FontWeight.W800, fontSize = 13.sp)
            }
            Text(
                text = localized("Запомнить меня", "Remember me", "Recordarme", "Angemeldet bleiben"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 9.dp),
            )
        }

        Button(
            onClick = ::submit,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent),
            shape = RoundedCornerShape(15.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(56.dp)
                .background(
                    Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))),
                    RoundedCornerShape(15.dp),
                ),
        ) {
            Text(
                text = if (isLoading) localized("Подождите…", "Please wait…", "Espere…", "Bitte warten…") else if (isRegistration) localized("Создать аккаунт", "Create account", "Crear cuenta", "Konto erstellen") else localized("Войти", "Sign in", "Iniciar sesión", "Anmelden"),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 16.sp,
            )
        }
        if (message != null) {
            Text(
                text = message!!,
                color = if (message == "Вход выполнен" || message?.startsWith("Проверьте") == true || message?.contains("отправлено", true) == true || message?.contains("sent", true) == true || message?.contains("enviado", true) == true || message?.contains("gesendet", true) == true) Color(0xFF22B07D) else Color(0xFFE0524B),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 22.dp),
        ) {
            Spacer(Modifier.weight(1f).height(1.dp).background(OdysseyBorder))
            Text(
                text = localized("или", "or", "o", "oder"),
                color = Color(0xFFB6B6BE),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f).height(1.dp).background(OdysseyBorder))
        }
        Button(
            onClick = ::signInWithGoogle,
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cardSurfaceColor(), contentColor = contentTextColor()),
            modifier = Modifier.fillMaxWidth().height(53.dp),
        ) {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.W800, fontSize = 18.sp)
            Text(
                text = localized("Продолжить с Google", "Continue with Google", "Continuar con Google", "Mit Google fortfahren"),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
        ) {
            Text(
                text = if (isRegistration) localized("Уже есть аккаунт?", "Already have an account?", "¿Ya tiene una cuenta?", "Bereits ein Konto?") else localized("Нет аккаунта?", "No account?", "¿No tiene cuenta?", "Noch kein Konto?"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 14.sp,
            )
            Text(
                text = if (isRegistration) " " + localized("Войти", "Sign in", "Iniciar sesión", "Anmelden") else " " + localized("Зарегистрироваться", "Sign up", "Registrarse", "Registrieren"),
                color = OdysseyPurple,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    isRegistration = !isRegistration
                    message = null
                },
            )
        }
    }
}

@Composable
private fun AuthField(
    label: String,
    placeholder: String,
    value: String,
    password: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    val surface = cardSurfaceColor()
    val border = if (darkTheme) Color(0xFF3A3D4C) else OdysseyBorder
    val text = contentTextColor()
    Text(
        text = label,
        color = if (darkTheme) Color(0xFFF5F6FA) else Color(0xFF3A3A42),
        fontFamily = Manrope,
        fontWeight = FontWeight.W800,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontFamily = Manrope, color = Color(0xFFB6B6BE)) },
        singleLine = true,
        visualTransformation = if (password) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        shape = RoundedCornerShape(14.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = text,
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 15.sp,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = OdysseyPurple,
            unfocusedBorderColor = border,
            focusedContainerColor = surface,
            unfocusedContainerColor = surface,
            cursorColor = OdysseyPurple,
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun MyTripsScreen(onTripClick: (String) -> Unit, onNewTrip: () -> Unit, onLogout: () -> Unit, onCatalog: () -> Unit, darkTheme: Boolean, onThemeToggle: () -> Unit, onThemeSet: (Boolean) -> Unit, language: String, onLanguageChange: (String) -> Unit) {
    var filter by remember { mutableStateOf("all") }
    var loading by remember { mutableStateOf(true) }
    var trips by remember { mutableStateOf<List<TripCard>>(emptyList()) }
    var loadFailed by remember { mutableStateOf(false) }
    var editingTrip by remember { mutableStateOf<TripCard?>(null) }
    var menuOpen by remember { mutableStateOf(false) }
    var accountMenuOpen by remember { mutableStateOf(false) }
    var profileEmail by remember { mutableStateOf("") }
    var profileAvatarUrl by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var passwordEditorOpen by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var repeatedNewPassword by remember { mutableStateOf("") }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    fun reloadTrips() {
        scope.launch {
            loading = true
            loadFailed = false
            runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadTrips() }
                .onSuccess { trips = it }
                .onFailure { loadFailed = true }
            loading = false
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            accountMessage = null
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Не удалось прочитать изображение")
                val repository = AccountRepository(SupabaseProvider.clientForCurrentAuthFlow())
                val url = repository.uploadProfilePhoto(bytes)
                repository.updateProfile(url, notificationsEnabled)
                url
            }.onSuccess { profileAvatarUrl = it; accountMessage = localized(language, "Фото профиля обновлено", "Profile photo updated", "Foto de perfil actualizada", "Profilbild aktualisiert") }
                .onFailure { accountMessage = it.message ?: localized(language, "Не удалось загрузить фото", "Could not upload photo", "No se pudo cargar la foto", "Foto konnte nicht hochgeladen werden") }
        }
    }

    LaunchedEffect(Unit) {
        profileEmail = runCatching {
            SupabaseProvider.clientForCurrentAuthFlow().auth.currentSessionOrNull()?.user?.email.orEmpty()
        }.getOrDefault("")
        runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadProfile() }.getOrNull()?.let { profile ->
            profileAvatarUrl = profile.avatarUrl
            notificationsEnabled = profile.notificationsEnabled
            onLanguageChange(normalizeLanguage(profile.language))
            onThemeSet(profile.darkTheme)
        }
    }

    LaunchedEffect(Unit) { reloadTrips() }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) reloadTrips()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val upcoming = trips.filter {
        !it.status.contains("чернов", ignoreCase = true) &&
            !it.status.contains("заверш", ignoreCase = true) &&
            !it.status.contains("прошед", ignoreCase = true)
    }
    val drafts = trips.filter { it.status.contains("чернов", ignoreCase = true) }
    val completed = trips.filter {
        it.status.contains("заверш", ignoreCase = true) ||
            it.status.contains("прошед", ignoreCase = true)
    }
    val visibleTrips = when (filter) {
        "upcoming" -> upcoming
        "drafts" -> drafts
        "completed" -> completed
        else -> trips
    }
    val filters = listOf(
        "all" to localized("Все · ${trips.size}", "All · ${trips.size}", "Todos · ${trips.size}", "Alle · ${trips.size}"),
        "upcoming" to localized("Предстоящие · ${upcoming.size}", "Upcoming · ${upcoming.size}", "Próximos · ${upcoming.size}", "Bevorstehend · ${upcoming.size}"),
        "drafts" to localized("Черновики · ${drafts.size}", "Drafts · ${drafts.size}", "Borradores · ${drafts.size}", "Entwürfe · ${drafts.size}"),
        "completed" to localized("Завершённые · ${completed.size}", "Completed · ${completed.size}", "Completados · ${completed.size}", "Abgeschlossen · ${completed.size}"),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (darkTheme) Color(0xFF141416) else OdysseyBackground)
                .padding(WindowInsets.statusBars.asPaddingValues()),
        ) {
        Box(modifier = Modifier.fillMaxWidth().height(54.dp)) {
            Icon(
                Icons.Outlined.Menu,
                contentDescription = localized("Открыть меню", "Open menu", "Abrir menú", "Menü öffnen"),
                tint = contentTextColor(),
                modifier = Modifier.align(Alignment.CenterStart).padding(start = 16.dp).size(24.dp).clickable { menuOpen = !menuOpen },
            )
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.Center)) {
                Text(
                    text = "O",
                    color = Color.White,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF8E7BF5))), RoundedCornerShape(8.dp))
                        .padding(horizontal = 9.dp, vertical = 5.dp),
                )
                Text(
                    text = "Одиссея",
                    color = OdysseyText,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 16.sp,
                    modifier = Modifier.padding(start = 9.dp),
                )
            }
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = localized("Мои путешествия", "My trips", "Mis viajes", "Meine Reisen"),
                    color = OdysseyText,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 32.sp,
                    lineHeight = 33.sp,
                    modifier = Modifier.padding(top = 22.dp),
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    filters.forEach { (key, label) ->
                        val selected = filter == key
                        Button(
                            onClick = { filter = key },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) OdysseyPurple else cardSurfaceColor(),
                                contentColor = if (selected) Color.White else contentTextColor(),
                            ),
                            border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, OdysseyBorder),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 4.dp else 0.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 15.dp, vertical = 9.dp),
                            modifier = Modifier.height(38.dp),
                        ) {
                            Text(label, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            if (loading) {
                item { TripsLoadingCard() }
            } else if (loadFailed) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Explore,
                        title = localized("Не удалось загрузить путешествия", "Could not load trips", "No se pudieron cargar los viajes", "Reisen konnten nicht geladen werden"),
                        body = localized("Проверьте соединение и попробуйте ещё раз", "Check your connection and try again", "Compruebe la conexión e inténtelo de nuevo", "Prüfen Sie die Verbindung und versuchen Sie es erneut"),
                        action = localized("Повторить", "Retry", "Reintentar", "Erneut versuchen"),
                        onAction = ::reloadTrips,
                    )
                }
            } else if (visibleTrips.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Explore,
                        title = localized("Здесь появятся ваши путешествия", "Your trips will appear here", "Aquí aparecerán sus viajes", "Hier erscheinen Ihre Reisen"),
                        body = localized("Создайте первую поездку с нуля или выберите готовый маршрут", "Create your first trip from scratch or choose a ready route", "Cree su primer viaje desde cero o elija una ruta", "Erstellen Sie Ihre erste Reise oder wählen Sie eine fertige Route"),
                        action = localized("Создать путешествие", "Create trip", "Crear viaje", "Reise erstellen"),
                        onAction = onNewTrip,
                    )
                }
            } else {
                items(visibleTrips, key = { it.id }) { trip -> TripListCard(trip, onTripClick) { editingTrip = trip } }
            }
            item { NewTripCard(onNewTrip) }
        }
        }

        if (editingTrip != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { editingTrip = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x730F0F19)),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 24.dp)
                            .heightIn(max = 700.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(cardSurfaceColor())
                            .verticalScroll(rememberScrollState()),
                    ) {
                        EditTripPanel(editingTrip!!, onClose = { editingTrip = null }, onSaved = { updated ->
                            trips = trips.map { if (it.id == updated.id) updated else it }
                            editingTrip = null
                        })
                    }
                }
            }
        }

        if (menuOpen) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0x66000000))
                        .clickable { menuOpen = false },
                )
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(312.dp)
                        .background(cardSurfaceColor())
                        .padding(start = 2.dp, top = 20.dp, end = 16.dp, bottom = 68.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 8.dp)) {
                        Text(
                            text = "O",
                            color = Color.White,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 16.sp,
                            modifier = Modifier.background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF8E7BF5))), RoundedCornerShape(10.dp)).padding(horizontal = 11.dp, vertical = 7.dp),
                        )
                        Text("Одиссея", color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 22.sp, modifier = Modifier.padding(start = 10.dp))
                    }
                    Button(
                        onClick = { menuOpen = false; onNewTrip() },
                        colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth().padding(top = 22.dp).height(52.dp),
                    ) {
                        Text("+  " + localized("Новое путешествие", "New trip", "Nuevo viaje", "Neue Reise"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp)
                    }
                    Text(localized("НАВИГАЦИЯ", "NAVIGATION", "NAVEGACIÓN", "NAVIGATION"), color = Color(0xFFA4A4AF), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.5.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 28.dp, start = 6.dp, bottom = 12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().background(Color(0xFFF0EDFF), RoundedCornerShape(12.dp)).clickable { accountMenuOpen = false; menuOpen = false }.padding(horizontal = 14.dp, vertical = 13.dp),
                    ) {
                        Text("◇", color = OdysseyPurple, fontSize = 22.sp)
                        Text(localized("Мои путешествия", "My trips", "Mis viajes", "Meine Reisen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable { menuOpen = false; onCatalog() }.padding(horizontal = 14.dp, vertical = 16.dp),
                    ) {
                        Text("+", color = Color(0xFF8B8B96), fontSize = 22.sp)
                        Text(localized("Каталог маршрутов", "Route catalog", "Catálogo de rutas", "Routenkatalog"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 16.sp, modifier = Modifier.padding(start = 12.dp))
                    }
                    Spacer(Modifier.weight(1f))
                    if (accountMenuOpen) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp).shadow(8.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x18000000), spotColor = Color(0x18000000)).border(1.dp, OdysseyBorder, RoundedCornerShape(18.dp)).clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(16.dp),
                        ) {
                            Text(localized("ЯЗЫК ИНТЕРФЕЙСА", "INTERFACE LANGUAGE", "IDIOMA DE LA INTERFAZ", "SPRACHE DER OBERFLÄCHE"), color = Color(0xFFA4A4AF), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 9.sp, letterSpacing = 0.7.sp)
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(5.dp),
                                modifier = Modifier.fillMaxWidth().height(42.dp).background(secondarySurfaceColor(), RoundedCornerShape(11.dp)).padding(4.dp),
                            ) {
                                listOf("RU", "EN", "ES", "DE").forEach { code ->
                                    val selected = language == code
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier.weight(1f).fillMaxHeight().background(if (selected) OdysseyPurple else Color.Transparent, RoundedCornerShape(8.dp)).clickable {
                                            onLanguageChange(code)
                                            scope.launch {
                                                runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateProfile(profileAvatarUrl, notificationsEnabled, language = code, darkTheme = darkTheme) }
                                                    .onFailure { accountMessage = it.message }
                                            }
                                        },
                                    ) {
                                        Text(code, color = if (selected) Color.White else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                                    }
                                }
                            }
                            AccountMenuItem(Icons.Outlined.Image, localized("Сменить фото профиля", "Change profile photo", "Cambiar foto de perfil", "Profilbild ändern")) { photoPicker.launch("image/*") }
                            AccountMenuItem(Icons.Outlined.Lock, localized("Сменить пароль", "Change password", "Cambiar contraseña", "Passwort ändern")) { passwordEditorOpen = !passwordEditorOpen; accountMessage = null }
                            if (passwordEditorOpen) {
                                AuthField(localized("Новый пароль", "New password", "Nueva contraseña", "Neues Passwort"), "••••••••", newPassword, password = true) { newPassword = it }
                                Spacer(Modifier.height(8.dp))
                                AuthField(localized("Повторите пароль", "Repeat password", "Repita la contraseña", "Passwort wiederholen"), "••••••••", repeatedNewPassword, password = true) { repeatedNewPassword = it }
                                Button(onClick = {
                                    if (newPassword != repeatedNewPassword) {
                                        accountMessage = localized(language, "Пароли не совпадают", "Passwords do not match", "Las contraseñas no coinciden", "Passwörter stimmen nicht überein")
                                    } else scope.launch {
                                        runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).changePassword(newPassword) }
                                            .onSuccess { newPassword = ""; repeatedNewPassword = ""; passwordEditorOpen = false; accountMessage = localized(language, "Пароль обновлён", "Password updated", "Contraseña actualizada", "Passwort aktualisiert") }
                                            .onFailure { accountMessage = it.message ?: localized(language, "Не удалось сменить пароль", "Could not change password", "No se pudo cambiar la contraseña", "Passwort konnte nicht geändert werden") }
                                    }
                                }, colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                                    Text(localized("Сохранить пароль", "Save password", "Guardar contraseña", "Passwort speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
                                }
                            }
                            Spacer(Modifier.fillMaxWidth().height(1.dp).background(OdysseyBorder).padding(top = 8.dp))
                            AccountMenuItem(Icons.Outlined.NotificationsNone, localized("Уведомления", "Notifications", "Notificaciones", "Benachrichtigungen")) {
                                scope.launch {
                                    val enabled = !notificationsEnabled
                                    runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateProfile(profileAvatarUrl, enabled) }
                                        .onSuccess { notificationsEnabled = enabled; accountMessage = localized(language, if (enabled) "Уведомления включены" else "Уведомления выключены", if (enabled) "Notifications enabled" else "Notifications disabled", if (enabled) "Notificaciones activadas" else "Notificaciones desactivadas", if (enabled) "Benachrichtigungen aktiviert" else "Benachrichtigungen deaktiviert") }
                                        .onFailure { accountMessage = it.message ?: localized(language, "Не удалось сохранить настройку", "Could not save setting", "No se pudo guardar el ajuste", "Einstellung konnte nicht gespeichert werden") }
                                }
                            }
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
                                Icon(Icons.Outlined.DarkMode, contentDescription = null, tint = OdysseyPurple, modifier = Modifier.size(21.dp))
                                Text(localized("Тёмная тема", "Dark theme", "Tema oscuro", "Dunkles Thema"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 16.sp, modifier = Modifier.weight(1f).padding(start = 11.dp))
                                Box(modifier = Modifier.width(43.dp).height(25.dp).background(if (darkTheme) OdysseyPurple else Color(0xFFD5D6DE), RoundedCornerShape(14.dp)).clickable {
                                    val nextTheme = !darkTheme
                                    onThemeToggle()
                                    scope.launch {
                                        runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateProfile(profileAvatarUrl, notificationsEnabled, language = language, darkTheme = nextTheme) }
                                            .onFailure { accountMessage = it.message }
                                    }
                                }) {
                                    Spacer(Modifier.align(if (darkTheme) Alignment.CenterEnd else Alignment.CenterStart).padding(horizontal = 3.dp).size(19.dp).background(Color.White, RoundedCornerShape(10.dp)))
                                }
                            }
                            Spacer(Modifier.fillMaxWidth().height(1.dp).background(OdysseyBorder).padding(top = 8.dp))
                            AccountMenuItem(Icons.Outlined.Logout, localized("Выйти", "Sign out", "Cerrar sesión", "Abmelden"), Color(0xFFE85B56)) {
                                scope.launch {
                                    SupabaseProvider.clientForCurrentAuthFlow().auth.signOut()
                                    onLogout()
                                }
                            }
                            accountMessage?.let { Text(it, color = if (it.contains("Не удалось") || it.contains("не совпадают")) Color(0xFFE85B56) else Color(0xFF249D72), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp, modifier = Modifier.padding(top = 4.dp)) }
                        }
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 10.dp).fillMaxWidth().height(62.dp).background(Color(0xFFF4F3F8), RoundedCornerShape(14.dp)).padding(horizontal = 10.dp),
                    ) {
                        if (profileAvatarUrl != null) {
                            AsyncImage(model = profileAvatarUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(40.dp).clip(RoundedCornerShape(10.dp)))
                        } else Text(profileEmail.firstOrNull()?.uppercaseChar()?.toString() ?: "T", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp, textAlign = androidx.compose.ui.text.style.TextAlign.Center, modifier = Modifier.size(40.dp).background(Color(0xFFFF974C), RoundedCornerShape(10.dp)).padding(top = 9.dp))
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                            Text(profileEmail.ifBlank { localized("Личный кабинет", "Account", "Cuenta", "Konto") }, color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, maxLines = 1)
                            Text(localized("Личный кабинет", "Account", "Cuenta", "Konto") + " · $language", color = Color(0xFF8E8D98), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, maxLines = 1)
                        }
                        Icon(Icons.Outlined.Settings, contentDescription = null, tint = Color(0xFF9A99A3), modifier = Modifier.size(19.dp).clickable { accountMenuOpen = !accountMenuOpen })
                    }
                }
            }
        }
    }
}

@Composable
private fun AccountMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color = OdysseyText, onClick: (() -> Unit)? = null) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().let { modifier -> if (onClick != null) modifier.clickable { onClick() } else modifier }.padding(vertical = 10.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (color == OdysseyText) OdysseyPurple else color, modifier = Modifier.size(21.dp))
        Text(label, color = color, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 16.sp, modifier = Modifier.padding(start = 11.dp))
    }
}

@Composable
private fun EditTripPanel(trip: TripCard, onClose: () -> Unit, onSaved: (TripCard) -> Unit) {
    val language = LocalLanguage.current
    val displayedTitle = localizedTripTitle(trip.title)
    val displayedCities = localizedCityList(trip.cities, language)
    val displayedDates = localizedTripDateText(trip.dates, language)
    var title by remember(trip.id, language) { mutableStateOf(displayedTitle) }
    var cities by remember(trip.id, language) { mutableStateOf(displayedCities) }
    var dates by remember(trip.id, language) { mutableStateOf(displayedDates) }
    var status by remember(trip.id) { mutableStateOf(trip.status) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardSurfaceColor()).padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("Редактировать путешествие", "Edit trip", "Editar viaje", "Reise bearbeiten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 18.sp)
        AuthField(localized("Название", "Title", "Nombre", "Name"), localized("Название", "Title", "Nombre", "Name"), title) { title = it }
        AuthField(localized("Города", "Cities", "Ciudades", "Städte"), localized("Города", "Cities", "Ciudades", "Städte"), cities) { cities = it }
        AuthField(localized("Даты", "Dates", "Fechas", "Daten"), localized("Например, 12–15 сентября", "For example, Sep 12–15", "Por ejemplo, 12–15 de septiembre", "Zum Beispiel 12.–15. September"), dates) { dates = it }
        Text(localized("Статус путешествия", "Trip status", "Estado del viaje", "Reisestatus"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "Предстоящее" to localized("Предстоящие", "Upcoming", "Próximos", "Bevorstehend"),
                "Черновик" to localized("Черновики", "Drafts", "Borradores", "Entwürfe"),
                "Прошедшее" to localized("Прошедшие", "Past", "Pasados", "Vergangen"),
            ).forEach { (value, label) ->
                val selected = when (value) {
                    "Предстоящее" -> status.contains("предст", ignoreCase = true) || status.equals("upcoming", ignoreCase = true)
                    "Черновик" -> status.contains("чернов", ignoreCase = true) || status.equals("draft", ignoreCase = true)
                    else -> status.contains("прошед", ignoreCase = true) || status.contains("заверш", ignoreCase = true) || status.equals("past", ignoreCase = true) || status.equals("completed", ignoreCase = true)
                }
                Button(onClick = { status = value }, colors = ButtonDefaults.buttonColors(containerColor = if (selected) OdysseyPurple else secondarySurfaceColor(), contentColor = if (selected) Color.White else contentTextColor()), shape = RoundedCornerShape(10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 4.dp, vertical = 8.dp), modifier = Modifier.weight(1f)) {
                    Text(label, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp, maxLines = 1)
                }
            }
        }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = {
                scope.launch {
                    saving = true
                    val savedTitle = title.trim().takeUnless { it == displayedTitle } ?: trip.title.trim()
                    val savedCities = cities.trim().takeUnless { it == displayedCities } ?: trip.cities.trim()
                    val savedDates = dates.trim().takeUnless { it == displayedDates } ?: trip.dates.trim()
                    runCatching {
                        val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                        repository.updateTripDetails(trip.id, savedTitle, savedDates, savedCities)
                        repository.updateTripSection(trip.id, "status", JsonPrimitive(status))
                    }
                        .onSuccess { onSaved(trip.copy(title = savedTitle, cities = savedCities, dates = savedDates, status = status)) }
                        .onFailure { message = it.message ?: localized(language, "Не удалось сохранить изменения", "Could not save changes", "No se pudieron guardar los cambios", "Änderungen konnten nicht gespeichert werden") }
                    saving = false
                }
            }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(11.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
        }
    }
}

@Composable
private fun CreateTripScreen(onBack: () -> Unit, onCreated: () -> Unit, template: String? = null) {
    val darkTheme = LocalDarkTheme.current
    val language = LocalLanguage.current
    val templateData = when (template) {
        "italy" -> localized("Рождественская Европа", "Christmas Europe", "Europa navideña", "Weihnachtliches Europa") to localized("Прага, Мюнхен, Верона, Милан, Венеция, Рим", "Prague, Munich, Verona, Milan, Venice, Rome", "Praga, Múnich, Verona, Milán, Venecia, Roma", "Prag, München, Verona, Mailand, Venedig, Rom")
        "czech" -> localized("Классическая Италия", "Classic Italy", "Italia clásica", "Klassisches Italien") to localized("Рим, Флоренция, Пиза, Венеция, Милан", "Rome, Florence, Pisa, Venice, Milan", "Roma, Florencia, Pisa, Venecia, Milán", "Rom, Florenz, Pisa, Venedig, Mailand")
        "alps" -> localized("Альпы с семьёй", "The Alps with family", "Los Alpes en familia", "Die Alpen mit der Familie") to localized("Мюнхен, Инсбрук, Зальцбург, Вена", "Munich, Innsbruck, Salzburg, Vienna", "Múnich, Innsbruck, Salzburgo, Viena", "München, Innsbruck, Salzburg, Wien")
        "baltic" -> localized("Балтийский маршрут", "Baltic route", "Ruta báltica", "Baltische Route") to localized("Таллин, Рига, Вильнюс", "Tallinn, Riga, Vilnius", "Tallin, Riga, Vilna", "Tallinn, Riga, Vilnius")
        else -> "" to ""
    }
    var title by remember(template) { mutableStateOf(templateData.first) }
    var startDate by remember { mutableStateOf("") }
    var endDate by remember { mutableStateOf("") }
    var cities by remember(template) { mutableStateOf(templateData.second) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun save() {
        if (title.isBlank()) {
            message = localized(language, "Укажите название путешествия", "Enter a trip name", "Indique un nombre para el viaje", "Geben Sie einen Reisenamen ein")
            return
        }
        if (startDate.isNotBlank() && endDate.isNotBlank() && startDate > endDate) {
            message = localized(language, "Дата окончания не может быть раньше даты начала", "The end date cannot be before the start date", "La fecha de finalización no puede ser anterior a la de inicio", "Das Enddatum darf nicht vor dem Startdatum liegen")
            return
        }
        scope.launch {
            saving = true
            message = null
            runCatching {
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).createTrip(title, startDate, endDate, cities)
            }.onSuccess { onCreated() }.onFailure {
                message = it.message ?: localized(language, "Не удалось создать путешествие", "Could not create trip", "No se pudo crear el viaje", "Reise konnte nicht erstellt werden")
            }
            saving = false
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().background(if (darkTheme) Color(0xFF141416) else OdysseyBackground).padding(WindowInsets.statusBars.asPaddingValues()).padding(horizontal = 18.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(58.dp)) {
            Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Назад", "Back", "Atrás", "Zurück"), tint = contentTextColor(), modifier = Modifier.width(40.dp).size(24.dp).clickable { onBack() })
            Text(localized("Новое путешествие", "New trip", "Nuevo viaje", "Neue Reise"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp)
        }
        Text(localized("С нуля", "From scratch", "Desde cero", "Von Grund auf"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, modifier = Modifier.padding(top = 22.dp))
        Text(localized("Спланируйте новую поездку", "Plan a new trip", "Planifique un nuevo viaje", "Planen Sie eine neue Reise"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 28.sp, modifier = Modifier.padding(top = 5.dp))
        Text(localized("Основные данные можно дополнить позже", "You can add details later", "Podrá añadir los detalles más tarde", "Details können Sie später ergänzen"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp, modifier = Modifier.padding(top = 6.dp, bottom = 24.dp))
        AuthField(localized("Название", "Title", "Nombre", "Name"), localized("Например, Италия с семьей", "For example, Italy with family", "Por ejemplo, Italia en familia", "Zum Beispiel Italien mit Familie"), title) { title = it }
        Spacer(Modifier.height(14.dp))
        AuthField(localized("Города", "Cities", "Ciudades", "Städte"), localized("Рим, Флоренция, Венеция", "Rome, Florence, Venice", "Roma, Florencia, Venecia", "Rom, Florenz, Venedig"), cities) { cities = it }
        Spacer(Modifier.height(14.dp))
        AuthField(localized("Дата начала", "Start date", "Fecha de inicio", "Startdatum"), "YYYY-MM-DD", startDate) { startDate = it }
        Spacer(Modifier.height(14.dp))
        AuthField(localized("Дата окончания", "End date", "Fecha de finalización", "Enddatum"), "YYYY-MM-DD", endDate) { endDate = it }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp, modifier = Modifier.padding(top = 12.dp))
        Button(
            onClick = ::save,
            enabled = !saving,
            colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(top = 18.dp),
        ) {
            Text(if (saving) localized("Создаём…", "Creating…", "Creando…", "Wird erstellt…") else localized("Создать путешествие", "Create trip", "Crear viaje", "Reise erstellen"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp)
        }
    }
}

@Composable
private fun RouteCatalogScreen(onBack: () -> Unit, onUseTemplate: (String) -> Unit) {
    val darkTheme = LocalDarkTheme.current
    val templates = listOf(
        listOf("italy", localized("Рождественская Европа", "Christmas Europe", "Europa navideña", "Weihnachtliches Europa"), localized("12 дней · 6 городов", "12 days · 6 cities", "12 días · 6 ciudades", "12 Tage · 6 Städte"), localized("Прага → Мюнхен → Верона → Милан → Венеция → Рим", "Prague → Munich → Verona → Milan → Venice → Rome", "Praga → Múnich → Verona → Milán → Venecia → Roma", "Prag → München → Verona → Mailand → Venedig → Rom"), "https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=1200&q=85"),
        listOf("czech", localized("Классическая Италия", "Classic Italy", "Italia clásica", "Klassisches Italien"), localized("10 дней · 5 городов", "10 days · 5 cities", "10 días · 5 ciudades", "10 Tage · 5 Städte"), localized("Рим → Флоренция → Пиза → Венеция → Милан", "Rome → Florence → Pisa → Venice → Milan", "Roma → Florencia → Pisa → Venecia → Milán", "Rom → Florenz → Pisa → Venedig → Mailand"), "https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=1200&q=85"),
        listOf("alps", localized("Альпы с семьёй", "The Alps with family", "Los Alpes en familia", "Die Alpen mit der Familie"), localized("7 дней · 4 города", "7 days · 4 cities", "7 días · 4 ciudades", "7 Tage · 4 Städte"), localized("Мюнхен → Инсбрук → Зальцбург → Вена", "Munich → Innsbruck → Salzburg → Vienna", "Múnich → Innsbruck → Salzburgo → Viena", "München → Innsbruck → Salzburg → Wien"), "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=85"),
        listOf("baltic", localized("Балтийский маршрут", "Baltic route", "Ruta báltica", "Baltische Route"), localized("6 дней · 3 города", "6 days · 3 cities", "6 días · 3 ciudades", "6 Tage · 3 Städte"), localized("Таллин → Рига → Вильнюс", "Tallinn → Riga → Vilnius", "Tallin → Riga → Vilna", "Tallinn → Riga → Vilnius"), "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=85"),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(if (darkTheme) Color(0xFF141416) else OdysseyBackground).padding(WindowInsets.statusBars.asPaddingValues()),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        item {
            Box(modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Назад", "Back", "Atrás", "Zurück"), tint = contentTextColor(), modifier = Modifier.align(Alignment.CenterStart).size(24.dp).clickable { onBack() })
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.align(Alignment.Center)) {
                    Text("O", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF8E7BF5))), RoundedCornerShape(8.dp)).padding(horizontal = 9.dp, vertical = 5.dp))
                    Text("Одиссея", color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, modifier = Modifier.padding(start = 9.dp))
                }
            }
        }
        item {
            Text(localized("Каталог\nмаршрутов", "Route\ncatalog", "Catálogo de\nrutas", "Routen\nkatalog"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 29.sp, lineHeight = 31.sp, modifier = Modifier.padding(top = 8.dp))
            Text(localized("Готовые маршруты — используйте как основу для своей поездки", "Ready routes to use as a starting point for your trip", "Rutas listas para usar como base de su viaje", "Fertige Routen als Grundlage für Ihre Reise"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, modifier = Modifier.padding(top = 8.dp, bottom = 6.dp))
        }
        itemsIndexed(templates, key = { _, template -> template[0] }) { index, template ->
            val (id, title, duration, route, image) = template
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardSurfaceColor())) {
                CatalogCover(imageUrl = image, index = index, darkTheme = darkTheme, duration = duration)
                Column(modifier = Modifier.padding(15.dp)) {
                    Text(title, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                    Text(route, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, lineHeight = 18.sp, modifier = Modifier.padding(top = 5.dp))
                    Button(onClick = { onUseTemplate(id) }, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFF0EDFF), contentColor = OdysseyPurple), shape = RoundedCornerShape(11.dp), modifier = Modifier.fillMaxWidth().height(42.dp).padding(top = 10.dp), contentPadding = androidx.compose.foundation.layout.PaddingValues()) {
                        Text(localized("Использовать шаблон", "Use template", "Usar plantilla", "Vorlage verwenden"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun CatalogCover(imageUrl: String, index: Int, darkTheme: Boolean, duration: String) {
    var imageFailed by remember(imageUrl) { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(130.dp)
            .background(catalogCoverBrush(index, darkTheme)),
    ) {
        if (!imageFailed) {
            AsyncImage(
                model = imageUrl,
                contentDescription = null,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
                onError = { imageFailed = true },
            )
        } else {
            Icon(
                imageVector = Icons.Outlined.Explore,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.42f),
                modifier = Modifier.align(Alignment.Center).size(48.dp),
            )
        }
        Text(duration, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).background(Color(0xAA26343D), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp))
    }
}

private fun catalogCoverBrush(index: Int, darkTheme: Boolean): Brush {
    val palettes = if (darkTheme) {
        listOf(
            listOf(Color(0xFF694A3B), Color(0xFF273A43)),
            listOf(Color(0xFF49385F), Color(0xFF25263A)),
            listOf(Color(0xFF315D73), Color(0xFF263E4D)),
            listOf(Color(0xFF6A4E37), Color(0xFF3F3042)),
        )
    } else {
        listOf(
            listOf(Color(0xFFE8B18C), Color(0xFF667A78)),
            listOf(Color(0xFF8872AA), Color(0xFF33354C)),
            listOf(Color(0xFF91B7C8), Color(0xFF557D82)),
            listOf(Color(0xFFE1B77D), Color(0xFF8C6170)),
        )
    }
    return Brush.linearGradient(palettes[index.coerceIn(palettes.indices)])
}

@Composable
private fun TripOverviewScreen(tripId: String, onBack: () -> Unit) {
    val darkTheme = LocalDarkTheme.current
    val language = LocalLanguage.current
    var overview by remember { mutableStateOf<TripOverview?>(null) }
    var weather by remember { mutableStateOf<Map<String, WeatherSnapshot>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf("overview") }
    var sectionMenuOpen by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(tripId, refresh) {
        val hadOverview = overview != null
        if (!hadOverview) loading = true
        loadError = null
        val loadedOverview = runCatching {
            SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadTripOverview(tripId)
        }.onFailure { loadError = it.message }.getOrNull()
        if (loadedOverview == null) {
            if (!hadOverview) overview = null
            loading = false
            return@LaunchedEffect
        }

        overview = loadedOverview
        loading = false

        loadedOverview.let { trip ->
            val cities = trip.overviewMapPoints.ifEmpty {
                trip.routeLegs.flatMap { listOf(it.from, it.to) }.distinct()
            }.distinct()
            weather = WeatherRepository().loadCurrent(cities, trip.dates)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (darkTheme) Color(0xFF141416) else OdysseyBackground)
                .padding(WindowInsets.statusBars.asPaddingValues()),
        ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val pixelPerfectTab = tab == "restaurants" || tab == "accommodation" || tab == "budget"
            val pageScale = if (pixelPerfectTab) (maxWidth.value / 368f) else 1f
            val pageWidth = if (pixelPerfectTab) 368.dp else maxWidth
            val pageHeight = if (pixelPerfectTab) maxHeight / pageScale else maxHeight
            Column(
                modifier = Modifier
                    .width(pageWidth)
                    .height(pageHeight)
                    .offset(y = if (pixelPerfectTab) (-2).dp else 0.dp)
                    .graphicsLayer {
                        scaleX = pageScale
                        scaleY = pageScale
                        transformOrigin = TransformOrigin(0f, 0f)
                    },
            ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(horizontal = 16.dp),
        ) {
            Box(
                modifier = Modifier
                    .width(28.dp)
                    .height(34.dp)
                    .offset(x = (-6).dp),
            ) {
                Box(
                    modifier = Modifier.size(34.dp).clickable { onBack() },
                    contentAlignment = Alignment.Center,
                ) {
                    OdysseyBackArrow()
                }
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.weight(1f).clickable { sectionMenuOpen = !sectionMenuOpen },
            ) {
                Text(
                    text = localizedTripTitle(overview?.title.orEmpty()),
                    color = Color(0xFFA0A0AA),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 10.sp,
                    lineHeight = 14.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    softWrap = false,
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        when (tab) {
                            "overview" -> localized("Главная", "Overview", "Inicio", "Übersicht")
                            "route" -> localized("Маршрут", "Route", "Ruta", "Route")
                            "sights" -> localized("Достопримечательности", "Sights", "Lugares", "Sehenswürdigkeiten")
                            "restaurants" -> localized("Рестораны", "Restaurants", "Restaurantes", "Restaurants")
                            "accommodation" -> localized("Жильё", "Lodging", "Alojamiento", "Unterkunft")
                            "budget" -> localized("Бюджет", "Budget", "Presupuesto", "Budget")
                            "members" -> localized("Участники", "Members", "Participantes", "Teilnehmer")
                            else -> localized("Фото", "Photos", "Fotos", "Fotos")
                        },
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 15.sp,
                        lineHeight = 21.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        maxLines = 1,
                        softWrap = false,
                    )
                    Spacer(Modifier.width(5.dp))
                    OdysseyChevronDown(14.dp)
                }
            }
            Spacer(Modifier.width(40.dp))
        }

        if (loading) {
            TripOverviewLoading()
        } else if (overview == null) {
            Text(
                text = loadError ?: localized("Путешествие не найдено", "Trip not found", "Viaje no encontrado", "Reise nicht gefunden"),
                color = Color(0xFFE0524B),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                modifier = Modifier.padding(18.dp),
            )
        } else {
            when (tab) {
                "overview" -> OverviewContent(overview!!, weather)
                "route" -> TripRouteContent(tripId, overview!!) { refresh++ }
                "sights" -> SightsContent(tripId, overview!!) { refresh++ }
                "restaurants" -> RestaurantsContent(tripId, overview!!) { refresh++ }
                "accommodation" -> AccommodationContent(tripId, overview!!) { refresh++ }
                "budget" -> BudgetContent(
                    tripId = tripId,
                    overview = overview!!,
                    onExpenseAdded = { refresh++ },
                    onCurrencyChanged = { selectedCurrency -> overview = overview?.copy(budgetCurrency = selectedCurrency) },
                )
                "members" -> MembersContent(tripId, overview!!) { refresh++ }
                else -> PhotosContent(tripId, overview!!) { refresh++ }
            }
        }
            }
        }
        }

        if (sectionMenuOpen) {
            Box(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.fillMaxSize().background(Color(0x66000000)).clickable { sectionMenuOpen = false })
                Column(modifier = Modifier.fillMaxHeight().width(330.dp).background(cardSurfaceColor()).padding(start = 18.dp, top = 22.dp, end = 18.dp, bottom = 32.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(45.dp).background(OdysseyPurple, RoundedCornerShape(12.dp))) {
                            Icon(Icons.Outlined.Explore, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 10.dp, top = 7.dp)) {
                            Text(localizedTripTitle(overview?.title.orEmpty()), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(localizedTripDateText(overview?.dates.orEmpty(), language, multilineDuration = true), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, lineHeight = 14.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(40.dp).background(secondarySurfaceColor(), CircleShape).clickable { sectionMenuOpen = false }) {
                            Icon(Icons.Filled.Close, contentDescription = null, tint = secondaryTextColor(), modifier = Modifier.size(20.dp))
                        }
                    }
                    Spacer(Modifier.fillMaxWidth().height(1.dp).background(OdysseyBorder))
                    Spacer(Modifier.height(16.dp))
                    listOf(
                        Triple("overview", Icons.Outlined.Explore, localized("Главная", "Overview", "Inicio", "Übersicht")),
                        Triple("route", Icons.Outlined.Share, localized("Маршрут", "Route", "Ruta", "Route")),
                        Triple("sights", Icons.Outlined.LocationOn, localized("Достопримечательности", "Sights", "Lugares", "Sehenswürdigkeiten")),
                        Triple("restaurants", Icons.Outlined.Restaurant, localized("Рестораны", "Restaurants", "Restaurantes", "Restaurants")),
                        Triple("accommodation", Icons.Outlined.Hotel, localized("Жильё", "Lodging", "Alojamiento", "Unterkunft")),
                        Triple("budget", Icons.Outlined.AccountBalanceWallet, localized("Бюджет", "Budget", "Presupuesto", "Budget")),
                        Triple("members", Icons.Outlined.Group, localized("Участники", "Members", "Participantes", "Teilnehmer")),
                        Triple("photos", Icons.Outlined.Image, localized("Фото", "Photos", "Fotos", "Fotos")),
                    ).forEach { (entry, icon, label) ->
                        val selected = tab == entry
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 8.dp).background(if (selected) OdysseyTint else Color.Transparent, RoundedCornerShape(12.dp)).clickable { tab = entry; sectionMenuOpen = false }.padding(horizontal = 14.dp, vertical = 13.dp)) {
                            Icon(icon, contentDescription = null, tint = if (selected) OdysseyPurple else secondaryTextColor(), modifier = Modifier.size(20.dp))
                            Text(label, color = if (selected) OdysseyPurple else contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, maxLines = 1, softWrap = false, modifier = Modifier.padding(start = 14.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TripTabs(selected: String, onSelect: (String) -> Unit) {
    val darkTheme = LocalDarkTheme.current
    Row(
        horizontalArrangement = Arrangement.spacedBy(20.dp),
        modifier = Modifier.fillMaxWidth().background(if (darkTheme) Color(0xFF20222E) else Color(0xFFEEF0F3)).horizontalScroll(rememberScrollState()).padding(horizontal = 18.dp),
    ) {
        listOf(
            "overview" to localized("Главная", "Overview", "Inicio", "Übersicht"),
            "route" to localized("Маршрут", "Route", "Ruta", "Route"),
            "sights" to localized("Достопримечательности", "Sights", "Lugares", "Sehenswürdigkeiten"),
            "restaurants" to localized("Рестораны", "Restaurants", "Restaurantes", "Restaurants"),
            "accommodation" to localized("Жильё", "Lodging", "Alojamiento", "Unterkunft"),
            "budget" to localized("Бюджет", "Budget", "Presupuesto", "Budget"),
            "members" to localized("Участники", "Members", "Participantes", "Teilnehmer"),
            "photos" to localized("Фото", "Photos", "Fotos", "Fotos"),
        ).forEach { (id, label) ->
            val active = selected == id
            Text(
                text = label,
                color = if (active) contentTextColor() else secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 13.sp,
                modifier = Modifier
                    .clickable { onSelect(id) }
                    .drawBehind {
                        if (active) drawLine(OdysseyPurple, Offset(0f, size.height - 1.dp.toPx()), Offset(size.width, size.height - 1.dp.toPx()), strokeWidth = 3.dp.toPx())
                    }
                    .padding(horizontal = 3.dp, vertical = 13.dp),
            )
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun SightsContent(tripId: String, overview: TripOverview, onSightUpdated: () -> Unit) {
    val context = LocalContext.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val language = LocalLanguage.current
    val sights = overview.sights.sortedWith(compareBy<com.odyssey.travelplanner.data.Sight> { sightRouteDay(it.walkDay) }.thenBy { it.walkOrder })
    val initialRouteCity = sights.firstOrNull()?.city?.ifBlank { null }
        ?: overview.routeLegs.firstOrNull()?.to.orEmpty()
    var routeDay by remember(tripId) { mutableStateOf(sights.firstOrNull()?.walkDay?.let(::sightRouteDay) ?: 1) }
    var dayMenuOpen by remember { mutableStateOf(false) }
    var creatingDay by remember { mutableStateOf(false) }
    val dayCities = remember(sights, overview.routeLegs, initialRouteCity) {
        val totalDays = maxOf(
            sights.maxOfOrNull { sightRouteDay(it.walkDay) } ?: 1,
            overview.routeDayCount,
            overview.routeLegs.maxOfOrNull { routeLegDayNumber(it, overview.routeLegs) } ?: overview.routeLegs.size,
        )
        (1..totalDays).map { day ->
            sights.firstOrNull { sightRouteDay(it.walkDay) == day }?.city?.takeIf(String::isNotBlank)
                ?: overview.routeLegs.firstOrNull { routeLegDayNumber(it, overview.routeLegs) == day }?.to
                ?: initialRouteCity
        }
    }
    val selectedDayCity = dayCities.getOrNull(routeDay - 1).orEmpty().ifBlank { initialRouteCity }
    val visibleSights = sights.filter { sightRouteDay(it.walkDay) == routeDay }
    var selectedSightId by remember(tripId, routeDay) { mutableStateOf<String?>(null) }
    val selectedLeg = overview.routeLegs.firstOrNull { routeLegDayNumber(it, overview.routeLegs) == routeDay }
    val mapCities = selectedLeg?.let { listOf(it.from, it.to) } ?: listOf(selectedDayCity)
    val sightRoutePoints = visibleSights.mapNotNull { sight -> sight.longitude?.let { longitude -> sight.latitude?.let { latitude -> Point.fromLngLat(longitude, latitude) } } }
    val sightMapEntries = visibleSights.mapNotNull { sight ->
        val point = sight.longitude?.let { longitude -> sight.latitude?.let { latitude -> Point.fromLngLat(longitude, latitude) } }
            ?: mapCoordinate(sight.city)
        point?.let { sight.id to it }
    }
    val sightMapPoints = sightMapEntries.map { it.second }
    val selectedSightMapIndex = sightMapEntries.indexOfFirst { it.first == selectedSightId }.takeIf { it >= 0 }
    val routeShareUrl = if (sightRoutePoints.size > 1) {
        val stops = sightRoutePoints.map { "${it.latitude()},${it.longitude()}" }
        "https://www.google.com/maps/dir/?api=1&origin=${stops.first()}&destination=${stops.last()}&waypoints=${stops.drop(1).dropLast(1).joinToString("|")}" 
    } else "https://www.google.com/maps/search/?api=1&query=${Uri.encode(selectedDayCity)}"
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("достопримечательности") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var editingSight by remember { mutableStateOf<com.odyssey.travelplanner.data.Sight?>(null) }
    var uploadingSightId by remember { mutableStateOf<String?>(null) }
    var fullScreenSight by remember { mutableStateOf<com.odyssey.travelplanner.data.Sight?>(null) }
    var editingDay by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val sightId = uploadingSightId ?: return@rememberLauncherForActivityResult
        if (uri == null) { uploadingSightId = null; return@rememberLauncherForActivityResult }
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Не удалось прочитать изображение")
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addSightPhoto(tripId, sightId, bytes)
            }.onSuccess { onSightUpdated() }
            uploadingSightId = null
        }
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                "${localizedCityName(selectedDayCity).uppercase()} · ${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} $routeDay",
                color = OdysseyPurple,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 11.sp,
                lineHeight = 14.sp,
                letterSpacing = 0.66.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            )
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(68.dp)
                    .shadow(4.dp, RoundedCornerShape(17.dp), clip = false, ambientColor = Color(0x0D141428), spotColor = Color(0x0D141428))
                    .clip(RoundedCornerShape(17.dp))
                    .background(cardSurfaceColor())
                    .border(1.dp, OdysseyBorder, RoundedCornerShape(17.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(13.dp),
            ) {
                Box(modifier = Modifier.size(46.dp).shadow(4.dp, RoundedCornerShape(13.dp), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7)).clip(RoundedCornerShape(13.dp)).background(OdysseyPurple), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                        Text(
                            routeDay.toString(),
                            color = Color.White,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 17.sp,
                            lineHeight = 17.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                        Text(
                            localized("ДЕНЬ", "DAY", "DÍA", "TAG"),
                            color = Color.White.copy(alpha = 0.8f),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 7.sp,
                            lineHeight = 9.sp,
                            letterSpacing = 0.7.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        localized("ВЫБЕРИТЕ ДЕНЬ", "SELECT DAY", "ELIGE UN DÍA", "TAG WÄHLEN"),
                        color = secondaryTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        letterSpacing = 0.8.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 1.dp)) {
                        Text(
                            localizedCityName(selectedDayCity),
                            color = contentTextColor(),
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 17.sp,
                            lineHeight = 23.sp,
                            letterSpacing = (-0.17).sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                        Box(modifier = Modifier.padding(start = 7.dp).size(22.dp).clip(RoundedCornerShape(7.dp)).background(OdysseyTint).clickable { editingDay = true }, contentAlignment = Alignment.Center) {
                            Icon(Icons.Outlined.Edit, contentDescription = localized("Изменить", "Edit", "Editar", "Bearbeiten"), tint = OdysseyPurple, modifier = Modifier.size(12.dp))
                        }
                    }
                }
                Box(modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)).background(OdysseyTint).clickable { dayMenuOpen = !dayMenuOpen }, contentAlignment = Alignment.Center) {
                    if (dayMenuOpen) {
                        OdysseyChevronUp(17.dp, color = OdysseyPurple)
                    } else {
                        OdysseyChevronDown(17.dp, color = OdysseyPurple)
                    }
                }
            }
            if (dayMenuOpen) {
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp).clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(vertical = 7.dp)) {
                    dayCities.forEachIndexed { index, dayCity ->
                        val selected = index + 1 == routeDay
                        Row(modifier = Modifier.fillMaxWidth().height(43.dp).padding(horizontal = 12.dp).clip(RoundedCornerShape(11.dp)).background(if (selected) Color(0xFFF0EDFF) else Color.Transparent).clickable { routeDay = index + 1; dayMenuOpen = false }.padding(horizontal = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} ${index + 1}", color = if (selected) OdysseyPurple else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp, modifier = Modifier.width(64.dp))
                            Text(localizedCityName(dayCity), color = if (selected) OdysseyPurple else contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                            Spacer(Modifier.weight(1f))
                            if (selected) Text("✓", color = OdysseyPurple, fontSize = 18.sp, fontWeight = FontWeight.W800)
                        }
                    }
                    Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(OdysseyBorder).padding(horizontal = 12.dp))
                    Row(modifier = Modifier.fillMaxWidth().height(52.dp).padding(horizontal = 22.dp).clickable { creatingDay = true; dayMenuOpen = false }, verticalAlignment = Alignment.CenterVertically) {
                        Box(modifier = Modifier.size(28.dp).clip(RoundedCornerShape(9.dp)).background(Color(0xFFF3F1FF)), contentAlignment = Alignment.Center) { Text("+", color = OdysseyPurple, fontSize = 23.sp, fontWeight = FontWeight.W500) }
                        Text(localized("Добавить день", "Add day", "Añadir día", "Tag hinzufügen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, modifier = Modifier.padding(start = 12.dp))
                    }
                }
            }
        }
        item {
            Box(modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)) {
                OverviewMapCard(
                    legs = overview.routeLegs,
                    cities = mapCities,
                    mapHeight = 220.dp,
                    cardShape = RoundedCornerShape(22.dp),
                    cardShadow = 10.dp,
                    routePoints = sightMapPoints,
                    selectedPointIndex = selectedSightMapIndex,
                    footer = {
                        Row(modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${localizedCityName(selectedDayCity).uppercase()} · ${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} $routeDay",
                                    color = OdysseyPurple,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W800,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    letterSpacing = 0.66.sp,
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                )
                                Text(
                                    "${localizedCityName(selectedDayCity)} · ${visibleSights.size} ${localized("места", "places", "lugares", "Orte")}",
                                    color = secondaryTextColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W700,
                                    fontSize = 13.sp,
                                    lineHeight = 18.sp,
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                    modifier = Modifier.padding(top = 2.dp),
                                )
                            }
                            Row(
                                modifier = Modifier
                                    .height(36.dp)
                                    .clip(RoundedCornerShape(11.dp))
                                    .background(OdysseyPurple)
                                    .clickable { clipboard.setText(AnnotatedString(routeShareUrl)) }
                                    .padding(horizontal = 13.dp, vertical = 9.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                            ) {
                                Icon(Icons.Outlined.ContentCopy, contentDescription = localized("Копировать", "Copy", "Copiar", "Kopieren"), tint = Color.White, modifier = Modifier.size(14.dp))
                                Text(
                                    localized("Копировать", "Copy", "Copiar", "Kopieren"),
                                    color = Color.White,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W700,
                                    fontSize = 13.sp,
                                    lineHeight = 16.sp,
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                    maxLines = 1,
                                    softWrap = false,
                                )
                            }
                        }
                    },
                )
            }
        }
        if (visibleSights.isEmpty()) {
            item { Text(localized("Достопримечательности пока не добавлены", "No sights added yet", "Aún no se han añadido lugares", "Noch keine Orte hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp) }
        } else {
            if (editingSight != null) item {
                EditSightPanel(editingSight!!, tripId, onClose = { editingSight = null }, onSaved = {
                    editingSight = null
                    onSightUpdated()
                })
            }
            items(visibleSights, key = { it.id }) { sight ->
                SightCard(
                    sight = sight,
                    uploading = uploadingSightId == sight.id,
                    selected = sight.id == selectedSightId,
                    onSelect = { selectedSightId = sight.id },
                    onOpenPhoto = { fullScreenSight = sight },
                )
            }
        }
    }
    fullScreenSight?.let { sight ->
        FullScreenSightPhotoViewer(
            sight = sight,
            onDismiss = { fullScreenSight = null },
        )
    }
    if (editingDay) {
        ModalBottomSheet(onDismissRequest = { editingDay = false }, containerColor = cardSurfaceColor()) {
            EditDaySheet(tripId, routeDay, selectedDayCity, visibleSights, onClose = { editingDay = false }, onSaved = onSightUpdated)
        }
    }
    if (creatingDay) {
        ModalBottomSheet(onDismissRequest = { creatingDay = false }, containerColor = cardSurfaceColor()) {
            CreateDaySheet(tripId = tripId, city = selectedDayCity, day = routeDay + 1, sights = visibleSights, onClose = { creatingDay = false }, onSaved = onSightUpdated)
        }
    }
}

private fun sightRouteDay(walkDay: Int): Int = walkDay.coerceAtLeast(1)

private val sightPhotoUrlCache = ConcurrentHashMap<String, String>()
private val sightBitmapCache = ConcurrentHashMap<String, Bitmap>()
private val sightPhotoSearchGate = Semaphore(6)
private val sightPhotoDownloadGate = Semaphore(6)

private fun knownSightPhotoUrl(sight: com.odyssey.travelplanner.data.Sight): String? {
    if (sight.city.trim().lowercase(Locale.ROOT) != "верона" || sight.walkDay != 2) return null
    return listOf(
        "https://api.openverse.org/v1/images/1943615d-4370-4634-93b6-0c11d304f75b/thumb/",
        "https://api.openverse.org/v1/images/6d13d700-5ffb-405d-a7b4-a5a34f9ce1be/thumb/",
        "https://api.openverse.org/v1/images/1943615d-4370-4634-93b6-0c11d304f75b/thumb/",
        "https://api.openverse.org/v1/images/196b5db9-4cd5-4157-ac87-5302eba8c335/thumb/",
        "https://api.openverse.org/v1/images/e92694b6-f5af-46cc-aaef-ed8e2009bb04/thumb/",
        "https://api.openverse.org/v1/images/e92694b6-f5af-46cc-aaef-ed8e2009bb04/thumb/",
        "https://api.openverse.org/v1/images/31541f5a-91f6-46ad-90d0-209d9f4ea5a4/thumb/",
        "https://api.openverse.org/v1/images/9d211074-fbaf-4ab1-ac95-756708f0a986/thumb/",
        "https://api.openverse.org/v1/images/2ed31ff9-c5d6-448d-bad3-9e074683cd3a/thumb/",
        "https://api.openverse.org/v1/images/02c3c260-169b-4e59-bb58-2c2a4bc44fda/thumb/",
    ).getOrNull(sight.walkOrder)
}

private suspend fun loadSightPhoto(vararg searchTexts: String): String? = withContext(Dispatchers.IO) {
    searchTexts.asSequence()
        .map(String::trim)
        .filter(String::isNotBlank)
        .distinct()
        .mapNotNull { searchText ->
            val query = URLEncoder.encode(searchText, Charsets.UTF_8.name())
            listOf(
                URL("https://api.openverse.org/v1/images?q=$query&page_size=5"),
                URL(
                    "https://commons.wikimedia.org/w/api.php?action=query&generator=search" +
                        "&gsrsearch=$query&gsrnamespace=6&prop=imageinfo&iiprop=url" +
                        "&iiurlwidth=900&format=json&origin=*",
                ),
                URL(
                    "https://en.wikipedia.org/w/api.php?action=query&generator=search" +
                        "&gsrsearch=$query&gsrnamespace=0&prop=pageimages" +
                        "&piprop=thumbnail&pithumbsize=900&format=json&origin=*",
                ),
            ).asSequence().mapNotNull { endpoint ->
                runCatching {
                    (endpoint.openConnection() as HttpURLConnection).run {
                        connectTimeout = 5_000
                        readTimeout = 5_000
                        requestMethod = "GET"
                        setRequestProperty("Accept", "application/json")
                        setRequestProperty("Accept-Encoding", "identity")
                        setRequestProperty("User-Agent", "OdysseyTravelPlanner/0.1 (Android)")
                        inputStream.bufferedReader().use { reader ->
                            val response = JSONObject(reader.readText())
                            if (endpoint.host == "api.openverse.org") {
                                val results = response.optJSONArray("results") ?: return@run null
                                for (index in 0 until results.length()) {
                                    val result = results.optJSONObject(index) ?: continue
                                    val photo = result.optString("thumbnail")
                                        .ifBlank { result.optString("url") }
                                    if (photo.isNotBlank()) return@run photo
                                }
                                return@run null
                            }
                            val pages = response
                                .optJSONObject("query")
                                ?.optJSONObject("pages")
                                ?: return@run null
                            val keys = pages.keys()
                            while (keys.hasNext()) {
                                val page = pages.optJSONObject(keys.next()) ?: continue
                                val photo = page.optJSONArray("imageinfo")
                                    ?.optJSONObject(0)
                                    ?.optString("thumburl")
                                    .orEmpty()
                                    .ifBlank { page.optJSONObject("thumbnail")?.optString("source").orEmpty() }
                                if (photo.isNotBlank()) return@run photo
                            }
                            null
                        }
                    }
                }.getOrNull()
            }.firstOrNull()
        }
        .firstOrNull()
}

private suspend fun cachedSightPhotoUrl(cacheKey: String, vararg searchTexts: String): String? {
    sightPhotoUrlCache[cacheKey]?.let { return it }
    val photoUrl = sightPhotoSearchGate.withPermit { loadSightPhoto(*searchTexts) }
    if (!photoUrl.isNullOrBlank()) sightPhotoUrlCache[cacheKey] = photoUrl
    return photoUrl
}

private suspend fun cachedSightBitmap(photoUrl: String): Bitmap? {
    sightBitmapCache[photoUrl]?.let { return it }
    val bitmap = withContext(Dispatchers.IO) {
        sightPhotoDownloadGate.withPermit {
        runCatching {
            (URL(photoUrl).openConnection() as HttpURLConnection).run {
                connectTimeout = 8_000
                readTimeout = 8_000
                requestMethod = "GET"
                setRequestProperty("Accept", "image/*")
                setRequestProperty("User-Agent", "OdysseyTravelPlanner/0.1 (Android)")
                inputStream.use { BitmapFactory.decodeStream(it) }
            }
        }.getOrNull()
        }
    }
    if (bitmap != null) sightBitmapCache[photoUrl] = bitmap
    return bitmap
}

@Composable
private fun rememberSightBitmap(sight: com.odyssey.travelplanner.data.Sight): Bitmap? {
    val displayedName = localizedSightName(sight.name)
    val displayedCity = localizedCityName(sight.city)
    val englishCity = localizedCityName(sight.city, "EN")
    var bitmap by remember(sight.id, sight.photo) { mutableStateOf<Bitmap?>(null) }
    LaunchedEffect(sight.id, sight.name, sight.city, sight.photo, displayedName, displayedCity) {
        bitmap = null
        val resolvedPhotoUrl = sight.photo.ifBlank { knownSightPhotoUrl(sight).orEmpty() }.ifBlank {
            cachedSightPhotoUrl(
                sight.id,
                "$displayedName $englishCity",
                "${sight.name} ${sight.city}",
                "$displayedName $displayedCity",
            ).orEmpty()
        }
        bitmap = if (resolvedPhotoUrl.isBlank()) null else cachedSightBitmap(resolvedPhotoUrl)
    }
    return bitmap
}

@Composable
private fun SightPhoto(
    sight: com.odyssey.travelplanner.data.Sight,
    modifier: Modifier,
    onClick: (() -> Unit)? = null,
) {
    val bitmap = rememberSightBitmap(sight)
    val canOpenPhoto = onClick != null && (sight.photo.isNotBlank() || bitmap != null)
    val photoModifier = if (canOpenPhoto) modifier.clickable { onClick?.invoke() } else modifier
    Box(modifier = photoModifier.background(Color(0xFFE3E1EC)), contentAlignment = Alignment.Center) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = sight.name,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            SurfaceEmptyMedia(Icons.Outlined.LocationOn, Modifier.fillMaxSize())
        }
    }
}

private fun routeLegDayNumber(
    leg: com.odyssey.travelplanner.data.RouteLeg,
    legs: List<com.odyssey.travelplanner.data.RouteLeg>,
): Int = leg.dayNumber.takeIf { it > 0 } ?: (legs.indexOf(leg) + 1)

@Composable
private fun CreateDaySheet(tripId: String, city: String, day: Int, sights: List<com.odyssey.travelplanner.data.Sight>, onClose: () -> Unit, onSaved: () -> Unit) {
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    var dayNumber by remember { mutableStateOf(day.toString()) }
    var placeName by remember { mutableStateOf("") }
    var placeNames by remember { mutableStateOf(emptyList<String>()) }
    var previewSights by remember(sights) { mutableStateOf(sights.take(3)) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    Column(modifier = Modifier.fillMaxWidth().verticalScroll(scrollState).padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) { Text(localized("СОЗДАТЬ ДЕНЬ", "CREATE DAY", "CREAR DÍA", "TAG ERSTELLEN"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp); Text(localized("Места и маршрут дня", "Places and day route", "Lugares y ruta del día", "Orte und Tagesroute"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 22.sp) }
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF5F4F8)).clickable { onClose() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = null, tint = OdysseySubtext, modifier = Modifier.size(18.dp)) }
        }
        RouteEditorField(localized("День", "Day", "Día", "Tag"), dayNumber, { dayNumber = it }, Modifier.fillMaxWidth())
        Text(localized("ДОСТОПРИМЕЧАТЕЛЬНОСТИ · ${sights.size + placeNames.size}", "SIGHTS · ${sights.size + placeNames.size}", "LUGARES · ${sights.size + placeNames.size}", "ORTE · ${sights.size + placeNames.size}"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            OutlinedTextField(
                value = placeName,
                onValueChange = { placeName = it },
                placeholder = { Text(localized("Напр. Хофбройхаус", "E.g. Hofbräuhaus", "P. ej. Hofbräuhaus", "Z. B. Hofbräuhaus"), color = OdysseySubtext, fontFamily = Manrope, fontSize = 13.sp) },
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp, color = contentTextColor(), platformStyle = OdysseyNoFontPadding),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f).height(54.dp),
            )
            Button(onClick = { if (placeName.isNotBlank()) { placeNames = placeNames + placeName.trim(); placeName = "" } }, modifier = Modifier.height(54.dp), colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(12.dp)) { Text(localized("＋ Добавить", "＋ Add", "＋ Añadir", "＋ Hinzufügen"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp) }
        }
        Text(localized("МАРШРУТ ДНЯ · порядок задаёт путь", "DAY ROUTE · order defines route", "RUTA DEL DÍA · el orden define la ruta", "TAGESROUTE · Reihenfolge bestimmt den Weg"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp)
        previewSights.forEachIndexed { index, sight ->
            Row(modifier = Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(13.dp)).background(secondarySurfaceColor()).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((index + 1).toString(), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, modifier = Modifier.size(34.dp).clip(CircleShape).border(2.dp, Color(0xFFCFC6FF), CircleShape).padding(start = 11.dp, top = 5.dp))
                Text(localizedSightName(sight.name), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, lineHeight = 18.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RouteOrderButton(Icons.Outlined.KeyboardArrowUp, index > 0, localized("Переместить вверх", "Move up", "Mover arriba", "Nach oben")) {
                        val reordered = previewSights.toMutableList()
                        val moved = reordered.removeAt(index)
                        reordered.add(index - 1, moved)
                        previewSights = reordered
                    }
                    RouteOrderButton(Icons.Outlined.KeyboardArrowDown, index < previewSights.lastIndex, localized("Переместить вниз", "Move down", "Mover abajo", "Nach unten")) {
                        val reordered = previewSights.toMutableList()
                        val moved = reordered.removeAt(index)
                        reordered.add(index + 1, moved)
                        previewSights = reordered
                    }
                }
            }
        }
        placeNames.forEachIndexed { index, pendingName ->
            Row(modifier = Modifier.fillMaxWidth().height(66.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFF1EEFF)).padding(horizontal = 12.dp), verticalAlignment = Alignment.CenterVertically) {
                Text((sights.take(3).size + index + 1).toString(), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, modifier = Modifier.size(34.dp).clip(CircleShape).border(2.dp, Color(0xFFCFC6FF), CircleShape).padding(start = 11.dp, top = 5.dp))
                Text(pendingName, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, lineHeight = 18.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    RouteOrderButton(Icons.Outlined.KeyboardArrowUp, index > 0, localized("Переместить вверх", "Move up", "Mover arriba", "Nach oben")) {
                        val reordered = placeNames.toMutableList()
                        val moved = reordered.removeAt(index)
                        reordered.add(index - 1, moved)
                        placeNames = reordered
                    }
                    RouteOrderButton(Icons.Outlined.KeyboardArrowDown, index < placeNames.lastIndex, localized("Переместить вниз", "Move down", "Mover abajo", "Nach unten")) {
                        val reordered = placeNames.toMutableList()
                        val moved = reordered.removeAt(index)
                        reordered.add(index + 1, moved)
                        placeNames = reordered
                    }
                    Text("×", color = Color(0xFFFF6B65), fontSize = 22.sp, modifier = Modifier.padding(start = 4.dp).clickable { placeNames = placeNames.filterIndexed { itemIndex, _ -> itemIndex != index } })
                }
            }
        }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Button(onClick = {
            scope.launch {
                saving = true
                runCatching {
                    val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                    repository.reorderSights(tripId, previewSights.map { it.id })
                    repository.addRouteLeg(tripId, city, city)
                    val namesToAdd = placeNames + placeName.trim().takeIf { it.isNotBlank() }.orEmpty()
                    namesToAdd.forEach { sightName ->
                        repository.addSightDetails(tripId, sightName, city, "достопримечательности", "", dayNumber.toIntOrNull() ?: day)
                    }
                }.onSuccess { onSaved(); onClose() }.onFailure {
                    message = it.message ?: localized(language, "Не удалось сохранить день", "Could not save day", "No se pudo guardar el día", "Tag konnte nicht gespeichert werden")
                }
                saving = false
            }
        }, enabled = !saving && city.isNotBlank(), modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 5.dp), colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(14.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить день", "Save day", "Guardar día", "Tag speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun EditDaySheet(tripId: String, day: Int, city: String, sights: List<com.odyssey.travelplanner.data.Sight>, onClose: () -> Unit, onSaved: () -> Unit) {
    var addingSight by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("Редактировать день", "Edit day", "Editar día", "Tag bearbeiten"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 22.5.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF5F4F8)).clickable { onClose() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = OdysseySubtext, modifier = Modifier.size(18.dp)) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RouteEditorField(localized("День №", "Day no.", "Día nº", "Tag Nr."), day.toString(), {}, Modifier.width(74.dp))
            RouteEditorField(localized("Город", "City", "Ciudad", "Stadt"), localizedCityName(city), {}, Modifier.weight(1f))
        }
        Text(localized("ДОСТОПРИМЕЧАТЕЛЬНОСТИ", "SIGHTS", "LUGARES", "SEHENSWÜRDIGKEITEN"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.7.sp, modifier = Modifier.padding(top = 6.dp))
        sights.take(3).forEach { sight ->
            Row(modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F4F8)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                SightPhoto(sight, Modifier.size(52.dp).clip(RoundedCornerShape(11.dp)))
                Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) { Text(localizedSightName(sight.name), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(localizedSightInfo(sight.description, sight.category), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.8.sp, maxLines = 1) }
                Box(modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(Color(0xFFFFE9E8)).clickable {
                    scope.launch {
                        runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "sights", sight.id) }
                            .onSuccess { onSaved() }
                    }
                }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить", "Delete", "Eliminar", "Löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(18.dp))
                }
            }
        }
        Text(localized("＋  Добавить достопримечательность", "＋  Add sight", "＋  Añadir lugar", "＋  Ort hinzufügen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().height(50.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFD7D0FF), RoundedCornerShape(14.dp)).background(Color(0xFFFAF9FF)).clickable { addingSight = true }.padding(top = 15.dp))
        Button(onClick = onClose, modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 5.dp), colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(14.dp)) { Text(localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
    }
    if (addingSight) {
        ModalBottomSheet(onDismissRequest = { addingSight = false }, containerColor = cardSurfaceColor()) {
            AddSightSheet(tripId, city, day, onClose = { addingSight = false }, onSaved = onSaved)
        }
    }
}

@Composable
private fun AddSightSheet(tripId: String, city: String, day: Int, onClose: () -> Unit, onSaved: () -> Unit) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var photoUri by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri -> photoUri = uri }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("${localizedCityName(city).uppercase()} · ${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} $day", color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp)
                Text(localized("Добавить\nдостопримечательность", "Add\nsight", "Añadir\nlugar", "Sehenswürdigkeit\nhinzufügen"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 24.sp, lineHeight = 27.sp)
            }
            Box(modifier = Modifier.size(36.dp).clip(CircleShape).background(Color(0xFFF5F4F8)).clickable { onClose() }, contentAlignment = Alignment.Center) { Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = OdysseySubtext, modifier = Modifier.size(18.dp)) }
        }
        RouteEditorField(localized("Главная достопримечательность", "Main sight", "Lugar principal", "Hauptsehenswürdigkeit"), name, { name = it }, Modifier.fillMaxWidth(), placeholder = localized("Напр. Две башни", "E.g. Two towers", "P. ej. Dos torres", "Z. B. Zwei Türme"))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(value = description, onValueChange = { description = it }, placeholder = { Text(localized("Описание объекта: что\nважно увидеть, время\nпосещения, заметки...", "Description", "Descripción", "Beschreibung"), color = OdysseySubtext, fontFamily = Manrope, fontSize = 13.sp) }, shape = RoundedCornerShape(14.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Color(0xFFE0DFE7), unfocusedBorderColor = Color(0xFFE0DFE7)), modifier = Modifier.weight(1f).height(110.dp))
            Box(modifier = Modifier.width(132.dp).height(110.dp).clip(RoundedCornerShape(14.dp)).border(1.dp, Color(0xFFD7D0FF), RoundedCornerShape(14.dp)).background(Color(0xFFFAF9FF)).clickable { photoPicker.launch("image/*") }, contentAlignment = Alignment.Center) {
                if (photoUri != null) AsyncImage(model = photoUri, contentDescription = localized("Выбранное фото", "Selected photo", "Foto seleccionada", "Ausgewähltes Foto"), contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize()) else Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) { Text("⇧", color = OdysseyPurple, fontSize = 28.sp); Text(localized("Фото объекта", "Photo", "Foto", "Foto"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp); Text(localized("Выберите\nфайл", "Choose file", "Elige archivo", "Datei wählen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, textAlign = TextAlign.Center) }
            }
        }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Button(
            onClick = {
                scope.launch {
                    saving = true
                    runCatching {
                        val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                        val sightId = repository.addSightDetails(tripId, name, city, "достопримечательности", description, day)
                        photoUri?.let { uri ->
                            val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                ?: error("Не удалось прочитать изображение")
                            repository.addSightPhoto(tripId, sightId, bytes)
                        }
                    }.onSuccess { onSaved(); onClose() }.onFailure {
                        message = it.message ?: localized(language, "Не удалось сохранить место", "Could not save sight", "No se pudo guardar el lugar", "Ort konnte nicht gespeichert werden")
                    }
                    saving = false
                }
            },
            enabled = !saving && name.isNotBlank(),
            modifier = Modifier.fillMaxWidth().height(54.dp).padding(bottom = 5.dp),
            colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple),
            shape = RoundedCornerShape(14.dp),
        ) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Добавить место", "Add sight", "Añadir lugar", "Ort hinzufügen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
    }
}

@Composable
private fun SightCard(
    sight: com.odyssey.travelplanner.data.Sight,
    uploading: Boolean,
    selected: Boolean,
    onSelect: () -> Unit,
    onOpenPhoto: () -> Unit,
) {
    val displayedName = localizedSightName(sight.name)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardSurfaceColor())
            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x10141428), spotColor = Color(0x10141428))
            .border(if (selected) 2.dp else 0.dp, if (selected) OdysseyPurple else Color.Transparent, RoundedCornerShape(18.dp))
            .padding(11.dp),
    ) {
        Box(modifier = Modifier.size(82.dp).clip(RoundedCornerShape(13.dp))) {
            SightPhoto(
                sight = sight,
                modifier = Modifier.fillMaxSize(),
                onClick = onOpenPhoto.takeIf { !uploading },
            )
        }
        Column(modifier = Modifier.weight(1f).clickable { onSelect() }, verticalArrangement = Arrangement.Center) {
            Text(
                displayedName,
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 15.sp,
                lineHeight = 17.25.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                localizedSightInfo(sight.description, sight.category),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 3.dp),
            )
            if (sight.rating != null) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 8.dp)) {
                    Text(
                        "★",
                        color = Color(0xFFF5A623),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W400,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                    Text(
                        sight.rating.toString(),
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 12.sp,
                        lineHeight = 17.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}
@Composable
private fun EditSightPanel(sight: com.odyssey.travelplanner.data.Sight, tripId: String, onClose: () -> Unit, onSaved: () -> Unit) {
    val language = LocalLanguage.current
    val displayedName = localizedSightName(sight.name)
    val displayedCity = localizedCityName(sight.city)
    val displayedCategory = localizedSightCategory(sight.category)
    val displayedDescription = localizedSightDescription(sight.description)
    var name by remember(sight.id, language) { mutableStateOf(displayedName) }
    var city by remember(sight.id, language) { mutableStateOf(displayedCity) }
    var category by remember(sight.id, language) { mutableStateOf(displayedCategory) }
    var description by remember(sight.id, language) { mutableStateOf(displayedDescription) }
    var walkDay by remember(sight.id) { mutableStateOf(sight.walkDay.toString()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("Редактировать место", "Edit sight", "Editar lugar", "Ort bearbeiten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
        AuthField(localized("Название", "Name", "Nombre", "Name"), localized("Название", "Name", "Nombre", "Name"), name) { name = it }
        AuthField(localized("Город", "City", "Ciudad", "Stadt"), localized("Город", "City", "Ciudad", "Stadt"), city) { city = it }
        AuthField(localized("Категория", "Category", "Categoría", "Kategorie"), localized("Категория", "Category", "Categoría", "Kategorie"), category) { category = it }
        AuthField(localized("Описание", "Description", "Descripción", "Beschreibung"), localized("Что важно увидеть", "What is important to see", "Qué es importante ver", "Was sehenswert ist"), description) { description = it }
        AuthField(localized("День маршрута", "Route day", "Día de ruta", "Reisetag"), localized("Например, 1", "For example, 1", "Por ejemplo, 1", "Zum Beispiel 1"), walkDay) { walkDay = it.filter(Char::isDigit) }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = {
                scope.launch {
                    saving = true
                    val savedName = name.trim().takeUnless { it == displayedName } ?: sight.name.trim()
                    val savedCity = city.trim().takeUnless { it == displayedCity } ?: sight.city.trim()
                    val savedCategory = category.trim().takeUnless { it == displayedCategory } ?: sight.category.trim()
                    val savedDescription = description.trim().takeUnless { it == displayedDescription } ?: sight.description.trim()
                    runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateSightDetailsRich(tripId, sight.id, savedName, savedCity, savedCategory, savedDescription, walkDay.toIntOrNull() ?: sight.walkDay) }
                        .onSuccess { onSaved() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось сохранить место", "Could not save sight", "No se pudo guardar el lugar", "Ort konnte nicht gespeichert werden") }
                    saving = false
                }
            }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(11.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun RestaurantsContent(tripId: String, overview: TripOverview, onRestaurantAdded: () -> Unit) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var cuisine by remember { mutableStateOf("") }
    var dateTime by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("хочу") }
    var priority by remember { mutableStateOf(false) }
    var price by remember { mutableStateOf("€€") }
    var newRestaurantPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var savingRestaurantId by remember { mutableStateOf<String?>(null) }
    var editingRestaurant by remember { mutableStateOf<com.odyssey.travelplanner.data.Restaurant?>(null) }
    var cityPickerOpen by remember { mutableStateOf(false) }
    var uploadingRestaurantId by remember { mutableStateOf<String?>(null) }
    var selectedCity by remember { mutableStateOf("Все города") }
    var cityMenuOpen by remember { mutableStateOf(false) }
    var filterMenuOpen by remember { mutableStateOf(false) }
    var priceFilter by remember { mutableStateOf("") }
    var ratingFilter by remember { mutableStateOf("") }
    var appliedTypeFilter by remember { mutableStateOf("Ресторан") }
    var appliedFeatureFilters by remember { mutableStateOf(setOf<String>()) }
    var draftTypeFilter by remember { mutableStateOf("Ресторан") }
    var draftFeatureFilters by remember { mutableStateOf(emptySet<String>()) }
    var draftPriceFilter by remember { mutableStateOf("") }
    var draftRatingFilter by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()
    val tripCityOptions = (
        overview.cities +
            overview.routeLegs.flatMap { listOf(it.from, it.to) } +
        overview.sights.map { it.city } +
            overview.accommodations.map { it.city } +
            overview.restaurants.map { it.city }
        ).flatMap(::splitStoredCityList).map(String::trim).filter(String::isNotBlank).distinctBy(::cityFilterKey)
    val cityOptions = listOf("Все города") + tripCityOptions
    val visibleRestaurants = overview.restaurants.filter { restaurant ->
        val note = restaurant.note.lowercase()
        val typeMatches = when (appliedTypeFilter) {
            "Бар" -> note.contains("бар") || note.contains("bar")
            "Кафе" -> note.contains("кафе") || note.contains("cafe")
            else -> true
        }
        val featureMatches = appliedFeatureFilters.all { feature ->
            when (feature) {
                "priority" -> note.contains("приоритет") || note.contains("priority")
                "dog" -> note.contains("с собакой") || note.contains("dog")
                "reservation" -> restaurant.status == "бронь" || note.contains("бронь") || note.contains("reserv")
                "vegan" -> note.contains("веган") || note.contains("vegan")
                else -> true
            }
        }
        val ratingMatches = ratingFilter.isBlank() || (restaurant.rating ?: 0.0) >= (ratingFilter.removeSuffix("+").toDoubleOrNull() ?: 0.0)
        (selectedCity == "Все города" || cityFilterKey(restaurant.city) == cityFilterKey(selectedCity)) &&
            typeMatches &&
            featureMatches &&
            (priceFilter.isBlank() || restaurant.price == priceFilter) &&
            ratingMatches
    }
    val filterCount = listOf(
        appliedTypeFilter != "Ресторан",
        priceFilter.isNotBlank(),
        ratingFilter.isNotBlank(),
    ).count { it } + appliedFeatureFilters.size
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val restaurantId = uploadingRestaurantId ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            uploadingRestaurantId = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось прочитать изображение")
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addRestaurantPhoto(tripId, restaurantId, bytes)
            }.onSuccess { onRestaurantAdded() }
            uploadingRestaurantId = null
        }
    }
    val newRestaurantPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        newRestaurantPhotoUri = uri
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth().height(44.dp),
                horizontalArrangement = Arrangement.spacedBy(9.dp),
            ) {
                Row(
                    modifier = Modifier
                        .weight(1.31f)
                        .fillMaxHeight()
                        .shadow(5.dp, RoundedCornerShape(13.dp), clip = false, ambientColor = Color(0x476C5CE7), spotColor = Color(0x476C5CE7))
                        .clip(RoundedCornerShape(13.dp))
                        .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))))
                        .clickable { cityMenuOpen = !cityMenuOpen }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    OdysseyLocationIcon(15.dp, Color.White)
                    Text(
                        localizedCityFilter(selectedCity),
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OdysseyChevronDown(16.dp, Color.White)
                }
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(13.dp))
                        .background(cardSurfaceColor())
                        .border(1.dp, OdysseyBorder, RoundedCornerShape(13.dp))
                        .clickable {
                            cityMenuOpen = false
                            filterMenuOpen = true
                        }
                        .padding(horizontal = 13.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(7.dp),
                ) {
                    OdysseyFilterIcon(15.dp)
                    Text(
                        localized("Фильтры", "Filters", "Filtros", "Filter"),
                        color = OdysseyLabel,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 14.sp,
                        lineHeight = 19.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                    Box(
                        modifier = Modifier.size(20.dp).clip(RoundedCornerShape(10.dp)).background(OdysseyPurple),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            filterCount.toString(),
                            color = Color.White,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 11.sp,
                            lineHeight = 14.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }
            }
        }
        if (cityMenuOpen) {
            item {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cardSurfaceColor()).padding(7.dp)) {
                    cityOptions.forEach { option ->
                        Text(localizedCityFilter(option), color = if (option == selectedCity) OdysseyPurple else contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (option == selectedCity) OdysseyTint else Color.Transparent).clickable { selectedCity = option; cityMenuOpen = false }.padding(horizontal = 12.dp, vertical = 11.dp))
                    }
                }
            }
        }
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp)
                    .height(47.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(OdysseyTint)
                    .drawBehind {
                        val stroke = 1.5.dp.toPx()
                        drawRoundRect(
                            color = Color(0xFFCFC7F2),
                            topLeft = Offset(stroke / 2f, stroke / 2f),
                            size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                            cornerRadius = CornerRadius(14.dp.toPx() - stroke / 2f),
                            style = Stroke(
                                width = stroke,
                                pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx())),
                            ),
                        )
                    }
                    .clickable { adding = true; message = null },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                OdysseyPlusIcon(18.dp)
                Text(
                    localized("Добавить ресторан", "Add restaurant", "Añadir restaurante", "Restaurant hinzufügen"),
                    color = OdysseyPurple,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
        item {
            RestaurantMapCard(
                restaurants = visibleRestaurants,
                modifier = Modifier.padding(top = 11.dp),
            )
        }
        if (visibleRestaurants.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Outlined.Restaurant,
                    title = if (overview.restaurants.isEmpty()) localized("Рестораны пока не добавлены", "No restaurants added yet", "Aún no se han añadido restaurantes", "Noch keine Restaurants hinzugefügt") else localized("Ничего не найдено", "Nothing found", "No se encontró nada", "Nichts gefunden"),
                    body = localized("Добавьте место или измените фильтры", "Add a place or change the filters", "Añada un lugar o cambie los filtros", "Fügen Sie einen Ort hinzu oder ändern Sie die Filter"),
                )
            }
        } else {
            itemsIndexed(visibleRestaurants, key = { _, restaurant -> restaurant.id }) { index, restaurant ->
                RestaurantCard(
                    restaurant,
                    savingRestaurantId == restaurant.id,
                    uploadingRestaurantId == restaurant.id,
                    onEdit = { editingRestaurant = restaurant },
                    onAddPhoto = { uploadingRestaurantId = restaurant.id; photoPicker.launch("image/*") },
                    modifier = Modifier.padding(top = if (index == 0) 16.dp else 13.dp),
                ) { status ->
                    scope.launch {
                        savingRestaurantId = restaurant.id
                        runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateRestaurantStatus(tripId, restaurant.id, status) }
                            .onSuccess { onRestaurantAdded() }
                        savingRestaurantId = null
                    }
                }
            }
        }
    }
    if (editingRestaurant != null) {
        ModalBottomSheet(
            onDismissRequest = { editingRestaurant = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = Color(0x730F0F19),
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            RestaurantEditSheet(
                restaurant = editingRestaurant!!,
                tripId = tripId,
                onClose = { editingRestaurant = null },
                onSaved = {
                    editingRestaurant = null
                    onRestaurantAdded()
                },
            )
        }
    }
    if (filterMenuOpen) {
        ModalBottomSheet(
            onDismissRequest = { filterMenuOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = Color(0x730F0F19),
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            RestaurantFilterSheet(
                type = draftTypeFilter,
                features = draftFeatureFilters,
                price = draftPriceFilter,
                rating = draftRatingFilter,
                onTypeChange = { draftTypeFilter = it },
                onFeatureToggle = { feature ->
                    draftFeatureFilters = if (feature in draftFeatureFilters) draftFeatureFilters - feature else draftFeatureFilters + feature
                },
                onPriceChange = { draftPriceFilter = it },
                onRatingChange = { draftRatingFilter = it },
                onReset = {
                    draftTypeFilter = "Ресторан"
                    draftFeatureFilters = emptySet()
                    draftPriceFilter = ""
                    draftRatingFilter = ""
                },
                onApply = {
                    appliedTypeFilter = draftTypeFilter
                    appliedFeatureFilters = draftFeatureFilters
                    priceFilter = draftPriceFilter
                    ratingFilter = draftRatingFilter
                    filterMenuOpen = false
                },
            )
        }
    }
    if (adding) {
        ModalBottomSheet(
            onDismissRequest = { adding = false; message = null; cityPickerOpen = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = Color(0x730F0F19),
            shape = RoundedCornerShape(topStart = 29.dp, topEnd = 29.dp),
            dragHandle = null,
        ) {
            RestaurantAddSheet(
                name = name,
                city = city,
                cuisine = cuisine,
                dateTime = dateTime,
                price = price,
                address = address,
                status = status,
                priority = priority,
                cityOptions = tripCityOptions,
                cityPickerOpen = cityPickerOpen,
                photoUri = newRestaurantPhotoUri,
                saving = saving,
                message = message,
                onNameChange = { name = it },
                onCityChange = { city = it },
                onCityPickerOpen = { cityPickerOpen = true },
                onCityPickerDismiss = { cityPickerOpen = false },
                onDatePickerOpen = {
                    val today = Calendar.getInstance()
                    DatePickerDialog(
                        localizedDatePickerContext(context, language),
                        { _, year, month, dayOfMonth ->
                            dateTime = String.format(Locale.US, "%04d-%02d-%02d", year, month + 1, dayOfMonth)
                        },
                        today.get(Calendar.YEAR),
                        today.get(Calendar.MONTH),
                        today.get(Calendar.DAY_OF_MONTH),
                    ).show()
                },
                onCuisineChange = { cuisine = it },
                onDateTimeChange = { dateTime = it },
                onPriceChange = { price = it },
                onAddressChange = { address = it },
                onStatusChange = { status = it },
                onPriorityChange = { priority = !priority },
                onPickPhoto = { newRestaurantPhotoPicker.launch("image/*") },
                onClose = { adding = false; message = null; cityPickerOpen = false },
                onSave = {
                    scope.launch {
                        saving = true
                        runCatching {
                            val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                            val restaurantId = repository.addRestaurantDetails(
                                com.odyssey.travelplanner.data.RestaurantInput(
                                    name = name,
                                    city = city,
                                    status = status,
                                    note = cuisine,
                                    price = price,
                                    link = address,
                                    date = dateTime,
                                ),
                                tripId,
                            )
                            newRestaurantPhotoUri?.let { uri ->
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    ?: error("Не удалось прочитать изображение")
                                repository.addRestaurantPhoto(tripId, restaurantId, bytes)
                            }
                        }.onSuccess {
                            adding = false
                            message = null
                            cityPickerOpen = false
                            name = ""
                            city = ""
                            cuisine = ""
                            dateTime = ""
                            price = "€€"
                            address = ""
                            status = "хочу"
                            priority = false
                            newRestaurantPhotoUri = null
                            onRestaurantAdded()
                        }.onFailure {
                            message = it.message ?: localized(language, "Не удалось сохранить ресторан", "Could not save restaurant", "No se pudo guardar el restaurante", "Restaurant konnte nicht gespeichert werden")
                        }
                        saving = false
                    }
                },
            )
        }
    }
}

@Composable
private fun RestaurantAddSheet(
    name: String,
    city: String,
    cuisine: String,
    dateTime: String,
    price: String,
    address: String,
    status: String,
    priority: Boolean,
    cityOptions: List<String>,
    cityPickerOpen: Boolean,
    photoUri: Uri?,
    saving: Boolean,
    message: String?,
    onNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onCityPickerOpen: () -> Unit,
    onCityPickerDismiss: () -> Unit,
    onDatePickerOpen: () -> Unit,
    onCuisineChange: (String) -> Unit,
    onDateTimeChange: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onPriorityChange: () -> Unit,
    onPickPhoto: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val inputTextStyle = androidx.compose.ui.text.TextStyle(
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = s(15f),
            lineHeight = s(20f),
            platformStyle = OdysseyNoFontPadding,
        )
        val labelStyle = androidx.compose.ui.text.TextStyle(
            color = OdysseyLabel,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            platformStyle = OdysseyNoFontPadding,
        )
        val scrollState = rememberScrollState()
        val photoScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(704f))
                .verticalScroll(scrollState),
        ) {
            Box(Modifier.fillMaxWidth().height(d(876f))) {
                Box(
                    modifier = Modifier
                        .offset(x = d(156.5f), y = d(12f))
                        .size(d(40f), d(4f))
                        .clip(RoundedCornerShape(d(2f)))
                        .background(Color(0xFFE2E2E8)),
                )

                Text(
                    text = localized("Новый ресторан", "New restaurant", "Nuevo restaurante", "Neues Restaurant"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(24f),
                    lineHeight = s(33f),
                    letterSpacing = (-0.24f * scale).sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier.offset(x = d(16f), y = d(30f)).width(d(260f)).height(d(34f)),
                    maxLines = 1,
                    softWrap = false,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = d(303f), y = d(30f))
                        .size(d(34f))
                        .clip(CircleShape)
                        .background(OdysseySurface2)
                        .clickable(onClick = onClose),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = OdysseySubtext, modifier = Modifier.size(d(16f)))
                }

                Text(
                    text = localized("Фотографии", "Photos", "Fotos", "Fotos"),
                    style = labelStyle,
                    modifier = Modifier.offset(x = d(16f), y = d(82f)).width(d(321f)).height(d(18f)),
                )
                Box(
                    modifier = Modifier
                        .offset(x = d(16f), y = d(108f))
                        .width(d(321f))
                        .height(d(172f))
                        .horizontalScroll(photoScrollState),
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(d(10f)),
                        modifier = Modifier.width(d(674f)).height(d(168f)),
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(d(240f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(16f)))
                                .background(OdysseySurface2)
                                .drawBehind {
                                    val stroke = d(1f).toPx()
                                    drawRoundRect(
                                        color = Color(0xFFCFC7F2),
                                        topLeft = Offset(stroke / 2f, stroke / 2f),
                                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                                        cornerRadius = CornerRadius(d(16f).toPx() - stroke / 2f),
                                        style = Stroke(
                                            width = stroke,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx())),
                                        ),
                                    )
                                }
                                .clickable(onClick = onPickPhoto),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = OdysseyPurple, modifier = Modifier.size(d(26f)))
                                Text(
                                    text = localized("Обложка — перетащите фото\nили выберите файл", "Cover — drag a photo\nor choose a file", "Portada — arrastre una foto\no elija un archivo", "Cover — Foto ziehen\noder Datei auswählen"),
                                    color = OdysseySubtext,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W800,
                                    fontSize = s(11.5f),
                                    lineHeight = s(17f),
                                    textAlign = TextAlign.Center,
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                    modifier = Modifier.padding(top = d(6f)),
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .width(d(128f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(14f)))
                                .background(Color(0xFFE9E7F4)),
                        ) {
                            if (photoUri != null) {
                                AsyncImage(
                                    model = photoUri,
                                    contentDescription = localized("Обложка ресторана", "Restaurant cover", "Portada del restaurante", "Restaurant-Titelbild"),
                                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize(),
                                )
                            }
                            Text(
                                text = localized("Обложка", "Cover", "Portada", "Cover"),
                                color = Color.White,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W800,
                                fontSize = s(10f),
                                lineHeight = s(14f),
                                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                modifier = Modifier
                                    .align(Alignment.TopStart)
                                    .padding(start = d(8f), top = d(8f))
                                    .background(Color(0x8C141419), RoundedCornerShape(d(20f)))
                                    .padding(horizontal = d(7f), vertical = d(3f)),
                            )
                        }

                        Box(
                            modifier = Modifier
                                .width(d(128f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(14f)))
                                .background(Color(0xFFE9E7F4)),
                        )
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(d(128f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(14f)))
                                .background(OdysseySurface2)
                                .drawBehind {
                                    val stroke = d(1f).toPx()
                                    drawRoundRect(
                                        color = Color(0xFFCFC7F2),
                                        topLeft = Offset(stroke / 2f, stroke / 2f),
                                        size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                                        cornerRadius = CornerRadius(d(14f).toPx() - stroke / 2f),
                                        style = Stroke(
                                            width = stroke,
                                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx())),
                                        ),
                                    )
                                }
                                .clickable(onClick = onPickPhoto),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OdysseyPlusIcon(d(18f))
                                Text(
                                    text = localized("Добавить", "Add", "Añadir", "Hinzufügen"),
                                    color = OdysseyPurple,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W800,
                                    fontSize = s(11.5f),
                                    lineHeight = s(15f),
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                    modifier = Modifier.padding(top = d(5f)),
                                )
                            }
                        }
                    }
                }

                RestaurantAddField(
                    label = localized("Название", "Name", "Nombre", "Name"),
                    value = name,
                    placeholder = localized("Название места", "Restaurant name", "Nombre del lugar", "Name des Lokals"),
                    scale = scale,
                    modifier = Modifier.offset(x = d(16f), y = d(296f)).width(d(321f)),
                    onValueChange = onNameChange,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d(12f)),
                    modifier = Modifier.offset(x = d(16f), y = d(389f)).width(d(321f)),
                ) {
                    RestaurantAddField(
                        label = localized("Город", "City", "Ciudad", "Stadt"),
                        value = if (city.isBlank()) "" else localizedCityName(city),
                        placeholder = localized("Выберите город", "Choose a city", "Elija una ciudad", "Stadt auswählen"),
                        scale = scale,
                        trailingChevron = true,
                        readOnly = true,
                        onClick = onCityPickerOpen,
                        modifier = Modifier.width(d(154.5f)),
                        onValueChange = { onCityChange(it) },
                    )
                    RestaurantAddField(
                        label = localized("Кухня", "Cuisine", "Cocina", "Küche"),
                        value = cuisine,
                        placeholder = localized("Например, итальянская", "For example, Italian", "Por ejemplo, italiana", "Zum Beispiel italienisch"),
                        scale = scale,
                        modifier = Modifier.width(d(154.5f)),
                        onValueChange = onCuisineChange,
                    )
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d(12f)),
                    modifier = Modifier.offset(x = d(16f), y = d(482f)).width(d(321f)),
                ) {
                    RestaurantAddField(
                        label = localized("Дата и время", "Date and time", "Fecha y hora", "Datum und Uhrzeit"),
                        value = dateTime,
                        placeholder = localized("Выберите дату", "Choose date", "Elija una fecha", "Datum auswählen"),
                        scale = scale,
                        trailingChevron = true,
                        readOnly = true,
                        onClick = onDatePickerOpen,
                        modifier = Modifier.width(d(154.5f)),
                        onValueChange = { onDateTimeChange(it) },
                    )
                    RestaurantAddPriceField(
                        selected = price,
                        scale = scale,
                        modifier = Modifier.width(d(154.5f)),
                        onSelect = onPriceChange,
                    )
                }
                RestaurantAddField(
                    label = localized("Адрес", "Address", "Dirección", "Adresse"),
                    value = address,
                    placeholder = localized("Адрес ресторана", "Restaurant address", "Dirección del restaurante", "Adresse des Lokals"),
                    scale = scale,
                    modifier = Modifier.offset(x = d(16f), y = d(577f)).width(d(321f)),
                    onValueChange = onAddressChange,
                )

                Text(
                    text = localized("Статус", "Status", "Estado", "Status"),
                    style = labelStyle,
                    modifier = Modifier.offset(x = d(16f), y = d(670f)).width(d(321f)).height(d(18f)),
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(d(9f)),
                    modifier = Modifier.offset(x = d(16f), y = d(696f)).height(d(38f)),
                ) {
                    RestaurantAddStatusChip(localized("хочу", "want", "quiero", "möchte"), "хочу", status == "хочу", 61.4f, scale, onStatusChange)
                    RestaurantAddStatusChip(localized("бронь", "reserved", "reserva", "Reservierung"), "бронь", status == "бронь", 71.4f, scale, onStatusChange)
                    RestaurantAddStatusChip(localized("были", "visited", "visitado", "besucht"), "были", status == "были", 65.1f, scale, onStatusChange)
                }
                RestaurantAddStatusChip(
                    label = localized("🔥 Приоритет", "🔥 Priority", "🔥 Prioridad", "🔥 Priorität"),
                    value = "priority",
                    selected = priority,
                    width = 124.1f,
                    scale = scale,
                    onClick = { onPriorityChange() },
                    modifier = Modifier.offset(x = d(16f), y = d(743f)),
                )

                if (message != null) {
                    Text(
                        text = message,
                        color = Color(0xFFE0524B),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W700,
                        fontSize = s(11f),
                        lineHeight = s(15f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        modifier = Modifier.offset(x = d(16f), y = d(784f)).width(d(336f)),
                    )
                }

                Box(
                    modifier = Modifier
                        .offset(x = d(16f), y = d(805f))
                        .width(d(135.3f))
                        .height(d(53f))
                        .clip(RoundedCornerShape(d(15f)))
                        .border(d(1f), OdysseyBorder, RoundedCornerShape(d(15f)))
                        .background(Color.White)
                        .clickable(onClick = onClose),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"),
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
                Box(
                    modifier = Modifier
                        .offset(x = d(162.3f), y = d(805f))
                        .width(d(174.7f))
                        .height(d(53f))
                        .shadow(d(8f), RoundedCornerShape(d(15f)), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7))
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))))
                        .clickable(enabled = !saving, onClick = onSave),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"),
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }
        }
    }
    if (cityPickerOpen) {
        AlertDialog(
            onDismissRequest = onCityPickerDismiss,
            title = { Text(localized("Выберите город", "Choose a city", "Elija una ciudad", "Stadt auswählen"), fontFamily = Manrope, fontWeight = FontWeight.W800) },
            text = {
                if (cityOptions.isEmpty()) {
                    Text(localized("В поездке пока нет городов", "No cities have been added to this trip yet", "Aún no hay ciudades en este viaje", "Für diese Reise wurden noch keine Städte hinzugefügt"), fontFamily = Manrope)
                } else {
                    Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                        cityOptions.forEach { option ->
                            Text(
                                text = localizedCityName(option),
                                color = if (option == city) OdysseyPurple else contentTextColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W700,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (option == city) OdysseyTint else Color.Transparent)
                                    .clickable {
                                        onCityChange(option)
                                        onCityPickerDismiss()
                                    }
                                    .padding(horizontal = 12.dp, vertical = 12.dp),
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = onCityPickerDismiss) {
                    Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800)
                }
            },
        )
    }
}

@Composable
private fun RestaurantAddField(
    label: String,
    value: String,
    placeholder: String,
    scale: Float,
    modifier: Modifier = Modifier,
    trailingChevron: Boolean = false,
    readOnly: Boolean = false,
    onClick: (() -> Unit)? = null,
    valueWeight: FontWeight = FontWeight.W600,
    onValueChange: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    val textStyle = androidx.compose.ui.text.TextStyle(
        color = contentTextColor(),
        fontFamily = Manrope,
        fontWeight = valueWeight,
        fontSize = s(15f),
        lineHeight = s(20f),
        platformStyle = OdysseyNoFontPadding,
    )
    Column(modifier = modifier.height(d(77f))) {
        Text(
            text = label,
            color = OdysseyLabel,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(d(18f)),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(d(8f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(Color.White)
                .border(d(1f), OdysseyBorder, RoundedCornerShape(d(14f))),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                readOnly = readOnly,
                singleLine = true,
                textStyle = textStyle,
                cursorBrush = androidx.compose.ui.graphics.SolidColor(OdysseyPurple),
                modifier = Modifier
                    .fillMaxSize()
                    .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(start = d(15f), end = if (trailingChevron) d(34f) else d(15f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                color = Color(0xFFA0A0AA),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W600,
                                fontSize = s(15f),
                                lineHeight = s(20f),
                                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
            if (trailingChevron) {
                Box(
                    Modifier
                        .fillMaxSize()
                        .padding(end = d(12f))
                        .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
                    contentAlignment = Alignment.CenterEnd,
                ) {
                    OdysseyChevronDown(d(16f), OdysseySubtext)
                }
            }
        }
    }
}

@Composable
private fun RestaurantAddPriceField(
    selected: String,
    scale: Float,
    modifier: Modifier = Modifier,
    onSelect: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Column(modifier = modifier.height(d(79f))) {
        Text(
            text = localized("Средний чек", "Average price", "Precio medio", "Durchschnittspreis"),
            color = OdysseyLabel,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(d(18f)),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(d(8f)))
        Row(
            horizontalArrangement = Arrangement.spacedBy(d(3.5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(OdysseyTrack)
                .padding(d(4f)),
        ) {
            listOf("€", "€€", "€€€", "€€€€").forEach { option ->
                val active = option == selected
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .height(d(43f))
                        .shadow(if (active) d(2f) else 0.dp, RoundedCornerShape(d(11f)), clip = false, ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
                        .clip(RoundedCornerShape(d(11f)))
                        .background(if (active) Color.White else Color.Transparent)
                        .clickable { onSelect(option) },
                ) {
                    Text(
                        text = option,
                        color = if (active) contentTextColor() else Color(0xFFA0A0AA),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(12f),
                        lineHeight = s(16f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun RestaurantAddStatusChip(
    label: String,
    value: String,
    selected: Boolean,
    width: Float,
    scale: Float,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .width(d(width))
            .height(d(38f))
            .clip(RoundedCornerShape(d(12f)))
            .background(if (selected) OdysseyPurple else Color.White)
            .border(d(1f), if (selected) OdysseyPurple else OdysseyBorder, RoundedCornerShape(d(12f)))
            .clickable { onClick(value) },
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else OdysseySubtext,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13.5f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun RestaurantEditSheet(
    restaurant: com.odyssey.travelplanner.data.Restaurant,
    tripId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val language = LocalLanguage.current
    var name by remember(restaurant.id) { mutableStateOf(restaurant.name) }
    var status by remember(restaurant.id) { mutableStateOf(restaurant.status.ifBlank { "хочу" }) }
    var whenBooked by remember(restaurant.id) { mutableStateOf(restaurant.date) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val booked = status == "бронь"
        val sheetHeight = if (booked) 470f else 377f

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(sheetHeight)),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = d(164f), y = d(12f))
                    .size(d(40f), d(4f))
                    .clip(RoundedCornerShape(d(2f)))
                    .background(Color(0xFFE6E6EC)),
            )
            Text(
                text = localized("Редактировать\nресторан", "Edit\nrestaurant", "Editar\nrestaurante", "Restaurant\nbearbeiten"),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = s(24f),
                lineHeight = s(33f),
                style = androidx.compose.ui.text.TextStyle(
                    letterSpacing = s(-0.24f),
                    platformStyle = OdysseyNoFontPadding,
                ),
                maxLines = 2,
                modifier = Modifier
                    .offset(x = d(16f), y = d(32f))
                    .width(d(292f))
                    .height(d(66f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = d(318f), y = d(48f))
                    .size(d(34f))
                    .clip(CircleShape)
                    .background(OdysseySurface2)
                    .clickable(onClick = onClose),
            ) {
                Icon(
                    Icons.Filled.Close,
                    contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"),
                    tint = OdysseySubtext,
                    modifier = Modifier.size(d(16f)),
                )
            }

            RestaurantAddField(
                label = localized("Название ресторана", "Restaurant name", "Nombre del restaurante", "Name des Restaurants"),
                value = name,
                placeholder = localized("Название ресторана", "Restaurant name", "Nombre del restaurante", "Name des Restaurants"),
                scale = scale,
                valueWeight = FontWeight.W700,
                modifier = Modifier
                    .offset(x = d(16f), y = d(118f))
                    .width(d(336f)),
                onValueChange = { name = it },
            )

            Text(
                text = localized("Статус", "Status", "Estado", "Status"),
                color = OdysseyLabel,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = s(13f),
                lineHeight = s(18f),
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier
                    .offset(x = d(16f), y = d(211f))
                    .width(d(336f))
                    .height(d(18f)),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(237f))
                    .width(d(336f))
                    .height(d(45f)),
            ) {
                RestaurantEditStatusChip(
                    label = localized("хочу", "want", "quiero", "möchte"),
                    value = "хочу",
                    selected = status == "хочу",
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { status = it },
                )
                RestaurantEditStatusChip(
                    label = localized("бронь", "reserved", "reserva", "Reservierung"),
                    value = "бронь",
                    selected = status == "бронь",
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { status = it },
                )
                RestaurantEditStatusChip(
                    label = localized("были", "visited", "visitado", "besucht"),
                    value = "были",
                    selected = status == "были",
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { status = it },
                )
            }

            if (booked) {
                RestaurantAddField(
                    label = localized("Когда бронь", "Reservation time", "Hora de la reserva", "Reservierungszeit"),
                    value = whenBooked,
                    placeholder = localized("Напр. 28 сен · 20:00", "E.g. 28 Sep · 20:00", "P. ej. 28 sep · 20:00", "Z. B. 28. Sep. · 20:00"),
                    scale = scale,
                    valueWeight = FontWeight.W600,
                    modifier = Modifier
                        .offset(x = d(16f), y = d(298f))
                        .width(d(336f)),
                    onValueChange = { whenBooked = it },
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(d(11f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(if (booked) 399f else 306f))
                    .width(d(336f))
                    .height(d(53f)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Color.White)
                        .border(d(1f), OdysseyBorder, RoundedCornerShape(d(15f)))
                        .clickable(onClick = onClose),
                ) {
                    Text(
                        text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"),
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .weight(1.3f)
                        .fillMaxHeight()
                        .shadow(
                            d(8f),
                            RoundedCornerShape(d(15f)),
                            clip = false,
                            ambientColor = Color(0x4D6C5CE7),
                            spotColor = Color(0x4D6C5CE7),
                        )
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))))
                        .clickable(enabled = !saving) {
                            scope.launch {
                                saving = true
                                message = null
                                runCatching {
                                    SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateRestaurantDetailsRich(
                                        tripId = tripId,
                                        restaurantId = restaurant.id,
                                        input = com.odyssey.travelplanner.data.RestaurantInput(
                                            name = name,
                                            city = restaurant.city,
                                            status = status,
                                            note = restaurant.note,
                                            price = restaurant.price,
                                            link = restaurant.link,
                                            date = whenBooked,
                                        ),
                                    )
                                }.onSuccess {
                                    onSaved()
                                }.onFailure {
                                    message = it.message ?: localized(language, "Не удалось сохранить ресторан", "Could not save restaurant", "No se pudo guardar el restaurante", "Restaurant konnte nicht gespeichert werden")
                                }
                                saving = false
                            }
                        },
                ) {
                    Text(
                        text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"),
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }

            message?.let {
                Text(
                    text = it,
                    color = Color(0xFFE0524B),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = s(11f),
                    lineHeight = s(15f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier
                        .offset(x = d(16f), y = d(if (booked) 455f else 362f))
                        .width(d(336f)),
                )
            }
        }
    }
}

@Composable
private fun RestaurantEditStatusChip(
    label: String,
    value: String,
    selected: Boolean,
    scale: Float,
    modifier: Modifier = Modifier,
    onClick: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(d(12f)))
            .background(if (selected) OdysseyPurple else Color.White)
            .border(d(1.5f), if (selected) OdysseyPurple else OdysseyBorder, RoundedCornerShape(d(12f)))
            .clickable { onClick(value) },
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else OdysseySubtext,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(14f),
            lineHeight = s(19f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun RestaurantFilterSheet(
    type: String,
    features: Set<String>,
    price: String,
    rating: String,
    onTypeChange: (String) -> Unit,
    onFeatureToggle: (String) -> Unit,
    onPriceChange: (String) -> Unit,
    onRatingChange: (String) -> Unit,
    onReset: () -> Unit,
    onApply: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val sectionStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(11f),
            lineHeight = s(15f),
            color = Color(0xFFB6B6BE),
            platformStyle = OdysseyNoFontPadding,
        )
        val bodyStyle = androidx.compose.ui.text.TextStyle(
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13.5f),
            lineHeight = s(18f),
            platformStyle = OdysseyNoFontPadding,
        )
        val controlShape = RoundedCornerShape(d(12f))
        val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

        Box(Modifier.fillMaxWidth().height(d(604f) + navigationBarInset)) {
            Box(
                modifier = Modifier
                    .offset(x = d(164f), y = d(12f))
                    .size(d(40f), d(4f))
                    .clip(RoundedCornerShape(d(2f)))
                    .background(Color(0xFFE6E6EC)),
            )
            Text(
                text = localized("Фильтры", "Filters", "Filtros", "Filter"),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = s(22f),
                lineHeight = s(30f),
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier.offset(x = d(16f), y = d(30f)).width(d(190f)).height(d(30f)),
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = d(271f), y = d(34.5f))
                    .width(d(81f))
                    .height(d(21f))
                    .clickable(onClick = onReset),
            ) {
                Text(
                    text = localized("Сбросить", "Reset", "Restablecer", "Zurücksetzen"),
                    color = OdysseyPurple,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(14f),
                    lineHeight = s(21f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
            }

            Text(
                text = localized("ТИП ЗАВЕДЕНИЯ", "VENUE TYPE", "TIPO DE LOCAL", "ART DES LOKALS"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(78f)).width(d(336f)).height(d(15f)),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(10f)),
                modifier = Modifier.offset(x = d(16f), y = d(103f)).width(d(336f)).height(d(75f)),
            ) {
                listOf(
                    "Ресторан" to "restaurant",
                    "Бар" to "bar",
                    "Кафе" to "cafe",
                ).forEach { (label, kind) ->
                    RestaurantFilterTypeButton(
                        label = localized(label, when (label) { "Бар" -> "Bar"; "Кафе" -> "Cafe"; else -> "Restaurant" }, when (label) { "Бар" -> "Bar"; "Кафе" -> "Café"; else -> "Restaurante" }, when (label) { "Бар" -> "Bar"; "Кафе" -> "Café"; else -> "Restaurant" }),
                        kind = kind,
                        selected = type == label,
                        scale = scale,
                        onClick = { onTypeChange(label) },
                    )
                }
            }

            Text(
                text = localized("ОСОБЕННОСТИ", "FEATURES", "CARACTERÍSTICAS", "MERKMALE"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(198f)).width(d(336f)).height(d(15f)),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier.offset(x = d(16f), y = d(223f)).height(d(38f)),
            ) {
                RestaurantFilterFeatureChip(localized("Приоритет", "Priority", "Prioridad", "Priorität"), "priority", "priority" in features, 122.5f, scale, onFeatureToggle)
                RestaurantFilterFeatureChip(localized("С собакой", "Dog-friendly", "Con perro", "Hundefreundlich"), "dog", "dog" in features, 117.1f, scale, onFeatureToggle)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier.offset(x = d(16f), y = d(270f)).height(d(38f)),
            ) {
                RestaurantFilterFeatureChip(localized("Есть бронь", "Has reservation", "Tiene reserva", "Reservierung vorhanden"), "reservation", "reservation" in features, 123.1f, scale, onFeatureToggle)
                RestaurantFilterFeatureChip(localized("Веган", "Vegan", "Vegano", "Vegan"), "vegan", "vegan" in features, 87.64f, scale, onFeatureToggle)
            }

            Text(
                text = localized("СРЕДНИЙ ЧЕК", "AVERAGE PRICE", "PRECIO MEDIO", "DURCHSCHNITTSPREIS"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(330f)).width(d(336f)).height(d(15f)),
            )
            RestaurantFilterSegmentedRow(
                options = listOf("€", "€€", "€€€", "€€€€"),
                selected = price,
                onSelect = onPriceChange,
                scale = scale,
                itemFontSize = 12f,
                modifier = Modifier.offset(x = d(16f), y = d(355f)),
            )

            Text(
                text = localized("РЕЙТИНГ ОТ", "RATING FROM", "VALORACIÓN DESDE", "BEWERTUNG AB"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(430f)).width(d(336f)).height(d(15f)),
            )
            RestaurantFilterSegmentedRow(
                options = listOf("4.0+", "4.5+", "4.8+"),
                selected = rating,
                onSelect = onRatingChange,
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(455f)),
            )

            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .offset(x = d(16f), y = d(532f))
                    .width(d(336f))
                    .height(d(54f))
                    .shadow(d(8f), RoundedCornerShape(d(15f)), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7))
                    .clip(RoundedCornerShape(d(15f)))
                    .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))))
                    .clickable(onClick = onApply),
            ) {
                Text(
                    text = localized("Показать результаты", "Show results", "Mostrar resultados", "Ergebnisse anzeigen"),
                    color = Color.White,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(16f),
                    lineHeight = s(22f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
            }
        }
    }
}

@Composable
private fun RestaurantFilterTypeButton(
    label: String,
    kind: String,
    selected: Boolean,
    scale: Float,
    onClick: () -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(d(7f)),
        modifier = Modifier
            .width(d(105.33f))
            .fillMaxHeight()
            .clip(RoundedCornerShape(d(15f)))
            .background(if (selected) Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))) else Brush.linearGradient(listOf(Color.White, Color.White)))
            .border(d(1.6f), if (selected) OdysseyPurple else OdysseyBorder, RoundedCornerShape(d(15f)))
            .clickable(onClick = onClick)
            .padding(top = d(14f), bottom = d(14f)),
    ) {
        RestaurantFilterTypeIcon(kind, d(20f), if (selected) Color.White else OdysseyPurple)
        Text(
            text = label,
            color = if (selected) Color.White else contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = d(13.5f).value.sp,
            lineHeight = d(18f).value.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
        )
    }
}

@Composable
private fun RestaurantFilterFeatureChip(
    label: String,
    kind: String,
    selected: Boolean,
    width: Float,
    scale: Float,
    onToggle: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(d(5f)),
        modifier = Modifier
            .width(d(width))
            .height(d(38f))
            .clip(RoundedCornerShape(d(12f)))
            .background(if (selected) OdysseyPurple else Color.White)
            .border(d(1.6f), if (selected) OdysseyPurple else OdysseyBorder, RoundedCornerShape(d(12f)))
            .clickable { onToggle(kind) }
            .padding(horizontal = d(13f)),
    ) {
        RestaurantFilterFeatureIcon(kind, d(14f), if (selected) Color.White else OdysseyPurple)
        Text(
            text = label,
            color = if (selected) Color.White else OdysseyLabel,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = d(13.5f).value.sp,
            lineHeight = d(18f).value.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun RestaurantFilterSegmentedRow(
    options: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    scale: Float,
    itemFontSize: Float = 14f,
    modifier: Modifier = Modifier,
) {
    fun d(value: Float) = (value * scale).dp
    Row(
        horizontalArrangement = Arrangement.spacedBy(d(5f)),
        modifier = modifier
            .width(d(336f))
            .height(d(53f))
            .clip(RoundedCornerShape(d(14f)))
            .background(Color(0xFFEEEEF2))
            .padding(d(5f)),
    ) {
        options.forEach { option ->
            val active = option == selected
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .height(d(43f))
                    .shadow(if (active) d(2f) else 0.dp, RoundedCornerShape(d(11f)), clip = false, ambientColor = Color(0x1A000000), spotColor = Color(0x1A000000))
                    .clip(RoundedCornerShape(d(11f)))
                    .background(if (active) Color.White else Color.Transparent)
                    .clickable { onSelect(option) },
            ) {
                Text(
                    text = option,
                    color = if (active) contentTextColor() else Color(0xFFA0A0AA),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = d(itemFontSize).value.sp,
                    lineHeight = d(itemFontSize * 1.35f).value.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RestaurantFilterTypeIcon(kind: String, iconSize: Dp, color: Color) {
    Canvas(Modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = 2.dp.toPx()
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        when (kind) {
            "restaurant" -> {
                drawLine(color, p(3f, 2f), p(3f, 9f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, p(5f, 2f), p(5f, 22f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, p(7f, 2f), p(7f, 9f), strokeWidth = stroke, cap = StrokeCap.Round)
                val fork = Path().apply {
                    moveTo(3f * sx, 9f * sy)
                    cubicTo(3f * sx, 10.1f * sy, 3.9f * sx, 11f * sy, 5f * sx, 11f * sy)
                    cubicTo(6.1f * sx, 11f * sy, 7f * sx, 10.1f * sy, 7f * sx, 9f * sy)
                }
                drawPath(fork, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                val spoon = Path().apply {
                    moveTo(17f * sx, 2f * sy)
                    lineTo(17f * sx, 12f * sy)
                    cubicTo(19f * sx, 12f * sy, 21f * sx, 10.5f * sy, 21f * sx, 7f * sy)
                    cubicTo(21f * sx, 3.5f * sy, 19f * sx, 2f * sy, 17f * sx, 2f * sy)
                    close()
                }
                drawPath(spoon, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
                drawLine(color, p(17f, 12f), p(17f, 22f), strokeWidth = stroke, cap = StrokeCap.Round)
            }
            "bar" -> {
                drawLine(color, p(8f, 22f), p(16f, 22f), strokeWidth = stroke, cap = StrokeCap.Round)
                drawLine(color, p(12f, 11f), p(12f, 22f), strokeWidth = stroke, cap = StrokeCap.Round)
                val glass = Path().apply { moveTo(3f * sx, 5f * sy); lineTo(21f * sx, 5f * sy); lineTo(12f * sx, 11f * sy); close() }
                drawPath(glass, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
            }
            else -> {
                val cup = Path().apply {
                    moveTo(2f * sx, 8f * sy)
                    lineTo(18f * sx, 8f * sy)
                    lineTo(18f * sx, 17f * sy)
                    cubicTo(18f * sx, 19.2f * sy, 16.2f * sx, 21f * sy, 14f * sx, 21f * sy)
                    lineTo(6f * sx, 21f * sy)
                    cubicTo(3.8f * sx, 21f * sy, 2f * sx, 19.2f * sy, 2f * sx, 17f * sy)
                    close()
                }
                drawPath(cup, color, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                val handle = Path().apply { moveTo(18f * sx, 8f * sy); lineTo(19f * sx, 8f * sy); cubicTo(24f * sx, 8f * sy, 24f * sx, 16f * sy, 19f * sx, 16f * sy); lineTo(18f * sx, 16f * sy) }
                drawPath(handle, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
            }
        }
    }
}

@Composable
private fun RestaurantFilterFeatureIcon(kind: String, iconSize: Dp, color: Color) {
    Canvas(Modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun p(x: Float, y: Float) = Offset(x * sx, y * sy)
        when (kind) {
            "priority" -> {
                val star = Path().apply {
                    moveTo(12f * sx, 2f * sy)
                    lineTo(15f * sx, 8.5f * sy)
                    lineTo(22f * sx, 9.1f * sy)
                    lineTo(16.7f * sx, 13.7f * sy)
                    lineTo(18.3f * sx, 20.5f * sy)
                    lineTo(12f * sx, 17.3f * sy)
                    lineTo(5.1f * sx, 20.5f * sy)
                    lineTo(6.7f * sx, 13.7f * sy)
                    lineTo(1.4f * sx, 9.1f * sy)
                    lineTo(8.4f * sx, 8.5f * sy)
                    close()
                }
                drawPath(star, color)
            }
            "dog" -> {
                drawCircle(color, radius = 2f * sx, center = p(5f, 9f))
                drawCircle(color, radius = 2f * sx, center = p(19f, 9f))
                drawCircle(color, radius = 2f * sx, center = p(9f, 5f))
                drawCircle(color, radius = 2f * sx, center = p(15f, 5f))
                val dog = Path().apply {
                    moveTo(12f * sx, 11f * sy)
                    cubicTo(9f * sx, 11f * sy, 7f * sx, 13.5f * sy, 7f * sx, 16f * sy)
                    cubicTo(7f * sx, 18f * sy, 8.5f * sx, 19f * sy, 10f * sx, 19f * sy)
                    cubicTo(11f * sx, 19f * sy, 11.5f * sx, 18.5f * sy, 12f * sx, 18.5f * sy)
                    cubicTo(12.5f * sx, 18.5f * sy, 13f * sx, 19f * sy, 14f * sx, 19f * sy)
                    cubicTo(15.5f * sx, 19f * sy, 17f * sx, 18f * sy, 17f * sx, 16f * sy)
                    cubicTo(17f * sx, 13.5f * sy, 15f * sx, 11f * sy, 12f * sx, 11f * sy)
                    close()
                }
                drawPath(dog, color)
            }
            "reservation" -> {
                val calendar = Path().apply {
                    moveTo(5f * sx, 4f * sy)
                    lineTo(19f * sx, 4f * sy)
                    cubicTo(20.1f * sx, 4f * sy, 21f * sx, 4.9f * sy, 21f * sx, 6f * sy)
                    lineTo(21f * sx, 20f * sy)
                    cubicTo(21f * sx, 21.1f * sy, 20.1f * sx, 22f * sy, 19f * sx, 22f * sy)
                    lineTo(5f * sx, 22f * sy)
                    cubicTo(3.9f * sx, 22f * sy, 3f * sx, 21.1f * sy, 3f * sx, 20f * sy)
                    lineTo(3f * sx, 6f * sy)
                    cubicTo(3f * sx, 4.9f * sy, 3.9f * sx, 4f * sy, 5f * sx, 4f * sy)
                }
                drawPath(calendar, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
                drawLine(color, p(8f, 2f), p(8f, 6f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, p(16f, 2f), p(16f, 6f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
                drawLine(color, p(3f, 10f), p(21f, 10f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
            else -> {
                val leaf = Path().apply {
                    moveTo(11f * sx, 20f * sy)
                    cubicTo(7f * sx, 20f * sy, 4f * sx, 17f * sy, 4f * sx, 13f * sy)
                    cubicTo(4f * sx, 7f * sy, 10f * sx, 3f * sy, 20f * sx, 3f * sy)
                    cubicTo(19f * sx, 11f * sy, 15f * sx, 18f * sy, 11f * sx, 20f * sy)
                    close()
                }
                drawPath(leaf, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
                drawLine(color, p(11f, 20f), p(17f, 12f), strokeWidth = 2.dp.toPx(), cap = StrokeCap.Round)
            }
        }
    }
}

@Composable
private fun RestaurantCard(
    restaurant: com.odyssey.travelplanner.data.Restaurant,
    saving: Boolean,
    uploading: Boolean,
    onEdit: () -> Unit,
    onAddPhoto: () -> Unit,
    modifier: Modifier = Modifier,
    onStatusChange: (String) -> Unit,
) {
    val photos = restaurant.photos
    var photoIndex by remember(restaurant.id, photos) { mutableStateOf(0) }
    var fullScreenPhotoIndex by remember(restaurant.id, photos) { mutableStateOf<Int?>(null) }
    val activePhotoIndex = photoIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    val displayedNote = localizedRestaurantNote(restaurant.note)
    val booked = restaurant.status == "бронь"
    val visited = restaurant.status == "были"
    val reviewsLabel = restaurant.reviews.trim().let { raw ->
        when {
            raw.isBlank() -> ""
            raw.contains("отзыв", ignoreCase = true) || raw.contains("review", ignoreCase = true) || raw.contains("reseña", ignoreCase = true) || raw.contains("Bewertung", ignoreCase = true) -> raw
            else -> localized("$raw отзывов", "$raw reviews", "$raw reseñas", "$raw Bewertungen")
        }
    }
    val reservation = when (restaurant.status) {
        "бронь" -> localized("Бронь подтверждена", "Reservation confirmed", "Reserva confirmada", "Reservierung bestätigt")
        "были" -> localized("Посещено", "Visited", "Visitado", "Besucht")
        else -> localized("Запланировать бронь", "Plan reservation", "Planificar reserva", "Reservierung planen")
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(250.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardSurfaceColor())
            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x0F141428), spotColor = Color(0x0F141428))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(132.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(132.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(Color(0xFFE4E1EA))
                    .clickable(enabled = !uploading) {
                        if (photos.isEmpty()) onAddPhoto() else fullScreenPhotoIndex = activePhotoIndex
                    },
            ) {
                photos.getOrNull(activePhotoIndex)?.let { AsyncImage(model = it, contentDescription = restaurant.name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                if (photos.isEmpty()) Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Color(0xFFA7A1B2), modifier = Modifier.align(Alignment.Center).size(27.dp))
                // Restaurant thumbnails stay clean; photo controls are available in the fullscreen viewer.
                if (photos.size > 1 && fullScreenPhotoIndex != null) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(start = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x990F0F19))
                            .clickable {
                                photoIndex = (activePhotoIndex - 1 + photos.size) % photos.size
                            },
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = localized("Предыдущее фото", "Previous photo", "Foto anterior", "Vorheriges Foto"),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 4.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(Color(0x990F0F19))
                            .clickable {
                                photoIndex = (activePhotoIndex + 1) % photos.size
                            },
                    ) {
                        Icon(
                            Icons.Outlined.ArrowBack,
                            contentDescription = localized("Следующее фото", "Next photo", "Foto siguiente", "Nächstes Foto"),
                            tint = Color.White,
                            modifier = Modifier.size(14.dp).graphicsLayer(rotationZ = 180f),
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 4.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0x990F0F19))
                            .padding(horizontal = 5.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = "${activePhotoIndex + 1}/${photos.size}",
                            color = Color.White,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 9.sp,
                            lineHeight = 11.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        )
                    }
                }
            }
            Column(modifier = Modifier.weight(1f).height(132.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        restaurant.name,
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 15.sp,
                        lineHeight = 17.25.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OdysseyExternalLinkIcon(17.dp, OdysseyPurple, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    restaurant.city.takeIf(String::isNotBlank)?.let { localizedCityName(it) }
                        ?: localized("Город не указан", "City not specified", "Ciudad no indicada", "Stadt nicht angegeben"),
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 3.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 9.dp).height(25.dp)) {
                    restaurant.rating?.let {
                        Row(
                            modifier = Modifier.height(25.dp).clip(RoundedCornerShape(8.dp)).background(Color(0xFFFDF5E6)).padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            Text("★", color = Color(0xFFF5A623), fontFamily = Manrope, fontWeight = FontWeight.W400, fontSize = 11.sp, lineHeight = 15.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                            Text(it.toString(), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                        }
                    }
                    if (restaurant.price.isNotBlank()) {
                        Box(modifier = Modifier.height(25.dp).clip(RoundedCornerShape(8.dp)).background(OdysseyTint).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                            Text(restaurant.price, color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                        }
                    }
                    if (displayedNote.isNotBlank() && !restaurant.note.contains("http", ignoreCase = true)) {
                        Box(modifier = Modifier.height(25.dp).clip(RoundedCornerShape(8.dp)).background(secondarySurfaceColor()).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                            Text(displayedNote, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                        }
                    }
                }
            }
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(29.dp)
                .drawBehind { drawLine(OdysseyBorder, Offset(0f, 0f), Offset(size.width, 0f), strokeWidth = 1.dp.toPx()) }
                .padding(top = 11.dp)
                .clickable(enabled = !saving) { onStatusChange(when (restaurant.status) { "хочу" -> "бронь"; "бронь" -> "были"; else -> "хочу" }) },
        ) {
            if (booked || visited) {
                Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    OdysseyCalendarIcon(14.dp, if (booked) Color(0xFF22B07D) else secondaryTextColor())
                    Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else reservation, color = if (booked) Color(0xFF22B07D) else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            } else {
                Text(
                    if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else reviewsLabel.ifBlank { reservation },
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 12.5.sp,
                    lineHeight = 17.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (restaurant.reviews.isNotBlank()) {
                    Text(localized("Забронировать", "Book", "Reservar", "Buchen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                }
            }
        }
        OutlinedButton(
            onClick = onEdit,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = OdysseyLabel),
            border = androidx.compose.foundation.BorderStroke(1.dp, OdysseyBorder),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth().padding(top = 11.dp).height(42.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(11.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                OdysseyEditIcon(15.dp, OdysseyPurple)
                Text(localized("Редактировать", "Edit", "Editar", "Bearbeiten"), color = OdysseyLabel, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
            }
        }
        fullScreenPhotoIndex?.let { initialIndex ->
            if (photos.isNotEmpty()) {
                FullScreenPhotoViewer(
                    photos = photos,
                    initialIndex = initialIndex,
                    accommodationName = restaurant.name,
                    onDismiss = { selectedIndex ->
                        photoIndex = selectedIndex
                        fullScreenPhotoIndex = null
                    },
                )
            }
        }
    }
}

@Composable
private fun RestaurantMapCard(
    restaurants: List<com.odyssey.travelplanner.data.Restaurant>,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val restaurantPoints = remember(restaurants) {
        restaurants
            .mapNotNull { mapCoordinate(it.city) }
            .distinctBy { "${it.longitude()},${it.latitude()}" }
    }
    var mapStyleReady by remember { mutableStateOf(false) }
    val mapView = remember(context) {
        MapView(
            context,
            MapInitOptions(
                context = context,
                textureView = true,
                styleUri = null,
            ),
        ).also {
            it.scalebar.enabled = false
        }
    }
    val annotationManager = remember(mapView) { mapView.annotations.createCircleAnnotationManager() }
    val numberAnnotationManager = remember(mapView) { mapView.annotations.createPointAnnotationManager() }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapStyleReady = false
                    mapView.onStart()
                    mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                        mapStyleReady = true
                    }
                }
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapStyleReady, restaurantPoints) {
        if (!mapStyleReady) return@LaunchedEffect
        annotationManager.deleteAll()
        numberAnnotationManager.deleteAll()
        restaurantPoints.forEachIndexed { index, point ->
            annotationManager.create(
                CircleAnnotationOptions()
                    .withPoint(point)
                    .withCircleRadius(9.0)
                    .withCircleColor("#6C5CE7")
                    .withCircleStrokeColor("#FFFFFF")
                    .withCircleStrokeWidth(3.0),
            )
            numberAnnotationManager.create(
                PointAnnotationOptions()
                    .withPoint(point)
                    .withTextField((index + 1).toString())
                    .withTextColor("#FFFFFF")
                    .withTextSize(12.0)
                    .withTextAnchor(TextAnchor.CENTER),
            )
        }
        val camera = when {
            restaurantPoints.size > 1 -> mapView.mapboxMap.cameraForCoordinates(
                restaurantPoints,
                EdgeInsets(34.0, 34.0, 34.0, 34.0),
                null,
                null,
            )
            restaurantPoints.size == 1 -> CameraOptions.Builder()
                .center(restaurantPoints.first())
                .zoom(9.0)
                .build()
            else -> CameraOptions.Builder()
                .center(Point.fromLngLat(12.4964, 41.9028))
                .zoom(5.0)
                .build()
        }
        mapView.mapboxMap.setCamera(camera)
    }

    val placesLabel = localized(
        "${restaurants.size} мест поблизости",
        "${restaurants.size} places nearby",
        "${restaurants.size} lugares cercanos",
        "${restaurants.size} Orte in der Nähe",
    )
    Column(
        modifier = modifier
            .fillMaxWidth()
            .height(267.dp)
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = Color(0x19141428), spotColor = Color(0x19141428))
            .clip(RoundedCornerShape(22.dp))
            .background(cardSurfaceColor()),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(220.dp)) {
            AndroidView(
                factory = { mapView },
                modifier = Modifier.fillMaxSize(),
            )
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(47.dp)
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    localized("РЕСТОРАНЫ", "RESTAURANTS", "RESTAURANTES", "RESTAURANTS"),
                    color = OdysseyPurple,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.66.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
            }
        }
    }
}

@Composable
private fun EditRestaurantPanel(restaurant: com.odyssey.travelplanner.data.Restaurant, tripId: String, onClose: () -> Unit, onDeleted: () -> Unit, onSaved: () -> Unit) {
    val language = LocalLanguage.current
    var name by remember(restaurant.id) { mutableStateOf(restaurant.name) }
    var city by remember(restaurant.id) { mutableStateOf(restaurant.city) }
    var note by remember(restaurant.id) { mutableStateOf(restaurant.note) }
    var price by remember(restaurant.id) { mutableStateOf(restaurant.price) }
    var link by remember(restaurant.id) { mutableStateOf(restaurant.link) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("Редактировать ресторан", "Edit restaurant", "Editar restaurante", "Restaurant bearbeiten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
        AuthField(localized("Название", "Name", "Nombre", "Name"), localized("Название", "Name", "Nombre", "Name"), name) { name = it }
        AuthField(localized("Город", "City", "Ciudad", "Stadt"), localized("Город", "City", "Ciudad", "Stadt"), city) { city = it }
        AuthField(localized("Заметка", "Note", "Nota", "Notiz"), localized("Кухня или комментарий", "Cuisine or note", "Cocina o comentario", "Küche oder Notiz"), note) { note = it }
        AuthField(localized("Цена", "Price", "Precio", "Preis"), "€€ / €€€", price) { price = it }
        AuthField(localized("Ссылка", "Link", "Enlace", "Link"), "https://…", link) { link = it }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFFFE9E8)).clickable {
                scope.launch {
                    saving = true
                    runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "restaurants", restaurant.id) }
                        .onSuccess { onDeleted() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось удалить ресторан", "Could not delete restaurant", "No se pudo eliminar el restaurante", "Restaurant konnte nicht gelöscht werden") }
                    saving = false
                }
            }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить", "Delete", "Eliminar", "Löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(19.dp))
            }
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = {
                scope.launch {
                    saving = true
                    runCatching {
                        SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateRestaurantDetailsRich(
                            tripId,
                            restaurant.id,
                            com.odyssey.travelplanner.data.RestaurantInput(
                                name = name,
                                city = city,
                                status = restaurant.status,
                                note = note,
                                price = price,
                                link = link,
                            ),
                        )
                    }
                        .onSuccess { onSaved() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось сохранить ресторан", "Could not save restaurant", "No se pudo guardar el restaurante", "Restaurant konnte nicht gespeichert werden") }
                    saving = false
                }
            }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(11.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
        }
    }
}

@Composable
private fun PhotosContent(tripId: String, overview: TripOverview, onPhotoAdded: () -> Unit) {
    val language = LocalLanguage.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var uploading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            uploading = true
            message = null
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось прочитать изображение")
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addCoverPhoto(tripId, bytes)
            }.onSuccess { onPhotoAdded() }.onFailure {
                message = it.message ?: localized(language, "Не удалось загрузить фото", "Could not upload photo", "No se pudo subir la foto", "Foto konnte nicht hochgeladen werden")
            }
            uploading = false
        }
    }
    val photos = buildList {
        overview.coverPhotos.forEach { add(it.imageUrl to it.city) }
        overview.accommodations.forEach { accommodation ->
            accommodation.photos.forEach { add(it to accommodation.city) }
        }
        overview.sights.filter { it.photo.isNotBlank() }.forEach { sight -> add(sight.photo to sight.city) }
        overview.restaurants.forEach { restaurant -> restaurant.photos.forEach { add(it to restaurant.city) } }
    }.filter { it.first.isNotBlank() }.distinctBy { it.first }
    val groupedPhotos = photos.groupBy { (_, city) -> city.ifBlank { localized(language, "Поездка", "Trip", "Viaje", "Reise") } }.toList()

    fun groupMeta(city: String, count: Int): String {
        val date = photoGroupDateRange(city, overview, groupedPhotos.indexOfFirst { it.first == city } + 1)
            ?.let { formatPhotoDateRange(it, language) }
        return listOfNotNull(date, "$count ${localized(language, "фото", "photos", "fotos", "Fotos")}").joinToString(" · ")
    }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
            ) {
                Text(localized("Фото", "Photos", "Fotos", "Fotos"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .clip(RoundedCornerShape(11.dp))
                        .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))))
                        .shadow(5.dp, RoundedCornerShape(11.dp), clip = false, ambientColor = Color(0x426C5CE7), spotColor = Color(0x426C5CE7))
                        .clickable(enabled = !uploading) { picker.launch("image/*") }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                ) {
                    Text(
                        if (uploading) localized("Загружаем…", "Uploading…", "Subiendo…", "Wird hochgeladen…") else localized("↑  Загрузить", "↑  Upload", "↑  Subir", "↑  Hochladen"),
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 12.5.sp,
                    )
                }
            }
        }
        if (message != null) item { Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp) }
        if (groupedPhotos.isEmpty()) {
            item { Text(localized("Фотографии пока не добавлены", "No photos added yet", "Aún no se han añadido fotos", "Noch keine Fotos hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp) }
        } else {
            itemsIndexed(groupedPhotos, key = { _, group -> group.first }) { index, (city, cityPhotos) ->
                Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(26.dp).background(Brush.linearGradient(listOf(Color(0xFFF5A623), Color(0xFFF77F4B))), CircleShape)) {
                            Text(photoGroupDay(city, overview, index + 1).toString(), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                        }
                    Text(localizedCityName(city), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 18.sp, modifier = Modifier.padding(start = 10.dp))
                        Spacer(Modifier.weight(1f))
                        Text(groupMeta(city, cityPhotos.size), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.5.sp)
                    }

                    if (index == 0 && cityPhotos.size >= 3) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                            PhotoTile(cityPhotos[0].first, Modifier.weight(1.7f).height(216.dp))
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.weight(1f)) {
                                PhotoTile(cityPhotos[1].first, Modifier.fillMaxWidth().height(104.dp))
                                PhotoTile(cityPhotos[2].first, Modifier.fillMaxWidth().height(104.dp))
                            }
                        }
                        cityPhotos.drop(3).chunked(3).forEach { row ->
                            PhotoTileRow(row)
                        }
                    } else {
                        cityPhotos.chunked(3).forEach { row ->
                            PhotoTileRow(row)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PhotoTileRow(photos: List<Pair<String, String>>) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
        photos.forEach { (imageUrl, _) ->
            PhotoTile(imageUrl, Modifier.weight(1f).height(112.dp))
        }
        repeat(3 - photos.size) { Spacer(Modifier.weight(1f)) }
    }
}

@Composable
private fun PhotoTile(imageUrl: String, modifier: Modifier = Modifier) {
    Box(modifier = modifier.clip(RoundedCornerShape(16.dp)).background(Color(0xFFD9D6E1))) {
        AsyncImage(model = imageUrl, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
    }
}

@Composable
private fun MembersContent(tripId: String, overview: TripOverview, onRoleUpdated: () -> Unit) {
    val language = LocalLanguage.current
    var savingMemberId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var editing by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("Редактор") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Text(localized("Участники", "Members", "Participantes", "Teilnehmer"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clip(RoundedCornerShape(11.dp)).background(OdysseyTint).clickable { editing = !editing }.padding(horizontal = 13.dp, vertical = 8.dp),
                ) {
                    Icon(Icons.Outlined.Edit, contentDescription = null, tint = OdysseyPurple, modifier = Modifier.size(16.dp))
                    Text(if (editing) localized("Готово", "Done", "Listo", "Fertig") else localized("Изменить", "Edit", "Editar", "Bearbeiten"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
        if (overview.members.isEmpty()) {
            item { Text(localized("Участники пока не добавлены", "No members added yet", "Aún no se han añadido participantes", "Noch keine Mitglieder hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp) }
        } else {
            items(overview.members, key = { it.id }) { member ->
                MemberCard(member, savingMemberId == member.id, editing, onDelete = {
                    scope.launch {
                        savingMemberId = member.id
                        runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "members", member.id) }
                            .onSuccess { onRoleUpdated() }
                        savingMemberId = null
                    }
                }) { role ->
                    scope.launch {
                        savingMemberId = member.id
                        runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateMemberRole(tripId, member.id, role) }
                            .onSuccess { onRoleUpdated() }
                        savingMemberId = null
                    }
                }
            }
        }
        item {
            if (adding) {
                Column(modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x0F141428), spotColor = Color(0x0F141428)).clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(horizontal = 15.dp, vertical = 13.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    InviteMemberField(localized("Имя участника", "Member name", "Nombre", "Name"), name) { name = it }
                    InviteMemberField("e-mail ${localized("нового участника", "of new member", "del nuevo participante", "des neuen Mitglieds")}", email) { email = it }
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(12.dp)).background(Color(0xFFEFEFF4)).padding(3.dp),
                    ) {
                        listOf(
                            localized("Редактор", "Editor", "Editor", "Editor") to "Редактор",
                            localized("Просмотр", "Viewer", "Lector", "Leser") to "Читатель",
                        ).forEach { (label, value) ->
                            val selected = value == role
                            Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(10.dp)).background(if (selected) Color.White else Color.Transparent).border(if (selected) 1.dp else 0.dp, if (selected) OdysseyBorder else Color.Transparent, RoundedCornerShape(10.dp)).clickable { role = value }) {
                                Text(label, color = if (selected) contentTextColor() else OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                            }
                        }
                    }
                    if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(47.dp).clip(RoundedCornerShape(13.dp)).background(Color.White).border(1.dp, OdysseyBorder, RoundedCornerShape(13.dp)).clickable { adding = false; message = null }) {
                            Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                        }
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(47.dp).shadow(6.dp, RoundedCornerShape(13.dp), clip = false, ambientColor = Color(0x476C5CE7), spotColor = Color(0x476C5CE7)).clip(RoundedCornerShape(13.dp)).background(OdysseyPurple).clickable(enabled = !saving) {
                            scope.launch {
                                saving = true
                                runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addMember(tripId, name, email, role) }
                                    .onSuccess { adding = false; name = ""; email = ""; onRoleUpdated() }
                                    .onFailure { message = it.message ?: localized(language, "Не удалось добавить участника", "Could not add member", "No se pudo añadir al participante", "Mitglied konnte nicht hinzugefügt werden") }
                                saving = false
                            }
                        }) {
                            Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Пригласить", "Invite", "Invitar", "Einladen"), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
        item {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.4f))
                    .clickable { adding = true }
                    .drawBehind {
                        drawRoundRect(
                            color = Color(0xFFD3D3DB),
                            cornerRadius = CornerRadius(18.dp.toPx()),
                            style = Stroke(width = 2.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(8.dp.toPx(), 6.dp.toPx()))),
                        )
                    },
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text("＋", color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 18.sp)
                    Text(localized("Пригласить участника", "Invite member", "Invitar participante", "Mitglied einladen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, modifier = Modifier.padding(start = 5.dp))
                }
            }
        }
    }
}

@Composable
private fun InviteMemberField(placeholder: String, value: String, onValueChange: (String) -> Unit) {
    val shape = RoundedCornerShape(13.dp)
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .clip(shape)
            .background(Color.White)
            .border(1.dp, OdysseyBorder, shape),
        contentAlignment = Alignment.CenterStart,
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 14.5.sp,
                platformStyle = OdysseyNoFontPadding,
            ),
            cursorBrush = androidx.compose.ui.graphics.SolidColor(OdysseyPurple),
            modifier = Modifier.fillMaxSize().padding(horizontal = 14.dp),
            decorationBox = { innerTextField ->
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.CenterStart) {
                    if (value.isBlank()) {
                        Text(
                            placeholder,
                            color = OdysseySubtext,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W600,
                            fontSize = 14.5.sp,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                            maxLines = 1,
                            softWrap = false,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                    innerTextField()
                }
            },
        )
    }
}

@Composable
private fun MemberCard(member: com.odyssey.travelplanner.data.TripMember, saving: Boolean, editing: Boolean, onDelete: () -> Unit, onRoleChange: (String) -> Unit) {
    val language = LocalLanguage.current
    val surface = cardSurfaceColor()
    val avatarColor = when (member.tone) {
        "sand", "orange" -> Color(0xFFF29A32)
        "teal", "green" -> Color(0xFF35AEB9)
        else -> OdysseyPurple
    }
    val isOwner = member.role == "Владелец"
    val roleLabel = when (member.role) {
        "Владелец" -> localized(language, "Владелец", "Owner", "Propietario", "Besitzer")
        "Редактор" -> localized(language, "Редактор", "Editor", "Editor", "Editor")
        "Читатель" -> localized(language, "Просмотр", "Viewer", "Lector", "Leser")
        else -> member.role
    }
    val roleBackground = when (member.role) {
        "Владелец" -> Color(0xFFEDEAFF)
        "Редактор" -> Color(0xFFEEFAF3)
        else -> Color(0xFFF3F3F6)
    }
    val roleColor = when (member.role) {
        "Владелец" -> OdysseyPurple
        "Редактор" -> Color(0xFF22B07D)
        else -> OdysseySubtext
    }
    Column(
        modifier = Modifier.fillMaxWidth().shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x0F141428), spotColor = Color(0x0F141428)).clip(RoundedCornerShape(18.dp)).background(surface).padding(horizontal = 15.dp, vertical = 13.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Box(contentAlignment = Alignment.Center, modifier = Modifier.size(44.dp).clip(RoundedCornerShape(13.dp)).background(avatarColor)) {
                Text(member.initials.take(1), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 18.sp)
            }
            Column(modifier = Modifier.weight(1f).padding(start = 13.dp)) {
                Text(member.name, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (member.email.isNotBlank()) Text(member.email, color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 2.dp), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            if (!editing || isOwner) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.clip(RoundedCornerShape(20.dp)).background(roleBackground).padding(horizontal = 10.dp, vertical = 5.dp)) {
                    Text(if (saving) "…" else roleLabel, color = roleColor, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp)
                }
            }
        }
        if (editing && !isOwner) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                listOf(
                    localized("Редактор", "Editor", "Editor", "Editor") to "Редактор",
                    localized("Просмотр", "Viewer", "Lector", "Leser") to "Читатель",
                ).forEach { (label, value) ->
                    val selected = member.role == value
                    Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).height(36.dp).clip(RoundedCornerShape(11.dp)).background(if (selected) Color.White else Color(0xFFEFEFF4)).border(if (selected) 1.dp else 0.dp, if (selected) OdysseyBorder else Color.Transparent, RoundedCornerShape(11.dp)).clickable(enabled = !saving) { onRoleChange(value) }) {
                        Text(label, color = if (selected) contentTextColor() else OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                    }
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(36.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFFFEBEB)).clickable(enabled = !saving) { onDelete() }) {
                    Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить участника", "Remove member", "Eliminar participante", "Mitglied entfernen"), tint = Color(0xFFE35D61), modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun BudgetContent(
    tripId: String,
    overview: TripOverview,
    onExpenseAdded: () -> Unit,
    onCurrencyChanged: (String) -> Unit,
) {
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()
    val expenses = overview.budgetExpenses
    val total = expenses.sumOf { it.amount }
    val categoryStyles = listOf(
        BudgetCategoryStyle("Жильё", "Жильё", Color(0xFF6C5CE7), setOf("жильё", "жилье", "проживание")),
        BudgetCategoryStyle("Транспорт", "Транспорт", Color(0xFFF5A623), setOf("транспорт")),
        BudgetCategoryStyle("Еда и рестораны", "Питание", Color(0xFF22B07D), setOf("еда и рестораны", "питание", "еда")),
        BudgetCategoryStyle("Активности и билеты", "Развлечения", Color(0xFF4AA3F0), setOf("активности и билеты", "развлечения", "активности")),
        BudgetCategoryStyle("Прочее", "Прочее", Color(0xFFEE6C8A), setOf("прочее")),
    )
    val currencyOptions = listOf(
        BudgetCurrencyStyle("RUB", "₽"),
        BudgetCurrencyStyle("EUR", "€"),
        BudgetCurrencyStyle("CZK", "Kč"),
    )
    val storedCurrencyCode = budgetCurrencyCode(overview.budgetCurrency)
    var selectedCurrencyCode by remember(tripId, storedCurrencyCode) { mutableStateOf(storedCurrencyCode) }
    val currencySymbol = currencyOptions.firstOrNull { it.code == selectedCurrencyCode }?.symbol ?: "₽"
    val peopleCount = (overview.budgetGroups.sumOf { it.people }.takeIf { it > 0 } ?: overview.members.size).coerceAtLeast(1)
    val dayCount = budgetTripDayCount(overview.dates)
    val budgetScrollState = rememberScrollState()
    LaunchedEffect(Unit) { budgetScrollState.scrollTo(0) }

    var adding by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<com.odyssey.travelplanner.data.BudgetExpense?>(null) }
    var editMode by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Еда и рестораны") }
    var scopeName by remember { mutableStateOf("общий") }
    var paidBy by remember { mutableStateOf("Общее") }
    var date by remember { mutableStateOf("") }
    var datePickerOpen by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var savingCurrency by remember { mutableStateOf(false) }
    var deletingExpenseId by remember { mutableStateOf<String?>(null) }
    var message by remember { mutableStateOf<String?>(null) }

    fun closeExpenseSheet() {
        adding = false
        editingExpense = null
        datePickerOpen = false
        message = null
    }

    fun openNewExpense() {
        name = ""
        amountInput = ""
        category = "Еда и рестораны"
        scopeName = "общий"
        paidBy = "Общее"
        date = ""
        message = null
        editingExpense = null
        adding = true
    }

    fun openEditExpense(expense: com.odyssey.travelplanner.data.BudgetExpense) {
        name = expense.name
        amountInput = formatBudgetInput(expense.amount, currencySymbol)
        category = categoryStyles.firstOrNull { it.aliases.contains(expense.category.trim().lowercase(java.util.Locale.ROOT)) }?.key ?: "Прочее"
        scopeName = budgetScopeValue(expense.scope)
        paidBy = expense.paidBy.ifBlank { "Общее" }
        date = ""
        message = null
        adding = false
        editingExpense = expense
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(budgetScrollState)
            .padding(start = 18.dp, top = 18.dp, end = 18.dp, bottom = 30.dp),
    ) {
        BudgetSummaryCard(total = total, currencySymbol = currencySymbol)
        Spacer(Modifier.height(14.dp))
        BudgetCurrencySelector(
            selectedCode = selectedCurrencyCode,
            options = currencyOptions,
            saving = savingCurrency,
            onSelect = { selected ->
                if (selected != selectedCurrencyCode && !savingCurrency) {
                    val previousCurrencyCode = selectedCurrencyCode
                    val previousAmountInput = amountInput
                    selectedCurrencyCode = selected
                    previousAmountInput.replace(',', '.').toDoubleOrNull()?.let { enteredAmount ->
                        val baseAmount = enteredAmount / budgetCurrencyRate(previousCurrencyCode)
                        amountInput = formatBudgetInput(baseAmount, selected)
                    }
                    scope.launch {
                        savingCurrency = true
                        runCatching {
                            SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateTripSection(
                                tripId,
                                "budgetCurrency",
                                JsonPrimitive(selected),
                            )
                        }.onSuccess { onCurrencyChanged(selected) }
                            .onFailure {
                                selectedCurrencyCode = previousCurrencyCode
                                amountInput = previousAmountInput
                                message = it.message ?: localized(language, "Не удалось изменить валюту", "Could not change currency", "No se pudo cambiar la moneda", "Währung konnte nicht geändert werden")
                            }
                        savingCurrency = false
                    }
                }
            },
        )
        Spacer(Modifier.height(14.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().height(71.dp)) {
            BudgetMetricCard(
                label = localized("НА ЧЕЛОВЕКА", "PER PERSON", "POR PERSONA", "PRO PERSON"),
                value = formatBudgetAmount(if (peopleCount == 0) 0.0 else total / peopleCount, currencySymbol),
                modifier = Modifier.weight(1f),
            )
            BudgetMetricCard(
                label = localized("В ДЕНЬ", "PER DAY", "POR DÍA", "PRO TAG"),
                value = formatBudgetAmount(if (dayCount == 0) 0.0 else total / dayCount, currencySymbol),
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(22.dp))
        Text(
            text = localized("По категориям", "By category", "Por categorías", "Nach Kategorien"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 16.sp,
            lineHeight = 22.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(22.dp),
        )
        Spacer(Modifier.height(14.dp))
        Column(verticalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.fillMaxWidth()) {
            categoryStyles.forEach { categoryStyle ->
                val categoryTotal = expenses.filter { expense ->
                    categoryStyle.aliases.contains(expense.category.trim().lowercase(java.util.Locale.ROOT))
                }.sumOf { it.amount }
                BudgetCategoryRow(
                    style = categoryStyle,
                    amount = categoryTotal,
                    total = total,
                    currencySymbol = currencySymbol,
                )
            }
        }
        Spacer(Modifier.height(22.dp))
        BudgetExpensesCard(
            expenses = expenses,
            currencySymbol = currencySymbol,
            editMode = editMode,
            deletingExpenseId = deletingExpenseId,
            onToggleEditMode = { editMode = !editMode },
            onAdd = ::openNewExpense,
            onEdit = ::openEditExpense,
            onDelete = { expense ->
                scope.launch {
                    deletingExpenseId = expense.id
                    runCatching {
                        SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "budgetExpenses", expense.id)
                    }.onSuccess { onExpenseAdded() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось удалить трату", "Could not delete expense", "No se pudo eliminar el gasto", "Ausgabe konnte nicht gelöscht werden") }
                    deletingExpenseId = null
                }
            },
        )
    }

    if (adding || editingExpense != null) {
        ModalBottomSheet(
            onDismissRequest = ::closeExpenseSheet,
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            tonalElevation = 0.dp,
            scrimColor = Color(0x730F0F19),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null,
        ) {
            BudgetExpenseSheet(
                title = if (editingExpense == null) localized("Новая трата", "New expense", "Nuevo gasto", "Neue Ausgabe") else localized("Редактировать трату", "Edit expense", "Editar gasto", "Ausgabe bearbeiten"),
                currencySymbol = currencySymbol,
                amount = amountInput,
                payer = paidBy,
                date = date,
                category = category,
                scopeName = scopeName,
                editing = editingExpense != null,
                saving = saving,
                message = message,
                onAmountChange = { amountInput = it },
                onPayerChange = { paidBy = it },
                onDateClick = { datePickerOpen = true },
                onCategoryChange = { category = it },
                onScopeChange = { scopeName = it },
                onClose = ::closeExpenseSheet,
                onSave = {
                    scope.launch {
                        saving = true
                        message = null
                        val value = amountInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                        val baseValue = value / budgetCurrencyRate(currencySymbol)
                        val expenseName = name.trim().ifBlank { category }
                        val input = com.odyssey.travelplanner.data.ExpenseInput(
                            name = expenseName,
                            amount = baseValue,
                            category = category,
                            scope = scopeName,
                            paidBy = paidBy,
                        )
                        runCatching {
                            val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                            editingExpense?.let { expense ->
                                repository.updateBudgetExpenseDetails(tripId, expense.id, input)
                            } ?: repository.addBudgetExpenseDetails(tripId, input)
                        }.onSuccess {
                            closeExpenseSheet()
                            onExpenseAdded()
                        }.onFailure {
                            message = it.message ?: localized(language, "Не удалось сохранить трату", "Could not save expense", "No se pudo guardar el gasto", "Ausgabe konnte nicht gespeichert werden")
                        }
                        saving = false
                    }
                },
            )
        }
    }
    if (datePickerOpen) {
        AccommodationCalendarDialog(
            initialValue = date,
            onDismiss = { datePickerOpen = false },
            onConfirm = {
                date = it
                datePickerOpen = false
            },
        )
    }
}

private data class BudgetCategoryStyle(
    val key: String,
    val label: String,
    val color: Color,
    val aliases: Set<String>,
)

private data class BudgetCurrencyStyle(val code: String, val symbol: String)

private fun budgetCurrencyCode(value: String): String = when (value.trim().uppercase(java.util.Locale.ROOT)) {
    "RUB", "₽" -> "RUB"
    "EUR", "€" -> "EUR"
    "CZK", "KČ", "Kč" -> "CZK"
    else -> "RUB"
}

@Composable
private fun localizedBudgetExpenseName(value: String): String {
    val normalized = value.trim().lowercase(Locale.ROOT)
    return when {
        normalized == "жильё" || normalized == "жилье" || normalized == "проживание" -> localized("Жильё", "Lodging", "Alojamiento", "Unterkunft")
        normalized == "аренда машины" -> localized("Аренда машины", "Car rental", "Alquiler de coche", "Mietwagen")
        normalized == "бензин" -> localized("Бензин", "Fuel", "Combustible", "Kraftstoff")
        normalized.startsWith("дневные траты") -> localized(
            "Дневные траты · 28 сентября – 13 октября",
            "Daily expenses · 28 Sep – 13 Oct",
            "Gastos diarios · 28 sep – 13 oct",
            "Tagesausgaben · 28. Sep. – 13. Okt.",
        )
        else -> value
    }
}

private fun budgetScopeValue(value: String): String = when (value.trim().lowercase(java.util.Locale.ROOT)) {
    "семья", "family" -> "семья"
    "личный", "личное", "personal" -> "личный"
    else -> "общий"
}

private fun budgetTripDayCount(value: String): Int {
    val match = Regex("""(\d+)\s*(?:дн\w*|day\w*|día\w*|tag\w*)""", RegexOption.IGNORE_CASE).find(value)
    return match?.groupValues?.getOrNull(1)?.toIntOrNull()?.coerceAtLeast(1) ?: 1
}

private fun budgetCurrencyRate(value: String): Double = when (budgetCurrencyCode(value)) {
    "EUR" -> 1.0 / 100.0
    "CZK" -> 1.0 / 4.0
    else -> 1.0
}

private fun formatBudgetInput(value: Double, currencySymbol: String): String =
    kotlin.math.round(value * budgetCurrencyRate(currencySymbol)).toLong().toString()

private fun formatBudgetAmount(value: Double, currencySymbol: String): String {
    val symbols = java.text.DecimalFormatSymbols(java.util.Locale("ru", "RU")).apply {
        groupingSeparator = '\u00A0'
        decimalSeparator = ','
    }
    val displayValue = kotlin.math.round(value * budgetCurrencyRate(currencySymbol))
    val pattern = if (displayValue % 1.0 == 0.0) "#,##0" else "#,##0.##"
    val formattedValue = java.text.DecimalFormat(pattern, symbols).format(displayValue)
    return if (budgetCurrencyCode(currencySymbol) == "RUB") "$formattedValue $currencySymbol" else "$currencySymbol $formattedValue"
}

@Composable
private fun BudgetSummaryCard(total: Double, currencySymbol: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(103.dp)
            .clip(RoundedCornerShape(22.dp))
            .background(OdysseyPurple)
            .padding(start = 22.dp, top = 22.dp),
    ) {
        Text(
            text = localized("ОБЩАЯ СУММА", "TOTAL", "TOTAL", "GESAMTSUMME"),
            color = Color.White,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 11.sp,
            lineHeight = 15.sp,
            letterSpacing = 1.1.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(15.dp),
        )
        Text(
            text = formatBudgetAmount(total, currencySymbol),
            color = Color.White,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 38.sp,
            lineHeight = 38.sp,
            letterSpacing = (-0.76).sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun BudgetCurrencySelector(
    selectedCode: String,
    options: List<BudgetCurrencyStyle>,
    saving: Boolean,
    onSelect: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(46.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(OdysseyTrack)
            .padding(5.dp),
    ) {
        options.forEach { option ->
            val selected = option.code == selectedCode
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(11.dp))
                    .background(if (selected) Color.White else Color.Transparent)
                    .clickable(enabled = !saving && !selected) { onSelect(option.code) },
            ) {
                Text(
                    text = option.symbol,
                    color = if (selected) OdysseyText else Color(0xFFA0A0AA),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 13.sp,
                    lineHeight = 17.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
            }
        }
    }
}

@Composable
private fun BudgetMetricCard(label: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxHeight()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White)
            .border(1.dp, OdysseyBorder, RoundedCornerShape(16.dp))
            .padding(start = 12.dp, top = 13.dp, end = 12.dp),
    ) {
        Text(
            text = label,
            color = OdysseySubtext,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            letterSpacing = 0.6.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(14.dp),
            maxLines = 1,
            softWrap = false,
        )
        Text(
            text = value,
            color = OdysseyText,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 17.sp,
            lineHeight = 23.sp,
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.padding(top = 6.dp),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun BudgetCategoryRow(style: BudgetCategoryStyle, amount: Double, total: Double, currencySymbol: String) {
    val fraction = if (total <= 0.0) 0f else (amount / total).toFloat().coerceIn(0f, 1f)
    val percent = if (total <= 0.0) 0 else (amount / total * 100.0).toInt()
    Column(modifier = Modifier.fillMaxWidth().height(35.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(19.dp)) {
            Box(modifier = Modifier.size(11.dp).clip(RoundedCornerShape(4.dp)).background(style.color))
            Text(
                text = localizedBudgetCategory(style.label),
                color = OdysseyText,
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier.padding(start = 9.dp),
                maxLines = 1,
                softWrap = false,
            )
            Text(
                text = " $percent%",
                color = Color(0xFFB6B6BE),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 13.sp,
                lineHeight = 18.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier.padding(start = 5.dp),
                maxLines = 1,
                softWrap = false,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = formatBudgetAmount(amount, currencySymbol),
                color = OdysseyText,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                lineHeight = 19.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                maxLines = 1,
                softWrap = false,
            )
        }
        Spacer(Modifier.height(8.dp))
        Box(modifier = Modifier.fillMaxWidth().height(8.dp).clip(RoundedCornerShape(5.dp)).background(OdysseyTrack)) {
            Box(modifier = Modifier.fillMaxHeight().fillMaxWidth(fraction).clip(RoundedCornerShape(5.dp)).background(style.color))
        }
    }
}

@Composable
private fun BudgetExpensesCard(
    expenses: List<com.odyssey.travelplanner.data.BudgetExpense>,
    currencySymbol: String,
    editMode: Boolean,
    deletingExpenseId: String?,
    onToggleEditMode: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (com.odyssey.travelplanner.data.BudgetExpense) -> Unit,
    onDelete: (com.odyssey.travelplanner.data.BudgetExpense) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
            .border(1.dp, OdysseyBorder, RoundedCornerShape(20.dp))
            .padding(start = 16.dp, top = 6.dp, end = 16.dp, bottom = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(62.dp)
                .drawBehind {
                    drawLine(Color(0xFFF2F2F5), Offset(0f, size.height - 0.5.dp.toPx()), Offset(size.width, size.height - 0.5.dp.toPx()), strokeWidth = 1.dp.toPx())
                },
        ) {
            Text(
                text = localized("Расходы", "Expenses", "Gastos", "Ausgaben"),
                color = OdysseyText,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            )
            Spacer(Modifier.weight(1f))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(34.dp)
                    .clip(RoundedCornerShape(11.dp))
                    .background(OdysseyTint)
                    .clickable(onClick = onToggleEditMode),
            ) {
                OdysseyEditIcon(16.dp, OdysseyPurple)
            }
        }
        expenses.forEachIndexed { index, expense ->
            BudgetExpenseRow(
                expense = expense,
                currencySymbol = currencySymbol,
                editMode = editMode,
                deleting = deletingExpenseId == expense.id,
                showDivider = index < expenses.lastIndex,
                onEdit = { onEdit(expense) },
                onDelete = { onDelete(expense) },
            )
        }
        Spacer(Modifier.height(5.dp))
        BudgetDashedButton(onClick = onAdd)
    }
}

@Composable
private fun BudgetExpenseRow(
    expense: com.odyssey.travelplanner.data.BudgetExpense,
    currencySymbol: String,
    editMode: Boolean,
    deleting: Boolean,
    showDivider: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
) {
    val categoryStyle = when (expense.category.trim().lowercase(java.util.Locale.ROOT)) {
        "проживание" -> BudgetCategoryStyle("Жильё", "Проживание", Color(0xFF6C5CE7), emptySet())
        "жильё", "жилье" -> BudgetCategoryStyle("Жильё", "Проживание", Color(0xFF6C5CE7), emptySet())
        "транспорт" -> BudgetCategoryStyle("Транспорт", "Транспорт", Color(0xFFF5A623), emptySet())
        "еда и рестораны", "еда", "питание" -> BudgetCategoryStyle("Еда и рестораны", "Питание", Color(0xFF22B07D), emptySet())
        "активности и билеты", "активности", "развлечения" -> BudgetCategoryStyle("Активности и билеты", "Развлечения", Color(0xFF4AA3F0), emptySet())
        else -> BudgetCategoryStyle("Прочее", "Прочее", Color(0xFFEE6C8A), emptySet())
    }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(74.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(categoryStyle.color))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = localizedBudgetExpenseName(expense.name),
                    color = OdysseyText,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 14.5.sp,
                    lineHeight = 19.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = localizedBudgetCategory(categoryStyle.label),
                    color = categoryStyle.color,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier
                        .padding(top = 5.dp)
                        .clip(RoundedCornerShape(7.dp))
                        .background(categoryStyle.color.copy(alpha = 0.10f))
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    maxLines = 1,
                    softWrap = false,
                )
            }
            if (editMode) {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    BudgetExpenseActionButton(
                        background = OdysseyTint,
                        onClick = onEdit,
                        enabled = !deleting,
                    ) { OdysseyEditIcon(14.dp, OdysseyPurple) }
                    BudgetExpenseActionButton(
                        background = Color(0xFFFFE9E8),
                        onClick = onDelete,
                        enabled = !deleting,
                    ) { Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить", "Delete", "Eliminar", "Löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(16.dp)) }
                }
            } else {
                Text(
                    text = formatBudgetAmount(expense.amount, currencySymbol),
                    color = OdysseyText,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    softWrap = false,
                )
            }
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(Color(0xFFF6F6F8)),
            )
        }
    }
}

@Composable
private fun BudgetExpenseActionButton(background: Color, enabled: Boolean, onClick: () -> Unit, content: @Composable () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(30.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(background)
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        content()
    }
}

@Composable
private fun BudgetDashedButton(onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .height(47.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF6F4FE))
            .drawBehind {
                val stroke = 1.6.dp.toPx()
                drawRoundRect(
                    color = Color(0xFFCFC7F2),
                    topLeft = Offset(stroke / 2f, stroke / 2f),
                    size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke),
                    cornerRadius = CornerRadius(14.dp.toPx() - stroke / 2f),
                    style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(6.dp.toPx(), 4.dp.toPx()))),
                )
            }
            .clickable(onClick = onClick),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OdysseyPlusIcon(17.dp, OdysseyPurple)
            Text(
                text = localized("Добавить трату", "Add expense", "Añadir gasto", "Ausgabe hinzufügen"),
                color = OdysseyPurple,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                lineHeight = 18.sp,
                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                modifier = Modifier.padding(start = 7.dp),
            )
        }
    }
}

@Composable
private fun BudgetChoiceChip(
    label: String,
    selected: Boolean,
    width: Float,
    scale: Float,
    onClick: () -> Unit,
) {
    val d = { value: Float -> (value * scale).dp }
    val s = { value: Float -> (value * scale).sp }
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(d(width))
            .height(d(40f))
            .clip(RoundedCornerShape(d(20f)))
            .background(if (selected) OdysseyPurple else Color.White)
            .border(d(1f), if (selected) OdysseyPurple else OdysseyBorder, RoundedCornerShape(d(20f)))
            .clickable(onClick = onClick),
    ) {
        Text(
            text = label,
            color = if (selected) Color.White else OdysseySubtext,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13.5f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            maxLines = 1,
            softWrap = false,
        )
    }
}

@Composable
private fun BudgetExpenseSheet(
    title: String,
    currencySymbol: String,
    amount: String,
    payer: String,
    date: String,
    category: String,
    scopeName: String,
    editing: Boolean,
    saving: Boolean,
    message: String?,
    onAmountChange: (String) -> Unit,
    onPayerChange: (String) -> Unit,
    onDateClick: () -> Unit,
    onCategoryChange: (String) -> Unit,
    onScopeChange: (String) -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val labelStyle = androidx.compose.ui.text.TextStyle(
            color = OdysseyLabel,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            platformStyle = OdysseyNoFontPadding,
        )
        Box(modifier = Modifier.fillMaxWidth().height(d(605f))) {
            Box(
                modifier = Modifier
                    .offset(x = d(164f), y = d(12f))
                    .size(d(40f), d(4f))
                    .clip(RoundedCornerShape(d(2f)))
                    .background(Color(0xFFE2E2E8)),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.offset(x = d(16f), y = d(30f)).width(d(336f)).height(d(34f)),
            ) {
                Text(
                    text = title,
                    color = OdysseyText,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(24f),
                    lineHeight = s(33f),
                    letterSpacing = s(-0.24f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    maxLines = 1,
                    softWrap = false,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(d(34f)).clip(CircleShape).background(OdysseySurface2).clickable(onClick = onClose),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = OdysseySubtext, modifier = Modifier.size(d(16f)))
                }
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(12f)),
                modifier = Modifier.offset(x = d(16f), y = d(86f)).width(d(336f)).height(d(77f)),
            ) {
                AccommodationEditTextField(
                    label = localized("Сумма, $currencySymbol", "Amount, $currencySymbol", "Importe, $currencySymbol", "Betrag, $currencySymbol"),
                    value = amount,
                    placeholder = "0",
                    valueWeight = FontWeight.W800,
                    valueColor = OdysseyText,
                    scale = scale,
                    modifier = Modifier.width(d(162f)),
                    onValueChange = onAmountChange,
                )
                AccommodationEditTextField(
                    label = localized("Кто платил", "Paid by", "Quién pagó", "Bezahlt von"),
                    value = payer,
                    placeholder = localized("Общее", "Shared", "Común", "Gemeinsam"),
                    valueWeight = FontWeight.W600,
                    valueColor = OdysseyText,
                    scale = scale,
                    modifier = Modifier.width(d(162f)),
                    onValueChange = onPayerChange,
                )
            }
            AccommodationEditDateField(
                label = localized("Дата", "Date", "Fecha", "Datum"),
                value = date,
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(179f)).width(d(336f)),
                onClick = onDateClick,
            )
            Text(
                text = localized("Категория", "Category", "Categoría", "Kategorie"),
                style = labelStyle,
                modifier = Modifier.offset(x = d(16f), y = d(272f)).width(d(336f)).height(d(18f)),
            )
            Column(modifier = Modifier.offset(x = d(16f), y = d(298f)).width(d(336f))) {
                Row(horizontalArrangement = Arrangement.spacedBy(d(9f))) {
                    BudgetChoiceChip(localized("Жильё", "Lodging", "Alojamiento", "Unterkunft"), category == "Жильё", 79.2f, scale) { onCategoryChange("Жильё") }
                    BudgetChoiceChip(localized("Транспорт", "Transport", "Transporte", "Transport"), category == "Транспорт", 106.8f, scale) { onCategoryChange("Транспорт") }
                }
                Spacer(Modifier.height(d(9f)))
                Row(horizontalArrangement = Arrangement.spacedBy(d(9f))) {
                    BudgetChoiceChip(localized("Еда и рестораны", "Food & restaurants", "Comida y restaurantes", "Essen & Restaurants"), category == "Еда и рестораны", 147.6f, scale) { onCategoryChange("Еда и рестораны") }
                    BudgetChoiceChip(localized("Активности и билеты", "Activities & tickets", "Actividades y entradas", "Aktivitäten & Tickets"), category == "Активности и билеты", 178.7f, scale) { onCategoryChange("Активности и билеты") }
                }
                Spacer(Modifier.height(d(9f)))
                BudgetChoiceChip(localized("Прочее", "Other", "Otros", "Sonstiges"), category == "Прочее", 85.1f, scale) { onCategoryChange("Прочее") }
            }
            Text(
                text = localized("Тип бюджета", "Budget type", "Tipo de presupuesto", "Budgettyp"),
                style = labelStyle,
                modifier = Modifier.offset(x = d(16f), y = d(452f)).width(d(336f)).height(d(18f)),
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier.offset(x = d(16f), y = d(478f)).height(d(40f)),
            ) {
                BudgetChoiceChip(localized("Общий", "Shared", "Común", "Gemeinsam"), scopeName == "общий", 79.8f, scale) { onScopeChange("общий") }
                BudgetChoiceChip(localized("Семья", "Family", "Familia", "Familie"), scopeName == "семья", 77.9f, scale) { onScopeChange("семья") }
                BudgetChoiceChip(localized("Личный", "Personal", "Personal", "Privat"), scopeName == "личный", 87.4f, scale) { onScopeChange("личный") }
            }
            message?.let {
                Text(
                    text = it,
                    color = Color(0xFFE0524B),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = s(11f),
                    lineHeight = s(15f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier.offset(x = d(16f), y = d(512f)).width(d(336f)),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(11f)),
                modifier = Modifier.offset(x = d(16f), y = d(534f)).width(d(336f)).height(d(53f)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(141.578f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Color.White)
                        .border(d(1f), OdysseyBorder, RoundedCornerShape(d(15f)))
                        .clickable(onClick = onClose),
                ) {
                    Text(
                        text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"),
                        color = OdysseyText,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(183.422f))
                        .fillMaxHeight()
                        .shadow(d(8f), RoundedCornerShape(d(15f)), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7))
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))))
                        .clickable(enabled = !saving, onClick = onSave),
                ) {
                    Text(
                        text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else if (editing) localized("Сохранить", "Save", "Guardar", "Speichern") else localized("Добавить", "Add", "Añadir", "Hinzufügen"),
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }
        }
    }
}

@Composable
private fun BudgetContentLegacy(tripId: String, overview: TripOverview, onExpenseAdded: () -> Unit) {
    val surface = cardSurfaceColor()
    val language = LocalLanguage.current
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var amountInput by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("Прочее") }
    var scopeName by remember { mutableStateOf("общий") }
    var paidBy by remember { mutableStateOf("Не указано") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var savingCurrency by remember { mutableStateOf(false) }
    var editingExpense by remember { mutableStateOf<com.odyssey.travelplanner.data.BudgetExpense?>(null) }
    var addingGroup by remember { mutableStateOf(false) }
    var groupName by remember { mutableStateOf("") }
    var groupPeople by remember { mutableStateOf("1") }
    var savingGroup by remember { mutableStateOf(false) }
    var groupMessage by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val expenses = overview.budgetExpenses
    val total = expenses.sumOf { it.amount }
    val currency = overview.budgetCurrency
    fun amount(value: Double) = (if (value % 1.0 == 0.0) value.toInt().toString() else "%.2f".format(value)) + " $currency"
    val categories = listOf("Жильё", "Транспорт", "Еда и рестораны", "Активности и билеты", "Прочее")
    val peopleTotal = overview.budgetGroups.sumOf { it.people }.coerceAtLeast(1)
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(localized("Бюджет поездки", "Trip budget", "Presupuesto del viaje", "Reisebudget"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 28.sp, modifier = Modifier.padding(top = 12.dp))
            Text(localized("${expenses.size} трат · $currency", "${expenses.size} expenses · $currency", "${expenses.size} gastos · $currency", "${expenses.size} Ausgaben · $currency"), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("EUR", "RUB", "USD").forEach { option ->
                    val selected = option == currency
                    Text(
                        text = if (savingCurrency && selected) "…" else option,
                        color = if (selected) Color.White else OdysseySubtext,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 12.sp,
                        modifier = Modifier
                            .background(if (selected) OdysseyPurple else surface, RoundedCornerShape(14.dp))
                            .clickable(enabled = !savingCurrency && !selected) {
                                scope.launch {
                                    savingCurrency = true
                                    runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateTripSection(tripId, "budgetCurrency", JsonPrimitive(option)) }
                                        .onSuccess { onExpenseAdded() }
                                    savingCurrency = false
                                }
                            }
                            .padding(horizontal = 13.dp, vertical = 8.dp),
                    )
                }
            }
        }
        item {
            if (adding) {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(localized("Новая трата", "New expense", "Nuevo gasto", "Neue Ausgabe"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                    AuthField(localized("Название", "Name", "Nombre", "Name"), localized("Например, билеты", "For example, tickets", "Por ejemplo, billetes", "Zum Beispiel Tickets"), name) { name = it }
                    AuthField(localized("Сумма в $currency", "Amount in $currency", "Importe en $currency", "Betrag in $currency"), "0", amountInput) { amountInput = it }
                    AuthField(localized("Кто оплатил", "Paid by", "Pagado por", "Bezahlt von"), localized("Имя", "Name", "Nombre", "Name"), paidBy) { paidBy = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        categories.forEach { option ->
                            val selected = category == option
                            Text(localizedBudgetCategory(option), color = if (selected) Color.White else OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp, modifier = Modifier.background(if (selected) OdysseyPurple else Color(0xFFF0F0F4), RoundedCornerShape(12.dp)).clickable { category = option }.padding(horizontal = 10.dp, vertical = 7.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("общий", "семья", "личный").forEach { option ->
                            val selected = scopeName == option
                            Text(localizedBudgetScope(option), color = if (selected) Color.White else OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp, modifier = Modifier.background(if (selected) OdysseyPurple else secondarySurfaceColor(), RoundedCornerShape(12.dp)).clickable { scopeName = option }.padding(horizontal = 10.dp, vertical = 7.dp))
                        }
                    }
                    if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { adding = false; message = null }, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
                        Button(onClick = {
                            scope.launch {
                                saving = true
                                val value = amountInput.replace(',', '.').toDoubleOrNull() ?: 0.0
                                runCatching {
                                    SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addBudgetExpenseDetails(
                                        tripId,
                                        com.odyssey.travelplanner.data.ExpenseInput(name = name, amount = value, category = category, scope = scopeName, paidBy = paidBy),
                                    )
                                }
                                    .onSuccess { adding = false; name = ""; amountInput = ""; paidBy = "Не указано"; onExpenseAdded() }
                                    .onFailure { message = it.message ?: localized(language, "Не удалось сохранить трату", "Could not save expense", "No se pudo guardar el gasto", "Ausgabe konnte nicht gespeichert werden") }
                                saving = false
                            }
                        }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(11.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
                    }
                }
            } else {
                Text(localized("＋ Добавить трату", "＋ Add expense", "＋ Añadir gasto", "＋ Ausgabe hinzufügen"), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(OdysseyPurple).clickable { adding = true }.padding(horizontal = 15.dp, vertical = 11.dp))
            }
        }
        item {
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(OdysseyPurple).padding(18.dp)) {
                Text(localized("Общий бюджет", "Total budget", "Presupuesto total", "Gesamtbudget"), color = Color(0xDFFFFFFF), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
                Text(amount(total), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 28.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        if (overview.budgetGroups.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                overview.budgetGroups.forEach { group ->
                    Column(modifier = Modifier.width(158.dp).clip(RoundedCornerShape(16.dp)).background(surface).padding(14.dp)) {
                        Text(group.name, color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                        Text(amount(total * group.people / peopleTotal), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, modifier = Modifier.padding(top = 5.dp))
                        Text(localized("Доля из общих трат", "Share of total expenses", "Parte de los gastos totales", "Anteil an den Gesamtausgaben"), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
        item {
            if (addingGroup) {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(localized("Новая группа", "New group", "Nuevo grupo", "Neue Gruppe"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                    AuthField(localized("Название", "Name", "Nombre", "Name"), localized("Например, Друзья", "For example, Friends", "Por ejemplo, Amigos", "Zum Beispiel Freunde"), groupName) { groupName = it }
                    AuthField(localized("Участников", "Members", "Participantes", "Mitglieder"), "1", groupPeople) { groupPeople = it }
                    if (groupMessage != null) Text(groupMessage!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(onClick = { addingGroup = false; groupMessage = null }, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
                        Button(onClick = {
                            scope.launch {
                                savingGroup = true
                                runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addBudgetGroup(tripId, groupName, groupPeople.toIntOrNull() ?: 0) }
                                    .onSuccess { addingGroup = false; groupName = ""; groupPeople = "1"; onExpenseAdded() }
                                    .onFailure { groupMessage = it.message ?: localized(language, "Не удалось сохранить группу", "Could not save group", "No se pudo guardar el grupo", "Gruppe konnte nicht gespeichert werden") }
                                savingGroup = false
                            }
                        }, enabled = !savingGroup, colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(11.dp)) { Text(if (savingGroup) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
                    }
                }
            } else {
                Text(localized("＋ Разделить бюджет", "＋ Split budget", "＋ Dividir presupuesto", "＋ Budget teilen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(surface).clickable { addingGroup = true }.padding(horizontal = 15.dp, vertical = 11.dp))
            }
        }
        item { Text(localized("По категориям", "By category", "Por categoría", "Nach Kategorie"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp)) }
        items(categories) { category ->
            val categoryTotal = expenses.filter { it.category == category }.sumOf { it.amount }
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(surface).padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(localizedBudgetCategory(category), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp)
                    Text(amount(categoryTotal), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(7.dp).background(Color(0xFFEEEEF2), RoundedCornerShape(4.dp))) {
                    Spacer(Modifier.fillMaxHeight().fillMaxWidth(if (total == 0.0) 0f else (categoryTotal / total).toFloat()).background(OdysseyPurple, RoundedCornerShape(4.dp)))
                }
            }
        }
        item { Text(localized("Траты", "Expenses", "Gastos", "Ausgaben"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp)) }
        if (editingExpense != null) item {
            EditExpensePanel(editingExpense!!, tripId, onClose = { editingExpense = null }, onDeleted = {
                editingExpense = null
                onExpenseAdded()
            }, onSaved = {
                editingExpense = null
                onExpenseAdded()
            })
        }
        items(expenses, key = { it.id }) { expense ->
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(surface).clickable { editingExpense = expense }.padding(14.dp)) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(expense.name, color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                    Text(listOf(localizedBudgetScope(expense.scope), expense.paidBy).filter(String::isNotBlank).joinToString(" · "), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
                }
                Text(amount(expense.amount), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun EditExpensePanel(expense: com.odyssey.travelplanner.data.BudgetExpense, tripId: String, onClose: () -> Unit, onDeleted: () -> Unit, onSaved: () -> Unit) {
    val language = LocalLanguage.current
    var name by remember(expense.id) { mutableStateOf(expense.name) }
    var amount by remember(expense.id) { mutableStateOf(expense.amount.toString()) }
    var category by remember(expense.id) { mutableStateOf(expense.category) }
    var scopeName by remember(expense.id) { mutableStateOf(expense.scope) }
    var paidBy by remember(expense.id) { mutableStateOf(expense.paidBy) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("Редактировать трату", "Edit expense", "Editar gasto", "Ausgabe bearbeiten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
        AuthField(localized("Название", "Name", "Nombre", "Name"), localized("Название", "Name", "Nombre", "Name"), name) { name = it }
        AuthField(localized("Сумма", "Amount", "Importe", "Betrag"), "0", amount) { amount = it }
        AuthField(localized("Категория", "Category", "Categoría", "Kategorie"), localized("Категория", "Category", "Categoría", "Kategorie"), category) { category = it }
        AuthField(localized("Кто оплатил", "Paid by", "Pagado por", "Bezahlt von"), localized("Имя", "Name", "Nombre", "Name"), paidBy) { paidBy = it }
        AuthField(localized("Тип бюджета", "Budget type", "Tipo de presupuesto", "Budgettyp"), localized("общий / семья / личный", "shared / family / personal", "compartido / familiar / personal", "gemeinsam / Familie / privat"), scopeName) { scopeName = it }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFFFE9E8)).clickable {
                scope.launch {
                    saving = true
                    runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "budgetExpenses", expense.id) }
                        .onSuccess { onDeleted() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось удалить трату", "Could not delete expense", "No se pudo eliminar el gasto", "Ausgabe konnte nicht gelöscht werden") }
                    saving = false
                }
            }, contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить", "Delete", "Eliminar", "Löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(19.dp))
            }
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = {
                scope.launch {
                    saving = true
                    val value = amount.replace(',', '.').toDoubleOrNull() ?: 0.0
                    runCatching {
                        SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateBudgetExpenseDetails(
                            tripId,
                            expense.id,
                            com.odyssey.travelplanner.data.ExpenseInput(
                                name = name,
                                amount = value,
                                category = category,
                                scope = scopeName,
                                paidBy = paidBy,
                            ),
                        )
                    }
                        .onSuccess { onSaved() }
                        .onFailure { message = it.message ?: localized(language, "Не удалось сохранить трату", "Could not save expense", "No se pudo guardar el gasto", "Ausgabe konnte nicht gespeichert werden") }
                    saving = false
                }
            }, enabled = !saving, colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(11.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AccommodationContent(tripId: String, overview: TripOverview, onStatusUpdated: () -> Unit) {
    val context = LocalContext.current
    val language = LocalLanguage.current
    var savingAccommodationId by remember { mutableStateOf<String?>(null) }
    var adding by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var dates by remember { mutableStateOf("") }
    var checkIn by remember { mutableStateOf("") }
    var checkOut by remember { mutableStateOf("") }
    var deadline by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("хочу") }
    var bookingUrl by remember { mutableStateOf("") }
    var details by remember { mutableStateOf("") }
    var newAccommodationPhotoUri by remember { mutableStateOf<Uri?>(null) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var editingAccommodation by remember { mutableStateOf<com.odyssey.travelplanner.data.Accommodation?>(null) }
    var uploadingAccommodationId by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        val accommodationId = uploadingAccommodationId ?: return@rememberLauncherForActivityResult
        if (uri == null) {
            uploadingAccommodationId = null
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                    ?: error("Не удалось прочитать изображение")
                SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).addAccommodationPhoto(tripId, accommodationId, bytes)
            }.onSuccess { onStatusUpdated() }
            uploadingAccommodationId = null
        }
    }
    val newAccommodationPhotoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        newAccommodationPhotoUri = uri
    }
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(
            start = 18.dp,
            top = 18.dp,
            end = 18.dp,
            bottom = 30.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        if (overview.accommodations.isEmpty()) {
            item { Text(localized("Жильё пока не добавлено", "No lodging added yet", "Aún no se ha añadido alojamiento", "Noch keine Unterkunft hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp) }
        } else {
            items(overview.accommodations, key = { it.id }) { accommodation ->
                AccommodationCard(
                    accommodation,
                    savingAccommodationId == accommodation.id,
                    uploadingAccommodationId == accommodation.id,
                    onEdit = { editingAccommodation = accommodation },
                    onAddPhoto = { uploadingAccommodationId = accommodation.id; photoPicker.launch("image/*") },
                    onMovePhoto = { index, direction ->
                        scope.launch {
                            savingAccommodationId = accommodation.id
                            runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).moveAccommodationPhoto(tripId, accommodation.id, index, direction) }
                                .onSuccess { onStatusUpdated() }
                            savingAccommodationId = null
                        }
                    },
                ) { status ->
                    scope.launch {
                        savingAccommodationId = accommodation.id
                        runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateAccommodationStatus(tripId, accommodation.id, status) }
                            .onSuccess { onStatusUpdated() }
                        savingAccommodationId = null
                    }
                }
            }
        }
        item {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(55.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.4f))
                    .border(androidx.compose.foundation.BorderStroke(2.dp, Color(0xFFD3D3DB)), RoundedCornerShape(18.dp))
                    .clickable { adding = true; message = null },
            ) {
                OdysseyPlusIcon(18.dp, OdysseyPurple)
                Text(localized("Добавить жильё", "Add lodging", "Añadir alojamiento", "Unterkunft hinzufügen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, lineHeight = 18.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.padding(start = 8.dp))
            }
        }
    }
    if (editingAccommodation != null) {
        ModalBottomSheet(
            onDismissRequest = { editingAccommodation = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = Color(0x730F0F19),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null,
        ) {
            AccommodationEditSheet(
                accommodation = editingAccommodation!!,
                tripId = tripId,
                onClose = { editingAccommodation = null },
                onSaved = {
                    editingAccommodation = null
                    onStatusUpdated()
                },
            )
        }
    }
    if (adding) {
        ModalBottomSheet(
            onDismissRequest = { adding = false; message = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
            tonalElevation = 0.dp,
            scrimColor = Color(0x730F0F19),
            shape = RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp),
            dragHandle = null,
        ) {
            AccommodationAddSheet(
                name = name,
                city = city,
                checkIn = checkIn,
                checkOut = checkOut,
                deadline = deadline,
                price = price,
                bookingUrl = bookingUrl,
                details = details,
                status = status,
                photoUri = newAccommodationPhotoUri,
                saving = saving,
                message = message,
                onNameChange = { name = it },
                onCityChange = { city = it },
                onCheckInClick = { datePickerTarget = "checkIn" },
                onCheckOutClick = { datePickerTarget = "checkOut" },
                onDeadlineClick = { datePickerTarget = "deadline" },
                onPriceChange = { price = it },
                onBookingUrlChange = { bookingUrl = it },
                onDetailsChange = { details = it },
                onStatusChange = { status = it },
                onPickPhoto = { newAccommodationPhotoPicker.launch("image/*") },
                onClose = { adding = false; message = null },
                onSave = {
                    scope.launch {
                        saving = true
                        runCatching {
                            val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                            val accommodationId = repository.addAccommodationDetails(
                                com.odyssey.travelplanner.data.AccommodationInput(
                                    name = name,
                                    city = city,
                                    dates = accommodationDateRange(checkIn, checkOut, dates),
                                    price = price,
                                    status = status,
                                    details = details,
                                    bookingUrl = bookingUrl,
                                    deadline = deadline,
                                ),
                                tripId,
                            )
                            newAccommodationPhotoUri?.let { uri ->
                                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
                                    ?: error("Не удалось прочитать изображение")
                                repository.addAccommodationPhoto(tripId, accommodationId, bytes)
                            }
                        }
                            .onSuccess {
                                adding = false
                                message = null
                                name = ""
                                city = ""
                                dates = ""
                                checkIn = ""
                                checkOut = ""
                                deadline = ""
                                price = ""
                                bookingUrl = ""
                                details = ""
                                newAccommodationPhotoUri = null
                                onStatusUpdated()
                            }
                            .onFailure { message = it.message ?: localized(language, "Не удалось сохранить жильё", "Could not save lodging", "No se pudo guardar el alojamiento", "Unterkunft konnte nicht gespeichert werden") }
                        saving = false
                    }
                },
            )
        }
    }
    datePickerTarget?.let { target ->
        AccommodationCalendarDialog(
            initialValue = when (target) {
                "checkIn" -> checkIn
                "checkOut" -> checkOut
                else -> deadline
            },
            onDismiss = { datePickerTarget = null },
            onConfirm = { selected ->
                when (target) {
                    "checkIn" -> checkIn = selected
                    "checkOut" -> checkOut = selected
                    else -> deadline = selected
                }
                datePickerTarget = null
            },
        )
    }
}

@Composable
private fun AccommodationCard(accommodation: com.odyssey.travelplanner.data.Accommodation, saving: Boolean, uploading: Boolean, onEdit: () -> Unit, onAddPhoto: () -> Unit, onMovePhoto: (Int, Int) -> Unit, onStatusChange: (String) -> Unit) {
    val uriHandler = LocalUriHandler.current
    val language = LocalLanguage.current
    val surface = cardSurfaceColor()
    val city = accommodation.city.trim()
    val cityPrefix = cityFlag(city).takeUnless { it == "📍" }.orEmpty()
    val cityLabel = listOf(cityPrefix, localizedCityName(city)).filter(String::isNotBlank).joinToString(" ")
    val dates = formatAccommodationDates(accommodation.dates, language)
    val price = formatAccommodationPrice(accommodation.price)
    val photos = accommodation.photos
    var photoIndex by remember(accommodation.id, photos) { mutableStateOf(0) }
    var fullScreenPhotoIndex by remember(accommodation.id, photos) { mutableStateOf<Int?>(null) }
    val activePhotoIndex = photoIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0))
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0x12141428), spotColor = Color(0x12141428)),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp).background(Color(0xFFCCCCCC))) {
            photos.getOrNull(activePhotoIndex)?.let { imageUrl ->
                AsyncImage(
                    model = imageUrl,
                    contentDescription = accommodation.name,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clickable { fullScreenPhotoIndex = activePhotoIndex },
                )
            }
            if (photos.size > 1) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 10.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x990F0F19))
                        .clickable {
                            photoIndex = (activePhotoIndex - 1 + photos.size) % photos.size
                        },
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = localized("Предыдущее фото", "Previous photo", "Foto anterior", "Vorheriges Foto"),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 10.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0x990F0F19))
                        .clickable {
                            photoIndex = (activePhotoIndex + 1) % photos.size
                        },
                ) {
                    Icon(
                        Icons.Outlined.ArrowBack,
                        contentDescription = localized("Следующее фото", "Next photo", "Foto siguiente", "Nächstes Foto"),
                        tint = Color.White,
                        modifier = Modifier.size(20.dp).graphicsLayer(rotationZ = 180f),
                    )
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(5.dp),
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 10.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0x990F0F19))
                        .padding(horizontal = 10.dp, vertical = 5.dp),
                ) {
                    Text(
                        text = "${activePhotoIndex + 1}/${photos.size}",
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }
        }
        Column(modifier = Modifier.padding(start = 15.dp, top = 13.dp, end = 15.dp, bottom = 15.dp)) {
            Row(modifier = Modifier.fillMaxWidth().height(22.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
                Text(accommodation.name, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, lineHeight = 22.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
                if (price.isNotBlank()) {
                    Text(price, color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 15.sp, lineHeight = 21.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, modifier = Modifier.padding(start = 8.dp))
                }
            }
            Text(cityLabel, color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp))
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 7.dp).height(17.dp)) {
                OdysseyCalendarIcon(14.dp, OdysseyPurple)
                Text(dates, color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 11.5.dp).height(17.dp)) {
                Text("★★★★", color = Color(0xFFF5A623), fontFamily = Manrope, fontWeight = FontWeight.W400, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                accommodation.rating?.let { rating ->
                    Text("· ${rating.toString().removeSuffix(".0")} / 10", color = Color(0xFFB6B6BE), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp, lineHeight = 15.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                }
            }
            if (accommodation.deadline.isNotBlank()) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 10.dp).height(17.dp)) {
                    Text("✓", color = Color(0xFF22B07D), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.width(14.dp))
                    Text(localized("Бесплатная отмена до ${formatAccommodationDeadline(accommodation.deadline, language)}", "Free cancellation until ${formatAccommodationDeadline(accommodation.deadline, language)}", "Cancelación gratuita hasta ${formatAccommodationDeadline(accommodation.deadline, language)}", "Kostenlose Stornierung bis ${formatAccommodationDeadline(accommodation.deadline, language)}"), color = Color(0xFF22B07D), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = if (accommodation.deadline.isNotBlank()) 15.5.dp else 12.dp).height(42.dp)) {
                Box(modifier = (if (accommodation.bookingUrl.isNotBlank()) Modifier.width(150.234.dp) else Modifier.weight(1f)).fillMaxHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, OdysseyBorder, RoundedCornerShape(12.dp)).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OdysseyEditIcon(15.dp, OdysseyPurple)
                        Text(localized("Редактировать", "Edit", "Editar", "Bearbeiten"), color = OdysseyLabel, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                    }
                }
                if (accommodation.bookingUrl.isNotBlank()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, OdysseyBorder, RoundedCornerShape(12.dp)).clickable { uriHandler.openUri(accommodation.bookingUrl) }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OdysseyExternalLinkIcon(15.dp, OdysseyPurple)
                            Text(localized("На Booking", "Booking", "En Booking", "Booking"), color = OdysseyLabel, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                        }
                    }
                }
            }
        }
        fullScreenPhotoIndex?.let { initialIndex ->
            if (photos.isNotEmpty()) {
                FullScreenPhotoViewer(
                    photos = photos,
                    initialIndex = initialIndex,
                    accommodationName = accommodation.name,
                    onDismiss = { selectedIndex ->
                        photoIndex = selectedIndex
                        fullScreenPhotoIndex = null
                    },
                )
            }
        }
    }
}

@Composable
private fun FullScreenSightPhotoViewer(
    sight: com.odyssey.travelplanner.data.Sight,
    onDismiss: () -> Unit,
) {
    val bitmap = rememberSightBitmap(sight)
    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = localizedSightName(sight.name),
                    contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                CircularProgressIndicator(
                    color = Color.White,
                    modifier = Modifier.align(Alignment.Center).size(34.dp),
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F0F19))
                        .clickable { onDismiss() },
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"),
                        tint = Color.White,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun FullScreenPhotoViewer(
    photos: List<String>,
    initialIndex: Int,
    accommodationName: String,
    onDismiss: (Int) -> Unit,
) {
    var photoIndex by remember(photos, initialIndex) {
        mutableStateOf(initialIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)))
    }

    androidx.compose.ui.window.Dialog(
        onDismissRequest = { onDismiss(photoIndex) },
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AsyncImage(
                model = photos[photoIndex],
                contentDescription = accommodationName,
                contentScale = androidx.compose.ui.layout.ContentScale.Fit,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(horizontal = 18.dp, vertical = 10.dp),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F0F19))
                        .clickable { onDismiss(photoIndex) },
                ) {
                    Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = Color.White, modifier = Modifier.size(22.dp))
                }
            }
            if (photos.size > 1) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .padding(start = 14.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F0F19))
                        .clickable { photoIndex = (photoIndex - 1 + photos.size) % photos.size },
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Предыдущее фото", "Previous photo", "Foto anterior", "Vorheriges Foto"), tint = Color.White, modifier = Modifier.size(22.dp))
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 14.dp)
                        .size(42.dp)
                        .clip(CircleShape)
                        .background(Color(0xAA0F0F19))
                        .clickable { photoIndex = (photoIndex + 1) % photos.size },
                ) {
                    Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Следующее фото", "Next photo", "Foto siguiente", "Nächstes Foto"), tint = Color.White, modifier = Modifier.size(22.dp).graphicsLayer(rotationZ = 180f))
                }
                Text(
                    text = "${photoIndex + 1}/${photos.size}",
                    color = Color.White,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 22.dp)
                        .background(Color(0xAA0F0F19), RoundedCornerShape(16.dp))
                        .padding(horizontal = 11.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun AccommodationAddSheet(
    name: String,
    city: String,
    checkIn: String,
    checkOut: String,
    deadline: String,
    price: String,
    bookingUrl: String,
    details: String,
    status: String,
    photoUri: Uri?,
    saving: Boolean,
    message: String?,
    onNameChange: (String) -> Unit,
    onCityChange: (String) -> Unit,
    onCheckInClick: () -> Unit,
    onCheckOutClick: () -> Unit,
    onDeadlineClick: () -> Unit,
    onPriceChange: (String) -> Unit,
    onBookingUrlChange: (String) -> Unit,
    onDetailsChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onPickPhoto: () -> Unit,
    onClose: () -> Unit,
    onSave: () -> Unit,
) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp
        val scrollState = rememberScrollState()
        val labelStyle = androidx.compose.ui.text.TextStyle(color = OdysseyLabel, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(13f), lineHeight = s(18f), platformStyle = OdysseyNoFontPadding)
        val photoScrollState = rememberScrollState()

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(720f))
                .verticalScroll(scrollState),
        ) {
            Box(Modifier.fillMaxWidth().height(d(1102f))) {
                Box(
                    modifier = Modifier
                        .offset(x = d(164f), y = d(12f))
                        .size(d(40f), d(4f))
                        .clip(RoundedCornerShape(d(2f)))
                        .background(Color(0xFFE2E2E8)),
                )
                Text(
                    text = localized("Новое жильё", "New lodging", "Nuevo alojamiento", "Neue Unterkunft"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(24f),
                    lineHeight = s(33f),
                    style = androidx.compose.ui.text.TextStyle(
                        letterSpacing = s(-0.24f),
                        platformStyle = OdysseyNoFontPadding,
                    ),
                    modifier = Modifier.offset(x = d(16f), y = d(30f)).width(d(260f)).height(d(34f)),
                    maxLines = 1,
                    softWrap = false,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .offset(x = d(303f), y = d(30f))
                        .size(d(34f))
                        .clip(CircleShape)
                        .background(OdysseySurface2)
                        .clickable(onClick = onClose),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"),
                        tint = OdysseySubtext,
                        modifier = Modifier.size(d(16f)),
                    )
                }

                Text(text = localized("Фотографии", "Photos", "Fotos", "Fotos"), style = labelStyle, modifier = Modifier.offset(x = d(16f), y = d(82f)).width(d(321f)).height(d(18f)))
                Box(
                    modifier = Modifier
                        .offset(x = d(16f), y = d(108f))
                        .width(d(321f))
                        .height(d(172f))
                        .horizontalScroll(photoScrollState),
                ) {
                    Row(horizontalArrangement = Arrangement.spacedBy(d(10f)), modifier = Modifier.width(d(674f)).height(d(168f))) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(d(240f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(16f)))
                                .background(OdysseySurface2)
                                .drawBehind {
                                    val stroke = d(1f).toPx()
                                    drawRoundRect(color = Color(0xFFCFC7F2), topLeft = Offset(stroke / 2f, stroke / 2f), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke), cornerRadius = CornerRadius(d(16f).toPx() - stroke / 2f), style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx()))))
                                }
                                .clickable(onClick = onPickPhoto),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(Icons.Outlined.Image, contentDescription = null, tint = OdysseyPurple, modifier = Modifier.size(d(26f)))
                                Text(text = localized("Обложка — перетащите фото\nили выберите файл", "Cover — drag a photo\nor choose a file", "Portada — arrastre una foto\no elija un archivo", "Cover — Foto ziehen\noder Datei auswählen"), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(11.5f), lineHeight = s(17f), textAlign = TextAlign.Center, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.padding(top = d(6f)))
                            }
                        }
                        Box(modifier = Modifier.width(d(128f)).height(d(168f)).clip(RoundedCornerShape(d(14f))).background(Color(0xFFE9E7F4))) {
                            if (photoUri != null) AsyncImage(model = photoUri, contentDescription = localized("Обложка жилья", "Accommodation cover", "Portada del alojamiento", "Unterkunft-Titelbild"), contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                            Text(text = localized("Обложка", "Cover", "Portada", "Cover"), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(10f), lineHeight = s(14f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.align(Alignment.TopStart).padding(start = d(8f), top = d(8f)).background(Color(0x8C141419), RoundedCornerShape(d(20f))).padding(horizontal = d(7f), vertical = d(3f)))
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .width(d(128f))
                                .height(d(168f))
                                .clip(RoundedCornerShape(d(14f)))
                                .background(OdysseySurface2)
                                .drawBehind {
                                    val stroke = d(1f).toPx()
                                    drawRoundRect(color = Color(0xFFCFC7F2), topLeft = Offset(stroke / 2f, stroke / 2f), size = androidx.compose.ui.geometry.Size(size.width - stroke, size.height - stroke), cornerRadius = CornerRadius(d(14f).toPx() - stroke / 2f), style = Stroke(width = stroke, pathEffect = PathEffect.dashPathEffect(floatArrayOf(d(6f).toPx(), d(4f).toPx()))))
                                }
                                .clickable(onClick = onPickPhoto),
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                OdysseyPlusIcon(d(18f))
                                Text(text = localized("Добавить", "Add", "Añadir", "Hinzufügen"), color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(11.5f), lineHeight = s(15f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.padding(top = d(5f)))
                            }
                        }
                    }
                }

                Text(text = localized("Статус", "Status", "Estado", "Status"), style = labelStyle, modifier = Modifier.offset(x = d(16f), y = d(298f)).width(d(321f)).height(d(18f)))
                @Composable
                fun AddStatusChip(label: String, value: String, width: Float, modifier: Modifier = Modifier) {
                    Box(contentAlignment = Alignment.Center, modifier = modifier.width(d(width)).height(d(41f)).clip(RoundedCornerShape(d(12f))).background(if (status == value) OdysseyPurple else Color.White).border(d(1f), if (status == value) OdysseyPurple else OdysseyBorder, RoundedCornerShape(d(12f))).clickable { onStatusChange(value) }) {
                        Text(text = label, color = if (status == value) Color.White else OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(12f), lineHeight = s(16f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(d(9f)), modifier = Modifier.offset(x = d(16f), y = d(324f)).height(d(41f))) {
                    AddStatusChip(localized("хочу", "want", "quiero", "möchte"), "хочу", 70.6f)
                    AddStatusChip(localized("бронь", "reserved", "reserva", "Reservierung"), "бронь", 81f)
                    AddStatusChip(localized("оплачено", "paid", "pagado", "bezahlt"), "оплачено", 106.6f)
                }
                AddStatusChip(localized("пожили", "stayed", "alojado", "übernachtet"), "пожили", 92.2f, Modifier.offset(x = d(16f), y = d(374f)))

                AccommodationEditTextField(label = localized("Название", "Name", "Nombre", "Name"), value = name, placeholder = localized("Название жилья", "Accommodation name", "Nombre del alojamiento", "Name der Unterkunft"), valueWeight = FontWeight.W600, valueColor = contentTextColor(), scale = scale, modifier = Modifier.offset(x = d(16f), y = d(431f)).width(d(321f)), onValueChange = onNameChange)
                Row(horizontalArrangement = Arrangement.spacedBy(d(12f)), modifier = Modifier.offset(x = d(16f), y = d(524f)).width(d(321f))) {
                    AccommodationEditTextField(label = localized("Город", "City", "Ciudad", "Stadt"), value = city, placeholder = localized("Город", "City", "Ciudad", "Stadt"), valueWeight = FontWeight.W600, valueColor = contentTextColor(), scale = scale, modifier = Modifier.width(d(154.5f)), onValueChange = onCityChange)
                    AccommodationEditTextField(label = localized("Цена", "Price", "Precio", "Preis"), value = price, placeholder = "€120", valueWeight = FontWeight.W700, valueColor = contentTextColor(), scale = scale, modifier = Modifier.width(d(154.5f)), onValueChange = onPriceChange)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(d(12f)), modifier = Modifier.offset(x = d(16f), y = d(617f)).width(d(321f))) {
                    AccommodationEditDateField(label = localized("Заезд", "Check-in", "Entrada", "Anreise"), value = checkIn, scale = scale, modifier = Modifier.width(d(154.5f)), onClick = onCheckInClick)
                    AccommodationEditDateField(label = localized("Выезд", "Check-out", "Salida", "Abreise"), value = checkOut, scale = scale, modifier = Modifier.width(d(154.5f)), onClick = onCheckOutClick)
                }
                AccommodationEditDateField(label = localized("Бесплатная отмена до", "Free cancellation until", "Cancelación gratuita hasta", "Kostenlose Stornierung bis"), value = deadline, scale = scale, modifier = Modifier.offset(x = d(16f), y = d(710f)).width(d(321f)), onClick = onDeadlineClick)
                AccommodationEditTextField(label = localized("Ссылка на Booking", "Booking link", "Enlace de Booking", "Booking-Link"), value = bookingUrl, placeholder = "https://booking.com/...", valueWeight = FontWeight.W600, valueColor = OdysseyPurple, scale = scale, modifier = Modifier.offset(x = d(16f), y = d(803f)).width(d(321f)), onValueChange = onBookingUrlChange)
                AccommodationEditTextField(label = localized("Адрес / заметка", "Address / note", "Dirección / nota", "Adresse / Notiz"), value = details, placeholder = localized("Дополнительные детали", "Additional details", "Detalles adicionales", "Zusätzliche Details"), valueWeight = FontWeight.W600, valueColor = contentTextColor(), scale = scale, modifier = Modifier.offset(x = d(16f), y = d(896f)).width(d(321f)), onValueChange = onDetailsChange)

                message?.let {
                    Text(text = it, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = s(11f), lineHeight = s(15f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.offset(x = d(16f), y = d(984f)).width(d(336f)))
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(x = d(16f), y = d(1031f)).width(d(135.3f)).height(d(53f)).clip(RoundedCornerShape(d(15f))).background(Color.White).border(d(1f), OdysseyBorder, RoundedCornerShape(d(15f))).clickable(onClick = onClose)) {
                    Text(text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(15f), lineHeight = s(20f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                }
                Box(contentAlignment = Alignment.Center, modifier = Modifier.offset(x = d(162.3f), y = d(1031f)).width(d(174.7f)).height(d(53f)).shadow(d(8f), RoundedCornerShape(d(15f)), clip = false, ambientColor = Color(0x4D6C5CE7), spotColor = Color(0x4D6C5CE7)).clip(RoundedCornerShape(d(15f))).background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0)))).clickable(enabled = !saving, onClick = onSave)) {
                    Text(text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = s(15f), lineHeight = s(20f), style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                }
            }
        }
    }
}

@Composable
private fun AccommodationEditSheet(
    accommodation: com.odyssey.travelplanner.data.Accommodation,
    tripId: String,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val language = LocalLanguage.current
    val initialDates = remember(accommodation.id) { accommodationDateParts(accommodation.dates) }
    var name by remember(accommodation.id) { mutableStateOf(accommodation.name) }
    var checkIn by remember(accommodation.id) { mutableStateOf(initialDates.first) }
    var checkOut by remember(accommodation.id) { mutableStateOf(initialDates.second) }
    var deadline by remember(accommodation.id) { mutableStateOf(accommodation.deadline) }
    var price by remember(accommodation.id) { mutableStateOf(formatAccommodationPrice(accommodation.price)) }
    var bookingUrl by remember(accommodation.id) { mutableStateOf(accommodation.bookingUrl) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var datePickerTarget by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val scale = maxWidth.value / 368f
        fun d(value: Float) = (value * scale).dp
        fun s(value: Float) = (value * scale).sp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(630f)),
        ) {
            Box(
                modifier = Modifier
                    .offset(x = d(164f), y = d(12f))
                    .size(d(40f), d(4f))
                    .clip(RoundedCornerShape(d(2f)))
                    .background(Color(0xFFE2E2E8)),
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .offset(x = d(16f), y = d(32f))
                    .width(d(336f))
                    .height(d(34f)),
            ) {
                Text(
                    text = localized("Редактировать жильё", "Edit lodging", "Editar alojamiento", "Unterkunft bearbeiten"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = s(24f),
                    lineHeight = s(33f),
                    style = androidx.compose.ui.text.TextStyle(
                        letterSpacing = s(-0.24f),
                        platformStyle = OdysseyNoFontPadding,
                    ),
                    maxLines = 1,
                    softWrap = false,
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(d(34f))
                        .clip(CircleShape)
                        .background(OdysseySurface2)
                        .clickable(onClick = onClose),
                ) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"),
                        tint = OdysseySubtext,
                        modifier = Modifier.size(d(16f)),
                    )
                }
            }

            AccommodationEditTextField(
                label = localized("Название жилья", "Accommodation name", "Nombre del alojamiento", "Name der Unterkunft"),
                value = name,
                placeholder = localized("Название жилья", "Accommodation name", "Nombre del alojamiento", "Name der Unterkunft"),
                valueWeight = FontWeight.W700,
                valueColor = contentTextColor(),
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(86f)).width(d(336f)),
                onValueChange = { name = it },
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(12f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(179f))
                    .width(d(336f))
                    .height(d(77f)),
            ) {
                AccommodationEditDateField(
                    label = localized("Заезд", "Check-in", "Entrada", "Anreise"),
                    value = checkIn,
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { datePickerTarget = "checkIn" },
                )
                AccommodationEditDateField(
                    label = localized("Выезд", "Check-out", "Salida", "Abreise"),
                    value = checkOut,
                    scale = scale,
                    modifier = Modifier.weight(1f),
                    onClick = { datePickerTarget = "checkOut" },
                )
            }
            AccommodationEditTextField(
                label = localized("Сумма", "Amount", "Importe", "Betrag"),
                value = price,
                placeholder = "€0",
                valueWeight = FontWeight.W700,
                valueColor = contentTextColor(),
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(365f)).width(d(336f)),
                onValueChange = { price = it },
            )
            AccommodationEditDateField(
                label = localized("Бесплатная отмена до", "Free cancellation until", "Cancelación gratuita hasta", "Kostenlose Stornierung bis"),
                value = deadline,
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(272f)).width(d(336f)),
                onClick = { datePickerTarget = "deadline" },
            )
            AccommodationEditTextField(
                label = localized("Ссылка на Booking", "Booking link", "Enlace de Booking", "Booking-Link"),
                value = bookingUrl,
                placeholder = "https://booking.com/...",
                valueWeight = FontWeight.W600,
                valueColor = OdysseyPurple,
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(458f)).width(d(336f)),
                onValueChange = { bookingUrl = it },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(d(11f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(559f))
                    .width(d(336f))
                    .height(d(53f)),
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(141.578f))
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Color.White)
                        .border(d(1f), OdysseyBorder, RoundedCornerShape(d(15f)))
                        .clickable(onClick = onClose),
                ) {
                    Text(
                        text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"),
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .width(d(183.422f))
                        .fillMaxHeight()
                        .shadow(
                            d(8f),
                            RoundedCornerShape(d(15f)),
                            clip = false,
                            ambientColor = Color(0x4D6C5CE7),
                            spotColor = Color(0x4D6C5CE7),
                        )
                        .clip(RoundedCornerShape(d(15f)))
                        .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))))
                        .clickable(enabled = !saving) {
                            scope.launch {
                                saving = true
                                message = null
                                runCatching {
                                    SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateAccommodationDetailsRich(
                                        tripId = tripId,
                                        accommodationId = accommodation.id,
                                        input = com.odyssey.travelplanner.data.AccommodationInput(
                                            name = name,
                                            city = accommodation.city,
                                            dates = accommodationDateRange(checkIn, checkOut, accommodation.dates),
                                            price = price,
                                            status = accommodation.status,
                                            details = accommodation.details,
                                            bookingUrl = bookingUrl,
                                            deadline = deadline,
                                        ),
                                    )
                                }.onSuccess {
                                    onSaved()
                                }.onFailure {
                                    message = it.message ?: localized(language, "Не удалось сохранить жильё", "Could not save lodging", "No se pudo guardar el alojamiento", "Unterkunft konnte nicht gespeichert werden")
                                }
                                saving = false
                            }
                        },
                ) {
                    Text(
                        text = if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"),
                        color = Color.White,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = s(15f),
                        lineHeight = s(20f),
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                }
            }
            message?.let {
                Text(
                    text = it,
                    color = Color(0xFFE0524B),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = s(11f),
                    lineHeight = s(15f),
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier.offset(x = d(16f), y = d(533f)).width(d(336f)),
                )
            }
        }
    }
    datePickerTarget?.let { target ->
        AccommodationCalendarDialog(
            initialValue = when (target) {
                "checkIn" -> checkIn
                "checkOut" -> checkOut
                else -> deadline
            },
            onDismiss = { datePickerTarget = null },
            onConfirm = { selected ->
                when (target) {
                    "checkIn" -> checkIn = selected
                    "checkOut" -> checkOut = selected
                    else -> deadline = selected
                }
                datePickerTarget = null
            },
        )
    }
}

@Composable
private fun AccommodationEditTextField(
    label: String,
    value: String,
    placeholder: String,
    valueWeight: FontWeight,
    valueColor: Color,
    scale: Float,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Column(modifier = modifier.height(d(77f))) {
        Text(
            text = label,
            color = OdysseyLabel,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(d(18f)),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(d(8f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(Color.White)
                .border(d(1f), OdysseyBorder, RoundedCornerShape(d(14f))),
        ) {
            BasicTextField(
                value = value,
                onValueChange = onValueChange,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = valueColor,
                    fontFamily = Manrope,
                    fontWeight = valueWeight,
                    fontSize = s(15f),
                    lineHeight = s(20f),
                    platformStyle = OdysseyNoFontPadding,
                ),
                cursorBrush = androidx.compose.ui.graphics.SolidColor(OdysseyPurple),
                modifier = Modifier.fillMaxSize(),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = d(15f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        if (value.isBlank()) {
                            Text(
                                text = placeholder,
                                color = Color(0xFFA0A0AA),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W600,
                                fontSize = s(15f),
                                lineHeight = s(20f),
                                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        innerTextField()
                    }
                },
            )
        }
    }
}

@Composable
private fun AccommodationEditDateField(
    label: String,
    value: String,
    scale: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
) {
    fun d(value: Float) = (value * scale).dp
    fun s(value: Float) = (value * scale).sp
    Column(modifier = modifier.height(d(77f))) {
        Text(
            text = label,
            color = OdysseyLabel,
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = s(13f),
            lineHeight = s(18f),
            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
            modifier = Modifier.height(d(18f)),
            maxLines = 1,
            softWrap = false,
        )
        Spacer(Modifier.height(d(8f)))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(Color.White)
                .border(d(1f), OdysseyBorder, RoundedCornerShape(d(14f)))
                .clickable(onClick = onClick),
        ) {
            BasicTextField(
                value = value,
                onValueChange = {},
                enabled = false,
                singleLine = true,
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = s(15f),
                    lineHeight = s(20f),
                    platformStyle = OdysseyNoFontPadding,
                ),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = d(32f)),
                decorationBox = { innerTextField ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = d(12f)),
                        contentAlignment = Alignment.CenterStart,
                    ) {
                        innerTextField()
                    }
                },
            )
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(end = d(12f)),
                contentAlignment = Alignment.CenterEnd,
            ) {
                OdysseyCalendarIcon(d(14f), OdysseyText)
            }
        }
    }
}

@Composable
private fun AccommodationCalendarDialog(
    initialValue: String,
    onDismiss: () -> Unit,
    onConfirm: (String) -> Unit,
) {
    val language = LocalLanguage.current
    val initialCalendar = remember(initialValue) { accommodationDateCalendar(initialValue) }
    var displayedYear by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.YEAR)) }
    var displayedMonth by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.MONTH)) }
    var selectedYear by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.YEAR)) }
    var selectedMonth by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.MONTH)) }
    var selectedDay by remember(initialValue) { mutableStateOf(initialCalendar.get(Calendar.DAY_OF_MONTH)) }
    val monthNames = when (language) {
        "EN" -> listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
        "ES" -> listOf("enero", "febrero", "marzo", "abril", "mayo", "junio", "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre")
        "DE" -> listOf("Januar", "Februar", "März", "April", "Mai", "Juni", "Juli", "August", "September", "Oktober", "November", "Dezember")
        else -> listOf("январь", "февраль", "март", "апрель", "май", "июнь", "июль", "август", "сентябрь", "октябрь", "ноябрь", "декабрь")
    }
    val weekDays = when (language) {
        "EN" -> listOf("M", "T", "W", "T", "F", "S", "S")
        "ES" -> listOf("L", "M", "X", "J", "V", "S", "D")
        "DE" -> listOf("M", "D", "M", "D", "F", "S", "S")
        else -> listOf("П", "В", "С", "Ч", "П", "С", "В")
    }
    val daysInMonth = Calendar.getInstance().apply {
        clear()
        set(displayedYear, displayedMonth + 1, 0)
    }.get(Calendar.DAY_OF_MONTH)
    val firstDay = Calendar.getInstance().apply {
        clear()
        set(displayedYear, displayedMonth, 1)
    }.get(Calendar.DAY_OF_WEEK)
    val leadingEmpty = (firstDay - Calendar.MONDAY + 7) % 7

    androidx.compose.ui.window.Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0x730F0F19)),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                modifier = Modifier
                    .width(336.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(Color.White)
                    .padding(16.dp),
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(34.dp),
                ) {
                    Text(
                        text = localized("Выберите дату", "Choose date", "Elige una fecha", "Datum auswählen"),
                        color = OdysseyText,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 18.sp,
                        lineHeight = 24.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(OdysseySurface2)
                            .clickable(onClick = onDismiss),
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = OdysseySubtext, modifier = Modifier.size(16.dp))
                    }
                }
                Spacer(Modifier.height(14.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth().height(32.dp),
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                            if (displayedMonth == 0) {
                                displayedMonth = 11
                                displayedYear -= 1
                            } else {
                                displayedMonth -= 1
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Предыдущий месяц", "Previous month", "Mes anterior", "Vorheriger Monat"), tint = OdysseyPurple, modifier = Modifier.size(20.dp))
                    }
                    Text(
                        text = "${monthNames[displayedMonth]} $displayedYear",
                        color = OdysseyText,
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 16.sp,
                        lineHeight = 22.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    )
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.size(32.dp).clip(CircleShape).clickable {
                            if (displayedMonth == 11) {
                                displayedMonth = 0
                                displayedYear += 1
                            } else {
                                displayedMonth += 1
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ArrowBack, contentDescription = localized("Следующий месяц", "Next month", "Mes siguiente", "Nächster Monat"), tint = OdysseyPurple, modifier = Modifier.size(20.dp).graphicsLayer { rotationY = 180f })
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth().height(24.dp)) {
                    weekDays.forEach { day ->
                        Text(
                            text = day,
                            color = OdysseySubtext,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 11.sp,
                            lineHeight = 16.sp,
                            textAlign = TextAlign.Center,
                            style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
                Column(verticalArrangement = Arrangement.spacedBy(0.dp), modifier = Modifier.fillMaxWidth()) {
                    (0 until 6).forEach { week ->
                        Row(modifier = Modifier.fillMaxWidth().height(42.dp)) {
                            (0 until 7).forEach { weekday ->
                                val dayIndex = week * 7 + weekday - leadingEmpty + 1
                                val validDay = dayIndex in 1..daysInMonth
                                Box(contentAlignment = Alignment.Center, modifier = Modifier.weight(1f).fillMaxHeight()) {
                                    if (validDay) {
                                        val selected = dayIndex == selectedDay && displayedYear == selectedYear && displayedMonth == selectedMonth
                                        Box(
                                            contentAlignment = Alignment.Center,
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(CircleShape)
                                                .background(if (selected) OdysseyPurple else Color.Transparent)
                                                .clickable {
                                                    selectedYear = displayedYear
                                                    selectedMonth = displayedMonth
                                                    selectedDay = dayIndex
                                                },
                                        ) {
                                            Text(
                                                text = dayIndex.toString(),
                                                color = if (selected) Color.White else OdysseyText,
                                                fontFamily = Manrope,
                                                fontWeight = if (selected) FontWeight.W800 else FontWeight.W600,
                                                fontSize = 14.sp,
                                                lineHeight = 18.sp,
                                                textAlign = TextAlign.Center,
                                                style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(11.dp), modifier = Modifier.fillMaxWidth().height(48.dp)) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Color.White)
                            .border(1.dp, OdysseyBorder, RoundedCornerShape(15.dp))
                            .clickable(onClick = onDismiss),
                    ) {
                        Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1.3f)
                            .fillMaxHeight()
                            .clip(RoundedCornerShape(15.dp))
                            .background(Brush.linearGradient(listOf(OdysseyPurple, Color(0xFF7D6CF0))))
                            .clickable { onConfirm(accommodationDateIso(selectedYear, selectedMonth, selectedDay)) },
                    ) {
                        Text(localized("Готово", "Done", "Listo", "Fertig"), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
                    }
                }
            }
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun TripRouteContent(tripId: String, overview: TripOverview, onRouteAdded: () -> Unit) {
    val language = LocalLanguage.current
    var adding by remember { mutableStateOf(false) }
    var from by remember { mutableStateOf("") }
    var to by remember { mutableStateOf("") }
    var checkIn by remember { mutableStateOf("") }
    var checkOut by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf("") }
    var mapsUrl by remember { mutableStateOf("") }
    var editingLeg by remember { mutableStateOf<com.odyssey.travelplanner.data.RouteLeg?>(null) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val cityCount = overview.overviewMapPoints.ifEmpty { overview.routeLegs.flatMap { listOf(it.from, it.to) }.distinct() }.size
    val tripDays = routeDurationDays(overview.dates)
    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 112.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Text(
                localized("${tripDays ?: ""} ДНЕЙ · $cityCount ГОРОДОВ".trim(), "${tripDays ?: ""} DAYS · $cityCount CITIES".trim(), "${tripDays ?: ""} DÍAS · $cityCount CIUDADES".trim(), "${tripDays ?: ""} TAGE · $cityCount STÄDTE".trim()),
                color = OdysseyPurple,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 11.sp,
            )
        }
        if (overview.routeLegs.isEmpty()) {
            item {
                Text(localized("Переезды пока не добавлены", "No route legs added yet", "Aún no se han añadido trayectos", "Noch keine Etappen hinzugefügt"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp, modifier = Modifier.padding(vertical = 20.dp))
            }
        } else {
            itemsIndexed(overview.routeLegs) { index, leg ->
                val dayIndex = leg.dayNumber.takeIf { it > 0 }?.minus(1) ?: index
                RouteLegCard(leg, dayIndex, overview.dates, onEdit = {
                    editingLeg = leg
                    from = leg.from
                    to = leg.to
                    checkIn = leg.checkIn
                    checkOut = leg.checkOut
                    notes = leg.notes
                    mapsUrl = leg.mapsUrl
                    adding = true
                }) { itemId, completed ->
                    scope.launch {
                        runCatching {
                            SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                                .updateRouteChecklist(tripId, leg.dayId, itemId, completed)
                        }.onSuccess { onRouteAdded() }
                    }
                }
            }
        }
        item {
            if (!adding) {
                Text(
                    localized("＋  Добавить день", "＋  Add day", "＋  Añadir día", "＋  Tag hinzufügen"),
                    color = OdysseyPurple,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().height(55.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFFFAF9FF)).drawBehind {
                        drawRoundRect(
                            color = Color(0xFFD7D0FF),
                            cornerRadius = CornerRadius(16.dp.toPx()),
                            style = Stroke(width = 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(5.dp.toPx(), 4.dp.toPx()))),
                        )
                    }.clickable { adding = true }.padding(top = 17.dp),
                )
            }
        }
    }
    if (adding) {
        ModalBottomSheet(
            onDismissRequest = { adding = false; editingLeg = null; message = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = cardSurfaceColor(),
        ) {
            RouteLegEditorSheet(
                from = from,
                to = to,
                checkIn = checkIn,
                mapsUrl = mapsUrl,
                saving = saving,
                message = message,
                onFromChange = { from = it },
                onToChange = { to = it },
                onCheckInChange = { checkIn = it },
                onMapsUrlChange = { mapsUrl = it },
                onCancel = { adding = false; editingLeg = null; message = null },
                canDelete = editingLeg != null,
                onDelete = {
                    editingLeg?.let { leg ->
                        scope.launch {
                            saving = true
                            runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteTripItem(tripId, "days", leg.dayId) }
                                .onSuccess { adding = false; editingLeg = null; onRouteAdded() }
                                .onFailure { message = it.message ?: localized(language, "Не удалось удалить день", "Could not delete day", "No se pudo eliminar el día", "Tag konnte nicht gelöscht werden") }
                            saving = false
                        }
                    }
                },
                onSave = {
                    scope.launch {
                        saving = true
                        runCatching {
                            val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                            editingLeg?.let { repository.updateRouteLegDetails(tripId, it.dayId, from, to, checkIn, checkOut, notes, mapsUrl) }
                                ?: repository.addRouteLeg(tripId, from, to)
                        }.onSuccess { adding = false; editingLeg = null; from = ""; to = ""; checkIn = ""; checkOut = ""; notes = ""; mapsUrl = ""; onRouteAdded() }
                            .onFailure { message = it.message ?: localized(language, "Не удалось сохранить переезд", "Could not save route leg", "No se pudo guardar el trayecto", "Etappe konnte nicht gespeichert werden") }
                        saving = false
                    }
                },
            )
        }
    }
}

@Composable
private fun RouteLegEditorSheet(
    from: String,
    to: String,
    checkIn: String,
    mapsUrl: String,
    saving: Boolean,
    message: String?,
    onFromChange: (String) -> Unit,
    onToChange: (String) -> Unit,
    onCheckInChange: (String) -> Unit,
    onMapsUrlChange: (String) -> Unit,
    onCancel: () -> Unit,
    canDelete: Boolean,
    onDelete: () -> Unit,
    onSave: () -> Unit,
) {
    val language = LocalLanguage.current
    val displayedFrom = localizedCityName(from)
    val displayedTo = localizedCityName(to)
    var visibleFrom by remember(from, language) { mutableStateOf(displayedFrom) }
    var visibleTo by remember(to, language) { mutableStateOf(displayedTo) }
    var distance by remember { mutableStateOf("") }
    var travelTime by remember { mutableStateOf("") }
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 6.dp)
            .padding(bottom = navigationBarInset),
        verticalArrangement = Arrangement.spacedBy(11.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("День маршрута", "Route day", "Día de ruta", "Reisetag"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 23.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(37.dp).clip(CircleShape).background(Color(0xFFF5F4F8)).clickable { onCancel() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = OdysseySubtext, modifier = Modifier.size(18.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RouteEditorField(localized("Откуда", "From", "Desde", "Von"), visibleFrom, { visibleFrom = it; onFromChange(it) }, Modifier.weight(1f))
            RouteEditorField(localized("Куда", "To", "A", "Nach"), visibleTo, { visibleTo = it; onToChange(it) }, Modifier.weight(1f))
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RouteEditorField(localized("Число", "Date", "Día", "Tag"), "", {}, Modifier.weight(.8f), placeholder = "—")
            RouteEditorField(localized("Месяц", "Month", "Mes", "Monat"), "", {}, Modifier.weight(.9f), placeholder = "—")
            RouteEditorField(localized("День недели", "Weekday", "Día de semana", "Wochentag"), "", {}, Modifier.weight(1.4f), placeholder = "—")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RouteEditorField(localized("Расстояние", "Distance", "Distancia", "Entfernung"), distance, { distance = it }, Modifier.weight(1f), placeholder = "—")
            RouteEditorField(localized("В пути", "Travel time", "En ruta", "Fahrzeit"), travelTime, { travelTime = it }, Modifier.weight(1f), placeholder = "—")
        }
        RouteEditorField(localized("Заселение до", "Check-in by", "Entrada antes de", "Check-in bis"), checkIn, onCheckInChange, Modifier.fillMaxWidth(), placeholder = "—")
        RouteEditorField(localized("Ссылка на карту", "Map link", "Enlace al mapa", "Kartenlink"), mapsUrl, onMapsUrlChange, Modifier.fillMaxWidth(), placeholder = "maps.app.goo.gl/..." )
        if (message != null) Text(message, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 2.dp, bottom = 8.dp)) {
            if (canDelete) {
                Box(modifier = Modifier.size(54.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFFFE9E8)).clickable { onDelete() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Delete, contentDescription = localized("Удалить день", "Delete day", "Eliminar día", "Tag löschen"), tint = Color(0xFFFF6B65), modifier = Modifier.size(22.dp))
                }
            }
            Button(onClick = onCancel, modifier = Modifier.height(54.dp).weight(1f), colors = ButtonDefaults.buttonColors(containerColor = cardSurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(14.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = onSave, enabled = !saving, modifier = Modifier.height(54.dp).weight(1.25f), colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(14.dp)) { Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить", "Save", "Guardar", "Speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
        }
    }
}

@Composable
private fun RouteOrderButton(icon: androidx.compose.ui.graphics.vector.ImageVector, enabled: Boolean, contentDescription: String, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(32.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(if (enabled) Color(0xFFF1EEFF) else Color(0xFFF7F6FA))
            .border(1.dp, if (enabled) Color(0xFFD9D1FF) else Color(0xFFE6E3EC), RoundedCornerShape(10.dp))
            .clickable(enabled = enabled, onClick = onClick),
    ) {
        Icon(icon, contentDescription = contentDescription, tint = if (enabled) OdysseyPurple else Color(0xFFC2BFCA), modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun RouteEditorField(label: String, value: String, onValueChange: (String) -> Unit, modifier: Modifier, placeholder: String = "") {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { if (placeholder.isNotBlank()) Text(placeholder, color = OdysseySubtext, fontFamily = Manrope, fontSize = 14.sp) },
            singleLine = true,
            textStyle = androidx.compose.ui.text.TextStyle(fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp, lineHeight = 20.sp, color = contentTextColor(), platformStyle = OdysseyNoFontPadding),
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = OdysseyBorder, unfocusedBorderColor = OdysseyBorder, focusedContainerColor = cardSurfaceColor(), unfocusedContainerColor = cardSurfaceColor()),
            modifier = Modifier.fillMaxWidth().height(50.dp),
        )
    }
}

@Composable
private fun RouteLegCard(leg: com.odyssey.travelplanner.data.RouteLeg, dayIndex: Int, tripDates: String, onEdit: () -> Unit, onChecklistChange: (String, Boolean) -> Unit) {
    val language = LocalLanguage.current
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val mapsUrl = leg.mapsUrl.ifBlank {
        "https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(leg.from)}&destination=${Uri.encode(leg.to)}"
    }
    val longDestination = leg.to.length > 14
    val dateParts = routeDateParts(leg.date, tripDates, dayIndex, language)
    Column(
        modifier = Modifier.fillMaxWidth().height(141.dp).clip(RoundedCornerShape(19.dp)).background(cardSurfaceColor()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
            Column(modifier = Modifier.width(38.dp).padding(top = 1.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Text(dateParts.first, color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 23.sp)
            Text(dateParts.second, color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 9.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                RouteStop(leg.from, cityFlag(leg.from), isLast = false)
                Spacer(Modifier.height(5.dp))
                RouteStop(leg.to, cityFlag(leg.to), isLast = true, compact = longDestination)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Box(modifier = Modifier.size(37.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF3F1FF)).clickable { clipboard.setText(AnnotatedString(mapsUrl)) }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.ContentCopy, contentDescription = localized("Копировать ссылку", "Copy link", "Copiar enlace", "Link kopieren"), tint = OdysseyPurple, modifier = Modifier.size(18.dp))
                }
                Box(modifier = Modifier.size(37.dp).clip(RoundedCornerShape(11.dp)).background(Color(0xFFF3F1FF)).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Edit, contentDescription = localized("Изменить", "Edit", "Editar", "Bearbeiten"), tint = OdysseyPurple, modifier = Modifier.size(18.dp))
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(secondarySurfaceColor()).padding(horizontal = 12.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Key, contentDescription = null, tint = OdysseyPurple, modifier = Modifier.size(16.dp))
            Text(localized("Заселение", "Check-in", "Entrada", "Check-in"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.padding(start = 10.dp))
            Spacer(Modifier.weight(1f))
            Text((leg.checkIn.ifBlank { leg.checkOut }).ifBlank { "—" }, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
        }
    }
}

@Composable
private fun RouteStop(city: String, flag: String, isLast: Boolean, compact: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.width(18.dp).height(22.dp).drawBehind {
            if (!isLast) drawLine(Color(0xFFD8D3F8), Offset(size.width / 2, 12.dp.toPx()), Offset(size.width / 2, size.height), strokeWidth = 1.5.dp.toPx())
        }) {
            Box(modifier = Modifier.size(if (isLast) 9.dp else 8.dp).clip(CircleShape).background(if (isLast) OdysseyPurple else Color.White).border(1.5.dp, if (isLast) OdysseyPurple else Color(0xFFC6BDF7), CircleShape).align(Alignment.Center))
        }
        Text("$flag ${localizedCityName(city)}", color = if (isLast) contentTextColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = if (isLast) FontWeight.W800 else FontWeight.W700, fontSize = if (compact) 14.sp else if (isLast) 17.sp else 13.sp, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(start = 4.dp))
    }
}

private fun routeDateParts(date: String, tripDates: String, dayIndex: Int, language: String): Pair<String, String> {
    val months = when (normalizeLanguage(language)) {
        "EN" -> listOf("JAN", "FEB", "MAR", "APR", "MAY", "JUN", "JUL", "AUG", "SEP", "OCT", "NOV", "DEC")
        "ES" -> listOf("ENE", "FEB", "MAR", "ABR", "MAY", "JUN", "JUL", "AGO", "SEP", "OCT", "NOV", "DIC")
        "DE" -> listOf("JAN", "FEB", "MÄR", "APR", "MAI", "JUN", "JUL", "AUG", "SEP", "OKT", "NOV", "DEZ")
        else -> listOf("ЯНВ", "ФЕВ", "МАР", "АПР", "МАЙ", "ИЮН", "ИЮЛ", "АВГ", "СЕН", "ОКТ", "НОЯ", "ДЕК")
    }
    val russianMonths = mapOf("января" to 0, "январь" to 0, "февраля" to 1, "февраль" to 1, "марта" to 2, "март" to 2, "апреля" to 3, "апрель" to 3, "мая" to 4, "май" to 4, "июня" to 5, "июнь" to 5, "июля" to 6, "июль" to 6, "августа" to 7, "август" to 7, "сентября" to 8, "сентябрь" to 8, "октября" to 9, "октябрь" to 9, "ноября" to 10, "ноябрь" to 10, "декабря" to 11, "декабрь" to 11)
    fun parse(source: String): Calendar? {
        val iso = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(source)
        val russian = Regex("(\\d{1,2})\\s+(${russianMonths.keys.joinToString("|")})\\s+(\\d{4})", RegexOption.IGNORE_CASE).find(source)
        return when {
            iso != null -> Calendar.getInstance().apply { clear(); set(iso.groupValues[1].toInt(), iso.groupValues[2].toInt() - 1, iso.groupValues[3].toInt()) }
            russian != null -> Calendar.getInstance().apply { clear(); set(russian.groupValues[3].toInt(), russianMonths[russian.groupValues[2].lowercase()] ?: 0, russian.groupValues[1].toInt()) }
            else -> null
        }
    }
    val legDate = parse(date)
    val calendar = legDate ?: parse(tripDates) ?: return "" to ""
    if (legDate == null) calendar.add(Calendar.DAY_OF_YEAR, dayIndex)
    return calendar.get(Calendar.DAY_OF_MONTH).toString() to months[calendar.get(Calendar.MONTH)]
}

private fun routeDurationDays(dates: String): Int? {
    val matches = Regex("(\\d{4})-(\\d{2})-(\\d{2})").findAll(dates).toList()
    if (matches.size < 2) {
        return Regex("·\\s*(\\d+)\\s+дн", RegexOption.IGNORE_CASE).find(dates)?.groupValues?.get(1)?.toIntOrNull()
    }
    fun day(match: MatchResult): Long = Calendar.getInstance().apply { clear(); set(match.groupValues[1].toInt(), match.groupValues[2].toInt() - 1, match.groupValues[3].toInt()) }.timeInMillis / 86_400_000
    return (day(matches[1]) - day(matches[0]) + 1).toInt().takeIf { it > 0 }
}

private fun cityFlag(city: String): String {
    val normalized = city.trim().lowercase()
    return when {
        normalized.contains("праг") || normalized.contains("prague") -> "🇨🇿"
        normalized.contains("мюнхен") || normalized.contains("munich") || normalized.contains("равенсбург") || normalized.contains("ravensburg") -> "🇩🇪"
        listOf("верон", "verona", "милан", "milan", "венеци", "venice", "рим", "rome", "фильине-вальдарно", "figline valdarno", "кьоджа", "chioggia").any(normalized::contains) -> "🇮🇹"
        else -> "📍"
    }
}

private fun formatAccommodationDates(value: String, language: String): String {
    val raw = value.trim()
    if (raw.isBlank()) return localized(language, "Даты не указаны", "Dates not specified", "Fechas no indicadas", "Keine Daten angegeben")
    val parts = raw.split(Regex("\\s+[–-]\\s+"))
    if (parts.size != 2) return localizeLegacyAccommodationDateText(raw, language)
    fun parseIso(source: String): Calendar? {
        val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(source.trim()) ?: return null
        return Calendar.getInstance().apply {
            clear()
            set(match.groupValues[1].toInt(), match.groupValues[2].toInt() - 1, match.groupValues[3].toInt())
        }
    }
    val start = parseIso(parts[0]) ?: return localizeLegacyAccommodationDateText(raw, language)
    val end = parseIso(parts[1]) ?: return localizeLegacyAccommodationDateText(raw, language)
    val months = when (normalizeLanguage(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    }
    val startDay = start.get(Calendar.DAY_OF_MONTH)
    val endDay = end.get(Calendar.DAY_OF_MONTH)
    val startMonth = months[start.get(Calendar.MONTH)]
    val endMonth = months[end.get(Calendar.MONTH)]
    val range = if (start.get(Calendar.MONTH) == end.get(Calendar.MONTH)) {
        "$startDay–$endDay $endMonth"
    } else {
        "$startDay $startMonth – $endDay $endMonth"
    }
    return range
}

private fun localizeLegacyAccommodationDateText(value: String, language: String): String {
    val monthNames = when (normalizeLanguage(language)) {
        "EN" -> mapOf("янв" to "Jan", "фев" to "Feb", "мар" to "Mar", "апр" to "Apr", "май" to "May", "июн" to "Jun", "июл" to "Jul", "авг" to "Aug", "сен" to "Sep", "окт" to "Oct", "ноя" to "Nov", "дек" to "Dec")
        "ES" -> mapOf("янв" to "ene", "фев" to "feb", "мар" to "mar", "апр" to "abr", "май" to "may", "июн" to "jun", "июл" to "jul", "авг" to "ago", "сен" to "sep", "окт" to "oct", "ноя" to "nov", "дек" to "dic")
        "DE" -> mapOf("янв" to "Jan", "фев" to "Feb", "мар" to "Mär", "апр" to "Apr", "май" to "Mai", "июн" to "Jun", "июл" to "Jul", "авг" to "Aug", "сен" to "Sep", "окт" to "Okt", "ноя" to "Nov", "дек" to "Dez")
        else -> mapOf("янв" to "янв", "фев" to "фев", "мар" to "мар", "апр" to "апр", "май" to "май", "июн" to "июн", "июл" to "июл", "авг" to "авг", "сен" to "сен", "окт" to "окт", "ноя" to "ноя", "дек" to "дек")
    }
    var result = Regex("(?i)(?<![\\p{L}])(янв|фев|мар|апр|май|июн|июл|авг|сен|окт|ноя|дек)(?![\\p{L}])").replace(value) { match ->
        monthNames[match.value.lowercase(Locale.ROOT)] ?: match.value
    }
    val nightPattern = Regex("(?i)(\\d+)\\s+(ночь|ночи|ночей)")
    result = nightPattern.replace(result) { match ->
        val count = match.groupValues[1].toIntOrNull() ?: 0
        val word = when (normalizeLanguage(language)) {
            "EN" -> if (count == 1) "night" else "nights"
            "ES" -> if (count == 1) "noche" else "noches"
            "DE" -> if (count == 1) "Nacht" else "Nächte"
            else -> when {
                count % 10 == 1 && count % 100 != 11 -> "ночь"
                count % 10 in 2..4 && count % 100 !in 12..14 -> "ночи"
                else -> "ночей"
            }
        }
        "${match.groupValues[1]} $word"
    }
    return result
}

private fun formatAccommodationDeadline(value: String, language: String): String {
    val raw = value.trim()
    val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(raw) ?: return raw
    val months = when (normalizeLanguage(language)) {
        "EN" -> listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
        "ES" -> listOf("ene", "feb", "mar", "abr", "may", "jun", "jul", "ago", "sep", "oct", "nov", "dic")
        "DE" -> listOf("Jan", "Feb", "Mär", "Apr", "Mai", "Jun", "Jul", "Aug", "Sep", "Okt", "Nov", "Dez")
        else -> listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
    }
    return "${match.groupValues[3].toInt()} ${months[match.groupValues[2].toInt() - 1]}"
}

private fun formatAccommodationPrice(value: String): String {
    val raw = value.trim()
    if (raw.isBlank()) return ""
    return if (raw.firstOrNull() in listOf('€', '$', '£', '₽') || raw.lastOrNull() in listOf('€', '$', '£', '₽')) raw else "€$raw"
}

private fun accommodationDateCalendar(value: String): Calendar {
    val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").find(value)
    return Calendar.getInstance().apply {
        if (match != null) {
            clear()
            set(match.groupValues[1].toInt(), match.groupValues[2].toInt() - 1, match.groupValues[3].toInt())
        }
    }
}

private fun accommodationDateIso(year: Int, month: Int, day: Int): String =
    "${year.toString().padStart(4, '0')}-${(month + 1).toString().padStart(2, '0')}-${day.toString().padStart(2, '0')}"

private fun accommodationDateParts(value: String): Pair<String, String> {
    val matches = Regex("\\d{4}-\\d{2}-\\d{2}").findAll(value).map { it.value }.toList()
    if (matches.size >= 2) return matches[0] to matches[1]
    val parts = value.trim().split(Regex("\\s+[–-]\\s+"))
    return (matches.firstOrNull() ?: parts.getOrNull(0).orEmpty()) to parts.getOrNull(1).orEmpty()
}

private fun accommodationDateRange(start: String, end: String, original: String): String {
    val checkIn = start.trim()
    val checkOut = end.trim()
    return when {
        checkIn.isNotBlank() && checkOut.isNotBlank() -> "$checkIn – $checkOut"
        checkIn.isNotBlank() -> checkIn
        checkOut.isNotBlank() -> checkOut
        else -> original.trim()
    }
}

@Composable
private fun RouteDetail(text: String, completed: Boolean, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { onClick() }) {
        Text(if (completed) "●" else "○", color = if (completed) Color(0xFF269B6A) else OdysseyPurple, fontSize = 16.sp)
        Text(text, color = if (completed) OdysseySubtext else Color(0xFF4B4B54), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun OverviewContent(overview: TripOverview, weather: Map<String, WeatherSnapshot>) {
    var photoIndex by remember { mutableStateOf(0) }
    var tripDatesWeather by remember { mutableStateOf(false) }
    val photos = overview.coverPhotos
    val activePhoto = photos.getOrNull(photoIndex.coerceIn(0, (photos.size - 1).coerceAtLeast(0)))
    val routeCities = overview.routeLegs
        .flatMap { listOf(it.from, it.to) }
        .filter(String::isNotBlank)
        .ifEmpty { overview.overviewMapPoints }
    val weatherCities = (overview.overviewMapPoints.ifEmpty { routeCities })
        .distinctBy { cityFilterKey(it) }

    LazyColumn(
        contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 30.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(270.dp)
                    .clip(RoundedCornerShape(22.dp))
                    .background(Color(0xFFCAC7D9)),
            ) {
                if (activePhoto != null) {
                    AsyncImage(
                        model = activePhoto.imageUrl,
                        contentDescription = activePhoto.city,
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.fillMaxSize(),
                    )
                }
                Text(
                    text = localizedCityName(activePhoto?.city.orEmpty()),
                    color = Color.White,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 24.sp,
                    modifier = Modifier.align(Alignment.BottomStart).padding(16.dp),
                )
                if (photos.size > 1) {
                    Text(
                        text = "‹",
                        color = Color.White,
                        fontSize = 31.sp,
                        modifier = Modifier
                            .align(Alignment.CenterStart)
                            .padding(12.dp)
                            .clickable { photoIndex = (photoIndex - 1 + photos.size) % photos.size },
                    )
                    Text(
                        text = "›",
                        color = Color.White,
                        fontSize = 31.sp,
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(12.dp)
                            .clickable { photoIndex = (photoIndex + 1) % photos.size },
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier.align(Alignment.TopEnd).padding(14.dp),
                    ) {
                        photos.forEachIndexed { index, _ ->
                            Spacer(
                                Modifier
                                    .height(6.dp)
                                    .width(if (index == photoIndex) 18.dp else 6.dp)
                                    .background(if (index == photoIndex) Color.White else Color(0x99FFFFFF), RoundedCornerShape(3.dp)),
                            )
                        }
                    }
                }
            }
        }
        item { OverviewMapCard(overview.routeLegs, routeCities) }
        item {
            Text(
                text = localized("Погода по маршруту", "Weather along the route", "Tiempo en la ruta", "Wetter entlang der Route"),
                color = contentTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 20.sp,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        item {
            Row(
                modifier = Modifier.background(if (LocalDarkTheme.current) Color(0xFF2B2D38) else Color(0xFFEEEEF2), RoundedCornerShape(12.dp)).padding(4.dp),
            ) {
                Text(
                    text = localized("Сейчас", "Now", "Ahora", "Jetzt"),
                    color = if (!tripDatesWeather) contentTextColor() else secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(if (!tripDatesWeather) cardSurfaceColor() else Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable { tripDatesWeather = false }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
                Text(
                    text = localized("На даты поездки", "Trip dates", "Fechas del viaje", "Reisedaten"),
                    color = if (tripDatesWeather) contentTextColor() else secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 13.sp,
                    modifier = Modifier
                        .background(if (tripDatesWeather) cardSurfaceColor() else Color.Transparent, RoundedCornerShape(9.dp))
                        .clickable { tripDatesWeather = true }
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                )
            }
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState()),
            ) {
                weatherCities.forEach { city -> WeatherPlaceholder(city, photos.firstOrNull { it.city.equals(city, true) }, weather[city], tripDatesWeather) }
            }
        }
    }
}

@Composable
private fun OverviewMapCard(
    legs: List<com.odyssey.travelplanner.data.RouteLeg>,
    cities: List<String>,
    mapHeight: Dp = 260.dp,
    footer: @Composable (() -> Unit)? = null,
    routePoints: List<Point> = emptyList(),
    selectedPointIndex: Int? = null,
    cardShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    cardShadow: Dp? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val cityCount = cities.distinctBy { cityFilterKey(it) }.size
    val coordinates = routePoints.ifEmpty { cities.mapNotNull(::mapCoordinate) }
    var mapStyleReady by remember { mutableStateOf(false) }
    val mapView = remember(context) {
        MapView(
            context,
            MapInitOptions(
                context = context,
                textureView = true,
                styleUri = null,
            ),
        ).also {
            it.scalebar.enabled = false
        }
    }
    val routeAnnotationManager = remember(mapView) { mapView.annotations.createPolylineAnnotationManager() }
    val sightAnnotationManager = remember(mapView) { mapView.annotations.createCircleAnnotationManager() }
    val sightNumberAnnotationManager = remember(mapView) { mapView.annotations.createPointAnnotationManager() }

    DisposableEffect(lifecycleOwner, mapView) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapStyleReady = false
                    mapView.onStart()
                    mapView.mapboxMap.loadStyle(Style.MAPBOX_STREETS) {
                        mapStyleReady = true
                    }
                }
                Lifecycle.Event.ON_STOP -> mapView.onStop()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            mapView.onStop()
            mapView.onDestroy()
        }
    }

    LaunchedEffect(mapStyleReady, coordinates, selectedPointIndex) {
        if (mapStyleReady && coordinates.isNotEmpty()) {
            routeAnnotationManager.deleteAll()
            sightAnnotationManager.deleteAll()
            sightNumberAnnotationManager.deleteAll()
            if (coordinates.size > 1) {
                routeAnnotationManager.create(
                    PolylineAnnotationOptions()
                        .withPoints(coordinates)
                        .withLineColor("#6C5CE7")
                        .withLineWidth(5.0),
                )
            }
            if (routePoints.isNotEmpty()) {
                routePoints.forEachIndexed { index, point ->
                    val selected = index == selectedPointIndex
                    sightAnnotationManager.create(
                        CircleAnnotationOptions()
                            .withPoint(point)
                            .withCircleRadius(if (selected) 14.0 else 9.0)
                            .withCircleColor(if (selected) "#FF6B65" else "#6C5CE7")
                            .withCircleStrokeColor("#FFFFFF")
                            .withCircleStrokeWidth(if (selected) 4.0 else 3.0),
                    )
                    sightNumberAnnotationManager.create(
                        PointAnnotationOptions()
                            .withPoint(point)
                            .withTextField((index + 1).toString())
                            .withTextColor("#FFFFFF")
                            .withTextSize(if (selected) 13.5 else 12.0)
                            .withTextAnchor(TextAnchor.CENTER),
                    )
                }
            }
        }
    }

    LaunchedEffect(mapStyleReady, coordinates) {
        if (mapStyleReady && coordinates.isNotEmpty()) {
            val camera = if (routePoints.isNotEmpty()) {
                mapView.mapboxMap.cameraForCoordinates(
                    coordinates,
                    EdgeInsets(34.0, 34.0, 34.0, 34.0),
                    null,
                    null,
                )
            } else {
                val center = coordinates.fold(Pair(0.0, 0.0)) { sum, point ->
                    Pair(sum.first + point.longitude(), sum.second + point.latitude())
                }
                CameraOptions.Builder()
                    .center(Point.fromLngLat(center.first / coordinates.size, center.second / coordinates.size))
                    .zoom(if (coordinates.size == 1) 9.0 else 3.7)
                    .build()
            }
            mapView.mapboxMap.setCamera(camera)
        }
    }

    LaunchedEffect(mapStyleReady, selectedPointIndex, routePoints) {
        if (mapStyleReady) {
            routePoints.getOrNull(selectedPointIndex ?: -1)?.let { point ->
                mapView.mapboxMap.setCamera(
                    CameraOptions.Builder()
                        .center(point)
                        .zoom(14.0)
                        .build(),
                )
            }
        }
    }

    val cardModifier = if (cardShadow != null) {
        Modifier.fillMaxWidth().shadow(cardShadow, cardShape, clip = false, ambientColor = Color(0x19141428), spotColor = Color(0x19141428))
    } else {
        Modifier.fillMaxWidth()
    }
    Column(
        modifier = cardModifier.clip(cardShape).background(cardSurfaceColor()),
    ) {
        if (coordinates.isEmpty()) {
            EmptyStateCard(
                icon = Icons.Outlined.LocationOn,
                title = localized("Карта появится после добавления городов", "The map appears after adding cities", "El mapa aparecerá al añadir ciudades", "Die Karte erscheint nach dem Hinzufügen von Städten"),
                body = localized("Добавьте города или координаты мест", "Add cities or place coordinates", "Añada ciudades o coordenadas", "Fügen Sie Städte oder Koordinaten hinzu"),
                action = null,
                onAction = null,
            )
        } else {
            AndroidView(factory = { mapView }, modifier = Modifier.fillMaxWidth().height(mapHeight))
        }
        if (footer != null) footer() else Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
        ) {
            Text(localized("Общий маршрут", "Full route", "Ruta completa", "Gesamtroute"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp)
            Text(text = localized("${legs.size} переездов · $cityCount городов", "${legs.size} legs · $cityCount cities", "${legs.size} trayectos · $cityCount ciudades", "${legs.size} Etappen · $cityCount Städte"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
        }
    }
}

private fun cityFilterKey(city: String): String {
    val point = mapCoordinate(city)
    return if (point != null) {
        "point:${String.format(Locale.US, "%.4f:%.4f", point.longitude(), point.latitude())}"
    } else {
        city.substringBefore(",").trim().lowercase(Locale.ROOT)
    }
}

private fun mapCoordinate(city: String): Point? = when (city.substringBefore(",").trim().lowercase(Locale.ROOT)) {
    "prague", "прага" -> Point.fromLngLat(14.4378, 50.0755)
    "salzburg" -> Point.fromLngLat(13.0550, 47.8095)
    "verona", "верона" -> Point.fromLngLat(10.9916, 45.4384)
    "rome", "рим" -> Point.fromLngLat(12.4964, 41.9028)
    "pisa", "пиза" -> Point.fromLngLat(10.4017, 43.7228)
    "figline valdarno", "фильине-вальдарно" -> Point.fromLngLat(11.4690, 43.6190)
    "san marino", "сан-марино" -> Point.fromLngLat(12.4578, 43.9424)
    "chioggia", "кьоджа" -> Point.fromLngLat(12.2786, 45.2181)
    "milan", "милан" -> Point.fromLngLat(9.1900, 45.4642)
    "valdidentro" -> Point.fromLngLat(10.2940, 46.4890)
    "ravensburg", "равенсбург" -> Point.fromLngLat(9.6110, 47.7810)
    "munich", "мюнхен" -> Point.fromLngLat(11.5820, 48.1351)
    "vienna", "вена" -> Point.fromLngLat(16.3738, 48.2082)
    "innsbruck", "инсбрук" -> Point.fromLngLat(11.4041, 47.2692)
    "florence", "флоренция" -> Point.fromLngLat(11.2558, 43.7696)
    "venice", "венеция" -> Point.fromLngLat(12.3155, 45.4408)
    "tallinn", "таллин" -> Point.fromLngLat(24.7536, 59.4370)
    "riga", "рига" -> Point.fromLngLat(24.1052, 56.9496)
    "vilnius", "вильнюс" -> Point.fromLngLat(25.2797, 54.6872)
    "castel gandolfo", "кастель-гандольфо" -> Point.fromLngLat(12.6500, 41.7475)
    "lake como", "озеро комо" -> Point.fromLngLat(9.2600, 45.8080)
    "bormio", "бормио" -> Point.fromLngLat(10.3740, 46.4670)
    "val viola valley", "долина валь-виола" -> Point.fromLngLat(10.1900, 46.4200)
    else -> null
}


@Composable
private fun WeatherPlaceholder(
    city: String,
    photo: com.odyssey.travelplanner.data.CoverPhoto?,
    weather: WeatherSnapshot?,
    tripDatesWeather: Boolean,
) {
    val temperature = weather?.temperature?.removeSuffix("°C")?.toIntOrNull()
    val displayedTemperature = if (tripDatesWeather) weather?.tripTemperature else weather?.temperature
    val displayedCondition = if (tripDatesWeather) {
        weather?.tripCondition?.let { localizedWeatherCondition(it) }
            ?: localized("Прогноз пока недоступен", "Forecast unavailable", "Pronóstico no disponible", "Vorhersage nicht verfügbar")
    } else {
        weather?.condition?.let { localizedWeatherCondition(it) }
    }
    Box(
        modifier = Modifier.width(104.dp).height(140.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF6C5CE7)),
    ) {
        if (photo != null) {
            AsyncImage(
                model = photo.imageUrl,
                contentDescription = city,
                contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
        Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color(0xA6000000)))))
        Text(
                    text = localizedCityName(city),
            color = Color.White,
            fontFamily = Manrope,
            fontWeight = FontWeight.W700,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            val temperatureText = if (tripDatesWeather && weather?.tripIsEstimate == true) {
                displayedTemperature?.let { "≈ $it" } ?: "…"
            } else {
                displayedTemperature ?: "…"
            }
            Text(temperatureText, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 26.sp)
            Text(displayedCondition.orEmpty(), color = Color(0xDDFFFFFF), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TripListCard(trip: TripCard, onTripClick: (String) -> Unit, onEdit: () -> Unit) {
    val darkTheme = LocalDarkTheme.current
    val language = LocalLanguage.current
    val isDraft = trip.status.contains("чернов", ignoreCase = true)
    val statusColor = if (isDraft) Color(0xFFE0A34B) else Color(0xFF22B07D)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = Color(0x20141428), spotColor = Color(0x20141428))
            .clip(RoundedCornerShape(22.dp))
            .background(if (darkTheme) Color(0xFF20222E) else Color.White)
            .clickable { onTripClick(trip.id) },
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(205.dp).background(Color(0xFFE6E4DD))) {
            if (trip.coverImage != null) {
                AsyncImage(
                    model = trip.coverImage,
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize(),
                )
            } else {
                Box(Modifier.fillMaxSize().background(Brush.linearGradient(listOf(Color(0xFFE8E5F4), Color(0xFFD7D2E9))))) {
                    Icon(Icons.Outlined.Explore, contentDescription = null, tint = Color(0xFF9B91C3), modifier = Modifier.align(Alignment.Center).size(52.dp))
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .padding(12.dp)
                    .background(Color(0xEEFFFFFF), RoundedCornerShape(20.dp))
                    .padding(horizontal = 11.dp, vertical = 5.dp),
            ) {
                Spacer(Modifier.size(7.dp).background(statusColor, RoundedCornerShape(4.dp)))
                Text(
                    text = localizedTripStatus(trip.status),
                    color = Color(0xFF33333A),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 11.sp,
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(36.dp).background(Color(0xF8FFFFFF), RoundedCornerShape(12.dp)).clickable { onEdit() },
            ) {
                Icon(Icons.Filled.MoreVert, contentDescription = null, tint = Color(0xFF46464D), modifier = Modifier.size(20.dp))
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 17.dp)) {
            Text(localizedTripTitle(trip.title), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 21.sp)
            Text(
                text = localizedTripDateText(trip.dates, language),
                color = OdysseySubtext,
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 7.dp),
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 13.dp)
                    .height(6.dp)
                    .background(Color(0xFFEEEEF2), RoundedCornerShape(4.dp)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((trip.progress.coerceAtLeast(3)) / 100f)
                        .fillMaxHeight()
                        .background(Brush.horizontalGradient(listOf(OdysseyPurple, Color(0xFF8069EE))), RoundedCornerShape(4.dp)),
                )
            }
            Text(
                text = buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.W800))
                    append(localized("Маршрут заполнен на ${trip.progress}%", "Route ${trip.progress}% complete", "Ruta completada al ${trip.progress}%", "Route zu ${trip.progress}% abgeschlossen"))
                    pop()
                    if (trip.cities.isNotBlank()) append(" · ${localizedCityList(trip.cities, language)}")
                },
                color = OdysseySubtext,
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}

@Composable
private fun TripsLoadingCard() {
    Box(
        modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(22.dp)).background(cardSurfaceColor()),
    )
}

@Composable
private fun EmptyStateCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    body: String,
    action: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(cardSurfaceColor()).padding(horizontal = 24.dp, vertical = 26.dp),
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(OdysseyTint)) {
            Icon(icon, contentDescription = null, tint = OdysseyPurple, modifier = Modifier.size(25.dp))
        }
        Text(title, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 13.dp))
        Text(body, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (action != null && onAction != null) {
            Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = OdysseyPurple), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                Text(action, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
            }
        }
    }
}

@Composable
private fun TripOverviewLoading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
    ) {
        CircularProgressIndicator(color = OdysseyPurple, strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
    }
}

@Composable
private fun NewTripCard(onClick: () -> Unit) {
    val darkTheme = LocalDarkTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 2.dp.toPx()
                val dash = 7.dp.toPx()
                drawRoundRect(Color(0xFFD3D3DB), style = Stroke(width = stroke, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dash, dash), 0f)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()))
            }
            .clip(RoundedCornerShape(22.dp))
            .background(if (darkTheme) Color(0x6620222E) else Color(0x66FFFFFF))
            .clickable { onClick() }
            .padding(vertical = 34.dp, horizontal = 20.dp),
    ) {
        Text(
            text = "+",
            color = OdysseyPurple,
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 28.sp,
            modifier = Modifier
                .background(Color(0xFFEFEAFE), RoundedCornerShape(16.dp))
                .padding(horizontal = 15.dp, vertical = 6.dp),
        )
        Text(
            text = localized("Новое путешествие", "New trip", "Nuevo viaje", "Neue Reise"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = localized("С нуля или из шаблона", "From scratch or from a template", "Desde cero o desde una plantilla", "Von Grund auf oder aus einer Vorlage"),
            color = OdysseySubtext,
            fontFamily = Manrope,
            fontWeight = FontWeight.W500,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
