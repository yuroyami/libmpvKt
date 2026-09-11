package io.github.yuroyami.libmpvkt.canvas

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import androidx.test.platform.app.InstrumentationRegistry
import io.github.yuroyami.libmpvkt.HwdecMode
import io.github.yuroyami.libmpvkt.KeepOpenMode
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvCommands
import io.github.yuroyami.libmpvkt.MpvEvent
import io.github.yuroyami.libmpvkt.MpvProperties
import io.github.yuroyami.libmpvkt.getOrThrow
import io.github.yuroyami.libmpvkt.view.MpvOptions
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.fail
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MpvRendererTest {
    @Test
    fun rendersFramesOfTheTestPattern(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val asset = TestVideo.writeTo(context.cacheDir)
        val mpv = Mpv.create(context)
        // keep-open defaults to Yes, which holds the last frame and never ends the file; this test
        // waits for the end, so it asks for the other behaviour.
        MpvOptions.forCanvas().copy(ao = "null", hwdec = HwdecMode.No, keepOpen = KeepOpenMode.No).applyTo(mpv)
        mpv.initialize().getOrThrow()
        val renderer = MpvRenderer(mpv)
        renderer.requestSize(320, 240)
        mpv.command(MpvCommands.loadFile(asset.absolutePath)).getOrThrow()
        withTimeoutOrNull(30_000) { mpv.events.first { it is MpvEvent.EndFile } }
            ?: fail("the file never ended")
        val stats = renderer.stats.value
        assertTrue(stats.framesRendered >= 30, "rendered ${stats.framesRendered}")
        renderer.close()
        mpv.close()
    }

    @Test
    fun aPublishedFrameIsNotBlack(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mpv = Mpv.create(context)
        MpvOptions.forCanvas().copy(ao = "null", hwdec = HwdecMode.No, keepOpen = KeepOpenMode.Yes).applyTo(mpv)
        mpv.initialize().getOrThrow()
        val probe = ProbeRenderer(mpv, 64, 48)
        mpv.command(MpvCommands.loadFile(TestVideo.writeTo(context.cacheDir).absolutePath)).getOrThrow()
        val pixel = withTimeoutOrNull(30_000) { probe.centrePixel.first { it != null } }
            ?: fail("no frame reached the probe")
        // The colour bytes, not the whole pixel: an opaque black frame is 0xff000000, which is not 0.
        assertTrue(pixel and 0xffffff != 0, "centre pixel is black: ${"%08x".format(pixel)}")
        probe.close(); mpv.close()
    }

    /** Android reads the slots from the top row down, so the top of the picture has to land at the top. */
    @Test
    fun theTopOfThePictureIsAtTheTop(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val picture = File(context.cacheDir, "red-over-blue.png").apply {
            val bitmap = Bitmap.createBitmap(64, 48, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).apply {
                drawRect(0f, 0f, 64f, 24f, Paint().apply { color = Color.RED })
                drawRect(0f, 24f, 64f, 48f, Paint().apply { color = Color.BLUE })
            }
            outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
        }
        val mpv = Mpv.create(context)
        MpvOptions.forCanvas().copy(ao = "null", hwdec = HwdecMode.No, keepOpen = KeepOpenMode.Yes).applyTo(mpv)
        mpv.initialize().getOrThrow()
        val probe = ProbeRenderer(mpv, 64, 48)
        try {
            mpv.command(MpvCommands.loadFile(picture.absolutePath)).getOrThrow()
            val (top, bottom) = withTimeoutOrNull(30_000) { probe.topAndBottom.first { it != null } }
                ?: fail("no frame reached the probe")
            assertTrue(isRed(top), "the top quarter is ${"%08x".format(top)}, not red")
            assertTrue(isBlue(bottom), "the bottom quarter is ${"%08x".format(bottom)}, not blue")
        } finally {
            probe.close()
            mpv.close()
        }
    }

    /** A resize while paused draws the current frame again at the new size, although mpv sends no new frame. */
    @Test
    fun aResizeWhilePausedRendersTheCurrentFrame(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val mpv = Mpv.create(context)
        MpvOptions.forCanvas().copy(ao = "null", hwdec = HwdecMode.No, keepOpen = KeepOpenMode.Yes).applyTo(mpv)
        mpv.initialize().getOrThrow()
        val renderer = MpvRenderer(mpv)
        try {
            renderer.requestSize(320, 240)
            mpv[MpvProperties.Pause] = true
            mpv.command(MpvCommands.loadFile(TestVideo.writeTo(context.cacheDir).absolutePath)).getOrThrow()
            withTimeoutOrNull(30_000) { renderer.stats.first { it.framesRendered > 0 } } ?: fail("no first frame")
            renderer.requestSize(160, 120)
            // The stats show the new width once the slots are replaced; after that only a new render gives a frame.
            withTimeoutOrNull(10_000) { renderer.stats.first { it.width == 160 } } ?: fail("the resize never applied")
            val frame = withTimeoutOrNull(10_000) {
                var f = renderer.currentFrame()
                while (f == null) { delay(20); f = renderer.currentFrame() }
                f
            } ?: fail("the resize drew nothing while paused")
            assertEquals(160, frame.width)
            assertEquals(120, frame.height)
        } finally {
            renderer.close()
            mpv.close()
        }
    }

    private fun isRed(argb: Int) = (argb shr 16 and 0xff) > 200 && (argb and 0xff) < 60
    private fun isBlue(argb: Int) = (argb and 0xff) > 200 && (argb shr 16 and 0xff) < 60
}
