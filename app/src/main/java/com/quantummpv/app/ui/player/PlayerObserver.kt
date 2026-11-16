/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player

import android.content.pm.ActivityInfo
import com.quantummpv.app.preferences.PlayerPreferences
import `is`.xyz.mpv.MPVLib
import `is`.xyz.mpv.MPVNode
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class PlayerObserver(
  private val activity: PlayerActivity,
) : MPVLib.EventObserver,
  KoinComponent {
  private val playerPreferences: PlayerPreferences by inject()

  private companion object {
    val ACTIVITY_DOUBLE_PROPERTIES = setOf("video-params/aspect", "container-fps", "sub-scale")
  }

  private fun shouldIgnoreCallback(): Boolean =
    activity.player.isExiting || !activity.isActivePlaybackOwner()

  private fun isVideoGeometryProperty(property: String): Boolean =
    property == "video-params/aspect" ||
      property == "video-params/w" ||
      property == "video-params/h"

  private fun shouldBypassUiThread(property: String): Boolean =
    isVideoGeometryProperty(property) ||
      property == "container-fps"

  /**
   * Stretch deliberately keeps a positive video-aspect-override so the picture fills the current
   * viewport. PlayerActivity's normal "Video" orientation refresh ignores positive overrides to
   * avoid treating custom aspect ratios as source geometry, which unintentionally also excluded
   * Stretch. Read the source video geometry from MPVView instead and only restore auto-orientation
   * for the built-in Stretch mode; custom aspect ratios remain untouched.
   */
  private fun requestStretchVideoOrientationUpdate(property: String? = null) {
    if (property != null && !isVideoGeometryProperty(property)) return

    activity.runOnUiThread {
      if (shouldIgnoreCallback() || activity.isFinishing || activity.isDestroyed) return@runOnUiThread
      if (playerPreferences.orientation.get() != PlayerOrientation.Video) return@runOnUiThread
      if (playerPreferences.lastCustomAspectRatio.get() > 0f) return@runOnUiThread
      if (playerPreferences.lastVideoAspect.get() != VideoAspect.Stretch) return@runOnUiThread

      val sourceAspect =
        runCatching { activity.player.getVideoOutAspect() }
          .getOrNull()
          ?.takeIf { it > 0.0 }
          ?: return@runOnUiThread

      val targetOrientation =
        if (sourceAspect > 1.0) {
          ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        } else {
          ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        }

      if (activity.requestedOrientation != targetOrientation) {
        activity.requestedOrientation = targetOrientation
      }
    }
  }

  // PlayerActivity.onObserverEvent(String) has no handlers; skip the main-thread hop.
  override fun eventProperty(property: String) = Unit

  override fun eventProperty(
    property: String,
    value: Long,
  ) {
    // The activity only reacts to the video size; everything else mpv reports would just wake the UI thread.
    if (!isVideoGeometryProperty(property) || shouldIgnoreCallback()) return
    if (shouldBypassUiThread(property)) {
      activity.runIfActivePlaybackOwner { activity.onObserverEvent(property, value) }
      requestStretchVideoOrientationUpdate(property)
    } else {
      activity.runOnUiThread {
        if (!shouldIgnoreCallback()) activity.onObserverEvent(property, value)
      }
    }
  }

  override fun eventProperty(
    property: String,
    value: Boolean,
  ) {
    if (shouldIgnoreCallback()) return
    // keep-open holds the last frame instead of emitting a natural END_FILE, so the raw
    // eof-reached edge is the only end-of-playback signal; PlayerActivity validates it by
    // position before advancing (a network stall can flip it mid-file).
    activity.runOnUiThread {
      if (!shouldIgnoreCallback()) activity.onObserverEvent(property, value)
    }
  }

  override fun eventProperty(
    property: String,
    value: String,
  ) {
    if ((property != "sub-text" && property.substringBeforeLast("/") != "user-data/mpvrx") || shouldIgnoreCallback()) return
    activity.runOnUiThread {
      if (!shouldIgnoreCallback()) activity.onObserverEvent(property, value)
    }
  }

  override fun eventProperty(
    property: String,
    value: Double,
  ) {
    // time-pos, playtime-remaining and the demuxer cache fire about once per frame and have no handler here.
    if (property !in ACTIVITY_DOUBLE_PROPERTIES || shouldIgnoreCallback()) return
    if (shouldBypassUiThread(property)) {
      activity.runIfActivePlaybackOwner { activity.onObserverEvent(property, value) }
      requestStretchVideoOrientationUpdate(property)
    } else {
      activity.runOnUiThread {
        if (!shouldIgnoreCallback()) activity.onObserverEvent(property, value)
      }
    }
  }

  @Suppress("EmptyFunctionBlock")
  override fun eventProperty(
    property: String,
    value: MPVNode,
  ) {
    if (shouldIgnoreCallback()) return
    activity.runOnUiThread {
      if (!shouldIgnoreCallback()) activity.onObserverEvent(property, value)
    }
  }

  override fun event(
    eventId: Int,
    data: MPVNode,
  ) {
    if (shouldIgnoreCallback()) return
    val naturalEnd = eventId == MPVLib.MpvEvent.MPV_EVENT_END_FILE && PlaybackSession.isNaturalEndFile(data)
    activity.runOnUiThread {
      if (shouldIgnoreCallback()) return@runOnUiThread
      activity.event(eventId)
      if (eventId == MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED) {
        requestStretchVideoOrientationUpdate()
      }
      if (naturalEnd) {
        activity.onObserverEvent("eof-reached", true)
      }
    }
  }
}
