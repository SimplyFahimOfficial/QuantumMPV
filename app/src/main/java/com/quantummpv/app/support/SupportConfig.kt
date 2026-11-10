/*
 * SPDX-License-Identifier: AGPL-3.0-or-later
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.quantummpv.app.support

/**
 * Single source of truth for the Help & Support and Support My Work screens.
 *
 * Update links or payment details here only. The screens, the generated UPI QR code, the share
 * text and the intents all read from this object. Nothing here is sent to a server: the app only
 * opens these links in apps the user picks.
 */
object SupportConfig {
  const val DEVELOPER_NAME = "SimplyFahim"

  /** This app's page on the developer's website. */
  const val PROJECT_WEBSITE_URL = "https://simplyfahim.pages.dev/quantummpv"

  // Shared by all of the developer's apps: personal contacts and extra payment methods live there.
  const val CONTACT_URL = "https://simplyfahim.pages.dev/contactme"
  const val PAYMENT_OPTIONS_URL = "https://simplyfahim.pages.dev/pay-options"

  const val UPI_ID = "SimplyFahim@sbi"
  const val UPI_PAYEE_NAME = "Md Fahim"
  const val UPI_CURRENCY = "INR"

  const val WEBSITE_URL = "https://SimplyFahim.pages.dev"
  const val GITHUB_PROFILE_URL = "https://github.com/SimplyFahimOfficial/"

  /** This app's own public repository. Each app links to its own repository, never a shared one. */
  const val PROJECT_REPOSITORY_URL = "https://github.com/SimplyFahimOfficial/QuantumMPV"
  const val PROJECT_LICENSE = "AGPL-3.0-or-later"

  /** The public issue tracker, where bug reports and feature requests go. */
  const val ISSUES_URL = "$PROJECT_REPOSITORY_URL/issues"

  /** This app's community group, the first place to ask for help. */
  const val TELEGRAM_GROUP_USERNAME = "Quantummpv"

  // Personal contact, shown as smaller tiles under the group card.
  const val INSTAGRAM_HANDLE = "SimplyFahimofficial"
  val CONTACT_EMAILS: List<String> = listOf("SimplyFahim@tuta.io", "simplyfahimofficial@gmail.com")
  const val EMAIL_SUBJECT = "QuantumMPV feedback"

  /** TODO_PERSONAL_TELEGRAM_USERNAME: set your Telegram username here (without @). Blank hides the tile. */
  const val PERSONAL_TELEGRAM_USERNAME = ""

  val telegramGroupUrl: String = SupportLinks.telegram(TELEGRAM_GROUP_USERNAME)
  val instagramUrl: String = SupportLinks.instagram(INSTAGRAM_HANDLE)
  val personalTelegramUrl: String? =
    PERSONAL_TELEGRAM_USERNAME.takeIf { it.isNotBlank() }?.let(SupportLinks::telegram)

  fun emailUri(address: String): String = SupportLinks.mailto(address, EMAIL_SUBJECT)

  /** `upi://pay?pa=SimplyFahim@sbi&pn=Md%20Fahim&cu=INR`, with no amount so the payer chooses it. */
  val upiPaymentUri: String =
    UpiPaymentUri.build(
      payeeAddress = UPI_ID,
      payeeName = UPI_PAYEE_NAME,
      currency = UPI_CURRENCY,
    )

  /** Strips the scheme and trailing slash for display, e.g. `github.com/SimplyFahimOfficial`. */
  fun displayUrl(url: String): String = url.substringAfter("://").trimEnd('/')
}
