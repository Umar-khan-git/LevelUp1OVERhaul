package com.example.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = ThemeState.dark,
  dynamicColor: Boolean = false, // Disable dynamic color to keep our accent
  content: @Composable () -> Unit,
) {
  // Built here (not a top-level val) so it re-reads the runtime Accent + dark state.
  val colorScheme = if (darkTheme) {
    darkColorScheme(
      primary = Accent,
      secondary = AccentEnd,
      tertiary = AccentEnd,
      background = CanvasBg,
      surface = LayerCard,
      onBackground = PrimaryText,
      onSurface = PrimaryText,
      primaryContainer = Accent,
      onPrimaryContainer = Color.White
    )
  } else {
    lightColorScheme(
      primary = Accent,
      secondary = AccentEnd,
      tertiary = AccentEnd,
      background = CanvasBg,
      surface = LayerCard,
      onBackground = PrimaryText,
      onSurface = PrimaryText,
      primaryContainer = Accent,
      onPrimaryContainer = Color.White
    )
  }

  // Status-bar icons: dark glyphs on the light theme, light glyphs on dark.
  val view = LocalView.current
  if (!view.isInEditMode) {
    SideEffect {
      val window = (view.context as? Activity)?.window
      if (window != null) {
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
      }
    }
  }

  MaterialTheme(
    colorScheme = colorScheme,
    typography = Typography,
    content = content
  )
}
