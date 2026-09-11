package io.github.yuroyami.libmpvkt

import android.content.Context
import android.util.Log
import android.view.Surface
import io.github.yuroyami.libmpvkt.jni.MpvNative
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi
import io.github.yuroyami.libmpvkt.stream.MpvStreamProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.DisposableHandle
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.buffer
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.launch

/**
 * One libmpv core, or one client handle on a shared core (see [createClient]).
 *
 * Create with [create], set options, call [initialize], then use properties and commands from any
 * thread. Events, log lines and observed properties arrive as flows, delivered from this
 * instance's own event thread; collect them on whatever dispatcher suits the caller. [close]
 * ends the core; a `quit` command does the same and [events] then ends with [MpvEvent.Shutdown].
 *
 * mpv errors are [MpvResult] values. [close] waits for calls already inside native code on other
 * threads; calling anything after it throws [IllegalStateException].
 */
@OptIn(MpvNativeApi::class)
public class Mpv private constructor(
    private val handle: Long,
    private val ownsCore: Boolean,
    /** The handle this client was made from, which closes it first. Null for a core. */
    private val parent: Mpv? = null,
) : AutoCloseable {

    public val clientName: String = MpvNative.clientName(handle)

    private val closing = AtomicBoolean(false)

    /** Every native call on [handle] runs inside this gate, so [close] can wait for it. */
    private val gate = CallGate()

    /** Clients made with [createClient]. mpv_terminate_destroy waits for every one, so [close] closes them first. */
    private val clients: MutableSet<Mpv> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var initialized = false

    private val replyIds = AtomicLong(1)
    private val replies = PendingReplies()
    private val observers = ConcurrentHashMap<Long, (MpvNode) -> Unit>()
    private val hooks = ConcurrentHashMap<Long, suspend (MpvEvent.Hook) -> Unit>()
    private val hookScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val listeners = CopyOnWriteArrayList<(MpvEvent) -> Unit>()

    /** Every event of this handle except log lines, in order. Slow collectors lose the oldest events, never the newest. */
    public val events: SharedFlow<MpvEvent>
        field = MutableSharedFlow(extraBufferCapacity = 512, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Log lines, once [requestLogMessages] asked for them. They arrive here only, so a burst cannot push events out of [events]. */
    public val logs: SharedFlow<MpvEvent.LogMessage>
        field = MutableSharedFlow(extraBufferCapacity = 512, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val eventThread = Thread({ pump() }, "mpv-events-$clientName").apply { isDaemon = true }

    /** The raw `mpv_handle`, for the library's own renderer. Not for apps. */
    @InternalLibmpvKtApi
    public val nativeHandle: Long get() = handle

    private val beforeClose = mutableListOf<() -> Unit>()

    /**
     * Runs [block] once, before the core is destroyed. A renderer registers here so its render context
     * dies first. Dispose the handle when the owner of [block] closes first, so the core stops holding it.
     */
    public fun onBeforeClose(block: () -> Unit): DisposableHandle {
        synchronized(beforeClose) {
            check(!closing.get()) { "this Mpv is closed" }
            beforeClose += block
        }
        return object : DisposableHandle {
            override fun dispose() {
                synchronized(beforeClose) { beforeClose.remove(block) }
            }
        }
    }

    public val isInitialized: Boolean get() = initialized
    public val isClosed: Boolean get() = closing.get()

    // ---- lifecycle ----

    /** Sets an option before [initialize]. After it, use [set]; mpv ignores most options set late. */
    public fun <T> setOption(option: MpvProperty<T>, value: T): MpvResult<Unit> {
        val bytes = NodeCodec.encode(option.encode(value))
        return gate.call { unitResult(MpvNative.setOptionNode(handle, option.name, bytes)) }
    }

    public fun setOption(name: String, value: String): MpvResult<Unit> =
        gate.call { unitResult(MpvNative.setOptionString(handle, name, value)) }

    /** Starts the core and this handle's event thread. Once. */
    public fun initialize(): MpvResult<Unit> = gate.call {
        check(!initialized) { "initialize() was already called" }
        val r = MpvNative.initialize(handle)
        if (r >= 0) {
            initialized = true
            eventThread.start()
        }
        unitResult(r)
    }

    /**
     * A second handle on the same core with its own event thread. Closing it does not end the core;
     * closing this handle closes it first, because mpv cannot end a core while a client is open.
     */
    public fun createClient(name: String): Mpv = gate.call {
        val h = MpvNative.createClient(handle, name)
        check(h != 0L) { "mpv_create_client failed" }
        Mpv(h, ownsCore = false, parent = this).also {
            clients += it
            it.initialized = true
            it.eventThread.start()
        }
    }

    override fun close() {
        if (!closing.compareAndSet(false, true)) return
        synchronized(beforeClose) { beforeClose.toList().also { beforeClose.clear() } }.forEach { runCatching(it) }
        // New calls fail from here, and the calls already inside native code finish before the handle is freed.
        gate.close()
        // mpv_terminate_destroy waits for every client of the core, so the clients go first.
        clients.toList().forEach { it.close() }
        if (eventThread.isAlive && Thread.currentThread() !== eventThread) {
            MpvNative.wakeup(handle)
            eventThread.join(5_000)
        }
        hookScope.cancel()
        replies.end()
        observers.clear()
        if (ownsCore) MpvNative.terminateDestroy(handle) else MpvNative.destroy(handle)
        parent?.clients?.remove(this)
        releaseSurface()
    }

    // ---- properties ----

    public operator fun <T> get(property: MpvProperty<T>): MpvResult<T> = getNode(property.name).flatDecode(property)

    public operator fun <T> set(property: MpvProperty<T>, value: T): MpvResult<Unit> = setNode(property.name, property.encode(value))

    public fun getNode(name: String): MpvResult<MpvNode> {
        val (error, node) = NodeCodec.decodeEnvelope(gate.call { MpvNative.getPropertyNode(handle, name) })
        return if (error < 0) MpvResult.Fail(MpvError.of(error), name) else MpvResult.Ok(node)
    }

    public fun setNode(name: String, node: MpvNode): MpvResult<Unit> {
        val bytes = NodeCodec.encode(node)
        return gate.call { unitResult(MpvNative.setPropertyNode(handle, name, bytes)) }
    }

    public fun getString(name: String): String? = gate.call { MpvNative.getPropertyString(handle, name) }
    public fun getOsdString(name: String): String? = gate.call { MpvNative.getPropertyOsdString(handle, name) }
    public fun setString(name: String, value: String): MpvResult<Unit> = gate.call { unitResult(MpvNative.setPropertyString(handle, name, value)) }
    public fun delete(name: String): MpvResult<Unit> = gate.call { unitResult(MpvNative.delProperty(handle, name)) }

    /** Reads [property] and suspends until mpv answers. A core that shuts down first gives a [MpvResult.Fail]. */
    public suspend fun <T> getAsync(property: MpvProperty<T>): MpvResult<T> {
        val reply = when (val r = await(property.name) { id -> MpvNative.getPropertyAsync(handle, id, property.name) }) {
            is MpvResult.Fail -> return r
            is MpvResult.Ok -> r.value as MpvEvent.GetPropertyReply
        }
        return if (reply.error.isError) MpvResult.Fail(reply.error, property.name) else MpvResult.Ok(reply.value).flatDecode(property)
    }

    /** Sets [property] and suspends until mpv answers. A core that shuts down first gives a [MpvResult.Fail]. */
    public suspend fun <T> setAsync(property: MpvProperty<T>, value: T): MpvResult<Unit> {
        val bytes = NodeCodec.encode(property.encode(value))
        val reply = when (val r = await(property.name) { id -> MpvNative.setPropertyAsync(handle, id, property.name, bytes) }) {
            is MpvResult.Fail -> return r
            is MpvResult.Ok -> r.value as MpvEvent.SetPropertyReply
        }
        return if (reply.error.isError) MpvResult.Fail(reply.error, property.name) else MpvResult.Ok(Unit)
    }

    /**
     * Every change of [property], starting with its current value, conflated to the latest.
     * Null when mpv reports the property unavailable (no file loaded, for example).
     */
    public fun <T> observe(property: MpvProperty<T>): Flow<T?> = observeNode(property.name).let { flow ->
        callbackFlow {
            val job = launch { flow.collect { node -> trySend(if (node == MpvNode.None) null else runCatching { property.decode(node) }.getOrNull()) } }
            awaitClose { job.cancel() }
        }.conflate()
    }

    /**
     * What the player is doing, kept current from the twelve properties the state is made of.
     * The flow starts observing on first access and stops when the core closes.
     */
    public val playback: StateFlow<MpvPlaybackState> by lazy {
        val idle = PlaybackStateReducer.Inputs(
            idleActive = true, coreIdle = true, pause = false, pausedForCache = false, eofReached = false,
            seeking = false, timePos = null, duration = null, path = null, cachePercent = null,
            seekable = false, speed = 1.0,
        )
        // Two combines of six: combine takes at most five flows before it needs an array.
        val part1 = combine(
            observe(MpvProperties.IdleActive),
            observe(MpvProperties.CoreIdle),
            observe(MpvProperties.Pause),
            observe(MpvProperties.PausedForCache),
            observe(MpvProperties.EofReached),
        ) { a, b, c, d, e -> listOf(a, b, c, d, e) }
        val part2 = combine(
            observe(MpvProperties.Seeking),
            observe(MpvProperties.TimePos),
            observe(MpvProperties.Duration),
            observe(MpvProperties.Path),
            observe(MpvProperties.Seekable),
        ) { a, b, c, d, e -> listOf(a, b, c, d, e) }
        val part3 = combine(
            observe(MpvProperties.CacheBufferingState),
            observe(MpvProperties.Speed),
        ) { a, b -> listOf(a, b) }
        combine(part1, part2, part3) { flags, values, rest ->
            PlaybackStateReducer.reduce(
                PlaybackStateReducer.Inputs(
                    idleActive = flags[0] as? Boolean ?: false,
                    coreIdle = flags[1] as? Boolean ?: false,
                    pause = flags[2] as? Boolean ?: false,
                    pausedForCache = flags[3] as? Boolean ?: false,
                    eofReached = flags[4] as? Boolean ?: false,
                    seeking = values[0] as? Boolean ?: false,
                    timePos = values[1] as? Double,
                    duration = values[2] as? Double,
                    path = values[3] as? String,
                    seekable = values[4] as? Boolean ?: false,
                    cachePercent = (rest[0] as? Long)?.toInt(),
                    speed = rest[1] as? Double ?: 1.0,
                ),
            )
        }.stateIn(hookScope, SharingStarted.Eagerly, PlaybackStateReducer.reduce(idle))
    }

    /** The raw form of [observe]: the node mpv sends, `None` when unavailable. A slow collector loses the oldest values, never the newest. */
    public fun observeNode(name: String, format: Int = 6): Flow<MpvNode> = callbackFlow {
        val id = replyIds.getAndIncrement()
        val r = gate.call {
            observers[id] = { node -> trySend(node) }
            MpvNative.observeProperty(handle, id, name, format)
        }
        if (r < 0) {
            observers.remove(id)
            close(MpvException(MpvError.of(r), name))
        }
        awaitClose {
            observers.remove(id)
            gate.callIfOpen { MpvNative.unobserveProperty(handle, id) }
        }
    }.buffer(64, BufferOverflow.DROP_OLDEST)

    // ---- commands ----

    public fun command(cmd: MpvCommand): MpvResult<MpvNode> {
        val bytes = NodeCodec.encode(cmd.toNode())
        val (error, node) = NodeCodec.decodeEnvelope(gate.call { MpvNative.commandNode(handle, bytes) })
        return if (error < 0) MpvResult.Fail(MpvError.of(error), cmd.toString()) else MpvResult.Ok(node)
    }

    public fun command(vararg args: String): MpvResult<Unit> = gate.call { unitResult(MpvNative.command(handle, arrayOf(*args))) }

    /**
     * Runs [cmd] and suspends until mpv replies. Cancelling the coroutine aborts the command where mpv
     * allows it. A core that shuts down first gives a [MpvResult.Fail].
     */
    public suspend fun commandAsync(cmd: MpvCommand): MpvResult<MpvNode> {
        val bytes = NodeCodec.encode(cmd.toNode())
        val reply = when (val r = await(cmd.toString(), abortable = true) { id -> MpvNative.commandNodeAsync(handle, id, bytes) }) {
            is MpvResult.Fail -> return r
            is MpvResult.Ok -> r.value as MpvEvent.CommandReply
        }
        return if (reply.error.isError) MpvResult.Fail(reply.error, cmd.toString()) else MpvResult.Ok(reply.result)
    }

    // ---- events, logs, hooks, streams ----

    public fun requestLogMessages(minLevel: MpvLogLevel): MpvResult<Unit> =
        gate.call { unitResult(MpvNative.requestLogMessages(handle, minLevel.mpvName)) }

    /** Runs [handler] every time mpv reaches the hook [name]; mpv waits until it returns. An exception from it is logged, and mpv continues. */
    public fun hook(name: String, priority: Int = 0, handler: suspend (MpvEvent.Hook) -> Unit): MpvResult<Unit> {
        val id = replyIds.getAndIncrement()
        val r = gate.call {
            hooks[id] = handler
            MpvNative.hookAdd(handle, id, name, priority)
        }
        if (r < 0) hooks.remove(id)
        return unitResult(r)
    }

    public fun addStreamProtocol(protocol: String, provider: MpvStreamProvider): MpvResult<Unit> =
        gate.call { unitResult(MpvNative.streamCbAddRo(handle, protocol, provider)) }

    /**
     * For [MPVLib]: [listener] sees every event synchronously on the event thread, in mpv's order,
     * before the flows do. An exception from it is logged.
     */
    internal fun addEventListener(listener: (MpvEvent) -> Unit) {
        listeners += listener
    }

    /** For [MPVLib]: observes [name] in [format] under reply id 0, which only the event listeners see. Returns mpv's code. */
    internal fun observeForListeners(name: String, format: Int): Int = gate.call { MpvNative.observeProperty(handle, 0, name, format) }

    /** For [MPVLib]: log lines at `terminal-default`, which follows `msg-level`, as 0.1.0 asked. */
    internal fun requestTerminalDefaultLogs(): MpvResult<Unit> =
        gate.call { unitResult(MpvNative.requestLogMessages(handle, "terminal-default")) }

    // ---- the window ----

    private var surfaceHandle: Long = 0

    /**
     * Gives mpv [surface] to render into: sets `wid`, then `force-window=yes`, then `vo` to
     * [vo]. Call [detachSurface] before the surface is destroyed.
     */
    public fun attachSurface(surface: Surface, vo: String = "gpu"): MpvResult<Unit> = gate.call {
        releaseSurface()
        surfaceHandle = MpvNative.surfaceHandle(surface)
        val r = MpvNative.setOptionNode(handle, "wid", NodeCodec.encode(MpvNode.Int64(surfaceHandle)))
        if (r < 0) {
            unitResult(r)
        } else {
            MpvNative.setOptionString(handle, "force-window", "yes")
            unitResult(MpvNative.setPropertyString(handle, "vo", vo))
        }
    }

    /** Takes the window away: `vo=null`, `force-window=no`, `wid=0`, and the reference is released. */
    public fun detachSurface() {
        gate.call {
            MpvNative.setPropertyString(handle, "vo", "null")
            MpvNative.setOptionString(handle, "force-window", "no")
            MpvNative.setOptionNode(handle, "wid", NodeCodec.encode(MpvNode.Int64(0)))
            releaseSurface()
        }
    }

    private fun releaseSurface() {
        if (surfaceHandle != 0L) {
            MpvNative.releaseSurfaceHandle(surfaceHandle)
            surfaceHandle = 0
        }
    }

    // ---- internals ----

    private fun <T> MpvResult<MpvNode>.flatDecode(property: MpvProperty<T>): MpvResult<T> = when (this) {
        is MpvResult.Fail -> this
        is MpvResult.Ok -> try {
            MpvResult.Ok(property.decode(value))
        } catch (e: IllegalArgumentException) {
            MpvResult.Fail(MpvError.PROPERTY_FORMAT, e.message)
        }
    }

    /**
     * Starts an async request and suspends for mpv's reply. A refused start fails with mpv's own code;
     * a core that shuts down or closes before it replies fails with [MpvError.GENERIC].
     */
    private suspend fun await(what: String, abortable: Boolean = false, start: (Long) -> Int): MpvResult<MpvEvent> {
        check(!isClosed) { "this Mpv is closed" }
        check(initialized) { "initialize() first" }
        val id = replyIds.getAndIncrement()
        val reply = replies.register(id) ?: return MpvResult.Fail(MpvError.GENERIC, "$what: the core has shut down")
        val r = try {
            gate.call { start(id) }
        } catch (e: IllegalStateException) {
            replies.remove(id)
            throw e
        }
        if (r < 0) {
            replies.remove(id)
            return MpvResult.Fail(MpvError.of(r), what)
        }
        return try {
            reply.await()?.let { MpvResult.Ok(it) } ?: MpvResult.Fail(MpvError.GENERIC, "$what: the core shut down before replying")
        } catch (e: CancellationException) {
            replies.remove(id)
            if (abortable) gate.callIfOpen { MpvNative.abortAsyncCommand(handle, id) }
            throw e
        }
    }

    private fun pump() {
        while (!gate.isClosed) {
            // Sleeps until mpv has an event or close() wakes it; mpv keeps a wakeup that comes early.
            val event = EventDecoder.decode(MpvNative.waitEvent(handle, -1.0)) ?: continue
            dispatch(event)
            if (event is MpvEvent.Shutdown) break
        }
        // No reply can come once this loop ends, so waiting async calls fail instead of hanging.
        replies.end()
    }

    private fun dispatch(event: MpvEvent) {
        for (listener in listeners) {
            try {
                listener(event)
            } catch (e: Exception) {
                Log.e(TAG, "an event listener failed on $event", e)
            }
        }
        when (event) {
            is MpvEvent.PropertyChange -> observers[event.replyId]?.invoke(event.value)
            is MpvEvent.CommandReply -> replies.complete(event.replyId, event)
            is MpvEvent.GetPropertyReply -> replies.complete(event.replyId, event)
            is MpvEvent.SetPropertyReply -> replies.complete(event.replyId, event)
            is MpvEvent.Hook -> hooks[event.replyId]?.let { handler ->
                hookScope.launch {
                    try {
                        handler(event)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        // A bug in the app's handler: log it, let mpv continue, and keep the app running.
                        Log.e(TAG, "hook ${event.name} failed", e)
                    } finally {
                        gate.callIfOpen { MpvNative.hookContinue(handle, event.hookId) }
                    }
                }
            }
            is MpvEvent.LogMessage -> {
                logs.tryEmit(event)
                return
            }
            else -> Unit
        }
        events.tryEmit(event)
    }

    public companion object {
        private const val TAG = "libmpvKt"

        @Volatile
        private var androidReady = false

        /** A new core. Nothing runs until [initialize]. */
        public fun create(context: Context): Mpv {
            if (!androidReady) {
                MpvNative.initAndroid(context.applicationContext)
                androidReady = true
            }
            val h = MpvNative.create()
            check(h != 0L) { "mpv_create failed" }
            return Mpv(h, ownsCore = true)
        }

        /** The libmpv client API version, major in the high 16 bits. */
        public val apiVersion: Long get() = MpvNative.clientApiVersion()
    }
}
