package io.github.yuroyami.libmpvkt.view

import android.view.Surface
import io.github.yuroyami.libmpvkt.InternalLibmpvKtApi
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvProperties
import io.github.yuroyami.libmpvkt.VideoOutput

/**
 * The three moments of a surface's life, as mpv needs them. The one copy both the View and the
 * Compose surface call, which is why it is public and opt-in rather than internal: a second copy
 * of this dance drifts from the first. Only the attached surface can resize or detach, so a surface
 * destroyed after another took over leaves the new one alone. A closed core ignores all three.
 */
@InternalLibmpvKtApi
public object SurfaceHandshake {
    public fun attach(mpv: Mpv, surface: Surface, vo: VideoOutput) {
        ifOpen(mpv) { mpv.attachSurface(surface, vo.value) }
    }

    public fun resize(mpv: Mpv, surface: Surface, width: Int, height: Int) {
        ifOpen(mpv) { if (mpv.attachedSurface === surface) mpv.set(MpvProperties.AndroidSurfaceSize, "${width}x$height") }
    }

    public fun detach(mpv: Mpv, surface: Surface) {
        ifOpen(mpv) { if (mpv.attachedSurface === surface) mpv.detachSurface() }
    }

    /** A late surface callback can reach a core that has closed, or is closing; there is nothing left to do. */
    private inline fun ifOpen(mpv: Mpv, block: () -> Unit) {
        if (mpv.isClosed) return
        try {
            block()
        } catch (e: IllegalStateException) {
            if (!mpv.isClosed) throw e
        }
    }
}
