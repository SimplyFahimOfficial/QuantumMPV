/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.preferences

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.rememberNavBackStack
import com.quantummpv.app.R
import com.quantummpv.app.preferences.SecureFolderPreferences
import com.quantummpv.app.preferences.SettingsPromo
import com.quantummpv.app.preferences.SettingsPromoPreferences
import com.quantummpv.app.preferences.preference.collectAsState
import com.quantummpv.app.preferences.resolve
import com.quantummpv.app.presentation.Screen
import com.quantummpv.app.ui.icons.AppIcon
import com.quantummpv.app.ui.icons.Icon
import com.quantummpv.app.ui.icons.Icons
import com.quantummpv.app.ui.player.controls.components.rememberTvInitialFocusRequester
import com.quantummpv.app.ui.player.controls.components.tvFocusGroup
import com.quantummpv.app.ui.player.controls.components.tvFocusHighlight
import com.quantummpv.app.ui.player.controls.components.tvInitialFocus
import com.quantummpv.app.ui.securefolder.SecureFolderGateScreen
import com.quantummpv.app.ui.theme.LocalEmphasizedTypography
import com.quantummpv.app.ui.utils.LocalBackStack
import com.quantummpv.app.ui.utils.LocalShowSettingsBackArrow
import com.quantummpv.app.ui.utils.ScreenNavDisplay
import com.quantummpv.app.ui.utils.appItemMotion
import com.quantummpv.app.ui.utils.navigateTo
import com.quantummpv.app.ui.utils.popSafely
import com.quantummpv.app.ui.utils.sharedNavBounds
import com.quantummpv.app.ui.utils.staggeredEntrance
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import org.koin.compose.koinInject

private data class SettingsDestination(
  val title: String,
  val summary: String,
  val icon: AppIcon,
  val screen: Screen,
)

private data class SettingsSection(
  val title: String,
  val tint: Color,
  val items: List<SettingsDestination>,
  /** Tints the group with the primary container so a section such as Help & Support stands out. */
  val highlighted: Boolean = false,
)

@Serializable
object PreferencesScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val backstack = LocalBackStack.current
    val colorScheme = MaterialTheme.colorScheme
    val sections = settingsSections()

    val configuration = LocalConfiguration.current
    val isTablet = configuration.smallestScreenWidthDp >= 600

    if (isTablet) {
      @Suppress("UNCHECKED_CAST")
      val detailBackstack = rememberNavBackStack(AppearancePreferencesScreen) as NavBackStack<Screen>
      val selectedScreen = detailBackstack.first()
      val selectDetail = { screen: Screen ->
        if (screen != selectedScreen) {
          // Replace the category atomically without ever exposing an empty stack.
          Snapshot.withMutableSnapshot {
            detailBackstack[0] = screen
            while (detailBackstack.size > 1) detailBackstack.removeAt(detailBackstack.lastIndex)
          }
        }
      }
      Row(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(0.4f).tvFocusGroup()) {
          SettingsPane(
            sections = sections,
            selectedScreen = selectedScreen,
            onScreenSelected = selectDetail,
            onPromoClick = selectDetail,
          )
        }
        VerticalDivider(
          modifier = Modifier.fillMaxHeight(),
          color = colorScheme.outlineVariant.copy(alpha = 0.5f),
          thickness = 1.dp,
        )
        Box(modifier = Modifier.weight(0.6f).tvFocusGroup()) {
          CompositionLocalProvider(LocalBackStack provides detailBackstack) {
            ScreenNavDisplay(backStack = detailBackstack, modifier = Modifier.fillMaxSize()) { screen ->
              CompositionLocalProvider(LocalShowSettingsBackArrow provides (screen != selectedScreen)) {
                screen.Content()
              }
            }
          }
        }
      }
    } else {
      SettingsPane(
        sections = sections,
        selectedScreen = null,
        onScreenSelected = {
          com.quantummpv.app.ui.utils.SharedNavigation.activeKey = com.quantummpv.app.ui.utils.SharedNavigation.screenKey(it)
          backstack.navigateTo(it)
        },
        onPromoClick = {
          // Not a tap on the permanent row, so no container transform from it.
          com.quantummpv.app.ui.utils.SharedNavigation.activeKey = null
          backstack.navigateTo(it)
        },
      )
    }
  }

  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  private fun SettingsPane(
    sections: List<SettingsSection>,
    selectedScreen: Screen?,
    onScreenSelected: (Screen) -> Unit,
    onPromoClick: (Screen) -> Unit,
  ) {
    val backstack = LocalBackStack.current
    val promos = rememberSettingsPromos()
    val colorScheme = MaterialTheme.colorScheme
    val emphasizedTypography = LocalEmphasizedTypography.current
    val initialFocusRequester = rememberTvInitialFocusRequester(enabled = promos.visible != null)
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val stagger =
      com.quantummpv.app.ui.utils.rememberEntranceStagger(
        key = "settings-root",
        ready = true,
        stepMillis = com.quantummpv.app.ui.theme.AppMotion.Timing.SETTINGS_STAGGER_STEP,
        maxItems = com.quantummpv.app.ui.theme.AppMotion.Timing.SETTINGS_STAGGER_ITEMS,
        fast = true,
      )
    // Global row index per section, so stagger order and tonal colours run across the whole list.
    val firstRowIndex = sections.runningFold(0) { acc, section -> acc + section.items.size }

    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      topBar = {
        LargeTopAppBar(
          scrollBehavior = scrollBehavior,
          title = {
            Text(
              text = stringResource(R.string.pref_preferences),
              fontWeight = emphasizedTypography.headlineSmall.fontWeight,
            )
          },
          navigationIcon = {
            IconButton(onClick = { backstack.popSafely() }) {
              Icon(
                Icons.RoundedFilled.ArrowBack,
                contentDescription = stringResource(R.string.back),
                tint = colorScheme.secondary,
              )
            }
          },
        )
      },
    ) { padding ->
      // Wait for the promo state so a dismissed or expired card never flashes and Appearance never jumps.
      val visiblePromos = promos.visible ?: return@Scaffold
      LazyColumn(
        modifier =
          Modifier
            .fillMaxSize()
            .tvFocusGroup()
            .padding(padding),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        item {
          SettingsSearchEntry(
            onClick = { backstack.navigateTo(SettingsSearchScreen) },
            modifier =
              Modifier
                .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 18.dp)
                .tvInitialFocus(initialFocusRequester),
          )
        }

        items(visiblePromos, key = { "promo-${it.name}" }) { promo ->
          val (title, icon, destination, dismissLabel) = promoContent(promo)
          val colors = MaterialTheme.colorScheme
          val (container, onContainer) =
            when (promo) {
              SettingsPromo.HELP_SUPPORT -> colors.primaryContainer to colors.onPrimaryContainer
              SettingsPromo.SUPPORT -> colors.tertiaryContainer to colors.onTertiaryContainer
            }
          SettingsPromoCard(
            title = title,
            icon = icon,
            dismissLabel = dismissLabel,
            container = container,
            onContainer = onContainer,
            onOpen = { onPromoClick(destination) },
            onDismiss = { promos.dismiss(promo) },
            modifier = appItemMotion().padding(horizontal = 16.dp),
          )
        }

        itemsIndexed(sections, key = { _, section -> section.title }) { sectionIndex, section ->
          SettingsSectionBlock(
            section = section,
            selectedScreen = selectedScreen,
            onItemClick = { destination -> onScreenSelected(destination.screen) },
            firstRowIndex = firstRowIndex[sectionIndex],
            stagger = stagger,
          )
        }
      }
    }
  }

  @Composable
  private fun settingsSections(): List<SettingsSection> {
    val colorScheme = MaterialTheme.colorScheme
    val secureFolderPreferences = koinInject<SecureFolderPreferences>()
    val isSecureFolderEntryHidden by secureFolderPreferences.isEntryPointHidden.collectAsState()
    return listOf(
      SettingsSection(
        title = stringResource(R.string.pref_section_appearance),
        tint = colorScheme.onSurfaceVariant,
        items =
          listOf(
            SettingsDestination(
              title = stringResource(R.string.pref_appearance_title),
              summary = stringResource(R.string.pref_appearance_summary),
              icon = Icons.RoundedFilled.Palette,
              screen = AppearancePreferencesScreen,
            ),
          ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_playback),
        tint = colorScheme.onSurfaceVariant,
        items =
          listOf(
            SettingsDestination(
              title = stringResource(R.string.pref_player),
              summary =
                listOf(
                  stringResource(R.string.pref_section_general),
                  stringResource(R.string.pref_player_seeking_title),
                  stringResource(R.string.pref_section_display_controls),
                  stringResource(R.string.pref_screenshots_section),
                ).joinToString(", "),
              icon = Icons.RoundedFilled.Slideshow,
              screen = PlayerPreferencesScreen,
            ),
            SettingsDestination(
              title = stringResource(R.string.pref_decoder),
              summary = stringResource(R.string.pref_decoder_summary),
              icon = Icons.RoundedFilled.DeveloperBoard,
              screen = DecoderPreferencesScreen,
            ),
            SettingsDestination(
              title = stringResource(R.string.pref_audio),
              summary = stringResource(R.string.pref_audio_summary),
              icon = Icons.RoundedFilled.Audiotrack,
              screen = AudioPreferencesScreen,
            ),
            SettingsDestination(
              title = stringResource(R.string.pref_subtitles),
              summary = stringResource(R.string.pref_subtitles_summary),
              icon = Icons.RoundedFilled.Subtitles,
              screen = SubtitlesPreferencesScreen,
            ),
          ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_gestures_controls),
        tint = colorScheme.onSurfaceVariant,
        items =
          listOf(
            SettingsDestination(
              title = stringResource(R.string.pref_gesture),
              summary = stringResource(R.string.pref_gesture_summary),
              icon = Icons.RoundedFilled.Gesture,
              screen = GesturePreferencesScreen,
            ),
            SettingsDestination(
              title = stringResource(R.string.pref_layout_title),
              summary = stringResource(R.string.pref_layout_summary),
              icon = Icons.RoundedFilled.GridView,
              screen = PlayerControlsPreferencesScreen,
            ),
          ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_storage),
        tint = colorScheme.onSurfaceVariant,
        items =
          listOfNotNull(
            SettingsDestination(
              title = stringResource(R.string.pref_folders_title),
              summary = stringResource(R.string.pref_folders_summary),
              icon = Icons.RoundedFilled.Folder,
              screen = FoldersPreferencesScreen,
            ),
            SettingsDestination(
              title = stringResource(R.string.pref_library_insights_title),
              summary = stringResource(R.string.pref_library_insights_summary),
              icon = Icons.RoundedFilled.Info,
              screen = LibraryInsightsScreen,
            ),
            if (!isSecureFolderEntryHidden) {
              SettingsDestination(
                title = stringResource(R.string.secure_folder_title),
                summary = stringResource(R.string.secure_folder_summary),
                icon = Icons.RoundedFilled.Lock,
                screen = SecureFolderGateScreen,
              )
            } else {
              null
            },
          ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_network),
        tint = colorScheme.onSurfaceVariant,
        items =
          listOf(
            SettingsDestination(
              title = stringResource(R.string.pref_media_servers_title),
              summary = stringResource(R.string.pref_media_servers_summary),
              icon = Icons.RoundedFilled.BringYourOwnIp,
              screen = MediaServersPreferencesScreen,
            ),
            SettingsDestination(
              title = stringResource(R.string.ui_network),
              summary = listOf(
                stringResource(R.string.pref_section_p2p_streaming),
                stringResource(R.string.ui_yt_dlp_manager),
              ).joinToString(", "),
              icon = Icons.RoundedFilled.SignalWifiStatusbarConnectedNoInternet4,
              screen = NetworkConfigurationPreferencesScreen,
            ),
          ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_ai),
        tint = colorScheme.onSurfaceVariant,
        items =
          listOf(
            SettingsDestination(
              title = stringResource(R.string.pref_section_ai_title),
              summary = stringResource(R.string.pref_section_ai_summary),
              icon = Icons.RoundedFilled.AutoAwesome,
              screen = AiIntegrationScreen,
            ),
          ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_advanced),
        tint = colorScheme.onSurfaceVariant,
        items =
          listOf(
            SettingsDestination(
              title = stringResource(R.string.pref_advanced),
              summary =
                listOf(
                  stringResource(R.string.pref_section_app_language),
                  stringResource(R.string.pref_section_backup_restore),
                  stringResource(R.string.pref_section_mpv_config),
                  stringResource(R.string.ui_network),
                ).joinToString(", "),
              icon = Icons.Alternatives.AdvancedSettings,
              screen = AdvancedPreferencesScreen,
            ),
            SettingsDestination(
              title = stringResource(R.string.pref_codecs_title),
              summary = stringResource(R.string.pref_codecs_summary),
              icon = Icons.RoundedFilled.Memory,
              screen = CodecCapabilitiesScreen,
            ),
          ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_about),
        tint = colorScheme.onSurfaceVariant,
        items =
          listOf(
            SettingsDestination(
              title = stringResource(R.string.pref_about_title),
              summary = stringResource(R.string.pref_about_summary),
              icon = Icons.RoundedFilled.Info,
              screen = AboutScreen,
            ),
          ),
      ),
      SettingsSection(
        title = stringResource(R.string.pref_section_support),
        tint = colorScheme.primary,
        highlighted = true,
        items =
          listOf(
            SettingsDestination(
              title = stringResource(R.string.help_support_title),
              summary = stringResource(R.string.help_support_summary),
              icon = Icons.RoundedFilled.HelpOutline,
              screen = HelpSupportScreen,
            ),
          ),
      ),
    )
  }
}

@Composable
private fun SettingsSearchEntry(
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier =
      modifier
        .fillMaxWidth()
        .tvFocusHighlight(MaterialTheme.shapes.extraExtraLarge, focusedScale = 1.02f)
        .clip(MaterialTheme.shapes.extraExtraLarge)
        .clickable(onClick = onClick),
    shape = MaterialTheme.shapes.extraExtraLarge,
    color = MaterialTheme.colorScheme.secondaryContainer,
    tonalElevation = 1.dp,
    shadowElevation = 1.dp,
  ) {
    Row(
      modifier =
        Modifier
          .fillMaxWidth()
          .padding(horizontal = 18.dp, vertical = 16.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(
        imageVector = Icons.RoundedFilled.Search,
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = MaterialTheme.colorScheme.onSecondaryContainer,
      )
      Spacer(modifier = Modifier.width(14.dp))
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = stringResource(R.string.settings_search_hint),
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.onSecondaryContainer,
        )
      }
      Icon(
        imageVector = Icons.RoundedFilled.ChevronRight,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.72f),
      )
    }
  }
}

@Composable
private fun SettingsSectionBlock(
  section: SettingsSection,
  selectedScreen: Screen?,
  onItemClick: (SettingsDestination) -> Unit,
  firstRowIndex: Int,
  stagger: com.quantummpv.app.ui.utils.EntranceStagger,
) {
  val emphasizedTypography = LocalEmphasizedTypography.current

  Column(
    modifier =
      Modifier
        .fillMaxWidth()
        .padding(top = 10.dp, bottom = 14.dp),
  ) {
    Column(
      modifier = Modifier.padding(horizontal = 24.dp),
    ) {
      Text(
        text = section.title,
        style = emphasizedTypography.titleLarge,
        color = MaterialTheme.colorScheme.onSurface,
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    SettingsDestinationGroup(
      section = section,
      selectedScreen = selectedScreen,
      onItemClick = onItemClick,
      firstRowIndex = firstRowIndex,
      stagger = stagger,
      modifier = Modifier.padding(horizontal = 16.dp),
    )
  }
}

@Composable
private fun SettingsDestinationGroup(
  section: SettingsSection,
  selectedScreen: Screen?,
  onItemClick: (SettingsDestination) -> Unit,
  firstRowIndex: Int,
  stagger: com.quantummpv.app.ui.utils.EntranceStagger,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.extraLargeIncreased,
    color = if (section.highlighted) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainerLow,
    tonalElevation = 1.dp,
  ) {
    Column(modifier = Modifier.fillMaxWidth()) {
      section.items.forEachIndexed { index, item ->
        SettingsDestinationRow(
          item = item,
          rowIndex = firstRowIndex + index,
          isSelected = selectedScreen == item.screen,
          highlighted = section.highlighted,
          onClick = { onItemClick(item) },
          modifier = Modifier.staggeredEntrance(firstRowIndex + index, stagger),
        )
        if (index < section.items.lastIndex) {
          HorizontalDivider(
            modifier = Modifier.padding(start = 14.dp, end = 18.dp),
            color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f),
          )
        }
      }
    }
  }
}

@Composable
private fun SettingsDestinationRow(
  item: SettingsDestination,
  rowIndex: Int,
  isSelected: Boolean,
  highlighted: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  // Tonal containers cycle primary -> secondary -> tertiary so the list follows dynamic color.
  val colors = MaterialTheme.colorScheme
  val (iconContainer, iconContent) =
    when {
      highlighted -> colors.primary to colors.onPrimary
      rowIndex % 3 == 0 -> colors.primaryContainer to colors.onPrimaryContainer
      rowIndex % 3 == 1 -> colors.secondaryContainer to colors.onSecondaryContainer
      else -> colors.tertiaryContainer to colors.onTertiaryContainer
    }
  // On the tinted group the text must pair with the primary container, not the plain surface.
  val titleColor = if (highlighted) colors.onPrimaryContainer else colors.onSurface
  val summaryColor = if (highlighted) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant
  val rowBgColor =
    if (isSelected) {
      MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f)
    } else {
      Color.Transparent
    }
  Row(
    modifier =
      modifier
        .fillMaxWidth()
        // Container transform into the sub-screen; the corner radius morphs on the way.
        .sharedNavBounds(com.quantummpv.app.ui.utils.SharedNavigation.screenKey(item.screen), MaterialTheme.shapes.medium)
        .tvFocusHighlight(MaterialTheme.shapes.medium, focusedScale = 1.01f)
        .background(rowBgColor)
        .clickable(onClick = onClick)
        .padding(horizontal = 14.dp, vertical = 13.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Surface(
      shape = MaterialTheme.shapes.largeIncreased,
      color = iconContainer,
    ) {
      Box(
        modifier = Modifier.size(48.dp),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = item.icon,
          contentDescription = null,
          modifier = Modifier.size(26.dp),
          tint = iconContent,
        )
      }
    }

    Spacer(modifier = Modifier.width(14.dp))

    Column(modifier = Modifier.weight(1f)) {
      Text(
        text = item.title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = if (highlighted) FontWeight.Bold else FontWeight.SemiBold,
        color = titleColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = item.summary,
        style = MaterialTheme.typography.bodyMedium,
        color = summaryColor,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }

    Spacer(modifier = Modifier.width(10.dp))

    Icon(
      imageVector = Icons.RoundedFilled.ChevronRight,
      contentDescription = null,
      modifier = Modifier.size(24.dp),
      tint = if (highlighted) summaryColor else MaterialTheme.colorScheme.outline,
    )
  }
}

private data class PromoContent(
  val title: String,
  val icon: AppIcon,
  val destination: Screen,
  val dismissLabel: String,
)

@Composable
private fun promoContent(promo: SettingsPromo): PromoContent =
  when (promo) {
    SettingsPromo.HELP_SUPPORT ->
      PromoContent(
        title = stringResource(R.string.help_support_title),
        icon = Icons.RoundedFilled.HelpOutline,
        destination = HelpSupportScreen,
        dismissLabel = stringResource(R.string.settings_promo_dismiss_help_support),
      )
    SettingsPromo.SUPPORT ->
      PromoContent(
        title = stringResource(R.string.support_title),
        icon = Icons.RoundedFilled.Favorite,
        destination = SupportScreen,
        dismissLabel = stringResource(R.string.settings_promo_dismiss_support),
      )
  }

private class SettingsPromos(
  /** Null until the state is resolved; the list waits for it. */
  val visible: List<SettingsPromo>?,
  val dismiss: (SettingsPromo) -> Unit,
)

/**
 * The cards to show now. Rechecked on resume and wall-clock change, plus one timer for the moment
 * the earliest visible card expires; there is no polling.
 */
@Composable
private fun rememberSettingsPromos(): SettingsPromos {
  val context = LocalContext.current
  val prefs = koinInject<SettingsPromoPreferences>()
  val scope = rememberCoroutineScope()
  var snapshot by remember { mutableStateOf(prefs.cached) }
  var resolved by remember { mutableStateOf(prefs.cached != null) }
  var now by remember { mutableLongStateOf(System.currentTimeMillis()) }

  suspend fun recheck() {
    withContext(Dispatchers.IO) { prefs.resolve(context) }
    // Read after the hop: a dismissal made meanwhile is already in the cache, the resolve result may not be.
    snapshot = prefs.cached
    now = System.currentTimeMillis()
    resolved = true
  }

  // Also runs for the initial resume, which covers the first resolve when App has not prewarmed.
  LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { recheck() } }
  DisposableEffect(context) {
    val receiver =
      object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
          scope.launch { recheck() }
        }
      }
    ContextCompat.registerReceiver(context, receiver, IntentFilter(Intent.ACTION_TIME_CHANGED), ContextCompat.RECEIVER_NOT_EXPORTED)
    onDispose { context.unregisterReceiver(receiver) }
  }

  val visible = if (resolved) snapshot?.visible(now).orEmpty() else null
  val end = snapshot?.end
  LaunchedEffect(end, visible.isNullOrEmpty(), now) {
    if (end != null && !visible.isNullOrEmpty()) {
      delay(end - now)
      recheck()
    }
  }

  return SettingsPromos(visible) { promo ->
    snapshot?.let { snapshot = prefs.dismiss(it.version, promo) ?: it }
  }
}

/** A compact tonal card: the open and dismiss actions are siblings so each has its own target and focus. */
@Composable
private fun SettingsPromoCard(
  title: String,
  icon: AppIcon,
  dismissLabel: String,
  container: Color,
  onContainer: Color,
  onOpen: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier.fillMaxWidth(),
    shape = MaterialTheme.shapes.medium,
    color = container,
    contentColor = onContainer,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Row(
        modifier =
          Modifier
            .weight(1f)
            .heightIn(min = 48.dp)
            .tvFocusHighlight(MaterialTheme.shapes.medium, focusedScale = 1.01f)
            .clickable(role = Role.Button, onClick = onOpen)
            .padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Icon(imageVector = icon, contentDescription = null, modifier = Modifier.size(24.dp), tint = onContainer)
        Spacer(modifier = Modifier.width(12.dp))
        Text(
          text = title,
          style = MaterialTheme.typography.titleMedium,
          fontWeight = FontWeight.SemiBold,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      IconButton(
        onClick = onDismiss,
        modifier = Modifier.size(48.dp).tvFocusHighlight(CircleShape),
      ) {
        Icon(Icons.RoundedFilled.Close, contentDescription = dismissLabel, modifier = Modifier.size(20.dp), tint = onContainer)
      }
    }
  }
}
