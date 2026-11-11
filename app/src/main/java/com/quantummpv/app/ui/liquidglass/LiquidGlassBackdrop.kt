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
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import com.kyant.backdrop.Backdrop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.quantummpv.app.preferences.AppearancePreferences
import com.quantummpv.app.preferences.preference.collectAsState
import org.koin.compose.koinInject

/**
 * The screen content that floating glass surfaces refract, or null when Liquid Glass is off or
 * unsupported. A glass surface must not be inside the layer it samples, so this is only provided
 * to siblings of the captured content, never to the content itself.
 */
val LocalLiquidGlassBackdrop = staticCompositionLocalOf<Backdrop?> { null }

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
 * Returns null when Liquid Glass is off, in which case [Modifier.captureLiquidGlass] does nothing.
 */
@Composable
fun rememberLiquidGlassCapture(): LayerBackdrop? {
  val background = MaterialTheme.colorScheme.background
  val layer =
    rememberLayerBackdrop {
      drawRect(background)
      drawContent()
    }
  return layer.takeIf { liquidGlassEnabled() }
}

fun Modifier.captureLiquidGlass(layer: LayerBackdrop?): Modifier = if (layer != null) layerBackdrop(layer) else this

/** Makes [backdrop] available to the glass surfaces inside [content] (or removes it when null). */
@Composable
fun ProvideLiquidGlassBackdrop(
  backdrop: Backdrop?,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(LocalLiquidGlassBackdrop provides backdrop, content = content)
}
