package com.odyssey.travelplanner.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.odyssey.travelplanner.data.TripCard
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCityList
import com.odyssey.travelplanner.ui.i18n.localizedTripDateText
import com.odyssey.travelplanner.ui.i18n.localizedTripStatus
import com.odyssey.travelplanner.ui.i18n.localizedTripTitle
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSurface
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
internal fun TripListCard(trip: TripCard, onTripClick: (String) -> Unit, onEdit: () -> Unit) {
    val darkTheme = LocalDarkTheme.current
    val language = LocalLanguage.current
    val isDraft = trip.status.contains("чернов", ignoreCase = true)
    val statusColor = if (isDraft) Color(0xFFE0A34B) else Color(0xFF22B07D)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(10.dp, RoundedCornerShape(22.dp), clip = false, ambientColor = Color(0x20141428), spotColor = Color(0x20141428))
            .clip(RoundedCornerShape(22.dp))
            .background(if (darkTheme) OdysseyDarkSurface else Color.White)
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
            if (trip.canEdit) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.align(Alignment.TopEnd).padding(12.dp).size(36.dp).background(Color(0xF8FFFFFF), RoundedCornerShape(12.dp)).clickable { onEdit() },
                ) {
                    Icon(Icons.Filled.MoreVert, contentDescription = localized("Действия с путешествием", "Trip actions", "Acciones del viaje", "Reiseaktionen"), tint = Color(0xFF46464D), modifier = Modifier.size(20.dp))
                }
            }
        }
        Column(modifier = Modifier.padding(start = 16.dp, top = 15.dp, end = 16.dp, bottom = 17.dp)) {
            Text(localizedTripTitle(trip.title), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 21.sp)
            Text(
                text = localizedTripDateText(trip.dates, language),
                color = secondaryTextColor(),
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
                        .background(Brush.horizontalGradient(listOf(primaryColor(), Color(0xFF8069EE))), RoundedCornerShape(4.dp)),
                )
            }
            Text(
                text = buildAnnotatedString {
                    pushStyle(androidx.compose.ui.text.SpanStyle(fontWeight = FontWeight.W800))
                    append(localized("Маршрут заполнен на ${trip.progress}%", "Route ${trip.progress}% complete", "Ruta completada al ${trip.progress}%", "Route zu ${trip.progress}% abgeschlossen"))
                    pop()
                    if (trip.cities.isNotBlank()) append(" · ${localizedCityList(trip.cities, language)}")
                },
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 11.5.sp,
                modifier = Modifier.padding(top = 9.dp),
            )
        }
    }
}

@Composable
internal fun TripsLoadingCard() {
    Box(
        modifier = Modifier.fillMaxWidth().height(250.dp).clip(RoundedCornerShape(22.dp)).background(cardSurfaceColor()),
    )
}

@Composable
internal fun EmptyStateCard(
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
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp).clip(RoundedCornerShape(16.dp)).background(tintedSurfaceColor())) {
            Icon(icon, contentDescription = null, tint = primaryColor(), modifier = Modifier.size(25.dp))
        }
        Text(title, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 16.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 13.dp))
        Text(body, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 13.sp, lineHeight = 18.sp, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 6.dp))
        if (action != null && onAction != null) {
            Button(onClick = onAction, colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(12.dp), modifier = Modifier.padding(top = 16.dp)) {
                Text(action, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
            }
        }
    }
}

@Composable
internal fun TripOverviewLoading() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxWidth().padding(top = 32.dp),
    ) {
        CircularProgressIndicator(color = primaryColor(), strokeWidth = 3.dp, modifier = Modifier.size(28.dp))
    }
}

