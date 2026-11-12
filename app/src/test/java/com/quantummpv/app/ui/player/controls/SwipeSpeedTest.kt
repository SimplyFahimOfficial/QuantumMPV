package com.quantummpv.app.ui.player.controls

import com.quantummpv.app.ui.player.PlayerUpdates
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test

class SwipeSpeedTest {
  private fun up(speed: Float) = swipeSpeedStep(speed, up = true)

  private fun down(speed: Float) = swipeSpeedStep(speed, up = false)

  @Test
  fun stepsMoveInQuarterSpeedIncrements() {
    assertEquals(1.25f, up(1f), 1e-6f)
    assertEquals(0.75f, down(1f), 1e-6f)
    assertEquals(2.5f, swipeSpeedAfter(1f, 6), 1e-6f)
    assertEquals(0.5f, swipeSpeedAfter(1f, -2), 1e-6f)
  }

  /** A speed off the grid snaps to the next grid line in the swipe direction instead of skipping one. */
  @Test
  fun offGridSpeedLandsOnTheNextGridLine() {
    assertEquals(1.25f, up(1.1f), 1e-6f)
    assertEquals(1f, down(1.1f), 1e-6f)
  }

  @Test
  fun speedStopsAtTheLimits() {
    assertEquals(4f, up(4f), 0f)
    assertEquals(4f, swipeSpeedAfter(3.5f, 10), 1e-6f)
    assertEquals(0.5f, down(0.5f), 0f)
    assertEquals(0.5f, swipeSpeedAfter(1f, -10), 1e-6f)
  }

  /** A speed set outside 0.5x-4x elsewhere (for example 8x) is never pushed further out, but may come back in. */
  @Test
  fun outOfRangeSpeedNeverMovesFurtherOut() {
    assertEquals(8f, up(8f), 0f)
    assertEquals(7.75f, down(8f), 1e-6f)
    assertEquals(0.25f, down(0.25f), 0f)
    assertEquals(0.5f, up(0.25f), 1e-6f)
  }

  @Test
  fun zeroStepsKeepTheSpeed() {
    assertEquals(1.1f, swipeSpeedAfter(1.1f, 0), 0f)
  }

  @Test
  fun speedSwipeCleanupClearsOnlyTheUpdateItOwns() {
    val shown = PlayerUpdates.DynamicSpeedControl(1.5f)
    assertSame(
      PlayerUpdates.None,
      clearOwnedSpeedSwipeUpdate(shown, shown),
    )

    val newer = PlayerUpdates.DynamicSpeedControl(1.5f)
    assertEquals(shown, newer)
    assertNotSame(shown, newer)
    assertSame(newer, clearOwnedSpeedSwipeUpdate(newer, shown))
  }
}
