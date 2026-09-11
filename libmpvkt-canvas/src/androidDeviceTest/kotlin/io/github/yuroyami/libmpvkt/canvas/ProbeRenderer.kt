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

    private val running = AtomicBoolean(true)
    private val started = CountDownLatch(1)
    private var handle = 0L
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
            handle = MpvRenderNative.create(mpvHandle, readback = true)
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
                centrePixel.value = pixelAtCentre()
            }
        } finally {
            MpvRenderNative.destroy(handle)
            handle = 0L
        }
    }

    /** The middle pixel, packed as ARGB from the RGBA the read gives, with the alpha mpv wrote. */
    private fun pixelAtCentre(): Int {
        val offset = ((height / 2) * width + width / 2) * 4
        fun byteAt(i: Int) = buffer.get(offset + i).toInt() and 0xff
        return (byteAt(3) shl 24) or (byteAt(0) shl 16) or (byteAt(1) shl 8) or byteAt(2)
    }

    override fun close() {
        running.set(false)
        if (handle != 0L) MpvRenderNative.wake(handle)
        thread.join()
    }
}
