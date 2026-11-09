/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.domain.update

import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/** Redirects only old preview APK paths to the verified current Pages host. */
internal fun normalizePreviewAssetDownloadUrl(downloadUrl: String): String {
  val url = downloadUrl.toHttpUrlOrNull() ?: return downloadUrl
  if (
    !url.isHttps ||
      url.host != LEGACY_PREVIEW_HOST ||
      !url.encodedPath.startsWith(PREVIEW_DOWNLOADS_PATH)
  ) {
    return downloadUrl
  }
  return url.newBuilder().host(CURRENT_PREVIEW_HOST).build().toString()
}

private const val LEGACY_PREVIEW_HOST = "fahimofficial.github.io"
private const val CURRENT_PREVIEW_HOST = "simplyfahimofficial.github.io"
private const val PREVIEW_DOWNLOADS_PATH = "/QuantumMPV/downloads/"
