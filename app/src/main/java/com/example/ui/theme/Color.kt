package com.example.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// ============================================================
// UmarOS "Bright" light theme — single source of truth.
// Flip the values here to restyle the whole app. Composables
// should reference these tokens, not raw hex.
// ============================================================

// --- Surfaces ---
val CanvasBg = Color(0xFFF7F5F1)        // warm off-white app background
val LayerCard = Color(0xFFFFFFFF)       // card surface (uses soft shadow, not border)
val BorderHighlight = Color(0x00000000) // no card border in light mode
val DividerColor = Color(0xFFF1ECE3)    // row dividers inside cards
val ChipBg = Color(0xFFEFEAE1)          // empty progress track / inactive fill

// --- Text ---
val PrimaryText = Color(0xFF1C1B1A)     // headings / primary text
val MutedText = Color(0xFF9A958C)       // secondary text
val TertiaryText = Color(0xFFB9B3A8)    // disabled / zero-state text

// --- Semantic ---
val NegativeRed = Color(0xFFD8607A)     // expenses / sleep debt
val PositiveGreen = Color(0xFF3E9E6A)   // income

// --- Accent (the tunable knob) — "Sunset" ---
val Accent = Color(0xFFE1477E)
val AccentEnd = Color(0xFFF7A34B)
val AccentGradient = Brush.linearGradient(listOf(Accent, AccentEnd))

// ============================================================
// Back-compat aliases. Existing code references these names;
// they now point at the Sunset accent so the gradient brush
// (InstaGradient) and old brand usages restyle automatically.
// ============================================================
val InstaPurple = Accent
val InstaRed = Accent
val InstaOrange = AccentEnd
val BrandAccent = Accent

// Material role holders (referenced by Theme.kt)
val Purple80 = AccentEnd
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)
val Purple40 = Accent
val PurpleGrey40 = LayerCard
val Pink40 = Accent
