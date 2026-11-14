package com.quantummpv.app.domain.lyrics

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsDraftTest {
  private fun synced(vararg lines: Pair<Int, String>) =
    Lyrics(synced = lines.map { SyncedLine(it.first, it.second) }, sourceType = LyricsSourceType.LOCAL)

  @Test
  fun timeParsingAndFormattingRoundTrip() {
    assertEquals("01:02.345", LyricsDraft.formatTime(62_345))
    assertEquals(62_345, LyricsDraft.parseTime("01:02.345"))
    assertEquals(62_345, LyricsDraft.parseTime("[01:02.345]"))
    assertEquals(62_500, LyricsDraft.parseTime("1:02.5"))
    assertEquals(62_000, LyricsDraft.parseTime("1:02"))
    assertNull(LyricsDraft.parseTime("1:60.000"))
    assertNull(LyricsDraft.parseTime("abc"))
    assertNull(LyricsDraft.parseTime("99999:59.999"))
  }

  @Test
  fun validationRules() {
    assertEquals(LyricsDraftError.EMPTY, LyricsDraft.from(null).validationError())
    assertNull(LyricsDraft.from(synced(1000 to "a", 2000 to "b")).validationError())
    val mixed = LyricsDraft.from(synced(1000 to "a", 2000 to "b")).setTime(1, null)
    assertEquals(LyricsDraftError.INCOMPLETE_TIMING, mixed.validationError())
    val backwards = LyricsDraft.from(synced(2000 to "a", 1000 to "b"))
    assertEquals(LyricsDraftError.OUT_OF_ORDER, backwards.validationError())
    assertNull(LyricsDraft.from(synced(1000 to "a", 2000 to "b")).clearTimes().validationError())
  }

  @Test
  fun movingALineKeepsItsWordTimingRelativeAndDroppingItWhenTextChanges() {
    val original =
      SyncedLine(
        time = 1000,
        line = "hello world",
        words = listOf(SyncedWord(1000, "hello"), SyncedWord(1500, "world")),
      )
    val draft = LyricsDraft.from(Lyrics(synced = listOf(original)))
    val moved = draft.setTime(0, 3000).toLyrics().synced!!.single()
    assertEquals(3000, moved.time)
    assertEquals(listOf(3000, 3500), moved.words!!.map { it.time })
    val reworded = draft.editText(0, "hello there").toLyrics().synced!!.single()
    assertNull(reworded.words)
  }

  @Test
  fun editingOperations() {
    var draft = LyricsDraft.from(synced(1000 to "a", 2000 to "b"))
    draft = draft.insertAfter(0)
    assertEquals(3, draft.lines.size)
    assertEquals(3, draft.lines.map { it.id }.toSet().size)
    draft = draft.delete(1).delete(0).delete(0)
    assertEquals(1, draft.lines.size) // never empty
    assertTrue(LyricsDraft.from(null).hasSameContent(draft))
    assertFalse(LyricsDraft.from(synced(1000 to "a")).hasSameContent(LyricsDraft.from(synced(1000 to "b"))))
  }

  @Test
  fun plainTextStaysPlainAndIsMarkedLocal() {
    val lyrics = LyricsDraft.from(Lyrics(plain = listOf("one", "", "two"))).toLyrics()
    assertNull(lyrics.synced)
    assertEquals(listOf("one", "two"), lyrics.plain)
    assertEquals(LyricsSourceType.LOCAL, lyrics.sourceType)
    assertFalse(lyrics.areFromRemote)
  }
}
