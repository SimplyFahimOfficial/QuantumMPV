package com.quantummpv.app.ui.browser.music

import android.graphics.Bitmap
import android.net.Uri
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import java.io.File

@RunWith(RobolectricTestRunner::class)
class AlbumArtDecodeTest {
  private val context = RuntimeEnvironment.getApplication()

  private fun cover(size: Int): Uri {
    val file = File(context.cacheDir, "cover$size.png")
    Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888).compress(Bitmap.CompressFormat.PNG, 100, file.outputStream())
    return Uri.fromFile(file)
  }

  @Test
  fun largeCoversAreDownsampledButStayAtLeastTheTargetEdge() {
    val decoded = decodeAlbumArt(context, cover(3000))
    assertNotNull(decoded)
    assertTrue(decoded!!.width in 1024..1500)
  }

  @Test
  fun smallCoversAreKeptAtFullSize() {
    assertTrue(decodeAlbumArt(context, cover(300))!!.width == 300)
  }
}
