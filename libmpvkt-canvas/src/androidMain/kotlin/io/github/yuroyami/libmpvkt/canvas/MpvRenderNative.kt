package io.github.yuroyami.libmpvkt.canvas

import android.hardware.HardwareBuffer
import java.nio.ByteBuffer

internal object MpvRenderNative {
    init { System.loadLibrary("mpvkt_render") }
    external fun create(mpvHandle: Long, readback: Boolean): Long
    external fun resize(handle: Long, width: Int, height: Int)
    external fun slotBuffer(handle: Long, slot: Int): HardwareBuffer?
    external fun waitUpdate(handle: Long, timeoutMs: Int): Int
    external fun render(handle: Long, slot: Int): Boolean
    external fun readPixels(handle: Long, slot: Int, dst: ByteBuffer)
    external fun wake(handle: Long)
    external fun destroy(handle: Long)
    const val UPDATE_FRAME = 1
}

public class MpvRendererException(message: String) : RuntimeException(message)
