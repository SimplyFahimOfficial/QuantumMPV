package com.quantummpv.app.domain.update

import org.junit.Assert.assertEquals
import org.junit.Test

class PreviewAssetUrlsTest {
  @Test
  fun rewritesLegacyPreviewApkUrlToVerifiedCurrentHost() {
    assertEquals(
      "https://simplyfahimofficial.github.io/QuantumMPV/downloads/QuantumMPV-preview-r2856-universal.apk",
      normalizePreviewAssetDownloadUrl(
        "https://fahimofficial.github.io/QuantumMPV/downloads/QuantumMPV-preview-r2856-universal.apk",
      ),
    )
  }

  @Test
  fun leavesCurrentAndUnrelatedUrlsUnchanged() {
    val current = "https://simplyfahimofficial.github.io/QuantumMPV/downloads/current.apk"
    val unrelated = "https://github.com/SimplyFahimOfficial/QuantumMPV/releases/latest"
    val wrongPath = "https://fahimofficial.github.io/QuantumMPV/other/current.apk"

    assertEquals(current, normalizePreviewAssetDownloadUrl(current))
    assertEquals(unrelated, normalizePreviewAssetDownloadUrl(unrelated))
    assertEquals(wrongPath, normalizePreviewAssetDownloadUrl(wrongPath))
  }
}
