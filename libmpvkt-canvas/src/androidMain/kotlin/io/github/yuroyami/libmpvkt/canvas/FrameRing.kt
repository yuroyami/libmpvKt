package io.github.yuroyami.libmpvkt.canvas

/**
 * Three slots between the render thread and Compose. The renderer draws into a slot nobody is
 * showing, publishes it, and Compose takes the newest published slot when it draws. A published
 * slot that Compose never took is replaced and counted as skipped.
 */
internal class FrameRing(private val size: Int) {
    private var displayed = -1
    private var pending = -1
    var published: Long = 0; private set
    var skipped: Long = 0; private set

    @Synchronized
    fun acquire(): Int {
        for (slot in 0 until size) if (slot != displayed && slot != pending) return slot
        error("no free slot with size $size")
    }

    @Synchronized
    fun publish(slot: Int) {
        if (pending != -1) skipped++
        pending = slot
        published++
    }

    /** The slot to draw now, or null before the first frame. */
    @Synchronized
    fun takeForDisplay(): Int? {
        if (pending != -1) { displayed = pending; pending = -1 }
        return displayed.takeIf { it != -1 }
    }

    @Synchronized
    fun reset() { displayed = -1; pending = -1; published = 0; skipped = 0 }
}
