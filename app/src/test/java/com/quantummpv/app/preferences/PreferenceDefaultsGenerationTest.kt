package com.quantummpv.app.preferences

import android.content.Context
import android.content.SharedPreferences
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
class PreferenceDefaultsGenerationTest {
  private lateinit var context: Context
  private lateinit var prefs: SharedPreferences

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    prefs = context.getSharedPreferences("defaults_generation_test", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
  }

  private fun appearance() = AppearancePreferences(AndroidPreferenceStore(context, prefs))

  private fun player() = PlayerPreferences(AndroidPreferenceStore(context, prefs))

  @Test
  fun freshInstallGetsTheFullLayout() {
    PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = false)

    val appearance = appearance()
    assertEquals(AppearancePreferences.DEFAULT_TOP_RIGHT_CONTROLS, appearance.topRightControls.get())
    assertEquals(AppearancePreferences.DEFAULT_PORTRAIT_BOTTOM_CONTROLS, appearance.portraitBottomControls.get())
    assertTrue(player().showControlsDrawer.get())
    assertEquals(PreferenceDefaultsGeneration.CURRENT, prefs.getInt(PreferenceDefaultsGeneration.KEY, 0))
  }

  @Test
  fun existingInstallKeepsTheDefaultsItStartedWith() {
    // Used before, never touched the layout: only an unrelated setting is stored.
    prefs.edit().putBoolean("amoled_mode", true).commit()

    PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = true)

    val appearance = appearance()
    assertEquals(
      PreferenceDefaultsGeneration.legacyDefault("top_right_controls"),
      appearance.topRightControls.get(),
    )
    assertEquals(
      PreferenceDefaultsGeneration.legacyDefault("portrait_bottom_controls"),
      appearance.portraitBottomControls.get(),
    )
    assertTrue(player().showControlsDrawer.get())
  }

  @Test
  fun savedLayoutsAreNeverRewritten() {
    prefs
      .edit()
      .putString("top_right_controls", "SUBTITLES")
      .putString("portrait_bottom_controls", "AUDIO_TRACK,MORE_OPTIONS")
      .putBoolean("cast_button_migration_complete", true)
      .commit()

    PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = true)
    val appearance = appearance()

    assertEquals("SUBTITLES", appearance.topRightControls.get())
    assertEquals("AUDIO_TRACK,MORE_OPTIONS", appearance.portraitBottomControls.get())
    // Cast and Clip are no longer forced into saved layouts.
    assertFalse("CAST" in appearance.topRightControls.get())
    assertFalse("CLIP" in appearance.portraitBottomControls.get())
    assertFalse(prefs.contains("cast_button_migration_complete"))
  }

  @Test
  fun emptyPreferencesWithExistingDatabaseCountAsExistingInstall() {
    PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = true)

    assertEquals(PreferenceDefaultsGeneration.legacyDefault("show_controls_drawer"), player().showControlsDrawer.get())
  }

  @Test
  fun runningAgainChangesNothing() {
    PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = false)
    appearance().topRightControls.set("AUDIO_TRACK")

    val from = PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = true)

    assertEquals(PreferenceDefaultsGeneration.CURRENT, from)
    assertEquals("AUDIO_TRACK", appearance().topRightControls.get())
    assertTrue(player().showControlsDrawer.get())
  }

  @Test
  fun recommendedPresetReplacesEveryRegionWithTheCompactLayout() {
    prefs.edit().putBoolean("amoled_mode", true).commit()
    PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = true)
    val appearance = appearance()

    appearance.resetPlayerLayoutToRecommended()

    assertEquals(AppearancePreferences.COMPACT_TOP_RIGHT_CONTROLS, appearance.topRightControls.get())
    assertEquals(AppearancePreferences.COMPACT_BOTTOM_RIGHT_CONTROLS, appearance.bottomRightControls.get())
    assertEquals(AppearancePreferences.COMPACT_BOTTOM_LEFT_CONTROLS, appearance.bottomLeftControls.get())
    assertEquals(AppearancePreferences.COMPACT_PORTRAIT_BOTTOM_CONTROLS, appearance.portraitBottomControls.get())
  }

  @Test
  fun generationMarkerIsNotPartOfSettingsBackups() {
    assertTrue(SettingsBackupPolicy.isProtectedKey(PreferenceDefaultsGeneration.KEY))
  }

  @Test
  fun resetToDefaultRestoresTheFullLayoutInEveryRegion() {
    appearance().resetPlayerLayoutToRecommended()
    val appearance = appearance()

    appearance.resetPlayerLayoutToDefault()

    assertEquals(AppearancePreferences.DEFAULT_TOP_LEFT_CONTROLS, appearance.topLeftControls.get())
    assertEquals(AppearancePreferences.DEFAULT_TOP_RIGHT_CONTROLS, appearance.topRightControls.get())
    assertEquals(AppearancePreferences.DEFAULT_BOTTOM_RIGHT_CONTROLS, appearance.bottomRightControls.get())
    assertEquals(AppearancePreferences.DEFAULT_BOTTOM_LEFT_CONTROLS, appearance.bottomLeftControls.get())
    assertEquals(AppearancePreferences.DEFAULT_PORTRAIT_BOTTOM_CONTROLS, appearance.portraitBottomControls.get())
  }

  @Test
  fun everyDefaultButtonNameExistsAndIsUsedOnceAcrossLandscape() {
    // A misspelt name is silently dropped from the player, so every token must be a real button.
    val landscape =
      listOf(
        AppearancePreferences.DEFAULT_TOP_LEFT_CONTROLS,
        AppearancePreferences.DEFAULT_TOP_RIGHT_CONTROLS,
        AppearancePreferences.DEFAULT_BOTTOM_RIGHT_CONTROLS,
        AppearancePreferences.DEFAULT_BOTTOM_LEFT_CONTROLS,
      ).flatMap { it.split(",") }
    (landscape + AppearancePreferences.DEFAULT_PORTRAIT_BOTTOM_CONTROLS.split(",")).forEach(PlayerButton::valueOf)
    assertEquals("a button appears in two landscape regions", landscape.size, landscape.toSet().size)
    val portrait = AppearancePreferences.DEFAULT_PORTRAIT_BOTTOM_CONTROLS.split(",")
    assertEquals(portrait.size, portrait.toSet().size)
  }

  @Test
  fun generationTwoInstallThatNeverCustomizedFollowsTheNewDefault() {
    // A generation-2 install with the compact layout saved only because the editor was opened and closed.
    prefs
      .edit()
      .putInt(PreferenceDefaultsGeneration.KEY, 2)
      .putString("top_right_controls", AppearancePreferences.COMPACT_TOP_RIGHT_CONTROLS)
      .putString("portrait_bottom_controls", AppearancePreferences.COMPACT_PORTRAIT_BOTTOM_CONTROLS)
      .commit()

    val from = PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = true)

    assertEquals(2, from)
    val appearance = appearance()
    assertEquals(AppearancePreferences.DEFAULT_TOP_RIGHT_CONTROLS, appearance.topRightControls.get())
    assertEquals(AppearancePreferences.DEFAULT_PORTRAIT_BOTTOM_CONTROLS, appearance.portraitBottomControls.get())
    assertTrue(player().showControlsDrawer.get())
    assertEquals(3, prefs.getInt(PreferenceDefaultsGeneration.KEY, 0))
  }

  @Test
  fun generationTwoInstallKeepsACustomizedRegionAndMigratesTheRest() {
    prefs
      .edit()
      .putInt(PreferenceDefaultsGeneration.KEY, 2)
      .putString("top_right_controls", "SUBTITLES,CLIP,MORE_OPTIONS")
      .putString("bottom_left_controls", AppearancePreferences.COMPACT_BOTTOM_LEFT_CONTROLS)
      .putBoolean("show_controls_drawer", false)
      .commit()

    PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = true)

    val appearance = appearance()
    assertEquals("SUBTITLES,CLIP,MORE_OPTIONS", appearance.topRightControls.get())
    assertEquals(AppearancePreferences.DEFAULT_BOTTOM_LEFT_CONTROLS, appearance.bottomLeftControls.get())
    // An explicit drawer choice is a preference, not a default.
    assertFalse(player().showControlsDrawer.get())
  }

  @Test
  fun legacyInstallStillEndsOnTheFullLayout() {
    prefs.edit().putBoolean("amoled_mode", true).commit()

    PreferenceDefaultsGeneration.apply(prefs, hasOtherAppData = true)

    assertEquals(AppearancePreferences.DEFAULT_BOTTOM_LEFT_CONTROLS, appearance().bottomLeftControls.get())
    assertEquals(
      PreferenceDefaultsGeneration.legacyDefault("bottom_left_controls"),
      appearance().bottomLeftControls.get(),
    )
  }
}
