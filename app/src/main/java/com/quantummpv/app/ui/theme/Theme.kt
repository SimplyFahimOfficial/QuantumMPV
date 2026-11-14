/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.theme

import android.content.Context
import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialExpressiveTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MotionScheme
import androidx.compose.material3.RippleConfiguration
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.quantummpv.app.R
import com.quantummpv.app.preferences.AppearancePreferences
import com.quantummpv.app.preferences.preference.collectAsState
import com.quantummpv.app.ui.theme.DesignTokens.rememberGlassConfig
import org.koin.compose.koinInject

/** Dark counterpart of the selected app palette, even while the app itself is using light mode. */
internal val LocalDarkAppColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

/** Selected palette, allowing reusable surfaces to add restrained theme-specific treatment. */
val LocalAppTheme = staticCompositionLocalOf { AppTheme.Default }

private const val THEME_FADE_DURATION_MS = 260

/** Linear blend of every color role the app reads; roles not listed keep [a]'s value. */
internal fun lerpColorScheme(
  a: ColorScheme,
  b: ColorScheme,
  t: Float,
): ColorScheme =
  a.copy(
    primary = lerp(a.primary, b.primary, t),
    onPrimary = lerp(a.onPrimary, b.onPrimary, t),
    primaryContainer = lerp(a.primaryContainer, b.primaryContainer, t),
    onPrimaryContainer = lerp(a.onPrimaryContainer, b.onPrimaryContainer, t),
    inversePrimary = lerp(a.inversePrimary, b.inversePrimary, t),
    secondary = lerp(a.secondary, b.secondary, t),
    onSecondary = lerp(a.onSecondary, b.onSecondary, t),
    secondaryContainer = lerp(a.secondaryContainer, b.secondaryContainer, t),
    onSecondaryContainer = lerp(a.onSecondaryContainer, b.onSecondaryContainer, t),
    tertiary = lerp(a.tertiary, b.tertiary, t),
    onTertiary = lerp(a.onTertiary, b.onTertiary, t),
    tertiaryContainer = lerp(a.tertiaryContainer, b.tertiaryContainer, t),
    onTertiaryContainer = lerp(a.onTertiaryContainer, b.onTertiaryContainer, t),
    background = lerp(a.background, b.background, t),
    onBackground = lerp(a.onBackground, b.onBackground, t),
    surface = lerp(a.surface, b.surface, t),
    onSurface = lerp(a.onSurface, b.onSurface, t),
    surfaceVariant = lerp(a.surfaceVariant, b.surfaceVariant, t),
    onSurfaceVariant = lerp(a.onSurfaceVariant, b.onSurfaceVariant, t),
    surfaceTint = lerp(a.surfaceTint, b.surfaceTint, t),
    inverseSurface = lerp(a.inverseSurface, b.inverseSurface, t),
    inverseOnSurface = lerp(a.inverseOnSurface, b.inverseOnSurface, t),
    error = lerp(a.error, b.error, t),
    onError = lerp(a.onError, b.onError, t),
    errorContainer = lerp(a.errorContainer, b.errorContainer, t),
    onErrorContainer = lerp(a.onErrorContainer, b.onErrorContainer, t),
    outline = lerp(a.outline, b.outline, t),
    outlineVariant = lerp(a.outlineVariant, b.outlineVariant, t),
    scrim = lerp(a.scrim, b.scrim, t),
    surfaceBright = lerp(a.surfaceBright, b.surfaceBright, t),
    surfaceDim = lerp(a.surfaceDim, b.surfaceDim, t),
    surfaceContainer = lerp(a.surfaceContainer, b.surfaceContainer, t),
    surfaceContainerHigh = lerp(a.surfaceContainerHigh, b.surfaceContainerHigh, t),
    surfaceContainerHighest = lerp(a.surfaceContainerHighest, b.surfaceContainerHighest, t),
    surfaceContainerLow = lerp(a.surfaceContainerLow, b.surfaceContainerLow, t),
    surfaceContainerLowest = lerp(a.surfaceContainerLowest, b.surfaceContainerLowest, t),
  )

/**
 * Cross-fades to [target] whenever it changes. A change mid-fade starts from the colors on
 * screen, so a quick second tap never jumps. [target] must be a remembered instance.
 */
@Composable
private fun rememberAnimatedColorScheme(target: ColorScheme): ColorScheme {
  val reduceMotion = LocalMotionPolicy.current.shouldReduceAnimations
  val progress = remember { Animatable(1f) }
  var from by remember { mutableStateOf(target) }
  var to by remember { mutableStateOf(target) }

  LaunchedEffect(target) {
    if (target === to) return@LaunchedEffect
    from = lerpColorScheme(from, to, progress.value)
    to = target
    progress.snapTo(0f)
    if (reduceMotion) {
      progress.snapTo(1f)
    } else {
      progress.animateTo(1f, tween(THEME_FADE_DURATION_MS, easing = FastOutSlowInEasing))
    }
  }
  return if (progress.value >= 1f) to else lerpColorScheme(from, to, progress.value)
}

// ============================================================================
// Main Theme
// ============================================================================

/**
 * Applies the selected palette, typography, and shapes to [content].
 * Provides spacing, motion policy, and glass configuration to descendants.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MpvrxTheme(content: @Composable () -> Unit,
) {
  val preferences = koinInject<AppearancePreferences>()
  val darkMode by preferences.darkMode.collectAsState()
  val amoledMode by preferences.amoledMode.collectAsState()
  val appTheme by preferences.appTheme.collectAsState()
  val useSystemFont by preferences.useSystemFont.collectAsState()
  val darkTheme = isSystemInDarkTheme()
  val configuration = LocalConfiguration.current
  val context = LocalContext.current
  val localeNeedsSystemFont =
    remember(configuration) {
      localeRequiresSystemFont(configuration.locales[0])
    }

  val useDarkTheme =
    when (darkMode) {
      DarkMode.Dark -> true
      DarkMode.Light -> false
      DarkMode.System -> darkTheme
    }

  val darkColorScheme =
    remember(context, configuration, appTheme, amoledMode) {
      resolveAppColorScheme(context, appTheme, useDarkTheme = true, amoledMode = amoledMode)
    }
  val targetColorScheme =
    remember(context, configuration, appTheme, amoledMode, useDarkTheme, darkColorScheme) {
      if (useDarkTheme) {
        darkColorScheme
      } else {
        resolveAppColorScheme(context, appTheme, useDarkTheme = false, amoledMode = amoledMode)
      }
    }
  val colorScheme = rememberAnimatedColorScheme(targetColorScheme)

  val glassConfig = rememberGlassConfig()
  val motionPolicy = rememberMotionPolicy()

  CompositionLocalProvider(
    LocalSpacing provides Spacing(),
    LocalMotionPolicy provides motionPolicy,
    LocalReduceMotion provides motionPolicy.shouldReduceAnimations,
    LocalEmphasizedTypography provides AppEmphasizedTypography,
    LocalDarkAppColorScheme provides darkColorScheme,
    LocalAppTheme provides appTheme,
    DesignTokens.LocalGlassConfig provides glassConfig,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      MaterialExpressiveTheme(
        colorScheme = colorScheme,
        typography = if (useSystemFont || localeNeedsSystemFont) SystemTypography else AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive(),
        // Routes every LocalHapticFeedback call through the app's Haptic feedback setting, the
        // system haptic setting, and one tuned vibration table.
        content = { com.quantummpv.app.ui.utils.ProvideAppHaptics(content) },
      )
    }
  }
}

private fun resolveAppColorScheme(
  context: Context,
  appTheme: AppTheme,
  useDarkTheme: Boolean,
  amoledMode: Boolean,
): ColorScheme =
  when {
    appTheme.isDynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
      when {
        useDarkTheme && amoledMode -> {
          dynamicDarkColorScheme(context).copy(
            background = backgroundPureBlack,
            surface = surfacePureBlack,
            surfaceDim = surfaceDimPureBlack,
            surfaceBright = surfaceBrightPureBlack,
            surfaceContainerLowest = surfaceContainerLowestPureBlack,
            surfaceContainerLow = surfaceContainerLowPureBlack,
            surfaceContainer = surfaceContainerPureBlack,
            surfaceContainerHigh = surfaceContainerHighPureBlack,
            surfaceContainerHighest = surfaceContainerHighestPureBlack,
          )
        }
        useDarkTheme -> dynamicDarkColorScheme(context)
        else -> dynamicLightColorScheme(context).withComfortableLightSurfaces()
      }
    }
    useDarkTheme && amoledMode -> appTheme.getAmoledColorScheme()
    useDarkTheme -> appTheme.getDarkColorScheme()
    else -> appTheme.getLightColorScheme()
  }

/** Keeps wallpaper-derived accents while replacing near-white full-screen surfaces. */
private fun androidx.compose.material3.ColorScheme.withComfortableLightSurfaces(): androidx.compose.material3.ColorScheme {
  val base = Color(0xFFF7F5F8)

  fun tint(amount: Float) =
    androidx.compose.ui.graphics
      .lerp(base, primary, amount)

  return copy(
    background = tint(0.018f),
    surface = tint(0.018f),
    surfaceDim = tint(0.085f),
    surfaceBright = Color(0xFFFBF9FC),
    surfaceContainerLowest = Color(0xFFFBF9FC),
    surfaceContainerLow = tint(0.030f),
    surfaceContainer = tint(0.050f),
    surfaceContainerHigh = tint(0.072f),
    surfaceContainerHighest = tint(0.098f),
  )
}

enum class DarkMode(
  @StringRes val titleRes: Int,
) {
  Dark(R.string.pref_appearance_darkmode_dark),
  Light(R.string.pref_appearance_darkmode_light),
  System(R.string.pref_appearance_darkmode_system),
}

private const val RIPPLE_DRAGGED_ALPHA = .5f
private const val RIPPLE_FOCUSED_ALPHA = .6f
private const val RIPPLE_HOVERED_ALPHA = .4f
private const val RIPPLE_PRESSED_ALPHA = .6f

@OptIn(ExperimentalMaterial3Api::class)
@Suppress("DEPRECATION")
val playerRippleConfiguration
  @Composable get() =
    RippleConfiguration(
      color = MaterialTheme.colorScheme.primaryContainer,
      rippleAlpha =
        RippleAlpha(
          draggedAlpha = RIPPLE_DRAGGED_ALPHA,
          focusedAlpha = RIPPLE_FOCUSED_ALPHA,
          hoveredAlpha = RIPPLE_HOVERED_ALPHA,
          pressedAlpha = RIPPLE_PRESSED_ALPHA,
        ),
    )
