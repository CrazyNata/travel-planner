package com.odyssey.travelplanner.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.primaryColor

@Composable
internal fun RamingoSplash(
    message: String? = null,
    onRetry: (() -> Unit)? = null,
) {
    val accentColor = primaryColor()
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
                    drawCircle(accentColor, radius = 4.4.dp.toPx(), center = center)
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
            if (message != null && onRetry != null) {
                Text(
                    text = message,
                    color = Color.White.copy(alpha = 0.82f),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 24.dp),
                )
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color(0xFF4C39B8),
                    ),
                    shape = RoundedCornerShape(13.dp),
                    modifier = Modifier.padding(top = 14.dp),
                ) {
                    Text(
                        text = localized("Повторить", "Try again", "Intentar de nuevo", "Erneut versuchen"),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

