package com.quantummpv.app.ui.theme

import android.app.Activity
import android.graphics.Bitmap
import android.view.PixelCopy
import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ThemeRevealTest {
  private val origin = Offset(10f, 10f)

  @Test
  fun reducedMotionAppliesEveryChangeImmediately() {
    val reveal = ThemeReveal().apply { reduceMotion = true }
    val applied = mutableListOf<String>()
    reveal.start(origin) { applied += "dark" }
    reveal.start(origin) { applied += "amoled" }
    assertEquals(listOf("dark", "amoled"), applied)
  }

  @Test
  fun changeRequestedDuringARunningRevealIsAppliedNotDropped() {
    val reveal = ThemeReveal()
    reveal.snapshot = Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
    assertTrue(reveal.isAnimating)
    var applied = false
    reveal.start(origin) { applied = true }
    assertTrue(applied)
  }

  @Test
  fun changesRequestedWhileTheWindowIsBeingCopiedAreAppliedInOrder() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    activity.window.decorView.layout(0, 0, 100, 100)
    var finishCopy: (Int) -> Unit = {}
    val reveal =
      ThemeReveal().apply {
        window = activity.window
        copyWindow = { _, _, done -> finishCopy = done }
      }
    val applied = mutableListOf<String>()
    reveal.start(origin) { applied += "first" }
    reveal.start(origin) { applied += "second" }
    reveal.start(origin) { applied += "third" }
    // The copy is still pending, so no change has been applied yet.
    assertTrue(applied.isEmpty())
    finishCopy(PixelCopy.SUCCESS)
    assertEquals(listOf("first", "second", "third"), applied)
    assertTrue(reveal.isAnimating)
  }

  @Test
  fun failedCopyStillAppliesEveryChange() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    activity.window.decorView.layout(0, 0, 100, 100)
    var finishCopy: (Int) -> Unit = {}
    val reveal =
      ThemeReveal().apply {
        window = activity.window
        copyWindow = { _, _, done -> finishCopy = done }
      }
    val applied = mutableListOf<String>()
    reveal.start(origin) { applied += "first" }
    reveal.start(origin) { applied += "second" }
    finishCopy(PixelCopy.ERROR_UNKNOWN)
    assertEquals(listOf("first", "second"), applied)
    assertFalse(reveal.isAnimating)
  }

  @Test
  fun aNewRequestAfterTheCopyFinishesStartsNormally() {
    val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
    activity.window.decorView.layout(0, 0, 100, 100)
    var finishCopy: (Int) -> Unit = {}
    val reveal =
      ThemeReveal().apply {
        window = activity.window
        copyWindow = { _, _, done -> finishCopy = done }
      }
    reveal.start(origin) {}
    finishCopy(PixelCopy.ERROR_UNKNOWN)
    var applied = false
    reveal.start(origin) { applied = true }
    assertFalse("a second capture should be pending, not applied", applied)
    finishCopy(PixelCopy.ERROR_UNKNOWN)
    assertTrue(applied)
  }
}
