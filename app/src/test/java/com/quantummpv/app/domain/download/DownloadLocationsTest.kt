package com.quantummpv.app.domain.download

import android.content.Context
import com.quantummpv.app.preferences.DownloadPreferences
import com.quantummpv.app.preferences.preference.AndroidPreferenceStore
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class DownloadLocationsTest {
  @get:Rule val tmp = TemporaryFolder()
  private lateinit var context: Context
  private lateinit var preferences: DownloadPreferences
  private lateinit var locations: DownloadLocations

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    val prefs = context.getSharedPreferences("download_locations_test", Context.MODE_PRIVATE)
    prefs.edit().clear().commit()
    preferences = DownloadPreferences(AndroidPreferenceStore(context, prefs))
    locations = DownloadLocations(context, preferences)
  }

  @Test
  fun statusWithNothingChosenReportsDefaultAndCreatesNothing() {
    val default = locations.defaultRoot()
    default.deleteRecursively()
    val status = locations.status()
    assertEquals(default.absolutePath, status.effectivePath)
    assertFalse(status.isCustom)
    assertFalse(status.isConfiguredUnavailable)
    assertNull(status.configuredPath)
    assertFalse("displaying the location must not create the folder", default.exists())
  }

  @Test
  fun statusReportsAUsableChosenFolder() {
    val chosen = tmp.newFolder("sd")
    preferences.downloadLocationPath.set(chosen.absolutePath)
    val status = locations.status()
    assertTrue(status.isCustom)
    assertEquals(chosen.absolutePath, status.effectivePath)
    assertFalse(status.isConfiguredUnavailable)
  }

  @Test
  fun missingChosenFolderFallsBackToDefaultAndSaysSo() {
    val missing = File(tmp.root, "gone-sd-card").absolutePath
    preferences.downloadLocationPath.set(missing)
    val status = locations.status()
    assertFalse(status.isCustom)
    assertTrue(status.isConfiguredUnavailable)
    assertEquals(missing, status.configuredPath)
    assertEquals(locations.defaultRoot().absolutePath, status.effectivePath)
    assertFalse("the chosen folder must not be recreated just to show it", File(missing).exists())
  }

  @Test
  fun rootStillCreatesTheDirectoryForTheDownloadWriter() {
    val default = locations.defaultRoot()
    default.deleteRecursively()
    assertTrue(locations.root().isDirectory)
  }
}
