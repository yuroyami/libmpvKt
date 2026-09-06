package io.github.yuroyami.libmpvkt.view

import io.github.yuroyami.libmpvkt.HwdecMode
import io.github.yuroyami.libmpvkt.VideoOutput
import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals

class MpvOptionsTest {
    private class Recorder : MpvOptions.OptionSink {
        val set = linkedMapOf<String, String>()
        override fun option(name: String, value: String) { set[name] = value }
    }

    @Test
    fun theDefaultsSendOnlyWhatDiffersFromMpv() {
        val sink = Recorder()
        MpvOptions().applyTo(sink)
        assertEquals(
            mapOf(
                "config" to "no", "vo" to "gpu", "gpu-context" to "android", "opengl-es" to "yes", "hwdec" to "auto",
                "hwdec-codecs" to "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1", "ao" to "audiotrack,opensles", "profile" to "fast",
                "video-sync" to "audio", "demuxer-max-bytes" to "67108864", "demuxer-max-back-bytes" to "67108864",
                "sub-font-provider" to "none", "input-default-bindings" to "no", "keep-open" to "yes", "force-window" to "no", "idle" to "yes",
            ),
            sink.set,
        )
    }

    @Test
    fun everyFieldMapsToItsOption() {
        val sink = Recorder()
        MpvOptions(
            configDir = File("/c"), cacheDir = File("/k"), vo = VideoOutput.GpuNext, hwdec = HwdecMode.No, tlsCaFile = File("/ca.pem"),
            audioLanguages = listOf("ja", "en"), subtitleLanguages = listOf("en"), deband = true, interpolation = true, displayFps = 120.0,
            extra = mapOf("vo" to "null"),
        ).applyTo(sink)
        assertEquals("yes", sink.set["config"])
        assertEquals("/c", sink.set["config-dir"])
        assertEquals("/k", sink.set["gpu-shader-cache-dir"])
        assertEquals("/k", sink.set["icc-cache-dir"])
        assertEquals("no", sink.set["hwdec"])
        assertEquals("yes", sink.set["tls-verify"])
        assertEquals("/ca.pem", sink.set["tls-ca-file"])
        assertEquals("ja,en", sink.set["alang"])
        assertEquals("en", sink.set["slang"])
        assertEquals("yes", sink.set["deband"])
        assertEquals("yes", sink.set["interpolation"])
        assertEquals("120.0", sink.set["display-fps-override"])
        assertEquals("null", sink.set["vo"], "extra wins over the field")
    }

    @Test
    fun theCanvasNeedsNoWindowContext() {
        val sink = Recorder()
        MpvOptions.forCanvas().applyTo(sink)
        assertEquals("libmpv", sink.set["vo"])
        assertEquals(null, sink.set["gpu-context"])
        assertEquals(null, sink.set["opengl-es"])
    }
}
