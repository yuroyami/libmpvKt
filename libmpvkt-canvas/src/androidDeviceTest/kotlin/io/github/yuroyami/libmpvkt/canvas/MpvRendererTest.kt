package io.github.yuroyami.libmpvkt.canvas

import androidx.test.platform.app.InstrumentationRegistry
import io.github.yuroyami.libmpvkt.HwdecMode
import io.github.yuroyami.libmpvkt.KeepOpenMode
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.MpvCommands
import io.github.yuroyami.libmpvkt.MpvEvent
import io.github.yuroyami.libmpvkt.getOrThrow
import io.github.yuroyami.libmpvkt.view.MpvOptions
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertTrue

class MpvRendererTest {
    @Test
    fun rendersFramesOfTheTestPattern(): Unit = runBlocking {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val asset = File(context.cacheDir, "testsrc.mp4").also { f ->
            InstrumentationRegistry.getInstrumentation().context.assets.open("testsrc_320x240_2s.mp4").use { i -> f.outputStream().use { i.copyTo(it) } }
        }
        val mpv = Mpv.create(context)
        MpvOptions.forCanvas().copy(ao = "null", hwdec = HwdecMode.No).applyTo(mpv)
        mpv.initialize().getOrThrow()
        val renderer = MpvRenderer(mpv)
        renderer.requestSize(320, 240)
        mpv.command(MpvCommands.loadFile(asset.absolutePath)).getOrThrow()
        withTimeout(15_000) { mpv.events.first { it is MpvEvent.EndFile } }
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
        mpv.command(MpvCommands.loadFile(File(context.cacheDir, "testsrc.mp4").absolutePath)).getOrThrow()
        val pixel = withTimeout(15_000) { probe.centrePixel.first { it != null } }!!
        assertTrue(pixel != 0, "centre pixel is black")
        probe.close(); mpv.close()
    }
}
