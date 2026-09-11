package io.github.yuroyami.libmpvkt.view

import io.github.yuroyami.libmpvkt.InternalLibmpvKtApi
import io.github.yuroyami.libmpvkt.Mpv
import kotlin.concurrent.thread

/**
 * Closes the core on a thread of its own. mpv_terminate_destroy waits for mpv to stop, which can take
 * long when a stream is stuck, and the View and Compose wrappers close from the main thread.
 */
@InternalLibmpvKtApi
public fun Mpv.closeInBackground() {
    if (isClosed) return
    thread(name = "mpv-close-$clientName", isDaemon = true) { close() }
}
