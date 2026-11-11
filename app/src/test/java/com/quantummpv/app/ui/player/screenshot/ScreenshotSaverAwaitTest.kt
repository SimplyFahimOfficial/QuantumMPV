package com.quantummpv.app.ui.player.screenshot

import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
      // mpv writes a large frame as a burst of chunks; the file must not be returned after the first one.
      // The chunks arrive faster than the wait's poll interval, which is what a real encode looks like.
      val writer =
        launch {
          for (step in 1..4) {
            file.appendBytes(ByteArray(1_000))
            delay(5)
          }
        }

      val result = ScreenshotSaver.awaitScreenshotFile(file)
      writer.join()

      assertNotNull(result)
      assertEquals("the whole image must have been written", 4_000L, file.length())
    }

  @Test
  fun givesUpWithNullWhenMpvNeverWritesAnything() =
    runBlocking {
      assertNull(ScreenshotSaver.awaitScreenshotFile(File(folder.root, "missing.png")))
    }
}
