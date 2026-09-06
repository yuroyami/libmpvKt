package io.github.yuroyami.libmpvkt.canvas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class FrameRingTest {
    @Test
    fun theRendererNeverGetsTheDisplayedOrPublishedSlot() {
        val ring = FrameRing(3)
        val a = ring.acquire()
        ring.publish(a)
        val shown = ring.takeForDisplay()
        assertEquals(a, shown)
        val b = ring.acquire()
        ring.publish(b)
        val c = ring.acquire()
        assertNotEquals(shown, c)
        assertNotEquals(b, c)
    }

    @Test
    fun publishingOverAnUnconsumedFrameCountsASkip() {
        val ring = FrameRing(3)
        ring.publish(ring.acquire())
        ring.publish(ring.acquire())
        assertEquals(1, ring.skipped)
        assertEquals(2, ring.published)
    }

    @Test
    fun takeForDisplayKeepsTheLastFrameUntilANewOne() {
        val ring = FrameRing(3)
        val a = ring.acquire(); ring.publish(a)
        assertEquals(a, ring.takeForDisplay())
        assertEquals(a, ring.takeForDisplay())
    }

    @Test
    fun resetForgetsEverything() {
        val ring = FrameRing(3)
        ring.publish(ring.acquire())
        ring.reset()
        assertNull(ring.takeForDisplay())
        assertEquals(0, ring.published)
    }
}
