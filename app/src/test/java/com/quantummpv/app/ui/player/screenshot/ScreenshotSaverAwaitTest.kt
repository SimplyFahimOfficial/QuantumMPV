package com.quantummpv.app.ui.player.screenshot

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ScreenshotSaverAwaitTest {
  @get:Rule
  val folder = TemporaryFolder()

  @Test
  fun returnsAFinishedFileOnceItStopsGrowing() =
    runBlocking {
      val file = File(folder.root, "shot.png")
      file.writeBytes(ByteArray(100))

      assertEquals(file, ScreenshotSaver.awaitScreenshotFile(file))
    }

  @Test
  fun waitsForMpvToFinishWritingInsteadOfReturningAPartialImage() =
    runBlocking {
      val file = File(folder.root, "slow.png")
      val firstChunkWritten = CompletableDeferred<Unit>()
      val continueWriting = CompletableDeferred<Unit>()
      val keepWriterOpen = CompletableDeferred<Unit>()
      val firstLengthRead = CompletableDeferred<Unit>()
      val writer =
        launch {
          file.writeBytes(ByteArray(1_000))
          firstChunkWritten.complete(Unit)
          continueWriting.await()
          file.appendBytes(ByteArray(3_000))
          keepWriterOpen.await()
        }
      firstChunkWritten.await()
      val observedFile =
        object : File(file.absolutePath) {
          override fun length(): Long {
            firstLengthRead.complete(Unit)
            return super.length()
          }
        }
      val writerCoordinator =
        launch {
          firstLengthRead.await()
          continueWriting.complete(Unit)
        }

      try {
        val result = ScreenshotSaver.awaitScreenshotFile(observedFile)
        assertNotNull(result)
        assertEquals("the whole image must be present when await returns", 4_000L, file.length())
        assertTrue("the assertion must run before waiting for the writer", writer.isActive)
      } finally {
        continueWriting.complete(Unit)
        keepWriterOpen.complete(Unit)
        writer.join()
        writerCoordinator.join()
      }
    }

  @Test
  fun givesUpWithNullWhenMpvNeverWritesAnything() =
    runBlocking {
      assertNull(ScreenshotSaver.awaitScreenshotFile(File(folder.root, "missing.png")))
    }
}
