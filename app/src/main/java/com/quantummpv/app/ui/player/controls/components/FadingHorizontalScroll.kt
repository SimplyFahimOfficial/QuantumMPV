/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player.controls.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quantummpv.app.ui.player.controls.LocalPlayerButtonsClickEvent
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.drop

/**
 * A horizontal scroller that fades the edge where more buttons are hidden, so a row that overflows reads as
 * scrollable. Adapted from mpvRx's `fadingHorizontalScroll` (1bf439137). Without overflow it draws nothing extra.
 */
internal fun Modifier.fadingHorizontalScroll(
  state: ScrollState,
  fadeLength: Dp = 24.dp,
): Modifier =
  this
    .graphicsLayer {
      // The fade erases pixels (DstIn), which needs an offscreen layer; skip that cost when nothing overflows.
      compositingStrategy = if (state.maxValue > 0) CompositingStrategy.Offscreen else CompositingStrategy.Auto
    }.drawWithContent {
      drawContent()
      val fadePx = fadeLength.toPx().coerceAtMost(size.width / 2f)
      if (fadePx <= 0f || state.maxValue <= 0) return@drawWithContent
      val (leftAlpha, rightAlpha) =
        fadeEdgeAlphas(state.value, state.maxValue, fadePx, firstItemOnRight = layoutDirection == LayoutDirection.Rtl)
      if (leftAlpha < 1f) {
        drawRect(
          brush =
            Brush.horizontalGradient(
              0f to Color.Black.copy(alpha = leftAlpha),
              1f to Color.Black,
              startX = 0f,
              endX = fadePx,
            ),
          size = Size(fadePx, size.height),
          blendMode = BlendMode.DstIn,
        )
      }
      if (rightAlpha < 1f) {
        drawRect(
          brush =
            Brush.horizontalGradient(
              0f to Color.Black,
              1f to Color.Black.copy(alpha = rightAlpha),
              startX = size.width - fadePx,
              endX = size.width,
            ),
          topLeft = Offset(size.width - fadePx, 0f),
          size = Size(fadePx, size.height),
          blendMode = BlendMode.DstIn,
        )
      }
    }.horizontalScroll(state)

/**
 * Opacity of the left and right edge for a scroller at [value] of [maxValue]. An edge is fully opaque when
 * nothing is hidden behind it and fades to transparent over [fadePx] of hidden content. In a right-to-left
 * layout ([firstItemOnRight]) scroll value 0 is the right edge.
 */
internal fun fadeEdgeAlphas(
  value: Int,
  maxValue: Int,
  fadePx: Float,
  firstItemOnRight: Boolean,
): Pair<Float, Float> {
  val hiddenBefore = value.toFloat()
  val hiddenAfter = (maxValue - value).toFloat()
  val hiddenLeft = if (firstItemOnRight) hiddenAfter else hiddenBefore
  val hiddenRight = if (firstItemOnRight) hiddenBefore else hiddenAfter
  return (1f - (hiddenLeft / fadePx).coerceIn(0f, 1f)) to (1f - (hiddenRight / fadePx).coerceIn(0f, 1f))
}

/**
 * Restarts the controls' auto-hide timer while [state] is being scrolled. Only taps and seekbar changes did
 * before, so a long drag toward a hidden button could outlast the hide delay and take the controls away.
 */
@Composable
internal fun KeepControlsShownWhileScrolling(state: ScrollState) {
  val onActivity by rememberUpdatedState(LocalPlayerButtonsClickEvent.current)
  LaunchedEffect(state) {
    keepControlsAliveWhileScrolling(snapshotFlow { state.isScrollInProgress }) { onActivity() }
  }
}

/** Calls [onActivity] when a scroll starts, every [intervalMs] while it lasts, and when it stops. */
internal suspend fun keepControlsAliveWhileScrolling(
  scrolling: Flow<Boolean>,
  intervalMs: Long = SCROLL_ACTIVITY_INTERVAL_MS,
  onActivity: () -> Unit,
) {
  // The first value is the resting state on composition, which is not user activity.
  scrolling.drop(1).collectLatest { active ->
    onActivity()
    while (active) {
      delay(intervalMs)
      onActivity()
    }
  }
}

// Shorter than the smallest hide delay offered in settings (500 ms), so one scroll never lets the timer expire.
private const val SCROLL_ACTIVITY_INTERVAL_MS = 250L
