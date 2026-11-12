/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.preferences

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.ToggleButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.quantummpv.app.preferences.preference.Preference
import com.quantummpv.app.preferences.preference.PreferenceStore
import com.quantummpv.app.preferences.preference.getEnum
import com.quantummpv.app.ui.player.controls.components.rememberTvInitialFocusRequester
import com.quantummpv.app.ui.player.controls.components.tvFocusGroup
import com.quantummpv.app.ui.player.controls.components.tvFocusHighlight
import com.quantummpv.app.ui.player.controls.components.tvInitialFocus
import com.quantummpv.app.ui.theme.AppTheme
import com.quantummpv.app.ui.theme.CustomThemeDefinition
import com.quantummpv.app.ui.theme.DarkMode
import com.quantummpv.app.ui.theme.DesignTokens.MotionStyle
import com.quantummpv.app.ui.theme.DesignTokens.PerformanceMode
import com.quantummpv.app.ui.theme.WallpaperScaleMode
import com.quantummpv.app.ui.theme.spacing
import kotlinx.collections.immutable.ImmutableList

/** Which edge-highlight style Liquid Glass draws; Component keeps each surface's own. */
enum class LiquidGlassHighlightStyle { Component, Default, Ambient, Plain }

class AppearancePreferences(
  private val preferenceStore: PreferenceStore,
) {
  companion object {
    const val CUSTOM_WALLPAPER_URI_KEY = "custom_wallpaper_uri"

    // The full layout: every region populated, so no button needs manual setup. Row overflow scrolls.
    const val DEFAULT_TOP_LEFT_CONTROLS = "BACK_ARROW,VIDEO_TITLE"
    const val DEFAULT_TOP_RIGHT_CONTROLS = "CAST,CURRENT_CHAPTER,DECODER,AUDIO_TRACK,SUBTITLES,MORE_OPTIONS"
    const val DEFAULT_BOTTOM_RIGHT_CONTROLS = "FRAME_NAVIGATION,CLIP,SCOPES,VIDEO_ZOOM,PICTURE_IN_PICTURE,ASPECT_RATIO"
    const val DEFAULT_BOTTOM_LEFT_CONTROLS =
      "BACKGROUND_PLAYBACK,LOCK_CONTROLS,SCREEN_ROTATION,PLAYBACK_SPEED,REPEAT_MODE,SHUFFLE,AB_LOOP"
    const val DEFAULT_PORTRAIT_BOTTOM_CONTROLS =
      "CAST,SCREEN_ROTATION,DECODER,AUDIO_TRACK,SUBTITLES,BOOKMARKS_CHAPTERS,PLAYBACK_SPEED," +
        "BACKGROUND_PLAYBACK,REPEAT_MODE,SHUFFLE,VIDEO_ZOOM,FRAME_NAVIGATION,CLIP,SCOPES,ASPECT_RATIO," +
        "PICTURE_IN_PICTURE,LOCK_CONTROLS,MORE_OPTIONS"

    // The trimmed layout that was the default in defaults generation 2. It is now an opt-in preset, and
    // PreferenceDefaultsGeneration uses these to tell an untouched generation-2 install from a customized one.
    const val COMPACT_TOP_RIGHT_CONTROLS = "SUBTITLES,AUDIO_TRACK,MORE_OPTIONS"
    const val COMPACT_BOTTOM_RIGHT_CONTROLS = "SCREEN_ROTATION,PICTURE_IN_PICTURE,ASPECT_RATIO"
    const val COMPACT_BOTTOM_LEFT_CONTROLS = "LOCK_CONTROLS,PLAYBACK_SPEED"
    const val COMPACT_PORTRAIT_BOTTOM_CONTROLS =
      "LOCK_CONTROLS,SUBTITLES,AUDIO_TRACK,PLAYBACK_SPEED,SCREEN_ROTATION,MORE_OPTIONS"
  }

  val darkMode = preferenceStore.getEnum("dark_mode", DarkMode.System)
  val appTheme = preferenceStore.getEnum("app_theme", AppTheme.Dynamic)
  val customTheme = preferenceStore.getString("custom_theme", "")
  val selectedCustomThemeName = preferenceStore.getString("selected_custom_theme_name", "")
  val customWallpaperUri = preferenceStore.getString(CUSTOM_WALLPAPER_URI_KEY, "")
  val customWallpaperZoom = preferenceStore.getFloat("custom_wallpaper_zoom", 1f)
  val customWallpaperOffsetX = preferenceStore.getFloat("custom_wallpaper_offset_x", 0f)
  val customWallpaperOffsetY = preferenceStore.getFloat("custom_wallpaper_offset_y", 0f)
  val customWallpaperScaleMode = preferenceStore.getEnum("custom_wallpaper_scale_mode", WallpaperScaleMode.Fit)
  val customWallpaperBlur = preferenceStore.getFloat("custom_wallpaper_blur", 0f)
  val customWallpaperAlpha = preferenceStore.getFloat("custom_wallpaper_alpha", 1f)
  val amoledMode = preferenceStore.getBoolean("amoled_mode", false)

  // Experimental Liquid Glass (mpvRx 3.0): a real backdrop blur and lens refraction on the browser dock and
  // mini player. Opt-in because it records the screen behind the glass every frame.
  val liquidGlassEnabled = preferenceStore.getBoolean("liquid_glass_enabled", false)
  val liquidGlassOpacity = preferenceStore.getFloat("liquid_glass_opacity", 1f)
  val liquidGlassBlur = preferenceStore.getFloat("liquid_glass_blur", 1f)
  val liquidGlassRefractionHeight = preferenceStore.getFloat("liquid_glass_refraction_height", 1f)
  val liquidGlassRefractionAmount = preferenceStore.getFloat("liquid_glass_refraction_amount", 1f)
  val liquidGlassDepthEffect = preferenceStore.getBoolean("liquid_glass_depth_effect", false)
  val liquidGlassChromaticAberration = preferenceStore.getBoolean("liquid_glass_chromatic_aberration", true)
  val liquidGlassVibrancy = preferenceStore.getBoolean("liquid_glass_vibrancy", true)
  val liquidGlassSaturation = preferenceStore.getFloat("liquid_glass_saturation", 1f)
  val liquidGlassBrightness = preferenceStore.getFloat("liquid_glass_brightness", 0f)
  val liquidGlassContrast = preferenceStore.getFloat("liquid_glass_contrast", 1f)
  val liquidGlassHighlightStyle = preferenceStore.getEnum("liquid_glass_highlight_style", LiquidGlassHighlightStyle.Component)
  val liquidGlassHighlightStrength = preferenceStore.getFloat("liquid_glass_highlight_strength", 1f)
  val liquidGlassHighlightWidth = preferenceStore.getFloat("liquid_glass_highlight_width", 1f)
  val liquidGlassHighlightBlur = preferenceStore.getFloat("liquid_glass_highlight_blur", 1f)

  /** Back to the defaults for every Liquid Glass tuning option; the on/off switch is left alone. */
  fun resetLiquidGlassOptions() {
    listOf<Preference<*>>(
      liquidGlassOpacity, liquidGlassBlur, liquidGlassRefractionHeight, liquidGlassRefractionAmount,
      liquidGlassDepthEffect, liquidGlassChromaticAberration, liquidGlassVibrancy,
      liquidGlassSaturation, liquidGlassBrightness, liquidGlassContrast,
      liquidGlassHighlightStyle, liquidGlassHighlightStrength, liquidGlassHighlightWidth, liquidGlassHighlightBlur,
    ).forEach { it.delete() }
  }

  val motionStyle = preferenceStore.getEnum("motion_style", MotionStyle.STANDARD)
  val performanceMode = preferenceStore.getEnum("performance_mode", PerformanceMode.AUTOMATIC)
  val useSystemOsdFont = preferenceStore.getBoolean("use_system_osd_font", false)
  val useSystemFont = preferenceStore.getBoolean("use_system_font", false)
  val unlimitedNameLines = preferenceStore.getBoolean("unlimited_name_lines", false)
  val hidePlayerButtonsBackground = preferenceStore.getBoolean("hide_player_buttons_background", false)
  val forceDarkPlayerButtonsBackground = preferenceStore.getBoolean("force_dark_player_buttons_background", false)

  // mpvRx 3.0.2. 0.55 keeps today's translucent buttons, 0 hides the fill and 1 makes it solid.
  val playerButtonsBackgroundOpacity = preferenceStore.getFloat("player_buttons_background_opacity", 0.55f)
  val showUnplayedOldVideoLabel = preferenceStore.getBoolean("show_unplayed_old_video_label", true)
  val unplayedOldVideoDays = preferenceStore.getInt("unplayed_old_video_days", 7)
  val showNetworkThumbnails = preferenceStore.getBoolean("show_network_thumbnails", false)
  val seekbarStyle = preferenceStore.getEnum("seekbar_style", SeekbarStyle.Thick)
  val portraitPlaybackControlsPosition =
    preferenceStore.getEnum("portrait_playback_controls_position", PortraitPlaybackControlsPosition.Center)
  val showMusicTab = preferenceStore.getBoolean("show_music_tab", true)

  // Recents and Playlists live in the Profile hub (ported from mpvRx 3.x) instead of their own tabs.
  val showProfileTab = preferenceStore.getBoolean("show_profile_tab", true)
  val profileName = preferenceStore.getString("profile_name", "")
  val profileImagePath = preferenceStore.getString("profile_image_path", "")

  /** Optional dock overflow shortcuts; Settings and Help & Support are always shown and never stored here. */
  val moreMenuShortcuts =
    preferenceStore.getStringSet("more_menu_shortcuts", MoreMenuShortcut.DEFAULT.mapTo(HashSet()) { it.name })
  val showNetworkTab = preferenceStore.getBoolean("show_network_tab", false)
  val showJellyfinTab = preferenceStore.getBoolean("show_jellyfin_tab", false)
  val showQuickPlayFab = preferenceStore.getBoolean("show_quick_play_fab", true)
  val quickPlayFabDirect = preferenceStore.getBoolean("quick_play_fab_direct", false)

  // Full layout by default. Stored layouts are never rewritten, except a stored copy of the old compact
  // default, which PreferenceDefaultsGeneration drops so it follows this default.
  val topLeftControls = preferenceStore.getString("top_left_controls", DEFAULT_TOP_LEFT_CONTROLS)
  val topRightControls = preferenceStore.getString("top_right_controls", DEFAULT_TOP_RIGHT_CONTROLS)
  val bottomRightControls = preferenceStore.getString("bottom_right_controls", DEFAULT_BOTTOM_RIGHT_CONTROLS)
  val bottomLeftControls = preferenceStore.getString("bottom_left_controls", DEFAULT_BOTTOM_LEFT_CONTROLS)
  val portraitBottomControls =
    preferenceStore.getString("portrait_bottom_controls", DEFAULT_PORTRAIT_BOTTOM_CONTROLS)

  init {
    if (selectedCustomThemeName.get().isBlank()) {
      CustomThemeDefinition.parse(customTheme.get())?.let { legacyTheme ->
        selectedCustomThemeName.set(legacyTheme.name)
      }
    }
  }

  /** Switches to the trimmed preset: fewer buttons on screen, the rest in More. */
  fun resetPlayerLayoutToRecommended() {
    topLeftControls.set(DEFAULT_TOP_LEFT_CONTROLS)
    topRightControls.set(COMPACT_TOP_RIGHT_CONTROLS)
    bottomRightControls.set(COMPACT_BOTTOM_RIGHT_CONTROLS)
    bottomLeftControls.set(COMPACT_BOTTOM_LEFT_CONTROLS)
    portraitBottomControls.set(COMPACT_PORTRAIT_BOTTOM_CONTROLS)
  }

  /**
   * Puts all five regions back to the default layout. The controls drawer lives in
   * [PlayerPreferences.showControlsDrawer]; the caller resets it too.
   */
  fun resetPlayerLayoutToDefault() {
    topLeftControls.set(DEFAULT_TOP_LEFT_CONTROLS)
    topRightControls.set(DEFAULT_TOP_RIGHT_CONTROLS)
    bottomRightControls.set(DEFAULT_BOTTOM_RIGHT_CONTROLS)
    bottomLeftControls.set(DEFAULT_BOTTOM_LEFT_CONTROLS)
    portraitBottomControls.set(DEFAULT_PORTRAIT_BOTTOM_CONTROLS)
  }

  fun parseButtons(
    csv: String,
    usedButtons: MutableSet<PlayerButton>,
  ): List<PlayerButton> =
    csv
      .splitToSequence(',')
      .map { it.trim().uppercase() }
      .mapNotNull { name ->
        try {
          PlayerButton.valueOf(name)
        } catch (_: IllegalArgumentException) {
          null
        }
      }.filter { it != PlayerButton.NONE }
      .filter { usedButtons.add(it) }
      .toList()
}

/** Optional rows of the dock's More menu, shown below the fixed Settings and Help & Support rows in this order. */
enum class MoreMenuShortcut {
  APPEARANCE,
  PLAYER,
  DECODER,
  AUDIO,
  SUBTITLES,
  GESTURES,
  LIBRARY_INSIGHTS,
  RECENTS,
  PLAYLISTS,
  ;

  companion object {
    val DEFAULT = setOf(APPEARANCE)

    /** Stored names to rows in a stable order; names from older or newer builds are ignored. */
    fun resolve(stored: Set<String>): List<MoreMenuShortcut> = entries.filter { it.name in stored }
  }
}

enum class PortraitPlaybackControlsPosition(
  val displayName: String,
) {
  Center("Center of screen"),
  BelowSeekbar("Between seekbar and controls"),
}

@Composable
fun MultiChoiceSegmentedButton(
  choices: ImmutableList<String>,
  selectedIndices: ImmutableList<Int>,
  onClick: (Int, Offset) -> Unit,
  modifier: Modifier = Modifier,
) {
  val initialFocusRequester =
    rememberTvInitialFocusRequester(
      enabled = choices.isNotEmpty(),
      requestKey = selectedIndices,
    )
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        .tvFocusGroup()
        .padding(MaterialTheme.spacing.medium),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    choices.forEachIndexed { index, choice ->
      var buttonCenter by remember(choice) { mutableStateOf(Offset.Zero) }
      ToggleButton(
        checked = selectedIndices.contains(index),
        onCheckedChange = { onClick(index, buttonCenter) },
        modifier =
          Modifier
            .weight(1f)
            .then(
              if (index == selectedIndices.firstOrNull()) {
                Modifier.tvInitialFocus(initialFocusRequester)
              } else {
                Modifier
              },
            ).tvFocusHighlight(MaterialTheme.shapes.medium, focusedScale = 1.03f)
            .defaultMinSize(minHeight = MaterialTheme.spacing.extraLarge)
            .onGloballyPositioned { buttonCenter = it.boundsInWindow().center }
            .semantics { role = Role.RadioButton },
        colors =
          ToggleButtonDefaults.colors(
            checkedContainerColor = MaterialTheme.colorScheme.primaryContainer,
            checkedContentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            containerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
            contentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
          ),
        shapes =
          when (index) {
            0 -> ButtonGroupDefaults.connectedLeadingButtonShapes()
            choices.lastIndex -> ButtonGroupDefaults.connectedTrailingButtonShapes()
            else -> ButtonGroupDefaults.connectedMiddleButtonShapes()
          },
      ) {
        Text(text = choice)
      }
    }
  }
}
