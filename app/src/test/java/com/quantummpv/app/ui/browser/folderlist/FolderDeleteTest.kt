package com.quantummpv.app.ui.browser.folderlist

import com.quantummpv.app.R
import com.quantummpv.app.ui.browser.selection.deleteResultMessage
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FolderDeleteTest {
  @get:Rule val tmp = TemporaryFolder()

  private fun folder(vararg names: String): File =
    tmp.newFolder().also { dir -> names.forEach { File(dir, it).writeText("x") } }

  @Test
  fun videoOnlyDeletionKeepsAudioAndUnrelatedFiles() {
    val dir = folder("a.mp4", "b.mkv", "song.mp3", "notes.txt", "cover.jpg")
    assertEquals(FolderDeleteOutcome.DELETED, deleteMediaFilesIn(dir, includeVideo = true, includeAudio = false))
    assertEquals(setOf("song.mp3", "notes.txt", "cover.jpg"), dir.list()!!.toSet())
  }

  @Test
  fun audioOnlyDeletionNeverTouchesVideo() {
    val dir = folder("a.mp4", "song.mp3", "t.flac")
    assertEquals(FolderDeleteOutcome.DELETED, deleteMediaFilesIn(dir, includeVideo = false, includeAudio = true))
    assertEquals(setOf("a.mp4"), dir.list()!!.toSet())
  }

  @Test
  fun subfoldersWithMediaNamesAreNotDeleted() {
    val dir = folder("a.mp4")
    File(dir, "inner.mp4").mkdir()
    deleteMediaFilesIn(dir, includeVideo = true, includeAudio = false)
    assertTrue(File(dir, "inner.mp4").isDirectory)
  }

  @Test
  fun folderWithNothingToDeleteOrUnreadableIsAFailureNotASuccess() {
    assertEquals(FolderDeleteOutcome.FAILED, deleteMediaFilesIn(folder("notes.txt"), includeVideo = true, includeAudio = true))
    assertEquals(FolderDeleteOutcome.FAILED, deleteMediaFilesIn(File(tmp.root, "missing"), includeVideo = true, includeAudio = true))
  }

  @Test
  fun wholeFolderOutcomeSeparatesSuccessPartialAndFailure() {
    assertEquals(FolderDeleteOutcome.DELETED, wholeFolderOutcome(3, 0, dirRemoved = true))
    assertEquals(FolderDeleteOutcome.PARTIAL, wholeFolderOutcome(2, 1, dirRemoved = false))
    assertEquals(FolderDeleteOutcome.PARTIAL, wholeFolderOutcome(0, 1, dirRemoved = true))
    assertEquals(FolderDeleteOutcome.FAILED, wholeFolderOutcome(0, 2, dirRemoved = false))
    assertEquals(FolderDeleteOutcome.PARTIAL, wholeFolderOutcome(3, 0, dirRemoved = false))
  }

  @Test
  fun countsAreFolderLevelAndAPartialFolderCountsOnBothSides() {
    val outcomes = listOf(FolderDeleteOutcome.DELETED, FolderDeleteOutcome.PARTIAL, FolderDeleteOutcome.FAILED)
    assertEquals(2 to 2, outcomes.toDeleteCounts())
  }

  @Test
  fun toastReportsPartialInsteadOfPlainSuccess() {
    assertEquals(R.string.ui_delete_partial, deleteResultMessage(2, 1, announceSuccess = true))
    assertEquals(R.string.ui_deleted_successfully, deleteResultMessage(2, 0, announceSuccess = true))
    assertEquals(R.string.ui_failed_to_delete, deleteResultMessage(0, 2, announceSuccess = true))
    assertEquals(R.string.ui_failed_to_delete, deleteResultMessage(2, 1, announceSuccess = false))
    assertNull(deleteResultMessage(2, 0, announceSuccess = false))
    assertNull(deleteResultMessage(0, 0, announceSuccess = true))
    assertFalse(deleteResultMessage(1, 1, true) == R.string.ui_deleted_successfully)
  }
}
