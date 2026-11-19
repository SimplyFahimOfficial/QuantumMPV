/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.browser.videolist

import androidx.annotation.StringRes
import com.quantummpv.app.R

/** Watch-state filter for a video folder. [ALL] shows everything. */
enum class WatchFilter(
  @StringRes val label: Int,
) {
  ALL(R.string.video_filter_all),
  UNWATCHED(R.string.video_filter_unwatched),
  IN_PROGRESS(R.string.video_filter_in_progress),
  WATCHED(R.string.video_filter_watched),
}

/**
 * Watched wins over progress; progress means saved playback of the video that is not watched yet
 * (a hidden near-0% or near-100% progress bar still counts); everything else, including a video
 * that was never played, is unwatched.
 */
internal fun VideoWithPlaybackInfo.matches(filter: WatchFilter): Boolean =
  when (filter) {
    WatchFilter.ALL -> true
    WatchFilter.WATCHED -> isWatched
    WatchFilter.IN_PROGRESS -> !isWatched && hasSavedProgress
    WatchFilter.UNWATCHED -> !isWatched && !hasSavedProgress
  }
