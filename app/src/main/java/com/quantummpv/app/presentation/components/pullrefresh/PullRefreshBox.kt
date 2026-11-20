/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.presentation.components.pullrefresh

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.pulltorefresh.pullToRefresh
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.launch

/**
 * A reusable Box that wraps content with pull-to-refresh functionality.
 *
 * @param isRefreshing Tracks refresh state.
 * @param onRefresh Invoked when refresh is triggered.
 * @param modifier Box modifier.
 * @param enabled Toggles pull-to-refresh.
 * @param listState Unused. Reserved for top-scroll checks.
 * @param refreshingOffset Unused. Reserved for offset customization.
 * @param refreshThreshold Pull distance required to trigger refresh.
 * @param delayAfterRefresh Delay (ms) to keep indicator visible after completion.
 * @param content Content displayed inside the Box.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun PullRefreshBox(
  isRefreshing: MutableState<Boolean>,
  onRefresh: suspend () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
  @Suppress("UNUSED_PARAMETER") listState: LazyListState? = null,
  @Suppress("UNUSED_PARAMETER") refreshingOffset: Dp = 80.dp,
  refreshThreshold: Dp = 80.dp,
  delayAfterRefresh: Long = 800L,
  content: @Composable BoxScope.() -> Unit,
) {
  val coroutineScope = rememberCoroutineScope()
  val state = rememberPullToRefreshState()
  val density = LocalDensity.current

  val maxTranslationPx = with(density) { refreshThreshold.toPx() }

  val singleFlight = remember { SingleFlight() }

  // The pull state already follows the finger and animates itself to the threshold while refreshing
  // and back to zero afterwards, so content and indicator read it directly in the draw phase:
  // 1:1 with the finger, no second spring lagging behind, and no recomposition per frame.
  fun contentTranslation(): Float = rubberBandedPull(state.distanceFraction, maxTranslationPx)
  fun indicatorProgress(): Float = state.distanceFraction.coerceIn(0f, 1f)

  // One tick at the moment a release would refresh, so the commit point can be felt.
  val haptics = LocalHapticFeedback.current
  val latestIsRefreshing by rememberUpdatedState(isRefreshing.value)
  LaunchedEffect(state, enabled) {
    if (!enabled) return@LaunchedEffect
    snapshotFlow { state.distanceFraction >= 1f }
      .distinctUntilChanged()
      .drop(1)
      .collect { armed -> if (armed && !latestIsRefreshing) haptics.performHapticFeedback(HapticFeedbackType.SegmentTick) }
  }

  val expressivePolygons =
    remember {
      listOf(
        MaterialShapes.Cookie4Sided,
        MaterialShapes.SoftBurst,
        MaterialShapes.Oval,
      )
    }

  val indicatorSize = 56.dp
  val indicatorSizePx = remember(density) { with(density) { indicatorSize.toPx() } }

  Box(
    modifier =
      modifier.pullToRefresh(
        isRefreshing = isRefreshing.value,
        state = state,
        enabled = enabled,
        threshold = refreshThreshold,
        onRefresh = {
          singleFlight.launch(
            scope = coroutineScope,
            onStart = { isRefreshing.value = true },
            onEnd = { isRefreshing.value = false },
          ) {
            onRefresh()
            delay(delayAfterRefresh)
          }
        },
      ),
  ) {
    Box(
      modifier =
        Modifier
          .matchParentSize()
          .graphicsLayer { translationY = contentTranslation() },
    ) {
      content()
    }

    Box(
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .graphicsLayer {
            val progress = indicatorProgress()
            translationY = (contentTranslation() / 2f) - (indicatorSizePx / 2f)
            scaleX = progress
            scaleY = progress
            alpha = progress
          }.shadow(elevation = 4.dp, shape = CircleShape, clip = false)
          .size(indicatorSize)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.surfaceContainerHigh)
          .padding(6.dp),
      contentAlignment = Alignment.Center,
    ) {
      if (isRefreshing.value) {
        LoadingIndicator(
          polygons = expressivePolygons,
          modifier = Modifier.fillMaxSize(),
        )
      } else {
        LoadingIndicator(
          progress = { state.distanceFraction.coerceIn(0f, 1f) },
          polygons = expressivePolygons,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}

/**
 * Runs at most one refresh at a time. A second request while one is in flight is ignored, so a late
 * release can neither cancel the running refresh nor let its completion clear a newer one.
 * [onEnd] runs from the job's completion handler, which also fires when the scope is cancelled
 * before the body ever started (e.g. the screen leaves composition right after the gesture).
 */
internal class SingleFlight {
  private var current: Job? = null

  fun launch(
    scope: CoroutineScope,
    onStart: () -> Unit,
    onEnd: () -> Unit,
    body: suspend () -> Unit,
  ): Boolean {
    if (current != null) return false
    onStart()
    val job = scope.launch(start = CoroutineStart.LAZY) { body() }
    current = job
    job.invokeOnCompletion {
      current = null
      onEnd()
    }
    job.start()
    return true
  }
}

/**
 * Pull distance in px for [fraction] of the threshold [thresholdPx]. Up to the threshold the content
 * follows 1:1; past it, resistance grows progressively (rubber band) instead of either stopping hard
 * or tracking without limit.
 */
internal fun rubberBandedPull(
  fraction: Float,
  thresholdPx: Float,
  constant: Float = 0.55f,
): Float {
  if (!fraction.isFinite() || fraction <= 0f || thresholdPx <= 0f) return 0f
  if (fraction <= 1f) return fraction * thresholdPx
  val overshoot = (fraction - 1f) * thresholdPx
  return thresholdPx + (overshoot * thresholdPx * constant) / (thresholdPx + constant * overshoot)
}
