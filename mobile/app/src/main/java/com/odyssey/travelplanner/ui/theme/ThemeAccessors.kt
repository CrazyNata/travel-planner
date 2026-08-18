package com.odyssey.travelplanner.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

@Composable
internal fun primaryColor() = if (LocalDarkTheme.current) OdysseyDarkPrimary else OdysseyPurple

@Composable
internal fun primaryContentColor() = if (LocalDarkTheme.current) OdysseyDarkOnPrimary else Color.White

@Composable
internal fun labelColor() = if (LocalDarkTheme.current) OdysseyDarkLabel else OdysseyLabel

@Composable
internal fun surfaceVariantColor() = if (LocalDarkTheme.current) OdysseyDarkSurface2 else OdysseySurface2

@Composable
internal fun trackColor() = if (LocalDarkTheme.current) OdysseyDarkTrack else OdysseyTrack

@Composable
internal fun contentTextColor() = if (LocalDarkTheme.current) OdysseyDarkText else OdysseyText

@Composable
internal fun secondaryTextColor() = if (LocalDarkTheme.current) OdysseyDarkSubtext else OdysseySubtext

@Composable
internal fun cardSurfaceColor() = if (LocalDarkTheme.current) OdysseyDarkSurface else Color.White

@Composable
internal fun secondarySurfaceColor() = if (LocalDarkTheme.current) OdysseyDarkSurface2 else Color(0xFFF0F0F4)

@Composable
internal fun contentBorderColor() = if (LocalDarkTheme.current) OdysseyDarkBorder else OdysseyBorder

@Composable
internal fun tintedSurfaceColor() = if (LocalDarkTheme.current) OdysseyDarkTint else Color(0xFFFAF9FF)

@Composable
internal fun dangerSurfaceColor() = if (LocalDarkTheme.current) Color(0xFF47282C) else Color(0xFFFFE9E8)

@Composable
internal fun warningSurfaceColor() = if (LocalDarkTheme.current) Color(0xFF443721) else Color(0xFFFDF5E6)

@Composable
internal fun SurfaceEmptyMedia(
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

