/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Adapted from mpvRx 3.0.2's Liquid Glass settings page.
 */

package com.quantummpv.app.ui.preferences

import android.os.Build
import androidx.annotation.StringRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.toggleableState
import androidx.compose.ui.state.ToggleableState
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.highlight.Highlight
import com.kyant.backdrop.shadow.InnerShadow
import com.kyant.backdrop.shadow.Shadow
import com.quantummpv.app.R
import com.quantummpv.app.preferences.AppearancePreferences
import com.quantummpv.app.preferences.LiquidGlassHighlightStyle
import com.quantummpv.app.preferences.preference.Preference
import com.quantummpv.app.preferences.preference.collectAsState
import com.quantummpv.app.presentation.Screen
import com.quantummpv.app.ui.icons.Icon
import com.quantummpv.app.ui.icons.Icons
import com.quantummpv.app.ui.liquidglass.LiquidButton
import com.quantummpv.app.ui.liquidglass.LiquidToggle
import com.quantummpv.app.ui.liquidglass.drawShapeTint
import com.quantummpv.app.ui.liquidglass.liquidGlassEffects
import com.quantummpv.app.ui.liquidglass.liquidGlassSupported
import com.quantummpv.app.ui.liquidglass.rememberLiquidGlassSettings
import com.quantummpv.app.ui.preferences.components.SwitchPreference
import com.quantummpv.app.ui.utils.LocalBackStack
import com.quantummpv.app.ui.utils.LocalShowSettingsBackArrow
import com.quantummpv.app.ui.utils.popSafely
import kotlinx.serialization.Serializable
import me.zhanghai.compose.preference.ListPreference
import me.zhanghai.compose.preference.ProvidePreferenceLocals
import me.zhanghai.compose.preference.SliderPreference
import org.koin.compose.koinInject
import java.text.NumberFormat

/**
 * Liquid Glass (experimental): the on/off switch, a live preview on the real renderer, and every
 * tuning option. The renderer itself is wired into the dock and mini player; see [GlassComponents].
 */
@Serializable
object LiquidGlassPreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val preferences = koinInject<AppearancePreferences>()
    val backstack = LocalBackStack.current
    val supported = liquidGlassSupported
    val storedEnabled by preferences.liquidGlassEnabled.collectAsState()
    val enabled = storedEnabled && supported

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              modifier = Modifier.settingsSearchTarget(R.string.pref_liquid_glass_title),
              text = stringResource(R.string.pref_liquid_glass_title),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            if (LocalShowSettingsBackArrow.current) {
              IconButton(onClick = { backstack.popSafely() }) {
                Icon(
                  Icons.RoundedFilled.ArrowBack,
                  contentDescription = stringResource(R.string.back),
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
            }
          },
        )
      },
    ) { padding ->
      ProvidePreferenceLocals {
        val (listState, highlight) =
          rememberSettingsSearchList(LiquidGlassPreferencesScreen, MaterialTheme.colorScheme.primary)
        BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(padding)) {
          // Short windows (landscape phones) get a smaller preview so the options keep most of the height.
          val compactPreview = maxHeight < 420.dp
          Column(Modifier.fillMaxSize()) {
            LiquidGlassPreview(
              enabled = enabled,
              height = if (compactPreview) 112.dp else 200.dp,
              compact = compactPreview,
            )
            LazyColumn(
              state = listState,
              modifier = Modifier.weight(1f).fillMaxWidth().then(highlight),
              contentPadding = PaddingValues(bottom = 28.dp),
            ) {
              item { ExperimentalNote() }
              item { PreferenceSectionHeader(title = stringResource(R.string.pref_section_general)) }
              item {
                PreferenceCard {
                  SwitchPreference(
                    value = enabled,
                    onValueChange = preferences.liquidGlassEnabled::set,
                    title = { Text(stringResource(R.string.pref_liquid_glass_enable_title)) },
                    summary = {
                      Text(
                        text =
                          stringResource(
                            if (supported) R.string.pref_liquid_glass_enable_summary else R.string.pref_liquid_glass_enable_unavailable,
                          ),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                      )
                    },
                    enabled = supported,
                    modifier = Modifier.settingsSearchTarget(R.string.pref_liquid_glass_enable_title),
                  )
                }
              }
              liquidGlassOptions(preferences, enabled)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ExperimentalNote() {
  Row(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(10.dp),
  ) {
    Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(50.dp)) {
      Text(
        text = stringResource(R.string.pref_experimental_badge),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.onTertiaryContainer,
        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
      )
    }
    Text(
      text = stringResource(R.string.pref_liquid_glass_experimental_note),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      modifier = Modifier.weight(1f),
    )
  }
}

/** A checkerboard of theme colours with a glass card on top, so every slider shows its effect at once. */
@Composable
private fun LiquidGlassPreview(
  enabled: Boolean,
  height: Dp,
  compact: Boolean,
) {
  val backdrop = rememberLayerBackdrop()
  val settings = rememberLiquidGlassSettings()
  val colors = MaterialTheme.colorScheme
  val shape = MaterialTheme.shapes.extraLargeIncreased
  var playing by rememberSaveable { mutableStateOf(false) }
  var repeating by rememberSaveable { mutableStateOf(false) }
  val playLabel = stringResource(R.string.pref_gesture_media_play)
  val repeatLabel = stringResource(R.string.pref_liquid_glass_preview_toggle)

  Box(
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).height(height).clip(shape),
    contentAlignment = Alignment.Center,
  ) {
    Canvas(Modifier.matchParentSize().layerBackdrop(backdrop)) {
      val tile = 40.dp.toPx()
      val palette = listOf(colors.primaryContainer, colors.secondaryContainer, colors.tertiaryContainer)
      for (row in 0..(size.height / tile).toInt()) {
        for (column in 0..(size.width / tile).toInt()) {
          drawRect(palette[(row + column) % palette.size], Offset(column * tile, row * tile), Size(tile, tile))
        }
      }
    }
    Column(
      modifier =
        Modifier
          .padding(if (compact) 8.dp else 20.dp)
          .fillMaxWidth()
          .then(
            if (enabled) {
              Modifier.drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                  liquidGlassEffects(
                    settings,
                    6.dp.toPx(),
                    12.dp.toPx(),
                    24.dp.toPx(),
                    vibrant = true,
                    refractionEnabled = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU,
                  )
                },
                highlight = { settings.highlight(Highlight.Default) },
                shadow = { Shadow.Default },
                innerShadow = { InnerShadow(radius = 4.dp, alpha = 0.2f) },
                onDrawSurface = { drawShapeTint(shape, settings.surfaceColor(colors.surface.copy(alpha = 0.35f))) },
              )
            } else {
              Modifier.background(colors.surfaceContainerHigh, shape)
            },
          ).padding(if (compact) 8.dp else 16.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      if (!compact) {
        Text(
          text = stringResource(R.string.app_name),
          style = MaterialTheme.typography.titleMedium,
          color = colors.onSurface,
        )
      }
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        val playIcon = if (playing) Icons.RoundedFilled.Pause else Icons.RoundedFilled.PlayArrow
        if (enabled) {
          LiquidButton(
            onClick = { playing = !playing },
            backdrop = backdrop,
            modifier = Modifier.size(48.dp),
            horizontalPadding = 0.dp,
          ) {
            Icon(playIcon, contentDescription = playLabel, tint = colors.onSurface)
          }
        } else {
          IconButton(onClick = { playing = !playing }, modifier = Modifier.size(48.dp)) {
            Icon(playIcon, contentDescription = playLabel, tint = colors.onSurface)
          }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.RoundedFilled.Repeat, contentDescription = null, tint = colors.onSurface)
          if (enabled) {
            LiquidToggle(
              selected = { repeating },
              onSelect = { repeating = it },
              backdrop = backdrop,
              modifier =
                Modifier.width(64.dp).height(48.dp).semantics {
                  contentDescription = repeatLabel
                  toggleableState = if (repeating) ToggleableState.On else ToggleableState.Off
                  onClick {
                    repeating = !repeating
                    true
                  }
                },
            )
          } else {
            Switch(
              checked = repeating,
              onCheckedChange = { repeating = it },
              modifier = Modifier.width(64.dp).height(48.dp).semantics { contentDescription = repeatLabel },
            )
          }
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyListScope.liquidGlassOptions(
  preferences: AppearancePreferences,
  enabled: Boolean,
) {
  val lensSupported = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

  item { SectionHeading(R.string.pref_liquid_glass_material) }
  item { PreferenceCard { GlassSlider(preferences.liquidGlassBlur, R.string.pref_liquid_glass_frost, enabled) } }

  item { SectionHeading(R.string.pref_liquid_glass_optics) }
  item {
    val refractionHeight by preferences.liquidGlassRefractionHeight.collectAsState()
    val refractionAmount by preferences.liquidGlassRefractionAmount.collectAsState()
    val lensEnabled = enabled && lensSupported
    val lensActive = lensEnabled && refractionHeight > 0f && refractionAmount > 0f
    PreferenceCard {
      GlassSlider(preferences.liquidGlassOpacity, R.string.pref_liquid_glass_opacity, enabled, 0f..1f)
      PreferenceDivider()
      GlassSlider(preferences.liquidGlassRefractionHeight, R.string.pref_liquid_glass_refraction_height, lensEnabled)
      PreferenceDivider()
      GlassSlider(preferences.liquidGlassRefractionAmount, R.string.pref_liquid_glass_refraction_amount, lensEnabled)
      PreferenceDivider()
      GlassSwitch(preferences.liquidGlassDepthEffect, R.string.pref_liquid_glass_depth, lensActive)
      PreferenceDivider()
      GlassSwitch(preferences.liquidGlassChromaticAberration, R.string.pref_liquid_glass_chromatic, lensActive)
      if (!lensSupported) {
        Text(
          stringResource(R.string.pref_liquid_glass_lens_unavailable),
          style = MaterialTheme.typography.bodySmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        )
      }
    }
  }

  item { SectionHeading(R.string.pref_liquid_glass_colors) }
  item {
    PreferenceCard {
      GlassSwitch(preferences.liquidGlassVibrancy, R.string.pref_liquid_glass_vibrancy, enabled)
      PreferenceDivider()
      GlassSlider(preferences.liquidGlassSaturation, R.string.player_sheets_filters_Saturation, enabled)
      PreferenceDivider()
      GlassSlider(preferences.liquidGlassBrightness, R.string.player_sheets_filters_brightness, enabled, -1f..1f)
      PreferenceDivider()
      GlassSlider(preferences.liquidGlassContrast, R.string.player_sheets_filters_contrast, enabled)
    }
  }

  item { SectionHeading(R.string.pref_liquid_glass_highlights) }
  item {
    val highlightStyle by preferences.liquidGlassHighlightStyle.collectAsState()
    val labels =
      mapOf(
        LiquidGlassHighlightStyle.Component to stringResource(R.string.pref_liquid_glass_component_default),
        LiquidGlassHighlightStyle.Default to stringResource(R.string.theme_default),
        LiquidGlassHighlightStyle.Ambient to stringResource(R.string.pref_liquid_glass_highlight_ambient),
        LiquidGlassHighlightStyle.Plain to stringResource(R.string.pref_liquid_glass_highlight_plain),
      )
    PreferenceCard {
      ListPreference(
        value = highlightStyle,
        onValueChange = preferences.liquidGlassHighlightStyle::set,
        values = LiquidGlassHighlightStyle.entries,
        valueToText = { AnnotatedString(labels.getValue(it)) },
        title = { Text(stringResource(R.string.pref_liquid_glass_highlight_style)) },
        summary = { Text(labels.getValue(highlightStyle), color = MaterialTheme.colorScheme.outline) },
        enabled = enabled,
        modifier = Modifier.settingsSearchTarget(R.string.pref_liquid_glass_highlight_style),
      )
      PreferenceDivider()
      GlassSlider(preferences.liquidGlassHighlightStrength, R.string.pref_liquid_glass_highlight_strength, enabled)
      PreferenceDivider()
      GlassSlider(preferences.liquidGlassHighlightWidth, R.string.pref_liquid_glass_highlight_width, enabled)
      PreferenceDivider()
      GlassSlider(preferences.liquidGlassHighlightBlur, R.string.pref_liquid_glass_highlight_blur, enabled)
    }
  }
  item {
    TextButton(
      onClick = preferences::resetLiquidGlassOptions,
      enabled = enabled,
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
      Icon(Icons.RoundedFilled.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
      Spacer(Modifier.width(8.dp))
      Text(stringResource(R.string.pref_layout_reset_default))
    }
  }
}

@Composable
private fun SectionHeading(
  @StringRes title: Int,
) {
  PreferenceSectionHeader(title = stringResource(title), modifier = Modifier.semantics { heading() })
}

@Composable
private fun GlassSwitch(
  preference: Preference<Boolean>,
  @StringRes title: Int,
  enabled: Boolean,
) {
  val value by preference.collectAsState()
  SwitchPreference(
    value = value,
    onValueChange = preference::set,
    title = { Text(stringResource(title)) },
    enabled = enabled,
    modifier = Modifier.settingsSearchTarget(title),
  )
}

@Composable
private fun GlassSlider(
  preference: Preference<Float>,
  @StringRes title: Int,
  enabled: Boolean,
  range: ClosedFloatingPointRange<Float> = 0f..2f,
) {
  val saved by preference.collectAsState()
  // A restored backup could hold NaN or an out-of-range value; show a sane position instead of crashing.
  val value = if (saved.isFinite()) saved.coerceIn(range) else preference.defaultValue()
  val locale = LocalConfiguration.current.locales[0]
  SliderPreference(
    value = value,
    onValueChange = preference::set,
    sliderValue = value,
    onSliderValueChange = preference::set,
    valueRange = range,
    valueSteps = 19,
    title = { Text(stringResource(title)) },
    summary = { Text(NumberFormat.getPercentInstance(locale).format(value), color = MaterialTheme.colorScheme.outline) },
    enabled = enabled,
    modifier = Modifier.settingsSearchTarget(title),
  )
}
