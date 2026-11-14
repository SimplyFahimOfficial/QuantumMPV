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
    repository.recordPlayback(video("a"), 40)
    repository.recordPlayback(audio("b"), 25)

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
    repository.recordPlayback(video("a"), 0)
    repository.recordPlayback(video("a"), -5)
    assertEquals(0L, repository.snapshot().totalSeconds)

    repository.recordPlayback(video("a"), 10)
    val before = repository.resetVersion.value
    repository.clear()

    assertEquals(0L, repository.snapshot().totalSeconds)
    assertEquals(before + 1, repository.resetVersion.value)
  }

  @Test
  fun statsSurviveANewRepositoryOverTheSameFile() = runBlocking {
    File(context.filesDir, "watch_stats.json").delete()
    WatchStatsRepository(context).recordPlayback(video("a"), 33)
    assertEquals(33L, WatchStatsRepository(context).snapshot().totalSeconds)
  }
}
