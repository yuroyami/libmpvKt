package io.github.yuroyami.libmpvkt

import android.content.Context
import android.view.Surface
import io.github.yuroyami.libmpvkt.jni.MpvNative
import io.github.yuroyami.libmpvkt.jni.MpvNativeApi
import io.github.yuroyami.libmpvkt.stream.MpvStreamProvider
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
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
 * mpv errors are [MpvResult] values. Calling anything after [close] throws [IllegalStateException].
 */
@OptIn(MpvNativeApi::class)
public class Mpv private constructor(
    private val handle: Long,
    private val ownsCore: Boolean,
) : AutoCloseable {

    public val clientName: String = MpvNative.clientName(handle)

    private val closed = AtomicBoolean(false)

    @Volatile
    private var initialized = false

    private val replyIds = AtomicLong(1)
    private val pendingReplies = ConcurrentHashMap<Long, CompletableDeferred<MpvEvent>>()
    private val observers = ConcurrentHashMap<Long, (MpvNode) -> Unit>()
    private val hooks = ConcurrentHashMap<Long, suspend (MpvEvent.Hook) -> Unit>()
    private val hookScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /** Every event of this handle, in order. Slow collectors lose the oldest events, never the newest. */
    public val events: SharedFlow<MpvEvent>
        field = MutableSharedFlow(extraBufferCapacity = 512, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    /** Log lines, once [requestLogMessages] asked for them. */
    public val logs: SharedFlow<MpvEvent.LogMessage>
        field = MutableSharedFlow(extraBufferCapacity = 512, onBufferOverflow = BufferOverflow.DROP_OLDEST)

    private val eventThread = Thread({ pump() }, "mpv-events-$clientName").apply { isDaemon = true }

    public val isInitialized: Boolean get() = initialized
    public val isClosed: Boolean get() = closed.get()

    // ---- lifecycle ----

    /** Sets an option before [initialize]. After it, use [set]; mpv ignores most options set late. */
    public fun <T> setOption(option: MpvProperty<T>, value: T): MpvResult<Unit> {
        checkOpen()
        return unitResult(MpvNative.setOptionNode(handle, option.name, NodeCodec.encode(option.encode(value))))
    }

    public fun setOption(name: String, value: String): MpvResult<Unit> {
        checkOpen()
        return unitResult(MpvNative.setOptionString(handle, name, value))
    }

    /** Starts the core and this handle's event thread. Once. */
    public fun initialize(): MpvResult<Unit> {
        checkOpen()
        check(!initialized) { "initialize() was already called" }
        val r = MpvNative.initialize(handle)
        if (r >= 0) {
            initialized = true
            eventThread.start()
        }
        return unitResult(r)
    }

    /** A second handle on the same core with its own event thread. Closing it does not end the core. */
    public fun createClient(name: String): Mpv {
        checkOpen()
        val h = MpvNative.createClient(handle, name)
        check(h != 0L) { "mpv_create_client failed" }
        return Mpv(h, ownsCore = false).also { it.initialized = true; it.eventThread.start() }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        if (eventThread.isAlive) {
            MpvNative.wakeup(handle)
            eventThread.join(5_000)
        }
        hookScope.cancel()
        pendingReplies.values.forEach { it.cancel() }
        pendingReplies.clear()
        observers.clear()
        if (ownsCore) MpvNative.terminateDestroy(handle) else MpvNative.destroy(handle)
        releaseSurface()
    }

    // ---- properties ----

    public operator fun <T> get(property: MpvProperty<T>): MpvResult<T> = getNode(property.name).flatDecode(property)

    public operator fun <T> set(property: MpvProperty<T>, value: T): MpvResult<Unit> = setNode(property.name, property.encode(value))

    public fun getNode(name: String): MpvResult<MpvNode> {
        checkOpen()
        val (error, node) = NodeCodec.decodeEnvelope(MpvNative.getPropertyNode(handle, name))
        return if (error < 0) MpvResult.Fail(MpvError.of(error), name) else MpvResult.Ok(node)
    }

    public fun setNode(name: String, node: MpvNode): MpvResult<Unit> {
        checkOpen()
        return unitResult(MpvNative.setPropertyNode(handle, name, NodeCodec.encode(node)))
    }

    public fun getString(name: String): String? { checkOpen(); return MpvNative.getPropertyString(handle, name) }
    public fun getOsdString(name: String): String? { checkOpen(); return MpvNative.getPropertyOsdString(handle, name) }
    public fun setString(name: String, value: String): MpvResult<Unit> { checkOpen(); return unitResult(MpvNative.setPropertyString(handle, name, value)) }
    public fun delete(name: String): MpvResult<Unit> { checkOpen(); return unitResult(MpvNative.delProperty(handle, name)) }

    public suspend fun <T> getAsync(property: MpvProperty<T>): MpvResult<T> {
        val reply = await { id -> MpvNative.getPropertyAsync(handle, id, property.name) } as? MpvEvent.GetPropertyReply
            ?: return MpvResult.Fail(MpvError.GENERIC, "no reply")
        return if (reply.error.isError) MpvResult.Fail(reply.error, property.name) else MpvResult.Ok(reply.value).flatDecode(property)
    }

    public suspend fun <T> setAsync(property: MpvProperty<T>, value: T): MpvResult<Unit> {
        val bytes = NodeCodec.encode(property.encode(value))
        val reply = await { id -> MpvNative.setPropertyAsync(handle, id, property.name, bytes) } as? MpvEvent.SetPropertyReply
            ?: return MpvResult.Fail(MpvError.GENERIC, "no reply")
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

    /** The raw form of [observe]: the node mpv sends, `None` when unavailable. */
    public fun observeNode(name: String, format: Int = 6): Flow<MpvNode> = callbackFlow {
        checkOpen()
        val id = replyIds.getAndIncrement()
        observers[id] = { node -> trySend(node) }
        val r = MpvNative.observeProperty(handle, id, name, format)
        if (r < 0) {
            observers.remove(id)
            close(MpvException(MpvError.of(r), name))
        }
        awaitClose {
            observers.remove(id)
            if (!closed.get()) MpvNative.unobserveProperty(handle, id)
        }
    }

    // ---- commands ----

    public fun command(cmd: MpvCommand): MpvResult<MpvNode> {
        checkOpen()
        val (error, node) = NodeCodec.decodeEnvelope(MpvNative.commandNode(handle, NodeCodec.encode(cmd.toNode())))
        return if (error < 0) MpvResult.Fail(MpvError.of(error), cmd.toString()) else MpvResult.Ok(node)
    }

    public fun command(vararg args: String): MpvResult<Unit> {
        checkOpen()
        return unitResult(MpvNative.command(handle, arrayOf(*args)))
    }

    /** Runs [cmd] and suspends until mpv replies. Cancelling the coroutine aborts the command where mpv allows it. */
    public suspend fun commandAsync(cmd: MpvCommand): MpvResult<MpvNode> {
        val bytes = NodeCodec.encode(cmd.toNode())
        val reply = await(abortable = true) { id -> MpvNative.commandNodeAsync(handle, id, bytes) } as? MpvEvent.CommandReply
            ?: return MpvResult.Fail(MpvError.GENERIC, "no reply")
        return if (reply.error.isError) MpvResult.Fail(reply.error, cmd.toString()) else MpvResult.Ok(reply.result)
    }

    // ---- events, logs, hooks, streams ----

    public fun requestLogMessages(minLevel: MpvLogLevel): MpvResult<Unit> {
        checkOpen()
        return unitResult(MpvNative.requestLogMessages(handle, minLevel.mpvName))
    }

    /** Runs [handler] every time mpv reaches the hook [name]; mpv waits until the handler returns. */
    public fun hook(name: String, priority: Int = 0, handler: suspend (MpvEvent.Hook) -> Unit): MpvResult<Unit> {
        checkOpen()
        val id = replyIds.getAndIncrement()
        hooks[id] = handler
        val r = MpvNative.hookAdd(handle, id, name, priority)
        if (r < 0) hooks.remove(id)
        return unitResult(r)
    }

    public fun addStreamProtocol(protocol: String, provider: MpvStreamProvider): MpvResult<Unit> {
        checkOpen()
        return unitResult(MpvNative.streamCbAddRo(handle, protocol, provider))
    }

    // ---- the window ----

    private var surfaceHandle: Long = 0

    /**
     * Gives mpv [surface] to render into: sets `wid`, then `force-window=yes`, then `vo` to
     * [vo]. Call [detachSurface] before the surface is destroyed.
     */
    public fun attachSurface(surface: Surface, vo: String = "gpu"): MpvResult<Unit> {
        checkOpen()
        releaseSurface()
        surfaceHandle = MpvNative.surfaceHandle(surface)
        val r = MpvNative.setOptionNode(handle, "wid", NodeCodec.encode(MpvNode.Int64(surfaceHandle)))
        if (r < 0) return unitResult(r)
        MpvNative.setOptionString(handle, "force-window", "yes")
        return setString("vo", vo)
    }

    /** Takes the window away: `vo=null`, `force-window=no`, `wid=0`, and the reference is released. */
    public fun detachSurface() {
        checkOpen()
        setString("vo", "null")
        MpvNative.setOptionString(handle, "force-window", "no")
        MpvNative.setOptionNode(handle, "wid", NodeCodec.encode(MpvNode.Int64(0)))
        releaseSurface()
    }

    private fun releaseSurface() {
        if (surfaceHandle != 0L) {
            MpvNative.releaseSurfaceHandle(surfaceHandle)
            surfaceHandle = 0
        }
    }

    // ---- internals ----

    private fun checkOpen() = check(!closed.get()) { "this Mpv is closed" }

    private fun <T> MpvResult<MpvNode>.flatDecode(property: MpvProperty<T>): MpvResult<T> = when (this) {
        is MpvResult.Fail -> this
        is MpvResult.Ok -> try {
            MpvResult.Ok(property.decode(value))
        } catch (e: IllegalArgumentException) {
            MpvResult.Fail(MpvError.PROPERTY_FORMAT, e.message)
        }
    }

    private suspend fun await(abortable: Boolean = false, start: (Long) -> Int): MpvEvent? {
        checkOpen()
        check(initialized) { "initialize() first" }
        val id = replyIds.getAndIncrement()
        val deferred = CompletableDeferred<MpvEvent>()
        pendingReplies[id] = deferred
        val r = start(id)
        if (r < 0) {
            pendingReplies.remove(id)
            return null
        }
        return try {
            deferred.await()
        } catch (e: CancellationException) {
            pendingReplies.remove(id)
            if (abortable && !closed.get()) MpvNative.abortAsyncCommand(handle, id)
            throw e
        }
    }

    private fun pump() {
        while (!closed.get()) {
            val event = EventDecoder.decode(MpvNative.waitEvent(handle, 1.0)) ?: continue
            dispatch(event)
            if (event is MpvEvent.Shutdown) break
        }
    }

    private fun dispatch(event: MpvEvent) {
        when (event) {
            is MpvEvent.PropertyChange -> observers[event.replyId]?.invoke(event.value)
            is MpvEvent.CommandReply -> pendingReplies.remove(event.replyId)?.complete(event)
            is MpvEvent.GetPropertyReply -> pendingReplies.remove(event.replyId)?.complete(event)
            is MpvEvent.SetPropertyReply -> pendingReplies.remove(event.replyId)?.complete(event)
            is MpvEvent.Hook -> hooks[event.replyId]?.let { handler ->
                hookScope.launch {
                    try { handler(event) } finally { if (!closed.get()) MpvNative.hookContinue(handle, event.hookId) }
                }
            }
            is MpvEvent.LogMessage -> logs.tryEmit(event)
            else -> Unit
        }
        events.tryEmit(event)
    }

    public companion object {
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
