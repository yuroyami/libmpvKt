package io.github.yuroyami.libmpvkt.canvas

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

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

    /** HWUI draws the frame that just left the screen after the UI thread has moved on, so that slot waits one more UI frame. */
    @Test
    fun theSlotThatLeftTheScreenIsNotHandedOutAtOnce() {
        val ring = FrameRing(4)
        val a = ring.acquire(); ring.publish(a)
        assertEquals(a, ring.takeForDisplay())
        val b = ring.acquire(); ring.publish(b)
        assertEquals(b, ring.takeForDisplay())
        val c = ring.acquire()
        assertTrue(c != a && c != b, "the renderer got slot $c; a=$a had just left the screen and b=$b is on it")
        ring.publish(c)
        val d = ring.acquire()
        assertTrue(d != a && d != b && d != c, "the renderer got slot $d; a=$a, b=$b and c=$c are all still in use")
    }

    @Test
    fun aRedrawWithoutANewFrameFreesTheOldSlot() {
        val ring = FrameRing(4)
        val a = ring.acquire(); ring.publish(a); ring.takeForDisplay()
        val b = ring.acquire(); ring.publish(b); ring.takeForDisplay()
        ring.takeForDisplay()
        assertEquals(a, ring.acquire(), "a has been off the screen for a whole UI frame")
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
