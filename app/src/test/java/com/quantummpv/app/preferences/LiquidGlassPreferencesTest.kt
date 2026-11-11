package com.quantummpv.app.preferences

import android.content.Context
import com.quantummpv.app.preferences.preference.AndroidPreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class LiquidGlassPreferencesTest {
  private lateinit var preferences: AppearancePreferences

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    val storage = context.getSharedPreferences("liquid-glass-test", Context.MODE_PRIVATE)
    storage.edit().clear().commit()
    preferences = AppearancePreferences(AndroidPreferenceStore(context, storage))
  }

  @Test
  fun liquidGlassIsOffUntilTheUserTurnsItOn() {
    assertFalse(preferences.liquidGlassEnabled.get())
  }

  @Test
  fun defaultsAreNeutralSoTheGlassIsUntouchedUntilTuned() {
    assertEquals(1f, preferences.liquidGlassOpacity.get(), 0f)
    assertEquals(1f, preferences.liquidGlassBlur.get(), 0f)
    assertEquals(1f, preferences.liquidGlassSaturation.get(), 0f)
    assertEquals(0f, preferences.liquidGlassBrightness.get(), 0f)
    assertEquals(1f, preferences.liquidGlassContrast.get(), 0f)
    assertTrue(preferences.liquidGlassVibrancy.get())
    assertTrue(preferences.liquidGlassChromaticAberration.get())
    assertFalse(preferences.liquidGlassDepthEffect.get())
    assertEquals(LiquidGlassHighlightStyle.Component, preferences.liquidGlassHighlightStyle.get())
  }

  @Test
  fun resetRestoresEveryTuningOptionButKeepsTheSwitch() {
    preferences.liquidGlassEnabled.set(true)
    preferences.liquidGlassOpacity.set(0.4f)
    preferences.liquidGlassBlur.set(1.8f)
    preferences.liquidGlassRefractionHeight.set(0f)
    preferences.liquidGlassRefractionAmount.set(1.5f)
    preferences.liquidGlassDepthEffect.set(true)
    preferences.liquidGlassChromaticAberration.set(false)
    preferences.liquidGlassVibrancy.set(false)
    preferences.liquidGlassSaturation.set(0.2f)
    preferences.liquidGlassBrightness.set(-0.5f)
    preferences.liquidGlassContrast.set(1.7f)
    preferences.liquidGlassHighlightStyle.set(LiquidGlassHighlightStyle.Plain)
    preferences.liquidGlassHighlightStrength.set(0f)
    preferences.liquidGlassHighlightWidth.set(2f)
    preferences.liquidGlassHighlightBlur.set(0.3f)

    preferences.resetLiquidGlassOptions()

    assertTrue("the on/off switch is not a tuning option", preferences.liquidGlassEnabled.get())
    assertEquals(1f, preferences.liquidGlassOpacity.get(), 0f)
    assertEquals(1f, preferences.liquidGlassBlur.get(), 0f)
    assertEquals(1f, preferences.liquidGlassRefractionHeight.get(), 0f)
    assertEquals(1f, preferences.liquidGlassRefractionAmount.get(), 0f)
    assertFalse(preferences.liquidGlassDepthEffect.get())
    assertTrue(preferences.liquidGlassChromaticAberration.get())
    assertTrue(preferences.liquidGlassVibrancy.get())
    assertEquals(1f, preferences.liquidGlassSaturation.get(), 0f)
    assertEquals(0f, preferences.liquidGlassBrightness.get(), 0f)
    assertEquals(1f, preferences.liquidGlassContrast.get(), 0f)
    assertEquals(LiquidGlassHighlightStyle.Component, preferences.liquidGlassHighlightStyle.get())
    assertEquals(1f, preferences.liquidGlassHighlightStrength.get(), 0f)
    assertEquals(1f, preferences.liquidGlassHighlightWidth.get(), 0f)
    assertEquals(1f, preferences.liquidGlassHighlightBlur.get(), 0f)
  }
}
