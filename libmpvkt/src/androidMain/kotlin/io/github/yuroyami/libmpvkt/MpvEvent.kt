package io.github.yuroyami.libmpvkt

/** Everything mpv reports, as one type per event id. [Mpv.events] emits these. */
public sealed interface MpvEvent {
    public data object Shutdown : MpvEvent
    public data class LogMessage(val prefix: String, val level: MpvLogLevel, val text: String) : MpvEvent
    public data class GetPropertyReply(val replyId: Long, val error: MpvError, val name: String, val value: MpvNode) : MpvEvent
    public data class SetPropertyReply(val replyId: Long, val error: MpvError) : MpvEvent
    public data class CommandReply(val replyId: Long, val error: MpvError, val result: MpvNode) : MpvEvent
    public data class StartFile(val playlistEntryId: Long) : MpvEvent
    public data class EndFile(val reason: EndFileReason, val fileError: String?, val playlistEntryId: Long, val playlistInsertId: Long, val playlistInsertNumEntries: Int) : MpvEvent
    public data object FileLoaded : MpvEvent
    public data class ClientMessage(val args: List<String>) : MpvEvent
    public data object VideoReconfig : MpvEvent
    public data object AudioReconfig : MpvEvent
    public data object Seek : MpvEvent
    public data object PlaybackRestart : MpvEvent
    public data class PropertyChange(val replyId: Long, val name: String, val value: MpvNode) : MpvEvent
    public data object QueueOverflow : MpvEvent
    public data class Hook(val replyId: Long, val name: String, val hookId: Long) : MpvEvent
    public data class Unknown(val id: Int, val node: MpvNode) : MpvEvent
}
/** Why playback of a file ended. */
public enum class EndFileReason(override val mpvName: String) : MpvChoice {
    Eof("eof"),
    Stop("stop"),
    Quit("quit"),
    Error("error"),
    Redirect("redirect"),
    Unknown("unknown"),
}
/** mpv's log levels. [numeric] is the `mpv_log_level` value. */
public enum class MpvLogLevel(override val mpvName: String, public val numeric: Int) : MpvChoice {
    None("no", 0),
    Fatal("fatal", 10),
    Error("error", 20),
    Warn("warn", 30),
    Info("info", 40),
    Verbose("v", 50),
    Debug("debug", 60),
    Trace("trace", 70),
}
