package com.quantummpv.app.ui.update

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class UpdateSheetTest {
  @Test
  fun unknownProgressHasNoPercent() {
    assertNull(downloadPercent(-1f))
    assertNull(downloadPercent(Float.NaN))
    assertNull(downloadPercent(Float.NEGATIVE_INFINITY))
  }

  @Test
  fun knownProgressIsAWholePercentClampedToOneHundred() {
    assertEquals(0, downloadPercent(0f))
    assertEquals(42, downloadPercent(42.9f))
    assertEquals(100, downloadPercent(100f))
    assertEquals(100, downloadPercent(137f))
    assertEquals(100, downloadPercent(Float.POSITIVE_INFINITY))
  }
}
