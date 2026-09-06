package io.github.yuroyami.libmpvkt.stream

import java.nio.ByteBuffer

/**
 * Supplies the bytes of a URI to mpv. Registered for one protocol name; mpv then calls [open]
 * for every URI of that protocol, on one of its own threads.
 */
public interface MpvStreamProvider {
    /** A stream for [uri], or null when it cannot be opened, which mpv reports as a loading failure. */
    public fun open(uri: String): MpvStream?
}

/**
 * One open stream. Every method is called on mpv's demuxer thread, never concurrently for the
 * same stream. Both classes are resolved by name from C, so they are kept by the consumer rules.
 */
public interface MpvStream {
    /** Whether [seek] can work. mpv treats a non-seekable stream as live. */
    public val isSeekable: Boolean

    /** Fills [into] from its position to its limit. Returns bytes written, 0 at the end of the stream, a negative number on error. */
    public fun read(into: ByteBuffer): Int

    /** Moves to an absolute [offset]. Returns the new position, or a negative number when the seek failed. */
    public fun seek(offset: Long): Long

    /** The total size in bytes, or a negative number when unknown. */
    public fun size(): Long

    public fun close()

    /** mpv asks for a blocked [read] to return early. Optional. */
    public fun cancel() {}
}
