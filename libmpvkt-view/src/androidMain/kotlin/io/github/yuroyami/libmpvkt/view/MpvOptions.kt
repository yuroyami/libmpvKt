package io.github.yuroyami.libmpvkt.view

import io.github.yuroyami.libmpvkt.HwdecMode
import io.github.yuroyami.libmpvkt.KeepOpenMode
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.SubFontProvider
import io.github.yuroyami.libmpvkt.VideoOutput
import io.github.yuroyami.libmpvkt.VideoSyncMode
import java.io.File

/**
 * How a core starts: one field per mpv option, with the value a phone player wants as the default.
 * A field left at its default is not sent, so an `mpv.conf` in [configDir] still decides it.
 */
public data class MpvOptions(
    /** `config-dir`. When set, mpv reads `mpv.conf`, `input.conf` and `subfont.ttf` there and starts with `config=yes`. */
    val configDir: File? = null,
    /** `gpu-shader-cache-dir` and `icc-cache-dir`. Compiled shaders survive restarts; first play is faster with it. */
    val cacheDir: File? = null,
    val surfaceType: SurfaceType = SurfaceType.Surface,
    /** `vo`. `Gpu` is the OpenGL ES renderer; `GpuNext` is libplacebo's newer path with better tone mapping and scaling. */
    val vo: VideoOutput = VideoOutput.Gpu,
    /** `hwdec`. `Auto` tries MediaCodec first; `No` is software; `MediacodecCopy` is hardware decode with a copy, for filters that need system memory. */
    val hwdec: HwdecMode = HwdecMode.Auto,
    /** `hwdec-codecs`. The codecs hardware decoding is allowed for. */
    val hwdecCodecs: String = "h264,hevc,mpeg4,mpeg2video,vp8,vp9,av1",
    /** `ao`. AudioTrack first, OpenSL ES as the fallback. */
    val ao: String = "audiotrack,opensles",
    /** `profile`. `fast` turns off the expensive scalers; `high-quality` is for a TV box, not a phone. */
    val profile: String? = "fast",
    /** `video-sync`. `Audio` is the phone default; `DisplayResample` needs a stable refresh rate and `displayFps`. */
    val videoSync: VideoSyncMode = VideoSyncMode.Audio,
    /** `display-fps-override`. The panel's refresh rate, for the `display-*` sync modes. `MpvView` fills it from the display when null. */
    val displayFps: Double? = null,
    /** `interpolation`. Motion interpolation; only meaningful with a `display-*` sync mode. */
    val interpolation: Boolean = false,
    /** `tls-verify` and `tls-ca-file`. Without a bundle every https URL fails: Mbed TLS cannot see Android's trust store. */
    val tlsCaFile: File? = null,
    /** `alang`. Preferred audio languages, first match wins. */
    val audioLanguages: List<String> = emptyList(),
    /** `slang`. Preferred subtitle languages. */
    val subtitleLanguages: List<String> = emptyList(),
    /** `demuxer-max-bytes` and `demuxer-max-back-bytes`. mpv's defaults are sized for a desktop. */
    val demuxerMaxBytes: Long = 64L * 1024 * 1024,
    /** `deband`. Cheap on the GPU and hides banding in dark gradients. */
    val deband: Boolean = false,
    /** `sub-font-provider`. This libass has no system provider; `None` plus a `subfont.ttf` in [configDir] is the working setup. */
    val subFontProvider: SubFontProvider = SubFontProvider.None,
    /** `input-default-bindings`. Off: an app draws its own controls. */
    val inputDefaultBindings: Boolean = false,
    /** `keep-open`. `Yes` holds the last frame at the end instead of going idle, which is what a player screen wants. */
    val keepOpen: KeepOpenMode = KeepOpenMode.Yes,
    /** Any other option, applied last, so an entry here wins over the fields above. */
    val extra: Map<String, String> = emptyMap(),
) {

    /** Where options go. [Mpv] is one; the host test records through another. */
    public fun interface OptionSink {
        public fun option(name: String, value: String)
    }

    public fun applyTo(mpv: Mpv) {
        applyTo { name, value -> mpv.setOption(name, value) }
    }

    public fun applyTo(sink: OptionSink) {
        val dir = configDir
        if (dir != null) { sink.option("config", "yes"); sink.option("config-dir", dir.absolutePath) } else sink.option("config", "no")
        cacheDir?.let { sink.option("gpu-shader-cache-dir", it.absolutePath); sink.option("icc-cache-dir", it.absolutePath) }
        sink.option("vo", vo.value)
        sink.option("gpu-context", "android")
        sink.option("opengl-es", "yes")
        sink.option("hwdec", hwdec.value)
        sink.option("hwdec-codecs", hwdecCodecs)
        sink.option("ao", ao)
        profile?.let { sink.option("profile", it) }
        sink.option("video-sync", videoSync.mpvName)
        displayFps?.let { sink.option("display-fps-override", it.toString()) }
        if (interpolation) sink.option("interpolation", "yes")
        tlsCaFile?.let { sink.option("tls-verify", "yes"); sink.option("tls-ca-file", it.absolutePath) }
        if (audioLanguages.isNotEmpty()) sink.option("alang", audioLanguages.joinToString(","))
        if (subtitleLanguages.isNotEmpty()) sink.option("slang", subtitleLanguages.joinToString(","))
        sink.option("demuxer-max-bytes", demuxerMaxBytes.toString())
        sink.option("demuxer-max-back-bytes", demuxerMaxBytes.toString())
        if (deband) sink.option("deband", "yes")
        sink.option("sub-font-provider", subFontProvider.mpvName)
        sink.option("input-default-bindings", if (inputDefaultBindings) "yes" else "no")
        sink.option("keep-open", keepOpen.mpvName)
        // No window until a surface exists, or mpv aborts; and stay alive between files.
        sink.option("force-window", "no")
        sink.option("idle", "yes")
        for ((name, value) in extra) sink.option(name, value)
    }
}
