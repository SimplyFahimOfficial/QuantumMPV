package com.quantummpv.app.preferences

import android.content.Context
import com.quantummpv.app.preferences.preference.AndroidPreferenceStore
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
class AudioPreferencesTest {
  private lateinit var preferences: AudioPreferences

  @Before
  fun setUp() {
    val context = RuntimeEnvironment.getApplication()
    val storage = context.getSharedPreferences("audio-preferences-test", Context.MODE_PRIVATE)
    storage.edit().clear().commit()
    preferences = AudioPreferences(AndroidPreferenceStore(context, storage))
  }

  @Test
  fun playerKeepsTakingAudioFocusUntilTheUserOptsOut() {
    assertFalse(preferences.ignoreAudioFocus.get())
  }

  @Test
  fun ignoringAudioFocusSurvivesBeingReadAgain() {
    preferences.ignoreAudioFocus.set(true)
    assertTrue(preferences.ignoreAudioFocus.get())
    preferences.ignoreAudioFocus.delete()
    assertFalse(preferences.ignoreAudioFocus.get())
  }
}
