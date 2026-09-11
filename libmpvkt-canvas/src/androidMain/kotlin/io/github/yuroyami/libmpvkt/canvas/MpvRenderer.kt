package io.github.yuroyami.libmpvkt.canvas

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.os.Build
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import io.github.yuroyami.libmpvkt.InternalLibmpvKtApi
import io.github.yuroyami.libmpvkt.Mpv
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public data class MpvRendererStats(
    val framesRendered: Long = 0, val framesSkipped: Long = 0, val width: Int = 0, val height: Int = 0,
    /** True on API 26 to 28, where every frame is copied to a bitmap on the CPU. */
    val readback: Boolean = false,
)

/**
 * Renders an [Mpv] started with `vo=libmpv` into bitmaps Compose can draw. One per core; [close]
 * before, or let, `mpv.close()` run it. Create it after `mpv.initialize()`.
 */
@OptIn(InternalLibmpvKtApi::class)
public class MpvRenderer(public val mpv: Mpv) : AutoCloseable {
    private val readback = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q
    private val ring = FrameRing(SLOTS)
    private val frameNumberState = mutableLongStateOf(0L)
    public val frameNumber: State<Long> get() = frameNumberState
    private val statsFlow = MutableStateFlow(MpvRendererStats(readback = readback))
    public val stats: StateFlow<MpvRendererStats> get() = statsFlow
    private val requested = AtomicReference<Pair<Int, Int>?>(null)
    private val running = AtomicBoolean(true)
    private val closed = AtomicBoolean(false)
    @Volatile private var bitmaps: Array<ImageBitmap?> = arrayOfNulls(SLOTS)
    @Volatile private var androidBitmaps: Array<Bitmap?> = arrayOfNulls(SLOTS)
    private var handle = 0L
    private var width = 0
    private var height = 0
    private var readbackBuffer: ByteBuffer? = null
    private val started = java.util.concurrent.CountDownLatch(1)
    private var startError: Throwable? = null

    private val thread = Thread({ loop() }, "mpvkt-render").apply { start() }
    private var beforeClose: DisposableHandle? = null

    init {
        started.await()
        startError?.let { throw it }
        beforeClose = mpv.onBeforeClose { close() }
        // The core starts with vo=null; libmpv can open only now that the render context exists.
        mpv.setString("vo", "libmpv")
    }

    /** Asks for frames of this size. The current frame is drawn again at the new size, also while paused. */
    public fun requestSize(width: Int, height: Int) {
        requested.set(width to height)
        if (handle != 0L) MpvRenderNative.wake(handle)
    }

    /**
     * The newest published frame; marks it as the one on screen so the renderer draws elsewhere. Each
     * call counts as one UI frame of the ring, so call it from one [MpvCanvas] only.
     */
    public fun currentFrame(): ImageBitmap? {
        val slot = ring.takeForDisplay() ?: return null
        return bitmaps[slot]
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        // Unhook from the core, so it stops holding a closed renderer and its buffers.
        beforeClose?.dispose()
        running.set(false)
        if (handle != 0L) MpvRenderNative.wake(handle)
        thread.join()
    }

    private fun loop() {
        try {
            handle = MpvRenderNative.create(mpv.nativeHandle, readback)
        } catch (t: Throwable) {
            startError = t; started.countDown(); return
        }
        started.countDown()
        try {
            while (running.get()) {
                val flags = MpvRenderNative.waitUpdate(handle, 100)
                // mpv does not know the target changed, so after a resize the current frame is drawn again unasked.
                val resized = applyRequestedSize()
                val newFrame = flags and MpvRenderNative.UPDATE_FRAME != 0
                if (width == 0) {
                    // render.h wants a render call for every new frame; with nowhere to draw yet, skip it, or mpv stalls.
                    if (newFrame) MpvRenderNative.skip(handle)
                    continue
                }
                if (!newFrame && !resized) continue
                val slot = ring.acquire()
                if (!MpvRenderNative.render(handle, slot)) continue
                if (readback) copyToBitmap(slot)
                ring.publish(slot)
                frameNumberState.longValue = ring.published
                statsFlow.value = MpvRendererStats(ring.published, ring.skipped, width, height, readback)
            }
        } finally {
            MpvRenderNative.destroy(handle)
            handle = 0L
        }
    }

    /** Applies the size the canvas asked for. True when the slots changed and there is something to draw into. */
    private fun applyRequestedSize(): Boolean {
        val (w, h) = requested.getAndSet(null) ?: return false
        if (w == width && h == height) return false
        MpvRenderNative.resize(handle, w, h)
        width = w; height = h
        ring.reset()
        val next = arrayOfNulls<ImageBitmap>(SLOTS)
        val nextAndroid = arrayOfNulls<Bitmap>(SLOTS)
        if (w > 0 && h > 0) {
            for (slot in 0 until SLOTS) {
                val bitmap = if (readback) {
                    Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                } else {
                    val buffer = checkNotNull(MpvRenderNative.slotBuffer(handle, slot)) { "slot $slot has no buffer" }
                    // The bitmap takes its own reference; this one would otherwise wait for the garbage collector.
                    buffer.use { checkNotNull(Bitmap.wrapHardwareBuffer(it, ColorSpace.get(ColorSpace.Named.SRGB))) { "wrapHardwareBuffer" } }
                }
                nextAndroid[slot] = bitmap
                next[slot] = bitmap.asImageBitmap()
            }
            if (readback) readbackBuffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
        }
        androidBitmaps = nextAndroid
        bitmaps = next
        statsFlow.value = statsFlow.value.copy(width = w, height = h)
        return w > 0 && h > 0
    }

    private fun copyToBitmap(slot: Int) {
        val buffer = readbackBuffer ?: return
        buffer.rewind()
        MpvRenderNative.readPixels(handle, slot, buffer)
        buffer.rewind()
        androidBitmaps[slot]?.copyPixelsFromBuffer(buffer)
    }

    private companion object { const val SLOTS = 4 }
}
