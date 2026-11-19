package com.quantummpv.app.ui.preferences

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.IOException

class CustomButtonScreenTest {
  private val button = CustomButton(id = "a", title = "A", content = "show-text hi", scriptLanguage = CustomButtonScriptLanguage.JS)

  @Test
  fun savedSlotFormatIsDecodedInPlace() {
    val saved = Json.encodeToString(CustomButtonSlots(List(8) { if (it == 5) button else null }))
    val slots = decodeCustomButtonSlots(saved)!!
    assertEquals(8, slots.size)
    assertEquals(button, slots[5])
  }

  @Test
  fun legacyListIsMigratedIntoLeadingSlots() {
    val legacy = Json.encodeToString(listOf(button, button.copy(id = "b")))
    val slots = decodeCustomButtonSlots(legacy)!!
    assertEquals(listOf("a", "b"), slots.take(2).map { it?.id })
    assertTrue(slots.drop(2).all { it == null })
  }

  @Test
  fun blankAndDefaultPayloadsMeanNothingSaved() {
    for (raw in listOf("", "  ", "[]", "{}")) assertTrue(raw, decodeCustomButtonSlots(raw)!!.all { it == null })
  }

  @Test
  fun payloadFromANewerVersionWithExtraKeysStaysReadable() {
    val raw = """{"slots":[{"id":"a","title":"A","content":"x","futureField":1}],"extra":true}"""
    assertEquals("a", decodeCustomButtonSlots(raw)!![0]?.id)
  }

  @Test
  fun unreadablePayloadIsReportedNotReplacedByEmptySlots() {
    assertNull(decodeCustomButtonSlots("{\"slots\":[{broken"))
    assertNull(decodeCustomButtonSlots("not json"))
  }

  @Test
  fun xmlExportImportRoundTripKeepsSlotsAndScripts() {
    val original = List<CustomButton?>(8) { null }.toMutableList()
    original[0] = button.copy(longPressContent = "a & <b>", onStartup = "init")
    original[7] = button.copy(id = "z", title = "Q \"x\" & 'y'")
    val imported = parseXmlImport(buildXmlExport(original))
    assertEquals(original, imported)
  }

  @Test
  fun xmlWithoutButtonsImportsNothing() {
    assertTrue(parseXmlImport("<customButtons></customButtons>").all { it == null })
    assertTrue(parseXmlImport("garbage").all { it == null })
  }

  @Test
  fun boundedReadAcceptsLimitAndRejectsLarger() {
    assertEquals("héllo", readBoundedText(ByteArrayInputStream("héllo".toByteArray()), 16))
    assertNotNull(readBoundedText(ByteArrayInputStream(ByteArray(10)), 10))
    try {
      readBoundedText(ByteArrayInputStream(ByteArray(11)), 10)
      fail("expected IOException")
    } catch (_: IOException) {
    }
  }
}
