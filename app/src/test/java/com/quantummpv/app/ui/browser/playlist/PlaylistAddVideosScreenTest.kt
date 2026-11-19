package com.quantummpv.app.ui.browser.playlist

import com.quantummpv.app.domain.media.model.VideoFolder
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaylistAddVideosScreenTest {
  private fun folder(id: String) = VideoFolder(bucketId = id, name = "F$id", path = "/m/$id", videoCount = 1)

  @Test
  fun savedBucketIdIsResolvedAgainstTheCurrentFolderList() {
    val refreshed = listOf(folder("1"), folder("2").copy(videoCount = 9))
    assertEquals(9, resolveSavedFolder(refreshed, "2")!!.videoCount)
  }

  @Test
  fun noSavedIdOrAnUnknownIdResolvesToNothing() {
    assertNull(resolveSavedFolder(listOf(folder("1")), null))
    assertNull(resolveSavedFolder(listOf(folder("1")), "gone"))
    assertNull(resolveSavedFolder(emptyList(), "1"))
  }

  @Test
  fun aSavedFolderIsKeptWhileTheListIsStillLoadingAndDroppedOnceItIsKnownMissing() {
    assertFalse(shouldDropSavedFolder("1", resolved = null, foldersLoaded = false))
    assertTrue(shouldDropSavedFolder("1", resolved = null, foldersLoaded = true))
    assertFalse(shouldDropSavedFolder("1", resolved = folder("1"), foldersLoaded = true))
    assertFalse(shouldDropSavedFolder(null, resolved = null, foldersLoaded = true))
  }
}
