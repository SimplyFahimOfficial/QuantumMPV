package com.quantummpv.app.ui.theme

import androidx.compose.ui.geometry.Offset
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ThemeRevealTest {
  @Test
  fun appliesTheChangeAtOnceWithoutAWindow() {
    var changes = 0
    val reveal = ThemeReveal()
    reveal.start(Offset(10f, 10f)) { changes++ }
    assertEquals(1, changes)
    assertFalse(reveal.isAnimating)
  }

  @Test
  fun appliesTheChangeAtOnceUnderReducedMotion() {
    var changes = 0
    val reveal = ThemeReveal().apply { reduceMotion = true }
    reveal.start(Offset.Zero) { changes++ }
    assertEquals(1, changes)
  }
}
