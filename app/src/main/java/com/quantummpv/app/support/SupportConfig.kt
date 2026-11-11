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
  /** This app's page on the developer's website. */
  const val PROJECT_WEBSITE_URL = "https://simplyfahim.pages.dev/quantummpv"

  // Shared by all of the developer's apps: personal contacts and extra payment methods live there.
  const val CONTACT_URL = "https://simplyfahim.pages.dev/contactme"
  const val PAYMENT_OPTIONS_URL = "https://simplyfahim.pages.dev/pay-options"

  const val UPI_ID = "SimplyFahim@sbi"
  const val UPI_PAYEE_NAME = "Md Fahim"
  const val UPI_CURRENCY = "INR"

  /** This app's own public repository. Each app links to its own repository, never a shared one. */
  const val PROJECT_REPOSITORY_URL = "https://github.com/SimplyFahimOfficial/QuantumMPV"

  /** The public issue tracker, where bug reports and feature requests go. */
  const val ISSUES_URL = "$PROJECT_REPOSITORY_URL/issues"

  /** This app's community group, the first place to ask for help. */
  const val TELEGRAM_GROUP_URL = "https://t.me/Quantummpv"

  /** `upi://pay?pa=SimplyFahim@sbi&pn=Md%20Fahim&cu=INR`, with no amount so the payer chooses it. */
  val upiPaymentUri: String =
    UpiPaymentUri.build(
      payeeAddress = UPI_ID,
      payeeName = UPI_PAYEE_NAME,
      currency = UPI_CURRENCY,
    )
}
