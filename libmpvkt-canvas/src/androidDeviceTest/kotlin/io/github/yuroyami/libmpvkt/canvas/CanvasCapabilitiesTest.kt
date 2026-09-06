package io.github.yuroyami.libmpvkt.canvas

import android.os.Build
import android.util.Log
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yuroyami.libmpvkt.HwdecMode
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvCommands
import io.github.yuroyami.libmpvkt.MpvProperties
import io.github.yuroyami.libmpvkt.MpvResult
import io.github.yuroyami.libmpvkt.getOrNull
import io.github.yuroyami.libmpvkt.getOrThrow
import io.github.yuroyami.libmpvkt.view.MpvOptions
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.runner.RunWith

/**
 * What the canvas renderer actually does on this runtime, written to logcat as one line per run so
 * the numbers can be read out of a CI log and put in the docs.
 *
 * These are questions, not gates: a run that answers "no" records the no and still passes. The
 * speed gate that decides whether the canvas stops being experimental is a phone measurement, and
 * it is not this.
 */
@RunWith(AndroidJUnit4::class)
class CanvasCapabilitiesTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun fixture(): File = File(context.cacheDir, "testsrc.mp4").also { f ->
        if (!f.exists()) {
            InstrumentationRegistry.getInstrumentation().context.assets.open("testsrc_320x240_2s.mp4")
                .use { input -> f.outputStream().use { input.copyTo(it) } }
        }
    }

    /** One run: play the fixture for [seconds] and report what mpv chose and how many frames arrived. */
    private fun measure(label: String, hwdec: HwdecMode, vo: String?, seconds: Int = 4) = runBlocking {
        val mpv = Mpv.create(context)
        val options = MpvOptions.forCanvas().copy(
            ao = "null",
            hwdec = hwdec,
            extra = if (vo != null) mapOf("vo" to vo) else emptyMap(),
        )
        options.applyTo(mpv)
        val started = mpv.initialize()
        if (started is MpvResult.Fail) {
            report(label, ok = false, note = "initialize failed: ${started.error}")
            mpv.close()
            return@runBlocking 0L
        }
        var frames = 0L
        try {
            val renderer = MpvRenderer(mpv)
            renderer.requestSize(320, 240)
            mpv.command(MpvCommands.loadFile(fixture().absolutePath)).getOrThrow()
            val until = System.currentTimeMillis() + seconds * 1000L
            while (System.currentTimeMillis() < until) Thread.sleep(100)
            val stats = renderer.stats.value
            frames = stats.framesRendered
            report(
                label,
                ok = frames > 0,
                note = "vo=${mpv[MpvProperties.CurrentVo].getOrNull()} " +
                    "hwdec=${mpv[MpvProperties.HwdecCurrent].getOrNull()} " +
                    "readback=${stats.readback} frames=$frames skipped=${stats.framesSkipped} " +
                    "fps=${"%.1f".format(frames.toDouble() / seconds)} " +
                    "dropped=${mpv[MpvProperties.FrameDropCount].getOrNull()}",
            )
            renderer.close()
        } catch (t: Throwable) {
            report(label, ok = false, note = "threw ${t::class.simpleName}: ${t.message}")
        } finally {
            mpv.close()
        }
        frames
    }

    private fun report(label: String, ok: Boolean, note: String) {
        Log.i(TAG, "CANVAS-MEASUREMENT api=${Build.VERSION.SDK_INT} run=$label ok=$ok $note")
    }

    @Test
    fun theRendererProducesFramesWithSoftwareDecoding() {
        val frames = measure("software", HwdecMode.No, vo = null)
        assertTrue(frames > 0, "the canvas renderer produced no frame at all")
    }

    @Test
    fun hardwareDecodingThroughTheRenderApiIsRecorded() {
        // Expected to reach MediaCodec through mpv's image-reader interop. A no is a recorded no.
        measure("hwdec-auto", HwdecMode.Auto, vo = null)
        measure("hwdec-mediacodec-copy", HwdecMode.MediacodecCopy, vo = null)
    }

    @Test
    fun whetherGpuNextWorksThroughTheRenderApiIsRecorded() {
        // vo=libmpv is the render API; this asks whether libplacebo's newer renderer drives it.
        measure("gpu-next", HwdecMode.No, vo = "gpu-next")
    }

    private companion object {
        const val TAG = "libmpvKt"
    }
}
