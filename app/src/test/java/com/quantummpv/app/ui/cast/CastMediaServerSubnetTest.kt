package com.quantummpv.app.ui.cast

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class CastMediaServerSubnetTest {
  private fun ip(text: String) = InetAddress.getByName(text)

  @Test
  fun hotspotReceiverMatchesTheHotspotInterface() {
    assertTrue(CastMediaServer.sameSubnet(ip("192.168.43.1"), 24, ip("192.168.43.87")))
  }

  @Test
  fun differentSubnetDoesNotMatch() {
    assertFalse(CastMediaServer.sameSubnet(ip("192.168.1.20"), 24, ip("192.168.43.87")))
  }

  @Test
  fun wideAndNarrowPrefixesAreRespected() {
    assertTrue(CastMediaServer.sameSubnet(ip("10.0.0.5"), 8, ip("10.200.3.4")))
    assertFalse(CastMediaServer.sameSubnet(ip("10.0.0.5"), 30, ip("10.0.0.9")))
    assertTrue(CastMediaServer.sameSubnet(ip("10.0.0.5"), 30, ip("10.0.0.6")))
  }

  @Test
  fun invalidPrefixOrIpv6NeverMatches() {
    assertFalse(CastMediaServer.sameSubnet(ip("192.168.1.2"), 0, ip("192.168.1.3")))
    assertFalse(CastMediaServer.sameSubnet(ip("192.168.1.2"), 33, ip("192.168.1.3")))
    assertFalse(CastMediaServer.sameSubnet(ip("fe80::1"), 64, ip("fe80::2")))
  }
}
