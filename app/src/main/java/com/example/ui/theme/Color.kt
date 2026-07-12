package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================
// LevelUp "Editorial" theme — warm ivory + ink (light) and warm
// near-black (dark), with a single switchable signal accent
// (default Orange). Surface/text tokens are getters that read
// ThemeState (dark + accent), so toggling either re-themes the app.
// ============================================================

enum class AccentTheme(val key: String, val label: String, val color: Color, val gradient: Brush) {
    ORANGE("orange", "Orange", Color(0xFFE8672E), Brush.linearGradient(listOf(Color(0xFFE8672E), Color(0xFFF5934B)))),
    VIOLET("violet", "Violet", Color(0xFF6D5CE7), Brush.linearGradient(listOf(Color(0xFF6D5CE7), Color(0xFF9C4FDC)))),
    OCEAN("ocean", "Ocean", Color(0xFF3A8DDE), Brush.linearGradient(listOf(Color(0xFF3A8DDE), Color(0xFF5BC8C0)))),
    SUNSET("sunset", "Sunset", Color(0xFFE1477E), Brush.linearGradient(listOf(Color(0xFFE1477E), Color(0xFFF7A34B)))),
    EMERALD("emerald", "Emerald", Color(0xFF2FA36B), Brush.linearGradient(listOf(Color(0xFF2FA36B), Color(0xFF7ED957))));
    companion object { fun fromKey(k: String?): AccentTheme = entries.firstOrNull { it.key == k } ?: ORANGE }
}

object ThemeState {
    var current by mutableStateOf(AccentTheme.ORANGE)
    var dark by mutableStateOf(false)
}

private fun pick(light: Long, dark: Long): Color = if (ThemeState.dark) Color(dark) else Color(light)

// --- Surfaces ---
val CanvasBg: Color get() = pick(0xFFF3EFE7, 0xFF16150F)
val LayerCard: Color get() = pick(0xFFFCFAF5, 0xFF211F18)
val BorderHighlight: Color get() = if (ThemeState.dark) Color(0x14FFFFFF) else Color(0x00000000)
val DividerColor: Color get() = pick(0xFFE7E0D3, 0xFF2A2820)
val ChipBg: Color get() = pick(0xFFEFE9DE, 0xFF201D17)
val CardOutline: Color get() = pick(0xFFE0D8C9, 0xFF2E2B22)

// --- Text ---
val PrimaryText: Color get() = pick(0xFF1C1A17, 0xFFF4F0E8)
val SecondaryText: Color get() = pick(0xFF57534C, 0xFFC9C2B6)
val MutedText: Color get() = pick(0xFF8C857A, 0xFF9A9282)
val TertiaryText: Color get() = pick(0xFFB4AC9E, 0xFF6E685C)
val InactiveIcon: Color get() = pick(0xFFB4AC9E, 0xFF6E685C)

// --- Accent (switchable) ---
val Accent: Color get() = ThemeState.current.color
val AccentGradient: Brush get() = ThemeState.current.gradient
val AccentBright = Color(0xFFF5934B)
val AccentPink = Color(0xFFC56B8E)
val AccentOrange = Color(0xFFE8672E)
val AccentEnd = AccentBright

// --- Semantic (muted editorial) ---
val PositiveGreen = Color(0xFF5B8C6E)
val NegativeRed = Color(0xFFC56B52)
val StreakOrange = Color(0xFFE8672E)
val SleepNavy = Color(0xFF2A2F45)
val SleepNavyDark = Color(0xFF161A2A)
val AccountBlue = Color(0xFF5E86C7)
val AccountTeal = Color(0xFF3FA6A0)

// --- Soft tints (dark-aware) ---
val GreenTint: Color get() = pick(0xFFE3EDE6, 0xFF1E2A22)
val PinkTint: Color get() = pick(0xFFF3E4E9, 0xFF2A1E22)
val OrangeTint: Color get() = pick(0xFFF6E7DB, 0xFF2A2017)
val VioletTint: Color get() = pick(0xFFEAE6F2, 0xFF221E2A)
val BlueTint: Color get() = pick(0xFFE6ECF6, 0xFF1A2230)

// ============================================================
// Back-compat aliases (existing code references these names).
// ============================================================
val InstaPurple: Color get() = Accent
val InstaRed = AccentPink
val InstaOrange = AccentOrange
val BrandAccent: Color get() = Accent

// Material role holders (referenced by Theme.kt)
val Purple80 = AccentBright
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40: Color get() = Accent
val PurpleGrey40: Color get() = LayerCard
val Pink40 = AccentPink
