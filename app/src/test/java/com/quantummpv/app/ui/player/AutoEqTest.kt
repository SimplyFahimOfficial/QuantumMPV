package com.quantummpv.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class AutoEqTest {
  private val base = "https://cdn.jsdelivr.net/gh/jaakkopasanen/AutoEq@master/results/"
  private val url = "${base}oratory1990/over-ear/Sennheiser%20HD%20600/Sennheiser%20HD%20600%20ParametricEQ.txt"
  private val entry = AutoEqEntry("Sennheiser HD 600", url)
  private val preset =
    """
    Preamp: -6.1 dB
    Filter 1: ON LSC Fc 105 Hz Gain 4.5 dB Q 0.70
    Filter 2: ON PK Fc 2400 Hz Gain -3.2 dB Q 2.50
    Filter 3: OFF PK Fc 100 Hz Gain 1.0 dB Q 1.00
    Filter 4: ON HSC Fc 10000 Hz Gain -2.0 dB Q 0.70
    """.trimIndent()

  @Test
  fun parsesPresetAndBuildsMpvFilter() {
    val profile = AutoEq.parse(entry, preset)
    assertEquals(-6.1, profile.preampDb, 0.0)
    assertEquals(3, profile.bands.size) // the OFF filter is dropped
    assertEquals(
      "@quantummpv-autoeq:lavfi=[volume=-6.100000dB," +
        "bass=f=105.000000:t=q:w=0.700000:g=4.500000," +
        "equalizer=f=2400.000000:t=q:w=2.500000:g=-3.200000," +
        "treble=f=10000.000000:t=q:w=0.700000:g=-2.000000]",
      profile.mpvFilter(),
    )
  }

  @Test
  fun savedProfileRoundTrips() {
    val restored = AutoEq.restore(AutoEq.parse(entry, preset).saved())
    assertNotNull(restored)
    assertEquals(entry, restored!!.entry)
    assertEquals(3, restored.bands.size)
  }

  @Test
  fun rejectsUnsafeOrMalformedInput() {
    assertNull(AutoEq.restore("not json"))
    assertNull(AutoEq.restore(""))
    val bad =
      listOf(
        "Preamp: -3 dB\nFilter 1: ON PK Fc 100 Hz Gain 40 dB Q 1", // gain out of range
        "Preamp: -3 dB\nFilter 1: ON PK Fc 100 Hz Gain 3 dB Q 99", // Q out of range
        "Preamp: -3 dB\nFilter 1: ON XX Fc 100 Hz Gain 3 dB Q 1", // unknown filter type
        "Preamp: -3 dB\nPreamp: -3 dB\nFilter 1: ON PK Fc 100 Hz Gain 3 dB Q 1", // duplicate preamp
        "Preamp: -3 dB", // no bands
        "Filter 1: ON PK Fc 100 Hz Gain 3 dB Q 1", // no preamp
        "Preamp: -3 dB\nsomething; af=evil", // free text
      )
    for (text in bad) {
      try {
        AutoEq.parse(entry, text)
        fail("accepted: $text")
      } catch (_: IllegalArgumentException) {
        // expected
      }
    }
  }

  @Test
  fun onlyTheAutoEqCdnPathIsAllowed() {
    assertTrue(AutoEq.validUrl(url))
    assertFalse(AutoEq.validUrl(url.replace("https", "http")))
    assertFalse(AutoEq.validUrl(url.replace("cdn.jsdelivr.net", "evil.example")))
    assertFalse(AutoEq.validUrl(url.replace("cdn.jsdelivr.net", "cdn.jsdelivr.net@evil.example")))
    assertFalse(AutoEq.validUrl("$url?x=1"))
    assertFalse(AutoEq.validUrl(url.replace("/gh/jaakkopasanen/AutoEq@master/results/", "/gh/other/repo@master/")))
    assertFalse(AutoEq.validUrl(url.removeSuffix("ParametricEQ.txt") + "x.txt"))
  }

  @Test
  fun bundledIndexOnlyContainsAllowedUrls() {
    val lines = File("src/main/assets/autoeq_index.tsv").readLines().filter { it.isNotBlank() }
    assertTrue(lines.size > 1000)
    for (line in lines) {
      val columns = line.trimStart('\uFEFF').split('\t', limit = 2)
      assertEquals(line, 2, columns.size)
      assertTrue(line, AutoEq.validUrl(columns[1]))
    }
  }
}
