package io.github.yuroyami.libmpvkt.view

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yuroyami.libmpvkt.KeepOpenMode
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvEvent
import io.github.yuroyami.libmpvkt.VideoOutput
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith

/**
 * The view without a window: it still owns the core, plays to the end, takes a second core, and
 * survives being destroyed twice. Drawing is what the sample proves; this is the lifecycle.
 */
@RunWith(AndroidJUnit4::class)
class MpvViewTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun headless() = MpvOptions(
        ao = "null",
        keepOpen = KeepOpenMode.No,
        extra = mapOf("vo" to "null"),
    )

    private fun onMain(block: () -> Unit) =
        InstrumentationRegistry.getInstrumentation().runOnMainSync(block)

    @Test
    fun itPlaysAFileAndTakesASecondCore() = runBlocking {
        val wav = File(context.cacheDir, "view.wav").apply { writeBytes(silentWav(1)) }
        lateinit var view: MpvView
        onMain {
            view = MpvView(context)
            view.initialize(headless())
        }
        val first = assertNotNull(view.mpv)
        onMain { view.playFile(wav.absolutePath) }
        withTimeout(20_000) { first.events.first { it is MpvEvent.EndFile } }

        onMain { view.initialize(headless()) }
        val second = assertNotNull(view.mpv)
        assertNotSame(first, second)

        onMain { view.destroy() }
        onMain { view.destroy() }
    }

    /** A valid 8 kHz mono 16-bit PCM WAV of silence. */
    private fun silentWav(seconds: Int): ByteArray {
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
