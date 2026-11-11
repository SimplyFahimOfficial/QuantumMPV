/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.liquidglass

import android.os.Build
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberCombinedBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.quantummpv.app.preferences.AppearancePreferences
import com.quantummpv.app.preferences.preference.collectAsState
import com.quantummpv.app.ui.theme.AppMotion
import org.koin.compose.koinInject

/**
 * The screen content that floating glass surfaces refract, or null when Liquid Glass is off or
 * unsupported. A glass surface must not be inside the layer it samples, so this is only provided
 * to siblings of the captured content, never to the content itself.
 */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/**
 * The custom wallpaper, recorded by [com.quantummpv.app.ui.theme.AppWallpaperHost] while Liquid Glass is on, or
 * null without one. Pages are transparent over a wallpaper, so glass needs it as well as the page content.
 */
val LocalAppBackgroundBackdrop = staticCompositionLocalOf<Backdrop?> { null }

/** Blur is a RenderEffect, so Liquid Glass needs Android 12; the lens refraction needs Android 13. */
val liquidGlassSupported: Boolean get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S

/** True when the user turned Liquid Glass on and this device can render it. */
@Composable
fun liquidGlassEnabled(): Boolean {
  val enabled by koinInject<AppearancePreferences>().liquidGlassEnabled.collectAsState()
  return enabled && liquidGlassSupported
}

/**
 * Records [content] into a layer for glass to sample. Unlike a plain layer it first fills with the
 * theme background, because transparent pages would otherwise let sharp text show through the blur.
 * With a wallpaper the wallpaper is that background instead, so no fill is drawn over it.
 * Returns null when Liquid Glass is off, in which case [Modifier.captureLiquidGlass] does nothing.
 */
@Composable
fun rememberLiquidGlassCapture(): LayerBackdrop? {
  val background = MaterialTheme.colorScheme.background
  val hasWallpaper = LocalAppBackgroundBackdrop.current != null
  val layer =
    rememberLayerBackdrop {
      if (!hasWallpaper) drawRect(background)
      drawContent()
    }
  return layer.takeIf { liquidGlassEnabled() }
}

fun Modifier.captureLiquidGlass(layer: LayerBackdrop?): Modifier = if (layer != null) layerBackdrop(layer) else this

/**
 * Draws the Kyant glass for [shape] over [backdrop]: blur, lens refraction, rim highlight and shadow, with
 * [tintColor] laid over the glass. Shared by the dock surfaces and the mini player so they look alike.
 * Navigation surfaces animate constantly, so [lightweight] gives them a lighter blur and a shallower lens.
 */
@Composable
fun Modifier.liquidGlassSurface(
  backdrop: Backdrop,
  shape: Shape,
  tintColor: Color,
  lightweight: Boolean = false,
): Modifier {
  val settings = rememberLiquidGlassSettings()
  val reduceMotion = AppMotion.shouldReduceMotion()
  val blurRadius = if (lightweight) 8.dp else 12.dp
  val refractionHeight = if (lightweight) 14.dp else 18.dp
  val refractionAmount = if (lightweight) 22.dp else 26.dp
  val highlight =
    remember(settings, reduceMotion) {
      settings.highlight(Highlight.Ambient.copy(alpha = if (reduceMotion) 0.28f else 0.52f))
    }
  val shadow = remember { Shadow(radius = 8.dp, color = Color.Black.copy(alpha = 0.16f)) }
  val innerShadow = remember { InnerShadow(radius = 2.dp, color = Color.White.copy(alpha = 0.14f)) }
  return drawBackdrop(
    backdrop = backdrop,
    shape = { shape },
    effects = {
      liquidGlassEffects(
        settings,
        blurRadius.toPx(),
        refractionHeight.toPx(),
        refractionAmount.toPx(),
        vibrant = true,
        refractionEnabled = !reduceMotion,
      )
    },
    highlight = { highlight },
    shadow = { shadow },
    innerShadow = { innerShadow },
    onDrawSurface = { drawShapeTint(shape, settings.surfaceColor(tintColor)) },
  )
}

/**
 * Makes [backdrop] available to the glass surfaces inside [content] (or removes it when null), with the
 * wallpaper underneath when there is one.
 */
@Composable
fun ProvideLiquidGlassBackdrop(
  backdrop: Backdrop?,
  content: @Composable () -> Unit,
) {
  val wallpaper = LocalAppBackgroundBackdrop.current
  val scene = if (backdrop != null && wallpaper != null) rememberCombinedBackdrop(wallpaper, backdrop) else backdrop
  CompositionLocalProvider(LocalLiquidGlassBackdrop provides scene, content = content)
}
