/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.downloads

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Android 13+ needs the user's consent before download progress can be shown as a notification. */
internal fun needsNotificationPermission(
  sdkInt: Int,
  granted: Boolean,
): Boolean = sdkInt >= Build.VERSION_CODES.TIRAMISU && !granted

/**
 * Returns a function to call when the user starts a download: it asks for the notification
 * permission if it is missing and returns at once. The download itself never waits for the answer,
 * and a refusal only means no progress notification.
 */
@Composable
fun rememberDownloadNotificationRequest(): () -> Unit {
  val context = LocalContext.current
  val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
  return remember(context, launcher) {
    {
      val granted =
        ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
          PackageManager.PERMISSION_GRANTED
      if (needsNotificationPermission(Build.VERSION.SDK_INT, granted)) {
        launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }
}
