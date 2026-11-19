package com.quantummpv.app.ui.downloads

import android.os.Build
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DownloadNotificationPermissionTest {
  @Test
  fun asksOnlyOnAndroid13PlusWhenNotGranted() {
    assertTrue(needsNotificationPermission(Build.VERSION_CODES.TIRAMISU, granted = false))
    assertTrue(needsNotificationPermission(Build.VERSION_CODES.UPSIDE_DOWN_CAKE, granted = false))
    assertFalse(needsNotificationPermission(Build.VERSION_CODES.TIRAMISU, granted = true))
    assertFalse(needsNotificationPermission(Build.VERSION_CODES.S_V2, granted = false))
  }
}
