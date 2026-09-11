package io.github.yuroyami.libmpvkt

import android.content.Context
import android.graphics.Bitmap
import android.util.Log
import android.view.Surface
import java.util.concurrent.CopyOnWriteArrayList
import io.github.yuroyami.libmpvkt.MpvEvent as Event

/**
 * The mpv-android surface, kept so a 0.1.0 consumer moves by bumping a version. One process-wide
 * core, string options, typed property getters, observers by interface. Everything here is a
 * thin call into [Mpv]; new capability goes there, not here.
 *
 * As in 0.1.0, property changes, events and log lines arrive on the core's event thread, in mpv's
 * order, never on the main thread. Keep the callbacks short. An observer that throws is logged.
 */
@Deprecated("The mpv-android surface. Use Mpv: typed, per instance, with flows.")
public object MPVLib {
    private const val TAG = "libmpvKt"
    private var mpv: Mpv? = null
    private val observers = CopyOnWriteArrayList<EventObserver>()
    private val logObservers = CopyOnWriteArrayList<LogObserver>()

    private fun core(): Mpv = checkNotNull(mpv) { "MPVLib.create() first" }

    public fun create(appctx: Context) {
        check(mpv == null) { "mpv is already initialized" }
        val core = Mpv.create(appctx)
        mpv = core
        core.addEventListener(::deliver)
        // As 0.1.0 did: log at the terminal's level, which msg-level decides, starting at v.
        core.requestTerminalDefaultLogs()
        core.setOption("msg-level", "all=v")
    }

    public fun init() { core().initialize().getOrThrow() }

    public fun destroy() {
        val core = mpv ?: return
        core.close()
        mpv = null
    }

    public fun attachSurface(surface: Surface) { core().attachSurface(surface, vo = core().getString("vo") ?: "gpu") }
    public fun detachSurface() { core().detachSurface() }
    public fun command(cmd: Array<out String>) { core().command(*cmd) }
    public fun setOptionString(name: String, value: String): Int = (core().setOption(name, value) as? MpvResult.Fail)?.error?.code ?: 0
    public fun grabThumbnail(dimension: Int): Bitmap? = core().thumbnail(dimension).getOrNull()

    /** As in 0.1.0, which asked mpv for INT64: a double property comes back truncated. */
    public fun getPropertyInt(property: String): Int? = when (val node = core().getNode(property).getOrNull()) {
        is MpvNode.Int64 -> node.value.toInt()
        is MpvNode.Dbl -> node.value.toLong().toInt()
        else -> null
    }

    public fun setPropertyInt(property: String, value: Int) { core().setNode(property, MpvNode.Int64(value.toLong())) }
    public fun getPropertyDouble(property: String): Double? = core().getNode(property).getOrNull()?.asDouble()
    public fun setPropertyDouble(property: String, value: Double) { core().setNode(property, MpvNode.Dbl(value)) }
    public fun getPropertyBoolean(property: String): Boolean? = core().getNode(property).getOrNull()?.asBoolean()
    public fun setPropertyBoolean(property: String, value: Boolean) { core().setNode(property, MpvNode.Flag(value)) }
    public fun getPropertyString(property: String): String? = core().getString(property)
    public fun setPropertyString(property: String, value: String) { core().setString(property, value) }

    /** Reports every change of [property] to the observers, in [format]. A format mpv refuses is logged, as in 0.1.0. */
    public fun observeProperty(property: String, format: Int) {
        val r = core().observeForListeners(property, format)
        if (r < 0) Log.e(TAG, "mpv_observe_property($property) format $format returned error ${MpvError.of(r)}")
    }

    /** Runs on the core's event thread for every event, the way 0.1.0's native event loop did. */
    private fun deliver(e: Event) {
        when (e) {
            is Event.LogMessage -> logMessage(e.prefix, e.level.numeric, e.text)
            is Event.PropertyChange -> if (e.replyId == 0L) when (val v = e.value) {
                MpvNode.None -> eventProperty(e.name)
                is MpvNode.Flag -> eventProperty(e.name, v.value)
                is MpvNode.Int64 -> eventProperty(e.name, v.value)
                is MpvNode.Dbl -> eventProperty(e.name, v.value)
                is MpvNode.Str -> eventProperty(e.name, v.value)
                else -> Unit // 0.1.0 skipped the node formats
            }
            else -> event(idOf(e))
        }
    }

    private fun idOf(e: Event): Int = when (e) {
        Event.Shutdown -> 1
        is Event.LogMessage -> 2
        is Event.GetPropertyReply -> 3
        is Event.SetPropertyReply -> 4
        is Event.CommandReply -> 5
        is Event.StartFile -> 6
        is Event.EndFile -> 7
        Event.FileLoaded -> 8
        is Event.ClientMessage -> 16
        Event.VideoReconfig -> 17
        Event.AudioReconfig -> 18
        Event.Seek -> 20
        Event.PlaybackRestart -> 21
        is Event.PropertyChange -> 22
        Event.QueueOverflow -> 24
        is Event.Hook -> 25
        is Event.Unknown -> e.id
    }

    @JvmStatic public fun addObserver(o: EventObserver) { observers.add(o) }
    @JvmStatic public fun removeObserver(o: EventObserver) { observers.remove(o) }
    @JvmStatic public fun addLogObserver(o: LogObserver) { logObservers.add(o) }
    @JvmStatic public fun removeLogObserver(o: LogObserver) { logObservers.remove(o) }

    // 0.1.0's native code called these by name, so an app built against 0.1.0 may call them too.

    @JvmStatic public fun eventProperty(property: String, value: Long) { eachObserver { it.eventProperty(property, value) } }
    @JvmStatic public fun eventProperty(property: String, value: Boolean) { eachObserver { it.eventProperty(property, value) } }
    @JvmStatic public fun eventProperty(property: String, value: Double) { eachObserver { it.eventProperty(property, value) } }
    @JvmStatic public fun eventProperty(property: String, value: String) { eachObserver { it.eventProperty(property, value) } }
    @JvmStatic public fun eventProperty(property: String) { eachObserver { it.eventProperty(property) } }
    @JvmStatic public fun event(eventId: Int) { eachObserver { it.event(eventId) } }

    @JvmStatic
    public fun logMessage(prefix: String, level: Int, text: String) {
        for (o in logObservers) {
            try { o.logMessage(prefix, level, text) } catch (t: Exception) { Log.e(TAG, "a LogObserver threw", t) }
        }
    }

    /** Calls every observer. One that throws is logged, and the others still run. */
    private inline fun eachObserver(call: (EventObserver) -> Unit) {
        for (o in observers) {
            try { call(o) } catch (t: Exception) { Log.e(TAG, "an EventObserver threw", t) }
        }
    }

    public interface EventObserver {
        public fun eventProperty(property: String)
        public fun eventProperty(property: String, value: Long)
        public fun eventProperty(property: String, value: Boolean)
        public fun eventProperty(property: String, value: String)
        public fun eventProperty(property: String, value: Double)
        public fun event(eventId: Int)
    }

    public interface LogObserver {
        public fun logMessage(prefix: String, level: Int, text: String)
    }

    /** The `mpv_format` values, for [observeProperty]. */
    public object MpvFormat {
        public const val MPV_FORMAT_NONE: Int = 0
        public const val MPV_FORMAT_STRING: Int = 1
        public const val MPV_FORMAT_OSD_STRING: Int = 2
        public const val MPV_FORMAT_FLAG: Int = 3
        public const val MPV_FORMAT_INT64: Int = 4
        public const val MPV_FORMAT_DOUBLE: Int = 5
        public const val MPV_FORMAT_NODE: Int = 6
        public const val MPV_FORMAT_NODE_ARRAY: Int = 7
        public const val MPV_FORMAT_NODE_MAP: Int = 8
        public const val MPV_FORMAT_BYTE_ARRAY: Int = 9
    }
    /** The `mpv_event_id` values [EventObserver.event] receives. */
    public object MpvEvent {
        public const val MPV_EVENT_NONE: Int = 0
        public const val MPV_EVENT_SHUTDOWN: Int = 1
        public const val MPV_EVENT_LOG_MESSAGE: Int = 2
        public const val MPV_EVENT_GET_PROPERTY_REPLY: Int = 3
        public const val MPV_EVENT_SET_PROPERTY_REPLY: Int = 4
        public const val MPV_EVENT_COMMAND_REPLY: Int = 5
        public const val MPV_EVENT_START_FILE: Int = 6
        public const val MPV_EVENT_END_FILE: Int = 7
        public const val MPV_EVENT_FILE_LOADED: Int = 8

        @Deprecated("Deprecated by mpv; not sent by current cores.")
        public const val MPV_EVENT_IDLE: Int = 11

        @Deprecated("Deprecated by mpv; not sent by current cores.")
        public const val MPV_EVENT_TICK: Int = 14
        public const val MPV_EVENT_CLIENT_MESSAGE: Int = 16
        public const val MPV_EVENT_VIDEO_RECONFIG: Int = 17
        public const val MPV_EVENT_AUDIO_RECONFIG: Int = 18
        public const val MPV_EVENT_SEEK: Int = 20
        public const val MPV_EVENT_PLAYBACK_RESTART: Int = 21
        public const val MPV_EVENT_PROPERTY_CHANGE: Int = 22
        public const val MPV_EVENT_QUEUE_OVERFLOW: Int = 24
        public const val MPV_EVENT_HOOK: Int = 25
    }
    /** The `mpv_log_level` values [LogObserver.logMessage] receives. */
    public object MpvLogLevel {
        public const val MPV_LOG_LEVEL_NONE: Int = 0
        public const val MPV_LOG_LEVEL_FATAL: Int = 10
        public const val MPV_LOG_LEVEL_ERROR: Int = 20
        public const val MPV_LOG_LEVEL_WARN: Int = 30
        public const val MPV_LOG_LEVEL_INFO: Int = 40
        public const val MPV_LOG_LEVEL_V: Int = 50
        public const val MPV_LOG_LEVEL_DEBUG: Int = 60
        public const val MPV_LOG_LEVEL_TRACE: Int = 70
    }
}
