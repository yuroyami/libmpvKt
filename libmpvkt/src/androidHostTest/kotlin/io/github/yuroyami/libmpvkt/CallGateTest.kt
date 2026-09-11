package io.github.yuroyami.libmpvkt

import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The gate that keeps a handle alive while calls are inside native code. */
class CallGateTest {

    @Test
    fun closeWaitsForTheCallsInside() {
        val gate = CallGate()
        val inside = CountDownLatch(1)
        val leave = CountDownLatch(1)
        val caller = thread { gate.call { inside.countDown(); leave.await() } }
        assertTrue(inside.await(5, TimeUnit.SECONDS))

        val closed = CountDownLatch(1)
        thread { gate.close(); closed.countDown() }
        assertFalse(closed.await(200, TimeUnit.MILLISECONDS), "close returned while a call was still inside")
        leave.countDown()
        assertTrue(closed.await(5, TimeUnit.SECONDS), "close did not return after the call left")
        caller.join()
    }

    @Test
    fun callsAfterCloseFailAtOnce() {
        val gate = CallGate()
        assertEquals(2, gate.call { gate.call { 2 } }, "a call inside a call is fine")
        assertTrue(gate.close())
        assertFalse(gate.close(), "only the first close reports true")
        assertTrue(gate.isClosed)
        assertFailsWith<IllegalStateException> { gate.call { 1 } }
        var ran = false
        gate.callIfOpen { ran = true }
        assertFalse(ran)
    }

    /** The gate counts calls; it is not a lock, so calls from many threads run at the same time. */
    @Test
    fun callsRunInParallel() {
        val gate = CallGate()
        val both = CountDownLatch(2)
        val sawTheOther = AtomicInteger()
        List(2) { thread { gate.call { both.countDown(); if (both.await(5, TimeUnit.SECONDS)) sawTheOther.incrementAndGet() } } }
            .forEach { it.join() }
        assertEquals(2, sawTheOther.get())
    }
}
