package com.example

import android.content.Context
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.BrandAccent
import com.example.ui.theme.InstaOrange
import com.example.ui.theme.InstaPurple
import com.example.ui.theme.LayerCard
import com.example.ui.theme.MutedText
import org.json.JSONArray
import org.json.JSONObject
import kotlin.math.cos
import kotlin.math.sin

// ============================================================
// DATA MODELS  (all persisted in SharedPreferences — no DB)
// ============================================================
data class RoadmapStep(val title: String, val done: Boolean)
data class Roadmap(val id: String, val name: String, val icon: String, val steps: List<RoadmapStep>)
data class RoadmapTemplate(val name: String, val icon: String, val steps: List<String>)
data class Achievement(val id: String, val title: String, val icon: String, val desc: String, val unlocked: Boolean, val color: Color)
data class DailyQuest(val id: String, val title: String, val xp: Int, val done: Boolean)

// ============================================================
// ROADMAP STORE  (JSON in SharedPreferences)
// ============================================================
object RoadmapStore {
    private const val PREFS = "levelup_rpg"
    private const val KEY = "roadmaps"

    fun load(context: Context): List<Roadmap> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                val stepsArr = o.getJSONArray("steps")
                Roadmap(
                    id = o.getString("id"),
                    name = o.getString("name"),
                    icon = o.optString("icon", "🎯"),
                    steps = (0 until stepsArr.length()).map { j ->
                        val s = stepsArr.getJSONObject(j)
                        RoadmapStep(s.getString("title"), s.getBoolean("done"))
                    }
                )
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun save(context: Context, roadmaps: List<Roadmap>) {
        val arr = JSONArray()
        roadmaps.forEach { r ->
            val o = JSONObject()
            o.put("id", r.id); o.put("name", r.name); o.put("icon", r.icon)
            val steps = JSONArray()
            r.steps.forEach { st ->
                val so = JSONObject(); so.put("title", st.title); so.put("done", st.done); steps.put(so)
            }
            o.put("steps", steps)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, arr.toString()).apply()
    }
}

val roadmapTemplates = listOf(
    RoadmapTemplate("Certification / Career", "🎓", listOf(
        "Choose your certification or target role",
        "Finish the study course / material",
        "Pass 3 practice exams",
        "Pass the real exam",
        "Add it to your resume & LinkedIn",
        "Start the next milestone"
    )),
    RoadmapTemplate("Get Fit", "💪", listOf(
        "Work out 3× in one week",
        "Hit a 7-day streak",
        "Complete 20 total workouts",
        "Hit a 30-day streak",
        "Reach your target"
    )),
    RoadmapTemplate("Save Money", "💰", listOf(
        "Set a monthly budget",
        "Save your first DH 1,000",
        "Build a 1-month safety net",
        "Save DH 10,000",
        "Start investing"
    )),
    RoadmapTemplate("Learn a Language", "🌍", listOf(
        "Learn 50 words",
        "Hold a basic conversation",
        "Learn 200 words",
        "Watch a show without subtitles",
        "Reach conversational fluency"
    )),
    RoadmapTemplate("Build a Habit", "🌱", listOf(
        "Do it 3 days in a row",
        "Hit a 1-week streak",
        "Hit a 2-week streak",
        "Hit a 30-day streak",
        "Make it automatic"
    )),
    RoadmapTemplate("Custom", "✨", emptyList())
)

// ============================================================
// DERIVED RPG STATE
// ============================================================
fun titleForLevel(level: Int): String = when {
    level >= 50 -> "The Legend"
    level >= 30 -> "The Master"
    level >= 20 -> "The Architect"
    level >= 15 -> "The Builder"
    level >= 10 -> "The Achiever"
    level >= 5 -> "The Riser"
    level >= 2 -> "The Starter"
    else -> "Newcomer"
}

fun computeAchievements(
    streak: Int, habitMaxStreak: Int, words: Int, learningCount: Int,
    studyHours: Float, bestSleep: Float, milestonesDone: Int,
    goalsTracked: Int, monthSaved: Double
): List<Achievement> = listOf(
    Achievement("first", "First Step", "👣", "Open the app", true, InstaPurple),
    Achievement("week", "Week Warrior", "🔥", "7-day streak", streak >= 7, InstaOrange),
    Achievement("century", "Centurion", "💯", "100-day streak", streak >= 100, Color(0xFFFFD54F)),
    Achievement("words", "Wordsmith", "📖", "Learn 25 words", words >= 25, Color(0xFF42A5F5)),
    Achievement("scholar", "Scholar", "🎓", "20 study hours", studyHours >= 20f, Color(0xFF26C6DA)),
    Achievement("rested", "Well Rested", "😴", "Sleep 7.5h+", bestSleep >= 7.5f, Color(0xFF66BB6A)),
    Achievement("habit", "Habit Hero", "⚡", "14-day habit streak", habitMaxStreak >= 14, Color(0xFFFF7043)),
    Achievement("goal", "Goal Getter", "🎯", "Track a goal", goalsTracked >= 1, Color(0xFFAB47BC)),
    Achievement("trail", "Trailblazer", "🧭", "Finish 5 milestones", milestonesDone >= 5, Color(0xFFEC407A)),
    Achievement("saver", "Big Saver", "💰", "Save money this month", monthSaved > 0, Color(0xFF66BB6A))
)

fun computeDailyQuests(
    habitDoneToday: Boolean, sleptToday: Boolean, txToday: Boolean, goalActionToday: Boolean
): List<DailyQuest> = listOf(
    DailyQuest("habit", "Complete a habit", 20, habitDoneToday),
    DailyQuest("sleep", "Log your sleep", 20, sleptToday),
    DailyQuest("money", "Track a transaction", 15, txToday),
    DailyQuest("goal", "Log a goal action", 25, goalActionToday)
)

// ============================================================
// SECTION HEADER
// ============================================================
@Composable
fun RpgSectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        color = MutedText,
        fontSize = 10.sp,
        letterSpacing = 2.sp,
        fontWeight = FontWeight.Bold,
        modifier = modifier
    )
}

// ============================================================
// HEXAGON RADAR
// ============================================================
@Composable
fun HexRadar(stats: List<Triple<String, Int, Color>>, modifier: Modifier = Modifier) {
    val anim = remember { Animatable(0f) }
    LaunchedEffect(Unit) { anim.animateTo(1f, tween(1100, easing = FastOutSlowInEasing)) }

    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val side = minOf(maxWidth, maxHeight)
        val rDp = side * 0.30f

        // ---- The hexagon web + data polygon ----
        Canvas(modifier = Modifier.matchParentSize()) {
            val cx = size.width / 2f
            val cy = size.height / 2f
            val r = rDp.toPx()
            val angles = (0 until 6).map { Math.toRadians(90.0 - it * 60.0) }
            fun pt(frac: Float, ang: Double) =
                Offset(cx + (frac * r * cos(ang)).toFloat(), cy - (frac * r * sin(ang)).toFloat())

            // concentric hexagon rings
            listOf(0.25f, 0.5f, 0.75f, 1f).forEach { f ->
                val rp = angles.map { pt(f, it) }
                for (i in rp.indices) {
                    drawLine(Color.White.copy(alpha = if (f == 1f) 0.16f else 0.05f), rp[i], rp[(i + 1) % 6], strokeWidth = if (f == 1f) 2f else 1f)
                }
            }
            // spokes
            angles.forEach { drawLine(Color.White.copy(alpha = 0.06f), Offset(cx, cy), pt(1f, it), strokeWidth = 1f) }

            // data polygon
            val dataPts = stats.mapIndexed { i, s -> pt((s.second / 100f) * anim.value, angles[i]) }
            val path = androidx.compose.ui.graphics.Path().apply {
                dataPts.forEachIndexed { i, p -> if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y) }
                close()
            }
            drawPath(path, brush = Brush.radialGradient(listOf(InstaPurple.copy(alpha = 0.5f), BrandAccent.copy(alpha = 0.28f)), center = Offset(cx, cy), radius = r))
            drawPath(path, color = InstaPurple, style = Stroke(width = 3f))
            dataPts.forEachIndexed { i, p ->
                drawCircle(stats[i].third.copy(alpha = 0.25f), 10f, p)
                drawCircle(stats[i].third, 5f, p)
                drawCircle(Color.White, 2f, p)
            }
        }

        // ---- Labels pinned to edges (top, bottom + 4 corners) ----
        stats.forEachIndexed { i, s ->
            val boxAlign = when (i) {
                0 -> Alignment.TopCenter      // Focus      (top vertex)
                1 -> Alignment.TopEnd         // Knowledge  (upper-right)
                2 -> Alignment.BottomEnd      // Languages  (lower-right)
                3 -> Alignment.BottomCenter   // Health     (bottom vertex)
                4 -> Alignment.BottomStart    // Consistency(lower-left)
                else -> Alignment.TopStart    // Discipline (upper-left)
            }
            val hAlign = when (i) {
                1, 2 -> Alignment.End
                4, 5 -> Alignment.Start
                else -> Alignment.CenterHorizontally
            }
            Column(
                modifier = Modifier.align(boxAlign),
                horizontalAlignment = hAlign
            ) {
                Text(s.first, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                Text("${s.second}", color = s.third, fontSize = 20.sp, fontWeight = FontWeight.Black)
            }
        }
    }
}

// ============================================================
// HERO MINI-STAT
// ============================================================
@Composable
fun HeroStat(icon: String, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 15.sp)
        Spacer(Modifier.height(2.dp))
        Text(value, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1)
        Text(label, color = MutedText, fontSize = 8.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.5.sp)
    }
}

// ============================================================
// DAILY QUESTS
// ============================================================
@Composable
fun QuestRow(quest: DailyQuest) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(if (quest.done) Brush.linearGradient(listOf(InstaPurple, BrandAccent)) else Brush.linearGradient(listOf(Color(0xFF222222), Color(0xFF222222))))
                .border(1.dp, if (quest.done) Color.Transparent else Color(0xFF3A3A3A), CircleShape)
        ) {
            if (quest.done) Text("✓", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Black)
        }
        Spacer(Modifier.width(12.dp))
        Text(
            quest.title,
            color = if (quest.done) MutedText else Color.White,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            textDecoration = if (quest.done) TextDecoration.LineThrough else TextDecoration.None,
            modifier = Modifier.weight(1f)
        )
        Text(
            "+${quest.xp} XP",
            color = if (quest.done) Color(0xFF66BB6A) else InstaOrange,
            fontSize = 12.sp,
            fontWeight = FontWeight.Black
        )
    }
}

// ============================================================
// ACHIEVEMENT BADGE
// ============================================================
@Composable
fun AchievementBadge(a: Achievement, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier.padding(vertical = 6.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(18.dp))
                .background(
                    if (a.unlocked) Brush.linearGradient(listOf(a.color.copy(alpha = 0.30f), a.color.copy(alpha = 0.12f)))
                    else Brush.linearGradient(listOf(Color(0xFF161616), Color(0xFF161616)))
                )
                .border(1.dp, if (a.unlocked) a.color.copy(alpha = 0.6f) else Color(0xFF2A2A2A), RoundedCornerShape(18.dp))
        ) {
            if (a.unlocked) Text(a.icon, fontSize = 26.sp)
            else Text("🔒", fontSize = 18.sp)
        }
        Spacer(Modifier.height(5.dp))
        Text(
            a.title,
            color = if (a.unlocked) Color.White else MutedText,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            a.desc,
            color = MutedText,
            fontSize = 7.5.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// ============================================================
// JOURNEY TIMELINE
// ============================================================
@Composable
fun JourneyTimeline(currentLevel: Int) {
    val marks = listOf(1, 5, 10, 15, 20, 30, 50)
    Row(
        modifier = Modifier
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        marks.forEachIndexed { i, lvl ->
            val reached = currentLevel >= lvl
            val isCurrent = currentLevel >= lvl && (i == marks.lastIndex || currentLevel < marks[i + 1])
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(96.dp)) {
                Box(modifier = Modifier.fillMaxWidth().height(44.dp), contentAlignment = Alignment.Center) {
                    // connector line
                    if (i != 0) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterStart)
                                .fillMaxWidth(0.5f)
                                .height(2.dp)
                                .background(if (reached) InstaPurple else Color(0xFF2A2A2A))
                        )
                    }
                    if (i != marks.lastIndex) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.CenterEnd)
                                .fillMaxWidth(0.5f)
                                .height(2.dp)
                                .background(if (currentLevel >= marks[i + 1]) InstaPurple else Color(0xFF2A2A2A))
                        )
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(if (isCurrent) 40.dp else 32.dp)
                            .clip(CircleShape)
                            .background(
                                if (reached) Brush.linearGradient(listOf(InstaPurple, BrandAccent))
                                else Brush.linearGradient(listOf(Color(0xFF1A1A1A), Color(0xFF1A1A1A)))
                            )
                            .border(if (isCurrent) 2.dp else 1.dp, if (reached) Color.White.copy(alpha = 0.5f) else Color(0xFF2A2A2A), CircleShape)
                    ) {
                        Text("$lvl", color = if (reached) Color.White else MutedText, fontSize = 12.sp, fontWeight = FontWeight.Black)
                    }
                }
                Spacer(Modifier.height(6.dp))
                Text("LVL $lvl", color = if (reached) Color.White else MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                Text(
                    titleForLevel(lvl),
                    color = if (reached) BrandAccent else MutedText.copy(alpha = 0.6f),
                    fontSize = 8.sp,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// ============================================================
// ROADMAP SECTION (interactive — manages its own dialogs)
// ============================================================
@Composable
fun RoadmapSection(roadmaps: List<Roadmap>, onChange: (List<Roadmap>) -> Unit) {
    var showAdd by remember { mutableStateOf(false) }
    var addStepFor by remember { mutableStateOf<String?>(null) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            RpgSectionLabel("MY ROADMAP")
            Box(
                modifier = Modifier
                    .background(InstaPurple.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .border(1.dp, InstaPurple.copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                    .clickable { showAdd = true }
                    .padding(horizontal = 10.dp, vertical = 5.dp)
            ) {
                Text("+ New Path", color = InstaPurple, fontSize = 11.sp, fontWeight = FontWeight.Black)
            }
        }

        Spacer(Modifier.height(12.dp))

        if (roadmaps.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("🧭", fontSize = 34.sp)
                    Spacer(Modifier.height(8.dp))
                    Text("No roadmap yet", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text(
                        "Set a goal and break it into steps. Earn XP each time you complete one.",
                        color = MutedText, fontSize = 11.sp, textAlign = TextAlign.Center, lineHeight = 16.sp
                    )
                    Spacer(Modifier.height(14.dp))
                    Button(
                        onClick = { showAdd = true },
                        colors = ButtonDefaults.buttonColors(containerColor = InstaPurple),
                        shape = RoundedCornerShape(10.dp)
                    ) { Text("Create your first roadmap", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp) }
                }
            }
        } else {
            roadmaps.forEach { rm ->
                val done = rm.steps.count { it.done }
                val total = rm.steps.size
                val frac = if (total > 0) done.toFloat() / total else 0f
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    border = BorderStroke(1.dp, BorderHighlight),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(rm.icon, fontSize = 22.sp)
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(rm.name, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Black, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                Text("$done / $total steps", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                "🗑",
                                fontSize = 14.sp,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onChange(roadmaps.filter { it.id != rm.id }) }
                                    .padding(6.dp)
                            )
                        }

                        Spacer(Modifier.height(10.dp))
                        // progress bar
                        Box(
                            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(100)).background(Color.White.copy(alpha = 0.08f))
                        ) {
                            val animFrac by animateFloatAsState(frac, tween(700, easing = FastOutSlowInEasing), label = "rm")
                            Box(
                                modifier = Modifier.fillMaxHeight().fillMaxWidth(animFrac).clip(RoundedCornerShape(100))
                                    .background(Brush.horizontalGradient(listOf(InstaPurple, BrandAccent, InstaOrange)))
                            )
                        }

                        Spacer(Modifier.height(12.dp))
                        rm.steps.forEachIndexed { idx, step ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onChange(roadmaps.map { r ->
                                            if (r.id == rm.id) r.copy(steps = r.steps.mapIndexed { i, s -> if (i == idx) s.copy(done = !s.done) else s })
                                            else r
                                        })
                                    }
                                    .padding(vertical = 7.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    contentAlignment = Alignment.Center,
                                    modifier = Modifier.size(22.dp).clip(CircleShape)
                                        .background(if (step.done) Brush.linearGradient(listOf(InstaPurple, BrandAccent)) else Brush.linearGradient(listOf(Color(0xFF222222), Color(0xFF222222))))
                                        .border(1.dp, if (step.done) Color.Transparent else Color(0xFF3A3A3A), CircleShape)
                                ) {
                                    if (step.done) Text("✓", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Black)
                                    else Text("${idx + 1}", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                                Spacer(Modifier.width(12.dp))
                                Text(
                                    step.title,
                                    color = if (step.done) MutedText else Color.White,
                                    fontSize = 13.sp,
                                    textDecoration = if (step.done) TextDecoration.LineThrough else TextDecoration.None,
                                    modifier = Modifier.weight(1f)
                                )
                                if (step.done) Text("+100", color = Color(0xFF66BB6A), fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }

                        Spacer(Modifier.height(6.dp))
                        Text(
                            "+ Add step",
                            color = InstaPurple,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { addStepFor = rm.id }.padding(vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }

    if (showAdd) {
        AddRoadmapDialog(
            onDismiss = { showAdd = false },
            onCreate = { name, icon, steps ->
                val rm = Roadmap(
                    id = System.currentTimeMillis().toString(),
                    name = name, icon = icon,
                    steps = steps.map { RoadmapStep(it, false) }
                )
                onChange(roadmaps + rm)
                showAdd = false
            }
        )
    }

    addStepFor?.let { id ->
        AddStepDialog(
            onDismiss = { addStepFor = null },
            onAdd = { title ->
                onChange(roadmaps.map { if (it.id == id) it.copy(steps = it.steps + RoadmapStep(title, false)) else it })
                addStepFor = null
            }
        )
    }
}

@Composable
fun AddRoadmapDialog(onDismiss: () -> Unit, onCreate: (String, String, List<String>) -> Unit) {
    var name by remember { mutableStateOf("") }
    var icon by remember { mutableStateOf("🎯") }
    var steps by remember { mutableStateOf<List<String>>(emptyList()) }
    var selectedTemplate by remember { mutableStateOf<String?>(null) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("New Roadmap", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Black)
                Text("Pick a starter template (you can edit everything after), or start from scratch.", color = MutedText, fontSize = 11.sp, lineHeight = 15.sp)

                // Template chips
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    roadmapTemplates.chunked(2).forEach { rowItems ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            rowItems.forEach { tpl ->
                                val sel = selectedTemplate == tpl.name
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (sel) InstaPurple.copy(alpha = 0.2f) else Color(0xFF222222))
                                        .border(1.dp, if (sel) InstaPurple else Color(0xFF333333), RoundedCornerShape(10.dp))
                                        .clickable {
                                            selectedTemplate = tpl.name
                                            icon = tpl.icon
                                            steps = tpl.steps
                                            if (tpl.name != "Custom") name = tpl.name
                                        }
                                        .padding(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Text("${tpl.icon}  ${tpl.name}", color = if (sel) Color.White else MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    placeholder = { Text("Roadmap name (e.g. Become a Cloud Engineer)", color = MutedText, fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                if (steps.isNotEmpty()) {
                    Text("${steps.size} starter steps included — edit them after creating.", color = MutedText, fontSize = 10.sp)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (name.isNotBlank()) onCreate(name.trim(), icon, steps) },
                        colors = ButtonDefaults.buttonColors(containerColor = InstaPurple),
                        modifier = Modifier.weight(1f)
                    ) { Text("Create", color = Color.White, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun AddStepDialog(onDismiss: () -> Unit, onAdd: (String) -> Unit) {
    var title by remember { mutableStateOf("") }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Add a Step", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = { Text("e.g. Pass AWS Cloud Practitioner", color = MutedText, fontSize = 12.sp) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { if (title.isNotBlank()) onAdd(title.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = InstaPurple),
                        modifier = Modifier.weight(1f)
                    ) { Text("Add", color = Color.White, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel", color = Color.White) }
                }
            }
        }
    }
}

@Composable
fun EditIdentityDialog(currentTitle: String, currentTagline: String, onDismiss: () -> Unit, onSave: (String, String) -> Unit) {
    var t by remember { mutableStateOf(currentTitle) }
    var tag by remember { mutableStateOf(currentTagline) }
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderHighlight),
            modifier = Modifier.fillMaxWidth().padding(8.dp)
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Edit Your Identity", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Black)
                OutlinedTextField(
                    value = t, onValueChange = { t = it },
                    label = { Text("Title", color = MutedText) },
                    placeholder = { Text("e.g. The Builder", color = MutedText) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    singleLine = true, modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = tag, onValueChange = { tag = it },
                    label = { Text("Tagline", color = MutedText) },
                    placeholder = { Text("e.g. Building the best version of me", color = MutedText) },
                    colors = TextFieldDefaults.colors(
                        focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                        focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF1E1E1E)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { onSave(t.trim(), tag.trim()) },
                        colors = ButtonDefaults.buttonColors(containerColor = InstaPurple),
                        modifier = Modifier.weight(1f)
                    ) { Text("Save", color = Color.White, fontWeight = FontWeight.Bold) }
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                        modifier = Modifier.weight(1f)
                    ) { Text("Cancel", color = Color.White) }
                }
            }
        }
    }
}
