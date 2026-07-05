package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

/** One stat tile: tinted icon chip + label + big value. Matches the redesign Stats cards. */
@Composable
fun StatCardTile(
    label: String,
    value: String,
    icon: ImageVector,
    tint: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LayerCard),
        border = BorderStroke(1.dp, BorderHighlight),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = modifier
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Box(
                    modifier = Modifier.size(30.dp).background(tint.copy(alpha = 0.15f), RoundedCornerShape(9.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(18.dp))
                }
                Text(label, color = MutedText, fontSize = 12.sp, fontWeight = FontWeight.Medium)
            }
            Text(value, color = PrimaryText, fontSize = 24.sp, fontWeight = FontWeight.Black)
        }
    }
}

/** "XP this week" card with a violet area+line chart and a green weekly delta. */
@Composable
fun XpWeekChart(series: List<Float>, deltaXp: Int) {
    Card(
        colors = CardDefaults.cardColors(containerColor = LayerCard),
        border = BorderStroke(1.dp, BorderHighlight),
        shape = RoundedCornerShape(18.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("XP this week", color = PrimaryText, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Text("+$deltaXp XP", color = PositiveGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(14.dp))
            val safe = if (series.isEmpty()) List(7) { 0f } else series
            Canvas(modifier = Modifier.fillMaxWidth().height(96.dp)) {
                val n = safe.size
                val maxV = (safe.maxOrNull() ?: 1f).coerceAtLeast(1f)
                val stepX = if (n > 1) size.width / (n - 1) else size.width
                val topPad = 6f
                val usableH = size.height - topPad - 4f
                val pts = safe.mapIndexed { i, v ->
                    Offset(i * stepX, topPad + (usableH - (v / maxV) * usableH))
                }
                // Area fill under the line
                val area = Path().apply {
                    moveTo(pts.first().x, size.height)
                    pts.forEach { lineTo(it.x, it.y) }
                    lineTo(pts.last().x, size.height)
                    close()
                }
                drawPath(area, Brush.verticalGradient(listOf(Accent.copy(alpha = 0.25f), Accent.copy(alpha = 0f))))
                // Line
                for (i in 0 until pts.size - 1) {
                    drawLine(Accent, pts[i], pts[i + 1], strokeWidth = 3.dp.toPx(), cap = StrokeCap.Round)
                }
                // Peak marker
                val peakIdx = safe.indices.maxByOrNull { safe[it] } ?: 0
                drawCircle(LayerCard, radius = 6.dp.toPx(), center = pts[peakIdx])
                drawCircle(Accent, radius = 6.dp.toPx(), center = pts[peakIdx], style = Stroke(width = 2.5.dp.toPx()))
            }
            Spacer(Modifier.height(8.dp))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                listOf("M", "T", "W", "T", "F", "S", "S").forEach {
                    Text(it, color = TertiaryText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
