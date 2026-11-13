package com.quantummpv.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FxAmbientShaderBuilderTest {
  private val fxStyles = AmbientStyle.entries - setOf(AmbientStyle.Glow, AmbientStyle.YouTube)

  private fun spec(
    style: AmbientStyle,
    samples: Int,
    sx: Double = 1.78,
    sy: Double = 1.0,
  ): FxAmbientShaderSpec {
    val (_, balanced, _) = ambientPresetsFor(style)
    return FxAmbientShaderSpec(
      style = style,
      scaleX = sx,
      scaleY = sy,
      blurSamples = samples,
      spread = balanced.maxRadius,
      intensity = balanced.glowIntensity,
      satBoost = balanced.satBoost,
      vignetteStrength = 0.5f,
      warmth = 0.3f,
      fadeCurve = balanced.fadeCurve,
      opacity = balanced.opacity,
    )
  }

  @Test
  fun everyFxStyleBuildsAWellFormedHookShader() {
    val dump = System.getenv("AMBIENT_SHADER_DUMP")?.let(::File)?.also { it.mkdirs() }
    for (style in fxStyles) {
      for (samples in listOf(1, 5, 14, 64)) {
        for ((sx, sy) in listOf(1.78 to 1.0, 1.0 to 2.1)) {
          val code = FxAmbientShaderBuilder.build(spec(style, samples, sx, sy))
          val label = "$style samples=$samples scale=$sx,$sy"
          assertTrue(label, code.startsWith("//!HOOK OUTPUT"))
          assertTrue(label, "vec4 hook()" in code)
          assertEquals(label, code.count { it == '{' }, code.count { it == '}' })
          assertEquals(label, code.count { it == '(' }, code.count { it == ')' })
          assertFalse(label, "NaN" in code || "Infinity" in code)
          assertFalse(label, Regex("\\$\\{|\\$[a-zA-Z]").containsMatchIn(code))
          dump?.let { File(it, "${style}_${samples}_${sx}x$sy.glsl".replace(' ', '_')).writeText(code) }
        }
      }
    }
  }

  @Test
  fun styleCannotReuseAnotherStylesShader() {
    val shaders = fxStyles.map { FxAmbientShaderBuilder.build(spec(it, 9)) }
    assertEquals(fxStyles.size, shaders.toSet().size)
  }

  @Test
  fun presetsExistForEveryStyleAndBalancedMatchesItself() {
    for (style in AmbientStyle.entries) {
      val (fast, balanced, hq) = ambientPresetsFor(style)
      assertTrue(matchesGlowPreset(balanced, balanced.blurSamples, balanced.maxRadius, balanced.glowIntensity, balanced.satBoost, balanced.vignetteStrength, balanced.warmth, balanced.fadeCurve, balanced.opacity))
      assertTrue("$style presets must differ", fast != hq)
    }
  }
}
