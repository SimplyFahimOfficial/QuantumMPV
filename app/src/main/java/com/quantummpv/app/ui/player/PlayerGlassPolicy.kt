/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player

/**
 * Decides whether the video is drawn on a Compose-recordable [android.view.TextureView] so the player controls
 * can show live glass over it. Everything else, including the default, keeps the native SurfaceView.
 *
 * Both the activity (which picks the surface) and the controls (which pick the button style) call this, so the
 * two can never disagree about whether glass is possible.
 *
 * Adapted from mpvRx's `shouldUseTextureRendering` (v3.0.2).
 */
internal object PlayerGlassPolicy {
  /** Backdrop blur is a RenderEffect, which needs Android 12 (API 31). */
  const val MIN_SDK = 31

  fun usesTextureSurface(
    glassEnabled: Boolean,
    sdkInt: Int,
    hdrOutput: Boolean,
    hdrOwnedByMpvConf: Boolean,
    glassInHdr: Boolean,
    youTubeAmbientSelected: Boolean,
    dolbyVisionDirect: Boolean,
  ): Boolean {
    if (!glassEnabled || sdkInt < MIN_SDK) return false
    // Direct Dolby Vision hands frames from MediaCodec to the video SurfaceView; a TextureView cannot show them.
    if (dolbyVisionDirect) return false
    // The YouTube ambient background samples the SurfaceView with PixelCopy and resizes it, so it keeps that
    // view. This is the user's setting, not whether a frame was captured: the capture depends on which surface
    // is bound, so choosing the surface from it would flip the surface, which would stop the capture, and so on.
    if (youTubeAmbientSelected) return false
    // A TextureView is composited into the window's SDR buffer, so HDR output stays native unless the user
    // accepted that trade. An mpv.conf that owns HDR output must never be overridden by a UI preference.
    if (hdrOutput || hdrOwnedByMpvConf) return glassInHdr && !hdrOwnedByMpvConf
    return true
  }
}
