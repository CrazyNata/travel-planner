package com.odyssey.travelplanner.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.Dp
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.labelColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor

@Composable
internal fun OdysseyChevronDown(iconSize: Dp, color: Color? = null) {
    val resolvedColor = color ?: primaryColor()
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.8.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(resolvedColor, point(6f, 9f), point(12f, 15f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, point(12f, 15f), point(18f, 9f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
internal fun OdysseyChevronUp(iconSize: Dp, color: Color? = null) {
    val resolvedColor = color ?: primaryColor()
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.8.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(resolvedColor, point(6f, 15f), point(12f, 9f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, point(12f, 9f), point(18f, 15f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
internal fun OdysseyPlusIcon(iconSize: Dp = 17.dp, color: Color? = null) {
    val resolvedColor = color ?: primaryColor()
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.2.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(resolvedColor, point(12f, 5f), point(12f, 19f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, point(5f, 12f), point(19f, 12f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
internal fun OdysseyLocationIcon(iconSize: Dp = 15.dp, color: Color? = null) {
    val resolvedColor = color ?: contentTextColor()
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
        drawPath(path, resolvedColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(resolvedColor, radius = 3f * sx, center = Offset(12f * sx, 10f * sy), style = Stroke(width = 2.dp.toPx()))
    }
}

@Composable
internal fun OdysseyFilterIcon(iconSize: Dp = 15.dp, color: Color? = null) {
    val resolvedColor = color ?: labelColor()
    Canvas(Modifier.size(iconSize)) {
        val stroke = 2.dp.toPx()
        val sx = size.width / 24f
        val sy = size.height / 24f
        fun point(x: Float, y: Float) = Offset(x * sx, y * sy)
        drawLine(resolvedColor, point(4f, 6f), point(20f, 6f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, point(7f, 12f), point(17f, 12f), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, point(10f, 18f), point(14f, 18f), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
internal fun OdysseyExternalLinkIcon(iconSize: Dp = 17.dp, color: Color? = null, modifier: Modifier = Modifier) {
    val resolvedColor = color ?: primaryColor()
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
        drawPath(frame, resolvedColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawLine(resolvedColor, Offset(15f * sx, 3f * sy), Offset(21f * sx, 3f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, Offset(21f * sx, 3f * sy), Offset(21f * sx, 9f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, Offset(10f * sx, 14f * sy), Offset(21f * sx, 3f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

@Composable
internal fun OdysseyEditIcon(iconSize: Dp = 15.dp, color: Color? = null, modifier: Modifier = Modifier) {
    val resolvedColor = color ?: primaryColor()
    Canvas(modifier.size(iconSize)) {
        val sx = size.width / 24f
        val sy = size.height / 24f
        val stroke = 2.1.dp.toPx()
        drawLine(
            resolvedColor,
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
        drawPath(pencil, resolvedColor, style = Stroke(width = stroke, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
internal fun OdysseyCalendarIcon(iconSize: Dp = 14.dp, color: Color? = null) {
    val resolvedColor = color ?: secondaryTextColor()
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
        drawPath(frame, resolvedColor, style = Stroke(width = stroke, cap = StrokeCap.Round))
        drawLine(resolvedColor, Offset(8f * sx, 2f * sy), Offset(8f * sx, 6f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, Offset(16f * sx, 2f * sy), Offset(16f * sx, 6f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
        drawLine(resolvedColor, Offset(3f * sx, 10f * sy), Offset(21f * sx, 10f * sy), strokeWidth = stroke, cap = StrokeCap.Round)
    }
}

