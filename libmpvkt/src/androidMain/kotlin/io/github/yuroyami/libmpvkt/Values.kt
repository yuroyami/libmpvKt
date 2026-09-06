package io.github.yuroyami.libmpvkt

internal fun mismatch(expected: String, node: MpvNode): Nothing =
    throw IllegalArgumentException("expected $expected, mpv answered ${node::class.simpleName}($node)")

/** mpv's string lists: an array of strings, or one comma-separated string. */
public object StringListCodec : MpvCodec<List<String>> {
    override fun encode(value: List<String>): MpvNode = MpvNode.Arr(value.map { MpvNode.Str(it) })
    override fun decode(node: MpvNode): List<String> = when (node) {
        is MpvNode.Arr -> node.values.map { it.asString() ?: mismatch("a list of strings", node) }
        is MpvNode.Str -> if (node.value.isEmpty()) emptyList() else node.value.split(',')
        MpvNode.None -> emptyList()
        else -> mismatch("a list of strings", node)
    }
}

/** mpv's key-value lists: a map of strings, or one `k=v,k=v` string. */
public object StringMapCodec : MpvCodec<Map<String, String>> {
    override fun encode(value: Map<String, String>): MpvNode = MpvNode.Dict(value.mapValues { MpvNode.Str(it.value) })
    override fun decode(node: MpvNode): Map<String, String> = when (node) {
        is MpvNode.Dict -> node.values.mapValues { (k, v) -> v.asString() ?: mismatch("string values in $k", node) }
        is MpvNode.Str -> if (node.value.isEmpty()) emptyMap() else node.value.split(',').associate { it.substringBefore('=') to it.substringAfter('=', "") }
        MpvNode.None -> emptyMap()
        else -> mismatch("a map of strings", node)
    }
}

/** `aid`, `vid`, `sid`, `secondary-sid`: a track id, `no`, or `auto`. */
public sealed interface TrackSelection {
    public data class Id(val id: Int) : TrackSelection
    public data object No : TrackSelection
    public data object Auto : TrackSelection

    public object Codec : MpvCodec<TrackSelection> {
        override fun encode(value: TrackSelection): MpvNode = when (value) {
            is Id -> MpvNode.Int64(value.id.toLong())
            No -> MpvNode.Str("no")
            Auto -> MpvNode.Str("auto")
        }
        override fun decode(node: MpvNode): TrackSelection = when {
            node is MpvNode.Int64 -> Id(node.value.toInt())
            node is MpvNode.Flag && !node.value -> No
            node.asString() == "no" -> No
            node.asString() == "auto" -> Auto
            node.asString()?.toIntOrNull() != null -> Id(node.asString()!!.toInt())
            else -> mismatch("a track id, no, or auto", node)
        }
    }
}

/** `loop-file`, `loop-playlist`, `ab-loop-count`: `no`, `inf`, or a count. */
public sealed interface LoopCount {
    public data object No : LoopCount
    public data object Inf : LoopCount
    public data class Times(val count: Int) : LoopCount

    public object Codec : MpvCodec<LoopCount> {
        override fun encode(value: LoopCount): MpvNode = when (value) {
            No -> MpvNode.Str("no")
            Inf -> MpvNode.Str("inf")
            is Times -> MpvNode.Int64(value.count.toLong())
        }
        override fun decode(node: MpvNode): LoopCount = when {
            node is MpvNode.Int64 -> Times(node.value.toInt())
            node is MpvNode.Flag && !node.value -> No
            node.asString() == "no" -> No
            node.asString() == "inf" -> Inf
            node.asString()?.toIntOrNull() != null -> Times(node.asString()!!.toInt())
            else -> mismatch("no, inf, or a count", node)
        }
    }
}

/** `track-list` entries. Unknown keys are ignored; missing keys are null or false. */
public data class MpvTrack(
    val id: Int, val type: TrackType, val srcId: Int?, val title: String?, val lang: String?,
    val isImage: Boolean, val isAlbumArt: Boolean, val isDefault: Boolean, val isForced: Boolean, val isDependent: Boolean,
    val isVisualImpaired: Boolean, val isHearingImpaired: Boolean, val isExternal: Boolean, val externalFilename: String?,
    val selected: Boolean, val mainSelection: Int?, val codec: String?, val codecDesc: String?, val codecProfile: String?,
    val ffIndex: Int?, val decoder: String?, val decoderDesc: String?, val demuxWidth: Int?, val demuxHeight: Int?,
    val demuxChannelCount: Int?, val demuxChannels: String?, val demuxSamplerate: Int?, val demuxFps: Double?,
    val demuxBitrate: Long?, val demuxRotation: Int?, val demuxPar: Double?, val formatName: String?,
    val hlsBitrate: Long?, val programId: Int?, val metadata: Map<String, String>,
) {
    public object Codec : MpvCodec<MpvTrack> {
        override fun encode(value: MpvTrack): MpvNode = throw UnsupportedOperationException("track-list is read-only")
        override fun decode(node: MpvNode): MpvTrack {
            val m = node.asMap() ?: mismatch("a track map", node)
            fun s(k: String) = m[k]?.asString()
            fun i(k: String) = m[k]?.asLong()?.toInt()
            fun l(k: String) = m[k]?.asLong()
            fun d(k: String) = m[k]?.asDouble()
            fun b(k: String) = m[k]?.asBoolean() ?: false
            return MpvTrack(
                id = i("id") ?: mismatch("a track with an id", node),
                type = TrackType.entries.firstOrNull { it.mpvName == s("type") } ?: mismatch("a track type", node),
                srcId = i("src-id"), title = s("title"), lang = s("lang"),
                isImage = b("image"), isAlbumArt = b("albumart"), isDefault = b("default"), isForced = b("forced"), isDependent = b("dependent"),
                isVisualImpaired = b("visual-impaired"), isHearingImpaired = b("hearing-impaired"), isExternal = b("external"), externalFilename = s("external-filename"),
                selected = b("selected"), mainSelection = i("main-selection"), codec = s("codec"), codecDesc = s("codec-desc"), codecProfile = s("codec-profile"),
                ffIndex = i("ff-index"), decoder = s("decoder"), decoderDesc = s("decoder-desc"), demuxWidth = i("demux-w"), demuxHeight = i("demux-h"),
                demuxChannelCount = i("demux-channel-count"), demuxChannels = s("demux-channels"), demuxSamplerate = i("demux-samplerate"), demuxFps = d("demux-fps"),
                demuxBitrate = l("demux-bitrate"), demuxRotation = i("demux-rotation"), demuxPar = d("demux-par"), formatName = s("format-name"),
                hlsBitrate = l("hls-bitrate"), programId = i("program-id"),
                metadata = m["metadata"]?.let { StringMapCodec.decode(it) }.orEmpty(),
            )
        }
    }

    public object ListCodec : MpvCodec<List<MpvTrack>> {
        override fun encode(value: List<MpvTrack>): MpvNode = throw UnsupportedOperationException("track-list is read-only")
        override fun decode(node: MpvNode): List<MpvTrack> = (node.asList() ?: mismatch("a track list", node)).map(Codec::decode)
    }
}

/** Which edition of a file to play. */
public sealed interface EditionSelection {
    public data class Index(val index: Int) : EditionSelection
    public data object Auto : EditionSelection

    public object Codec : MpvCodec<EditionSelection> {
        override fun encode(value: EditionSelection): MpvNode = when (value) {
            is Index -> MpvNode.Int64(value.index.toLong())
            Auto -> MpvNode.Str("auto")
        }

        override fun decode(node: MpvNode): EditionSelection = when {
            node.asString() == "auto" -> Auto
            node.asLong() != null -> Index(node.asLong()!!.toInt())
            else -> mismatch("an edition index or auto", node)
        }
    }
}

/** One end of the A-B loop: a position, or off. */
public sealed interface AbLoopPoint {
    public data object No : AbLoopPoint
    public data class At(val seconds: Double) : AbLoopPoint

    public object Codec : MpvCodec<AbLoopPoint> {
        override fun encode(value: AbLoopPoint): MpvNode = when (value) {
            No -> MpvNode.Str("no")
            is At -> MpvNode.Dbl(value.seconds)
        }

        override fun decode(node: MpvNode): AbLoopPoint = when {
            node.asString() == "no" -> No
            node.asDouble() != null -> At(node.asDouble()!!)
            else -> mismatch("a loop point or no", node)
        }
    }
}

/**
 * What `video-aspect-override` holds: the file's own ratio, aspect handling switched off, a
 * number, or a written ratio such as `16:9`.
 */
public sealed interface AspectOverride {
    public data object Original : AspectOverride
    public data object Disabled : AspectOverride
    public data class Ratio(val value: Double) : AspectOverride
    public data class Named(val text: String) : AspectOverride

    public object Codec : MpvCodec<AspectOverride> {
        override fun encode(value: AspectOverride): MpvNode = when (value) {
            Original -> MpvNode.Dbl(-1.0)
            Disabled -> MpvNode.Dbl(0.0)
            is Ratio -> MpvNode.Dbl(value.value)
            is Named -> MpvNode.Str(value.text)
        }

        override fun decode(node: MpvNode): AspectOverride {
            node.asDouble()?.let { d ->
                return when {
                    d < 0.0 -> Original
                    d == 0.0 -> Disabled
                    else -> Ratio(d)
                }
            }
            return Named(node.asString() ?: mismatch("an aspect override", node))
        }
    }
}

/** `video-rotate`: a clockwise angle, or no rotation at all. */
public sealed interface VideoRotation {
    public data class Degrees(val value: Int) : VideoRotation
    public data object No : VideoRotation

    public object Codec : MpvCodec<VideoRotation> {
        override fun encode(value: VideoRotation): MpvNode = when (value) {
            is Degrees -> MpvNode.Int64(value.value.toLong())
            No -> MpvNode.Int64(-1)
        }

        override fun decode(node: MpvNode): VideoRotation {
            val v = node.asLong() ?: mismatch("a rotation", node)
            return if (v < 0) No else Degrees(v.toInt())
        }
    }
}

/**
 * The `hwdec` value. Any string mpv accepts is allowed, so a new decoder needs no library
 * release; the constants are the ones that matter on Android.
 */
@JvmInline
public value class HwdecMode(public val value: String) {
    public companion object {
        public val No: HwdecMode = HwdecMode("no")
        public val Auto: HwdecMode = HwdecMode("auto")
        public val AutoSafe: HwdecMode = HwdecMode("auto-safe")
        public val AutoCopy: HwdecMode = HwdecMode("auto-copy")
        public val Mediacodec: HwdecMode = HwdecMode("mediacodec")
        public val MediacodecCopy: HwdecMode = HwdecMode("mediacodec-copy")
    }

    public object Codec : MpvCodec<HwdecMode> {
        override fun encode(value: HwdecMode): MpvNode = MpvNode.Str(value.value)
        override fun decode(node: MpvNode): HwdecMode = HwdecMode(node.asString() ?: mismatch("a hwdec name", node))
    }
}

/** The `vo` value. Open for the same reason as [HwdecMode]. */
@JvmInline
public value class VideoOutput(public val value: String) {
    public companion object {
        public val Gpu: VideoOutput = VideoOutput("gpu")
        public val GpuNext: VideoOutput = VideoOutput("gpu-next")
        public val Libmpv: VideoOutput = VideoOutput("libmpv")
        public val Null: VideoOutput = VideoOutput("null")
        public val MediacodecEmbed: VideoOutput = VideoOutput("mediacodec_embed")
    }

    public object Codec : MpvCodec<VideoOutput> {
        override fun encode(value: VideoOutput): MpvNode = MpvNode.Str(value.value)
        override fun decode(node: MpvNode): VideoOutput = VideoOutput(node.asString() ?: mismatch("a vo name", node))
    }
}

/** A position in the playlist, or one of mpv's two words for one. */
public sealed interface PlaylistIndex {
    public data class At(val index: Int) : PlaylistIndex
    public data object Current : PlaylistIndex
    public data object None : PlaylistIndex

    public object Codec : MpvCodec<PlaylistIndex> {
        override fun encode(value: PlaylistIndex): MpvNode = when (value) {
            is At -> MpvNode.Int64(value.index.toLong())
            Current -> MpvNode.Str("current")
            None -> MpvNode.Str("none")
        }

        override fun decode(node: MpvNode): PlaylistIndex = when (node.asString()) {
            "current" -> Current
            "none" -> None
            else -> At((node.asLong() ?: mismatch("a playlist index", node)).toInt())
        }
    }
}

/** One entry of `chapter-list`. */
public data class MpvChapter(val title: String?, val timeSeconds: Double) {
    public object Codec : MpvCodec<MpvChapter> {
        override fun encode(value: MpvChapter): MpvNode = MpvNode.Dict(
            buildMap {
                value.title?.let { put("title", MpvNode.Str(it)) }
                put("time", MpvNode.Dbl(value.timeSeconds))
            },
        )

        override fun decode(node: MpvNode): MpvChapter {
            val m = node.asMap() ?: mismatch("a chapter", node)
            return MpvChapter(m["title"]?.asString(), m["time"]?.asDouble() ?: 0.0)
        }
    }

    public object ListCodec : MpvCodec<List<MpvChapter>> {
        override fun encode(value: List<MpvChapter>): MpvNode = MpvNode.Arr(value.map(Codec::encode))
        override fun decode(node: MpvNode): List<MpvChapter> =
            (node.asList() ?: mismatch("a chapter list", node)).map(Codec::decode)
    }
}

/** One entry of `playlist`. */
public data class MpvPlaylistEntry(
    val filename: String,
    val title: String?,
    val id: Long,
    val current: Boolean,
    val playing: Boolean,
    val playlistPath: String?,
) {
    public object Codec : MpvCodec<MpvPlaylistEntry> {
        override fun encode(value: MpvPlaylistEntry): MpvNode =
            MpvNode.Dict(mapOf("filename" to MpvNode.Str(value.filename)))

        override fun decode(node: MpvNode): MpvPlaylistEntry {
            val m = node.asMap() ?: mismatch("a playlist entry", node)
            return MpvPlaylistEntry(
                filename = m["filename"]?.asString() ?: mismatch("a playlist entry with a filename", node),
                title = m["title"]?.asString(),
                id = m["id"]?.asLong() ?: 0L,
                current = m["current"]?.asBoolean() ?: false,
                playing = m["playing"]?.asBoolean() ?: false,
                playlistPath = m["playlist-path"]?.asString(),
            )
        }
    }

    public object ListCodec : MpvCodec<List<MpvPlaylistEntry>> {
        override fun encode(value: List<MpvPlaylistEntry>): MpvNode = MpvNode.Arr(value.map(Codec::encode))
        override fun decode(node: MpvNode): List<MpvPlaylistEntry> =
            (node.asList() ?: mismatch("a playlist", node)).map(Codec::decode)
    }
}

/** One entry of `audio-device-list`. */
public data class MpvAudioDevice(val name: String, val description: String) {
    public object Codec : MpvCodec<MpvAudioDevice> {
        override fun encode(value: MpvAudioDevice): MpvNode =
            MpvNode.Dict(mapOf("name" to MpvNode.Str(value.name), "description" to MpvNode.Str(value.description)))

        override fun decode(node: MpvNode): MpvAudioDevice {
            val m = node.asMap() ?: mismatch("an audio device", node)
            return MpvAudioDevice(m["name"]?.asString().orEmpty(), m["description"]?.asString().orEmpty())
        }
    }

    public object ListCodec : MpvCodec<List<MpvAudioDevice>> {
        override fun encode(value: List<MpvAudioDevice>): MpvNode = MpvNode.Arr(value.map(Codec::encode))
        override fun decode(node: MpvNode): List<MpvAudioDevice> =
            (node.asList() ?: mismatch("an audio device list", node)).map(Codec::decode)
    }
}

/** The `audio-params` map. Every field is absent until a file is playing. */
public data class AudioParams(
    val format: String?,
    val samplerate: Int?,
    val channels: String?,
    val hrChannels: String?,
    val channelCount: Int?,
) {
    public object Codec : MpvCodec<AudioParams> {
        override fun encode(value: AudioParams): MpvNode = throw UnsupportedOperationException("audio-params is read-only")

        override fun decode(node: MpvNode): AudioParams {
            val m = node.asMap() ?: mismatch("audio parameters", node)
            return AudioParams(
                format = m["format"]?.asString(),
                samplerate = m["samplerate"]?.asLong()?.toInt(),
                channels = m["channels"]?.asString(),
                hrChannels = m["hr-channels"]?.asString(),
                channelCount = m["channel-count"]?.asLong()?.toInt(),
            )
        }
    }
}

/** The `video-params` map. mpv's short keys (`w`, `dw`, `crop-x`) are spelled out here. */
public data class VideoParams(
    val pixelformat: String?,
    val hwPixelformat: String?,
    val averageBpp: Int?,
    val width: Int?,
    val height: Int?,
    val displayWidth: Int?,
    val displayHeight: Int?,
    val aspect: Double?,
    val par: Double?,
    val colormatrix: String?,
    val colorlevels: String?,
    val primaries: String?,
    val gamma: String?,
    val sigPeak: Double?,
    val light: String?,
    val chromaLocation: String?,
    val rotate: Int?,
    val stereoIn: String?,
    val alpha: String?,
    val cropLeft: Int?,
    val cropTop: Int?,
    val cropRight: Int?,
    val cropBottom: Int?,
) {
    public object Codec : MpvCodec<VideoParams> {
        override fun encode(value: VideoParams): MpvNode = throw UnsupportedOperationException("video-params is read-only")

        override fun decode(node: MpvNode): VideoParams {
            val m = node.asMap() ?: mismatch("video parameters", node)
            fun s(k: String) = m[k]?.asString()
            fun i(k: String) = m[k]?.asLong()?.toInt()
            fun d(k: String) = m[k]?.asDouble()
            return VideoParams(
                pixelformat = s("pixelformat"), hwPixelformat = s("hw-pixelformat"), averageBpp = i("average-bpp"),
                width = i("w"), height = i("h"), displayWidth = i("dw"), displayHeight = i("dh"),
                aspect = d("aspect"), par = d("par"), colormatrix = s("colormatrix"), colorlevels = s("colorlevels"),
                primaries = s("primaries"), gamma = s("gamma"), sigPeak = d("sig-peak"), light = s("light"),
                chromaLocation = s("chroma-location"), rotate = i("rotate"), stereoIn = s("stereo-in"), alpha = s("alpha"),
                cropLeft = i("crop-x"), cropTop = i("crop-y"), cropRight = i("crop-w"), cropBottom = i("crop-h"),
            )
        }
    }
}

/** The `video-frame-info` map. */
public data class VideoFrameInfo(
    val pictureType: String?,
    val interlaced: Boolean?,
    val tff: Boolean?,
    val repeat: Boolean?,
) {
    public object Codec : MpvCodec<VideoFrameInfo> {
        override fun encode(value: VideoFrameInfo): MpvNode = throw UnsupportedOperationException("video-frame-info is read-only")

        override fun decode(node: MpvNode): VideoFrameInfo {
            val m = node.asMap() ?: mismatch("frame information", node)
            return VideoFrameInfo(
                pictureType = m["picture-type"]?.asString(),
                interlaced = m["interlaced"]?.asBoolean(),
                tff = m["tff"]?.asBoolean(),
                repeat = m["repeat"]?.asBoolean(),
            )
        }
    }
}

/** The `osd-dimensions` map: the size of the OSD surface and its margins, in pixels. */
public data class OsdDimensions(
    val width: Int,
    val height: Int,
    val par: Double,
    val aspect: Double,
    val marginTop: Int,
    val marginBottom: Int,
    val marginLeft: Int,
    val marginRight: Int,
) {
    public object Codec : MpvCodec<OsdDimensions> {
        override fun encode(value: OsdDimensions): MpvNode = throw UnsupportedOperationException("osd-dimensions is read-only")

        override fun decode(node: MpvNode): OsdDimensions {
            val m = node.asMap() ?: mismatch("osd dimensions", node)
            fun i(k: String) = m[k]?.asLong()?.toInt() ?: 0
            return OsdDimensions(
                width = i("w"), height = i("h"),
                par = m["par"]?.asDouble() ?: 0.0, aspect = m["aspect"]?.asDouble() ?: 0.0,
                marginTop = i("mt"), marginBottom = i("mb"), marginLeft = i("ml"), marginRight = i("mr"),
            )
        }
    }
}

/** One byte range the demuxer cache holds, in seconds of playback time. */
public data class SeekableRange(val start: Double, val end: Double)

/** The `demuxer-cache-state` map: what the demuxer has buffered and whether it is stalled. */
public data class DemuxerCacheState(
    val cacheEnd: Double?,
    val readerPts: Double?,
    val cacheDuration: Double?,
    val eof: Boolean,
    val underrun: Boolean,
    val idle: Boolean,
    val totalBytes: Long?,
    val fwBytes: Long?,
    val fileCacheBytes: Long?,
    val rawInputRate: Long?,
    val seekableRanges: List<SeekableRange>,
    val bofCache: Boolean,
    val eofCache: Boolean,
) {
    public object Codec : MpvCodec<DemuxerCacheState> {
        override fun encode(value: DemuxerCacheState): MpvNode = throw UnsupportedOperationException("demuxer-cache-state is read-only")

        override fun decode(node: MpvNode): DemuxerCacheState {
            val m = node.asMap() ?: mismatch("a demuxer cache state", node)
            fun d(k: String) = m[k]?.asDouble()
            fun l(k: String) = m[k]?.asLong()
            fun b(k: String) = m[k]?.asBoolean() ?: false
            val ranges = m["seekable-ranges"]?.asList().orEmpty().mapNotNull { r ->
                val rm = r.asMap() ?: return@mapNotNull null
                SeekableRange(rm["start"]?.asDouble() ?: return@mapNotNull null, rm["end"]?.asDouble() ?: return@mapNotNull null)
            }
            return DemuxerCacheState(
                cacheEnd = d("cache-end"), readerPts = d("reader-pts"), cacheDuration = d("cache-duration"),
                eof = b("eof"), underrun = b("underrun"), idle = b("idle"),
                totalBytes = l("total-bytes"), fwBytes = l("fw-bytes"), fileCacheBytes = l("file-cache-bytes"),
                rawInputRate = l("raw-input-rate"), seekableRanges = ranges,
                bofCache = b("bof-cached"), eofCache = b("eof-cached"),
            )
        }
    }
}

/** One argument of a command, as `command-list` describes it. */
public data class MpvCommandArg(val name: String, val type: String, val optional: Boolean)

/** One entry of `command-list`: what mpv itself says a command takes. */
public data class MpvCommandInfo(val name: String, val args: List<MpvCommandArg>, val varargs: Boolean) {
    public object Codec : MpvCodec<MpvCommandInfo> {
        override fun encode(value: MpvCommandInfo): MpvNode = throw UnsupportedOperationException("command-list is read-only")

        override fun decode(node: MpvNode): MpvCommandInfo {
            val m = node.asMap() ?: mismatch("a command description", node)
            val args = m["args"]?.asList().orEmpty().mapNotNull { a ->
                val am = a.asMap() ?: return@mapNotNull null
                MpvCommandArg(
                    am["name"]?.asString().orEmpty(),
                    am["type"]?.asString().orEmpty(),
                    am["optional"]?.asBoolean() ?: false,
                )
            }
            return MpvCommandInfo(
                name = m["name"]?.asString() ?: mismatch("a command with a name", node),
                args = args,
                varargs = m["vararg"]?.asBoolean() ?: false,
            )
        }
    }

    public object ListCodec : MpvCodec<List<MpvCommandInfo>> {
        override fun encode(value: List<MpvCommandInfo>): MpvNode = throw UnsupportedOperationException("command-list is read-only")
        override fun decode(node: MpvNode): List<MpvCommandInfo> =
            (node.asList() ?: mismatch("a command list", node)).map(Codec::decode)
    }
}

/** A track that may be absent, as `current-tracks/<type>` is when nothing is selected. */
public object NullableTrackCodec : MpvCodec<MpvTrack?> {
    override fun encode(value: MpvTrack?): MpvNode = throw UnsupportedOperationException("current-tracks is read-only")
    override fun decode(node: MpvNode): MpvTrack? = if (node == MpvNode.None) null else MpvTrack.Codec.decode(node)
}
