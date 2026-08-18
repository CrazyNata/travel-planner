package com.odyssey.travelplanner.ui.theme

import androidx.compose.foundation.background
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.PlatformTextStyle
import com.odyssey.travelplanner.R

internal val OdysseyPurple = Color(0xFF6C5CE7)
internal val OdysseyBackground = Color(0xFFF4F4F7)
internal val OdysseySurface = Color(0xFFFFFFFF)
internal val OdysseySurface2 = Color(0xFFF5F5F8)
internal val OdysseyTrack = Color(0xFFEEEEF2)
internal val OdysseyText = Color(0xFF141419)
internal val OdysseyLabel = Color(0xFF3A3A42)
internal val OdysseySubtext = Color(0xFF8A8A95)
internal val OdysseyBorder = Color(0xFFE6E6EC)
internal val OdysseyTint = Color(0xFFF1EEFE)
internal val OdysseyDarkPrimary = Color(0xFFA79BFF)
internal val OdysseyDarkOnPrimary = Color(0xFF18152D)
internal val OdysseyDarkBackground = Color(0xFF141416)
internal val OdysseyDarkSurface = Color(0xFF222531)
internal val OdysseyDarkSurface2 = Color(0xFF303443)
internal val OdysseyDarkTrack = Color(0xFF3A3E4B)
internal val OdysseyDarkText = Color(0xFFF7F8FC)
internal val OdysseyDarkLabel = Color(0xFFF7F8FC)
internal val OdysseyDarkSubtext = Color(0xFFD9DBE6)
internal val OdysseyDarkBorder = Color(0xFF697084)
internal val OdysseyDarkTint = Color(0xFF332F50)
internal val OdysseyDarkMuted = Color(0xFFA8ADBC)

// Semantic tokens for colours that were repeated as raw literals across the UI.
// Values are unchanged; these names only remove the duplication.
internal val OdysseyError = Color(0xFFE0524B)
internal val OdysseySheetScrim = Color(0x730F0F19)
internal val OdysseyPurpleShadow = Color(0x4D6C5CE7)
internal val OdysseyPurpleGradientEnd = Color(0xFF7D6CF0)
internal val OdysseySuccess = Color(0xFF22B07D)
internal val OdysseyDangerBright = Color(0xFFFF6B65)
internal val OdysseyDanger = Color(0xFFE85B56)
internal val OdysseyDangerDeep = Color(0xFFD9534F)
internal val OdysseyScrimStrong = Color(0x990F0F19)
internal val OdysseyShadowFaint = Color(0x0F141428)
internal val OdysseyWarning = Color(0xFFF5A623)
internal val OdysseyScrimHeavy = Color(0xAA0F0F19)
internal val OdysseyWarningDeep = Color(0xFFE29B32)
internal val OdysseyScrimSoft = Color(0x8C141419)
internal val OdysseyPurpleShadowSoft = Color(0x476C5CE7)
internal val OdysseyShadowBlack = Color(0x1A000000)
internal val OdysseyCardShadow = Color(0x19141428)

internal val OdysseyLightColors = lightColorScheme(
    primary = OdysseyPurple,
    onPrimary = Color.White,
    background = OdysseyBackground,
    onBackground = OdysseyText,
    surface = OdysseySurface,
    onSurface = OdysseyText,
    surfaceVariant = OdysseySurface2,
    onSurfaceVariant = OdysseySubtext,
    outline = OdysseyBorder,
    error = OdysseyError,
)
internal val OdysseyDarkColors = darkColorScheme(
    primary = OdysseyDarkPrimary,
    onPrimary = OdysseyDarkOnPrimary,
    background = OdysseyDarkBackground,
    onBackground = OdysseyDarkText,
    surface = OdysseyDarkSurface,
    onSurface = OdysseyDarkText,
    surfaceVariant = OdysseyDarkSurface2,
    onSurfaceVariant = OdysseyDarkSubtext,
    outline = OdysseyDarkBorder,
    error = Color(0xFFFF7B76),
)
internal val Manrope = FontFamily(
    Font(R.font.manrope_regular, FontWeight.W400),
    Font(R.font.manrope_medium, FontWeight.W500),
    Font(R.font.manrope_semibold, FontWeight.W600),
    Font(R.font.manrope_bold, FontWeight.W700),
    Font(R.font.manrope_extrabold, FontWeight.W800),
)
internal val OdysseyNoFontPadding = PlatformTextStyle(includeFontPadding = false)
internal val OdysseyFontPadding = PlatformTextStyle(includeFontPadding = true)

// The text style used by almost every Text in the app: font padding off, so the
// glyph box matches the design's line boxes.
internal val OdysseyTightText = androidx.compose.ui.text.TextStyle(platformStyle = OdysseyNoFontPadding)
internal val LocalDarkTheme = staticCompositionLocalOf { false }
internal val LocalLanguage = staticCompositionLocalOf { "RU" }

