/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.graphics.Bitmap
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.view.PixelCopy
import android.view.Window
import androidx.annotation.StringRes
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animate
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
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ClipOp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.IntSize
import com.quantummpv.app.R
import com.quantummpv.app.preferences.AppearancePreferences
import com.quantummpv.app.preferences.preference.collectAsState
import com.quantummpv.app.ui.theme.DesignTokens.rememberGlassConfig
import org.koin.compose.koinInject
import kotlin.math.hypot
import kotlin.math.roundToInt

/** Dark counterpart of the selected app palette, even while the app itself is using light mode. */
internal val LocalDarkAppColorScheme = staticCompositionLocalOf<ColorScheme?> { null }

/** Selected palette, allowing reusable surfaces to add restrained theme-specific treatment. */
val LocalAppTheme = staticCompositionLocalOf { AppTheme.Default }

private const val THEME_REVEAL_DURATION_MS = 520

/**
 * A circular theme change: the old screen is copied from the window, the new theme is applied under
 * it, and a circle growing from the tap uncovers the new theme. Without a window copy (reduced
 * motion, copy failure) the change is applied at once.
 */
@Stable
class ThemeReveal {
  internal var window: Window? = null
  internal var reduceMotion = false
  private var capturing = false

  // Changes requested while the old frame is still being copied; applied in order once it is.
  private val pending = ArrayList<() -> Unit>()
  internal var snapshot by mutableStateOf<Bitmap?>(null)
  internal var origin by mutableStateOf(Offset.Zero)
  var progress by mutableFloatStateOf(0f)
    internal set
  val isAnimating get() = snapshot != null

  /**
   * Applies [change] (the new theme preference) and plays the reveal when it can; origin is in window
   * pixels. Every change is applied, in request order: one that arrives while a reveal is already
   * running takes effect at once, and one that arrives during the window copy waits for it.
   */
  fun start(
    origin: Offset,
    change: () -> Unit,
  ) {
    if (capturing) {
      pending += change
      return
    }
    if (isAnimating) return change()
    val target = window
    if (reduceMotion || target == null) return change()
    val decor = target.decorView
    val bitmap =
      runCatching { Bitmap.createBitmap(decor.width, decor.height, Bitmap.Config.ARGB_8888) }.getOrNull()
        ?: return change()
    // PixelCopy reads the composited window, so blur, glass and video surfaces are in the copy.
    capturing = true
    runCatching {
      copyWindow(target, bitmap) { result ->
        capturing = false
        if (result == PixelCopy.SUCCESS) {
          progress = 0f
          this.origin = origin
          snapshot = bitmap
        } else {
          bitmap.recycle()
        }
        applyWithPending(change)
      }
    }.onFailure {
      capturing = false
      bitmap.recycle()
      applyWithPending(change)
    }
  }

  /** Seam for tests; [done] is called on the main thread with a [PixelCopy] result code. */
  internal var copyWindow: (Window, Bitmap, (Int) -> Unit) -> Unit = { window, bitmap, done ->
    PixelCopy.request(window, bitmap, { done(it) }, Handler(Looper.getMainLooper()))
  }

  private fun applyWithPending(change: () -> Unit) {
    change()
    while (pending.isNotEmpty()) pending.removeAt(0)()
  }

  internal fun finish() {
    val old = snapshot
    snapshot = null
    // Let Compose stop drawing the bitmap before its pixels are released.
    window?.decorView?.postDelayed({ old?.recycle() }, 96)
  }
}

/** Default is inert, so previews and secondary activities simply apply the change. */
val LocalThemeReveal = staticCompositionLocalOf { ThemeReveal() }

@Composable
fun rememberThemeReveal(): ThemeReveal = remember { ThemeReveal() }

private tailrec fun Context.findWindow(): Window? =
  when (this) {
    is Activity -> window
    is ContextWrapper -> baseContext.findWindow()
    else -> null
  }

@Composable
private fun ThemeRevealOverlay(reveal: ThemeReveal) {
  val bitmap = reveal.snapshot
  LaunchedEffect(bitmap) {
    if (bitmap == null) return@LaunchedEffect
    animate(0f, 1f, animationSpec = tween(THEME_REVEAL_DURATION_MS, easing = FastOutSlowInEasing)) { value, _ ->
      reveal.progress = value
    }
    reveal.finish()
  }
  if (bitmap == null) return
  val image = remember(bitmap) { bitmap.asImageBitmap() }
  Box(
    Modifier
      .fillMaxSize()
      .drawBehind {
        val center = reveal.origin.takeUnless { it == Offset.Zero } ?: Offset(size.width / 2f, size.height / 2f)
        val maxRadius =
          maxOf(
            hypot(center.x, center.y),
            hypot(size.width - center.x, center.y),
            hypot(center.x, size.height - center.y),
            hypot(size.width - center.x, size.height - center.y),
          )
        val hole = Path().apply { addOval(Rect(center, maxRadius * reveal.progress)) }
        clipPath(hole, ClipOp.Difference) {
          drawImage(image, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()))
        }
      }.pointerInput(Unit) {
        // The old frame is only a picture; swallow touches until the reveal ends.
        awaitPointerEventScope {
          while (true) awaitPointerEvent().changes.forEach { it.consume() }
        }
      },
  )
}

// ============================================================================
// Main Theme
// ============================================================================

/**
 * Applies the selected palette, typography, and shapes to [content].
 * Provides spacing, motion policy, glass configuration, and [reveal] to descendants.
 */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun MpvrxTheme(
  reveal: ThemeReveal = rememberThemeReveal(),
  content: @Composable () -> Unit,
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

  val glassConfig = rememberGlassConfig()
  val motionPolicy = rememberMotionPolicy()
  reveal.reduceMotion = motionPolicy.shouldReduceAnimations
  reveal.window = context.findWindow()

  CompositionLocalProvider(
    LocalSpacing provides Spacing(),
    LocalThemeReveal provides reveal,
    LocalMotionPolicy provides motionPolicy,
    LocalReduceMotion provides motionPolicy.shouldReduceAnimations,
    LocalEmphasizedTypography provides AppEmphasizedTypography,
    LocalDarkAppColorScheme provides darkColorScheme,
    LocalAppTheme provides appTheme,
    DesignTokens.LocalGlassConfig provides glassConfig,
  ) {
    Box(modifier = Modifier.fillMaxSize()) {
      MaterialExpressiveTheme(
        colorScheme = targetColorScheme,
        typography = if (useSystemFont || localeNeedsSystemFont) SystemTypography else AppTypography,
        shapes = AppShapes,
        motionScheme = MotionScheme.expressive(),
        // Routes every LocalHapticFeedback call through the app's Haptic feedback setting, the
        // system haptic setting, and one tuned vibration table.
        content = { com.quantummpv.app.ui.utils.ProvideAppHaptics(content) },
      )
      ThemeRevealOverlay(reveal)
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
