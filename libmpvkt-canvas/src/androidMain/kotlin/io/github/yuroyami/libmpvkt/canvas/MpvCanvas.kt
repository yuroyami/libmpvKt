package io.github.yuroyami.libmpvkt.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import io.github.yuroyami.libmpvkt.Mpv
import kotlin.math.roundToInt

/**
 * One renderer for the life of the composition. Start the [mpv] without a window context, as
 * `MpvOptions.forCanvas()` does; the renderer switches `vo` to `libmpv` once its render context exists.
 */
@Composable
public fun rememberMpvRenderer(mpv: Mpv): MpvRenderer {
    val renderer = remember(mpv) { MpvRenderer(mpv) }
    DisposableEffect(renderer) { onDispose { renderer.close() } }
    return renderer
}

/**
 * mpv's output as a plain drawn image. Anything Compose can do to a drawing, it can do to this:
 * blur it, animate it, clip it, put it in a graphics layer, capture it. It costs one extra GPU
 * pass over a `SurfaceView`, and a CPU copy per frame on API 26 to 28; see `docs/compose-canvas.md`
 * for the measured numbers and for when a surface is the better choice. Draw each [renderer] in one
 * `MpvCanvas` only: two would free each other's frames.
 *
 * Frames are rendered at the canvas size, and mpv fits the video inside them. mpv's `keepaspect`,
 * `panscan` and `video-zoom` properties decide that fit.
 */
@Composable
public fun MpvCanvas(
    renderer: MpvRenderer,
    modifier: Modifier = Modifier,
    filterQuality: FilterQuality = FilterQuality.Low,
) {
    // A layer of its own, so each new frame re-records this drawing only, not everything that shares the parent's layer.
    Canvas(modifier.onSizeChanged { renderer.requestSize(it.width, it.height) }.graphicsLayer()) {
        renderer.frameNumber.value // read so this draw runs once per published frame
        val frame = renderer.currentFrame() ?: return@Canvas
        drawImage(frame, dstSize = IntSize(size.width.roundToInt(), size.height.roundToInt()), filterQuality = filterQuality)
    }
}
