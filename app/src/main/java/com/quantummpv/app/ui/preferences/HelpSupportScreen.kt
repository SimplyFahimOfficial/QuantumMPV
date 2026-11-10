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
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
        PreferenceCard {
          SupportLinkRow(
            icon = Icons.RoundedFilled.Favorite,
            title = stringResource(R.string.support_title),
            subtitle = stringResource(R.string.support_entry_summary),
            external = false,
            onClick = { backstack.navigateTo(SupportScreen) },
          )
        }

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
