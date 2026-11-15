package com.quantummpv.app.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Test

class LerpColorSchemeTest {
  private val light = lightColorScheme(primary = Color.White, surface = Color.White)
  private val dark = darkColorScheme(primary = Color.Black, surface = Color.Black)

  @Test
  fun endpointsMatchInputs() {
    assertEquals(light.surface, lerpColorScheme(light, dark, 0f).surface)
    assertEquals(dark.surface, lerpColorScheme(light, dark, 1f).surface)
  }

  @Test
  fun midpointIsBetween() {
    val mid = lerpColorScheme(light, dark, 0.5f)
    assertEquals(lerp(Color.White, Color.Black, 0.5f), mid.primary)
  }
}
