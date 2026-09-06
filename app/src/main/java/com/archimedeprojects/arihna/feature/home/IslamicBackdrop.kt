package com.archimedeprojects.arihna.feature.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val BackdropGold = Color(0xFFD8B95A)
private val BackdropGreen = Color(0xFF1D5A43)

internal fun Modifier.islamicBackdrop(): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(
                BackdropGold.copy(alpha = 0.08f),
                BackdropGreen.copy(alpha = 0.025f),
                Color.Transparent,
            ),
            center = Offset(size.width * 0.78f, size.height * 0.08f),
            radius = size.width * 0.9f,
        ),
    )

    val cell = 74.dp.toPx()
    val outerRadius = 19.dp.toPx()
    val innerRadius = outerRadius * 0.47f
    val lineWidth = 0.7.dp.toPx()
    var row = 0
    var centerY = cell * 0.45f
    while (centerY < size.height + cell) {
        var centerX = if (row % 2 == 0) cell * 0.35f else cell * 0.85f
        while (centerX < size.width + cell) {
            val star = Path()
            repeat(16) { index ->
                val angle = -PI / 2.0 + index * PI / 8.0
                val radius = if (index % 2 == 0) outerRadius else innerRadius
                val x = centerX + cos(angle).toFloat() * radius
                val y = centerY + sin(angle).toFloat() * radius
                if (index == 0) star.moveTo(x, y) else star.lineTo(x, y)
            }
            star.close()
            drawPath(
                star,
                BackdropGold.copy(alpha = 0.055f),
                style = Stroke(width = lineWidth),
            )
            drawCircle(
                BackdropGreen.copy(alpha = 0.06f),
                radius = innerRadius * 0.42f,
                center = Offset(centerX, centerY),
                style = Stroke(width = lineWidth),
            )
            centerX += cell
        }
        centerY += cell * 0.82f
        row += 1
    }

    val archBottom = min(size.height * 0.43f, 320.dp.toPx())
    val archTop = 34.dp.toPx()
    val middle = size.width / 2f
    val left = size.width * 0.10f
    val right = size.width * 0.90f
    val shoulder = 76.dp.toPx()
    val arch = Path().apply {
        moveTo(left, archBottom)
        cubicTo(left, archBottom * 0.58f, middle - shoulder, archTop + shoulder, middle, archTop)
        cubicTo(middle + shoulder, archTop + shoulder, right, archBottom * 0.58f, right, archBottom)
    }
    drawPath(
        arch,
        BackdropGold.copy(alpha = 0.085f),
        style = Stroke(width = 1.05.dp.toPx()),
    )
}
