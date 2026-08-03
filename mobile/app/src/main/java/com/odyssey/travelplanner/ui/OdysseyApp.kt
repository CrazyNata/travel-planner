package com.odyssey.travelplanner.ui

import android.app.DatePickerDialog
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
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.JsonPrimitive
import java.util.Calendar
import java.time.LocalDate
import java.util.Locale

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

private fun localized(language: String, ru: String, en: String, es: String, de: String): String = when (language) {
    "EN" -> en
    "ES" -> es
    "DE" -> de
    else -> ru
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

private fun formatPhotoDateRange(range: PhotoDateRange): String {
    val startMonth = PhotoMonthNames[range.start.monthValue - 1]
    val endMonth = PhotoMonthNames[range.end.monthValue - 1]
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
    var darkTheme by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("RU") }

    LaunchedEffect(Unit) {
        if (SupabaseProvider.restorePersistentSession()) {
            runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadProfile() }.getOrNull()?.let { profile ->
                darkTheme = profile.darkTheme
                language = profile.language
            }
            navController.navigate("trips") { popUpTo("foundation") { inclusive = true } }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme, LocalLanguage provides language) {
    MaterialTheme {
        Surface(color = if (darkTheme) Color(0xFF141416) else OdysseyBackground) {
            NavHost(navController = navController, startDestination = "foundation") {
                composable("foundation") { AuthScreen(onAuthenticated = { navController.navigate("trips") }) }
                composable("trips") {
                    MyTripsScreen(
                        onTripClick = { navController.navigate("trip/$it") },
                        onNewTrip = { navController.navigate("create-trip") },
                        onLogout = {
                            navController.navigate("foundation") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        onCatalog = { navController.navigate("catalog") },
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme },
                        onThemeSet = { darkTheme = it },
                        language = language,
                        onLanguageChange = { language = it },
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
private fun AuthScreen(onAuthenticated: () -> Unit) {
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
    var rememberSession by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    fun messageText(ru: String, en: String, es: String, de: String) = when (language) { "EN" -> en; "ES" -> es; "DE" -> de; else -> ru }

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
        AuthField("E-mail", "you@example.com", email) { email = it }
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
            modifier = Modifier.padding(top = 14.dp).clickable { rememberSession = !rememberSession },
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
            onLanguageChange(profile.language)
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
            if (editingTrip != null) item {
                EditTripPanel(editingTrip!!, onClose = { editingTrip = null }, onSaved = { updated ->
                    trips = trips.map { if (it.id == updated.id) updated else it }
                    editingTrip = null
                })
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
    var title by remember(trip.id) { mutableStateOf(trip.title) }
    var cities by remember(trip.id) { mutableStateOf(trip.cities) }
    var dates by remember(trip.id) { mutableStateOf(trip.dates) }
    var status by remember(trip.id) { mutableStateOf(trip.status) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardSurfaceColor()).padding(17.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("Редактировать путешествие", "Edit trip", "Editar viaje", "Reise bearbeiten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 18.sp)
        AuthField("Название", "Название", title) { title = it }
        AuthField("Города", "Города", cities) { cities = it }
        AuthField("Даты", "Например, 12–15 сентября", dates) { dates = it }
        Text(localized("Статус путешествия", "Trip status", "Estado del viaje", "Reisestatus"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
            listOf(
                "Предстоящее" to localized("Предстоящие", "Upcoming", "Próximos", "Bevorstehend"),
                "Черновик" to localized("Черновики", "Drafts", "Borradores", "Entwürfe"),
                "Прошедшее" to localized("Прошедшие", "Past", "Pasados", "Vergangen"),
            ).forEach { (value, label) ->
                val selected = status == value
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
                    runCatching {
                        val repository = SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow())
                        repository.updateTripDetails(trip.id, title, dates, cities)
                        repository.updateTripSection(trip.id, "status", JsonPrimitive(status))
                    }
                        .onSuccess { onSaved(trip.copy(title = title.trim(), cities = cities.trim(), dates = dates.trim(), status = status)) }
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
        "italy" -> "Рождественская Европа" to "Прага, Мюнхен, Верона, Милан, Венеция, Рим"
        "czech" -> "Классическая Италия" to "Рим, Флоренция, Пиза, Венеция, Милан"
        "alps" -> "Альпы с семьёй" to "Мюнхен, Инсбрук, Зальцбург, Вена"
        "baltic" -> "Балтийский маршрут" to "Таллин, Рига, Вильнюс"
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
        listOf("italy", "Рождественская Европа", "12 дней · 6 городов", "Прага → Мюнхен → Верона → Милан → Венеция → Рим", "https://images.unsplash.com/photo-1500534623283-312aade485b7?auto=format&fit=crop&w=1200&q=85"),
        listOf("czech", "Классическая Италия", "10 дней · 5 городов", "Рим → Флоренция → Пиза → Венеция → Милан", "https://images.unsplash.com/photo-1506157786151-b8491531f063?auto=format&fit=crop&w=1200&q=85"),
        listOf("alps", "Альпы с семьёй", "7 дней · 4 города", "Мюнхен → Инсбрук → Зальцбург → Вена", "https://images.unsplash.com/photo-1464822759023-fed622ff2c3b?auto=format&fit=crop&w=1200&q=85"),
        listOf("baltic", "Балтийский маршрут", "6 дней · 3 города", "Таллин → Рига → Вильнюс", "https://images.unsplash.com/photo-1500530855697-b586d89ba3ee?auto=format&fit=crop&w=1200&q=85"),
    )
    LazyColumn(
        modifier = Modifier.fillMaxSize().background(if (darkTheme) Color(0xFF141416) else OdysseyBackground).padding(WindowInsets.statusBars.asPaddingValues()),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(18.dp),
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
        items(templates) { template ->
            val (id, title, duration, route, image) = template
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(cardSurfaceColor())) {
                Box(modifier = Modifier.fillMaxWidth().height(130.dp)) {
                    AsyncImage(model = image, contentDescription = null, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
                    Text(duration, color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp, modifier = Modifier.align(Alignment.BottomStart).padding(12.dp).background(Color(0xAA26343D), RoundedCornerShape(10.dp)).padding(horizontal = 10.dp, vertical = 6.dp))
                }
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
private fun TripOverviewScreen(tripId: String, onBack: () -> Unit) {
    val darkTheme = LocalDarkTheme.current
    var overview by remember { mutableStateOf<TripOverview?>(null) }
    var weather by remember { mutableStateOf<Map<String, WeatherSnapshot>>(emptyMap()) }
    var loading by remember { mutableStateOf(true) }
    var tab by remember { mutableStateOf("overview") }
    var sectionMenuOpen by remember { mutableStateOf(false) }
    var refresh by remember { mutableStateOf(0) }
    var loadError by remember { mutableStateOf<String?>(null) }
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, tripId) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) refresh++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(tripId, refresh) {
        loading = true
        loadError = null
        overview = runCatching {
            SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadTripOverview(tripId)
        }.onFailure { loadError = it.message }.getOrNull()
        overview?.let { trip ->
            val cities = trip.overviewMapPoints.ifEmpty {
                trip.routeLegs.flatMap { listOf(it.from, it.to) }.distinct()
            }
            weather = WeatherRepository().loadCurrent(cities, trip.dates)
        }
        loading = false
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
                    text = overview?.title.orEmpty(),
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
                            Text(overview?.title.orEmpty(), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(overview?.dates.orEmpty().replace("декабря", "дек").replace("января", "янв").replace(" · 16 дней", " ·\n16 дней"), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.5.sp, lineHeight = 14.5.sp, maxLines = 2, overflow = TextOverflow.Ellipsis)
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
    val selectedLeg = overview.routeLegs.firstOrNull { routeLegDayNumber(it, overview.routeLegs) == routeDay }
    val mapCities = selectedLeg?.let { listOf(it.from, it.to) } ?: listOf(selectedDayCity)
    val sightRoutePoints = visibleSights.mapNotNull { sight -> sight.longitude?.let { longitude -> sight.latitude?.let { latitude -> Point.fromLngLat(longitude, latitude) } } }
    val sightMapPoints = visibleSights.mapNotNull { sight ->
        sight.longitude?.let { longitude -> sight.latitude?.let { latitude -> Point.fromLngLat(longitude, latitude) } }
            ?: mapCoordinate(sight.city)
    }
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
                "${selectedDayCity.uppercase()} · ${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} $routeDay",
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
                            selectedDayCity,
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
                            Text(dayCity, color = if (selected) OdysseyPurple else contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
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
                    footer = {
                        Row(modifier = Modifier.fillMaxWidth().height(62.dp).padding(horizontal = 15.dp, vertical = 13.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    "${selectedDayCity.uppercase()} · ${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} $routeDay",
                                    color = OdysseyPurple,
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W800,
                                    fontSize = 11.sp,
                                    lineHeight = 15.sp,
                                    letterSpacing = 0.66.sp,
                                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                                )
                                Text(
                                    "$selectedDayCity · ${visibleSights.size} ${localized("места", "places", "lugares", "Orte")}",
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
                SightCard(sight, uploadingSightId == sight.id, onEdit = { editingSight = sight }, onAddPhoto = { uploadingSightId = sight.id; photoPicker.launch("image/*") })
            }
        }
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
                Text(sight.name, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, lineHeight = 18.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), modifier = Modifier.weight(1f).padding(start = 12.dp), maxLines = 2, overflow = TextOverflow.Ellipsis)
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
            RouteEditorField(localized("Город", "City", "Ciudad", "Stadt"), city, {}, Modifier.weight(1f))
        }
        Text(localized("ДОСТОПРИМЕЧАТЕЛЬНОСТИ", "SIGHTS", "LUGARES", "SEHENSWÜRDIGKEITEN"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.7.sp, modifier = Modifier.padding(top = 6.dp))
        sights.take(3).forEach { sight ->
            Row(modifier = Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFF5F4F8)).padding(10.dp), verticalAlignment = Alignment.CenterVertically) {
                if (sight.photo.isNotBlank()) {
                    AsyncImage(model = sight.photo, contentDescription = sight.name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.size(52.dp).clip(RoundedCornerShape(11.dp)))
                } else {
                    SurfaceEmptyMedia(Icons.Outlined.LocationOn, Modifier.size(52.dp).clip(RoundedCornerShape(11.dp)))
                }
                Column(modifier = Modifier.weight(1f).padding(start = 11.dp)) { Text(sight.name, color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(sight.description.ifBlank { sight.category }, color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.8.sp, maxLines = 1) }
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
                Text("${city.uppercase()} · ${localized("ДЕНЬ", "DAY", "DÍA", "TAG")} $day", color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp)
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
private fun SightCard(sight: com.odyssey.travelplanner.data.Sight, uploading: Boolean, onEdit: () -> Unit, onAddPhoto: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(13.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(cardSurfaceColor())
            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x10141428), spotColor = Color(0x10141428))
            .padding(11.dp),
    ) {
        Box(modifier = Modifier.size(82.dp).clip(RoundedCornerShape(13.dp)).background(Color(0xFFE3E1EC)).clickable(enabled = !uploading) { onAddPhoto() }) {
            if (sight.photo.isNotBlank()) {
                AsyncImage(model = sight.photo, contentDescription = sight.name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                SurfaceEmptyMedia(Icons.Outlined.LocationOn, Modifier.fillMaxSize())
            }
        }
        Column(modifier = Modifier.weight(1f).clickable { onEdit() }, verticalArrangement = Arrangement.Center) {
            Text(
                sight.name,
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
                sight.category.ifBlank { sight.description },
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
    var name by remember(sight.id) { mutableStateOf(sight.name) }
    var city by remember(sight.id) { mutableStateOf(sight.city) }
    var category by remember(sight.id) { mutableStateOf(sight.category) }
    var description by remember(sight.id) { mutableStateOf(sight.description) }
    var walkDay by remember(sight.id) { mutableStateOf(sight.walkDay.toString()) }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(cardSurfaceColor()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(localized("Редактировать место", "Edit sight", "Editar lugar", "Ort bearbeiten"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
        AuthField("Название", "Название", name) { name = it }
        AuthField("Город", "Город", city) { city = it }
        AuthField("Категория", "Категория", category) { category = it }
        AuthField("Описание", "Что важно увидеть", description) { description = it }
        AuthField("День маршрута", "Например, 1", walkDay) { walkDay = it.filter(Char::isDigit) }
        if (message != null) Text(message!!, color = Color(0xFFE0524B), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Button(onClick = onClose, colors = ButtonDefaults.buttonColors(containerColor = secondarySurfaceColor(), contentColor = contentTextColor()), shape = RoundedCornerShape(11.dp)) { Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope, fontWeight = FontWeight.W800) }
            Button(onClick = {
                scope.launch {
                    saving = true
                    runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateSightDetailsRich(tripId, sight.id, name, city, category, description, walkDay.toIntOrNull() ?: sight.walkDay) }
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
    var draftFeatureFilters by remember { mutableStateOf(setOf("Приоритет", "С собакой")) }
    var draftPriceFilter by remember { mutableStateOf("€€") }
    var draftRatingFilter by remember { mutableStateOf("4.5+") }
    val scope = rememberCoroutineScope()
    val tripCityOptions = (
        overview.cities +
            overview.routeLegs.flatMap { listOf(it.from, it.to) } +
        overview.sights.map { it.city } +
            overview.accommodations.map { it.city } +
            overview.restaurants.map { it.city }
        ).map(String::trim).filter(String::isNotBlank).distinctBy(::cityFilterKey)
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
                "Приоритет" -> note.contains("приоритет") || note.contains("priority")
                "С собакой" -> note.contains("с собакой") || note.contains("dog")
                "Есть бронь" -> restaurant.status == "бронь" || note.contains("бронь") || note.contains("reserv")
                "Веган" -> note.contains("веган") || note.contains("vegan")
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
    val activeFilterCount = listOf(priceFilter.isNotBlank(), ratingFilter.isNotBlank()).count { it }
    val filterCount = if (activeFilterCount == 0) 2 else activeFilterCount
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
                        selectedCity,
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
                        Text(option, color = if (option == selectedCity) OdysseyPurple else contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).background(if (option == selectedCity) OdysseyTint else Color.Transparent).clickable { selectedCity = option; cityMenuOpen = false }.padding(horizontal = 12.dp, vertical = 11.dp))
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
                        context,
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
                        value = city,
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
                    RestaurantAddStatusChip("хочу", "want", status == "хочу", 61.4f, scale, onStatusChange)
                    RestaurantAddStatusChip("бронь", "reserve", status == "бронь", 71.4f, scale, onStatusChange)
                    RestaurantAddStatusChip("были", "visited", status == "были", 65.1f, scale, onStatusChange)
                }
                RestaurantAddStatusChip(
                    label = "🔥 Приоритет",
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
                                text = option,
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
            horizontalArrangement = Arrangement.spacedBy(d(5f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(d(51f))
                .clip(RoundedCornerShape(d(14f)))
                .background(OdysseyTrack)
                .padding(d(5f)),
        ) {
            listOf("€€", "€€€", "€€€€").forEach { option ->
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
                        fontSize = s(14f),
                        lineHeight = s(19f),
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

        Box(Modifier.fillMaxWidth().height(d(604f))) {
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
                RestaurantFilterFeatureChip("Приоритет", "priority", "Приоритет" in features, 122.5f, scale, onFeatureToggle)
                RestaurantFilterFeatureChip("С собакой", "dog", "С собакой" in features, 117.1f, scale, onFeatureToggle)
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(d(9f)),
                modifier = Modifier.offset(x = d(16f), y = d(270f)).height(d(38f)),
            ) {
                RestaurantFilterFeatureChip("Есть бронь", "reservation", "Есть бронь" in features, 123.1f, scale, onFeatureToggle)
                RestaurantFilterFeatureChip("Веган", "vegan", "Веган" in features, 87.64f, scale, onFeatureToggle)
            }

            Text(
                text = localized("СРЕДНИЙ ЧЕК", "AVERAGE PRICE", "PRECIO MEDIO", "DURCHSCHNITTSPREIS"),
                style = sectionStyle,
                modifier = Modifier.offset(x = d(16f), y = d(330f)).width(d(336f)).height(d(15f)),
            )
            RestaurantFilterSegmentedRow(
                options = listOf("€", "€€", "€€€"),
                selected = price,
                onSelect = onPriceChange,
                scale = scale,
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
            .clickable { onToggle(label) }
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
                    fontSize = d(14f).value.sp,
                    lineHeight = d(19f).value.sp,
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
    var photoIndex by remember(restaurant.id) { mutableStateOf(0) }
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
            .height(196.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(cardSurfaceColor())
            .shadow(6.dp, RoundedCornerShape(18.dp), clip = false, ambientColor = Color(0x0F141428), spotColor = Color(0x0F141428))
            .padding(12.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().height(78.dp),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(13.dp),
        ) {
            Box(modifier = Modifier.size(78.dp).clip(RoundedCornerShape(14.dp)).background(Color(0xFFE4E1EA)).clickable(enabled = !uploading) { onAddPhoto() }) {
                restaurant.photos.getOrNull(photoIndex)?.let { AsyncImage(model = it, contentDescription = restaurant.name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
                if (restaurant.photos.isEmpty()) Icon(Icons.Outlined.Restaurant, contentDescription = null, tint = Color(0xFFA7A1B2), modifier = Modifier.align(Alignment.Center).size(27.dp))
            }
            Column(modifier = Modifier.weight(1f).height(78.dp)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
                    Text(
                        restaurant.name,
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 16.sp,
                        lineHeight = 17.6.sp,
                        style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f),
                    )
                    OdysseyExternalLinkIcon(17.dp, OdysseyPurple, modifier = Modifier.padding(top = 2.dp))
                }
                Text(
                    restaurant.city.ifBlank { localized("Город не указан", "City not specified", "Ciudad no indicada", "Stadt nicht angegeben") },
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
                    if (restaurant.note.isNotBlank() && !restaurant.note.contains("http", ignoreCase = true)) {
                        Box(modifier = Modifier.height(25.dp).clip(RoundedCornerShape(8.dp)).background(secondarySurfaceColor()).padding(horizontal = 8.dp, vertical = 4.dp), contentAlignment = Alignment.Center) {
                            Text(restaurant.note, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, lineHeight = 17.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding))
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
            .height(282.dp)
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
                .height(62.dp)
                .padding(horizontal = 15.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "RESTAURANTS",
                    color = OdysseyPurple,
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 11.sp,
                    lineHeight = 15.sp,
                    letterSpacing = 0.66.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                )
                Text(
                    placesLabel,
                    color = secondaryTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W700,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding),
                    modifier = Modifier.padding(top = 2.dp),
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
        AuthField("Название", "Название", name) { name = it }
        AuthField("Город", "Город", city) { city = it }
        AuthField("Заметка", "Кухня или комментарий", note) { note = it }
        AuthField("Цена", "€€ / €€€", price) { price = it }
        AuthField("Ссылка", "https://…", link) { link = it }
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
                message = it.message ?: "Не удалось загрузить фото"
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
            ?.let(::formatPhotoDateRange)
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
                        Text(city, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 18.sp, modifier = Modifier.padding(start = 10.dp))
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
                        listOf("Редактор" to "Редактор", "Просмотр" to "Читатель").forEach { (label, value) ->
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
    val surface = cardSurfaceColor()
    val avatarColor = when (member.tone) {
        "sand", "orange" -> Color(0xFFF29A32)
        "teal", "green" -> Color(0xFF35AEB9)
        else -> OdysseyPurple
    }
    val isOwner = member.role == "Владелец"
    val roleLabel = if (member.role == "Читатель") "Просмотр" else member.role
    val roleBackground = when (roleLabel) {
        "Владелец" -> Color(0xFFEDEAFF)
        "Редактор" -> Color(0xFFEEFAF3)
        else -> Color(0xFFF3F3F6)
    }
    val roleColor = when (roleLabel) {
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
                listOf("Редактор" to "Редактор", "Просмотр" to "Читатель").forEach { (label, value) ->
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
                text = style.label,
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
                    text = expense.name,
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
                    text = categoryStyle.label,
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
                    BudgetChoiceChip("Жильё", category == "Жильё", 79.2f, scale) { onCategoryChange("Жильё") }
                    BudgetChoiceChip("Транспорт", category == "Транспорт", 106.8f, scale) { onCategoryChange("Транспорт") }
                }
                Spacer(Modifier.height(d(9f)))
                Row(horizontalArrangement = Arrangement.spacedBy(d(9f))) {
                    BudgetChoiceChip("Еда и рестораны", category == "Еда и рестораны", 147.6f, scale) { onCategoryChange("Еда и рестораны") }
                    BudgetChoiceChip("Активности и билеты", category == "Активности и билеты", 178.7f, scale) { onCategoryChange("Активности и билеты") }
                }
                Spacer(Modifier.height(d(9f)))
                BudgetChoiceChip("Прочее", category == "Прочее", 85.1f, scale) { onCategoryChange("Прочее") }
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
                BudgetChoiceChip("Общий", scopeName == "общий", 79.8f, scale) { onScopeChange("общий") }
                BudgetChoiceChip("Семья", scopeName == "семья", 77.9f, scale) { onScopeChange("семья") }
                BudgetChoiceChip("Личный", scopeName == "личный", 87.4f, scale) { onScopeChange("личный") }
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
            Text("${expenses.size} трат · $currency", color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
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
                    Text("Новая трата", color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                    AuthField("Название", "Например, билеты", name) { name = it }
                    AuthField("Сумма в $currency", "0", amountInput) { amountInput = it }
                    AuthField("Кто оплатил", "Имя", paidBy) { paidBy = it }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                        categories.forEach { option ->
                            val selected = category == option
                            Text(option, color = if (selected) Color.White else OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp, modifier = Modifier.background(if (selected) OdysseyPurple else Color(0xFFF0F0F4), RoundedCornerShape(12.dp)).clickable { category = option }.padding(horizontal = 10.dp, vertical = 7.dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        listOf("общий", "семья", "личный").forEach { option ->
                            val selected = scopeName == option
                            Text(option, color = if (selected) Color.White else OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 11.sp, modifier = Modifier.background(if (selected) OdysseyPurple else secondarySurfaceColor(), RoundedCornerShape(12.dp)).clickable { scopeName = option }.padding(horizontal = 10.dp, vertical = 7.dp))
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
                Text("Общий бюджет", color = Color(0xDFFFFFFF), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp)
                Text(amount(total), color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 28.sp, modifier = Modifier.padding(top = 5.dp))
            }
        }
        if (overview.budgetGroups.isNotEmpty()) item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.horizontalScroll(rememberScrollState())) {
                overview.budgetGroups.forEach { group ->
                    Column(modifier = Modifier.width(158.dp).clip(RoundedCornerShape(16.dp)).background(surface).padding(14.dp)) {
                        Text(group.name, color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
                        Text(amount(total * group.people / peopleTotal), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, modifier = Modifier.padding(top = 5.dp))
                        Text("Доля из общих трат", color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp))
                    }
                }
            }
        }
        item {
            if (addingGroup) {
                Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(surface).padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Новая группа", color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp)
                    AuthField("Название", "Например, Друзья", groupName) { groupName = it }
                    AuthField("Участников", "1", groupPeople) { groupPeople = it }
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
                Text("＋ Разделить бюджет", color = OdysseyPurple, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(surface).clickable { addingGroup = true }.padding(horizontal = 15.dp, vertical = 11.dp))
            }
        }
        item { Text("По категориям", color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp)) }
        items(categories) { category ->
            val categoryTotal = expenses.filter { it.category == category }.sumOf { it.amount }
            Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(surface).padding(14.dp)) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    Text(category, color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp)
                    Text(amount(categoryTotal), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
                }
                Spacer(Modifier.height(8.dp))
                Box(Modifier.fillMaxWidth().height(7.dp).background(Color(0xFFEEEEF2), RoundedCornerShape(4.dp))) {
                    Spacer(Modifier.fillMaxHeight().fillMaxWidth(if (total == 0.0) 0f else (categoryTotal / total).toFloat()).background(OdysseyPurple, RoundedCornerShape(4.dp)))
                }
            }
        }
        item { Text("Траты", color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp, modifier = Modifier.padding(top = 4.dp)) }
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
                    Text(listOf(expense.scope, expense.paidBy).filter(String::isNotBlank).joinToString(" · "), color = OdysseySubtext, fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 12.sp, modifier = Modifier.padding(top = 3.dp))
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
        AuthField("Название", "Название", name) { name = it }
        AuthField("Сумма", "0", amount) { amount = it }
        AuthField("Категория", "Категория", category) { category = it }
        AuthField("Кто оплатил", "Имя", paidBy) { paidBy = it }
        AuthField("Тип бюджета", "общий / семья / личный", scopeName) { scopeName = it }
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
    val surface = cardSurfaceColor()
    val city = accommodation.city.trim()
    val cityPrefix = cityFlag(city).takeUnless { it == "📍" }.orEmpty()
    val cityLabel = listOf(cityPrefix, city).filter(String::isNotBlank).joinToString(" ")
    val dates = formatAccommodationDates(accommodation.dates)
    val price = formatAccommodationPrice(accommodation.price)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(surface)
            .shadow(8.dp, RoundedCornerShape(20.dp), clip = false, ambientColor = Color(0x12141428), spotColor = Color(0x12141428)),
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(210.dp).background(Color(0xFFCCCCCC))) {
            accommodation.photos.firstOrNull()?.let { imageUrl ->
                AsyncImage(model = imageUrl, contentDescription = accommodation.name, contentScale = androidx.compose.ui.layout.ContentScale.Crop, modifier = Modifier.fillMaxSize())
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
                    Text("Бесплатная отмена до ${formatAccommodationDeadline(accommodation.deadline)}", color = Color(0xFF22B07D), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.padding(top = if (accommodation.deadline.isNotBlank()) 15.5.dp else 12.dp).height(42.dp)) {
                Box(modifier = (if (accommodation.bookingUrl.isNotBlank()) Modifier.width(150.234.dp) else Modifier.weight(1f)).fillMaxHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, OdysseyBorder, RoundedCornerShape(12.dp)).clickable { onEdit() }, contentAlignment = Alignment.Center) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                        OdysseyEditIcon(15.dp, OdysseyPurple)
                        Text("Редактировать", color = OdysseyLabel, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                    }
                }
                if (accommodation.bookingUrl.isNotBlank()) {
                    Box(modifier = Modifier.weight(1f).fillMaxHeight().clip(RoundedCornerShape(12.dp)).border(1.dp, OdysseyBorder, RoundedCornerShape(12.dp)).clickable { uriHandler.openUri(accommodation.bookingUrl) }, contentAlignment = Alignment.Center) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
                            OdysseyExternalLinkIcon(15.dp, OdysseyPurple)
                            Text("На Booking", color = OdysseyLabel, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.5.sp, lineHeight = 17.sp, style = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding), maxLines = 1)
                        }
                    }
                }
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
                    AddStatusChip("хочу", "хочу", 70.6f)
                    AddStatusChip("бронь", "бронь", 81f)
                    AddStatusChip("оплачено", "оплачено", 106.6f)
                }
                AddStatusChip("пожили", "пожили", 92.2f, Modifier.offset(x = d(16f), y = d(374f)))

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
                .height(d(537f)),
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
                modifier = Modifier.offset(x = d(16f), y = d(272f)).width(d(336f)),
                onValueChange = { price = it },
            )
            AccommodationEditTextField(
                label = localized("Ссылка на Booking", "Booking link", "Enlace de Booking", "Booking-Link"),
                value = bookingUrl,
                placeholder = "https://booking.com/...",
                valueWeight = FontWeight.W600,
                valueColor = OdysseyPurple,
                scale = scale,
                modifier = Modifier.offset(x = d(16f), y = d(365f)).width(d(336f)),
                onValueChange = { bookingUrl = it },
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(d(11f)),
                modifier = Modifier
                    .offset(x = d(16f), y = d(466f))
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
                    modifier = Modifier.offset(x = d(16f), y = d(440f)).width(d(336f)),
                )
            }
        }
    }
    datePickerTarget?.let { target ->
        AccommodationCalendarDialog(
            initialValue = if (target == "checkIn") checkIn else checkOut,
            onDismiss = { datePickerTarget = null },
            onConfirm = { selected ->
                if (target == "checkIn") checkIn = selected else checkOut = selected
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
        ModalBottomSheet(onDismissRequest = { adding = false; editingLeg = null; message = null }, containerColor = cardSurfaceColor()) {
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
    var distance by remember { mutableStateOf("") }
    var travelTime by remember { mutableStateOf("") }
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(11.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(localized("День маршрута", "Route day", "Día de ruta", "Reisetag"), color = OdysseyText, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 23.sp)
            Spacer(Modifier.weight(1f))
            Box(modifier = Modifier.size(37.dp).clip(CircleShape).background(Color(0xFFF5F4F8)).clickable { onCancel() }, contentAlignment = Alignment.Center) {
                Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = OdysseySubtext, modifier = Modifier.size(18.dp))
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            RouteEditorField(localized("Откуда", "From", "Desde", "Von"), from, onFromChange, Modifier.weight(1f))
            RouteEditorField(localized("Куда", "To", "A", "Nach"), to, onToChange, Modifier.weight(1f))
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
    val clipboard = androidx.compose.ui.platform.LocalClipboardManager.current
    val mapsUrl = leg.mapsUrl.ifBlank {
        "https://www.google.com/maps/dir/?api=1&origin=${Uri.encode(leg.from)}&destination=${Uri.encode(leg.to)}"
    }
    val longDestination = leg.to.length > 14
    val dateParts = routeDateParts(leg.date, tripDates, dayIndex)
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
        Text("$flag $city", color = if (isLast) contentTextColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = if (isLast) FontWeight.W800 else FontWeight.W700, fontSize = if (compact) 14.sp else if (isLast) 17.sp else 13.sp, maxLines = 1, overflow = TextOverflow.Clip, modifier = Modifier.padding(start = 4.dp))
    }
}

private fun routeDateParts(date: String, tripDates: String, dayIndex: Int): Pair<String, String> {
    val months = mapOf("01" to "ЯНВ", "02" to "ФЕВ", "03" to "МАР", "04" to "АПР", "05" to "МАЙ", "06" to "ИЮН", "07" to "ИЮЛ", "08" to "АВГ", "09" to "СЕН", "10" to "ОКТ", "11" to "НОЯ", "12" to "ДЕК")
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
    return calendar.get(Calendar.DAY_OF_MONTH).toString() to (months[(calendar.get(Calendar.MONTH) + 1).toString().padStart(2, '0')] ?: "")
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

private fun formatAccommodationDates(value: String): String {
    val raw = value.trim()
    if (raw.isBlank()) return "Даты не указаны"
    val parts = raw.split(Regex("\\s+[–-]\\s+"))
    if (parts.size != 2) return raw
    fun parseIso(source: String): Calendar? {
        val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(source.trim()) ?: return null
        return Calendar.getInstance().apply {
            clear()
            set(match.groupValues[1].toInt(), match.groupValues[2].toInt() - 1, match.groupValues[3].toInt())
        }
    }
    val start = parseIso(parts[0]) ?: return raw
    val end = parseIso(parts[1]) ?: return raw
    val months = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
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

private fun formatAccommodationDeadline(value: String): String {
    val raw = value.trim()
    val match = Regex("(\\d{4})-(\\d{2})-(\\d{2})").matchEntire(raw) ?: return raw
    val months = listOf("янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек")
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
    val cities = overview.overviewMapPoints.ifEmpty {
        overview.routeLegs.flatMap { listOf(it.from, it.to) }.distinct()
    }

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
                    text = activePhoto?.city.orEmpty(),
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
        item { OverviewMapCard(overview.routeLegs, cities) }
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
                cities.forEach { city -> WeatherPlaceholder(city, photos.firstOrNull { it.city.equals(city, true) }, weather[city], tripDatesWeather) }
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
    cardShape: RoundedCornerShape = RoundedCornerShape(20.dp),
    cardShadow: Dp? = null,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
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

    LaunchedEffect(mapStyleReady, coordinates) {
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
                    sightAnnotationManager.create(
                        CircleAnnotationOptions()
                            .withPoint(point)
                            .withCircleRadius(9.0)
                            .withCircleColor("#6C5CE7")
                            .withCircleStrokeColor("#FFFFFF")
                            .withCircleStrokeWidth(3.0),
                    )
                    sightNumberAnnotationManager.create(
                        PointAnnotationOptions()
                            .withPoint(point)
                            .withTextField((index + 1).toString())
                            .withTextColor("#FFFFFF")
                            .withTextSize(12.0)
                            .withTextAnchor(TextAnchor.CENTER),
                    )
                }
            }
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
            Text(text = localized("${legs.size} переездов · ${cities.size} городов", "${legs.size} legs · ${cities.size} cities", "${legs.size} trayectos · ${cities.size} ciudades", "${legs.size} Etappen · ${cities.size} Städte"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 14.sp)
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
    "munich", "мюнхен" -> Point.fromLngLat(11.5820, 48.1351)
    "vienna", "вена" -> Point.fromLngLat(16.3738, 48.2082)
    "innsbruck", "инсбрук" -> Point.fromLngLat(11.4041, 47.2692)
    "florence", "флоренция" -> Point.fromLngLat(11.2558, 43.7696)
    "venice", "венеция" -> Point.fromLngLat(12.3155, 45.4408)
    "tallinn", "таллин" -> Point.fromLngLat(24.7536, 59.4370)
    "riga", "рига" -> Point.fromLngLat(24.1052, 56.9496)
    "vilnius", "вильнюс" -> Point.fromLngLat(25.2797, 54.6872)
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
    val displayedCondition = if (tripDatesWeather) weather?.tripCondition ?: localized("Прогноз пока недоступен", "Forecast unavailable", "Pronóstico no disponible", "Vorhersage nicht verfügbar") else weather?.condition
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
            text = city,
            color = Color.White,
            fontFamily = Manrope,
            fontWeight = FontWeight.W700,
            fontSize = 13.sp,
            modifier = Modifier.align(Alignment.TopStart).padding(12.dp),
        )
        Column(modifier = Modifier.align(Alignment.BottomStart).padding(12.dp)) {
            Text(displayedTemperature ?: "…", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 26.sp)
            Text(displayedCondition.orEmpty(), color = Color(0xDDFFFFFF), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp)
        }
    }
}

@Composable
private fun TripListCard(trip: TripCard, onTripClick: (String) -> Unit, onEdit: () -> Unit) {
    val darkTheme = LocalDarkTheme.current
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
                    text = trip.status,
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
            Text(trip.title, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 21.sp)
            Text(
                text = trip.dates,
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
                    append("Маршрут заполнен на ${trip.progress}%")
                    pop()
                    if (trip.cities.isNotBlank()) append(" · ${trip.cities}")
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
            text = "Новое путешествие",
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 16.sp,
            modifier = Modifier.padding(top = 10.dp),
        )
        Text(
            text = "С нуля или из шаблона",
            color = OdysseySubtext,
            fontFamily = Manrope,
            fontWeight = FontWeight.W500,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
