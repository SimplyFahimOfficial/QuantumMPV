package com.quantummpv.app.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class SupportLinksTest {
  @Test
  fun `telegram builds a t_me link and rejects bad usernames`() {
    assertEquals("https://t.me/Quantummpv", SupportLinks.telegram("Quantummpv"))
    listOf("", "abc", "@Quantummpv", "1abcde", "bad name", "a/b/c/d", "x".repeat(33)).forEach {
      assertThrows(IllegalArgumentException::class.java) { SupportLinks.telegram(it) }
    }
  }

  @Test
  fun `instagram builds a profile link and rejects bad handles`() {
    assertEquals("https://instagram.com/SimplyFahimofficial", SupportLinks.instagram("SimplyFahimofficial"))
    assertEquals("https://instagram.com/a.b_c", SupportLinks.instagram("a.b_c"))
    listOf("", "@handle", "a/b", "has space", "x".repeat(31)).forEach {
      assertThrows(IllegalArgumentException::class.java) { SupportLinks.instagram(it) }
    }
  }

  @Test
  fun `mailto encodes the subject with percent-20 and rejects injected fields`() {
    assertEquals(
      "mailto:SimplyFahim@astermail.com?subject=QuantumMPV%20feedback",
      SupportLinks.mailto("SimplyFahim@astermail.com", "QuantumMPV feedback"),
    )
    assertEquals("mailto:a@b.io?subject=A%26B%3F%20%C3%A9", SupportLinks.mailto("a@b.io", "A&B? é"))
    listOf("", "no-at.com", "a@b", "a@b.com?cc=x@y.com", "a@b.com,c@d.com", "a b@c.com").forEach {
      assertThrows(IllegalArgumentException::class.java) { SupportLinks.mailto(it, "s") }
    }
  }

  @Test
  fun `configured contact links are exact`() {
    assertEquals("https://t.me/Quantummpv", SupportConfig.telegramGroupUrl)
    assertEquals("https://instagram.com/SimplyFahimofficial", SupportConfig.instagramUrl)
    assertEquals(listOf("SimplyFahim@astermail.com"), SupportConfig.CONTACT_EMAILS)
    assertEquals(
      "mailto:SimplyFahim@astermail.com?subject=QuantumMPV%20feedback",
      SupportConfig.emailUri(SupportConfig.CONTACT_EMAILS.single()),
    )
    // Personal Telegram is optional: blank hides the tile, anything else must be a valid username.
    assertEquals(
      SupportConfig.PERSONAL_TELEGRAM_USERNAME.takeIf { it.isNotBlank() }?.let { "https://t.me/$it" },
      SupportConfig.personalTelegramUrl,
    )
  }
}
