package io.github.yuroyami.libmpvkt

/*
 * Every mpv command this library names, as a function whose arguments are the ones the mpv manual
 * lists, in order. An optional argument that is null is left out: at the end it is dropped, and in
 * the middle the command switches to mpv's named arguments, so no later argument moves into its
 * slot. MpvCatalogTest checks each name against mpv's own command-list on a device.
 */
public object MpvCommands {

    // Files and playback

    /** `loadfile`: plays [target], a path or a URL. [options] are per-file option overrides. */
    public fun loadFile(
        target: String,
        mode: LoadFileMode = LoadFileMode.Replace,
        index: Int? = null,
        options: Map<String, String> = emptyMap(),
    ): MpvCommand = MpvCommand.buildNamed(
        "loadfile",
        "url" to target,
        "flags" to mode.mpvName,
        "index" to index,
        "options" to options.takeIf { it.isNotEmpty() }?.let { MpvNode.Dict(it.mapValues { (_, v) -> MpvNode.Str(v) }) },
    )

    /** `loadlist`: loads a playlist file. */
    public fun loadList(path: String, mode: LoadListMode = LoadListMode.Replace, index: Int? = null): MpvCommand =
        MpvCommand.build("loadlist", path, mode.mpvName, index)

    /** `seek`: moves by or to [seconds], depending on [mode]. */
    public fun seek(
        seconds: Double,
        mode: SeekMode = SeekMode.Relative,
        precision: SeekPrecision = SeekPrecision.Default,
    ): MpvCommand = MpvCommand.build(
        "seek",
        seconds,
        listOf(mode.mpvName, precision.mpvName).filter { it.isNotEmpty() }.joinToString("+"),
    )

    /** `revert-seek`: undoes the last seek, or marks the current position. */
    public fun revertSeek(mode: RevertSeekMode? = null): MpvCommand =
        MpvCommand.build("revert-seek", mode?.mpvName)

    /** `frame-step`: steps [frames] forward. */
    public fun frameStep(frames: Int = 1, mode: FrameStepMode = FrameStepMode.Play): MpvCommand =
        MpvCommand.build("frame-step", frames, mode.mpvName)

    /** `frame-back-step`: steps one frame back, which needs a seekable file. */
    public fun frameBackStep(): MpvCommand = MpvCommand.build("frame-back-step")

    /** `stop`: stops playback. [keepPlaylist] leaves the playlist loaded. */
    public fun stop(keepPlaylist: Boolean = false): MpvCommand =
        MpvCommand.build("stop", if (keepPlaylist) "keep-playlist" else null)

    /** `quit`: ends the core with an optional exit code. */
    public fun quit(code: Int? = null): MpvCommand = MpvCommand.build("quit", code)

    /** `quit-watch-later`: quits and writes the resume file. */
    public fun quitWatchLater(code: Int? = null): MpvCommand = MpvCommand.build("quit-watch-later", code)

    // Properties

    /**
     * `set`: writes a property by name. mpv takes the value as a string, so a flag or a number is
     * sent in its string form; for a list or a map, use `Mpv.setNode`.
     */
    public fun set(name: String, value: MpvNode): MpvCommand = MpvCommand.build("set", name, value.commandString())

    /** `set`: writes a property from the catalog, from its own type. */
    public fun <T> set(property: MpvProperty<T>, value: T): MpvCommand = set(property.name, property.encode(value))

    /** `del`: removes a property that can be unset. */
    public fun del(name: String): MpvCommand = MpvCommand.build("del", name)

    /** `add`: adds [value] to a numeric property. */
    public fun add(name: String, value: Double = 1.0): MpvCommand = MpvCommand.build("add", name, value)

    /** `cycle`: steps a property through its values. */
    public fun cycle(name: String, direction: CycleDirection = CycleDirection.Up): MpvCommand =
        MpvCommand.build("cycle", name, direction.mpvName)

    /** `multiply`: multiplies a numeric property. */
    public fun multiply(name: String, factor: Double): MpvCommand = MpvCommand.build("multiply", name, factor)

    /** `cycle-values`: steps a property through the values given here, sent as strings like [set]. */
    public fun cycleValues(name: String, values: List<MpvNode>, reverse: Boolean = false): MpvCommand =
        MpvCommand(
            buildList {
                add(MpvNode.Str("cycle-values"))
                if (reverse) add(MpvNode.Str("!reverse"))
                add(MpvNode.Str(name))
                values.forEach { add(MpvNode.Str(it.commandString())) }
            },
        )

    /** A flag or a number as mpv's command parser reads it. */
    private fun MpvNode.commandString(): String =
        asString() ?: throw IllegalArgumentException("set and cycle-values take a string, a flag or a number, not $this")

    // Screenshots

    /** `screenshot`: writes a screenshot to the screenshot directory. */
    public fun screenshot(mode: ScreenshotMode = ScreenshotMode.Subtitles, eachFrame: Boolean = false): MpvCommand =
        MpvCommand.build("screenshot", mode.mpvName + if (eachFrame) "+each-frame" else "")

    /** `screenshot-to-file`: writes a screenshot to [path]. */
    public fun screenshotToFile(path: String, mode: ScreenshotMode = ScreenshotMode.Subtitles): MpvCommand =
        MpvCommand.build("screenshot-to-file", path, mode.mpvName)

    /** `screenshot-raw`: returns the frame as a map of `w`, `h`, `stride`, `format` and `data`. */
    public fun screenshotRaw(
        mode: ScreenshotMode = ScreenshotMode.Subtitles,
        format: ScreenshotRawFormat = ScreenshotRawFormat.Bgr0,
    ): MpvCommand = MpvCommand.build("screenshot-raw", mode.mpvName, format.mpvName)

    // The playlist

    public fun playlistNext(mode: PlaylistNextMode = PlaylistNextMode.Weak): MpvCommand =
        MpvCommand.build("playlist-next", mode.mpvName)

    public fun playlistPrev(mode: PlaylistNextMode = PlaylistNextMode.Weak): MpvCommand =
        MpvCommand.build("playlist-prev", mode.mpvName)

    public fun playlistNextPlaylist(): MpvCommand = MpvCommand.build("playlist-next-playlist")

    public fun playlistPrevPlaylist(): MpvCommand = MpvCommand.build("playlist-prev-playlist")

    /** `playlist-play-index`: plays one entry, by index or by one of mpv's two words. */
    public fun playlistPlayIndex(index: PlaylistIndex): MpvCommand =
        MpvCommand(listOf(MpvNode.Str("playlist-play-index"), PlaylistIndex.Codec.encode(index)))

    public fun playlistClear(): MpvCommand = MpvCommand.build("playlist-clear")

    public fun playlistRemove(index: PlaylistIndex): MpvCommand =
        MpvCommand(listOf(MpvNode.Str("playlist-remove"), PlaylistIndex.Codec.encode(index)))

    public fun playlistMove(from: Int, to: Int): MpvCommand = MpvCommand.build("playlist-move", from, to)

    public fun playlistShuffle(): MpvCommand = MpvCommand.build("playlist-shuffle")

    public fun playlistUnshuffle(): MpvCommand = MpvCommand.build("playlist-unshuffle")

    // External tracks

    /** `sub-add`: loads a subtitle file or URL. */
    public fun subAdd(
        url: String,
        mode: SubAddMode = SubAddMode.Select,
        title: String? = null,
        lang: String? = null,
    ): MpvCommand = MpvCommand.buildNamed("sub-add", "url" to url, "flags" to mode.mpvName, "title" to title, "lang" to lang)

    public fun subRemove(id: Int? = null): MpvCommand = MpvCommand.build("sub-remove", id)

    public fun subReload(id: Int? = null): MpvCommand = MpvCommand.build("sub-reload", id)

    /** `sub-step`: shifts subtitle timing by [skip] subtitles. */
    public fun subStep(skip: Int, which: SubtitleSlot = SubtitleSlot.Primary): MpvCommand =
        MpvCommand.build("sub-step", skip, which.mpvName)

    /** `sub-seek`: seeks the video to the next or previous subtitle. */
    public fun subSeek(skip: Int, which: SubtitleSlot = SubtitleSlot.Primary): MpvCommand =
        MpvCommand.build("sub-seek", skip, which.mpvName)

    public fun audioAdd(
        url: String,
        mode: SubAddMode = SubAddMode.Select,
        title: String? = null,
        lang: String? = null,
    ): MpvCommand = MpvCommand.buildNamed("audio-add", "url" to url, "flags" to mode.mpvName, "title" to title, "lang" to lang)

    public fun audioRemove(id: Int? = null): MpvCommand = MpvCommand.build("audio-remove", id)

    public fun audioReload(id: Int? = null): MpvCommand = MpvCommand.build("audio-reload", id)

    public fun videoAdd(
        url: String,
        mode: SubAddMode = SubAddMode.Select,
        title: String? = null,
        lang: String? = null,
    ): MpvCommand = MpvCommand.buildNamed("video-add", "url" to url, "flags" to mode.mpvName, "title" to title, "lang" to lang)

    public fun videoRemove(id: Int? = null): MpvCommand = MpvCommand.build("video-remove", id)

    public fun videoReload(id: Int? = null): MpvCommand = MpvCommand.build("video-reload", id)

    /** `rescan-external-files`: looks again for files beside the one playing. */
    public fun rescanExternalFiles(mode: RescanMode = RescanMode.Reselect): MpvCommand =
        MpvCommand.build("rescan-external-files", mode.mpvName)

    // On-screen text

    /** `show-text`: shows [text] on the OSD for [durationMs]. */
    public fun showText(text: String, durationMs: Int? = null, level: Int? = null): MpvCommand =
        MpvCommand.buildNamed("show-text", "text" to text, "duration" to durationMs, "level" to level)

    public fun showProgress(): MpvCommand = MpvCommand.build("show-progress")

    /** `expand-text`: expands mpv's property templates, such as `${time-pos}`. */
    public fun expandText(text: String): MpvCommand = MpvCommand.build("expand-text", text)

    /** `expand-path`: expands `~~/` and the other mpv path prefixes. */
    public fun expandPath(path: String): MpvCommand = MpvCommand.build("expand-path", path)

    /** `print-text`: writes to mpv's log at info level. */
    public fun printText(text: String): MpvCommand = MpvCommand.build("print-text", text)

    /** `escape-ass`: escapes text so libass draws it literally. */
    public fun escapeAss(text: String): MpvCommand = MpvCommand.build("escape-ass", text)

    /** `osd-overlay`: draws ASS over the video. mpv takes this command with named arguments only. */
    public fun osdOverlay(
        id: Int,
        format: OsdOverlayFormat,
        data: String,
        resX: Int = 0,
        resY: Int = 720,
        z: Int = 0,
        hidden: Boolean = false,
        computeBounds: Boolean = false,
    ): MpvCommand = MpvCommand.named(
        "osd-overlay",
        "id" to id,
        "format" to format.mpvName,
        "data" to data,
        "res_x" to resX,
        "res_y" to resY,
        "z" to z,
        "hidden" to hidden,
        "compute_bounds" to computeBounds,
    )

    // Configuration, filters and scripts

    public fun writeWatchLaterConfig(): MpvCommand = MpvCommand.build("write-watch-later-config")

    public fun deleteWatchLaterConfig(filename: String? = null): MpvCommand =
        MpvCommand.build("delete-watch-later-config", filename)

    /** `keypress`: sends a key as if the user pressed it. */
    public fun keypress(name: String, scale: Double? = null): MpvCommand =
        MpvCommand.build("keypress", name, scale)

    public fun keydown(name: String): MpvCommand = MpvCommand.build("keydown", name)

    public fun keyup(name: String? = null): MpvCommand = MpvCommand.build("keyup", name)

    /** `keybind`: binds a key to an input command. */
    public fun keybind(name: String, command: String): MpvCommand = MpvCommand.build("keybind", name, command)

    /** `af`: changes the audio filter chain. */
    public fun af(operation: FilterOperation, value: String): MpvCommand =
        MpvCommand.build("af", operation.mpvName, value)

    /** `vf`: changes the video filter chain. */
    public fun vf(operation: FilterOperation, value: String): MpvCommand =
        MpvCommand.build("vf", operation.mpvName, value)

    public fun afCommand(label: String, command: String, argument: String, target: String? = null): MpvCommand =
        MpvCommand.build("af-command", label, command, argument, target)

    public fun vfCommand(label: String, command: String, argument: String, target: String? = null): MpvCommand =
        MpvCommand.build("vf-command", label, command, argument, target)

    public fun scriptMessage(vararg args: String): MpvCommand =
        MpvCommand(listOf(MpvNode.Str("script-message")) + args.map { MpvNode.Str(it) })

    public fun scriptMessageTo(target: String, vararg args: String): MpvCommand =
        MpvCommand(listOf(MpvNode.Str("script-message-to"), MpvNode.Str(target)) + args.map { MpvNode.Str(it) })

    public fun scriptBinding(name: String): MpvCommand = MpvCommand.build("script-binding", name)

    /** `ab-loop`: sets the next A-B loop point, or clears both. */
    public fun abLoop(): MpvCommand = MpvCommand.build("ab-loop")

    /** `drop-buffers`: throws away decoded and demuxed data, for a hard resync. */
    public fun dropBuffers(): MpvCommand = MpvCommand.build("drop-buffers")

    public fun applyProfile(name: String, mode: ApplyProfileMode = ApplyProfileMode.Apply): MpvCommand =
        MpvCommand.build("apply-profile", name, mode.mpvName)

    public fun loadScript(path: String): MpvCommand = MpvCommand.build("load-script", path)

    public fun loadConfigFile(path: String): MpvCommand = MpvCommand.build("load-config-file", path)

    public fun loadInputConf(path: String): MpvCommand = MpvCommand.build("load-input-conf", path)

    /** `change-list`: adds to, removes from or clears a list option. */
    public fun changeList(name: String, operation: ChangeListOp, value: String): MpvCommand =
        MpvCommand.build("change-list", name, operation.mpvName, value)

    /** `dump-cache`: writes the cached range between [start] and [end] to a file. */
    public fun dumpCache(start: Double, end: Double, filename: String): MpvCommand =
        MpvCommand.build("dump-cache", start, end, filename)

    public fun abLoopDumpCache(filename: String): MpvCommand = MpvCommand.build("ab-loop-dump-cache", filename)

    public fun abLoopAlignCache(): MpvCommand = MpvCommand.build("ab-loop-align-cache")

    /** `ignore`: does nothing, which is what an input binding uses to swallow a key. */
    public fun ignore(): MpvCommand = MpvCommand.build("ignore")

    /**
     * Every command above with a sample call, so a test can compare the names against mpv's own
     * `command-list`. The arguments are only there to build a command; nothing runs them.
     */
    public val all: List<Pair<String, () -> MpvCommand>> = listOf(
        "loadfile" to { loadFile("a.mkv") },
        "loadlist" to { loadList("a.m3u") },
        "seek" to { seek(10.0) },
        "revert-seek" to { revertSeek() },
        "frame-step" to { frameStep() },
        "frame-back-step" to { frameBackStep() },
        "stop" to { stop() },
        "quit" to { quit() },
        "quit-watch-later" to { quitWatchLater() },
        "set" to { set("pause", MpvNode.Flag(true)) },
        "del" to { del("user-data/x") },
        "add" to { add("volume") },
        "cycle" to { cycle("pause") },
        "multiply" to { multiply("speed", 2.0) },
        "cycle-values" to { cycleValues("speed", listOf(MpvNode.Dbl(1.0), MpvNode.Dbl(2.0))) },
        "screenshot" to { screenshot() },
        "screenshot-to-file" to { screenshotToFile("/tmp/a.png") },
        "screenshot-raw" to { screenshotRaw() },
        "playlist-next" to { playlistNext() },
        "playlist-prev" to { playlistPrev() },
        "playlist-next-playlist" to { playlistNextPlaylist() },
        "playlist-prev-playlist" to { playlistPrevPlaylist() },
        "playlist-play-index" to { playlistPlayIndex(PlaylistIndex.Current) },
        "playlist-clear" to { playlistClear() },
        "playlist-remove" to { playlistRemove(PlaylistIndex.Current) },
        "playlist-move" to { playlistMove(0, 1) },
        "playlist-shuffle" to { playlistShuffle() },
        "playlist-unshuffle" to { playlistUnshuffle() },
        "sub-add" to { subAdd("a.srt") },
        "sub-remove" to { subRemove() },
        "sub-reload" to { subReload() },
        "sub-step" to { subStep(1) },
        "sub-seek" to { subSeek(1) },
        "audio-add" to { audioAdd("a.mka") },
        "audio-remove" to { audioRemove() },
        "audio-reload" to { audioReload() },
        "video-add" to { videoAdd("a.mkv") },
        "video-remove" to { videoRemove() },
        "video-reload" to { videoReload() },
        "rescan-external-files" to { rescanExternalFiles() },
        "show-text" to { showText("hello") },
        "show-progress" to { showProgress() },
        "expand-text" to { expandText("x") },
        "expand-path" to { expandPath("~~/") },
        "print-text" to { printText("x") },
        "escape-ass" to { escapeAss("x") },
        "osd-overlay" to { osdOverlay(1, OsdOverlayFormat.AssEvents, "") },
        "write-watch-later-config" to { writeWatchLaterConfig() },
        "delete-watch-later-config" to { deleteWatchLaterConfig() },
        "keypress" to { keypress("a") },
        "keydown" to { keydown("a") },
        "keyup" to { keyup() },
        "keybind" to { keybind("a", "ignore") },
        "af" to { af(FilterOperation.Set, "") },
        "vf" to { vf(FilterOperation.Set, "") },
        "af-command" to { afCommand("l", "c", "a") },
        "vf-command" to { vfCommand("l", "c", "a") },
        "script-message" to { scriptMessage("x") },
        "script-message-to" to { scriptMessageTo("t", "x") },
        "script-binding" to { scriptBinding("x") },
        "ab-loop" to { abLoop() },
        "drop-buffers" to { dropBuffers() },
        "apply-profile" to { applyProfile("fast") },
        "load-script" to { loadScript("a.lua") },
        "load-config-file" to { loadConfigFile("mpv.conf") },
        "load-input-conf" to { loadInputConf("input.conf") },
        "change-list" to { changeList("sub-file-paths", ChangeListOp.Append, "x") },
        "dump-cache" to { dumpCache(0.0, 1.0, "/tmp/a.mkv") },
        "ab-loop-dump-cache" to { abLoopDumpCache("/tmp/a.mkv") },
        "ab-loop-align-cache" to { abLoopAlignCache() },
        "ignore" to { ignore() },
    )
}
