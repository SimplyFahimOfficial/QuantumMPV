package com.quantummpv.app.ui.utils

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StaggerDelayTest {
  @Test
  fun rowsStartInOrderAtTheStep() {
    assertEquals(0L, staggerDelayMillis(0, 35, 8))
    assertEquals(35L, staggerDelayMillis(1, 35, 8))
    assertEquals(280L, staggerDelayMillis(8, 35, 8))
  }

  @Test
  fun rowsPastTheLimitNeverStartBeforeTheOnesAbove() {
    val last = staggerDelayMillis(8, 35, 8)
    for (index in 9..40) assertEquals(last, staggerDelayMillis(index, 35, 8))
    assertTrue((0..40).map { staggerDelayMillis(it, 35, 8) }.zipWithNext().all { (a, b) -> b >= a })
  }
}
