package io.github.yuroyami.libmpvkt

import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock

/**
 * Counts the calls inside native code, so a close can wait for them before the handle is freed.
 * A counter, not a lock: calls from many threads run at once, and calls after [close] fail at once.
 */
internal class CallGate {
    private val lock = ReentrantLock()
    private val empty = lock.newCondition()
    private var inside = 0

    @Volatile
    var isClosed: Boolean = false
        private set

    /** Enters the gate, or returns false once it is closed. Every true needs one [leave]. */
    fun enter(): Boolean = lock.withLock {
        if (isClosed) return false
        inside++
        true
    }

    fun leave() {
        lock.withLock { if (--inside == 0) empty.signalAll() }
    }

    /** Closes the gate and waits until every call inside has left. True for the first caller only. */
    fun close(): Boolean = lock.withLock {
        if (isClosed) return false
        isClosed = true
        while (inside > 0) empty.awaitUninterruptibly()
        true
    }

    /** Runs [block] inside the gate. Throws [IllegalStateException] once the gate is closed. */
    inline fun <R> call(block: () -> R): R {
        check(enter()) { "this Mpv is closed" }
        try {
            return block()
        } finally {
            leave()
        }
    }

    /** Runs [block] inside the gate, or skips it once the gate is closed. */
    inline fun callIfOpen(block: () -> Unit) {
        if (!enter()) return
        try {
            block()
        } finally {
            leave()
        }
    }
}
