package com.archimedeprojects.arihna.feature.home

import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val BackdropGold = Color(0xFFC9A43F)
private val BackdropGreen = Color(0xFF567764)
private val BackdropSun = Color(0xFFFFD979)

internal fun Modifier.islamicBackdrop(): Modifier = drawBehind {
    drawRect(
        brush = Brush.radialGradient(
  colors = listOf(
      BackdropSun.copy(alpha = 0.30f),
      BackdropGold.copy(alpha = 0.10f),
      Color.Transparent,
  ),
  center = Offset(size.width * 0.82f, size.height * 0.055f),
  radius = size.width * 0.72f,
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
  colors = listOf(BackdropGreen.copy(alpha = 0.10f), Color.Transparent),
  center = Offset(size.width * 0.12f, size.height * 0.66f),
  radius = size.width * 0.95f,
        ),
    )

    val cell = 86.dp.toPx()
    val outerRadius = 18.dp.toPx()
    val innerRadius = outerRadius * 0.48f
    val lineWidth = 0.72.dp.toPx()
    var row = 0
    var centerY = cell * 0.55f
    while (centerY < size.height + cell) {
        var centerX = if (row % 2 == 0) cell * 0.28f else cell * 0.78f
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
  drawPath(star, BackdropGreen.copy(alpha = 0.075f), style = Stroke(width = lineWidth))
  drawCircle(
      BackdropGold.copy(alpha = 0.075f),
      radius = innerRadius * 0.40f,
      center = Offset(centerX, centerY),
      style = Stroke(width = lineWidth),
  )
  centerX += cell
        }
        centerY += cell * 0.86f
        row += 1
    }

    val archBottom = min(size.height * 0.34f, 270.dp.toPx())
    val archTop = 24.dp.toPx()
    val middle = size.width / 2f
    val left = size.width * 0.06f
    val right = size.width * 0.94f
    val shoulder = 84.dp.toPx()
    val arch = Path().apply {
        moveTo(left, archBottom)
        cubicTo(left, archBottom * 0.50f, middle - shoulder, archTop + shoulder, middle, archTop)
        cubicTo(middle + shoulder, archTop + shoulder, right, archBottom * 0.50f, right, archBottom)
    }
    drawPath(arch, BackdropGold.copy(alpha = 0.17f), style = Stroke(width = 1.15.dp.toPx()))

    val skyline = BackdropGreen.copy(alpha = 0.055f)
    val horizon = min(size.height * 0.22f, 178.dp.toPx())
    val minaretX = size.width * 0.79f
    drawRect(
        color = skyline,
        topLeft = Offset(minaretX, horizon - 64.dp.toPx()),
        size = Size(8.dp.toPx(), 64.dp.toPx()),
    )
    drawCircle(
        color = skyline,
        radius = 6.dp.toPx(),
        center = Offset(minaretX + 4.dp.toPx(), horizon - 66.dp.toPx()),
    )
    val domeCenter = Offset(size.width * 0.69f, horizon - 18.dp.toPx())
    drawCircle(color = skyline, radius = 22.dp.toPx(), center = domeCenter)
    drawRect(
        color = skyline,
        topLeft = Offset(domeCenter.x - 24.dp.toPx(), domeCenter.y),
        size = Size(48.dp.toPx(), 22.dp.toPx()),
    )
    drawLine(
        color = BackdropGold.copy(alpha = 0.12f),
        start = Offset(size.width * 0.60f, horizon),
        end = Offset(size.width * 0.93f, horizon),
        strokeWidth = 0.8.dp.toPx(),
    )
}
