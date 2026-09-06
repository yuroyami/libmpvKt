package io.github.yuroyami.libmpvkt.view

import android.view.Surface
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvProperties
import io.github.yuroyami.libmpvkt.VideoOutput

/** The three moments of a surface's life, as mpv needs them. The one copy both the View and the Compose surface call. */
internal object SurfaceHandshake {
    fun attach(mpv: Mpv, surface: Surface, vo: VideoOutput) { mpv.attachSurface(surface, vo.value) }
    fun resize(mpv: Mpv, width: Int, height: Int) { mpv[MpvProperties.AndroidSurfaceSize] = "${width}x$height" }
    fun detach(mpv: Mpv) { mpv.detachSurface() }
}
