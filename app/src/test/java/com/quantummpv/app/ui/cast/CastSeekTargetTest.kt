package com.quantummpv.app.ui.cast

import org.junit.Assert.assertEquals
import org.junit.Test

class CastSeekTargetTest {
  @Test
  fun forwardSeekWithUnknownDurationMovesForwardInsteadOfToTheStart() {
    assertEquals(60_000L, castSeekTarget(position = 30_000, deltaMs = 30_000, duration = 0))
  }

  @Test
  fun forwardSeekIsCappedAtAKnownDuration() {
    assertEquals(100_000L, castSeekTarget(position = 90_000, deltaMs = 30_000, duration = 100_000))
    assertEquals(120_000L, castSeekTarget(position = 90_000, deltaMs = 30_000, duration = 500_000))
  }

  @Test
  fun backwardSeekNeverGoesBelowZero() {
    assertEquals(0L, castSeekTarget(position = 4_000, deltaMs = -10_000, duration = 100_000))
    assertEquals(0L, castSeekTarget(position = 4_000, deltaMs = -10_000, duration = 0))
    assertEquals(20_000L, castSeekTarget(position = 30_000, deltaMs = -10_000, duration = 0))
  }
}
