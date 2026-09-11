package io.github.yuroyami.libmpvkt.stream

import android.content.Context
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.system.ErrnoException
import android.system.Os
import android.system.OsConstants
import android.system.StructPollfd
import java.io.FileDescriptor
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
        return runCatching { DescriptorStream(descriptor) }.getOrElse { descriptor.close(); null }
    }

    internal class DescriptorStream(private val descriptor: ParcelFileDescriptor) : MpvStream {
        private val channel: FileChannel = FileInputStream(descriptor.fileDescriptor).channel
        override val isSeekable: Boolean = runCatching { channel.position(channel.position()); true }.getOrDefault(false)

        /** [cancel] writes a byte here to wake a read that waits in poll(). */
        private val wakeup: Array<FileDescriptor> = Os.pipe()
        private val wakeupLock = Any()
        private var wakeupClosed = false

        @Volatile
        private var cancelled = false

        override fun read(into: ByteBuffer): Int = try {
            if (!awaitReadable()) {
                -1
            } else {
                val n = channel.read(into)
                if (n < 0) 0 else n
            }
        } catch (e: Exception) { -1 }

        /**
         * Waits until the descriptor has data or has ended; false once [cancel] runs. Closing the channel
         * does not wake a read blocked on a pipe on Android 6 and older, so the wait happens here instead.
         */
        private fun awaitReadable(): Boolean {
            val fds = arrayOf(pollIn(descriptor.fileDescriptor), pollIn(wakeup[0]))
            while (!cancelled) {
                try {
                    Os.poll(fds, -1)
                } catch (e: ErrnoException) {
                    if (e.errno != OsConstants.EINTR) throw e
                    continue
                }
                if (fds[1].revents.toInt() != 0) return false
                if (fds[0].revents.toInt() != 0) return true
            }
            return false
        }

        override fun seek(offset: Long): Long = try { channel.position(offset); offset } catch (e: Exception) { -1 }
        /** From stat(): -1 for a pipe or a socket, where FileChannel.size() says 0. */
        override fun size(): Long = descriptor.statSize

        /**
         * Ends a waiting read with an error; mpv closes the stream after that. Closing the channel as well
         * interrupts a read already inside read(), which Android 7 and newer support.
         */
        override fun cancel() {
            cancelled = true
            synchronized(wakeupLock) { if (!wakeupClosed) runCatching { Os.write(wakeup[1], byteArrayOf(1), 0, 1) } }
            runCatching { channel.close() }
        }

        override fun close() {
            runCatching { channel.close() }
            runCatching { descriptor.close() }
            synchronized(wakeupLock) {
                wakeupClosed = true
                wakeup.forEach { runCatching { Os.close(it) } }
            }
        }

        private fun pollIn(fd: FileDescriptor) = StructPollfd().apply {
            this.fd = fd
            events = OsConstants.POLLIN.toShort()
        }
    }
}
