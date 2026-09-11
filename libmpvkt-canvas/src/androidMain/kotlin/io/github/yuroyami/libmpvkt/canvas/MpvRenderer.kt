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
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference

public data class MpvRendererStats(
    val framesRendered: Long = 0, val framesSkipped: Long = 0, val width: Int = 0, val height: Int = 0,
    /** True on API 26 to 28, where every frame is copied to a bitmap on the CPU. */
    val readback: Boolean = false,
    /** Why the renderer stopped, such as a buffer it could not allocate; null while it runs. */
    val failure: String? = null,
)

/**
 * Renders an [Mpv] into bitmaps Compose can draw. One per core; [close] before, or let, `mpv.close()`
 * run it. Create it after `mpv.initialize()`; a closed core is refused.
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

    /** The native renderer. Only the render thread sets it, under [handleLock], so a [wake] never meets a freed one. */
    @Volatile private var handle = 0L
    private val handleLock = Any()
    private var width = 0
    private var height = 0
    private var readbackBuffer: ByteBuffer? = null
    private val started = CountDownLatch(1)
    @Volatile private var startError: Throwable? = null
    @Volatile private var beforeClose: DisposableHandle? = null

    // Started in init, after every field above is set: the render thread uses them at once.
    private val thread: Thread

    init {
        check(!mpv.isClosed) { "this Mpv is closed" }
        thread = Thread({ loop() }, "mpvkt-render")
        thread.start()
        started.await()
        startError?.let { throw it }
        // The core starts with vo=null; libmpv can open only now that the render context exists.
        mpv.setString("vo", "libmpv")
    }

    /** Asks for frames of this size. The current frame is drawn again at the new size, also while paused. */
    public fun requestSize(width: Int, height: Int) {
        requested.set(width to height)
        wake()
    }

    /**
     * The newest published frame; marks it as the one on screen so the renderer draws elsewhere. Each
     * call counts as one UI frame of the ring, so call it from one [MpvCanvas] only.
     */
    public fun currentFrame(): ImageBitmap? {
        val slot = ring.takeForDisplay() ?: return null
        return bitmaps[slot]
    }

    /** Stops the render thread and frees the render context. Every call returns only once that is done. */
    override fun close() {
        if (closed.compareAndSet(false, true)) {
            // Unhook from the core, so it stops holding a closed renderer and its buffers.
            beforeClose?.dispose()
            // Freeing the context under vo=libmpv makes mpv drop the video track, so the output goes first,
            // while the render thread still runs to serve mpv. A core that has closed throws; nothing is lost then.
            runCatching { mpv.setString("vo", "null") }
            running.set(false)
            wake()
        }
        // A second caller waits too: Mpv.close() must not destroy the core before the context is gone.
        if (Thread.currentThread() !== thread) thread.join()
    }

    private fun wake() {
        synchronized(handleLock) { if (handle != 0L) MpvRenderNative.wake(handle) }
    }

    private fun loop() {
        try {
            // Inside the core's call gate, so the core cannot be destroyed while the context is made.
            mpv.withNativeHandle { core ->
                val created = MpvRenderNative.create(core, readback)
                synchronized(handleLock) { handle = created }
                try {
                    beforeClose = mpv.onBeforeClose { close() }
                } catch (e: IllegalStateException) {
                    // The core began closing meanwhile: free the context while the core is still alive.
                    synchronized(handleLock) { handle = 0L }
                    MpvRenderNative.destroy(created)
                    throw e
                }
            }
        } catch (t: Throwable) {
            startError = t
            started.countDown()
            return
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
        } catch (e: Exception) {
            // A failed step, such as a buffer allocation on resize, stops this renderer rather than the app.
            statsFlow.value = statsFlow.value.copy(failure = e.message ?: e::class.java.simpleName)
        } finally {
            val native = handle
            synchronized(handleLock) { handle = 0L }
            MpvRenderNative.destroy(native)
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
