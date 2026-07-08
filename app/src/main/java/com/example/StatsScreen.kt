package com.example

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.ui.graphics.graphicsLayer
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

/** A single character stat for the hexagon layout. */
data class HexStatNode(val label: String, val value: Int, val icon: ImageVector, val color: Color)

/**
 * Character-sheet hexagon: a center avatar (tappable to set a photo) with six
 * circular stat nodes placed around it, matching the redesign reference.
 */
@Composable
fun CharacterHexNodes(
    stats: List<HexStatNode>,
    centerInitial: String,
    centerBitmap: android.graphics.Bitmap?,
    onAvatarTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier.height(300.dp), contentAlignment = Alignment.Center) {
        val radius = 108.dp
        // faint hexagon web behind the nodes
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val rPx = radius.toPx() * 0.86f
            val web = Path()
            for (i in 0..5) {
                val a = Math.toRadians((-90 + i * 60).toDouble())
                val x = cx + rPx * Math.cos(a).toFloat()
                val y = cy + rPx * Math.sin(a).toFloat()
                if (i == 0) web.moveTo(x, y) else web.lineTo(x, y)
            }
            web.close()
            drawPath(web, color = Color(0x14000000), style = Stroke(width = 1.2.dp.toPx()))
            // spokes
            for (i in 0..5) {
                val a = Math.toRadians((-90 + i * 60).toDouble())
                drawLine(
                    Color(0x0F000000),
                    Offset(cx, cy),
                    Offset(cx + rPx * Math.cos(a).toFloat(), cy + rPx * Math.sin(a).toFloat()),
                    strokeWidth = 1.dp.toPx()
                )
            }
        }
        // six stat nodes
        stats.take(6).forEachIndexed { i, s ->
            val angle = Math.toRadians((-90 + i * 60).toDouble())
            val dx = (radius.value * Math.cos(angle)).dp
            val dy = (radius.value * Math.sin(angle)).dp
            val appear = remember { Animatable(0f) }
            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(i * 70L)
                appear.animateTo(1f, animationSpec = tween(420, easing = FastOutSlowInEasing))
            }
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.align(Alignment.Center).offset(x = dx, y = dy)
                    .graphicsLayer {
                        val sc = 0.7f + 0.3f * appear.value
                        scaleX = sc
                        scaleY = sc
                        alpha = appear.value
                    }
            ) {
                Box(
                    modifier = Modifier.size(54.dp).clip(CircleShape).background(LayerCard)
                        .border(2.dp, s.color, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(s.icon, contentDescription = null, tint = s.color, modifier = Modifier.size(14.dp))
                        Text("${s.value}", color = s.color, fontSize = 15.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(3.dp))
                Text(s.label, color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Medium)
            }
        }
        // center avatar (tap to add/change photo) — gentle pulse
        val pulse = rememberInfiniteTransition(label = "pulse")
        val pulseScale by pulse.animateFloat(
            initialValue = 1f, targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(1600, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "pulseScale"
        )
        Box(
            modifier = Modifier.size(76.dp).graphicsLayer { scaleX = pulseScale; scaleY = pulseScale }.clip(CircleShape).background(AccentGradient)
                .clickable { onAvatarTap() },
            contentAlignment = Alignment.Center
        ) {
            if (centerBitmap != null) {
                androidx.compose.foundation.Image(
                    bitmap = centerBitmap.asImageBitmap(),
                    contentDescription = "Profile photo",
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.matchParentSize().clip(CircleShape)
                )
            } else {
                Text(centerInitial, color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

/** One highlight line: tinted icon chip + label + right-aligned value. */
@Composable
fun HighlightRow(icon: ImageVector, tint: Color, label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier.size(28.dp).background(tint.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
            }
            Text(label, color = SecondaryText, fontSize = 13.sp)
        }
        Text(value, color = PrimaryText, fontSize = 14.sp, fontWeight = FontWeight.Bold)
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
