/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player

import android.view.Surface
import com.quantummpv.app.BuildConfig
import com.quantummpv.app.preferences.DecoderPreferences
import `is`.xyz.mpv.MPVLib
import java.lang.reflect.Method

/**
 * FongMi's mpv can hand Dolby Vision tracks to MediaCodec on the video SurfaceView
 * (`android-dolby-vision-output=direct`) and draw subtitles and OSD on a transparent overlay Surface. Only the
 * FongMi mpvlib has the overlay calls, so they are looked up at run time; the other builds compile against
 * libraries without them.
 *
 * Adapted from mpvRx's `DolbyVisionDirect` (39472fff7).
 */
internal object DolbyVisionDirect {
  private val attachMethod: Method? by lazy { lookup("attachOsdSurface", Surface::class.java) }
  private val detachMethod: Method? by lazy { lookup("detachOsdSurface") }

  /** True only in a FongMi build whose mpvlib actually exposes both overlay calls. */
  val isBuildSupported: Boolean
    get() = BuildConfig.MPV_SUPPORTS_DOLBY_VISION_DIRECT && attachMethod != null && detachMethod != null

  fun isEnabled(preferences: DecoderPreferences): Boolean = isBuildSupported && preferences.dolbyVisionDirect.get()

  fun attachOsdSurface(surface: Surface) {
    attachMethod?.invoke(MPVLib, surface)
  }

  fun detachOsdSurface() {
    detachMethod?.invoke(MPVLib)
  }

  private fun lookup(
    name: String,
    vararg parameterTypes: Class<*>,
  ): Method? =
    if (!BuildConfig.MPV_SUPPORTS_DOLBY_VISION_DIRECT) {
      null
    } else {
      runCatching { MPVLib::class.java.getMethod(name, *parameterTypes) }.getOrNull()
    }
}
