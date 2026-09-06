package io.github.yuroyami.libmpvkt.view

import android.view.Surface
import io.github.yuroyami.libmpvkt.InternalLibmpvKtApi
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvProperties
import io.github.yuroyami.libmpvkt.VideoOutput

/**
 * The three moments of a surface's life, as mpv needs them. The one copy both the View and the
 * Compose surface call, which is why it is public and opt-in rather than internal: a second copy
 * of this dance drifts from the first.
 */
@InternalLibmpvKtApi
public object SurfaceHandshake {
    public fun attach(mpv: Mpv, surface: Surface, vo: VideoOutput) { mpv.attachSurface(surface, vo.value) }
    public fun resize(mpv: Mpv, width: Int, height: Int) { mpv[MpvProperties.AndroidSurfaceSize] = "${width}x$height" }
    public fun detach(mpv: Mpv) { mpv.detachSurface() }
}
