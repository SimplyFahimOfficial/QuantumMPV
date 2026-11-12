/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player

import android.os.Build
import android.view.View
import android.view.ViewGroup
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.constraintlayout.widget.ConstraintLayout
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.quantummpv.app.preferences.AppearancePreferences
import com.quantummpv.app.preferences.DecoderPreferences
import com.quantummpv.app.preferences.MpvConfigControlledFeatures
import com.quantummpv.app.preferences.MpvConfigOverridePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Owns the optional [MpvTextureHost] and decides, through [PlayerGlassPolicy], whether mpv draws on it or on the
 * player's SurfaceView. When glass is off (the default) nothing is added to the layout and [video] is untouched.
 *
 * The host is placed directly above [video] and mirrors its zoom and pan, so a switch between the two renderers
 * looks the same to the user.
 */
internal class PlayerGlassRenderer(
  private val video: MPVView,
  private val appearance: AppearancePreferences,
  private val decoder: DecoderPreferences,
) {
  internal var host: MpvTextureHost? = null
    private set
  private val textureActive = MutableStateFlow(false)
  private var released = false

  /** True while mpv is drawing on the texture, which is what lets the controls use glass. */
  val isTextureActive: StateFlow<Boolean> = textureActive

  val backdrop: LayerBackdrop?
    get() = host?.videoBackdrop

  val frameReady: StateFlow<Boolean>?
    get() = host?.frameReadyState

  /** Re-evaluates the policy. Call when any of its inputs change, and once after the layout exists. */
  fun update(ambientPresentation: Boolean) {
    // After teardown a late preference change must not mount a host into a player that is going away.
    if (released) return
    val wantTexture =
      PlayerGlassPolicy.usesTextureSurface(
        glassEnabled = appearance.liquidGlassPlayerEnabled.get(),
        sdkInt = Build.VERSION.SDK_INT,
        hdrOutput = decoder.hdrScreenMode.get() != HdrScreenMode.OFF,
        hdrOwnedByMpvConf = MpvConfigOverridePolicy.ownsAny(MpvConfigControlledFeatures.HDR_OUTPUT),
        glassInHdr = appearance.liquidGlassInHdr.get(),
        ambientPresentation = ambientPresentation,
      )
    if (wantTexture == textureActive.value) return
    if (wantTexture) mount() else unmount()
  }

  /** Marks the recorded frame stale, for a colour-space change that keeps the same surface. */
  fun invalidateFrame() {
    host?.invalidateFrame()
  }

  /** Zoom and pan apply to whichever view is showing the video. */
  fun applyTransform(
    scale: Float,
    panX: Float,
    panY: Float,
  ) {
    host?.let {
      it.scaleX = scale
      it.scaleY = scale
      it.translationX = panX
      it.translationY = panY
    }
  }

  /**
   * Shows or hides the video. The SurfaceView is hidden whenever the texture is the renderer, so it cannot
   * come back and take the Surface; INVISIBLE also makes Android destroy its Surface, which detaches mpv.
   */
  fun setVideoVisibility(visibility: Int) {
    if (host == null) {
      video.visibility = visibility
    } else {
      host?.visibility = visibility
      video.visibility = View.INVISIBLE
    }
  }

  /** Teardown only: removes the host and detaches mpv without handing it back to the SurfaceView. */
  fun release() {
    released = true
    val current = host ?: return
    host = null
    textureActive.value = false
    video.releaseExternalSurface()
    (current.parent as? ViewGroup)?.removeView(current)
  }

  private fun mount() {
    val parent = video.parent as? ConstraintLayout ?: return
    val created =
      MpvTextureHost(video.context).apply {
        onSurfaceAvailable = { surface, width, height -> video.bindExternalSurface(surface, width, height) }
        onSurfaceSizeChanged = { width, height -> video.resizeExternalSurface(width, height) }
        onSurfaceDestroyed = { video.unbindExternalSurface() }
        layoutParams =
          ConstraintLayout
            .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            .apply {
              startToStart = ConstraintLayout.LayoutParams.PARENT_ID
              endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
              topToTop = ConstraintLayout.LayoutParams.PARENT_ID
              bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
        scaleX = video.scaleX
        scaleY = video.scaleY
        translationX = video.translationX
        translationY = video.translationY
        visibility = video.visibility
      }
    host = created
    // Directly above the SurfaceView, below the controls, so the HUD still draws over the video.
    parent.addView(created, parent.indexOfChild(video) + 1)
    video.setExternalSurface(true)
    video.visibility = View.INVISIBLE
    textureActive.value = true
  }

  private fun unmount() {
    val current = host ?: return
    host = null
    textureActive.value = false
    // Whatever the host was showing is what the SurfaceView should show again, for example hidden for audio.
    video.visibility = current.visibility
    video.setExternalSurface(false)
    (current.parent as? ViewGroup)?.removeView(current)
  }
}

/** The renderer for the current player screen, or null where the player controls are shown without one. */
internal val LocalPlayerGlassRenderer = staticCompositionLocalOf<PlayerGlassRenderer?> { null }
