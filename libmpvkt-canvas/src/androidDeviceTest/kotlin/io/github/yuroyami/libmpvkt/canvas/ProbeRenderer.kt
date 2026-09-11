package io.github.yuroyami.libmpvkt.canvas

import io.github.yuroyami.libmpvkt.InternalLibmpvKtApi
import io.github.yuroyami.libmpvkt.Mpv
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * The renderer loop with the bitmap step replaced by a pixel read, so a test can look at what mpv
 * actually drew. The production renderer never reads back on API 29 and above, and the test must
 * not depend on the device's API level.
 */
@OptIn(InternalLibmpvKtApi::class)
internal class ProbeRenderer(mpv: Mpv, private val width: Int, private val height: Int) : AutoCloseable {

    /** The colour at the middle of the newest frame, as ARGB, or null before the first one. */
    val centrePixel: StateFlow<Int?> field = MutableStateFlow(null)

    /** The colours a quarter and three quarters of the way down the newest frame, as ARGB. */
    val topAndBottom: StateFlow<Pair<Int, Int>?> field = MutableStateFlow(null)

    private val running = AtomicBoolean(true)
    private val started = CountDownLatch(1)

    /** Set only on the render thread, under [handleLock], so [close]'s wake never meets a freed renderer. */
    @Volatile private var handle = 0L
    private val handleLock = Any()
    private var startError: Throwable? = null
    private val mpvHandle = mpv.nativeHandle
    private val buffer: ByteBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())

    private val thread = Thread({ loop() }, "mpvkt-probe").apply { start() }

    init {
        started.await()
        startError?.let { throw it }
        // As MpvRenderer does: the core starts with vo=null, and libmpv can open only once a render context exists.
        mpv.setString("vo", "libmpv")
    }

    private fun loop() {
        try {
            val created = MpvRenderNative.create(mpvHandle, readback = true)
            synchronized(handleLock) { handle = created }
        } catch (t: Throwable) {
            startError = t
            started.countDown()
            return
        }
        started.countDown()
        try {
            MpvRenderNative.resize(handle, width, height)
            while (running.get()) {
                val flags = MpvRenderNative.waitUpdate(handle, 100)
                if (flags and MpvRenderNative.UPDATE_FRAME == 0) continue
                if (!MpvRenderNative.render(handle, 0)) continue
                buffer.rewind()
                MpvRenderNative.readPixels(handle, 0, buffer)
                centrePixel.value = pixelAt(width / 2, height / 2)
                topAndBottom.value = pixelAt(width / 2, height / 4) to pixelAt(width / 2, height * 3 / 4)
            }
        } finally {
            val native = handle
            synchronized(handleLock) { handle = 0L }
            MpvRenderNative.destroy(native)
        }
    }

    /**
     * The pixel at column [x], row [y], packed as ARGB from the RGBA the read gives, with the alpha mpv
     * wrote. Row 0 of the read is the row Android shows at the top, so [y] counts from the top.
     */
    private fun pixelAt(x: Int, y: Int): Int {
        val offset = (y * width + x) * 4
        fun byteAt(i: Int) = buffer.get(offset + i).toInt() and 0xff
        return (byteAt(3) shl 24) or (byteAt(0) shl 16) or (byteAt(1) shl 8) or byteAt(2)
    }

    override fun close() {
        running.set(false)
        synchronized(handleLock) { if (handle != 0L) MpvRenderNative.wake(handle) }
        thread.join()
    }
}
