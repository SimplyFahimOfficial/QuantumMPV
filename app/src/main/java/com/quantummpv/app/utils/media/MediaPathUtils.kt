/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.utils.media

import com.quantummpv.app.preferences.PlayerPreferences
import com.quantummpv.app.ui.player.PlaybackSession

/**
 * Extracts the file extension from a path or URI string, stripping query
 * parameters and fragment identifiers before extracting the last dot-delimited
 * segment.  Returns a lowercase extension without the leading dot, or an empty
 * string if no extension is found.
 *
 * Example:
 * ```
 * "video.mkv?quality=1080#t=10".fileExtension() == "mkv"
 * "/Audio/clip #edit.mp4".fileExtension() == "mp4"
 * "content://media/123".fileExtension() == ""
 * ```
 *
 * Ported from mpvRx 18470efc4 (#943).
 */
fun String.fileExtension(): String {
  val scheme = substringBefore("://", missingDelimiterValue = "")
  // '#' and '?' are legal in file names (e.g. hashtags), so paths keep them while they still end in an extension.
  // shortcut: a scheme-less "a.mkv?t=1.5" reads as "5"; upgrade if such strings reach this function.
  if (scheme.isEmpty() || scheme.equals("file", ignoreCase = true)) {
    lastExtension(this).takeIf { it.isNotEmpty() && it.all(Char::isLetterOrDigit) }?.let { return it }
  }
  return lastExtension(substringBefore('?').substringBefore('#'))
}

private fun lastExtension(path: String): String {
  val lastSlash = path.lastIndexOf('/')
  val lastDot = path.lastIndexOf('.')
  return if (lastDot > lastSlash && lastDot < path.length - 1) path.substring(lastDot + 1).lowercase() else ""
}

/**
 * Returns the MPV seek mode string based on the user's precise-seeking preference
 * and the current media duration.  Videos shorter than 2 minutes always use
 * precise seeking for a better scrubbing experience.
 */
fun resolveSeekMode(playerPreferences: PlayerPreferences): String {
  val duration = PlaybackSession.getPropertyDouble("duration")?.toInt() ?: 0
  val usePrecise = playerPreferences.usePreciseSeeking.get() || duration < 120
  return if (usePrecise) "relative+exact" else "relative+keyframes"
}
