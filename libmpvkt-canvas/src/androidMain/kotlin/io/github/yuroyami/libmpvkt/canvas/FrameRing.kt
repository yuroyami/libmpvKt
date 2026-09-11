package io.github.yuroyami.libmpvkt.canvas

/**
 * Four slots between the render thread and Compose. The renderer draws into a free slot, publishes
 * it, and Compose takes the newest published slot when it draws. A published slot that Compose never
 * took is replaced and counted as skipped. The slot that just left the screen stays out of reach for
 * one more UI frame, because HWUI's render thread can still be drawing it.
 */
internal class FrameRing(private val size: Int) {
    private var displayed = -1
    private var previous = -1
    private var pending = -1
    var published: Long = 0; private set
    var skipped: Long = 0; private set

    @Synchronized
    fun acquire(): Int {
        for (slot in 0 until size) if (slot != displayed && slot != previous && slot != pending) return slot
        error("no free slot with size $size")
    }

    @Synchronized
    fun publish(slot: Int) {
        if (pending != -1) skipped++
        pending = slot
        published++
    }

    /** The slot to draw now, or null before the first frame. Each call is one UI frame. */
    @Synchronized
    fun takeForDisplay(): Int? {
        if (pending != -1) {
            previous = displayed
            displayed = pending
            pending = -1
        } else {
            previous = -1
        }
        return displayed.takeIf { it != -1 }
    }

    @Synchronized
    fun reset() { displayed = -1; previous = -1; pending = -1; published = 0; skipped = 0 }
}
