package com.quantummpv.app.ui.player

import org.junit.Assert.assertEquals
import org.junit.Test

class PlaybackHttpHeadersTest {
  @Test
  fun preservesNativeMpvHeaderEntries() {
    assertEquals(
      linkedMapOf(
        "Authorization" to "Bearer token",
        "X-Values" to "one,two",
        "X-Path" to "a\\b",
      ),
      PlaybackHttpHeaders.fromMpvHeaderFields(
        listOf(
          "Authorization: Bearer token",
          "X-Values: one,two",
          "X-Path: a\\b",
        ),
      ),
    )
  }

  @Test
  fun generatedUserAgentWinsWhenNoConfiguredDefaultExists() {
    assertEquals(
      "generated-android-agent",
      PlaybackHttpHeaders.effectiveUserAgent(
        itemHeaders = mapOf("User-Agent" to "generated-android-agent"),
        defaultUserAgent = null,
        defaultHeaders = emptyMap(),
        hasCallerSuppliedHeaders = false,
      ),
    )
  }

  @Test
  fun configuredUserAgentWinsOverGeneratedFallback() {
    assertEquals(
      "configured-agent",
      PlaybackHttpHeaders.effectiveUserAgent(
        itemHeaders = mapOf("User-Agent" to "generated-android-agent"),
        defaultUserAgent = "configured-agent",
        defaultHeaders = emptyMap(),
        hasCallerSuppliedHeaders = false,
      ),
    )
  }

  @Test
  fun generatedFallbackHeadersDoNotReplaceConfiguredMpvHeaders() {
    val itemHeaders =
      linkedMapOf(
        "Referer" to "https://generated.example/",
        "User-Agent" to "generated-agent",
      )
    val defaults =
      linkedMapOf(
        "Authorization" to "Bearer configured",
        "Referer" to "https://configured.example/",
      )

    assertEquals(
      linkedMapOf(
        "User-Agent" to "generated-agent",
        "Authorization" to "Bearer configured",
        "Referer" to "https://configured.example/",
      ),
      PlaybackHttpHeaders.forPlaybackItem(itemHeaders, emptyMap(), defaults),
    )
  }

  @Test
  fun callerSuppliedHeadersKeepTheirExistingOverrideBehavior() {
    val itemHeaders =
      linkedMapOf(
        "Authorization" to "Bearer item",
        "Referer" to "https://generated.example/",
        "User-Agent" to "generated-agent",
      )
    val callerHeaders = mapOf("Authorization" to "Bearer item")

    assertEquals(
      itemHeaders,
      PlaybackHttpHeaders.forPlaybackItem(
        itemHeaders = itemHeaders,
        callerSuppliedHeaders = callerHeaders,
        defaultHeaders = mapOf("X-Configured" to "retained only for headerless items"),
      ),
    )
  }
}
