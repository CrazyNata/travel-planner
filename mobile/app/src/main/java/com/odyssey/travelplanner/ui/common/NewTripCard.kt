package com.odyssey.travelplanner.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBorder
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSurface
import com.odyssey.travelplanner.ui.theme.OdysseyDarkTint
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor

@Composable
internal fun NewTripCard(onClick: () -> Unit) {
    val darkTheme = LocalDarkTheme.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth()
            .drawBehind {
                val stroke = 2.dp.toPx()
                val dash = 7.dp.toPx()
                drawRoundRect(if (darkTheme) OdysseyDarkBorder else Color(0xFFD3D3DB), style = Stroke(width = stroke, pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(floatArrayOf(dash, dash), 0f)), cornerRadius = androidx.compose.ui.geometry.CornerRadius(22.dp.toPx()))
            }
            .clip(RoundedCornerShape(22.dp))
            .background(if (darkTheme) OdysseyDarkSurface.copy(alpha = 0.4f) else Color(0x66FFFFFF))
            .clickable { onClick() }
            .padding(vertical = 34.dp, horizontal = 20.dp),
    ) {
        Text(
            text = "+",
            color = primaryColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 28.sp,
            modifier = Modifier
                .background(if (darkTheme) OdysseyDarkTint else Color(0xFFEFEAFE), RoundedCornerShape(16.dp))
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
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W500,
            fontSize = 13.sp,
            modifier = Modifier.padding(top = 2.dp),
        )
    }
}
