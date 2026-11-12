package com.quantummpv.app.ui.player.controls.components

import org.junit.Assert.assertEquals
import org.junit.Test

class FadingHorizontalScrollTest {
  private val fade = 24f

  @Test
  fun atTheStartOnlyTheTrailingEdgeFades() {
    val (left, right) = fadeEdgeAlphas(value = 0, maxValue = 200, fadePx = fade, firstItemOnRight = false)
    assertEquals(1f, left, 0f)
    assertEquals(0f, right, 0f)
  }

  @Test
  fun atTheEndOnlyTheLeadingEdgeFades() {
    val (left, right) = fadeEdgeAlphas(value = 200, maxValue = 200, fadePx = fade, firstItemOnRight = false)
    assertEquals(0f, left, 0f)
    assertEquals(1f, right, 0f)
  }

  @Test
  fun inTheMiddleBothEdgesFade() {
    val (left, right) = fadeEdgeAlphas(value = 100, maxValue = 200, fadePx = fade, firstItemOnRight = false)
    assertEquals(0f, left, 0f)
    assertEquals(0f, right, 0f)
  }

  @Test
  fun anEdgeBecomesOpaqueAsItsHiddenContentShrinksBelowTheFadeLength() {
    val (left, right) = fadeEdgeAlphas(value = 6, maxValue = 200, fadePx = fade, firstItemOnRight = false)
    assertEquals(0.75f, left, 0.0001f)
    assertEquals(0f, right, 0f)
  }

  @Test
  fun rightToLeftSwapsTheSides() {
    val (left, right) = fadeEdgeAlphas(value = 0, maxValue = 200, fadePx = fade, firstItemOnRight = true)
    assertEquals(0f, left, 0f)
    assertEquals(1f, right, 0f)
  }

  @Test
  fun aRowThatDoesNotOverflowKeepsBothEdgesOpaque() {
    val (left, right) = fadeEdgeAlphas(value = 0, maxValue = 0, fadePx = fade, firstItemOnRight = false)
    assertEquals(1f, left, 0f)
    assertEquals(1f, right, 0f)
  }
}
