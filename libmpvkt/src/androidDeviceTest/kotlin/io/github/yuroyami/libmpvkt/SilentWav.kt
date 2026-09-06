package io.github.yuroyami.libmpvkt

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** A valid 8 kHz mono 16-bit PCM WAV of silence: a 44 byte header and zeroed samples. */
internal object SilentWav {
    fun bytes(seconds: Int): ByteArray {
        val sampleRate = 8000
        val dataSize = sampleRate * 2 * seconds
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        buf.put("data".toByteArray()).putInt(dataSize)
        return buf.array()
    }
}
