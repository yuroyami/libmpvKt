package io.github.yuroyami.libmpvkt.stream

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import java.io.FileInputStream
import java.nio.ByteBuffer
import java.nio.channels.FileChannel

/**
 * Plays Android `content://` documents by opening them through the ContentResolver, which is the
 * one way that respects every permission grant. Register with
 * `mpv.addStreamProtocol("content", ContentResolverStreamProvider(context))`. A document served
 * through a pipe has no size and cannot seek, so mpv plays it like a live stream.
 */
public class ContentResolverStreamProvider(context: Context) : MpvStreamProvider {
    private val resolver = context.applicationContext.contentResolver

    override fun open(uri: String): MpvStream? {
        val descriptor = runCatching { resolver.openFileDescriptor(Uri.parse(uri), "r") }.getOrNull() ?: return null
        return DescriptorStream(descriptor)
    }

    internal class DescriptorStream(private val descriptor: ParcelFileDescriptor) : MpvStream {
        private val channel: FileChannel = FileInputStream(descriptor.fileDescriptor).channel
        override val isSeekable: Boolean = runCatching { channel.position(channel.position()); true }.getOrDefault(false)

        override fun read(into: ByteBuffer): Int = try {
            val n = channel.read(into)
            if (n < 0) 0 else n
        } catch (e: Exception) { -1 }

        override fun seek(offset: Long): Long = try { channel.position(offset); offset } catch (e: Exception) { -1 }
        /** From stat(): -1 for a pipe or a socket, where FileChannel.size() says 0. */
        override fun size(): Long = descriptor.statSize

        /** Closing the channel makes a blocked read return with an error; mpv closes the stream after that. */
        override fun cancel() { runCatching { channel.close() } }

        override fun close() { runCatching { channel.close() }; runCatching { descriptor.close() } }
    }
}
