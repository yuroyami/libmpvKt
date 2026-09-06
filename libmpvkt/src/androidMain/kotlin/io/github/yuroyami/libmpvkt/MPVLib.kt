package io.github.yuroyami.libmpvkt

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import io.github.yuroyami.libmpvkt.MpvEvent as Event
import io.github.yuroyami.libmpvkt.MpvLogLevel as LogLevel

/**
 * The mpv-android surface, kept so a 0.1.0 consumer moves by bumping a version. One process-wide
 * core, string options, typed property getters, observers by interface. Everything here is a
 * thin call into [Mpv]; new capability goes there, not here.
 */
@Deprecated("The mpv-android surface. Use Mpv: typed, per instance, with flows.")
public object MPVLib {
    private var mpv: Mpv? = null
    private var scope: CoroutineScope? = null
    private val observeJobs = mutableListOf<Job>()
    private val observers = mutableListOf<EventObserver>()
    private val logObservers = mutableListOf<LogObserver>()

    private fun core(): Mpv = checkNotNull(mpv) { "MPVLib.create() first" }

    public fun create(appctx: Context) {
        check(mpv == null) { "mpv is already initialized" }
        val core = Mpv.create(appctx)
        mpv = core
        val s = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        scope = s
        s.launch { core.events.collect { e -> if (e !is Event.PropertyChange && e !is Event.LogMessage) fanOut(e) } }
        s.launch { core.logs.collect { l -> synchronized(logObservers) { logObservers.forEach { it.logMessage(l.prefix, l.level.numeric, l.text) } } } }
        core.setOption("msg-level", "all=v")
        core.requestLogMessages(LogLevel.Verbose)
    }

    public fun init() { core().initialize().getOrThrow() }

    public fun destroy() {
        val core = mpv ?: return
        scope?.cancel()
        scope = null
        observeJobs.clear()
        core.close()
        mpv = null
    }

    public fun attachSurface(surface: Surface) { core().attachSurface(surface, vo = core().getString("vo") ?: "gpu") }
    public fun detachSurface() { core().detachSurface() }
    public fun command(cmd: Array<out String>) { core().command(*cmd) }
    public fun setOptionString(name: String, value: String): Int = (core().setOption(name, value) as? MpvResult.Fail)?.error?.code ?: 0
    public fun grabThumbnail(dimension: Int): Bitmap? = core().thumbnail(dimension).getOrNull()
    public fun getPropertyInt(property: String): Int? = core().getNode(property).getOrNull()?.asLong()?.toInt()
    public fun setPropertyInt(property: String, value: Int) { core().setNode(property, MpvNode.Int64(value.toLong())) }
    public fun getPropertyDouble(property: String): Double? = core().getNode(property).getOrNull()?.asDouble()
    public fun setPropertyDouble(property: String, value: Double) { core().setNode(property, MpvNode.Dbl(value)) }
    public fun getPropertyBoolean(property: String): Boolean? = core().getNode(property).getOrNull()?.asBoolean()
    public fun setPropertyBoolean(property: String, value: Boolean) { core().setNode(property, MpvNode.Flag(value)) }
    public fun getPropertyString(property: String): String? = core().getString(property)
    public fun setPropertyString(property: String, value: String) { core().setString(property, value) }

    public fun observeProperty(property: String, format: Int) {
        val s = checkNotNull(scope)
        observeJobs += s.launch {
            core().observeNode(property, format).collect { node ->
                synchronized(observers) {
                    for (o in observers) when (node) {
                        is MpvNode.Int64 -> o.eventProperty(property, node.value)
                        is MpvNode.Flag -> o.eventProperty(property, node.value)
                        is MpvNode.Dbl -> o.eventProperty(property, node.value)
                        is MpvNode.Str -> o.eventProperty(property, node.value)
                        else -> o.eventProperty(property)
                    }
                }
            }
        }
    }

    private fun fanOut(event: Event) {
        val id = when (event) {
            Event.Shutdown -> 1
            is Event.StartFile -> 6
            is Event.EndFile -> 7
            Event.FileLoaded -> 8
            is Event.ClientMessage -> 16
            Event.VideoReconfig -> 17
            Event.AudioReconfig -> 18
            Event.Seek -> 20
            Event.PlaybackRestart -> 21
            Event.QueueOverflow -> 24
            is Event.Hook -> 25
            is Event.Unknown -> event.id
            else -> return
        }
        synchronized(observers) { observers.forEach { it.event(id) } }
    }

    @JvmStatic public fun addObserver(o: EventObserver) { synchronized(observers) { observers.add(o) } }
    @JvmStatic public fun removeObserver(o: EventObserver) { synchronized(observers) { observers.remove(o) } }
    @JvmStatic public fun addLogObserver(o: LogObserver) { synchronized(logObservers) { logObservers.add(o) } }
    @JvmStatic public fun removeLogObserver(o: LogObserver) { synchronized(logObservers) { logObservers.remove(o) } }

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
