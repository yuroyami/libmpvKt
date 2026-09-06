package io.github.yuroyami.libmpvkt.jni

import android.content.Context
import android.view.Surface
import io.github.yuroyami.libmpvkt.stream.MpvStreamProvider

/**
 * The libmpv client API, one `external` function per call, every handle a `Long`.
 *
 * Nodes cross as byte arrays in the format `node_codec.cpp` and `NodeCodec.kt` share. Functions
 * returning `Int` return the `mpv_error` code. Nothing here validates a handle: a wrong or closed
 * handle is undefined behaviour in libmpv, which is why `Mpv` owns every handle and this object
 * is opt-in.
 */
@MpvNativeApi
public object MpvNative {
    init {
        System.loadLibrary("mpv")
        System.loadLibrary("mpvkt_jni")
    }

    /** Hands the JVM and the application context to FFmpeg (MediaCodec needs both). Once per process; later calls are ignored. */
    public external fun initAndroid(appContext: Context)

    public external fun clientApiVersion(): Long
    public external fun create(): Long
    public external fun createClient(handle: Long, name: String?): Long
    public external fun createWeakClient(handle: Long, name: String?): Long
    public external fun clientName(handle: Long): String
    public external fun clientId(handle: Long): Long
    public external fun initialize(handle: Long): Int
    public external fun destroy(handle: Long)
    public external fun terminateDestroy(handle: Long)
    public external fun loadConfigFile(handle: Long, path: String): Int
    public external fun timeUs(handle: Long): Long

    public external fun setOptionString(handle: Long, name: String, value: String): Int
    public external fun setOptionNode(handle: Long, name: String, node: ByteArray): Int

    public external fun command(handle: Long, args: Array<String>): Int
    /** Returns a result envelope: `i32` error, then the result node. */
    public external fun commandNode(handle: Long, args: ByteArray): ByteArray
    public external fun commandString(handle: Long, command: String): Int
    public external fun commandAsync(handle: Long, replyId: Long, args: Array<String>): Int
    public external fun commandNodeAsync(handle: Long, replyId: Long, args: ByteArray): Int
    public external fun abortAsyncCommand(handle: Long, replyId: Long)

    /** Returns a result envelope: `i32` error, then the property as a node. */
    public external fun getPropertyNode(handle: Long, name: String): ByteArray
    public external fun getPropertyString(handle: Long, name: String): String?
    public external fun getPropertyOsdString(handle: Long, name: String): String?
    public external fun setPropertyNode(handle: Long, name: String, node: ByteArray): Int
    public external fun setPropertyString(handle: Long, name: String, value: String): Int
    public external fun getPropertyAsync(handle: Long, replyId: Long, name: String): Int
    public external fun setPropertyAsync(handle: Long, replyId: Long, name: String, node: ByteArray): Int
    public external fun delProperty(handle: Long, name: String): Int
    public external fun observeProperty(handle: Long, replyId: Long, name: String, format: Int): Int
    public external fun unobserveProperty(handle: Long, replyId: Long): Int

    public external fun requestEvent(handle: Long, eventId: Int, enable: Boolean): Int
    public external fun requestLogMessages(handle: Long, minLevel: String): Int
    /** Blocks up to [timeoutSeconds]. Returns an event envelope: `i32` id, `i64` reply id, `i32` error, then the event node. */
    public external fun waitEvent(handle: Long, timeoutSeconds: Double): ByteArray
    public external fun wakeup(handle: Long)

    public external fun hookAdd(handle: Long, replyId: Long, name: String, priority: Int): Int
    public external fun hookContinue(handle: Long, hookId: Long): Int

    public external fun errorString(code: Int): String
    public external fun eventName(eventId: Int): String

    /** Registers [provider] for `protocol://` URIs on this core. The provider is held for the life of the core. */
    public external fun streamCbAddRo(handle: Long, protocol: String, provider: MpvStreamProvider): Int

    /** A global reference to [surface], as the `int64` mpv's `wid` option takes. Release it with [releaseSurfaceHandle]. */
    public external fun surfaceHandle(surface: Surface): Long
    public external fun releaseSurfaceHandle(surfaceHandle: Long)
}
