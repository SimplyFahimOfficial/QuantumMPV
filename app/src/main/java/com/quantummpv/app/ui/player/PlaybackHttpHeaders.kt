/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.player

/** Header parsing and serialization shared by every HTTP playback entry point. */
internal object PlaybackHttpHeaders {
  private val headerNamePattern = Regex("^[!#$%&'*+.^_`|~0-9A-Za-z-]+$")

  fun fromFlatPairs(values: Array<String>?): Map<String, String> {
    if (values == null) return emptyMap()
    val headers = linkedMapOf<String, String>()
    values.asList().chunked(2).forEach { pair ->
      if (pair.size == 2) put(headers, pair[0], pair[1])
    }
    return headers
  }

  fun merge(vararg sources: Map<String, String>): Map<String, String> {
    val headers = linkedMapOf<String, String>()
    sources.forEach { source -> source.forEach { (name, value) -> put(headers, name, value) } }
    return headers
  }

  fun withDefault(
    headers: Map<String, String>,
    name: String,
    value: String?,
  ): Map<String, String> {
    if (value.isNullOrBlank() || value(headers, name) != null) return headers
    return merge(headers, mapOf(name to value))
  }

  fun value(
    headers: Map<String, String>,
    name: String,
  ): String? = headers.entries.firstOrNull { it.key.equals(name, ignoreCase = true) }?.value

  fun userAgent(headers: Map<String, String>): String? = value(headers, "User-Agent")

  fun fromMpvHeaderFields(value: String?): Map<String, String> {
    if (value.isNullOrBlank()) return emptyMap()
    val fields = linkedMapOf<String, String>()
    val field = StringBuilder()
    var escaped = false
    val entries = mutableListOf<String>()
    value.forEach { character ->
      when {
        escaped -> {
          field.append(character)
          escaped = false
        }
        character == '\\' -> escaped = true
        character == ',' -> {
          entries += field.toString()
          field.clear()
        }
        else -> field.append(character)
      }
    }
    if (escaped) field.append('\\')
    entries += field.toString()
    entries.forEach { entry ->
      val separator = entry.indexOf(':')
      if (separator > 0) put(fields, entry.substring(0, separator), entry.substring(separator + 1))
    }
    return fields
  }

  fun forPlaybackItem(
    itemHeaders: Map<String, String>,
    callerSuppliedHeaders: Map<String, String>,
    defaultHeaders: Map<String, String>,
  ): Map<String, String> =
    if (callerSuppliedHeaders.isNotEmpty()) {
      merge(itemHeaders)
    } else {
      // Explicit mpv.conf values win over generated Referer/User-Agent fallbacks.
      merge(itemHeaders, defaultHeaders)
    }

  fun toMpvHeaderFields(headers: Map<String, String>): String =
    headers.entries
      .filterNot { it.key.equals("User-Agent", ignoreCase = true) }
      .joinToString(",") { (name, value) -> "$name: ${escapeMpvListValue(value)}" }

  private fun put(
    target: MutableMap<String, String>,
    rawName: String,
    rawValue: String,
  ) {
    val name = rawName.trim()
    val value = rawValue.trim()
    if (!headerNamePattern.matches(name) || value.any { it == '\r' || it == '\n' || it == '\u0000' }) return
    target.keys.firstOrNull { it.equals(name, ignoreCase = true) }?.let { existing -> target.remove(existing) }
    target[name] = value
  }

  private fun escapeMpvListValue(value: String): String =
    value
      .replace("\\", "\\\\")
      .replace(",", "\\,")
}
