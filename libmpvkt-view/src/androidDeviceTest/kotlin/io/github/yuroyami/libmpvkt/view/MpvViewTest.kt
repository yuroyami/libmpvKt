package io.github.yuroyami.libmpvkt.view

import android.graphics.SurfaceTexture
import android.view.Surface
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yuroyami.libmpvkt.InternalLibmpvKtApi
import io.github.yuroyami.libmpvkt.KeepOpenMode
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvCommands
import io.github.yuroyami.libmpvkt.MpvEvent
import io.github.yuroyami.libmpvkt.MpvProperties
import io.github.yuroyami.libmpvkt.VideoOutput
import io.github.yuroyami.libmpvkt.getOrNull
import io.github.yuroyami.libmpvkt.getOrThrow
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
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
    fun itPlaysAFileAndTakesASecondCore(): Unit = runBlocking {
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

    /** A file started before any surface keeps its video, and gets a picture once a surface comes. */
    @OptIn(InternalLibmpvKtApi::class)
    @Test
    fun aFileStartedBeforeItsSurfaceKeepsItsVideo(): Unit = runBlocking {
        val video = TestVideo.writeTo(context.cacheDir)
        val options = MpvOptions(ao = "null")
        val mpv = Mpv.create(context)
        options.applyTo(mpv)
        mpv.initialize().getOrThrow()
        val texture = SurfaceTexture(0).apply { setDefaultBufferSize(320, 240) }
        val surface = Surface(texture)
        try {
            val attached = CompletableDeferred<Unit>()
            val loaded = async { mpv.events.onSubscription { attached.complete(Unit) }.first { it == MpvEvent.FileLoaded } }
            attached.await()
            mpv.command(MpvCommands.loadFile(video.absolutePath)).getOrThrow()
            withTimeout(20_000) { loaded.await() }
            assertNotNull(mpv[MpvProperties.CurrentTrackVideo].getOrNull(), "mpv dropped the video track before the surface came")

            SurfaceHandshake.attach(mpv, surface, options.vo)
            SurfaceHandshake.resize(mpv, surface, 320, 240)
            withTimeout(20_000) { while (mpv.getString("current-vo") != "gpu") delay(50) }
            assertNotNull(mpv[MpvProperties.CurrentTrackVideo].getOrNull(), "the video track is gone after the surface came")
        } finally {
            SurfaceHandshake.detach(mpv, surface)
            mpv.close()
            surface.release()
            texture.release()
        }
    }

    /** Two surfaces on one core: the old one's late destroy leaves the new one alone, and a closed core ignores the handshake. */
    @OptIn(InternalLibmpvKtApi::class)
    @Test
    fun aDestroyedSurfaceLeavesTheNewOneAlone() {
        val mpv = Mpv.create(context)
        MpvOptions(ao = "null").applyTo(mpv)
        mpv.initialize().getOrThrow()
        val textures = List(2) { SurfaceTexture(0).apply { setDefaultBufferSize(64, 64) } }
        val (leaving, arriving) = textures.map { Surface(it) }
        try {
            SurfaceHandshake.attach(mpv, leaving, VideoOutput.Gpu)
            SurfaceHandshake.attach(mpv, arriving, VideoOutput.Gpu)
            SurfaceHandshake.detach(mpv, leaving)
            assertSame(arriving, mpv.attachedSurface)
            assertEquals("gpu", mpv.getString("vo"))

            mpv.close()
            SurfaceHandshake.resize(mpv, arriving, 32, 32)
            SurfaceHandshake.detach(mpv, arriving)
        } finally {
            mpv.close()
            leaving.release()
            arriving.release()
            textures.forEach { it.release() }
        }
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
