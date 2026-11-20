package com.quantummpv.app.preferences

import android.content.Context
import com.quantummpv.app.preferences.SettingsPromo.HELP_SUPPORT
import com.quantummpv.app.preferences.SettingsPromo.SUPPORT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE, sdk = [34])
class SettingsPromoPreferencesTest {
  private val day = 24L * 60 * 60 * 1000
  private val window = SettingsPromoPreferences.WINDOW_MILLIS
  private val installedAt = 1_000_000_000_000L
  private var now = installedAt + 1_000L
  private lateinit var context: Context

  private fun open() =
    SettingsPromoPreferences(context.getSharedPreferences(SettingsPromoPreferences.FILE_NAME, Context.MODE_PRIVATE)) { now }

  @Before
  fun setUp() {
    context = RuntimeEnvironment.getApplication()
    context.getSharedPreferences(SettingsPromoPreferences.FILE_NAME, Context.MODE_PRIVATE).edit().clear().commit()
  }

  @Test
  fun bothCardsShowOnFirstObservationOfAVersionInOrder() {
    val snapshot = open().resolve(10, installedAt)
    assertEquals(listOf(HELP_SUPPORT, SUPPORT), snapshot.visible(now))
    assertEquals(installedAt, snapshot.start)
  }

  @Test
  fun windowIsHalfOpenAndEndsExactlyOnDaySeven() {
    val snapshot = open().resolve(10, installedAt)
    assertTrue(snapshot.visible(installedAt - 1).isEmpty())
    assertEquals(2, snapshot.visible(installedAt).size)
    assertEquals(2, snapshot.visible(installedAt + window - 1).size)
    assertTrue(snapshot.visible(installedAt + window).isEmpty())
  }

  @Test
  fun dismissalsAreIndependentInEitherOrder() {
    val promos = open()
    promos.resolve(10, installedAt)
    assertEquals(listOf(SUPPORT), promos.dismiss(10, HELP_SUPPORT)!!.visible(now))
    assertEquals(emptyList<SettingsPromo>(), promos.dismiss(10, SUPPORT)!!.visible(now))

    context.getSharedPreferences(SettingsPromoPreferences.FILE_NAME, Context.MODE_PRIVATE).edit().clear().commit()
    val other = open()
    other.resolve(10, installedAt)
    assertEquals(listOf(HELP_SUPPORT), other.dismiss(10, SUPPORT)!!.visible(now))
  }

  @Test
  fun dismissalsAndWindowSurviveProcessDeath() {
    open().apply {
      resolve(10, installedAt)
      dismiss(10, HELP_SUPPORT)
    }
    now = installedAt + 3 * day
    val restored = open().resolve(10, installedAt)
    assertEquals(installedAt, restored.start)
    assertEquals(listOf(SUPPORT), restored.visible(now))
  }

  @Test
  fun sameVersionCodeReplacementKeepsTheRecordedWindowAndDismissals() {
    val first = open()
    first.resolve(10, installedAt)
    first.dismiss(10, SUPPORT)
    now = installedAt + 2 * day
    // adb install -r of the same version code moves lastUpdateTime, which must not restart the window.
    val after = open().resolve(10, installedAt + 2 * day)
    assertEquals(installedAt, after.start)
    assertEquals(listOf(HELP_SUPPORT), after.visible(now))
  }

  @Test
  fun aNewVersionResetsBothCardsAndStartsAFreshWindow() {
    val promos = open()
    promos.resolve(10, installedAt)
    promos.dismiss(10, HELP_SUPPORT)
    promos.dismiss(10, SUPPORT)
    now = installedAt + 10 * day
    val updated = open().resolve(11, now - 1_000)
    assertEquals(listOf(HELP_SUPPORT, SUPPORT), updated.visible(now))
    assertEquals(now - 1_000, updated.start)
  }

  @Test
  fun aLateFirstVisitAfterTheWindowShowsNothing() {
    now = installedAt + window + day
    assertTrue(open().resolve(10, installedAt).visible(now).isEmpty())
  }

  @Test
  fun onceExpiryIsObservedARollbackDoesNotReviveTheCards() {
    val promos = open()
    promos.resolve(10, installedAt)
    now = installedAt + window + 1
    assertTrue(promos.resolve(10, installedAt).expired)
    now = installedAt + day // clock moved back into the window
    assertTrue(open().resolve(10, installedAt).visible(now).isEmpty())
  }

  @Test
  fun missingOrFutureInstallTimeOpensNoWindow() {
    for (invalid in listOf(0L, -5L, now + day)) {
      context.getSharedPreferences(SettingsPromoPreferences.FILE_NAME, Context.MODE_PRIVATE).edit().clear().commit()
      val snapshot = open().resolve(10, invalid)
      assertTrue("installedAt=$invalid", snapshot.visible(now).isEmpty())
      // Later observations of the same version stay closed even with valid metadata.
      assertTrue(open().resolve(10, installedAt).visible(now).isEmpty())
    }
  }

  @Test
  fun dismissForAVersionThatIsNoLongerStoredIsIgnored() {
    val promos = open()
    promos.resolve(10, installedAt)
    promos.resolve(11, installedAt)
    promos.dismiss(10, HELP_SUPPORT)
    assertEquals(listOf(HELP_SUPPORT, SUPPORT), promos.resolve(11, installedAt).visible(now))
    assertNull(open().dismiss(10, SUPPORT))
  }
}
