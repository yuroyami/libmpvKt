package io.github.yuroyami.libmpvkt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yuroyami.libmpvkt.stream.MpvStream
import io.github.yuroyami.libmpvkt.stream.MpvStreamProvider
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MpvStreamTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A WAV held in memory, served through the Kotlin stream interface. */
    private class MemoryProvider(private val bytes: ByteArray) : MpvStreamProvider {
        var opened = 0
        override fun open(uri: String): MpvStream {
            opened++
            return object : MpvStream {
                var pos = 0
                override val isSeekable = true
                override fun read(into: ByteBuffer): Int {
                    val n = minOf(into.remaining(), bytes.size - pos)
                    if (n <= 0) return 0
                    into.put(bytes, pos, n); pos += n; return n
                }
                override fun seek(offset: Long): Long { pos = offset.toInt().coerceIn(0, bytes.size); return pos.toLong() }
                override fun size(): Long = bytes.size.toLong()
                override fun close() {}
            }
        }
    }

    @Test
    fun aKotlinStreamPlaysToTheEnd(): Unit = runBlocking {
        val mpv = Mpv.create(context).apply { setOption("vo", "null"); setOption("ao", "null"); setOption("idle", "yes"); initialize().getOrThrow() }
        val provider = MemoryProvider(SilentWav.bytes(seconds = 1))
        try {
            mpv.addStreamProtocol("memtest", provider).getOrThrow()
            val ended = withTimeout(15_000) {
                mpv.command(MpvCommands.loadFile("memtest://silence.wav")).getOrThrow()
                mpv.events.filterIsInstance<MpvEvent.EndFile>().first()
            }
            assertEquals(EndFileReason.Eof, ended.reason)
            assertEquals(1, provider.opened)
        } finally { mpv.close() }
    }
}
