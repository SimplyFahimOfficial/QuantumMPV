/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.ui.preferences

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Settings search history, newest first, stored as a JSON array of strings so that any query text
 * round-trips. Older versions stored the entries joined with `|`; that format is still read, and is
 * replaced by JSON on the next save. A legacy query that itself contained `|` was already split
 * when it was saved and cannot be put back together.
 */
internal object SettingsSearchHistory {
  const val MAX_ENTRIES = 10

  fun parse(raw: String): List<String> {
    if (raw.isEmpty()) return emptyList()
    if (raw.startsWith("[")) {
      runCatching { Json.decodeFromString<List<String>>(raw) }.getOrNull()?.let { return it.filter(String::isNotEmpty) }
    }
    return raw.split("|").filter { it.isNotEmpty() }
  }

  fun add(
    raw: String,
    query: String,
  ): String = encode((listOf(query) + parse(raw).filter { it != query }).take(MAX_ENTRIES))

  fun remove(
    raw: String,
    query: String,
  ): String = encode(parse(raw).filter { it != query })

  private fun encode(entries: List<String>): String = if (entries.isEmpty()) "" else Json.encodeToString(entries)
}
