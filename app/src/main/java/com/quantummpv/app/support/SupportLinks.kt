/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.support

import java.net.URLEncoder
import java.nio.charset.StandardCharsets

/**
 * Builds the contact links shown on the Support My Work screen. Plain JVM code so it can be unit
 * tested; each builder rejects input that would produce a broken or misleading link.
 */
object SupportLinks {
  /** Telegram public usernames: 5 to 32 characters, letters, digits and `_`, starting with a letter. */
  private val TELEGRAM_USERNAME = Regex("^[A-Za-z][A-Za-z0-9_]{4,31}$")

  /** Instagram handles: up to 30 letters, digits, `.` and `_`. */
  private val INSTAGRAM_HANDLE = Regex("^[A-Za-z0-9._]{1,30}$")

  /** One address, no URI delimiters, so it cannot smuggle extra mailto fields. */
  private val EMAIL_ADDRESS = Regex("^[A-Za-z0-9._%+-]+@[A-Za-z0-9-]+(\\.[A-Za-z0-9-]+)*\\.[A-Za-z]{2,}$")

  fun telegram(username: String): String {
    require(TELEGRAM_USERNAME.matches(username)) { "Invalid Telegram username: $username" }
    return "https://t.me/$username"
  }

  fun instagram(handle: String): String {
    require(INSTAGRAM_HANDLE.matches(handle)) { "Invalid Instagram handle: $handle" }
    return "https://instagram.com/$handle"
  }

  /** `mailto:` link per RFC 6068, with the subject percent-encoded (spaces as `%20`, not `+`). */
  fun mailto(
    address: String,
    subject: String,
  ): String {
    require(EMAIL_ADDRESS.matches(address)) { "Invalid email address: $address" }
    val encodedSubject = URLEncoder.encode(subject, StandardCharsets.UTF_8.name()).replace("+", "%20")
    return "mailto:$address?subject=$encodedSubject"
  }
}
