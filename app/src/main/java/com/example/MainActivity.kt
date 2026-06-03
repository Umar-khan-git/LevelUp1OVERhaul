package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import java.text.SimpleDateFormat
import java.util.Date
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.room.Room
import com.example.data.*
import com.example.ui.DashboardViewModel
import com.example.ui.FinanceTabScreen
import com.example.ui.theme.BorderHighlight
import com.example.ui.theme.CanvasBg
import com.example.ui.theme.LayerCard
import com.example.ui.theme.MutedText
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.InstaPurple
import com.example.ui.theme.InstaRed
import com.example.ui.theme.InstaOrange
import com.example.ui.theme.BrandAccent
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import java.io.File
import java.util.Calendar
import java.util.Locale
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private lateinit var database: AppDatabase
    private lateinit var repository: DashboardRepository
    private lateinit var viewModel: DashboardViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Initialize Room Database
        database = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "levelup_db"
        ).fallbackToDestructiveMigration().build()

        repository = DashboardRepository(database.dashboardDao())

        // ViewModel Factory
        viewModel = ViewModelProvider(this, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return DashboardViewModel(repository) as T
            }
        })[DashboardViewModel::class.java]

        // Schedule daily notification alarm at 11:00 AM
        try {
            NotificationReceiver.scheduleDailyNotification(applicationContext)
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Request notification permission if running on Android 13+ (API 33)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Track app open streak
        val appOpenStreak = updateAppOpenStreak(this)

        setContent {
            MyApplicationTheme {
                val context = LocalContext.current
                val levelUpPrefs = remember { context.getSharedPreferences("levelup_prefs", android.content.Context.MODE_PRIVATE) }
                var showSplash by remember { mutableStateOf(true) }
                var onboardingDone by remember { mutableStateOf(levelUpPrefs.getBoolean("onboarding_done", false)) }

                if (showSplash) {
                    SplashScreen(onFinished = { showSplash = false })
                } else if (!onboardingDone) {
                    OnboardingScreen(
                        onComplete = { name ->
                            levelUpPrefs.edit()
                                .putString("user_name", name)
                                .putBoolean("onboarding_done", true)
                                .apply()
                            onboardingDone = true
                        }
                    )
                } else {
                    MainAppContainer(viewModel = viewModel, appOpenStreak = appOpenStreak)
                }
            }
        }
    }
}

fun updateAppOpenStreak(context: android.content.Context): Int {
    val prefs = context.getSharedPreferences("app_streak_prefs", android.content.Context.MODE_PRIVATE)
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    val todayStr = sdf.format(Date())
    val lastOpenDate = prefs.getString("last_open_date", "") ?: ""
    val currentStreak = prefs.getInt("open_streak", 0)
    val cal = Calendar.getInstance()
    cal.add(Calendar.DAY_OF_YEAR, -1)
    val yesterdayStr = sdf.format(cal.time)
    return when {
        lastOpenDate == todayStr -> currentStreak
        lastOpenDate == yesterdayStr -> {
            val newStreak = currentStreak + 1
            prefs.edit().putString("last_open_date", todayStr).putInt("open_streak", newStreak).apply()
            newStreak
        }
        else -> {
            prefs.edit().putString("last_open_date", todayStr).putInt("open_streak", 1).apply()
            1
        }
    }
}

@Composable
fun SplashScreen(onFinished: () -> Unit) {
    var alpha by remember { mutableStateOf(0f) }
    val animAlpha by animateFloatAsState(
        targetValue = alpha,
        animationSpec = androidx.compose.animation.core.tween(700, easing = EaseInOut),
        label = "splash_alpha"
    )
    var scale by remember { mutableStateOf(0.85f) }
    val animScale by animateFloatAsState(
        targetValue = scale,
        animationSpec = androidx.compose.animation.core.tween(700, easing = EaseInOut),
        label = "splash_scale"
    )

    LaunchedEffect(Unit) {
        alpha = 1f
        scale = 1f
        kotlinx.coroutines.delay(2200)
        onFinished()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A)),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .alpha(animAlpha)
                .scale(animScale)
        ) {
            Text(
                text = "LevelUp",
                style = TextStyle(
                    brush = InstaGradient,
                    fontSize = 52.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-1).sp
                )
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Personal Self-Improvement Hub",
                color = Color(0xFF666666),
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(48.dp))
            CircularProgressIndicator(
                color = InstaOrange,
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp
            )
        }
    }
}

// ============================================
// ONBOARDING SCREEN
// ============================================
@Composable
fun OnboardingScreen(onComplete: (String) -> Unit) {
    val pagerState = rememberPagerState(initialPage = 0) { 3 }
    var userName by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0A0A))
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 120.dp),
            userScrollEnabled = true
        ) { page ->
            when (page) {
                0 -> OnboardPage1()
                1 -> OnboardPage2()
                2 -> OnboardPage3(userName = userName, onNameChange = { userName = it })
            }
        }

        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(3) { idx ->
                    val isActive = idx == pagerState.currentPage
                    Box(
                        modifier = Modifier
                            .size(width = if (isActive) 22.dp else 6.dp, height = 6.dp)
                            .background(
                                if (isActive) InstaPurple else Color(0xFF333333),
                                androidx.compose.foundation.shape.RoundedCornerShape(100)
                            )
                    )
                }
            }
            Spacer(modifier = Modifier.height(20.dp))
            val isLast = pagerState.currentPage == 2
            Button(
                onClick = {
                    if (!isLast) scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    else if (userName.isNotBlank()) onComplete(userName.trim())
                },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isLast && userName.isNotBlank()) InstaPurple else Color(0xFF222222)
                ),
                shape = androidx.compose.foundation.shape.RoundedCornerShape(14.dp)
            ) {
                Text(
                    text = if (isLast) "Get Started →" else "Next →",
                    color = Color.White,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Black
                )
            }
            if (!isLast) {
                TextButton(
                    onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Skip", color = Color(0xFF444444), fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun OnboardPage1() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("⚡", fontSize = 72.sp)
        Spacer(modifier = Modifier.height(20.dp))
        Text(
            text = "LevelUp",
            style = TextStyle(brush = InstaGradient, fontSize = 52.sp, fontWeight = FontWeight.Black, letterSpacing = (-2).sp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Your Self-Improvement Hub",
            color = Color(0xFF777777),
            fontSize = 15.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            "Build habits. Crush goals. Track sleep.\nMaster your finances. All in one place.",
            color = Color(0xFF555555),
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )
    }
}

@Composable
fun OnboardPage2() {
    val features = listOf(
        Triple("🎯", "Habits & Goals", "Daily habit streaks + a point-based goal tracking system"),
        Triple("💰", "Finance Tracker", "Log income, expenses and transfers — see where money goes"),
        Triple("😴", "Sleep Tracker", "Log sleep times and track your weekly sleep quality"),
        Triple("📚", "Learn & Vocab", "Build your personal knowledge and vocabulary library")
    )
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 36.dp),
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "What's inside",
            color = Color.White,
            fontSize = 26.sp,
            fontWeight = FontWeight.Black
        )
        Spacer(modifier = Modifier.height(28.dp))
        features.forEach { (icon, title, desc) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Text(icon, fontSize = 26.sp, modifier = Modifier.padding(end = 14.dp, top = 2.dp))
                Column {
                    Text(title, color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                    Text(desc, color = MutedText, fontSize = 12.sp, lineHeight = 17.sp)
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnboardPage3(userName: String, onNameChange: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Almost there!",
            style = TextStyle(brush = InstaGradient, fontSize = 30.sp, fontWeight = FontWeight.Black)
        )
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            "What should we call you?",
            color = Color(0xFF888888),
            fontSize = 16.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(40.dp))
        OutlinedTextField(
            value = userName,
            onValueChange = onNameChange,
            placeholder = {
                Text(
                    "Enter your name",
                    color = Color(0xFF444444),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            },
            colors = TextFieldDefaults.colors(
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = Color(0xFF1A1A1A),
                unfocusedContainerColor = Color(0xFF141414),
                focusedIndicatorColor = InstaPurple,
                unfocusedIndicatorColor = Color(0xFF333333)
            ),
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            textStyle = TextStyle(
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                color = Color.White
            )
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your name appears in the app header. You can change it any time in settings.",
            color = Color(0xFF444444),
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
            lineHeight = 16.sp
        )
    }
}

// ============================================
// GUIDE TIP COMPONENT
// ============================================
@Composable
fun GuideTip(
    text: String,
    icon: String,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D2B)),
        border = BorderStroke(1.dp, InstaPurple.copy(alpha = 0.5f)),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(icon, fontSize = 22.sp)
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = text,
                color = Color.White,
                fontSize = 13.sp,
                lineHeight = 19.sp,
                modifier = Modifier.weight(1f)
            )
            Spacer(modifier = Modifier.width(6.dp))
            TextButton(onClick = onDismiss) {
                Text("Got it", color = InstaPurple, fontWeight = FontWeight.Bold, fontSize = 12.sp)
            }
        }
    }
}


// Global gradient brush
val InstaGradient = Brush.linearGradient(
    colors = listOf(InstaPurple, InstaRed, InstaOrange)
)

@Composable
fun GradientText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default
) {
    Text(
        text = text,
        modifier = modifier.drawBehind {  },
        style = style.copy(brush = InstaGradient)
    )
}

@Composable
fun MainAppContainer(viewModel: DashboardViewModel, appOpenStreak: Int = 1) {
    val tabIds = listOf("today", "goals", "learning", "sleep", "stats", "finance", "week")
    val pagerState = rememberPagerState(initialPage = 0) { tabIds.size }
    var selectedTab by rememberSaveable { mutableStateOf("today") }

    // Keep pager and bottom nav in sync
    LaunchedEffect(pagerState.currentPage) {
        selectedTab = tabIds[pagerState.currentPage]
    }
    LaunchedEffect(selectedTab) {
        val idx = tabIds.indexOf(selectedTab)
        if (idx >= 0 && pagerState.currentPage != idx) {
            pagerState.animateScrollToPage(idx)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(CanvasBg),
        bottomBar = {
            BottomNavBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(CanvasBg)
                .padding(innerPadding)
        ) {
            AppHeader(viewModel = viewModel, appOpenStreak = appOpenStreak)

            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                userScrollEnabled = true
            ) { page ->
                when (tabIds[page]) {
                    "today" -> TodayTabScreen(viewModel = viewModel)
                    "goals" -> GoalsTabScreen(viewModel = viewModel)
                    "learning" -> LearningTabScreen(viewModel = viewModel)
                    "sleep" -> SleepTabScreen(viewModel = viewModel)
                    "stats" -> ProfileScreen(viewModel = viewModel, appOpenStreak = appOpenStreak)
                    "finance" -> FinanceTabScreen(viewModel = viewModel)
                    "week" -> WeekTabScreen(viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
fun AppHeader(viewModel: DashboardViewModel, appOpenStreak: Int = 1) {

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        val context = LocalContext.current
        val userName = remember {
            context.getSharedPreferences("levelup_prefs", android.content.Context.MODE_PRIVATE)
                .getString("user_name", "Welcome") ?: "Welcome"
        }

        Column {
            Text(
                text = userName.uppercase(),
                style = TextStyle(
                    fontFamily = FontFamily.SansSerif,
                    fontWeight = FontWeight.Black,
                    fontSize = 24.sp,
                    letterSpacing = (-0.5).sp,
                    brush = InstaGradient
                )
            )
            Text(
                text = "Self-Improvement Hub",
                style = TextStyle(
                    color = MutedText,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            )
        }

        // App open streak pill
        Row(
            modifier = Modifier
                .background(LayerCard, shape = RoundedCornerShape(100))
                .border(1.dp, BorderHighlight, shape = RoundedCornerShape(100))
                .padding(horizontal = 14.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(text = "🔥", fontSize = 14.sp)
            Text(
                text = "$appOpenStreak DAY${if (appOpenStreak != 1) "S" else ""}",
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Black
            )
        }
    }
}

@Composable
fun BottomNavBar(
    selectedTab: String,
    onTabSelected: (String) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars),
        color = Color(0xFF111111),
        border = BorderStroke(1.dp, BorderHighlight)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tabItems = listOf(
                TabItem("today", "Today", Icons.Default.Home),
                TabItem("goals", "Goals", Icons.Default.Star),
                TabItem("learning", "Learn", Icons.Default.List),
                TabItem("sleep", "Sleep", Icons.Default.Notifications),
                TabItem("stats", "Profile", Icons.Default.Person),
                TabItem("finance", "Money", Icons.Default.ShoppingCart),
                TabItem("week", "Week", Icons.Default.Refresh)
            )

            tabItems.forEach { item ->
                val isSelected = selectedTab == item.id
                Column(
                    modifier = Modifier
                        .clickable { onTabSelected(item.id) }
                        .weight(1f)
                        .testTag("nav_${item.id}"),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .width(34.dp)
                                .height(6.dp)
                                .background(InstaGradient, shape = RoundedCornerShape(100))
                        )
                    } else {
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    Icon(
                        imageVector = item.icon,
                        contentDescription = item.label,
                        tint = if (isSelected) Color.White else MutedText,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = item.label,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) Color.White else MutedText
                    )
                }
            }
        }
    }
}

data class TabItem(val id: String, val label: String, val icon: androidx.compose.ui.graphics.vector.ImageVector)


// ============================================
// TODAY TAB SCREEN
// ============================================
@Composable
fun TodayTabScreen(viewModel: DashboardViewModel) {
    val context = LocalContext.current
    val _guidePrefsToday = context.getSharedPreferences("levelup_prefs", android.content.Context.MODE_PRIVATE)
    var showGuideToday by remember { mutableStateOf(!_guidePrefsToday.getBoolean("guide_today", false)) }
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val intents by viewModel.intents.collectAsStateWithLifecycle()

    var showAddHabit by rememberSaveable { mutableStateOf(false) }
    var showAddIntent by rememberSaveable { mutableStateOf(false) }
    var newHabitName by rememberSaveable { mutableStateOf("") }
    var newIntentName by rememberSaveable { mutableStateOf("") }
    var editingHabit by remember { mutableStateOf<HabitEntity?>(null) }
    var editingIntent by remember { mutableStateOf<IntentEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showGuideToday) {
            item {
                GuideTip(
                    text = "Tap ✓ to complete a habit. Tap + to add habits and daily intents. Your streak grows every day you stay consistent!",
                    icon = "💡",
                    onDismiss = {
                        _guidePrefsToday.edit().putBoolean("guide_today", true).apply()
                        showGuideToday = false
                    }
                )
            }
        }
        // Section: Daily Habits
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Habits Logs",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                IconButton(onClick = { showAddHabit = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Habit", tint = BrandAccent)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (habits.isEmpty()) {
                        Text(
                            text = "No habits. Add logs above!",
                            color = MutedText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        habits.forEach { habit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleHabit(habit) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (habit.isCompleted) InstaGradient else Brush.linearGradient(
                                                    listOf(Color(0xFF222222), Color(0xFF222222))
                                                )
                                            )
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (habit.isCompleted) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = habit.name,
                                        color = if (habit.isCompleted) MutedText else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        overflow = TextOverflow.Ellipsis,
                                        maxLines = 1
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (habit.streak > 0) {
                                        Text(
                                            text = "🔥 ${habit.streak}",
                                            color = InstaOrange,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(end = 4.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { editingHabit = habit },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Edit,
                                            contentDescription = "Edit",
                                            tint = Color.Gray.copy(alpha = 0.6f),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteHabit(habit.id) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = Color.Red.copy(alpha = 0.6f),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // Section: Daily Intents
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Daily Intent (Non-negotiables)",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 0.5.sp
                )
                IconButton(onClick = { showAddIntent = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Intent", tint = BrandAccent)
                }
            }
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    if (intents.isEmpty()) {
                        Text(
                            text = "No daily intents set.",
                            color = MutedText,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        intents.forEach { intent ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { viewModel.toggleIntent(intent) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(22.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (intent.isCompleted) InstaGradient else Brush.linearGradient(
                                                    listOf(Color(0xFF222222), Color(0xFF222222))
                                                )
                                            )
                                            .padding(4.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (intent.isCompleted) {
                                            Icon(
                                                Icons.Default.Check,
                                                contentDescription = "Completed",
                                                tint = Color.White,
                                                modifier = Modifier.size(12.dp)
                                            )
                                        }
                                    }

                                    Text(
                                        text = intent.name,
                                        color = if (intent.isCompleted) MutedText else Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Medium,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                IconButton(
                                    onClick = { editingIntent = intent },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit",
                                        tint = Color.Gray.copy(alpha = 0.6f),
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                                IconButton(
                                    onClick = { viewModel.deleteIntent(intent.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = Color.Red.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Modal: Add Habit Input Drawer / Dialog
    if (showAddHabit) {
        Dialog(onDismissRequest = { showAddHabit = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Track New Habit",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = newHabitName,
                        onValueChange = { newHabitName = it },
                        placeholder = { Text("e.g. Mass gainer (12 PM)", color = MutedText, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("habit_input_field")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newHabitName.isNotBlank()) {
                                    viewModel.addHabit(newHabitName)
                                    newHabitName = ""
                                    showAddHabit = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f).testTag("save_habit_btn")
                        ) {
                            Text("Save", color = Color.White)
                        }
                        Button(
                            onClick = { showAddHabit = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Habit
    editingHabit?.let { habit ->
        var editName by remember(habit.id) { mutableStateOf(habit.name) }
        Dialog(onDismissRequest = { editingHabit = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Edit Habit", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editName.isNotBlank()) {
                                    viewModel.editHabit(habit, editName.trim())
                                    editingHabit = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) { Text("Save", color = Color.White) }
                        Button(
                            onClick = { editingHabit = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel", color = Color.White) }
                    }
                }
            }
        }
    }

    // Modal: Edit Intent
    editingIntent?.let { intent ->
        var editName by remember(intent.id) { mutableStateOf(intent.name) }
        Dialog(onDismissRequest = { editingIntent = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Edit Intent", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = editName,
                        onValueChange = { editName = it },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White, unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editName.isNotBlank()) {
                                    viewModel.editIntent(intent, editName.trim())
                                    editingIntent = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) { Text("Save", color = Color.White) }
                        Button(
                            onClick = { editingIntent = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) { Text("Cancel", color = Color.White) }
                    }
                }
            }
        }
    }

    // Modal: Add Intent Dialog
    if (showAddIntent) {
        Dialog(onDismissRequest = { showAddIntent = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "Add Daily Intent",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    OutlinedTextField(
                        value = newIntentName,
                        onValueChange = { newIntentName = it },
                        placeholder = { Text("e.g. Read Arabic vocabulary (10 words)", color = MutedText, fontSize = 13.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF2A2A2A),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (newIntentName.isNotBlank()) {
                                    viewModel.addIntent(newIntentName)
                                    newIntentName = ""
                                    showAddIntent = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save", color = Color.White)
                        }
                        Button(
                            onClick = { showAddIntent = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}


// ============================================
// GOALS TAB SCREEN
// ============================================
@Composable
fun GoalsTabScreen(viewModel: DashboardViewModel) {
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val pointLogs by viewModel.pointLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val _guidePrefsGoals = context.getSharedPreferences("levelup_prefs", android.content.Context.MODE_PRIVATE)
    var showGuideGoals by remember { mutableStateOf(!_guidePrefsGoals.getBoolean("guide_goals", false)) }

    var showAddGoal by remember { mutableStateOf(false) }
    var showAddPointsId by remember { mutableStateOf<Long?>(null) }

    var goalName by remember { mutableStateOf("") }
    var goalWhy by remember { mutableStateOf("") }
    var goalStatus by remember { mutableStateOf("ACTIVE") }

    var logActivityName by remember { mutableStateOf("") }
    var logHours by remember { mutableStateOf("") }

    var editingGoal by remember { mutableStateOf<GoalEntity?>(null) }
    var editingLog by remember { mutableStateOf<PointLogEntity?>(null) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showGuideGoals) {
            item {
                GuideTip(
                    text = "Tap + to create a goal with a 'why'. Then tap '+ Log Action' on any goal to log hours — 1 hour = 1 point = 1% progress!",
                    icon = "🎯",
                    onDismiss = {
                        _guidePrefsGoals.edit().putBoolean("guide_goals", true).apply()
                        showGuideGoals = false
                    }
                )
            }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Core Point-System Goals",
                    color = Color.White,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = { showAddGoal = true }) {
                    Icon(Icons.Default.Add, contentDescription = "Add Goal", tint = BrandAccent)
                }
            }
        }

        // Active Goals
        items(goals) { goal ->
            // Calculate progress cumulative logic
            val currentPoints = remember(pointLogs, goal.id) {
                pointLogs.filter { it.goalId == goal.id }.sumOf { it.hours.toDouble() }.toFloat() + goal.bonusPoints
            }
            val progressPercent = remember(currentPoints) {
                minOf(100f, currentPoints)
            }

            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Header (Title, pts counter)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = goal.name,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Black
                            )
                            Text(
                                text = "Why: ${goal.why}",
                                color = MutedText,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            GradientText(
                                text = "${String.format("%.1f", progressPercent)}%",
                                style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black)
                            )
                            Text(
                                text = "${String.format("%.1f", currentPoints)} PTS",
                                color = InstaOrange,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Progress bar
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .background(Color(0xFF222222), shape = RoundedCornerShape(100))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth((progressPercent / 100f).coerceIn(0f, 1f))
                                .background(InstaGradient, shape = RoundedCornerShape(100))
                        )
                    }

                    // Status Capsules & Action Links
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Surface(
                            color = when (goal.status) {
                                "ACTIVE" -> InstaPurple.copy(alpha = 0.2f)
                                "NEXT" -> Color.DarkGray.copy(alpha = 0.4f)
                                else -> InstaOrange.copy(alpha = 0.2f)
                            },
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = goal.status,
                                color = when (goal.status) {
                                    "ACTIVE" -> InstaPurple
                                    "NEXT" -> Color.LightGray
                                    else -> InstaOrange
                                },
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { showAddPointsId = goal.id },
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Text("+ Log Action", color = BrandAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            IconButton(
                                onClick = { editingGoal = goal },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    tint = Color.Gray.copy(alpha = 0.6f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }

                            IconButton(
                                onClick = { viewModel.deleteGoal(goal.id) },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Default.Delete,
                                    contentDescription = "Delete",
                                    tint = Color.Red.copy(alpha = 0.6f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    // Inline Logs display drawer
                    val activeLogs = remember(pointLogs, goal.id) {
                        pointLogs.filter { it.goalId == goal.id }
                    }

                    if (activeLogs.isNotEmpty()) {
                        Divider(color = BorderHighlight, thickness = 0.5.dp, modifier = Modifier.padding(vertical = 4.dp))
                        Text("Action Logs:", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        activeLogs.take(3).forEach { log ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "- ${log.activity}",
                                    color = Color.LightGray,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "+${log.hours} hrs/pts",
                                        color = InstaOrange,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Edit,
                                        contentDescription = "Edit Log",
                                        tint = Color.Gray.copy(0.5f),
                                        modifier = Modifier
                                            .size(13.dp)
                                            .clickable { editingLog = log }
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete Log",
                                        tint = Color.Red.copy(0.4f),
                                        modifier = Modifier
                                            .size(14.dp)
                                            .clickable { viewModel.deletePointLog(log.id) }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Modal: Add Goal
    if (showAddGoal) {
        Dialog(onDismissRequest = { showAddGoal = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Add Custom Goal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = goalName,
                        onValueChange = { goalName = it },
                        placeholder = { Text("e.g. Start YT Gaming Channel", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = goalWhy,
                        onValueChange = { goalWhy = it },
                        placeholder = { Text("Why does this match your life purpose?", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Cycle status
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("ACTIVE", "NEXT", "SOMEDAY").forEach { targetStatus ->
                            val activeVal = goalStatus == targetStatus
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeVal) InstaGradient else Brush.linearGradient(
                                            listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
                                        ),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .clickable { goalStatus = targetStatus }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(targetStatus, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (goalName.isNotBlank()) {
                                    viewModel.addGoal(goalName, goalWhy, goalStatus)
                                    goalName = ""
                                    goalWhy = ""
                                    showAddGoal = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = { showAddGoal = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Modal: Log Points / Action
    if (showAddPointsId != null) {
        Dialog(onDismissRequest = { showAddPointsId = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Log Goal Progress Activity", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = logActivityName,
                        onValueChange = { logActivityName = it },
                        placeholder = { Text("Activity (e.g. Studied Core 1 practice)", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = logHours,
                        onValueChange = { logHours = it },
                        placeholder = { Text("Hours spent (1hr = 1pt = 1% progress)", color = MutedText, fontSize = 13.sp) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                val floatHours = logHours.toFloatOrNull() ?: 0.0f
                                if (logActivityName.isNotBlank() && floatHours > 0f) {
                                    viewModel.addPointsToGoal(showAddPointsId!!, logActivityName, floatHours)
                                    logActivityName = ""
                                    logHours = ""
                                    showAddPointsId = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Log Points")
                        }
                        Button(
                            onClick = { showAddPointsId = null },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Goal
    editingGoal?.let { goal ->
        var editName by remember(goal.id) { mutableStateOf(goal.name) }
        var editWhy by remember(goal.id) { mutableStateOf(goal.why) }
        var editStatus by remember(goal.id) { mutableStateOf(goal.status) }
        Dialog(onDismissRequest = { editingGoal = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Edit Goal", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = editName, onValueChange = { editName = it },
                        placeholder = { Text("Goal name", color = MutedText) },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editWhy, onValueChange = { editWhy = it },
                        placeholder = { Text("Why this goal?", color = MutedText) },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        listOf("ACTIVE", "NEXT", "SOMEDAY").forEach { s ->
                            Box(
                                modifier = Modifier.weight(1f)
                                    .background(if (editStatus == s) InstaGradient else Brush.linearGradient(listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))), shape = RoundedCornerShape(10.dp))
                                    .clickable { editStatus = s }.padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) { Text(s, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Black) }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (editName.isNotBlank()) {
                                    viewModel.editGoal(goal, editName.trim(), editWhy.trim(), editStatus)
                                    editingGoal = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent), modifier = Modifier.weight(1f)
                        ) { Text("Save") }
                        Button(onClick = { editingGoal = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                }
            }
        }
    }

    // Modal: Edit Log
    editingLog?.let { log ->
        var editActivity by remember(log.id) { mutableStateOf(log.activity) }
        var editHours by remember(log.id) { mutableStateOf(log.hours.toString()) }
        Dialog(onDismissRequest = { editingLog = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Edit Log Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(
                        value = editActivity, onValueChange = { editActivity = it },
                        placeholder = { Text("Activity description", color = MutedText) },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = editHours, onValueChange = { editHours = it },
                        placeholder = { Text("Hours", color = MutedText) },
                        colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val hrs = editHours.toFloatOrNull() ?: 0f
                                if (editActivity.isNotBlank() && hrs > 0f) {
                                    viewModel.editPointLog(log, editActivity.trim(), hrs)
                                    editingLog = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent), modifier = Modifier.weight(1f)
                        ) { Text("Save") }
                        Button(onClick = { editingLog = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                }
            }
        }
    }
}


// ============================================
// LEARNING TAB SCREEN (5 SUB-TABS)
// ============================================
@Composable
fun LearningTabScreen(viewModel: DashboardViewModel) {
    val learningItems by viewModel.learningItems.collectAsStateWithLifecycle()
    val wordsList by viewModel.words.collectAsStateWithLifecycle()

    var activeSubTab by remember { mutableStateOf("ALL") } // "ALL", "IT", "LANGUAGES", "VOCABULARY", "COURSES"

    var showAddItem by remember { mutableStateOf(false) }
    var showAddWord by remember { mutableStateOf(false) }

    // Forms
    var itemName by remember { mutableStateOf("") }
    var itemSubtext by remember { mutableStateOf("") }
    var itemCategory by remember { mutableStateOf("IT") }

    var wordOriginal by remember { mutableStateOf("") }
    var wordEnglish by remember { mutableStateOf("") }
    var wordLangCat by remember { mutableStateOf("ARABIC") }

    var editingItem by remember { mutableStateOf<LearningEntity?>(null) }
    var editingWord by remember { mutableStateOf<WordEntity?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        // Sub tabs rows
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            val subTabs = listOf("ALL", "IT", "ARABIC", "JAPANESE", "VOCABULARY", "COURSES")
            subTabs.forEach { tab ->
                val active = tab == activeSubTab
                Surface(
                    color = if (active) BrandAccent.copy(alpha = 0.2f) else LayerCard,
                    border = BorderStroke(1.dp, if (active) BrandAccent else BorderHighlight),
                    shape = RoundedCornerShape(100),
                    modifier = Modifier
                        .clickable { activeSubTab = tab }
                ) {
                    Text(
                        text = tab,
                        color = if (active) Color.White else MutedText,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Black,
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                    )
                }
            }
        }

        // Fast actions row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = { showAddItem = true },
                colors = ButtonDefaults.buttonColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Topic", modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Topic", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            Button(
                onClick = { showAddWord = true },
                colors = ButtonDefaults.buttonColors(containerColor = LayerCard),
                border = BorderStroke(1.dp, BorderHighlight),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add Word", modifier = Modifier.size(16.dp), tint = Color.White)
                Spacer(modifier = Modifier.width(4.dp))
                Text("Add Word", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Grid lists of topics / words
        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Render learning items
            val filteredLearning = learningItems.filter {
                activeSubTab == "ALL" || 
                it.category == activeSubTab ||
                (activeSubTab == "ARABIC" && it.category == "LANGUAGES" && it.name.contains("arabic", ignoreCase = true)) ||
                (activeSubTab == "JAPANESE" && it.category == "LANGUAGES" && it.name.contains("japanese", ignoreCase = true))
            }

            if (filteredLearning.isNotEmpty()) {
                item {
                    Text("Academic & IT Topics", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                items(filteredLearning) { item ->
                    Card(
                        colors = CardDefaults.cardColors(containerColor = LayerCard),
                        shape = RoundedCornerShape(18.dp),
                        border = BorderStroke(1.dp, BorderHighlight),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(item.name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Black)
                                Text(item.subtext, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Medium)
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${item.category} • ${item.status}",
                                    color = InstaOrange,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            IconButton(onClick = { editingItem = item }) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray.copy(0.6f))
                            }
                            IconButton(onClick = { viewModel.deleteLearning(item.id) }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.6f))
                            }
                        }
                    }
                }
            }

            // Render Words lists
            val filteredWords = wordsList.filter {
                activeSubTab == "ALL" ||
                        (activeSubTab == "ARABIC" && it.category == "ARABIC") ||
                        (activeSubTab == "JAPANESE" && it.category == "JAPANESE") ||
                        (activeSubTab == "VOCABULARY" && it.category == "ENGLISH")
            }

            if (filteredWords.isNotEmpty()) {
                val groupedWords = filteredWords.groupBy { it.category }
                val sortedCategories = listOf("ARABIC", "JAPANESE", "ENGLISH")
                
                sortedCategories.forEach { cat ->
                    val list = groupedWords[cat]
                    if (list != null && list.isNotEmpty()) {
                        val sectionTitle = when (cat) {
                            "ARABIC" -> "Arabic Dictionary Logs"
                            "JAPANESE" -> "Japanese Dictionary/Words List"
                            "ENGLISH" -> "Vocabulary & English Logs"
                            else -> "$cat Words"
                        }
                        
                        item {
                            Text(
                                text = sectionTitle,
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(top = 10.dp)
                            )
                        }

                        items(list) { item ->
                            Card(
                                colors = CardDefaults.cardColors(containerColor = LayerCard),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, BorderHighlight),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.word,
                                            color = Color.White,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Black
                                        )
                                        Text(
                                            text = "Meaning: ${item.meaning}",
                                            color = Color.LightGray,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Normal
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(
                                                Icons.Default.PlayArrow,
                                                contentDescription = "Share",
                                                tint = InstaPurple,
                                                modifier = Modifier.size(12.dp)
                                            )
                                            Text(
                                                text = "${item.category} (Tap voice symbol on WhatsApp)",
                                                color = MutedText,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }

                                    IconButton(onClick = { editingWord = item }) {
                                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray.copy(0.6f))
                                    }
                                    IconButton(onClick = { viewModel.deleteWord(item.id) }) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.6f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Topic
    if (showAddItem) {
        Dialog(onDismissRequest = { showAddItem = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Track Academic Topic", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = itemName,
                        onValueChange = { itemName = it },
                        placeholder = { Text("e.g. CompTIA A+ Core 2", color = MutedText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = itemSubtext,
                        onValueChange = { itemSubtext = it },
                        placeholder = { Text("Short focus note", color = MutedText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Pick Category
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("IT", "COURSES", "LANGUAGES").forEach { cat ->
                            val activeVal = itemCategory == cat
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeVal) InstaGradient else Brush.linearGradient(
                                            listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { itemCategory = cat }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(cat, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (itemName.isNotBlank()) {
                                    viewModel.addLearning(itemName, itemSubtext, itemCategory, "ACTIVE")
                                    itemName = ""
                                    itemSubtext = ""
                                    showAddItem = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = { showAddItem = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Modal: Add Word Dialog
    if (showAddWord) {
        Dialog(onDismissRequest = { showAddWord = false }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text("Add Word Entry", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)

                    OutlinedTextField(
                        value = wordOriginal,
                        onValueChange = { wordOriginal = it },
                        placeholder = { Text("Word (e.g. Shukran)", color = MutedText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = wordEnglish,
                        onValueChange = { wordEnglish = it },
                        placeholder = { Text("English meaning", color = MutedText) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Select language
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        listOf("ARABIC", "JAPANESE", "ENGLISH").forEach { lang ->
                            val activeVal = wordLangCat == lang
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .background(
                                        if (activeVal) InstaGradient else Brush.linearGradient(
                                            listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { wordLangCat = lang }
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(lang, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                if (wordOriginal.isNotBlank() && wordEnglish.isNotBlank()) {
                                    viewModel.addWord(wordOriginal, wordEnglish, wordLangCat)
                                    wordOriginal = ""
                                    wordEnglish = ""
                                    showAddWord = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Save")
                        }
                        Button(
                            onClick = { showAddWord = false },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                    }
                }
            }
        }
    }

    // Modal: Edit Topic
    editingItem?.let { item ->
        var eName by remember(item.id) { mutableStateOf(item.name) }
        var eSubtext by remember(item.id) { mutableStateOf(item.subtext) }
        var eCat by remember(item.id) { mutableStateOf(item.category) }
        var eStatus by remember(item.id) { mutableStateOf(item.status) }
        Dialog(onDismissRequest = { editingItem = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Edit Topic", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(value = eName, onValueChange = { eName = it }, placeholder = { Text("Topic name", color = MutedText) }, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = eSubtext, onValueChange = { eSubtext = it }, placeholder = { Text("Focus note", color = MutedText) }, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("IT", "COURSES", "LANGUAGES").forEach { cat ->
                            Box(modifier = Modifier.weight(1f).background(if (eCat == cat) InstaGradient else Brush.linearGradient(listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))), shape = RoundedCornerShape(8.dp)).clickable { eCat = cat }.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(cat, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("ACTIVE", "NEXT", "SOMEDAY").forEach { s ->
                            Box(modifier = Modifier.weight(1f).background(if (eStatus == s) InstaGradient else Brush.linearGradient(listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))), shape = RoundedCornerShape(8.dp)).clickable { eStatus = s }.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(s, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (eName.isNotBlank()) { viewModel.editLearning(item, eName.trim(), eSubtext.trim(), eCat, eStatus); editingItem = null } }, colors = ButtonDefaults.buttonColors(containerColor = BrandAccent), modifier = Modifier.weight(1f)) { Text("Save") }
                        Button(onClick = { editingItem = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                }
            }
        }
    }

    // Modal: Edit Word
    editingWord?.let { word ->
        var eWord by remember(word.id) { mutableStateOf(word.word) }
        var eMeaning by remember(word.id) { mutableStateOf(word.meaning) }
        var eLang by remember(word.id) { mutableStateOf(word.category) }
        Dialog(onDismissRequest = { editingWord = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Edit Word", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    OutlinedTextField(value = eWord, onValueChange = { eWord = it }, placeholder = { Text("Word", color = MutedText) }, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = eMeaning, onValueChange = { eMeaning = it }, placeholder = { Text("English meaning", color = MutedText) }, colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF2A2A2A), unfocusedContainerColor = Color(0xFF1E1E1E)), modifier = Modifier.fillMaxWidth())
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        listOf("ARABIC", "JAPANESE", "ENGLISH").forEach { lang ->
                            Box(modifier = Modifier.weight(1f).background(if (eLang == lang) InstaGradient else Brush.linearGradient(listOf(Color(0xFF2A2A2A), Color(0xFF2A2A2A))), shape = RoundedCornerShape(8.dp)).clickable { eLang = lang }.padding(8.dp), contentAlignment = Alignment.Center) {
                                Text(lang, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                            }
                        }
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { if (eWord.isNotBlank() && eMeaning.isNotBlank()) { viewModel.editWord(word, eWord.trim(), eMeaning.trim(), eLang); editingWord = null } }, colors = ButtonDefaults.buttonColors(containerColor = BrandAccent), modifier = Modifier.weight(1f)) { Text("Save") }
                        Button(onClick = { editingWord = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.weight(1f)) { Text("Cancel") }
                    }
                }
            }
        }
    }
}

// ============================================
// SLEEP TAB SCREEN
// ============================================
@Composable
fun SleepTabScreen(viewModel: DashboardViewModel) {
    val sleepLogs by viewModel.sleepLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val _guidePrefsSleep = context.getSharedPreferences("levelup_prefs", android.content.Context.MODE_PRIVATE)
    var showGuideSleep by remember { mutableStateOf(!_guidePrefsSleep.getBoolean("guide_sleep", false)) }

    var sleptHour by remember { mutableStateOf("01:00") }
    var wakeHour by remember { mutableStateOf("05:30") }
    var editingSleep by remember { mutableStateOf<SleepLogEntity?>(null) }

    // Analytics computation
    val averageSlept = remember(sleepLogs) {
        if (sleepLogs.isEmpty()) 0f else sleepLogs.sumOf { it.hoursSlept.toDouble() }.toFloat() / sleepLogs.size
    }
    val weeklyDeficit = remember(averageSlept) {
        maxOf(0f, (7.0f - averageSlept) * 7f)
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (showGuideSleep) {
            item {
                GuideTip(
                    text = "Enter your sleep and wake times in HH:MM format (e.g. 23:30 and 06:30) then tap 'Log Routine Sleep' to track your night.",
                    icon = "😴",
                    onDismiss = {
                        _guidePrefsSleep.edit().putBoolean("guide_sleep", true).apply()
                        showGuideSleep = false
                    }
                )
            }
        }
        item {
            Text(
                text = "Sleep Routine Calculator",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Stats boxes grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Average Slept Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderHighlight)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("SAVG / NIGHT", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        GradientText(
                            text = "${String.format("%.1f", averageSlept)} hrs",
                            style = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Black)
                        )
                    }
                }

                // Target Slept Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderHighlight)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("MY TARGET", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "7.0 hrs",
                            color = Color.White,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }

                // Weekly Deficit Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = LayerCard),
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, BorderHighlight)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("WEEKLY DEBT", color = MutedText, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                        Text(
                            text = "${String.format("%.1f", weeklyDeficit)} hrs",
                            color = if (weeklyDeficit > 5f) Color.Red else InstaOrange,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }
        }

        // Input Form
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Slept Last Evening", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = sleptHour,
                            onValueChange = { sleptHour = it },
                            label = { Text("Slept At (e.g. 01:00)", fontSize = 10.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF222222),
                                unfocusedContainerColor = Color(0xFF1E1E1E)
                            ),
                            modifier = Modifier.weight(1f)
                        )

                        OutlinedTextField(
                            value = wakeHour,
                            onValueChange = { wakeHour = it },
                            label = { Text("Wake At (e.g. 05:30)", fontSize = 10.sp) },
                            colors = TextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedContainerColor = Color(0xFF222222),
                                unfocusedContainerColor = Color(0xFF1E1E1E)
                            ),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Button(
                        onClick = {
                            val currentDate = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(java.util.Date())
                            viewModel.addSleep(currentDate, sleptHour, wakeHour)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Log Routine Sleep", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Weekly grid (highlighting quality)
        item {
            Text("This Week's Routine Grid (Target: 6.5h+)", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    // Pre-defined 7 days grid representation based on sleep logs
                    val calendar = Calendar.getInstance()
                    val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")

                    for (i in 6 downTo 0) {
                        val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                        val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
                        val loggedObj = sleepLogs.find { it.dateString == dateStr }

                        val isComplete = loggedObj != null
                        val isGood = loggedObj != null && loggedObj.hoursSlept >= 6.5f

                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(dayNames.getOrElse(d.get(Calendar.DAY_OF_WEEK) - 1) { " " }, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)

                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isComplete) {
                                            if (isGood) InstaGradient else Brush.linearGradient(
                                                listOf(Color(0xFFFD1D1D), Color(0xFFFD1D1D))
                                            )
                                        } else {
                                            Brush.linearGradient(listOf(Color(0xFF222222), Color(0xFF222222)))
                                        }
                                    )
                                    .border(1.dp, BorderHighlight, shape = RoundedCornerShape(8.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (isComplete) "${loggedObj!!.hoursSlept}h" else "-",
                                    color = Color.White,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }

        // List of past sleep entries
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Sleep Schedule Logs", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }

        items(sleepLogs.take(10)) { logEntry ->
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(logEntry.dateString, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            "Bed: ${logEntry.sleptAt} • Wake: ${logEntry.wokeUp}",
                            color = MutedText,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${logEntry.hoursSlept} hrs",
                            color = if (logEntry.hoursSlept >= 6.5f) InstaOrange else Color.Red,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Black,
                            modifier = Modifier.padding(end = 6.dp)
                        )

                        IconButton(onClick = { editingSleep = logEntry }) {
                            Icon(Icons.Default.Edit, contentDescription = "Edit", tint = Color.Gray.copy(0.6f))
                        }
                        IconButton(onClick = { viewModel.deleteSleepLog(logEntry.dateString) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red.copy(0.6f))
                        }
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }

    // Modal: Edit Sleep Log
    editingSleep?.let { log ->
        var eSlept by remember(log.id) { mutableStateOf(log.sleptAt) }
        var eWake by remember(log.id) { mutableStateOf(log.wokeUp) }
        Dialog(onDismissRequest = { editingSleep = null }) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E1E)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth().padding(20.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    Text("Edit Sleep Log (${log.dateString})", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = eSlept, onValueChange = { eSlept = it },
                            label = { Text("Slept At (HH:MM)", fontSize = 10.sp) },
                            colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF1E1E1E)),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = eWake, onValueChange = { eWake = it },
                            label = { Text("Wake At (HH:MM)", fontSize = 10.sp) },
                            colors = TextFieldDefaults.colors(focusedTextColor = Color.White, unfocusedTextColor = Color.White, focusedContainerColor = Color(0xFF222222), unfocusedContainerColor = Color(0xFF1E1E1E)),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                if (eSlept.isNotBlank() && eWake.isNotBlank()) {
                                    viewModel.editSleepLog(log, eSlept.trim(), eWake.trim())
                                    editingSleep = null
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandAccent), modifier = Modifier.weight(1f)
                        ) { Text("Save", color = Color.White) }
                        Button(onClick = { editingSleep = null }, colors = ButtonDefaults.buttonColors(containerColor = Color.DarkGray), modifier = Modifier.weight(1f)) { Text("Cancel", color = Color.White) }
                    }
                }
            }
        }
    }
}


// ============================================
// PROFILE TAB SCREEN (RPG CHARACTER STATS)
// ============================================
@Composable
fun ProfileScreen(viewModel: DashboardViewModel, appOpenStreak: Int = 1) {
    val habits by viewModel.habits.collectAsStateWithLifecycle()
    val goals by viewModel.goals.collectAsStateWithLifecycle()
    val pointLogs by viewModel.pointLogs.collectAsStateWithLifecycle()
    val learningItems by viewModel.learningItems.collectAsStateWithLifecycle()
    val words by viewModel.words.collectAsStateWithLifecycle()
    val sleepLogs by viewModel.sleepLogs.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val profilePrefs = context.getSharedPreferences("profile_prefs", android.content.Context.MODE_PRIVATE)
    var photoPath by remember { mutableStateOf(profilePrefs.getString("photo_path", null)) }

    val photoBitmap = remember(photoPath) {
        photoPath?.let { path ->
            try { BitmapFactory.decodeFile(path) } catch (e: Exception) { null }
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        uri?.let {
            val file = File(context.filesDir, "profile_photo.jpg")
            try {
                context.contentResolver.openInputStream(uri)?.use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
                photoPath = file.absolutePath
                profilePrefs.edit().putString("photo_path", file.absolutePath).apply()
            } catch (e: Exception) { /* ignore */ }
        }
    }

    // ---- Score calculations ----
    val totalHabits = habits.size
    val completedHabits = habits.count { it.isCompleted }
    val habitCompletionPct = if (totalHabits > 0) (completedHabits.toFloat() / totalHabits) * 100f else 0f

    val avgGoalPct = remember(goals, pointLogs) {
        if (goals.isEmpty()) 0f
        else goals.map { goal ->
            val pts = pointLogs.filter { it.goalId == goal.id }.sumOf { it.hours.toDouble() }.toFloat() + goal.bonusPoints
            minOf(100f, pts)
        }.average().toFloat()
    }

    val recentLogHours = remember(pointLogs) {
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        pointLogs.filter { it.dateAdded >= weekAgo }.sumOf { it.hours.toDouble() }.toFloat()
    }

    val focusScore = ((avgGoalPct * 0.4f) + (habitCompletionPct * 0.3f) + (minOf(100f, recentLogHours / 7f * 100f) * 0.3f)).toInt().coerceIn(0, 100)

    val avgHabitStreak = if (habits.isEmpty()) 0f else habits.map { it.streak.toFloat() }.average().toFloat()
    val disciplineScore = ((minOf(100f, avgHabitStreak / 30f * 100f) * 0.5f) + (minOf(100f, appOpenStreak / 30f * 100f) * 0.5f)).toInt().coerceIn(0, 100)

    val totalPointHours = remember(pointLogs) { pointLogs.sumOf { it.hours.toDouble() }.toFloat() }
    val knowledgeScore = ((minOf(100f, learningItems.size * 10f) * 0.4f) + (minOf(100f, totalPointHours * 2f) * 0.6f)).toInt().coerceIn(0, 100)

    val langItems = remember(learningItems) { learningItems.filter { it.category in listOf("LANGUAGES", "ARABIC", "JAPANESE") } }
    val languagesScore = ((minOf(100f, words.size * 2f) * 0.6f) + (minOf(100f, langItems.size * 25f) * 0.4f)).toInt().coerceIn(0, 100)

    val recentSleepLogs = remember(sleepLogs) { sleepLogs.take(7) }
    val avgSleep = if (recentSleepLogs.isEmpty()) 0f else recentSleepLogs.map { it.hoursSlept }.average().toFloat()
    val healthScore = ((minOf(100f, avgSleep / 8f * 100f) * 0.6f) + (minOf(100f, recentSleepLogs.size / 7f * 100f) * 0.4f)).toInt().coerceIn(0, 100)

    val goalsWithLogs = remember(goals, pointLogs) { goals.count { goal -> pointLogs.any { it.goalId == goal.id } } }
    val goalTrackingRate = if (goals.isEmpty()) 0f else (goalsWithLogs.toFloat() / goals.size) * 100f
    val consistencyScore = ((minOf(100f, appOpenStreak / 30f * 100f) * 0.3f) + (habitCompletionPct * 0.4f) + (goalTrackingRate * 0.3f)).toInt().coerceIn(0, 100)

    val totalXP = remember(habits, words, pointLogs, goals, learningItems) {
        habits.sumOf { it.streak } * 10 + words.size * 5 + pointLogs.size * 50 +
        goals.count { goal -> pointLogs.any { it.goalId == goal.id } } * 100 + learningItems.size * 25
    }
    val level = (totalXP / 1000) + 1
    val xpInLevel = totalXP % 1000
    val xpProgress = xpInLevel / 1000f

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(CanvasBg),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // ---- Level header ----
        item {
            Column(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .background(
                            brush = Brush.horizontalGradient(listOf(InstaPurple, BrandAccent, InstaOrange)),
                            shape = RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 22.dp, vertical = 6.dp)
                ) {
                    Text("LEVEL  $level", color = Color.White, fontWeight = FontWeight.Black, fontSize = 15.sp, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text("Character Profile", color = MutedText, fontSize = 12.sp, letterSpacing = 1.sp)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text("XP", color = MutedText, fontSize = 10.sp, modifier = Modifier.width(22.dp))
                    Box(
                        modifier = Modifier.weight(1f).height(6.dp).clip(RoundedCornerShape(3.dp)).background(Color.White.copy(alpha = 0.1f))
                    ) {
                        val animXP by animateFloatAsState(targetValue = xpProgress, animationSpec = tween(1000, easing = FastOutSlowInEasing), label = "xp")
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(animXP)
                                .clip(RoundedCornerShape(3.dp))
                                .background(Brush.horizontalGradient(listOf(InstaPurple, BrandAccent, InstaOrange)))
                        )
                    }
                    Text("$xpInLevel/1000", color = MutedText, fontSize = 10.sp, modifier = Modifier.padding(start = 8.dp).width(55.dp))
                }
            }
        }

        // ---- Radial profile layout ----
        item {
            Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
                // Radial web background
                Canvas(modifier = Modifier.matchParentSize()) {
                    val cx = size.width / 2f
                    val cy = size.height / 2f
                    val r = minOf(cx, cy) * 0.88f
                    for (i in 0..5) {
                        val a = Math.toRadians(i * 60.0 - 90.0)
                        drawLine(Color.White.copy(alpha = 0.04f), Offset(cx, cy), Offset((cx + r * cos(a)).toFloat(), (cy + r * sin(a)).toFloat()), 1.dp.toPx())
                    }
                    listOf(0.3f, 0.6f, 0.9f).forEach { f ->
                        drawCircle(Color.White.copy(alpha = 0.03f), r * f, Offset(cx, cy), style = Stroke(1.dp.toPx()))
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Row 1 — Focus
                    ProfileStatNode("Focus", focusScore, Color(0xFFCE93D8), Color(0xFF9B27AF))

                    Spacer(Modifier.height(12.dp))

                    // Row 2 — Discipline · · Knowledge
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        ProfileStatNode("Discipline", disciplineScore, Color(0xFFFFAB91), Color(0xFFFF7043))
                        Spacer(Modifier.width(72.dp))
                        ProfileStatNode("Knowledge", knowledgeScore, Color(0xFF90CAF9), Color(0xFF1E88E5))
                    }

                    Spacer(Modifier.height(10.dp))

                    // Row 3 — Avatar
                    ProfileAvatar(
                        photoBitmap = photoBitmap,
                        level = level,
                        onTap = { photoLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                    )

                    Spacer(Modifier.height(10.dp))

                    // Row 4 — Health · · Languages
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        ProfileStatNode("Health", healthScore, Color(0xFFA5D6A7), Color(0xFF43A047))
                        Spacer(Modifier.width(72.dp))
                        ProfileStatNode("Languages", languagesScore, Color(0xFFF48FB1), Color(0xFFE91E8C))
                    }

                    Spacer(Modifier.height(12.dp))

                    // Row 5 — Consistency
                    ProfileStatNode("Consistency", consistencyScore, Color(0xFF80DEEA), Color(0xFF00ACC1))

                    Spacer(Modifier.height(12.dp))
                }
            }
        }

        // ---- Divider ----
        item {
            HorizontalDivider(modifier = Modifier.padding(horizontal = 20.dp), color = Color.White.copy(alpha = 0.08f))
            Spacer(Modifier.height(16.dp))
            Text(
                "STATS OVERVIEW",
                color = MutedText,
                fontSize = 10.sp,
                letterSpacing = 2.sp,
                modifier = Modifier.padding(horizontal = 20.dp)
            )
            Spacer(Modifier.height(12.dp))
        }

        // ---- Stats grid ----
        item {
            Column(modifier = Modifier.padding(horizontal = 12.dp)) {
                Row(Modifier.fillMaxWidth()) {
                    RpgStatCard("App Streak", "🔥 $appOpenStreak days", InstaPurple, Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    RpgStatCard("Total XP", "⚡ $totalXP", InstaOrange, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    RpgStatCard("Avg Sleep", "😴 ${String.format("%.1f", avgSleep)} h/night", Color(0xFF1E88E5), Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    RpgStatCard("Words Learned", "📚 ${words.size}", BrandAccent, Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    RpgStatCard("Active Goals", "🎯 ${goals.count { it.status == "ACTIVE" }}", Color(0xFF43A047), Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    RpgStatCard("Study Hours", "⏱ ${String.format("%.1f", totalPointHours)} h", Color(0xFF00ACC1), Modifier.weight(1f))
                }
                Spacer(Modifier.height(8.dp))
                Row(Modifier.fillMaxWidth()) {
                    RpgStatCard("Habit Rate", "✔ ${habitCompletionPct.toInt()}%", Color(0xFFFF7043), Modifier.weight(1f))
                    Spacer(Modifier.width(8.dp))
                    RpgStatCard("Level", "🏆 $level", InstaPurple, Modifier.weight(1f))
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
fun ProfileStatNode(label: String, score: Int, accentLight: Color, accentDark: Color, modifier: Modifier = Modifier) {
    val animScore by animateFloatAsState(
        targetValue = score.toFloat(),
        animationSpec = tween(durationMillis = 1200, easing = FastOutSlowInEasing),
        label = "stat_$label"
    )
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .width(82.dp)
            .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
            .border(1.dp, accentLight.copy(alpha = 0.28f), RoundedCornerShape(14.dp))
            .padding(horizontal = 6.dp, vertical = 10.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(50.dp)) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val sw = 5.dp.toPx()
                drawArc(accentLight.copy(alpha = 0.2f), -90f, 360f, false, style = Stroke(sw, cap = StrokeCap.Round))
                if (animScore > 0f) {
                    drawArc(accentDark, -90f, animScore * 3.6f, false, style = Stroke(sw, cap = StrokeCap.Round))
                }
            }
            Text(animScore.toInt().toString(), color = accentLight, fontWeight = FontWeight.ExtraBold, fontSize = 14.sp)
        }
        Spacer(Modifier.height(5.dp))
        Text(label, color = Color.White.copy(alpha = 0.82f), fontSize = 9.5.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center, maxLines = 1)
    }
}

@Composable
fun ProfileAvatar(photoBitmap: android.graphics.Bitmap?, level: Int, onTap: () -> Unit) {
    Box(contentAlignment = Alignment.BottomEnd, modifier = Modifier.size(100.dp)) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(96.dp)
                .clip(CircleShape)
                .background(Brush.radialGradient(listOf(InstaPurple.copy(alpha = 0.6f), Color(0xFF1A1A1A))))
                .border(
                    BorderStroke(2.5.dp, Brush.sweepGradient(listOf(InstaPurple, BrandAccent, InstaOrange, InstaPurple))),
                    CircleShape
                )
                .clickable(onClick = onTap)
        ) {
            if (photoBitmap != null) {
                Image(
                    bitmap = photoBitmap.asImageBitmap(),
                    contentDescription = "Profile photo",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("U", color = Color.White, fontSize = 30.sp, fontWeight = FontWeight.Black)
                    Text("TAP", color = Color.White.copy(alpha = 0.45f), fontSize = 8.sp, letterSpacing = 1.sp)
                }
            }
        }
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(26.dp)
                .background(Brush.linearGradient(listOf(InstaPurple, BrandAccent)), CircleShape)
        ) {
            Text("$level", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
        }
    }
}

@Composable
fun RpgStatCard(title: String, value: String, accent: Color, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(12.dp))
            .border(1.dp, accent.copy(alpha = 0.22f), RoundedCornerShape(12.dp))
            .padding(14.dp)
    ) {
        Text(title, color = MutedText, fontSize = 10.sp)
        Spacer(Modifier.height(4.dp))
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    }
}



// ============================================
// WEEK TAB SCREEN
// ============================================
@Composable
fun WeekTabScreen(viewModel: DashboardViewModel) {
    val sleepLogs by viewModel.sleepLogs.collectAsStateWithLifecycle()
    val rEntity by viewModel.currentReflection.collectAsStateWithLifecycle()

    var textReflection by remember { mutableStateOf("") }
    var textIntention by remember { mutableStateOf("") }

    // Sync from database
    LaunchedEffect(rEntity) {
        if (rEntity != null) {
            textReflection = rEntity!!.reflection
            textIntention = rEntity!!.intention
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "Weekly Deep Reflection",
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }

        // Emoji display based on sleeping logs
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Daily Sleeping Quality (Emoji row)", color = MutedText, fontSize = 10.sp, fontWeight = FontWeight.Bold)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        val dayNames = listOf("S", "M", "T", "W", "T", "F", "S")
                        for (i in 6 downTo 0) {
                            val d = Calendar.getInstance().apply { add(Calendar.DAY_OF_YEAR, -i) }
                            val dateStr = java.text.SimpleDateFormat("yyyy-MM-dd", Locale.US).format(d.time)
                            val log = sleepLogs.find { it.dateString == dateStr }

                            val emoji = when {
                                log == null -> "—"
                                log.hoursSlept >= 6.5f -> "😎"
                                log.hoursSlept >= 5.0f -> "😐"
                                else -> "😴"
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(dayNames.getOrElse(d.get(Calendar.DAY_OF_WEEK) - 1) { " " }, color = MutedText, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                Text(emoji, fontSize = 18.sp, modifier = Modifier.padding(top = 4.dp))
                            }
                        }
                    }
                }
            }
        }

        // Reflection Note
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = LayerCard),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BorderHighlight),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Weekly Feedback & Review", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = textReflection,
                        onValueChange = { textReflection = it },
                        placeholder = { Text("How was your weekly schedule, productivity, and sleep balance?", color = MutedText, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF222222),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(110.dp)
                    )

                    Text("Next Week Action Steps", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)

                    OutlinedTextField(
                        value = textIntention,
                        onValueChange = { textIntention = it },
                        placeholder = { Text("What single action items will you tackle next week?", color = MutedText, fontSize = 12.sp) },
                        colors = TextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = Color(0xFF222222),
                            unfocusedContainerColor = Color(0xFF1E1E1E)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(80.dp)
                    )

                    Button(
                        onClick = {
                            viewModel.saveReflection("2026-W22", textReflection, textIntention)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Save Reflections", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}
