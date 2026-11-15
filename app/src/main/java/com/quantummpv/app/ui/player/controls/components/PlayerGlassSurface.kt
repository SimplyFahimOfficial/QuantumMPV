/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 */

package com.quantummpv.app.ui.player.controls.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quantummpv.app.ui.liquidglass.liquidGlassSurface
import com.quantummpv.app.ui.player.controls.LocalPlayerGlassBackdrop

/**
 * A drop-in for the HUD's [Surface]. With the experimental player glass on, the fill and border are
 * replaced by live glass over the video, tinted with the [color] the caller chose; otherwise this is a
 * plain [Surface]. A transparent [color] or `glass = false` (hide button background) never gets glass.
 *
 * The glass is drawn as the first layer inside the surface, so it takes the final size and shape the
 * caller's modifiers produce, and a click ripple or focus highlight on [modifier] still sits on top.
 */
@Composable
internal fun PlayerGlassSurface(
  modifier: Modifier = Modifier,
  shape: Shape = RectangleShape,
  color: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = contentColorFor(color),
  tonalElevation: Dp = 0.dp,
  shadowElevation: Dp = 0.dp,
  border: BorderStroke? = null,
  glass: Boolean = true,
  content: @Composable () -> Unit,
) {
  val backdrop = LocalPlayerGlassBackdrop.current
  if (backdrop == null || !glass || color.alpha <= 0f) {
    Surface(modifier, shape, color, contentColor, tonalElevation, shadowElevation, border, content)
    return
  }
  Surface(
    modifier = modifier,
    shape = shape,
    color = Color.Transparent,
    contentColor = contentColor,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = null,
  ) {
    Box(propagateMinConstraints = true) {
      Box(Modifier.matchParentSize().liquidGlassSurface(backdrop, shape, color, lightweight = true))
      content()
    }
  }
}

/** The clickable variant, for HUD surfaces that are buttons themselves. */
@Composable
internal fun PlayerGlassSurface(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  shape: Shape = RectangleShape,
  color: Color = MaterialTheme.colorScheme.surface,
  contentColor: Color = contentColorFor(color),
  tonalElevation: Dp = 0.dp,
  shadowElevation: Dp = 0.dp,
  border: BorderStroke? = null,
  content: @Composable () -> Unit,
) {
  val backdrop = LocalPlayerGlassBackdrop.current
  if (backdrop == null || color.alpha <= 0f) {
    Surface(onClick, modifier, enabled, shape, color, contentColor, tonalElevation, shadowElevation, border, content = content)
    return
  }
  Surface(
    onClick = onClick,
    modifier = modifier,
    enabled = enabled,
    shape = shape,
    color = Color.Transparent,
    contentColor = contentColor,
    tonalElevation = 0.dp,
    shadowElevation = 0.dp,
    border = null,
  ) {
    Box(propagateMinConstraints = true) {
      Box(Modifier.matchParentSize().liquidGlassSurface(backdrop, shape, color, lightweight = true))
      content()
    }
  }
}
