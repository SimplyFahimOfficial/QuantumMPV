package com.quantummpv.app.utils.media

import org.junit.Assert.assertEquals
import org.junit.Test

class MediaPathUtilsTest {
  @Test
  fun hashtagInFileNameKeepsTheExtension() {
    assertEquals("mp4", "/Audio/clip #edit.mp4".fileExtension())
    assertEquals("mkv", "file:///sdcard/Movies/a #1.mkv".fileExtension())
  }

  @Test
  fun queryAndFragmentAreStrippedFromUrls() {
    assertEquals("mkv", "video.mkv?quality=1080#t=10".fileExtension())
    assertEquals("mp4", "https://example.com/a/b.mp4?token=1.5".fileExtension())
  }

  @Test
  fun noExtensionGivesEmpty() {
    assertEquals("", "content://media/123".fileExtension())
    assertEquals("", "https://example.com/stream".fileExtension())
    assertEquals("", "/dir.d/file".fileExtension())
    assertEquals("", "name.".fileExtension())
  }
}
