package com.odyssey.travelplanner.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.material.icons.outlined.Image
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.R
import io.github.jan.supabase.auth.auth
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBorder
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSurface
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor

@Composable
internal fun RamingoBrand(modifier: Modifier = Modifier) {
    val darkTheme = LocalDarkTheme.current
    val shape = RoundedCornerShape(15.dp)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .clip(shape)
            .background(if (darkTheme) OdysseyDarkSurface else Color.White)
            .border(1.dp, if (darkTheme) OdysseyDarkBorder else contentBorderColor(), shape)
            .padding(horizontal = 8.dp, vertical = 5.dp),
    ) {
        Image(
            painter = painterResource(R.drawable.ic_ramingo_mark),
            contentDescription = localized("Логотип Ramingo", "Ramingo logo", "Logotipo de Ramingo", "Ramingo-Logo"),
            modifier = Modifier.size(32.dp).clip(RoundedCornerShape(10.dp)),
        )
        Text("Ramingo", color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 17.sp, modifier = Modifier.padding(start = 9.dp))
    }
}

