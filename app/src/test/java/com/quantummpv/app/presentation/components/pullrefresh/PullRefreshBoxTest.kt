package com.quantummpv.app.presentation.components.pullrefresh

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger

class PullRefreshBoxTest {
  private val executor = Executors.newSingleThreadExecutor()
  private val scope = CoroutineScope(Job() + executor.asCoroutineDispatcher())

  @After
  fun tearDown() {
    scope.cancel()
    executor.shutdownNow()
  }

  @Test
  fun pullFollowsFingerUpToThresholdThenResists() {
    assertEquals(0f, rubberBandedPull(0f, 200f), 0f)
    assertEquals(100f, rubberBandedPull(0.5f, 200f), 0.001f)
    assertEquals(200f, rubberBandedPull(1f, 200f), 0.001f)
    assertTrue(rubberBandedPull(1.5f, 200f) in 200f..300f)
  }

  @Test
  fun pullNeverReachesTwiceTheThresholdHoweverFarTheDragGoes() {
    var previous = 0f
    for (fraction in listOf(1f, 1.1f, 2f, 5f, 50f, 1_000f, 1e6f)) {
      val pull = rubberBandedPull(fraction, 200f)
      assertTrue("fraction $fraction -> $pull", pull >= previous && pull < 400f)
      previous = pull
    }
  }

  @Test
  fun invalidInputProducesNoTranslation() {
    assertEquals(0f, rubberBandedPull(-1f, 200f), 0f)
    assertEquals(0f, rubberBandedPull(Float.NaN, 200f), 0f)
    assertEquals(0f, rubberBandedPull(Float.POSITIVE_INFINITY, 200f), 0f)
    assertEquals(0f, rubberBandedPull(1f, 0f), 0f)
  }

  @Test
  fun secondReleaseWhileRefreshingIsIgnoredAndTheFirstStillCompletes() =
    runBlocking {
      val flight = SingleFlight()
      val gate = CompletableDeferred<Unit>()
      val ran = AtomicInteger()
      val ended = AtomicInteger()
      assertTrue(flight.launch(scope, onStart = {}, onEnd = { ended.incrementAndGet() }) { ran.incrementAndGet(); gate.await() })
      assertFalse(flight.launch(scope, onStart = { error("must not start") }, onEnd = {}) { ran.incrementAndGet() })
      gate.complete(Unit)
      awaitTrue { ended.get() == 1 }
      assertEquals(1, ran.get())
    }

  @Test
  fun aFinishedRefreshAllowsTheNextGesture() =
    runBlocking {
      val flight = SingleFlight()
      val ended = AtomicInteger()
      repeat(3) { round ->
        assertTrue(flight.launch(scope, onStart = {}, onEnd = { ended.incrementAndGet() }) {})
        awaitTrue { ended.get() == round + 1 }
      }
    }

  @Test
  fun cancelledBeforeTheBodyStartsStillResetsState() =
    runBlocking {
      val flight = SingleFlight()
      val block = CountDownLatch(1)
      executor.execute { block.await() } // keeps the dispatcher busy so the body cannot start yet
      val bodyRan = AtomicInteger()
      val refreshing = AtomicBoolean()
      flight.launch(scope, onStart = { refreshing.set(true) }, onEnd = { refreshing.set(false) }) { bodyRan.incrementAndGet() }
      assertTrue(refreshing.get())
      scope.coroutineContext[Job]!!.cancel() // screen leaves composition right after the release
      block.countDown()
      awaitTrue { !refreshing.get() }
      assertEquals(0, bodyRan.get())
    }

  @Test
  fun aCancelledRefreshEndsOnceAndDoesNotClearTheNextOne() =
    runBlocking {
      val flight = SingleFlight()
      val firstEnded = AtomicInteger()
      val firstJob = CoroutineScope(Job() + executor.asCoroutineDispatcher())
      val gate = CompletableDeferred<Unit>()
      flight.launch(firstJob, onStart = {}, onEnd = { firstEnded.incrementAndGet() }) { gate.await() }
      firstJob.cancel()
      awaitTrue { firstEnded.get() == 1 }

      val refreshing = AtomicBoolean()
      assertTrue(flight.launch(scope, onStart = { refreshing.set(true) }, onEnd = { refreshing.set(false) }) { gate.await() })
      Thread.sleep(50)
      assertTrue("the first job's completion must not reset the second refresh", refreshing.get())
      assertEquals(1, firstEnded.get())
      gate.complete(Unit)
      awaitTrue { !refreshing.get() }
    }

  private suspend fun awaitTrue(condition: () -> Boolean) =
    withTimeout(5_000) { while (!condition()) kotlinx.coroutines.delay(5) }
}
