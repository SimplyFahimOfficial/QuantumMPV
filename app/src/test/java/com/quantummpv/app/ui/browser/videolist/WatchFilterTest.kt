package com.quantummpv.app.ui.browser.videolist

import android.net.Uri
import com.quantummpv.app.database.entities.PlaybackStateEntity
import com.quantummpv.app.domain.media.model.Video
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class WatchFilterTest {
  private val durationSeconds = 1000L
  private val video =
    Video(
      id = 1,
      title = "t",
      displayName = "t.mp4",
      path = "/m/t.mp4",
      uri = Uri.parse("file:///m/t.mp4"),
      duration = durationSeconds * 1000,
      durationFormatted = "16:40",
      size = 1,
      sizeFormatted = "1 B",
      dateModified = 0,
      dateAdded = 0,
      mimeType = "video/mp4",
      bucketId = "b",
      bucketDisplayName = "b",
      width = 1,
      height = 1,
      fps = 30f,
      resolution = "1x1",
    )

  private fun state(
    watchedSeconds: Int,
    hasBeenWatched: Boolean = false,
    lastPosition: Int = watchedSeconds,
  ) = PlaybackStateEntity(
    mediaTitle = "t",
    lastPosition = lastPosition,
    playbackSpeed = 1.0,
    sid = -1,
    subDelay = 0,
    subSpeed = 1.0,
    aid = -1,
    audioDelay = 0,
    timeRemaining = (durationSeconds - watchedSeconds).toInt(),
    hasBeenWatched = hasBeenWatched,
  )

  private fun info(
    playback: PlaybackStateEntity?,
    watchedThreshold: Int = 95,
    video: Video = this.video,
  ) = buildVideoWithPlaybackInfo(video, playback, currentTimeMillis = 0, newLabelDays = 7, watchedThreshold = watchedThreshold)

  private fun statuses(info: VideoWithPlaybackInfo) = WatchFilter.entries.filter { it != WatchFilter.ALL && info.matches(it) }

  @Test
  fun everyVideoMatchesAll() {
    assertTrue(info(null).matches(WatchFilter.ALL))
    assertTrue(info(state(500)).matches(WatchFilter.ALL))
    assertTrue(info(state(990, hasBeenWatched = true)).matches(WatchFilter.ALL))
  }

  @Test
  fun neverPlayedIsUnwatched() {
    assertEquals(listOf(WatchFilter.UNWATCHED), statuses(info(null)))
    assertEquals(listOf(WatchFilter.UNWATCHED), statuses(VideoWithPlaybackInfo(video)))
  }

  @Test
  fun partialPlaybackIsInProgress() {
    assertEquals(listOf(WatchFilter.IN_PROGRESS), statuses(info(state(500))))
    assertEquals(listOf(WatchFilter.IN_PROGRESS), statuses(info(state(10))))
  }

  @Test
  fun progressHiddenFromTheBarStillCountsAsInProgress() {
    // 99.5% is not shown as a bar, and is not watched while the threshold is disabled.
    val nearlyDone = info(state(995), watchedThreshold = 0)
    assertEquals(null, nearlyDone.progressPercentage)
    assertEquals(listOf(WatchFilter.IN_PROGRESS), statuses(nearlyDone))
  }

  @Test
  fun lessThanOnePercentIsStillUnwatched() {
    val barely = info(state(5))
    assertEquals(listOf(WatchFilter.UNWATCHED), statuses(barely))
  }

  @Test
  fun reachingTheThresholdMarksWatchedAndOverridesProgress() {
    val done = info(state(960))
    assertEquals(listOf(WatchFilter.WATCHED), statuses(done))
    assertFalse(done.matches(WatchFilter.IN_PROGRESS))
  }

  @Test
  fun manuallyMarkedWatchedIsWatchedWhateverTheProgress() {
    assertEquals(listOf(WatchFilter.WATCHED), statuses(info(state(0, hasBeenWatched = true))))
    assertEquals(listOf(WatchFilter.WATCHED), statuses(info(state(300, hasBeenWatched = true))))
  }

  @Test
  fun unknownLengthUsesTheSavedPosition() {
    val live = video.copy(duration = 0)
    assertEquals(listOf(WatchFilter.IN_PROGRESS), statuses(info(state(0, lastPosition = 42), video = live)))
    assertEquals(listOf(WatchFilter.UNWATCHED), statuses(info(state(0, lastPosition = 0), video = live)))
  }

  @Test
  fun everyVideoHasExactlyOneStatus() {
    val all =
      listOf(
        info(null),
        info(state(5)),
        info(state(500)),
        info(state(995), watchedThreshold = 0),
        info(state(960)),
        info(state(0, hasBeenWatched = true)),
      )
    all.forEach { assertEquals(1, statuses(it).size) }
  }
}
