package io.github.yuroyami.libmpvkt.compose

import androidx.compose.foundation.AndroidEmbeddedExternalSurface
import androidx.compose.foundation.AndroidExternalSurface
import androidx.compose.foundation.AndroidExternalSurfaceZOrder
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.ui.Modifier
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.VideoOutput
import io.github.yuroyami.libmpvkt.view.SurfaceHandshake
import io.github.yuroyami.libmpvkt.view.SurfaceType
import io.github.yuroyami.libmpvkt.InternalLibmpvKtApi

@OptIn(InternalLibmpvKtApi::class)
/**
 * mpv's output in Compose, with no View subclass in between: Compose Foundation's own external
 * surface composables carry the handshake. [surfaceType] is the same choice as [SurfaceType]
 * documents: [SurfaceType.Surface] is `AndroidExternalSurface` (a SurfaceView underneath, cheapest,
 * cannot be transformed or sampled), [SurfaceType.Texture] is `AndroidEmbeddedExternalSurface`
 * (a TextureView underneath, a real node, one copy per frame). [zOrder] and [isOpaque] apply to
 * the first only. A different [mpv] gets a new surface.
 */
@Composable
public fun MpvSurface(
    mpv: Mpv,
    modifier: Modifier = Modifier,
    surfaceType: SurfaceType = SurfaceType.Surface,
    vo: VideoOutput = VideoOutput.Gpu,
    isOpaque: Boolean = true,
    zOrder: AndroidExternalSurfaceZOrder = AndroidExternalSurfaceZOrder.Behind,
) {
    // Each surface's callbacks keep the core they were made for, so a new core needs a new surface.
    key(mpv) {
        when (surfaceType) {
            SurfaceType.Surface -> AndroidExternalSurface(modifier = modifier, isOpaque = isOpaque, zOrder = zOrder) {
                onSurface { surface, width, height ->
                    SurfaceHandshake.attach(mpv, surface, vo)
                    SurfaceHandshake.resize(mpv, surface, width, height)
                    surface.onChanged { w, h -> SurfaceHandshake.resize(mpv, surface, w, h) }
                    surface.onDestroyed { SurfaceHandshake.detach(mpv, surface) }
                }
            }
            SurfaceType.Texture -> AndroidEmbeddedExternalSurface(modifier = modifier, isOpaque = isOpaque) {
                onSurface { surface, width, height ->
                    SurfaceHandshake.attach(mpv, surface, vo)
                    SurfaceHandshake.resize(mpv, surface, width, height)
                    surface.onChanged { w, h -> SurfaceHandshake.resize(mpv, surface, w, h) }
                    surface.onDestroyed { SurfaceHandshake.detach(mpv, surface) }
                }
            }
        }
    }
}
