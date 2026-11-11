package com.quantummpv.app.ui.player.controls

import org.junit.Assert.assertEquals
import org.junit.Test

class PlayerButtonAlphaTest {
  private val default = PlayerButtonAlpha.DEFAULT_BACKGROUND_LEVEL

  /** The default level must leave today's look untouched. */
  @Test
  fun defaultLevelKeepsTheDesignedAlpha() {
    assertEquals(PlayerButtonAlpha.CONTAINER, PlayerButtonAlpha.background(PlayerButtonAlpha.CONTAINER, default), 1e-6f)
    assertEquals(PlayerButtonAlpha.BORDER, PlayerButtonAlpha.background(PlayerButtonAlpha.BORDER, default), 1e-6f)
  }

  @Test
  fun zeroHidesTheFillAndOneMakesItSolid() {
    assertEquals(0f, PlayerButtonAlpha.background(PlayerButtonAlpha.CONTAINER, 0f), 0f)
    assertEquals(1f, PlayerButtonAlpha.background(PlayerButtonAlpha.CONTAINER, 1f), 1e-6f)
    assertEquals(1f, PlayerButtonAlpha.background(PlayerButtonAlpha.BORDER, 1f), 1e-6f)
  }

  @Test
  fun alphaGrowsMonotonicallyWithTheLevel() {
    val alphas = (0..20).map { PlayerButtonAlpha.background(PlayerButtonAlpha.CONTAINER, it / 20f) }
    assertEquals(alphas.sorted(), alphas)
  }

  @Test
  fun aTransparentHitAreaNeverBecomesVisible() {
    for (level in listOf(0f, default, 1f)) assertEquals(0f, PlayerButtonAlpha.background(0f, level), 0f)
  }

  @Test
  fun badStoredLevelsFallBackSafely() {
    assertEquals(default, PlayerButtonAlpha.level(Float.NaN), 0f)
    assertEquals(default, PlayerButtonAlpha.level(Float.POSITIVE_INFINITY), 0f)
    assertEquals(0f, PlayerButtonAlpha.level(-3f), 0f)
    assertEquals(1f, PlayerButtonAlpha.level(7f), 0f)
    assertEquals(PlayerButtonAlpha.CONTAINER, PlayerButtonAlpha.background(PlayerButtonAlpha.CONTAINER, Float.NaN), 1e-6f)
  }
}
