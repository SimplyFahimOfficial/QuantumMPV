/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player

import android.graphics.PixelFormat
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup
import androidx.constraintlayout.widget.ConstraintLayout

/**
 * Owns the transparent SurfaceView that carries subtitles and the OSD while Dolby Vision plays directly on
 * [video]'s Surface. With direct output the frames go from MediaCodec to the video Surface untouched, so mpv can
 * no longer draw text into them.
 *
 * The overlay sits directly above [video] and below the Compose controls. It binds to [PlaybackSession] under
 * its own owner token, like the other surface owners, so a late destroy callback can only detach itself.
 *
 * Nothing is mounted unless [setEnabled] is called with true, which only happens in a FongMi build with the
 * setting on.
 *
 * Adapted from the overlay in mpvRx's `MpvSurfaceHost` (39472fff7).
 */
internal class OsdOverlayHost(
  private val video: View,
) {
  private var overlay: SurfaceView? = null
  private var surface: android.view.Surface? = null
  private var width = 0
  private var height = 0

  val isMounted: Boolean
    get() = overlay != null

  fun setEnabled(enabled: Boolean) {
    if (enabled == isMounted) return
    if (enabled) mount() else unmount()
  }

  /** Attaches again after the core was rebuilt or another owner held the session. */
  fun rebind() {
    val current = surface ?: return
    if (!current.isValid || PlaybackSession.isOsdSurfaceAttachedTo(this)) return
    PlaybackSession.bindOsdSurface(current, width, height, owner = this)
  }

  /** Teardown: detach and remove without touching the video Surface. */
  fun release() {
    unmount()
  }

  private fun mount() {
    val parent = video.parent as? ConstraintLayout ?: return
    val created =
      SurfaceView(video.context).apply {
        // Above the video SurfaceView but still behind the window, so the Compose controls stay on top.
        setZOrderMediaOverlay(true)
        holder.setFormat(PixelFormat.TRANSLUCENT)
        isClickable = false
        isFocusable = false
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
        layoutParams =
          ConstraintLayout
            .LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            .apply {
              startToStart = ConstraintLayout.LayoutParams.PARENT_ID
              endToEnd = ConstraintLayout.LayoutParams.PARENT_ID
              topToTop = ConstraintLayout.LayoutParams.PARENT_ID
              bottomToBottom = ConstraintLayout.LayoutParams.PARENT_ID
            }
      }
    created.holder.addCallback(
      object : SurfaceHolder.Callback {
        override fun surfaceCreated(holder: SurfaceHolder) {
          if (created !== overlay) return
          surface = holder.surface
          width = created.width
          height = created.height
          rebind()
        }

        override fun surfaceChanged(
          holder: SurfaceHolder,
          format: Int,
          newWidth: Int,
          newHeight: Int,
        ) {
          if (created !== overlay) return
          width = newWidth
          height = newHeight
          if (!PlaybackSession.resizeOsdSurface(newWidth, newHeight, owner = this@OsdOverlayHost)) rebind()
        }

        override fun surfaceDestroyed(holder: SurfaceHolder) {
          if (created !== overlay) return
          PlaybackSession.unbindOsdSurface(this@OsdOverlayHost)
          surface = null
          width = 0
          height = 0
        }
      },
    )
    overlay = created
    parent.addView(created, parent.indexOfChild(video) + 1)
  }

  private fun unmount() {
    val current = overlay ?: return
    overlay = null
    PlaybackSession.unbindOsdSurface(this)
    surface = null
    width = 0
    height = 0
    (current.parent as? ViewGroup)?.removeView(current)
  }
}
