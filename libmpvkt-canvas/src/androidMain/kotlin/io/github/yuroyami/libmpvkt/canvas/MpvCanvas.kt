package io.github.yuroyami.libmpvkt.canvas

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntOffset
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
 */
@Composable
public fun MpvCanvas(
    renderer: MpvRenderer,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    filterQuality: FilterQuality = FilterQuality.Low,
) {
    Canvas(modifier.onSizeChanged { renderer.requestSize(it.width, it.height) }) {
        renderer.frameNumber.value // read so this draw runs once per published frame
        val frame = renderer.currentFrame() ?: return@Canvas
        val src = Size(frame.width.toFloat(), frame.height.toFloat())
        val scale = contentScale.computeScaleFactor(src, size)
        val dst = IntSize((src.width * scale.scaleX).roundToInt(), (src.height * scale.scaleY).roundToInt())
        val offset = IntOffset(((size.width - dst.width) / 2f).roundToInt(), ((size.height - dst.height) / 2f).roundToInt())
        drawImage(frame, dstOffset = offset, dstSize = dst, filterQuality = filterQuality)
    }
}
