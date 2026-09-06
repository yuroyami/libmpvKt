package io.github.yuroyami.libmpvkt

import android.content.Context
import android.graphics.Bitmap
import android.view.Surface

/**
 * The libmpv client API for Android, as one core per process.
 *
 * Call order: [create], then options through [setOptionString], then [init], then commands and
 * properties. [attachSurface] before playing video, [detachSurface] before the surface goes
 * away, [destroy] when done. A second [create] before [destroy] terminates the process.
 *
 * Property changes, events and log lines arrive on mpv's own event thread, never on the main
 * thread. Do the work on your own thread, and keep the callback short.
 */
public object MPVLib {
    init {
        System.loadLibrary("mpv")
        System.loadLibrary("mpvkt_jni")
    }

    /** Creates the core and hands the application context to FFmpeg for MediaCodec. Nothing runs until [init]. */
    public external fun create(appctx: Context)

    /** Initialises the core and starts the event thread. Options set after this may be ignored; use properties instead. */
    public external fun init()

    /** Stops the event thread and destroys the core. Safe to call when nothing was created. */
    public external fun destroy()

    /** Gives mpv a window to draw into. Pair it with `force-window=yes` and a `vo` value; the sample shows the sequence. */
    public external fun attachSurface(surface: Surface)

    /** Takes the window away. Set `vo` to `null` first so the renderer has stopped using it. */
    public external fun detachSurface()

    /** Runs one mpv command, for example `arrayOf("loadfile", path)` or `arrayOf("seek", "10", "relative")`. */
    public external fun command(cmd: Array<out String>)

    /** Sets an option by name, before or after [init]. Returns 0, or a negative mpv error code. */
    public external fun setOptionString(name: String, value: String): Int

    /** A square thumbnail of the current video frame, [dimension] pixels wide, or null when there is no video. */
    public external fun grabThumbnail(dimension: Int): Bitmap?

    /** The property as an integer, or null when mpv has no value for it. */
    public external fun getPropertyInt(property: String): Int?

    public external fun setPropertyInt(property: String, value: Int)

    /** The property as a double, or null when mpv has no value for it. */
    public external fun getPropertyDouble(property: String): Double?

    public external fun setPropertyDouble(property: String, value: Double)

    /** The property as a flag, or null when mpv has no value for it. */
    public external fun getPropertyBoolean(property: String): Boolean?

    public external fun setPropertyBoolean(property: String, value: Boolean)

    /** The property as a string, or null when mpv has no value for it. */
    public external fun getPropertyString(property: String): String?

    public external fun setPropertyString(property: String, value: String)

    /** Asks mpv to report every change of [property] to the observers, in [format], one of the [MpvFormat] values. */
    public external fun observeProperty(property: String, format: Int)

    private val observers = mutableListOf<EventObserver>()

    @JvmStatic
    public fun addObserver(o: EventObserver) {
        synchronized(observers) { observers.add(o) }
    }

    @JvmStatic
    public fun removeObserver(o: EventObserver) {
        synchronized(observers) { observers.remove(o) }
    }

    // Called from libmpvkt_jni.so by name and signature. None of these has a Kotlin caller.

    @JvmStatic
    public fun eventProperty(property: String, value: Long) {
        synchronized(observers) { for (o in observers) o.eventProperty(property, value) }
    }

    @JvmStatic
    public fun eventProperty(property: String, value: Boolean) {
        synchronized(observers) { for (o in observers) o.eventProperty(property, value) }
    }

    @JvmStatic
    public fun eventProperty(property: String, value: Double) {
        synchronized(observers) { for (o in observers) o.eventProperty(property, value) }
    }

    @JvmStatic
    public fun eventProperty(property: String, value: String) {
        synchronized(observers) { for (o in observers) o.eventProperty(property, value) }
    }

    @JvmStatic
    public fun eventProperty(property: String) {
        synchronized(observers) { for (o in observers) o.eventProperty(property) }
    }

    @JvmStatic
    public fun event(eventId: Int) {
        synchronized(observers) { for (o in observers) o.event(eventId) }
    }

    private val logObservers = mutableListOf<LogObserver>()

    @JvmStatic
    public fun addLogObserver(o: LogObserver) {
        synchronized(logObservers) { logObservers.add(o) }
    }

    @JvmStatic
    public fun removeLogObserver(o: LogObserver) {
        synchronized(logObservers) { logObservers.remove(o) }
    }

    /** Called from libmpvkt_jni.so for every mpv log line. */
    @JvmStatic
    public fun logMessage(prefix: String, level: Int, text: String) {
        synchronized(logObservers) { for (o in logObservers) o.logMessage(prefix, level, text) }
    }

    /** Receives property changes and events. Called on mpv's event thread. */
    public interface EventObserver {
        /** A property changed and was observed in [MpvFormat.MPV_FORMAT_NONE], so it carries no value. */
        public fun eventProperty(property: String)
        public fun eventProperty(property: String, value: Long)
        public fun eventProperty(property: String, value: Boolean)
        public fun eventProperty(property: String, value: String)
        public fun eventProperty(property: String, value: Double)

        /** An event by id, one of the [MpvEvent] values. */
        public fun event(eventId: Int)
    }

    /** Receives mpv's log lines. Called on mpv's event thread. */
    public interface LogObserver {
        /** [level] is one of the [MpvLogLevel] values; [prefix] names the mpv component. */
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
