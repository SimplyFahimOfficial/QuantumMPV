package com.quantummpv.app.ui.player.controls.components

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class KeepControlsAliveWhileScrollingTest {
  private suspend fun waitUntil(condition: () -> Boolean) = withTimeout(5_000) { while (!condition()) delay(5) }

  @Test
  fun restingRowIsNotActivity() =
    runBlocking {
      val scrolling = MutableStateFlow(false)
      val activity = AtomicInteger()
      val job = launch(Dispatchers.Default) { keepControlsAliveWhileScrolling(scrolling, 10) { activity.incrementAndGet() } }

      delay(80)
      job.cancel()

      assertEquals(0, activity.get())
    }

  @Test
  fun scrollingKeepsReportingUntilItStops() =
    runBlocking {
      val scrolling = MutableStateFlow(false)
      val activity = AtomicInteger()
      // A StateFlow hands a late collector its current value first; the helper discards that one as "resting",
      // so scrolling must not start before the collector has seen false.
      val restingSeen = CompletableDeferred<Unit>()
      val job =
        launch(Dispatchers.Default) {
          keepControlsAliveWhileScrolling(scrolling.onEach { restingSeen.complete(Unit) }, 10) { activity.incrementAndGet() }
        }
      withTimeout(5_000) { restingSeen.await() }

      scrolling.value = true
      waitUntil { activity.get() >= 4 }
      val whileScrolling = activity.get()

      scrolling.value = false
      // Stopping counts as activity too, so the hide delay starts again from the end of the scroll.
      waitUntil { activity.get() > whileScrolling }
      delay(40)
      val settled = activity.get()
      delay(120)
      job.cancel()

      assertEquals("no reports after the scroll ended", settled, activity.get())
    }
}
