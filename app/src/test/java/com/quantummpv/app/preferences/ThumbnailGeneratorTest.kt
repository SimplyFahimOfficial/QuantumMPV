package com.quantummpv.app.preferences

import android.content.Context
import com.quantummpv.app.preferences.preference.AndroidPreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class ThumbnailGeneratorTest {
  @Test
  fun newSettingKeepsTodaysBehaviourForEveryone() {
    val context = RuntimeEnvironment.getApplication()
    val storage = context.getSharedPreferences("thumbnail-generator-test", Context.MODE_PRIVATE)
    storage.edit().clear().commit()

    val preferences = BrowserPreferences(AndroidPreferenceStore(context, storage), context)

    assertEquals(ThumbnailGenerator.Native, preferences.thumbnailGenerator.get())
  }

  @Test
  fun nativeAddsNothingToTheCacheKeySoExistingThumbnailsStayValid() {
    assertEquals("", ThumbnailGenerator.Native.cacheSuffix)
  }

  @Test
  fun everyOtherSourceHasItsOwnCacheKey() {
    val suffixes = ThumbnailGenerator.entries.map { it.cacheSuffix }
    assertEquals("a source must not reuse another's images", suffixes.size, suffixes.toSet().size)
  }

  @Test
  fun sourcesAreTriedInTheDocumentedOrder() {
    assertFalse(ThumbnailGenerator.Native.triesMediaStore)
    assertTrue(ThumbnailGenerator.Native.triesNative)

    assertTrue(ThumbnailGenerator.Auto.triesMediaStore)
    assertTrue("Auto falls back to native", ThumbnailGenerator.Auto.triesNative)

    assertTrue(ThumbnailGenerator.MediaStore.triesMediaStore)
    assertFalse("MediaStore only never extracts its own", ThumbnailGenerator.MediaStore.triesNative)
  }

  @Test
  fun everySourceUsesAtLeastOneGenerator() {
    ThumbnailGenerator.entries.forEach { generator ->
      assertTrue("$generator would produce no thumbnails", generator.triesMediaStore || generator.triesNative)
    }
  }
}
