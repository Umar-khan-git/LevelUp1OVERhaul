package com.example.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================
// UmarOS "Bright" light theme — single source of truth.
// Values taken from the UmarOS redesign handoff. Composables
// reference these tokens; raw hex should not appear elsewhere.
// ============================================================

// --- Surfaces ---
val CanvasBg = Color(0xFFF6F4EF)        // warm off-white app background
val LayerCard = Color(0xFFFFFFFF)       // card surface (soft shadow, no border)
val BorderHighlight = Color(0x00000000) // no card border in light mode
val DividerColor = Color(0xFFEAE5DC)    // row dividers inside cards
val ChipBg = Color(0xFFF0ECE4)          // empty progress track / inactive fill
val CardOutline = Color(0xFFDAD4C9)     // subtle outline (empty checkbox etc.)

// --- Text ---
val PrimaryText = Color(0xFF1C1B1A)     // headings / primary text
val SecondaryText = Color(0xFF57534C)   // body text on cards
val MutedText = Color(0xFF9A958C)       // secondary / meta text
val TertiaryText = Color(0xFFB9B3A8)    // disabled / zero-state
val InactiveIcon = Color(0xFFB4AFA6)    // inactive nav icons / muted glyphs

// --- Primary accent (runtime-switchable via ThemeState) ---
enum class AccentTheme(val key: String, val label: String, val color: Color, val gradient: Brush) {
    VIOLET("violet", "Violet", Color(0xFF6D5CE7), Brush.linearGradient(listOf(Color(0xFF6D5CE7), Color(0xFF9C4FDC)))),
    OCEAN("ocean", "Ocean", Color(0xFF3A8DDE), Brush.linearGradient(listOf(Color(0xFF3A8DDE), Color(0xFF5BC8C0)))),
    SUNSET("sunset", "Sunset", Color(0xFFE1477E), Brush.linearGradient(listOf(Color(0xFFE1477E), Color(0xFFF7A34B)))),
    EMERALD("emerald", "Emerald", Color(0xFF2FA36B), Brush.linearGradient(listOf(Color(0xFF2FA36B), Color(0xFF7ED957))));
    companion object { fun fromKey(k: String?): AccentTheme = entries.firstOrNull { it.key == k } ?: VIOLET }
}

object ThemeState { var current by mutableStateOf(AccentTheme.VIOLET) }

val Accent: Color get() = ThemeState.current.color
val AccentGradient: Brush get() = ThemeState.current.gradient
val AccentBright = Color(0xFF7C5CFF)
val AccentPink = Color(0xFFE1477E)
val AccentOrange = Color(0xFFF7A34B)
val AccentEnd = AccentPink // legacy alias

// --- Semantic ---
val PositiveGreen = Color(0xFF3DA35D)   // income / completed / good
val NegativeRed = Color(0xFFE1477E)     // expense / debt / bad
val StreakOrange = Color(0xFFE8722F)    // streak flame
val SleepNavy = Color(0xFF3B4FA0)       // sleep hero / bars
val SleepNavyDark = Color(0xFF2A3A78)
val AccountBlue = Color(0xFF3B7DE0)     // categorical (accounts)
val AccountTeal = Color(0xFF12A594)     // categorical (accounts)

// --- Soft tints (row / tile backgrounds) ---
val GreenTint = Color(0xFFE7F4EC)
val PinkTint = Color(0xFFFBE9F0)
val OrangeTint = Color(0xFFFDEEDD)
val VioletTint = Color(0xFFEEEBFC)
val BlueTint = Color(0xFFEAF0FC)

// ============================================================
// Back-compat aliases. Existing code references these names;
// InstaGradient (= InstaPurple/Red/Orange) becomes the violet
// -> pink -> orange brand gradient automatically.
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
val PurpleGrey40 = LayerCard
val Pink40 = AccentPink
