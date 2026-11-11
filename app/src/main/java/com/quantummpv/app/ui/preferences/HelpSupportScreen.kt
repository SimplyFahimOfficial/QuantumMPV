/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.preferences

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quantummpv.app.R
import com.quantummpv.app.presentation.Screen
import com.quantummpv.app.support.SupportConfig
import com.quantummpv.app.support.SupportIntents
import com.quantummpv.app.ui.icons.AppIcon
import com.quantummpv.app.ui.icons.Icon
import com.quantummpv.app.ui.icons.Icons
import com.quantummpv.app.ui.utils.LocalBackStack
import com.quantummpv.app.ui.utils.LocalShowSettingsBackArrow
import com.quantummpv.app.ui.utils.navigateTo
import com.quantummpv.app.ui.utils.popSafely
import com.quantummpv.app.ui.utils.rememberAppHaptics
import kotlinx.serialization.Serializable

/**
 * Help & Support: community help, bug reports and project links. Donations sit one level down on
 * [SupportScreen], so asking for help never goes through payment options. Links come from [SupportConfig].
 */
@Serializable
object HelpSupportScreen : Screen {
  @OptIn(ExperimentalMaterial3Api::class)
  @Composable
  override fun Content() {
    val context = LocalContext.current
    val backstack = LocalBackStack.current
    val haptics = rememberAppHaptics()

    fun open(url: String) {
      haptics.tick()
      SupportIntents.openLink(context, url)
    }

    Scaffold(
      topBar = {
        TopAppBar(
          title = {
            Text(
              modifier = Modifier.settingsSearchTarget(R.string.help_support_title),
              text = stringResource(R.string.help_support_title),
              style = MaterialTheme.typography.headlineSmall,
              fontWeight = FontWeight.ExtraBold,
              color = MaterialTheme.colorScheme.primary,
            )
          },
          navigationIcon = {
            if (LocalShowSettingsBackArrow.current) {
              IconButton(onClick = { backstack.popSafely() }) {
                Icon(
                  imageVector = Icons.RoundedFilled.ArrowBack,
                  contentDescription = stringResource(R.string.ui_go_back),
                  tint = MaterialTheme.colorScheme.secondary,
                )
              }
            }
          },
        )
      },
    ) { padding ->
      Column(modifier = Modifier.padding(padding).verticalScroll(rememberScrollState())) {
        val headingModifier = Modifier.semantics { heading() }

        PreferenceSectionHeader(stringResource(R.string.help_section_get_help), headingModifier)
        PreferenceCard {
          SupportLinkRow(
            painter = R.drawable.ic_telegram,
            title = stringResource(R.string.help_telegram_title),
            subtitle = stringResource(R.string.help_telegram_summary),
            external = true,
            onClick = { open(SupportConfig.TELEGRAM_GROUP_URL) },
          )
          PreferenceDivider()
          SupportLinkRow(
            icon = Icons.RoundedFilled.BugReport,
            title = stringResource(R.string.help_report_bug_title),
            subtitle = stringResource(R.string.help_report_bug_summary),
            external = true,
            onClick = { open(SupportConfig.ISSUES_URL) },
          )
        }

        PreferenceSectionHeader(stringResource(R.string.help_section_project), headingModifier)
        PreferenceCard {
          SupportLinkRow(
            icon = Icons.RoundedFilled.Language,
            title = stringResource(R.string.help_website_title),
            subtitle = stringResource(R.string.help_website_summary),
            external = true,
            onClick = { open(SupportConfig.PROJECT_WEBSITE_URL) },
          )
        }

        PreferenceSectionHeader(stringResource(R.string.help_section_support), headingModifier)
        // Same tertiary accent as the Support My Work card in About, so the two read as one thing.
        SupportHighlightCard(
          icon = Icons.RoundedFilled.Favorite,
          title = stringResource(R.string.support_title),
          subtitle = stringResource(R.string.support_entry_summary),
          container = MaterialTheme.colorScheme.tertiaryContainer,
          onContainer = MaterialTheme.colorScheme.onTertiaryContainer,
          accent = MaterialTheme.colorScheme.tertiary,
          onAccent = MaterialTheme.colorScheme.onTertiary,
          external = false,
          onClick = { backstack.navigateTo(SupportScreen) },
        )

        PreferenceSectionHeader(stringResource(R.string.help_section_developer), headingModifier)
        PreferenceCard {
          SupportLinkRow(
            icon = Icons.RoundedFilled.Person,
            title = stringResource(R.string.help_contact_title),
            subtitle = stringResource(R.string.help_contact_summary),
            external = true,
            onClick = { open(SupportConfig.CONTACT_URL) },
          )
        }

        Spacer(Modifier.height(24.dp))
      }
    }
  }
}

/**
 * A full-width tinted card for the one action that deserves emphasis on its screen. Like
 * [SupportLinkRow] it ends in a chevron for an in-app screen or an open-in-new mark for a link that leaves the app.
 */
@Composable
internal fun SupportHighlightCard(
  icon: AppIcon,
  title: String,
  subtitle: String,
  container: Color,
  onContainer: Color,
  accent: Color,
  onAccent: Color,
  external: Boolean,
  onClick: () -> Unit,
) {
  Card(
    onClick = onClick,
    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
    shape = MaterialTheme.shapes.extraLargeIncreased,
    colors = CardDefaults.cardColors(containerColor = container, contentColor = onContainer),
  ) {
    Row(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Surface(shape = CircleShape, color = accent, modifier = Modifier.size(40.dp)) {
        Box(contentAlignment = Alignment.Center) {
          Icon(imageVector = icon, contentDescription = null, tint = onAccent, modifier = Modifier.size(22.dp))
        }
      }
      Spacer(Modifier.size(14.dp))
      Column(Modifier.weight(1f)) {
        Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
        Text(subtitle, style = MaterialTheme.typography.bodySmall, color = onContainer.copy(alpha = 0.8f))
      }
      Spacer(Modifier.size(8.dp))
      if (external) {
        Icon(
          imageVector = Icons.RoundedFilled.OpenInNew,
          contentDescription = stringResource(R.string.help_opens_externally),
          tint = onContainer.copy(alpha = 0.8f),
          modifier = Modifier.size(20.dp),
        )
      } else {
        Icon(imageVector = Icons.RoundedFilled.ChevronRight, contentDescription = null, tint = onContainer.copy(alpha = 0.8f))
      }
    }
  }
}

/** One tappable row: a chevron for an in-app screen, an open-in-new mark for a link that leaves the app. */
@Composable
internal fun SupportLinkRow(
  title: String,
  subtitle: String,
  external: Boolean,
  onClick: () -> Unit,
  icon: AppIcon? = null,
  @DrawableRes painter: Int? = null,
) {
  val cs = MaterialTheme.colorScheme
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clickable(role = Role.Button, onClick = onClick)
        .padding(horizontal = 16.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    if (painter != null) {
      Icon(painter = painterResource(painter), contentDescription = null, tint = cs.primary, modifier = Modifier.size(22.dp))
    } else if (icon != null) {
      Icon(imageVector = icon, contentDescription = null, tint = cs.primary, modifier = Modifier.size(22.dp))
    }
    Spacer(Modifier.size(16.dp))
    Column(Modifier.weight(1f)) {
      Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
      Text(subtitle, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
    }
    Spacer(Modifier.size(8.dp))
    if (external) {
      Icon(
        imageVector = Icons.RoundedFilled.OpenInNew,
        contentDescription = stringResource(R.string.help_opens_externally),
        tint = cs.outline,
        modifier = Modifier.size(20.dp),
      )
    } else {
      Icon(imageVector = Icons.RoundedFilled.ChevronRight, contentDescription = null, tint = cs.outline)
    }
  }
}
