package com.quantummpv.app.repository

import com.quantummpv.app.ui.player.PlaybackItem
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class WatchStatsRepositoryTest {
  private val context = RuntimeEnvironment.getApplication()

  private fun video(id: String) = PlaybackItem(stableId = id, originalUri = "file:///v/$id.mp4", title = id, mimeType = "video/mp4")

  private fun audio(id: String) = PlaybackItem(stableId = id, originalUri = "file:///a/$id.mp3", title = id, mimeType = "audio/mpeg")

  @Test
  fun recordsTimeSessionsAndSplitsVideoFromAudio() = runBlocking {
    File(context.filesDir, "watch_stats.json").delete()
    val repository = WatchStatsRepository(context)
    repository.recordSession(video("a"))
    repository.recordPlayback(video("a"), 40, repository.resetVersion.value)
    repository.recordPlayback(audio("b"), 25, repository.resetVersion.value)

    val stats = repository.snapshot()
    assertEquals(65L, stats.totalSeconds)
    assertEquals(40L, stats.videoSeconds)
    assertEquals(25L, stats.audioSeconds)
    assertEquals(1L, stats.sessions)
    assertEquals(40L, stats.media["a"]?.seconds)
    assertTrue(stats.media["b"]?.isAudio == true)
    assertEquals(65L, stats.days.values.sum())
  }

  @Test
  fun ignoresNonPositiveDurationsAndClearResets() = runBlocking {
    File(context.filesDir, "watch_stats.json").delete()
    val repository = WatchStatsRepository(context)
    repository.recordPlayback(video("a"), 0, repository.resetVersion.value)
    repository.recordPlayback(video("a"), -5, repository.resetVersion.value)
    assertEquals(0L, repository.snapshot().totalSeconds)

    repository.recordPlayback(video("a"), 10, repository.resetVersion.value)
    val before = repository.resetVersion.value
    repository.clear()

    assertEquals(0L, repository.snapshot().totalSeconds)
    assertEquals(before + 1, repository.resetVersion.value)
  }

  @Test
  fun statsSurviveANewRepositoryOverTheSameFile() = runBlocking {
    File(context.filesDir, "watch_stats.json").delete()
    WatchStatsRepository(context).let { it.recordPlayback(video("a"), 33, it.resetVersion.value) }
    assertEquals(33L, WatchStatsRepository(context).snapshot().totalSeconds)
  }

  @Test
  fun dropsPlaybackFromBeforeAResetAndExpiredDaysAndRemovedMedia() = runBlocking {
    File(context.filesDir, "watch_stats.json").writeText(
      """{"days":{"2000-01-01":99},"media":{"a":{"title":"a","sourceUri":"file:///v/a.mp4"},"b":{"title":"b"}}}""",
    )
    val repository = WatchStatsRepository(context)
    assertTrue(repository.snapshot().days.isEmpty())

    repository.removeMedia(setOf("file:///v/a.mp4"))
    assertEquals(setOf("b"), repository.snapshot().media.keys)

    val stale = repository.resetVersion.value
    repository.clear()
    repository.recordPlayback(video("a"), 30, stale)
    assertEquals(0L, repository.snapshot().totalSeconds)
  }
}
