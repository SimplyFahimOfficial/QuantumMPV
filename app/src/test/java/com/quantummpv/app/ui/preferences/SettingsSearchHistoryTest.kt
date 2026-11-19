package com.quantummpv.app.ui.preferences

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsSearchHistoryTest {
  @Test
  fun queriesWithDelimitersQuotesAndUnicodeRoundTrip() {
    var raw = ""
    val queries = listOf("a|b", "say \"hi\"", "日本語 ✓", "[brackets]", "back\\slash")
    queries.forEach { raw = SettingsSearchHistory.add(raw, it) }
    assertEquals(queries.reversed(), SettingsSearchHistory.parse(raw))
  }

  @Test
  fun legacyPipeSeparatedHistoryIsReadAndUpgradedOnNextSave() {
    val legacy = "theme|audio|subtitles"
    assertEquals(listOf("theme", "audio", "subtitles"), SettingsSearchHistory.parse(legacy))
    val saved = SettingsSearchHistory.add(legacy, "player")
    assertTrue(saved.startsWith("["))
    assertEquals(listOf("player", "theme", "audio", "subtitles"), SettingsSearchHistory.parse(saved))
  }

  @Test
  fun addMovesAnExistingQueryToTheFrontWithoutDuplicates() {
    val raw = listOf("a", "b", "c").fold("") { acc, q -> SettingsSearchHistory.add(acc, q) } // c, b, a
    assertEquals(listOf("a", "c", "b"), SettingsSearchHistory.parse(SettingsSearchHistory.add(raw, "a")))
  }

  @Test
  fun historyKeepsOnlyTheNewestTenEntries() {
    val raw = (1..15).fold("") { acc, i -> SettingsSearchHistory.add(acc, "q$i") }
    val parsed = SettingsSearchHistory.parse(raw)
    assertEquals(10, parsed.size)
    assertEquals("q15", parsed.first())
    assertEquals("q6", parsed.last())
  }

  @Test
  fun removeAndClearBehave() {
    val raw = SettingsSearchHistory.add(SettingsSearchHistory.add("", "a|b"), "c")
    assertEquals(listOf("c"), SettingsSearchHistory.parse(SettingsSearchHistory.remove(raw, "a|b")))
    assertEquals("", SettingsSearchHistory.remove(SettingsSearchHistory.add("", "x"), "x"))
  }

  @Test
  fun malformedStoredValueDoesNotCrash() {
    assertEquals(listOf("[\"unterminated"), SettingsSearchHistory.parse("[\"unterminated"))
    assertEquals(emptyList<String>(), SettingsSearchHistory.parse(""))
    assertEquals(emptyList<String>(), SettingsSearchHistory.parse("||"))
    // Valid JSON of the wrong shape falls back to the legacy reading instead of throwing.
    assertEquals(listOf("{\"a\":1}"), SettingsSearchHistory.parse("{\"a\":1}"))
    assertEquals(listOf("[1,2]"), SettingsSearchHistory.parse("[1,2]"))
  }
}
