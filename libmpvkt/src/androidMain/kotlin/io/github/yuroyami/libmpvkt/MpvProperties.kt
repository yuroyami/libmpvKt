package io.github.yuroyami.libmpvkt

/*
 * Every mpv property and option this library names, with the type mpv gives it. A property
 * that is also an option is set before initialize() with Mpv.setOption and after it with
 * Mpv.set, under the same key. MpvCatalogTest checks each name against mpv's own
 * property-list on a device, so a name that upstream renames fails there rather than at a
 * user's runtime.
 */
public object MpvProperties {

    // Playback and timing
    public val Pause: MpvProperty<Boolean> = MpvProperty.Flag("pause")
    public val TimePos: MpvProperty<Double> = MpvProperty.Dbl("time-pos")
    public val PlaybackTime: MpvProperty<Double> = MpvProperty.Dbl("playback-time")
    public val TimeRemaining: MpvProperty<Double> = MpvProperty.Dbl("time-remaining")
    public val PlaytimeRemaining: MpvProperty<Double> = MpvProperty.Dbl("playtime-remaining")
    public val Duration: MpvProperty<Double> = MpvProperty.Dbl("duration")
    public val PercentPos: MpvProperty<Double> = MpvProperty.Dbl("percent-pos")
    public val Speed: MpvProperty<Double> = MpvProperty.Dbl("speed")
    public val AudioSpeedCorrection: MpvProperty<Double> = MpvProperty.Dbl("audio-speed-correction")
    public val VideoSpeedCorrection: MpvProperty<Double> = MpvProperty.Dbl("video-speed-correction")
    public val DisplaySyncActive: MpvProperty<Boolean> = MpvProperty.Flag("display-sync-active")
    public val Seeking: MpvProperty<Boolean> = MpvProperty.Flag("seeking")
    public val Seekable: MpvProperty<Boolean> = MpvProperty.Flag("seekable")
    public val PartiallySeekable: MpvProperty<Boolean> = MpvProperty.Flag("partially-seekable")
    public val EofReached: MpvProperty<Boolean> = MpvProperty.Flag("eof-reached")
    public val CoreIdle: MpvProperty<Boolean> = MpvProperty.Flag("core-idle")
    public val IdleActive: MpvProperty<Boolean> = MpvProperty.Flag("idle-active")
    public val PausedForCache: MpvProperty<Boolean> = MpvProperty.Flag("paused-for-cache")
    public val CacheBufferingState: MpvProperty<Long> = MpvProperty.Int64("cache-buffering-state")
    public val DemuxerCacheDuration: MpvProperty<Double> = MpvProperty.Dbl("demuxer-cache-duration")
    public val DemuxerCacheTime: MpvProperty<Double> = MpvProperty.Dbl("demuxer-cache-time")
    public val DemuxerCacheIdle: MpvProperty<Boolean> = MpvProperty.Flag("demuxer-cache-idle")
    public val DemuxerCacheState: MpvProperty<DemuxerCacheState> = MpvProperty.Typed("demuxer-cache-state", io.github.yuroyami.libmpvkt.DemuxerCacheState.Codec)
    public val DemuxerViaNetwork: MpvProperty<Boolean> = MpvProperty.Flag("demuxer-via-network")
    public val DemuxerStartTime: MpvProperty<Double> = MpvProperty.Dbl("demuxer-start-time")
    public val EstimatedFrameCount: MpvProperty<Long> = MpvProperty.Int64("estimated-frame-count")
    public val EstimatedFrameNumber: MpvProperty<Long> = MpvProperty.Int64("estimated-frame-number")
    public val FrameDropCount: MpvProperty<Long> = MpvProperty.Int64("frame-drop-count")
    public val MistimedFrameCount: MpvProperty<Long> = MpvProperty.Int64("mistimed-frame-count")
    public val VsyncRatio: MpvProperty<Double> = MpvProperty.Dbl("vsync-ratio")
    public val VoDelayedFrameCount: MpvProperty<Long> = MpvProperty.Int64("vo-delayed-frame-count")
    public val Avsync: MpvProperty<Double> = MpvProperty.Dbl("avsync")
    public val TotalAvsyncChange: MpvProperty<Double> = MpvProperty.Dbl("total-avsync-change")
    public val HrSeek: MpvProperty<HrSeekMode> = MpvProperty.Choice("hr-seek", HrSeekMode.entries)
    public val HrSeekFramedrop: MpvProperty<Boolean> = MpvProperty.Flag("hr-seek-framedrop")
    public val HrSeekDemuxerOffset: MpvProperty<Double> = MpvProperty.Dbl("hr-seek-demuxer-offset")
    public val Framedrop: MpvProperty<FramedropMode> = MpvProperty.Choice("framedrop", FramedropMode.entries)
    public val KeepOpen: MpvProperty<KeepOpenMode> = MpvProperty.Choice("keep-open", KeepOpenMode.entries)
    public val KeepOpenPause: MpvProperty<Boolean> = MpvProperty.Flag("keep-open-pause")
    public val Idle: MpvProperty<IdleMode> = MpvProperty.Choice("idle", IdleMode.entries)
    public val Start: MpvProperty<String> = MpvProperty.Str("start")
    public val End: MpvProperty<String> = MpvProperty.Str("end")
    public val Length: MpvProperty<String> = MpvProperty.Str("length")
    public val AbLoopA: MpvProperty<AbLoopPoint> = MpvProperty.Typed("ab-loop-a", AbLoopPoint.Codec)
    public val AbLoopB: MpvProperty<AbLoopPoint> = MpvProperty.Typed("ab-loop-b", AbLoopPoint.Codec)
    public val AbLoopCount: MpvProperty<LoopCount> = MpvProperty.Typed("ab-loop-count", LoopCount.Codec)
    public val LoopFile: MpvProperty<LoopCount> = MpvProperty.Typed("loop-file", LoopCount.Codec)
    public val LoopPlaylist: MpvProperty<LoopCount> = MpvProperty.Typed("loop-playlist", LoopCount.Codec)
    public val ChapterSeekThreshold: MpvProperty<Double> = MpvProperty.Dbl("chapter-seek-threshold")
    public val ResumePlayback: MpvProperty<Boolean> = MpvProperty.Flag("resume-playback")
    public val SavePositionOnQuit: MpvProperty<Boolean> = MpvProperty.Flag("save-position-on-quit")

    // File, playlist, chapters, editions
    public val Filename: MpvProperty<String> = MpvProperty.Str("filename")
    public val FilenameNoExt: MpvProperty<String> = MpvProperty.Str("filename/no-ext")
    public val FileSize: MpvProperty<Long> = MpvProperty.Int64("file-size")
    public val Path: MpvProperty<String> = MpvProperty.Str("path")
    public val StreamOpenFilename: MpvProperty<String> = MpvProperty.Str("stream-open-filename")
    public val MediaTitle: MpvProperty<String> = MpvProperty.Str("media-title")
    public val FileFormat: MpvProperty<String> = MpvProperty.Str("file-format")
    public val CurrentDemuxer: MpvProperty<String> = MpvProperty.Str("current-demuxer")
    public val StreamPath: MpvProperty<String> = MpvProperty.Str("stream-path")
    public val StreamPos: MpvProperty<Long> = MpvProperty.Int64("stream-pos")
    public val StreamEnd: MpvProperty<Long> = MpvProperty.Int64("stream-end")
    public val Metadata: MpvProperty<Map<String, String>> = MpvProperty.Typed("metadata", StringMapCodec)
    public val FilteredMetadata: MpvProperty<Map<String, String>> = MpvProperty.Typed("filtered-metadata", StringMapCodec)
    public val ChapterMetadata: MpvProperty<Map<String, String>> = MpvProperty.Typed("chapter-metadata", StringMapCodec)
    public val Chapter: MpvProperty<Long> = MpvProperty.Int64("chapter")
    public val Chapters: MpvProperty<Long> = MpvProperty.Int64("chapters")
    public val ChapterList: MpvProperty<List<MpvChapter>> = MpvProperty.Typed("chapter-list", MpvChapter.ListCodec)
    public val Edition: MpvProperty<EditionSelection> = MpvProperty.Typed("edition", EditionSelection.Codec)
    public val CurrentEdition: MpvProperty<Long> = MpvProperty.Int64("current-edition")
    public val Editions: MpvProperty<Long> = MpvProperty.Int64("editions")
    public val EditionList: MpvProperty<MpvNode> = MpvProperty.Node("edition-list")
    public val PlaylistPos: MpvProperty<Long> = MpvProperty.Int64("playlist-pos")
    public val PlaylistPos1: MpvProperty<Long> = MpvProperty.Int64("playlist-pos-1")
    public val PlaylistCurrentPos: MpvProperty<Long> = MpvProperty.Int64("playlist-current-pos")
    public val PlaylistPlayingPos: MpvProperty<Long> = MpvProperty.Int64("playlist-playing-pos")
    public val PlaylistCount: MpvProperty<Long> = MpvProperty.Int64("playlist-count")
    public val Playlist: MpvProperty<List<MpvPlaylistEntry>> = MpvProperty.Typed("playlist", MpvPlaylistEntry.ListCodec)
    public val PlaylistPath: MpvProperty<String> = MpvProperty.Str("playlist-path")
    public val Shuffle: MpvProperty<Boolean> = MpvProperty.Flag("shuffle")
    public val MergeFiles: MpvProperty<Boolean> = MpvProperty.Flag("merge-files")
    public val ChaptersFile: MpvProperty<String> = MpvProperty.Str("chapters-file")
    public val ExternalFiles: MpvProperty<List<String>> = MpvProperty.Typed("external-files", StringListCodec)
    public val AutoloadFiles: MpvProperty<Boolean> = MpvProperty.Flag("autoload-files")
    public val CoverArtAuto: MpvProperty<AutoloadMode> = MpvProperty.Choice("cover-art-auto", AutoloadMode.entries)
    public val CoverArtFiles: MpvProperty<List<String>> = MpvProperty.Typed("cover-art-files", StringListCodec)
    public val AudioDisplay: MpvProperty<AudioDisplayMode> = MpvProperty.Choice("audio-display", AudioDisplayMode.entries)
    public val ImageDisplayDuration: MpvProperty<String> = MpvProperty.Str("image-display-duration")

    // Audio
    public val Volume: MpvProperty<Double> = MpvProperty.Dbl("volume")
    public val VolumeMax: MpvProperty<Double> = MpvProperty.Dbl("volume-max")
    public val VolumeGain: MpvProperty<Double> = MpvProperty.Dbl("volume-gain")
    public val Mute: MpvProperty<Boolean> = MpvProperty.Flag("mute")
    public val AoVolume: MpvProperty<Double> = MpvProperty.Dbl("ao-volume")
    public val AoMute: MpvProperty<Boolean> = MpvProperty.Flag("ao-mute")
    public val AudioDelay: MpvProperty<Double> = MpvProperty.Dbl("audio-delay")
    public val Aid: MpvProperty<TrackSelection> = MpvProperty.Typed("aid", TrackSelection.Codec)
    public val Alang: MpvProperty<List<String>> = MpvProperty.Typed("alang", StringListCodec)
    public val AudioCodec: MpvProperty<String> = MpvProperty.Str("audio-codec")
    public val AudioCodecName: MpvProperty<String> = MpvProperty.Str("audio-codec-name")
    public val AudioParams: MpvProperty<AudioParams> = MpvProperty.Typed("audio-params", io.github.yuroyami.libmpvkt.AudioParams.Codec)
    public val AudioOutParams: MpvProperty<AudioParams> = MpvProperty.Typed("audio-out-params", io.github.yuroyami.libmpvkt.AudioParams.Codec)
    public val AudioBitrate: MpvProperty<Long> = MpvProperty.Int64("audio-bitrate")
    public val AudioDevice: MpvProperty<String> = MpvProperty.Str("audio-device")
    public val AudioDeviceList: MpvProperty<List<MpvAudioDevice>> = MpvProperty.Typed("audio-device-list", MpvAudioDevice.ListCodec)
    public val CurrentAo: MpvProperty<String> = MpvProperty.Str("current-ao")
    public val Ao: MpvProperty<String> = MpvProperty.Str("ao")
    public val AudioChannels: MpvProperty<String> = MpvProperty.Str("audio-channels")
    public val AudioSamplerate: MpvProperty<Long> = MpvProperty.Int64("audio-samplerate")
    public val AudioFormat: MpvProperty<String> = MpvProperty.Str("audio-format")
    public val AudioPitchCorrection: MpvProperty<Boolean> = MpvProperty.Flag("audio-pitch-correction")
    public val AudioExclusive: MpvProperty<Boolean> = MpvProperty.Flag("audio-exclusive")
    public val AudioNormalizeDownmix: MpvProperty<Boolean> = MpvProperty.Flag("audio-normalize-downmix")
    public val AudioBuffer: MpvProperty<Double> = MpvProperty.Dbl("audio-buffer")
    public val AudioStreamSilence: MpvProperty<Boolean> = MpvProperty.Flag("audio-stream-silence")
    public val AudioWaitOpen: MpvProperty<Double> = MpvProperty.Dbl("audio-wait-open")
    public val GaplessAudio: MpvProperty<GaplessAudioMode> = MpvProperty.Choice("gapless-audio", GaplessAudioMode.entries)
    public val Replaygain: MpvProperty<ReplaygainMode> = MpvProperty.Choice("replaygain", ReplaygainMode.entries)
    public val ReplaygainPreamp: MpvProperty<Double> = MpvProperty.Dbl("replaygain-preamp")
    public val ReplaygainClip: MpvProperty<Boolean> = MpvProperty.Flag("replaygain-clip")
    public val ReplaygainFallback: MpvProperty<Double> = MpvProperty.Dbl("replaygain-fallback")
    public val Af: MpvProperty<String> = MpvProperty.Str("af")
    public val AudioFiles: MpvProperty<List<String>> = MpvProperty.Typed("audio-files", StringListCodec)
    public val AudioFileAuto: MpvProperty<AutoloadMode> = MpvProperty.Choice("audio-file-auto", AutoloadMode.entries)

    // Video
    public val Vid: MpvProperty<TrackSelection> = MpvProperty.Typed("vid", TrackSelection.Codec)
    public val Vlang: MpvProperty<List<String>> = MpvProperty.Typed("vlang", StringListCodec)
    public val VideoCodec: MpvProperty<String> = MpvProperty.Str("video-codec")
    public val VideoFormat: MpvProperty<String> = MpvProperty.Str("video-format")
    public val VideoParams: MpvProperty<VideoParams> = MpvProperty.Typed("video-params", io.github.yuroyami.libmpvkt.VideoParams.Codec)
    public val VideoDecParams: MpvProperty<VideoParams> = MpvProperty.Typed("video-dec-params", io.github.yuroyami.libmpvkt.VideoParams.Codec)
    public val VideoOutParams: MpvProperty<VideoParams> = MpvProperty.Typed("video-out-params", io.github.yuroyami.libmpvkt.VideoParams.Codec)
    public val VideoFrameInfo: MpvProperty<VideoFrameInfo> = MpvProperty.Typed("video-frame-info", io.github.yuroyami.libmpvkt.VideoFrameInfo.Codec)
    public val VideoBitrate: MpvProperty<Long> = MpvProperty.Int64("video-bitrate")
    public val Width: MpvProperty<Long> = MpvProperty.Int64("width")
    public val Height: MpvProperty<Long> = MpvProperty.Int64("height")
    public val Dwidth: MpvProperty<Long> = MpvProperty.Int64("dwidth")
    public val Dheight: MpvProperty<Long> = MpvProperty.Int64("dheight")
    public val ContainerFps: MpvProperty<Double> = MpvProperty.Dbl("container-fps")
    public val EstimatedVfFps: MpvProperty<Double> = MpvProperty.Dbl("estimated-vf-fps")
    public val CurrentVo: MpvProperty<String> = MpvProperty.Str("current-vo")
    public val CurrentGpuContext: MpvProperty<String> = MpvProperty.Str("current-gpu-context")
    public val Vo: MpvProperty<VideoOutput> = MpvProperty.Typed("vo", VideoOutput.Codec)
    public val GpuContext: MpvProperty<String> = MpvProperty.Str("gpu-context")
    public val GpuApi: MpvProperty<GpuApi> = MpvProperty.Choice("gpu-api", io.github.yuroyami.libmpvkt.GpuApi.entries)
    public val OpenglEs: MpvProperty<OpenglEsMode> = MpvProperty.Choice("opengl-es", OpenglEsMode.entries)
    public val Hwdec: MpvProperty<HwdecMode> = MpvProperty.Typed("hwdec", HwdecMode.Codec)
    public val HwdecCurrent: MpvProperty<String> = MpvProperty.Str("hwdec-current")
    public val HwdecInterop: MpvProperty<String> = MpvProperty.Str("hwdec-interop")
    public val HwdecCodecs: MpvProperty<String> = MpvProperty.Str("hwdec-codecs")
    public val Vf: MpvProperty<String> = MpvProperty.Str("vf")
    public val Deinterlace: MpvProperty<DeinterlaceMode> = MpvProperty.Choice("deinterlace", DeinterlaceMode.entries)
    public val VideoSync: MpvProperty<VideoSyncMode> = MpvProperty.Choice("video-sync", VideoSyncMode.entries)
    public val VideoSyncMaxVideoChange: MpvProperty<Double> = MpvProperty.Dbl("video-sync-max-video-change")
    public val VideoSyncMaxAudioChange: MpvProperty<Double> = MpvProperty.Dbl("video-sync-max-audio-change")
    public val Interpolation: MpvProperty<Boolean> = MpvProperty.Flag("interpolation")
    public val Tscale: MpvProperty<TemporalScaler> = MpvProperty.Choice("tscale", TemporalScaler.entries)
    public val Scale: MpvProperty<Scaler> = MpvProperty.Choice("scale", Scaler.entries)
    public val Dscale: MpvProperty<Scaler> = MpvProperty.Choice("dscale", Scaler.entries)
    public val Cscale: MpvProperty<Scaler> = MpvProperty.Choice("cscale", Scaler.entries)
    public val ScaleAntiring: MpvProperty<Double> = MpvProperty.Dbl("scale-antiring")
    public val Deband: MpvProperty<Boolean> = MpvProperty.Flag("deband")
    public val DebandIterations: MpvProperty<Long> = MpvProperty.Int64("deband-iterations")
    public val DebandThreshold: MpvProperty<Double> = MpvProperty.Dbl("deband-threshold")
    public val DebandRange: MpvProperty<Double> = MpvProperty.Dbl("deband-range")
    public val DebandGrain: MpvProperty<Double> = MpvProperty.Dbl("deband-grain")
    public val Dither: MpvProperty<DitherMode> = MpvProperty.Choice("dither", DitherMode.entries)
    public val DitherDepth: MpvProperty<String> = MpvProperty.Str("dither-depth")
    public val SigmoidUpscaling: MpvProperty<Boolean> = MpvProperty.Flag("sigmoid-upscaling")
    public val LinearDownscaling: MpvProperty<Boolean> = MpvProperty.Flag("linear-downscaling")
    public val CorrectDownscaling: MpvProperty<Boolean> = MpvProperty.Flag("correct-downscaling")
    public val ToneMapping: MpvProperty<ToneMapping> = MpvProperty.Choice("tone-mapping", io.github.yuroyami.libmpvkt.ToneMapping.entries)
    public val ToneMappingParam: MpvProperty<Double> = MpvProperty.Dbl("tone-mapping-param")
    public val HdrComputePeak: MpvProperty<AutoYesNo> = MpvProperty.Choice("hdr-compute-peak", AutoYesNo.entries)
    public val TargetPeak: MpvProperty<String> = MpvProperty.Str("target-peak")
    public val TargetTrc: MpvProperty<TransferCharacteristic> = MpvProperty.Choice("target-trc", TransferCharacteristic.entries)
    public val TargetPrim: MpvProperty<ColorPrimaries> = MpvProperty.Choice("target-prim", ColorPrimaries.entries)
    public val TargetColorspaceHint: MpvProperty<AutoYesNo> = MpvProperty.Choice("target-colorspace-hint", AutoYesNo.entries)
    public val IccProfile: MpvProperty<String> = MpvProperty.Str("icc-profile")
    public val IccProfileAuto: MpvProperty<Boolean> = MpvProperty.Flag("icc-profile-auto")
    public val Contrast: MpvProperty<Long> = MpvProperty.Int64("contrast")
    public val Brightness: MpvProperty<Long> = MpvProperty.Int64("brightness")
    public val Gamma: MpvProperty<Long> = MpvProperty.Int64("gamma")
    public val Saturation: MpvProperty<Long> = MpvProperty.Int64("saturation")
    public val Hue: MpvProperty<Long> = MpvProperty.Int64("hue")
    public val VideoAspectOverride: MpvProperty<AspectOverride> = MpvProperty.Typed("video-aspect-override", AspectOverride.Codec)
    public val VideoAspectMethod: MpvProperty<AspectMethod> = MpvProperty.Choice("video-aspect-method", AspectMethod.entries)
    public val VideoRotate: MpvProperty<VideoRotation> = MpvProperty.Typed("video-rotate", VideoRotation.Codec)
    public val VideoZoom: MpvProperty<Double> = MpvProperty.Dbl("video-zoom")
    public val VideoPanX: MpvProperty<Double> = MpvProperty.Dbl("video-pan-x")
    public val VideoPanY: MpvProperty<Double> = MpvProperty.Dbl("video-pan-y")
    public val VideoAlignX: MpvProperty<Double> = MpvProperty.Dbl("video-align-x")
    public val VideoAlignY: MpvProperty<Double> = MpvProperty.Dbl("video-align-y")
    public val VideoScaleX: MpvProperty<Double> = MpvProperty.Dbl("video-scale-x")
    public val VideoScaleY: MpvProperty<Double> = MpvProperty.Dbl("video-scale-y")
    public val VideoMarginRatioLeft: MpvProperty<Double> = MpvProperty.Dbl("video-margin-ratio-left")
    public val VideoMarginRatioRight: MpvProperty<Double> = MpvProperty.Dbl("video-margin-ratio-right")
    public val VideoMarginRatioTop: MpvProperty<Double> = MpvProperty.Dbl("video-margin-ratio-top")
    public val VideoMarginRatioBottom: MpvProperty<Double> = MpvProperty.Dbl("video-margin-ratio-bottom")
    public val VideoCrop: MpvProperty<String> = MpvProperty.Str("video-crop")
    public val Panscan: MpvProperty<Double> = MpvProperty.Dbl("panscan")
    public val Keepaspect: MpvProperty<Boolean> = MpvProperty.Flag("keepaspect")
    public val KeepaspectWindow: MpvProperty<Boolean> = MpvProperty.Flag("keepaspect-window")
    public val VideoUnscaled: MpvProperty<VideoUnscaledMode> = MpvProperty.Choice("video-unscaled", VideoUnscaledMode.entries)
    public val OsdDimensions: MpvProperty<OsdDimensions> = MpvProperty.Typed("osd-dimensions", io.github.yuroyami.libmpvkt.OsdDimensions.Codec)
    public val OsdWidth: MpvProperty<Long> = MpvProperty.Int64("osd-width")
    public val OsdHeight: MpvProperty<Long> = MpvProperty.Int64("osd-height")
    public val OsdPar: MpvProperty<Double> = MpvProperty.Dbl("osd-par")
    public val DisplayFps: MpvProperty<Double> = MpvProperty.Dbl("display-fps")
    public val DisplayFpsOverride: MpvProperty<Double> = MpvProperty.Dbl("display-fps-override")
    public val EstimatedDisplayFps: MpvProperty<Double> = MpvProperty.Dbl("estimated-display-fps")
    public val VsyncJitter: MpvProperty<Double> = MpvProperty.Dbl("vsync-jitter")
    public val DisplayWidth: MpvProperty<Long> = MpvProperty.Int64("display-width")
    public val DisplayHeight: MpvProperty<Long> = MpvProperty.Int64("display-height")
    public val VoConfigured: MpvProperty<Boolean> = MpvProperty.Flag("vo-configured")
    public val VoPasses: MpvProperty<MpvNode> = MpvProperty.Node("vo-passes")
    public val PerfInfo: MpvProperty<MpvNode> = MpvProperty.Node("perf-info")
    public val VdLavcThreads: MpvProperty<Long> = MpvProperty.Int64("vd-lavc-threads")
    public val VdLavcFast: MpvProperty<Boolean> = MpvProperty.Flag("vd-lavc-fast")
    public val VdLavcSkiploopfilter: MpvProperty<String> = MpvProperty.Str("vd-lavc-skiploopfilter")
    public val VdLavcFramedrop: MpvProperty<String> = MpvProperty.Str("vd-lavc-framedrop")
    public val VdLavcDr: MpvProperty<AutoYesNo> = MpvProperty.Choice("vd-lavc-dr", AutoYesNo.entries)
    public val AndroidSurfaceSize: MpvProperty<String> = MpvProperty.Str("android-surface-size")
    public val Wid: MpvProperty<Long> = MpvProperty.Int64("wid")
    public val ForceWindow: MpvProperty<ForceWindowMode> = MpvProperty.Choice("force-window", ForceWindowMode.entries)
    public val ScreenshotFormat: MpvProperty<ScreenshotFormat> = MpvProperty.Choice("screenshot-format", io.github.yuroyami.libmpvkt.ScreenshotFormat.entries)
    public val ScreenshotTemplate: MpvProperty<String> = MpvProperty.Str("screenshot-template")
    public val ScreenshotDirectory: MpvProperty<String> = MpvProperty.Str("screenshot-directory")
    public val ScreenshotJpegQuality: MpvProperty<Long> = MpvProperty.Int64("screenshot-jpeg-quality")
    public val ScreenshotPngCompression: MpvProperty<Long> = MpvProperty.Int64("screenshot-png-compression")
    public val ScreenshotTagColorspace: MpvProperty<Boolean> = MpvProperty.Flag("screenshot-tag-colorspace")
    public val ScreenshotHighBitDepth: MpvProperty<Boolean> = MpvProperty.Flag("screenshot-high-bit-depth")
    public val ScreenshotSw: MpvProperty<Boolean> = MpvProperty.Flag("screenshot-sw")

    // Subtitles and OSD
    public val Sid: MpvProperty<TrackSelection> = MpvProperty.Typed("sid", TrackSelection.Codec)
    public val SecondarySid: MpvProperty<TrackSelection> = MpvProperty.Typed("secondary-sid", TrackSelection.Codec)
    public val Slang: MpvProperty<List<String>> = MpvProperty.Typed("slang", StringListCodec)
    public val SubDelay: MpvProperty<Double> = MpvProperty.Dbl("sub-delay")
    public val SecondarySubDelay: MpvProperty<Double> = MpvProperty.Dbl("secondary-sub-delay")
    public val SubPos: MpvProperty<Long> = MpvProperty.Int64("sub-pos")
    public val SecondarySubPos: MpvProperty<Long> = MpvProperty.Int64("secondary-sub-pos")
    public val SubScale: MpvProperty<Double> = MpvProperty.Dbl("sub-scale")
    public val SubVisibility: MpvProperty<Boolean> = MpvProperty.Flag("sub-visibility")
    public val SecondarySubVisibility: MpvProperty<Boolean> = MpvProperty.Flag("secondary-sub-visibility")
    public val SubText: MpvProperty<String> = MpvProperty.Str("sub-text")
    public val SecondarySubText: MpvProperty<String> = MpvProperty.Str("secondary-sub-text")
    public val SubTextAss: MpvProperty<String> = MpvProperty.Str("sub-text-ass")
    public val SubStart: MpvProperty<Double> = MpvProperty.Dbl("sub-start")
    public val SubEnd: MpvProperty<Double> = MpvProperty.Dbl("sub-end")
    public val SecondarySubStart: MpvProperty<Double> = MpvProperty.Dbl("secondary-sub-start")
    public val SecondarySubEnd: MpvProperty<Double> = MpvProperty.Dbl("secondary-sub-end")
    public val SubAssExtradata: MpvProperty<String> = MpvProperty.Str("sub-ass-extradata")
    public val SubFont: MpvProperty<String> = MpvProperty.Str("sub-font")
    public val SubFontSize: MpvProperty<Double> = MpvProperty.Dbl("sub-font-size")
    public val SubColor: MpvProperty<String> = MpvProperty.Str("sub-color")
    public val SubOutlineColor: MpvProperty<String> = MpvProperty.Str("sub-outline-color")
    public val SubBackColor: MpvProperty<String> = MpvProperty.Str("sub-back-color")
    public val SubShadowColor: MpvProperty<String> = MpvProperty.Str("sub-shadow-color")
    public val SubOutlineSize: MpvProperty<Double> = MpvProperty.Dbl("sub-outline-size")
    public val SubShadowOffset: MpvProperty<Double> = MpvProperty.Dbl("sub-shadow-offset")
    public val SubSpacing: MpvProperty<Double> = MpvProperty.Dbl("sub-spacing")
    public val SubMarginX: MpvProperty<Long> = MpvProperty.Int64("sub-margin-x")
    public val SubMarginY: MpvProperty<Long> = MpvProperty.Int64("sub-margin-y")
    public val SubAlignX: MpvProperty<HorizontalAlign> = MpvProperty.Choice("sub-align-x", HorizontalAlign.entries)
    public val SubAlignY: MpvProperty<VerticalAlign> = MpvProperty.Choice("sub-align-y", VerticalAlign.entries)
    public val SubJustify: MpvProperty<Justify> = MpvProperty.Choice("sub-justify", Justify.entries)
    public val SubBold: MpvProperty<Boolean> = MpvProperty.Flag("sub-bold")
    public val SubItalic: MpvProperty<Boolean> = MpvProperty.Flag("sub-italic")
    public val SubUseMargins: MpvProperty<Boolean> = MpvProperty.Flag("sub-use-margins")
    public val SubAssOverride: MpvProperty<SubAssOverrideMode> = MpvProperty.Choice("sub-ass-override", SubAssOverrideMode.entries)
    public val SecondarySubAssOverride: MpvProperty<SubAssOverrideMode> = MpvProperty.Choice("secondary-sub-ass-override", SubAssOverrideMode.entries)
    public val SubAssStyleOverrides: MpvProperty<List<String>> = MpvProperty.Typed("sub-ass-style-overrides", StringListCodec)
    public val SubAssForceMargins: MpvProperty<Boolean> = MpvProperty.Flag("sub-ass-force-margins")
    public val SubAssHinting: MpvProperty<SubAssHinting> = MpvProperty.Choice("sub-ass-hinting", io.github.yuroyami.libmpvkt.SubAssHinting.entries)
    public val SubAssLineSpacing: MpvProperty<Double> = MpvProperty.Dbl("sub-ass-line-spacing")
    public val SubAssShaper: MpvProperty<SubAssShaper> = MpvProperty.Choice("sub-ass-shaper", io.github.yuroyami.libmpvkt.SubAssShaper.entries)
    public val SubAssStyles: MpvProperty<String> = MpvProperty.Str("sub-ass-styles")
    public val SubAssUseVideoData: MpvProperty<SubAssUseVideoData> = MpvProperty.Choice("sub-ass-use-video-data", io.github.yuroyami.libmpvkt.SubAssUseVideoData.entries)
    public val SubAssVsfilterColorCompat: MpvProperty<VsfilterColorCompat> = MpvProperty.Choice("sub-ass-vsfilter-color-compat", VsfilterColorCompat.entries)
    public val SubFixTiming: MpvProperty<Boolean> = MpvProperty.Flag("sub-fix-timing")
    public val SubForcedEventsOnly: MpvProperty<Boolean> = MpvProperty.Flag("sub-forced-events-only")
    public val SubFps: MpvProperty<Double> = MpvProperty.Dbl("sub-fps")
    public val SubSpeed: MpvProperty<Double> = MpvProperty.Dbl("sub-speed")
    public val SubCodepage: MpvProperty<String> = MpvProperty.Str("sub-codepage")
    public val SubAuto: MpvProperty<AutoloadMode> = MpvProperty.Choice("sub-auto", AutoloadMode.entries)
    public val SubFilePaths: MpvProperty<List<String>> = MpvProperty.Typed("sub-file-paths", StringListCodec)
    public val SubFiles: MpvProperty<List<String>> = MpvProperty.Typed("sub-files", StringListCodec)
    public val SubFontProvider: MpvProperty<SubFontProvider> = MpvProperty.Choice("sub-font-provider", io.github.yuroyami.libmpvkt.SubFontProvider.entries)
    public val SubFontsDir: MpvProperty<String> = MpvProperty.Str("sub-fonts-dir")
    public val SubGauss: MpvProperty<Double> = MpvProperty.Dbl("sub-gauss")
    public val SubGray: MpvProperty<Boolean> = MpvProperty.Flag("sub-gray")
    public val SubBlur: MpvProperty<Double> = MpvProperty.Dbl("sub-blur")
    public val BlendSubtitles: MpvProperty<BlendSubtitlesMode> = MpvProperty.Choice("blend-subtitles", BlendSubtitlesMode.entries)
    public val SubClearOnSeek: MpvProperty<Boolean> = MpvProperty.Flag("sub-clear-on-seek")
    public val SubScaleByWindow: MpvProperty<Boolean> = MpvProperty.Flag("sub-scale-by-window")
    public val SubScaleWithWindow: MpvProperty<Boolean> = MpvProperty.Flag("sub-scale-with-window")
    public val SubAssScaleWithWindow: MpvProperty<Boolean> = MpvProperty.Flag("sub-ass-scale-with-window")
    public val SubPastVideoEnd: MpvProperty<Boolean> = MpvProperty.Flag("sub-past-video-end")
    public val SubFilterSdh: MpvProperty<Boolean> = MpvProperty.Flag("sub-filter-sdh")
    public val SubFilterSdhHarder: MpvProperty<Boolean> = MpvProperty.Flag("sub-filter-sdh-harder")
    public val SubFilterRegex: MpvProperty<List<String>> = MpvProperty.Typed("sub-filter-regex", StringListCodec)
    public val SubFilterRegexEnable: MpvProperty<Boolean> = MpvProperty.Flag("sub-filter-regex-enable")
    public val StretchImageSubsToScreen: MpvProperty<Boolean> = MpvProperty.Flag("stretch-image-subs-to-screen")
    public val ImageSubsVideoResolution: MpvProperty<Boolean> = MpvProperty.Flag("image-subs-video-resolution")
    public val OsdLevel: MpvProperty<Long> = MpvProperty.Int64("osd-level")
    public val OsdDuration: MpvProperty<Long> = MpvProperty.Int64("osd-duration")
    public val OsdFont: MpvProperty<String> = MpvProperty.Str("osd-font")
    public val OsdFontSize: MpvProperty<Double> = MpvProperty.Dbl("osd-font-size")
    public val OsdColor: MpvProperty<String> = MpvProperty.Str("osd-color")
    public val OsdBar: MpvProperty<Boolean> = MpvProperty.Flag("osd-bar")
    public val OsdMsg1: MpvProperty<String> = MpvProperty.Str("osd-msg1")
    public val OsdMsg2: MpvProperty<String> = MpvProperty.Str("osd-msg2")
    public val OsdMsg3: MpvProperty<String> = MpvProperty.Str("osd-msg3")
    public val OsdStatusMsg: MpvProperty<String> = MpvProperty.Str("osd-status-msg")
    public val OsdPlayingMsg: MpvProperty<String> = MpvProperty.Str("osd-playing-msg")
    public val OsdScale: MpvProperty<Double> = MpvProperty.Dbl("osd-scale")
    public val OsdScaleByWindow: MpvProperty<Boolean> = MpvProperty.Flag("osd-scale-by-window")
    public val OsdOnSeek: MpvProperty<OsdOnSeekMode> = MpvProperty.Choice("osd-on-seek", OsdOnSeekMode.entries)

    // Tracks
    public val TrackList: MpvProperty<List<MpvTrack>> = MpvProperty.Typed("track-list", MpvTrack.ListCodec)
    public val TrackListCount: MpvProperty<Long> = MpvProperty.Int64("track-list/count")
    public val CurrentTrackVideo: MpvProperty<MpvTrack?> = MpvProperty.Typed("current-tracks/video", NullableTrackCodec)
    public val CurrentTrackAudio: MpvProperty<MpvTrack?> = MpvProperty.Typed("current-tracks/audio", NullableTrackCodec)
    public val CurrentTrackSub: MpvProperty<MpvTrack?> = MpvProperty.Typed("current-tracks/sub", NullableTrackCodec)
    public val CurrentTrackSub2: MpvProperty<MpvTrack?> = MpvProperty.Typed("current-tracks/sub2", NullableTrackCodec)

    // Network, cache, demuxer
    public val Cache: MpvProperty<AutoYesNo> = MpvProperty.Choice("cache", AutoYesNo.entries)
    public val CacheSecs: MpvProperty<Double> = MpvProperty.Dbl("cache-secs")
    public val CachePause: MpvProperty<Boolean> = MpvProperty.Flag("cache-pause")
    public val CachePauseWait: MpvProperty<Double> = MpvProperty.Dbl("cache-pause-wait")
    public val CachePauseInitial: MpvProperty<Boolean> = MpvProperty.Flag("cache-pause-initial")
    public val DemuxerMaxBytes: MpvProperty<Long> = MpvProperty.Int64("demuxer-max-bytes")
    public val DemuxerMaxBackBytes: MpvProperty<Long> = MpvProperty.Int64("demuxer-max-back-bytes")
    public val DemuxerReadaheadSecs: MpvProperty<Double> = MpvProperty.Dbl("demuxer-readahead-secs")
    public val DemuxerHysteresisSecs: MpvProperty<Double> = MpvProperty.Dbl("demuxer-hysteresis-secs")
    public val DemuxerSeekableCache: MpvProperty<AutoYesNo> = MpvProperty.Choice("demuxer-seekable-cache", AutoYesNo.entries)
    public val DemuxerThread: MpvProperty<Boolean> = MpvProperty.Flag("demuxer-thread")
    public val DemuxerTerminationTimeout: MpvProperty<Double> = MpvProperty.Dbl("demuxer-termination-timeout")
    public val StreamBufferSize: MpvProperty<String> = MpvProperty.Str("stream-buffer-size")
    public val NetworkTimeout: MpvProperty<Double> = MpvProperty.Dbl("network-timeout")
    public val UserAgent: MpvProperty<String> = MpvProperty.Str("user-agent")
    public val HttpHeaderFields: MpvProperty<List<String>> = MpvProperty.Typed("http-header-fields", StringListCodec)
    public val HttpProxy: MpvProperty<String> = MpvProperty.Str("http-proxy")
    public val Referrer: MpvProperty<String> = MpvProperty.Str("referrer")
    public val Cookies: MpvProperty<Boolean> = MpvProperty.Flag("cookies")
    public val CookiesFile: MpvProperty<String> = MpvProperty.Str("cookies-file")
    public val TlsVerify: MpvProperty<Boolean> = MpvProperty.Flag("tls-verify")
    public val TlsCaFile: MpvProperty<String> = MpvProperty.Str("tls-ca-file")
    public val TlsCertFile: MpvProperty<String> = MpvProperty.Str("tls-cert-file")
    public val TlsKeyFile: MpvProperty<String> = MpvProperty.Str("tls-key-file")
    public val StreamLavfO: MpvProperty<Map<String, String>> = MpvProperty.Typed("stream-lavf-o", StringMapCodec)
    public val DemuxerLavfO: MpvProperty<Map<String, String>> = MpvProperty.Typed("demuxer-lavf-o", StringMapCodec)
    public val DemuxerLavfFormat: MpvProperty<String> = MpvProperty.Str("demuxer-lavf-format")
    public val DemuxerLavfProbesize: MpvProperty<Long> = MpvProperty.Int64("demuxer-lavf-probesize")
    public val DemuxerLavfAnalyzeduration: MpvProperty<Double> = MpvProperty.Dbl("demuxer-lavf-analyzeduration")
    public val DemuxerLavfProbeInfo: MpvProperty<ProbeInfoMode> = MpvProperty.Choice("demuxer-lavf-probe-info", ProbeInfoMode.entries)
    public val DemuxerLavfHacks: MpvProperty<Boolean> = MpvProperty.Flag("demuxer-lavf-hacks")
    public val DemuxerLavfBuffersize: MpvProperty<Long> = MpvProperty.Int64("demuxer-lavf-buffersize")
    public val DemuxerMkvSubtitlePreroll: MpvProperty<SubtitlePrerollMode> = MpvProperty.Choice("demuxer-mkv-subtitle-preroll", SubtitlePrerollMode.entries)
    public val DemuxerMkvSubtitlePrerollSecs: MpvProperty<Double> = MpvProperty.Dbl("demuxer-mkv-subtitle-preroll-secs")
    public val Index: MpvProperty<IndexMode> = MpvProperty.Choice("index", IndexMode.entries)
    public val ForceSeekable: MpvProperty<Boolean> = MpvProperty.Flag("force-seekable")
    public val LoadUnsafePlaylists: MpvProperty<Boolean> = MpvProperty.Flag("load-unsafe-playlists")
    public val AccessReferences: MpvProperty<Boolean> = MpvProperty.Flag("access-references")
    public val Ytdl: MpvProperty<Boolean> = MpvProperty.Flag("ytdl")

    // Core, configuration, introspection
    public val MpvVersion: MpvProperty<String> = MpvProperty.Str("mpv-version")
    public val MpvConfiguration: MpvProperty<String> = MpvProperty.Str("mpv-configuration")
    public val FfmpegVersion: MpvProperty<String> = MpvProperty.Str("ffmpeg-version")
    public val LibassVersion: MpvProperty<Long> = MpvProperty.Int64("libass-version")
    public val Platform: MpvProperty<String> = MpvProperty.Str("platform")
    public val WorkingDirectory: MpvProperty<String> = MpvProperty.Str("working-directory")
    public val Pid: MpvProperty<Long> = MpvProperty.Int64("pid")
    public val Config: MpvProperty<Boolean> = MpvProperty.Flag("config")
    public val ConfigDir: MpvProperty<String> = MpvProperty.Str("config-dir")
    public val LoadScripts: MpvProperty<Boolean> = MpvProperty.Flag("load-scripts")
    public val Scripts: MpvProperty<List<String>> = MpvProperty.Typed("scripts", StringListCodec)
    public val ScriptOpts: MpvProperty<Map<String, String>> = MpvProperty.Typed("script-opts", StringMapCodec)
    public val MsgLevel: MpvProperty<Map<String, String>> = MpvProperty.Typed("msg-level", StringMapCodec)
    public val LogFile: MpvProperty<String> = MpvProperty.Str("log-file")
    public val InputDefaultBindings: MpvProperty<Boolean> = MpvProperty.Flag("input-default-bindings")
    public val InputConf: MpvProperty<String> = MpvProperty.Str("input-conf")
    public val InputCommands: MpvProperty<List<String>> = MpvProperty.Typed("input-commands", StringListCodec)
    public val LoadStatsOverlay: MpvProperty<Boolean> = MpvProperty.Flag("load-stats-overlay")
    public val LoadOsdConsole: MpvProperty<Boolean> = MpvProperty.Flag("load-osd-console")
    public val LoadAutoProfiles: MpvProperty<AutoYesNo> = MpvProperty.Choice("load-auto-profiles", AutoYesNo.entries)
    public val Profile: MpvProperty<List<String>> = MpvProperty.Typed("profile", StringListCodec)
    public val ProfileList: MpvProperty<MpvNode> = MpvProperty.Node("profile-list")
    public val PropertyList: MpvProperty<List<String>> = MpvProperty.Typed("property-list", StringListCodec)
    public val CommandList: MpvProperty<List<MpvCommandInfo>> = MpvProperty.Typed("command-list", MpvCommandInfo.ListCodec)
    public val InputBindings: MpvProperty<MpvNode> = MpvProperty.Node("input-bindings")
    public val ProtocolList: MpvProperty<List<String>> = MpvProperty.Typed("protocol-list", StringListCodec)
    public val DecoderList: MpvProperty<MpvNode> = MpvProperty.Node("decoder-list")
    public val EncoderList: MpvProperty<MpvNode> = MpvProperty.Node("encoder-list")
    public val DemuxerLavfList: MpvProperty<List<String>> = MpvProperty.Typed("demuxer-lavf-list", StringListCodec)
    public val UserData: MpvProperty<MpvNode> = MpvProperty.Node("user-data")
    public val Clock: MpvProperty<String> = MpvProperty.Str("clock")
    public val WindowId: MpvProperty<Long> = MpvProperty.Int64("window-id")
    public val WatchLaterDir: MpvProperty<String> = MpvProperty.Str("watch-later-dir")
    public val WatchLaterOptions: MpvProperty<List<String>> = MpvProperty.Typed("watch-later-options", StringListCodec)
    public val ResetOnNextFile: MpvProperty<List<String>> = MpvProperty.Typed("reset-on-next-file", StringListCodec)
    public val GpuShaderCacheDir: MpvProperty<String> = MpvProperty.Str("gpu-shader-cache-dir")
    public val IccCacheDir: MpvProperty<String> = MpvProperty.Str("icc-cache-dir")
    public val GpuDebug: MpvProperty<Boolean> = MpvProperty.Flag("gpu-debug")
    public val GpuDumbMode: MpvProperty<AutoYesNo> = MpvProperty.Choice("gpu-dumb-mode", AutoYesNo.entries)

    /** `option-info/<name>`: type, default, choices, min and max of an option. */
    public fun optionInfo(name: String): MpvProperty<MpvNode> = MpvProperty.Node("option-info/$name")

    /** `options/<name>`: an option's current value, as a node. */
    public fun option(name: String): MpvProperty<MpvNode> = MpvProperty.Node("options/$name")

    /** Every fixed entry above, for tests and tooling. */
    public val all: List<MpvProperty<*>> = listOf(
        Pause, TimePos, PlaybackTime, TimeRemaining, PlaytimeRemaining, Duration,
        PercentPos, Speed, AudioSpeedCorrection, VideoSpeedCorrection, DisplaySyncActive, Seeking,
        Seekable, PartiallySeekable, EofReached, CoreIdle, IdleActive, PausedForCache,
        CacheBufferingState, DemuxerCacheDuration, DemuxerCacheTime, DemuxerCacheIdle, DemuxerCacheState, DemuxerViaNetwork,
        DemuxerStartTime, EstimatedFrameCount, EstimatedFrameNumber, FrameDropCount, MistimedFrameCount, VsyncRatio,
        VoDelayedFrameCount, Avsync, TotalAvsyncChange, HrSeek, HrSeekFramedrop, HrSeekDemuxerOffset,
        Framedrop, KeepOpen, KeepOpenPause, Idle, Start, End,
        Length, AbLoopA, AbLoopB, AbLoopCount, LoopFile, LoopPlaylist,
        ChapterSeekThreshold, ResumePlayback, SavePositionOnQuit, Filename, FilenameNoExt, FileSize,
        Path, StreamOpenFilename, MediaTitle, FileFormat, CurrentDemuxer, StreamPath,
        StreamPos, StreamEnd, Metadata, FilteredMetadata, ChapterMetadata, Chapter,
        Chapters, ChapterList, Edition, CurrentEdition, Editions, EditionList,
        PlaylistPos, PlaylistPos1, PlaylistCurrentPos, PlaylistPlayingPos, PlaylistCount, Playlist,
        PlaylistPath, Shuffle, MergeFiles, ChaptersFile, ExternalFiles, AutoloadFiles,
        CoverArtAuto, CoverArtFiles, AudioDisplay, ImageDisplayDuration, Volume, VolumeMax,
        VolumeGain, Mute, AoVolume, AoMute, AudioDelay, Aid,
        Alang, AudioCodec, AudioCodecName, AudioParams, AudioOutParams, AudioBitrate,
        AudioDevice, AudioDeviceList, CurrentAo, Ao, AudioChannels, AudioSamplerate,
        AudioFormat, AudioPitchCorrection, AudioExclusive, AudioNormalizeDownmix, AudioBuffer, AudioStreamSilence,
        AudioWaitOpen, GaplessAudio, Replaygain, ReplaygainPreamp, ReplaygainClip, ReplaygainFallback,
        Af, AudioFiles, AudioFileAuto, Vid, Vlang, VideoCodec,
        VideoFormat, VideoParams, VideoDecParams, VideoOutParams, VideoFrameInfo, VideoBitrate,
        Width, Height, Dwidth, Dheight, ContainerFps, EstimatedVfFps,
        CurrentVo, CurrentGpuContext, Vo, GpuContext, GpuApi, OpenglEs,
        Hwdec, HwdecCurrent, HwdecInterop, HwdecCodecs, Vf, Deinterlace,
        VideoSync, VideoSyncMaxVideoChange, VideoSyncMaxAudioChange, Interpolation, Tscale, Scale,
        Dscale, Cscale, ScaleAntiring, Deband, DebandIterations, DebandThreshold,
        DebandRange, DebandGrain, Dither, DitherDepth, SigmoidUpscaling, LinearDownscaling,
        CorrectDownscaling, ToneMapping, ToneMappingParam, HdrComputePeak, TargetPeak, TargetTrc,
        TargetPrim, TargetColorspaceHint, IccProfile, IccProfileAuto, Contrast, Brightness,
        Gamma, Saturation, Hue, VideoAspectOverride, VideoAspectMethod, VideoRotate,
        VideoZoom, VideoPanX, VideoPanY, VideoAlignX, VideoAlignY, VideoScaleX,
        VideoScaleY, VideoMarginRatioLeft, VideoMarginRatioRight, VideoMarginRatioTop, VideoMarginRatioBottom, VideoCrop,
        Panscan, Keepaspect, KeepaspectWindow, VideoUnscaled, OsdDimensions, OsdWidth,
        OsdHeight, OsdPar, DisplayFps, DisplayFpsOverride, EstimatedDisplayFps, VsyncJitter,
        DisplayWidth, DisplayHeight, VoConfigured, VoPasses, PerfInfo, VdLavcThreads,
        VdLavcFast, VdLavcSkiploopfilter, VdLavcFramedrop, VdLavcDr, AndroidSurfaceSize, Wid,
        ForceWindow, ScreenshotFormat, ScreenshotTemplate, ScreenshotDirectory, ScreenshotJpegQuality, ScreenshotPngCompression,
        ScreenshotTagColorspace, ScreenshotHighBitDepth, ScreenshotSw, Sid, SecondarySid, Slang,
        SubDelay, SecondarySubDelay, SubPos, SecondarySubPos, SubScale, SubVisibility,
        SecondarySubVisibility, SubText, SecondarySubText, SubTextAss, SubStart, SubEnd,
        SecondarySubStart, SecondarySubEnd, SubAssExtradata, SubFont, SubFontSize, SubColor,
        SubOutlineColor, SubBackColor, SubShadowColor, SubOutlineSize, SubShadowOffset, SubSpacing,
        SubMarginX, SubMarginY, SubAlignX, SubAlignY, SubJustify, SubBold,
        SubItalic, SubUseMargins, SubAssOverride, SecondarySubAssOverride, SubAssStyleOverrides, SubAssForceMargins,
        SubAssHinting, SubAssLineSpacing, SubAssShaper, SubAssStyles, SubAssUseVideoData, SubAssVsfilterColorCompat,
        SubFixTiming, SubForcedEventsOnly, SubFps, SubSpeed, SubCodepage, SubAuto,
        SubFilePaths, SubFiles, SubFontProvider, SubFontsDir, SubGauss, SubGray,
        SubBlur, BlendSubtitles, SubClearOnSeek, SubScaleByWindow, SubScaleWithWindow, SubAssScaleWithWindow,
        SubPastVideoEnd, SubFilterSdh, SubFilterSdhHarder, SubFilterRegex, SubFilterRegexEnable, StretchImageSubsToScreen,
        ImageSubsVideoResolution, OsdLevel, OsdDuration, OsdFont, OsdFontSize, OsdColor,
        OsdBar, OsdMsg1, OsdMsg2, OsdMsg3, OsdStatusMsg, OsdPlayingMsg,
        OsdScale, OsdScaleByWindow, OsdOnSeek, TrackList, TrackListCount, CurrentTrackVideo,
        CurrentTrackAudio, CurrentTrackSub, CurrentTrackSub2, Cache, CacheSecs, CachePause,
        CachePauseWait, CachePauseInitial, DemuxerMaxBytes, DemuxerMaxBackBytes, DemuxerReadaheadSecs, DemuxerHysteresisSecs,
        DemuxerSeekableCache, DemuxerThread, DemuxerTerminationTimeout, StreamBufferSize, NetworkTimeout, UserAgent,
        HttpHeaderFields, HttpProxy, Referrer, Cookies, CookiesFile, TlsVerify,
        TlsCaFile, TlsCertFile, TlsKeyFile, StreamLavfO, DemuxerLavfO, DemuxerLavfFormat,
        DemuxerLavfProbesize, DemuxerLavfAnalyzeduration, DemuxerLavfProbeInfo, DemuxerLavfHacks, DemuxerLavfBuffersize, DemuxerMkvSubtitlePreroll,
        DemuxerMkvSubtitlePrerollSecs, Index, ForceSeekable, LoadUnsafePlaylists, AccessReferences, Ytdl,
        MpvVersion, MpvConfiguration, FfmpegVersion, LibassVersion, Platform, WorkingDirectory,
        Pid, Config, ConfigDir, LoadScripts, Scripts, ScriptOpts,
        MsgLevel, LogFile, InputDefaultBindings, InputConf, InputCommands, LoadStatsOverlay,
        LoadOsdConsole, LoadAutoProfiles, Profile, ProfileList, PropertyList, CommandList,
        InputBindings, ProtocolList, DecoderList, EncoderList, DemuxerLavfList, UserData,
        Clock, WindowId, WatchLaterDir, WatchLaterOptions, ResetOnNextFile, GpuShaderCacheDir,
        IccCacheDir, GpuDebug, GpuDumbMode,
    )
}
