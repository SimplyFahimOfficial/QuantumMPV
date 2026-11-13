/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player

import android.content.Context
import android.graphics.SurfaceTexture
import android.view.Surface
import android.view.TextureView
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.viewinterop.AndroidView
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Draws mpv's output on a [TextureView] inside a Compose scene, so the same frames can be recorded into a
 * [LayerBackdrop] and sampled by glass controls. The default renderer is the player's SurfaceView; this host is
 * only mounted while [PlayerGlassPolicy] asks for it.
 *
 * The host binds its [Surface] to [PlaybackSession] under its own owner token. The session ignores an unbind from
 * anything that is not the current owner, so a late callback from the other renderer cannot detach this one.
 *
 * Adapted from mpvRx's `MpvSurfaceHost` (v3.0.2).
 */
internal class MpvTextureHost(
  context: Context,
) : FrameLayout(context) {
  /** Receives the texture's Surface once it exists, and again after every recreation. */
  var onSurfaceAvailable: ((Surface, Int, Int) -> Unit)? = null
  var onSurfaceSizeChanged: ((Int, Int) -> Unit)? = null
  var onSurfaceDestroyed: (() -> Unit)? = null

  /** The recorder the glass samples; null until the composition has created it. */
  var videoBackdrop: LayerBackdrop? by mutableStateOf(null)
    private set

  private val frameReady = MutableStateFlow(false)

  /** True once the texture has received a real frame, so glass never samples a blank layer. */
  val frameReadyState: StateFlow<Boolean> = frameReady

  private var ownedSurface: Surface? = null
  private val composeView =
    ComposeView(context).apply {
      setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
      setContent {
        val scene = rememberLayerBackdrop()
        DisposableEffect(scene) {
          videoBackdrop = scene
          onDispose { if (videoBackdrop === scene) videoBackdrop = null }
        }
        Box(Modifier.fillMaxSize().background(Color.Black).layerBackdrop(scene)) {
          AndroidView(
            // A compositing layer just below opaque makes Compose record the TextureView's GPU layer instead of
            // reading it back, so no bitmap copy or second decoder is involved.
            modifier = Modifier.fillMaxSize().graphicsLayer { alpha = RECORDABLE_ALPHA },
            factory = { viewContext -> createTextureView(viewContext) },
          )
        }
      }
    }

  init {
    descendantFocusability = FOCUS_BLOCK_DESCENDANTS
    addView(composeView, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
  }

  /**
   * Ends the Compose scene and its recorded layer now. The scene is otherwise disposed with the activity, so a
   * host that was removed and replaced would keep its graphics layer alive until the player closed.
   */
  fun dispose() {
    composeView.disposeComposition()
  }

  /** Forget the last frame, for example after a colour-space change that replaces the pixels in place. */
  fun invalidateFrame() {
    frameReady.value = false
  }

  private fun createTextureView(viewContext: Context): TextureView =
    TextureView(viewContext).apply {
      isOpaque = true
      isClickable = false
      isFocusable = false
      importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
      surfaceTextureListener =
        object : TextureView.SurfaceTextureListener {
          override fun onSurfaceTextureAvailable(
            texture: SurfaceTexture,
            width: Int,
            height: Int,
          ) {
            val surface = Surface(texture)
            ownedSurface = surface
            onSurfaceAvailable?.invoke(surface, width, height)
          }

          override fun onSurfaceTextureSizeChanged(
            texture: SurfaceTexture,
            width: Int,
            height: Int,
          ) {
            onSurfaceSizeChanged?.invoke(width, height)
          }

          override fun onSurfaceTextureDestroyed(texture: SurfaceTexture): Boolean {
            frameReady.value = false
            onSurfaceDestroyed?.invoke()
            ownedSurface?.release()
            ownedSurface = null
            // The Surface was created here, so it is released here, after mpv has detached from it.
            return true
          }

          override fun onSurfaceTextureUpdated(texture: SurfaceTexture) {
            if (!frameReady.value) frameReady.value = true
          }
        }
    }

  private companion object {
    const val RECORDABLE_ALPHA = 0.999f
  }
}
