package com.quantummpv.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerGlassPolicyTest {
  private fun texture(
    glass: Boolean = true,
    sdk: Int = 34,
    hdr: Boolean = false,
    confOwnsHdr: Boolean = false,
    inHdr: Boolean = false,
    ambient: Boolean = false,
  ) = PlayerGlassPolicy.usesTextureSurface(glass, sdk, hdr, confOwnsHdr, inHdr, ambient)

  @Test
  fun offByDefaultSoPlaybackKeepsTheNativeSurface() {
    assertFalse(texture(glass = false))
  }

  @Test
  fun sdrContentOnAndroid12OrNewerUsesTheTexture() {
    assertTrue(texture())
    assertTrue(texture(sdk = PlayerGlassPolicy.MIN_SDK))
  }

  @Test
  fun androidBeforeTheBlurApiNeverUsesTheTexture() {
    assertFalse(texture(sdk = PlayerGlassPolicy.MIN_SDK - 1))
    assertFalse(texture(sdk = 26))
  }

  @Test
  fun hdrOutputKeepsTheNativeSurfaceUnlessTheUserAcceptedToneMapping() {
    assertFalse(texture(hdr = true))
    assertTrue(texture(hdr = true, inHdr = true))
  }

  @Test
  fun anMpvConfThatOwnsHdrIsNeverOverriddenByAPreference() {
    assertFalse(texture(confOwnsHdr = true))
    assertFalse(texture(confOwnsHdr = true, inHdr = true))
    assertFalse(texture(hdr = true, confOwnsHdr = true, inHdr = true))
  }

  @Test
  fun theAmbientBackgroundKeepsItsSurfaceView() {
    assertFalse(texture(ambient = true))
    assertFalse(texture(ambient = true, hdr = true, inHdr = true))
  }

  @Test
  fun theOptInForHdrAloneNeverTurnsGlassOn() {
    assertFalse(texture(glass = false, inHdr = true, hdr = true))
    assertEquals(false, texture(glass = false, sdk = 34, inHdr = true))
  }
}
