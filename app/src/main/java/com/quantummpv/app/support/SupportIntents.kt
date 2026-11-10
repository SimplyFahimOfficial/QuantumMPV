/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.support

import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import com.quantummpv.app.R
import com.quantummpv.app.utils.clipboard.SafeClipboard
import java.io.File
import java.io.IOException
import java.io.OutputStream

/** Android side of the Help & Support and Support My Work screens: links, UPI intents and QR image export. */
object SupportIntents {
  private const val SHARE_DIR = "shared"
  const val QR_FILE_NAME = "SimplyFahim-UPI-QR.png"

  /**
   * Opens an https link with ACTION_VIEW (browser, or the Telegram or GitHub app if it claims the
   * link). When no app can handle it, copies the link and says so in a toast, so the user can
   * still paste it somewhere. Returns true when an app opened.
   */
  fun openLink(
    context: Context,
    link: String,
  ): Boolean {
    val uri = link.toUri()
    val intent =
      when (uri.scheme) {
        "https", "http" -> Intent(Intent.ACTION_VIEW, uri).addCategory(Intent.CATEGORY_BROWSABLE)
        else -> null
      }
    val opened =
      intent != null &&
        try {
          context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
          true
        } catch (_: ActivityNotFoundException) {
          false
        } catch (_: SecurityException) {
          false
        }
    if (!opened) {
      val copied = runCatching { SafeClipboard.copyPlainText(context, "support_link", link, showToast = false) }.isSuccess
      val message = if (copied) R.string.support_link_copied_instead else R.string.support_link_open_failed
      Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }
    return opened
  }

  private fun upiIntent(): Intent = Intent(Intent.ACTION_VIEW, SupportConfig.upiPaymentUri.toUri())

  /** Number of installed apps that handle `upi://pay`. Needs the `<queries>` entry on API 30+. */
  fun installedUpiAppCount(context: Context): Int {
    val pm = context.packageManager
    val intent = upiIntent()
    val matches =
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(PackageManager.MATCH_DEFAULT_ONLY.toLong()))
      } else {
        @Suppress("DEPRECATION")
        pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY)
      }
    return matches.size
  }

  /**
   * Shows the system app picker for UPI apps. Returns false when no UPI app is installed.
   * A successful launch only means an app opened. It says nothing about whether anyone paid.
   */
  fun openUpiApp(
    context: Context,
    chooserTitle: CharSequence,
  ): Boolean {
    if (installedUpiAppCount(context) == 0) return false
    val chooser =
      Intent
        .createChooser(upiIntent(), chooserTitle)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    return try {
      context.startActivity(chooser)
      true
    } catch (_: ActivityNotFoundException) {
      false
    }
  }

  fun createQrBitmap(sizePx: Int): Bitmap {
    val qr = UpiQrCode.encode(SupportConfig.upiPaymentUri, sizePx)
    return Bitmap.createBitmap(qr.argb, qr.width, qr.height, Bitmap.Config.ARGB_8888)
  }

  @Throws(IOException::class)
  fun writeQrPng(
    bitmap: Bitmap,
    out: OutputStream,
  ) {
    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)) {
      throw IOException("PNG encoding failed")
    }
    out.flush()
  }

  /** Writes the QR to the app cache and opens the share sheet. Uses the existing FileProvider. */
  @Throws(IOException::class)
  fun shareQr(
    context: Context,
    bitmap: Bitmap,
    shareText: String,
    chooserTitle: CharSequence,
  ) {
    val dir = File(context.cacheDir, SHARE_DIR).apply { mkdirs() }
    val file = File(dir, QR_FILE_NAME)
    file.outputStream().use { writeQrPng(bitmap, it) }
    val uri: Uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
    val send =
      Intent(Intent.ACTION_SEND)
        .setType("image/png")
        .putExtra(Intent.EXTRA_STREAM, uri)
        .putExtra(Intent.EXTRA_TEXT, shareText)
        .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    send.clipData = ClipData.newRawUri(QR_FILE_NAME, uri)
    context.startActivity(
      Intent.createChooser(send, chooserTitle).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    )
  }
}
