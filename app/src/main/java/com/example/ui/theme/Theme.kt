package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme =
  darkColorScheme(
    primary = InstaPurple,
    secondary = InstaRed,
    tertiary = InstaOrange,
    background = CanvasBg,
    surface = LayerCard,
    onBackground = Color.White,
    onSurface = Color.White,
    primaryContainer = BrandAccent,
    onPrimaryContainer = Color.White
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = true, // Force dark theme
  dynamicColor: Boolean = false, // Disable dynamic color to match Instagram pink/orange brand palette
  content: @Composable () -> Unit,
) {
  val colorScheme = DarkColorScheme

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
