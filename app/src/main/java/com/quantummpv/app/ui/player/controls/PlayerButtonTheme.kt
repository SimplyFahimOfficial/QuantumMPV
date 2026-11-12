/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player.controls

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.quantummpv.app.ui.player.controls.components.LocalForceDarkPlayerButtonsBackground
import com.quantummpv.app.ui.player.controls.components.LocalHidePlayerButtonsBackground
import com.quantummpv.app.ui.player.controls.components.LocalPlayerButtonLabel
import com.quantummpv.app.ui.theme.LocalDarkAppColorScheme

/** Designed button alphas and the user's background opacity level (mpvRx 3.0.2). */
internal object PlayerButtonAlpha {
  const val CONTAINER = 0.55f
  const val BORDER = 0.4f

  /** The level at which every surface keeps its designed alpha. */
  const val DEFAULT_BACKGROUND_LEVEL = CONTAINER

  /** The stored level made safe: a non-finite or out-of-range value (e.g. from an edited backup) cannot reach drawing. */
  fun level(raw: Float): Float = if (raw.isFinite()) raw.coerceIn(0f, 1f) else DEFAULT_BACKGROUND_LEVEL

  /**
   * Maps a designed [alpha] through the user's [level]: 0 gives no fill, [DEFAULT_BACKGROUND_LEVEL]
   * keeps [alpha], 1 gives a solid fill. A fully transparent hit area stays transparent.
   */
  fun background(
    alpha: Float,
    level: Float,
  ): Float {
    if (alpha <= 0f) return 0f
    val l = level(level)
    return if (l >= DEFAULT_BACKGROUND_LEVEL) {
      alpha + (1f - alpha) * ((l - DEFAULT_BACKGROUND_LEVEL) / (1f - DEFAULT_BACKGROUND_LEVEL))
    } else {
      alpha * (l / DEFAULT_BACKGROUND_LEVEL)
    }
  }
}

/**
 * Carries the background visibility choice through every player-button implementation.
 *
 * Color overrides are deliberately not applied to this complete subtree: top-player groups also
 * contain status text and the audio player contains artwork, lyrics and sheets. Re-theming that
 * whole hierarchy just to style its buttons caused unrelated UI to switch palette.
 */
@Composable
internal fun PlayerButtonTheme(
  hideBackground: Boolean,
  content: @Composable () -> Unit,
) {
  CompositionLocalProvider(
    LocalHidePlayerButtonsBackground provides hideBackground,
    content = content,
  )
}

/**
 * Applies the selected dark app palette only to the contents of one player button, and gives it
 * [label] as its spoken name unless the button passes its own.
 */
@Composable
internal fun PlayerButtonContentTheme(
  label: String? = null,
  content: @Composable () -> Unit,
) {
  val useDarkPalette =
    LocalForceDarkPlayerButtonsBackground.current && !LocalHidePlayerButtonsBackground.current
  val darkColors = LocalDarkAppColorScheme.current
  CompositionLocalProvider(LocalPlayerButtonLabel provides label) {
    if (!useDarkPalette || darkColors == null) {
      content()
    } else {
      MaterialTheme(
        colorScheme = darkColors,
        typography = MaterialTheme.typography,
        shapes = MaterialTheme.shapes,
        content = content,
      )
    }
  }
}
