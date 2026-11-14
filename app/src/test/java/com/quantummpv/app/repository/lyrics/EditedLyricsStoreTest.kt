package com.quantummpv.app.repository.lyrics

import com.quantummpv.app.domain.lyrics.Lyrics
import com.quantummpv.app.domain.lyrics.LyricsSourceType
import com.quantummpv.app.domain.lyrics.SyncedLine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
class EditedLyricsStoreTest {
  @get:Rule val folder = TemporaryFolder()

  private val lyrics = Lyrics(synced = listOf(SyncedLine(1000, "a"), SyncedLine(2000, "b")))

  @Test
  fun writesReadsAndRemoves() {
    val store = EditedLyricsStore(File(folder.root, "lyrics"))
    assertNull(store.read("/music/a.mp3"))
    assertFalse(store.contains("/music/a.mp3"))
    store.write("/music/a.mp3", lyrics)
    assertTrue(store.contains("/music/a.mp3"))
    val read = store.read("/music/a.mp3")!!
    assertEquals(listOf("a", "b"), read.synced!!.map { it.line })
    assertEquals(LyricsSourceType.LOCAL, read.sourceType)
    assertNull(store.read("/music/b.mp3")) // another track is unaffected
    store.remove("/music/a.mp3")
    assertFalse(store.contains("/music/a.mp3"))
  }

  @Test
  fun refusesEmptyLyricsAndBlankPaths() {
    val store = EditedLyricsStore(File(folder.root, "lyrics"))
    try {
      store.write("/music/a.mp3", Lyrics())
      fail("empty lyrics were accepted")
    } catch (_: IllegalArgumentException) {
    }
    try {
      store.write(" ", lyrics)
      fail("blank path was accepted")
    } catch (_: IllegalArgumentException) {
    }
  }

  @Test
  fun corruptFileIsReportedAndKept() {
    val dir = File(folder.root, "lyrics").apply { mkdirs() }
    val store = EditedLyricsStore(dir)
    store.write("/music/a.mp3", lyrics)
    val file = dir.listFiles()!!.single { it.extension == "json" }
    file.writeText("not json")
    try {
      store.read("/music/a.mp3")
      fail("corrupt file was read")
    } catch (_: IOException) {
    }
    assertTrue(file.exists()) // never deleted just because it could not be read
    assertTrue(store.contains("/music/a.mp3"))
  }
}
