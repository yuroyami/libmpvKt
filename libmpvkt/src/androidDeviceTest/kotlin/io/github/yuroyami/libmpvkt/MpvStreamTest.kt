package io.github.yuroyami.libmpvkt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.github.yuroyami.libmpvkt.stream.MpvStream
import io.github.yuroyami.libmpvkt.stream.MpvStreamProvider
import java.nio.ByteBuffer
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
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

    /** Plays [uri] and answers how it ended. The collector is attached before the command, as events has no replay. */
    private suspend fun endOf(mpv: Mpv, uri: String): MpvEvent.EndFile = coroutineScope {
        val attached = CompletableDeferred<Unit>()
        val ended = async { mpv.events.onSubscription { attached.complete(Unit) }.filterIsInstance<MpvEvent.EndFile>().first() }
        attached.await()
        mpv.command(MpvCommands.loadFile(uri)).getOrThrow()
        withTimeout(15_000) { ended.await() }
    }

    /** A provider whose isSeekable throws, or whose read claims more bytes than it was given, ends the file, not the process. */
    @Test
    fun aMisbehavingStreamEndsTheFileNotTheProcess(): Unit = runBlocking {
        val mpv = Mpv.create(context).apply { setOption("vo", "null"); setOption("ao", "null"); setOption("idle", "yes"); initialize().getOrThrow() }
        val wav = SilentWav.bytes(seconds = 1)
        try {
            mpv.addStreamProtocol("throwing", object : MpvStreamProvider {
                override fun open(uri: String): MpvStream = object : MpvStream by MemoryProvider(wav).open(uri) {
                    override val isSeekable: Boolean get() = throw IllegalStateException("isSeekable failed")
                }
            }).getOrThrow()
            assertEquals(EndFileReason.Error, endOf(mpv, "throwing://a.wav").reason)

            mpv.addStreamProtocol("overlong", object : MpvStreamProvider {
                override fun open(uri: String): MpvStream = object : MpvStream by MemoryProvider(wav).open(uri) {
                    override fun read(into: ByteBuffer): Int = into.remaining() + 1
                }
            }).getOrThrow()
            endOf(mpv, "overlong://a.wav")
        } finally { mpv.close() }
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
