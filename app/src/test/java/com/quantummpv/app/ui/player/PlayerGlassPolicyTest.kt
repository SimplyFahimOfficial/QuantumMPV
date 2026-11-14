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
    youTubeAmbient: Boolean = false,
    dolbyVisionDirect: Boolean = false,
  ) = PlayerGlassPolicy.usesTextureSurface(glass, sdk, hdr, confOwnsHdr, inHdr, youTubeAmbient, dolbyVisionDirect)

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
  fun theYouTubeAmbientBackgroundKeepsItsSurfaceView() {
    assertFalse(texture(youTubeAmbient = true))
    assertFalse(texture(youTubeAmbient = true, hdr = true, inHdr = true))
  }

  @Test
  fun directDolbyVisionNeedsTheRealSurfaceSoItAlwaysBeatsGlass() {
    assertFalse(texture(dolbyVisionDirect = true))
    assertFalse(texture(dolbyVisionDirect = true, hdr = true, inHdr = true))
    // Turning the option off gives glass back.
    assertTrue(texture(dolbyVisionDirect = false))
  }

  @Test
  fun theAnswerOnlyDependsOnSettingsSoSwitchingSurfacesCannotChangeIt() {
    // The ambient capture needs a bound SurfaceView, so it appears and disappears as the renderer switches.
    // If the renderer were chosen from that, it would flip back and forth. The policy has no such input:
    // identical settings must always give the identical answer.
    val flags = listOf(true, false)
    val combinations =
      flags.flatMap { glass ->
        flags.flatMap { hdr ->
          flags.flatMap { conf ->
            flags.flatMap { inHdr -> flags.map { ambient -> listOf(glass, hdr, conf, inHdr, ambient) } }
          }
        }
      }
    assertEquals(32, combinations.size)
    combinations.forEach { (glass, hdr, conf, inHdr, ambient) ->
      val first = texture(glass, 34, hdr, conf, inHdr, ambient)
      repeat(3) { assertEquals(first, texture(glass, 34, hdr, conf, inHdr, ambient)) }
    }
  }

  @Test
  fun addingAnInputToTheRuleMeansReadingThisFirst() {
    // The ambient capture only works while a SurfaceView is bound, so it appears and disappears as the renderer
    // switches. An input such as "a frame was captured" or "the surface is attached" would therefore make the
    // rule flip the renderer back and forth (found in review). Every input must be a stored setting or a device
    // fact. If this fails because a parameter was added, check that it is neither before updating the list.
    val shape =
      PlayerGlassPolicy::class.java.declaredMethods
        .single { it.name == "usesTextureSurface" }
        .parameterTypes
        .map { it.simpleName }
    // dolbyVisionDirect is a stored setting, so it is allowed; it was added with the Dolby Vision option.
    assertEquals(listOf("boolean", "int", "boolean", "boolean", "boolean", "boolean", "boolean"), shape)
  }

  @Test
  fun theOptInForHdrAloneNeverTurnsGlassOn() {
    assertFalse(texture(glass = false, inHdr = true, hdr = true))
    assertEquals(false, texture(glass = false, sdk = 34, inHdr = true))
  }
}
