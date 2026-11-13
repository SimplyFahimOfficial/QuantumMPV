/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player.controls.components

import android.view.View
import android.view.ViewTreeObserver
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop

/**
 * Maps the live GPU layer of the video host into the controls' own ComposeView.
 *
 * The video and the controls are two separate Compose scenes, so glass in the controls cannot sample the video
 * directly. [drawVideo] draws the video's recorded layer, scaled and offset to where the video really is in the
 * window, into a backdrop that belongs to the controls. The layer is a reference to the GPU texture, not a
 * snapshot, so frames reach the glass at display rate without readback.
 *
 * Adapted from mpvRx's `VideoGlassSource` (v3.0.2).
 */
@Stable
internal class VideoGlassSource(
  val scene: LayerBackdrop?,
) {
  var boundsInWindow by mutableStateOf(Rect.Zero)
  var originInWindow by mutableStateOf(Offset.Zero)

  fun DrawScope.drawVideo() {
    drawRect(Color.Black)
    val layer = scene?.graphicsLayer ?: return
    // Use the recorded layer's own size: the recording can be resized independently of the host's layout, and
    // the video can be zoomed, so the host's layout ratio would stretch colours over unrelated controls.
    val content = layer.size
    val bounds = boundsInWindow
    if (content.width <= 0 || content.height <= 0 || bounds.width <= 0f || bounds.height <= 0f) return
    withTransform({
      translate(bounds.left - originInWindow.x, bounds.top - originInWindow.y)
      scale(bounds.width / content.width, bounds.height / content.height, pivot = Offset.Zero)
    }) {
      drawLayer(layer)
    }
  }
}

/** Tracks where the video host sits in the window, including zoom and pan, and builds the source for it. */
@Composable
internal fun rememberVideoGlassSource(
  videoHost: View?,
  scene: LayerBackdrop?,
): VideoGlassSource {
  val source = remember(videoHost, scene) { VideoGlassSource(scene) }
  DisposableEffect(videoHost, source) {
    if (videoHost == null || scene == null) return@DisposableEffect onDispose {}
    val location = IntArray(2)

    fun updateBounds() {
      videoHost.getLocationInWindow(location)
      source.boundsInWindow =
        if (videoHost.isShown) {
          Rect(
            location[0].toFloat(),
            location[1].toFloat(),
            location[0] + videoHost.width * videoHost.scaleX,
            location[1] + videoHost.height * videoHost.scaleY,
          )
        } else {
          Rect.Zero
        }
    }

    val observer = videoHost.viewTreeObserver
    val listener =
      ViewTreeObserver.OnPreDrawListener {
        updateBounds()
        true
      }
    updateBounds()
    observer.addOnPreDrawListener(listener)
    onDispose { if (observer.isAlive) observer.removeOnPreDrawListener(listener) }
  }
  return source
}

/** Records [source] into [backdrop] in the controls' coordinates, without drawing the video a second time. */
@Composable
internal fun VideoGlassBackdropSource(
  source: VideoGlassSource,
  backdrop: LayerBackdrop,
) {
  Box(
    Modifier
      .fillMaxSize()
      .onGloballyPositioned { source.originInWindow = it.positionInWindow() }
      .layerBackdrop(backdrop),
  )
}
