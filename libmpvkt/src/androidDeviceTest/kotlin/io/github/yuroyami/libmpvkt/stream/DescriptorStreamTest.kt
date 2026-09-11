package io.github.yuroyami.libmpvkt.stream

import android.os.ParcelFileDescriptor
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.ByteBuffer
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith

/** A document provider's pipe, as ContentResolverStreamProvider sees it. */
@RunWith(AndroidJUnit4::class)
class DescriptorStreamTest {

    @Test
    fun aPipeHasNoKnownSize() {
        val (readEnd, writeEnd) = ParcelFileDescriptor.createPipe()
        val stream = ContentResolverStreamProvider.DescriptorStream(readEnd)
        try {
            assertEquals(-1L, stream.size())
            assertFalse(stream.isSeekable)
        } finally {
            stream.close()
            writeEnd.close()
        }
    }

    @Test
    fun cancelEndsABlockedRead() {
        val (readEnd, writeEnd) = ParcelFileDescriptor.createPipe()
        val stream = ContentResolverStreamProvider.DescriptorStream(readEnd)
        try {
            val result = AtomicInteger(Int.MIN_VALUE)
            val reader = thread { result.set(stream.read(ByteBuffer.allocate(16))) }
            Thread.sleep(200) // nothing was written, so the read blocks
            stream.cancel()
            reader.join(5_000)
            assertFalse(reader.isAlive, "cancel did not end the blocked read")
            assertTrue(result.get() < 0, "a cancelled read reports an error, not ${result.get()}")
        } finally {
            stream.close()
            writeEnd.close()
        }
    }
}
