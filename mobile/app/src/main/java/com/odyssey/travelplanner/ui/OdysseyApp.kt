Exit code: 0
Wall time: 0.2 seconds
Total output lines: 13495
Output:
package com.odyssey.travelplanner.ui

import android.app.DatePickerDialog
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.viewinterop.AndroidView
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
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
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Logout
import androidx.compose.material.icons.outlined.Menu
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.NotificationsNone
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.DateRange
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
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.outlined.OpenInNew
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Check
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
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.maps.plugin.scalebar.scalebar
import com.mapbox.maps.extension.localization.localizeLabels
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
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
import com.odyssey.travelplanner.data.ExchangeRateRepository
import com.odyssey.travelplanner.data.WeatherRepository
import com.odyssey.travelplanner.data.WeatherSnapshot
import com.odyssey.travelplanner.data.localizedCityCatalogName
import com.odyssey.travelplanner.data.cityCatalogEntry
import com.odyssey.travelplanner.data.cityFlag
import com.odyssey.travelplanner.data.resolveTripPhotoReference
import com.odyssey.travelplanner.data.parseSightLinkCoordinates
import com.odyssey.travelplanner.data.resolveSightLinkCoordinates
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
import java.util.Locale
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Calendar
import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit
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
private val OdysseyLightColors = lightColorScheme(
    primary = OdysseyPurple,
    onPrimary = Color.White,
    background = OdysseyBackground,
    onBackground = OdysseyText,
    surface = OdysseySurface,
    onSurface = OdysseyText,
    surfaceVariant = OdysseySurface2,
    onSurfaceVariant = OdysseySubtext,
    outline = OdysseyBorder,
    error = Color(0xFFE0524B),
)
private val OdysseyDarkColors = darkColorScheme(
    primary = OdysseyPurple,
    onPrimary = Color.White,
    background = Color(0xFF141416),
    onBackground = Color(0xFFF5F6FA),
    surface = Color(0xFF20222E),
    onSurface = Color(0xFFF5F6FA),
    surfaceVariant = Color(0xFF2B2E3B),
    onSurfaceVariant = Color(0xFFBEC1CC),
    outline = Color(0xFF454958),
    error = Color(0xFFFF7B76),
)
private val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.W400),
    Font(R.font.manrope_medium, FontWeight.W500),
    Font(R.font.manrope_semibold, FontWeight.W600),
    Font(R.font.manrope_bold, FontWeight.W700),
    Font(R.font.manrope_extrabold, FontWeight.W800),
)
private val OdysseyNoFontPadding = PlatformTextStyle(includeFontPadding = false)
private val OdysseyFontPadding = PlatformTextStyle(includeFontPadding = true)
private val LocalDarkTheme = staticCompositionLocalOf { false }
private val LocalLanguage = staticCompositionLocalOf { "RU" }

private fun mapLocale(language: String): Locale = when (normalizeLanguage(language)) {
    "EN" -> Locale.ENGLISH
    "ES" -> Locale("es", "ES")
    "DE" -> Locale.GERMAN
    else -> Locale("ru", "RU")
}

private fun labelMapboxAccessibility(view: View, attributionDescription: String) {
    when {
        view.javaClass.name.endsWith("LogoViewImpl") -> view.contentDescription = "Mapbox"
        view.javaClass.name.endsWith("AttributionViewImpl") -> view.contentDescription = attributionDescription
    }
    if (view is ViewGroup) {
        repeat(view.childCount) { index -> labelMapboxAccessibility(view.getChildAt(index), attributionDescription) }
    }
}

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
    val configuration = Configuration(context.resources.configuration).apply { setLocale(locale) }
    return ContextThemeWrapper(context, R.style.Theme_Ramingo_DatePicker).apply {
        applyOverrideConfiguration(configuration)
    }
}

private fun localized(language: String, ru: String, en: String, es: String, de: String): String = when (normalizeLanguage(language)) {
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

private val tripTemplateKeys = listOf("italy", "czech", "alps", "tuscany")

private fun tripTemplateData(key: String?, language: String): Pair<String, String> = when (key) {
    "italy" -> localized(language, "Классическая Италия", "Classic Italy", "Italia clásica", "Klassisches Italien") to localized(language, "Рим, Флоренция, Пиза, Венеция, Милан", "Rome, Florence, Pisa, Venice, Milan", "Roma, Florencia, Pisa, Venecia, Milán", "Rom, Florenz, Pisa, Venedig, Mailand")
    "czech" -> localized(language, "Рождественская Европа", "Christmas Europe", "Europa navideña", "Weihnachtliches Europa") to localized(language, "Прага, Мюнхен, Верона, Милан, Венеция, Рим", "Prague, Munich, Verona, Milan, Venice, Rome", "Praga, Múnich, Verona, Milán, Venecia, Roma", "Prag, München, Verona, Mailand, Venedig, Rom")
    "alps" -> localized(language, "Австрия и Альпы", "Austria and the Alps", "Austria y los Alpes", "Österreich und die Alpen") to localized(language, "Вена, Зальцбург, Инсбрук, Грац", "Vienna, Salzburg, Innsbruck, Graz", "Viena, Salzburgo, Innsbruck, Graz", "Wien, Salzburg, Innsbruck, Graz")
    "tuscany" -> localized(language, "Тоскана на машине", "Tuscany by car", "Toscana en coche", "Toskana mit dem Auto") to localized(language, "Флоренция, Сиена, Сан-Джиминьяно, Лукка", "Florence, Siena, San Gimignano, Lucca", "Florencia, Siena, San Gimignano, Lucca", "Florenz, Siena, San Gimignano, Lucca")
    else -> "" to ""
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
            …180652 tokens truncated…fontSize = 14.sp)
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

@Composable
private fun RouteEditorDateField(
    label: String,
    value: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(5.dp)) {
        Text(label, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(cardSurfaceColor())
                .border(1.dp, OdysseyBorder, RoundedCornerShape(14.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 12.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = value.ifBlank { localized("Выберите дату", "Choose date", "Elige una fecha", "Datum auswählen") },
                    color = if (value.isBlank()) OdysseySubtext else contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 14.sp,
                    lineHeight = 20.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
                Spacer(Modifier.weight(1f))
                OdysseyCalendarIcon(17.dp, if (value.isBlank()) OdysseySubtext else OdysseyPurple)
            }
        }
    }
}

private fun mapCoordinate(city: String): Point? = cityCatalogEntry(city)?.let { entry ->
    Point.fromLngLat(entry.longitude, entry.latitude)
}

private fun restaurantLinkUri(value: String): String? {
    val raw = value.trim()
    if (raw.isBlank()) return null
    return if (raw.startsWith("http://", ignoreCase = true) || raw.startsWith("https://", ignoreCase = true)) {
        raw
    } else {
        "https://www.google.com/maps/search/?api=1&query=${Uri.encode(raw)}"
    }
}

private fun formatSightCoordinate(point: Point): String =
    String.format(Locale.US, "%.5f, %.5f", point.latitude(), point.longitude())


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
        modifier = Modifier.width(120.dp).height(150.dp).clip(RoundedCornerShape(16.dp)).background(Color(0xFF6C5CE7)),
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
                Icon(Icons.Filled.MoreVert, contentDescription = localized("Действия с путешествием", "Trip actions", "Acciones del viaje", "Reiseaktionen"), tint = Color(0xFF46464D), modifier = Modifier.size(20.dp))
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
private fun RamingoSplash() {
    val transition = rememberInfiniteTransition(label = "ramingo-splash")
    val iconScale by transition.animateFloat(
        initialValue = 0.96f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "splash-icon-scale",
    )
    val dotsProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100), RepeatMode.Reverse),
        label = "splash-dots",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.linearGradient(
                    colors = listOf(Color(0xFF4C39B8), Color(0xFF6C5CE7), Color(0xFF9D8FF4)),
                ),
            ),
    ) {
        Canvas(Modifier.fillMaxSize()) {
            drawCircle(
                color = Color.White.copy(alpha = 0.08f),
                radius = size.width * 0.72f,
                center = Offset(size.width * 0.96f, size.height * 0.12f),
            )
            drawCircle(
                color = Color.White.copy(alpha = 0.06f),
                radius = size.width * 0.56f,
                center = Offset(size.width * 0.02f, size.height * 0.88f),
            )
            val route = Path().apply {
                moveTo(size.width * 0.12f, size.height * 0.22f)
                cubicTo(
                    size.width * 0.78f, size.height * 0.29f,
                    size.width * 0.20f, size.height * 0.47f,
                    size.width * 0.76f, size.height * 0.57f,
                )
                cubicTo(
                    size.width * 0.91f, size.height * 0.64f,
                    size.width * 0.35f, size.height * 0.79f,
                    size.width * 0.86f, size.height * 0.91f,
                )
            }
            drawPath(
                route,
                Color.White.copy(alpha = 0.42f),
                style = Stroke(width = 1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 8.dp.toPx()), 0f)),
            )
            listOf(
                Offset(size.width * 0.12f, size.height * 0.22f),
                Offset(size.width * 0.76f, size.height * 0.57f),
                Offset(size.width * 0.86f, size.height * 0.91f),
            ).forEach { point ->
                drawCircle(Color.White.copy(alpha = 0.88f), radius = 3.5.dp.toPx(), center = point)
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 24.dp),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(128.dp)
                    .graphicsLayer {
                        scaleX = iconScale
                        scaleY = iconScale
                    }
                    .shadow(18.dp, RoundedCornerShape(38.dp), ambientColor = Color(0x40251B78), spotColor = Color(0x40251B78))
                    .clip(RoundedCornerShape(38.dp))
                    .border(1.dp, Color.White.copy(alpha = 0.28f), RoundedCornerShape(38.dp))
                    .background(Color.White.copy(alpha = 0.16f)),
            ) {
                Canvas(Modifier.size(78.dp)) {
                    val center = Offset(size.width / 2f, size.height / 2f)
                    val radius = size.minDimension * 0.34f
                    val stroke = 3.2.dp.toPx()
                    drawCircle(Color.White.copy(alpha = 0.96f), radius, center, style = Stroke(stroke))

                    val northNeedle = Path().apply {
                        moveTo(center.x, center.y - size.height * 0.34f)
                        lineTo(center.x + size.width * 0.11f, center.y)
                        lineTo(center.x, center.y + size.height * 0.05f)
                        lineTo(center.x - size.width * 0.11f, center.y)
                        close()
                    }
                    drawPath(northNeedle, Color.White)

                    val southNeedle = Path().apply {
                        moveTo(center.x, center.y + size.height * 0.34f)
                        lineTo(center.x + size.width * 0.11f, center.y)
                        lineTo(center.x, center.y - size.height * 0.05f)
                        lineTo(center.x - size.width * 0.11f, center.y)
                        close()
                    }
                    drawPath(southNeedle, Color(0xFFCFC8FF))
                    drawCircle(OdysseyPurple, radius = 4.4.dp.toPx(), center = center)
                }
            }

            Text(
                text = "Ramingo",
                color = Color.White,
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 30.sp,
                letterSpacing = (-0.7).sp,
                modifier = Modifier.padding(top = 24.dp),
            )
            Text(
                text = "Планируй. Путешествуй. Запоминай.",
                color = Color.White.copy(alpha = 0.78f),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 7.dp),
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(7.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 32.dp),
            ) {
                repeat(3) { index ->
                    val phase = (dotsProgress + index * 0.22f) % 1f
                    val emphasis = 1f - kotlin.math.abs(phase * 2f - 1f)
                    Box(
                        modifier = Modifier
                            .size(7.dp)
                            .graphicsLayer {
                                scaleX = 0.82f + emphasis * 0.28f
                                scaleY = 0.82f + emphasis * 0.28f
                                alpha = 0.35f + emphasis * 0.65f
                            }
                            .background(Color.White, CircleShape),
                    )
                }
            }
        }
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

