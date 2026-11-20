/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.preferences

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.pm.PackageInfoCompat

/** The temporary cards shown above the first Settings section, in display order. */
enum class SettingsPromo(
  internal val dismissedKey: String,
) {
  HELP_SUPPORT("dismissed_help_support"),
  SUPPORT("dismissed_support"),
}

/**
 * Per-app-version state of the temporary Settings cards.
 *
 * The first time a version is observed, its seven-day window is recorded once from the package's
 * install/update time. Reopening Settings, reinstalling the same version code, or moving the clock
 * never reopens that window, and a new version code starts a fresh one with both cards back.
 *
 * Kept in its own file ([FILE_NAME]) so backup rules exclude it and settings import/export never
 * touch it: a restored dismissal or window from another device would be wrong here.
 */
class SettingsPromoPreferences(
  private val prefs: SharedPreferences,
  private val clock: () -> Long = System::currentTimeMillis,
) {
  data class Snapshot(
    val version: Long,
    val start: Long,
    val dismissed: Set<SettingsPromo>,
    /** Set once expiry has been observed, so a clock rollback cannot revive the cards. */
    val expired: Boolean,
  ) {
    val end: Long get() = start + WINDOW_MILLIS

    fun visible(now: Long): List<SettingsPromo> =
      if (expired || now < start || now >= end) {
        emptyList()
      } else {
        SettingsPromo.entries.filter { it !in dismissed }
      }
  }

  /** The state resolved in this process, if any. A process only ever runs one version. */
  @Volatile
  var cached: Snapshot? = null
    private set

  /**
   * Returns the state for [version], recording its window from [installedAt] if this is the first
   * time the version is seen. An [installedAt] that is missing or in the future is invalid and
   * records the version with no window, instead of opening a new one.
   */
  @Synchronized
  fun resolve(
    version: Long,
    installedAt: Long,
  ): Snapshot {
    val now = clock()
    var snapshot = cached?.takeIf { it.version == version } ?: load(version) ?: initialize(version, installedAt, now)
    if (!snapshot.expired && now >= snapshot.end) {
      prefs.edit().putBoolean(KEY_EXPIRED, true).apply()
      snapshot = snapshot.copy(expired = true)
    }
    cached = snapshot
    return snapshot
  }

  /**
   * Hides one card for [version]. Only that card's key is written, so dismissing one never
   * overwrites the other, and a write for a version that is no longer stored is ignored.
   */
  @Synchronized
  fun dismiss(
    version: Long,
    promo: SettingsPromo,
  ): Snapshot? {
    val stored = load(version) ?: return cached
    prefs.edit().putBoolean(promo.dismissedKey, true).apply()
    return stored.copy(dismissed = stored.dismissed + promo).also { cached = it }
  }

  private fun load(version: Long): Snapshot? {
    if (!prefs.contains(KEY_VERSION) || prefs.getLong(KEY_VERSION, 0L) != version) return null
    return Snapshot(
      version = version,
      start = prefs.getLong(KEY_START, 0L),
      dismissed = SettingsPromo.entries.filterTo(mutableSetOf()) { prefs.getBoolean(it.dismissedKey, false) },
      expired = prefs.getBoolean(KEY_EXPIRED, false),
    )
  }

  private fun initialize(
    version: Long,
    installedAt: Long,
    now: Long,
  ): Snapshot {
    val valid = installedAt in 1..now
    val snapshot = Snapshot(version, start = if (valid) installedAt else 0L, dismissed = emptySet(), expired = !valid)
    // One edit replaces every field of the previous version, so a reader never sees a mix.
    prefs
      .edit()
      .clear()
      .putLong(KEY_VERSION, version)
      .putLong(KEY_START, snapshot.start)
      .putBoolean(KEY_EXPIRED, snapshot.expired)
      .apply()
    return snapshot
  }

  companion object {
    const val FILE_NAME = "settings_promos"
    const val WINDOW_MILLIS = 7L * 24 * 60 * 60 * 1000

    private const val KEY_VERSION = "version"
    private const val KEY_START = "start"
    private const val KEY_EXPIRED = "expired"
  }
}

/**
 * Resolves against the installed package: the real long version code (which differs per ABI/channel
 * build) and the last update time, falling back to the first install time. Blocking; call off-main.
 */
fun SettingsPromoPreferences.resolve(context: Context): SettingsPromoPreferences.Snapshot? =
  try {
    val info = context.packageManager.getPackageInfo(context.packageName, 0)
    resolve(
      version = PackageInfoCompat.getLongVersionCode(info),
      installedAt = if (info.lastUpdateTime > 0) info.lastUpdateTime else info.firstInstallTime,
    )
  } catch (_: android.content.pm.PackageManager.NameNotFoundException) {
    // Unknown identity: show nothing and record nothing, rather than guess a window.
    null
  }
