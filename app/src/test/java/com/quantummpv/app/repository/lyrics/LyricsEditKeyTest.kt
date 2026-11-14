package com.quantummpv.app.repository.lyrics

import com.quantummpv.app.data.lyrics.LrcLibApiService
import com.quantummpv.app.domain.lyrics.Lyrics
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class LyricsEditKeyTest {
  private val repository = LyricsRepository(RuntimeEnvironment.getApplication(), mockk<LrcLibApiService>(relaxed = true))

  @Test
  fun editsStayWithTheirSourceEvenWhenTheRuntimePathIsReused() = runBlocking {
    // Two tracks reopened through the same fd:// path must not share a cache entry or an edit.
    repository.saveEditedLyrics("content://a", Lyrics(plain = listOf("A edited")))

    val a = repository.loadLyricsForTrack("fd://5", title = null, artist = null, editKey = "content://a")
    val b = repository.loadLyricsForTrack("fd://5", title = null, artist = null, editKey = "content://b")

    assertEquals(listOf("A edited"), a.activeLyrics?.plain)
    assertEquals(true, a.hasSavedEdits)
    assertNull(b.activeLyrics)
    assertFalse(b.hasSavedEdits)
  }

  @Test
  fun removingAnEditIsSeenByTheNextLoad() = runBlocking {
    repository.saveEditedLyrics("content://c", Lyrics(plain = listOf("C edited")))
    repository.loadLyricsForTrack("fd://9", title = null, artist = null, editKey = "content://c")

    repository.removeEditedLyrics("content://c")

    val after = repository.loadLyricsForTrack("fd://9", title = null, artist = null, editKey = "content://c")
    assertNull(after.activeLyrics)
    assertFalse(after.hasSavedEdits)
  }
}
