package com.quantummpv.app.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportConfigTest {
  @Test
  fun `official support details are exact`() {
    assertEquals("SimplyFahim@sbi", SupportConfig.UPI_ID)
    assertEquals("Md Fahim", SupportConfig.UPI_PAYEE_NAME)
    assertEquals("INR", SupportConfig.UPI_CURRENCY)
  }

  @Test
  fun `help and support links are exact`() {
    assertEquals("https://t.me/Quantummpv", SupportConfig.TELEGRAM_GROUP_URL)
    assertEquals("https://simplyfahim.pages.dev/quantummpv", SupportConfig.PROJECT_WEBSITE_URL)
    assertEquals("https://github.com/SimplyFahimOfficial/QuantumMPV/issues", SupportConfig.ISSUES_URL)
    assertEquals("https://simplyfahim.pages.dev/contactme", SupportConfig.CONTACT_URL)
    assertEquals("https://simplyfahim.pages.dev/pay-options", SupportConfig.PAYMENT_OPTIONS_URL)
  }

  @Test
  fun `repository link points at this app's own repository`() {
    assertEquals("https://github.com/SimplyFahimOfficial/QuantumMPV", SupportConfig.PROJECT_REPOSITORY_URL)
    assertFalse(SupportConfig.PROJECT_REPOSITORY_URL == SupportConfig.PROJECT_WEBSITE_URL)
  }

  @Test
  fun `upi uri has payee, name and currency but no amount`() {
    assertEquals("upi://pay?pa=SimplyFahim@sbi&pn=Md%20Fahim&cu=INR", SupportConfig.upiPaymentUri)
    val params = UpiPaymentUri.parse(SupportConfig.upiPaymentUri)!!
    assertEquals(mapOf("pa" to "SimplyFahim@sbi", "pn" to "Md Fahim", "cu" to "INR"), params)
    assertNull(params["am"])
  }

  @Test
  fun `builder percent-encodes names with spaces and rejects invalid ids`() {
    assertEquals(
      "upi://pay?pa=a.b@okaxis&pn=A%20B%26C&cu=INR",
      UpiPaymentUri.build("a.b@okaxis", "A B&C"),
    )
    assertTrue(UpiPaymentUri.isValidVpa("SimplyFahim@sbi"))
    assertFalse(UpiPaymentUri.isValidVpa("no-at-sign"))
    assertFalse(UpiPaymentUri.isValidVpa("x@"))
  }

  @Test
  fun `old donation details and retired routes are gone`() {
    val all =
      listOf(
        SupportConfig.UPI_ID,
        SupportConfig.upiPaymentUri,
        SupportConfig.PROJECT_WEBSITE_URL,
        SupportConfig.CONTACT_URL,
        SupportConfig.PAYMENT_OPTIONS_URL,
      ).joinToString()
    listOf("adityanarvekar", "aadinarvekar", "aadiinarvekar", "paypal", "@Sbi", "vercel.app", "ko-fi", "/pay/").forEach {
      assertFalse("found $it", all.contains(it))
    }
  }
}
