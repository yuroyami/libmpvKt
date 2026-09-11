package io.github.yuroyami.libmpvkt.jni

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import org.junit.runner.RunWith

/** The raw binding on a real core: every call family once, plus a node round trip through the C++ codec. */
@OptIn(MpvNativeApi::class)
@RunWith(AndroidJUnit4::class)
class MpvNativeTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun start(): Long {
        MpvNative.initAndroid(context)
        val h = MpvNative.create()
        assertNotEquals(0L, h)
        assertEquals(0, MpvNative.setOptionString(h, "vo", "null"))
        assertEquals(0, MpvNative.setOptionString(h, "ao", "null"))
        assertEquals(0, MpvNative.setOptionString(h, "idle", "yes"))
        assertEquals(0, MpvNative.initialize(h))
        return h
    }

    /** Tag 1 STRING with a u32 length. */
    private fun str(s: String): ByteArray {
        val b = s.toByteArray()
        return ByteBuffer.allocate(5 + b.size).order(ByteOrder.LITTLE_ENDIAN).put(1).putInt(b.size).put(b).array()
    }

    /** Tag 7 ARRAY of the given encoded nodes. */
    private fun arr(vararg nodes: ByteArray): ByteArray {
        val body = nodes.fold(ByteArray(0)) { acc, n -> acc + n }
        return ByteBuffer.allocate(5 + body.size).order(ByteOrder.LITTLE_ENDIAN).put(7).putInt(nodes.size).put(body).array()
    }

    /** Tag 4 INT64, little-endian. */
    private fun int64(v: Long): ByteArray = ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN).put(4).putLong(v).array()

    /** Tag 5 DOUBLE, as its IEEE bits. */
    private fun double(v: Double): ByteArray =
        ByteBuffer.allocate(9).order(ByteOrder.LITTLE_ENDIAN).put(5).putLong(java.lang.Double.doubleToRawLongBits(v)).array()

    /** Tag 9 BYTE_ARRAY with a u32 length. */
    private fun bytes(b: ByteArray): ByteArray = ByteBuffer.allocate(5 + b.size).order(ByteOrder.LITTLE_ENDIAN).put(9).putInt(b.size).put(b).array()

    /** Tag 8 MAP: a u32 count, then each key as a u32 length and UTF-8 bytes, followed by its value node. */
    private fun map(vararg entries: Pair<String, ByteArray>): ByteArray {
        val body = entries.fold(ByteArray(0)) { acc, (key, value) ->
            val k = key.toByteArray()
            acc + ByteBuffer.allocate(4 + k.size).order(ByteOrder.LITTLE_ENDIAN).putInt(k.size).put(k).array() + value
        }
        return ByteBuffer.allocate(5 + body.size).order(ByteOrder.LITTLE_ENDIAN).put(8).putInt(entries.size).put(body).array()
    }

    /** The error and the payload start of a result envelope. */
    private fun envelope(bytes: ByteArray): Pair<Int, ByteArray> {
        val error = ByteBuffer.wrap(bytes, 0, 4).order(ByteOrder.LITTLE_ENDIAN).int
        return error to bytes.copyOfRange(4, bytes.size)
    }

    @Test
    fun versionsAndClientsAndNodes() {
        val h = start()
        try {
            assertTrue(MpvNative.getPropertyString(h, "mpv-version")!!.startsWith("mpv "))
            assertEquals(2L shl 16, MpvNative.clientApiVersion() and 0xffff0000L, "client API major 2")
            val client = MpvNative.createClient(h, "second")
            assertNotEquals(0L, client)
            assertEquals("second", MpvNative.clientName(client))
            MpvNative.destroy(client)

            // A string round trip through user-data: Kotlin bytes in, C++ decodes, mpv stores, C++ encodes, Kotlin bytes out.
            assertEquals(0, MpvNative.setPropertyNode(h, "user-data/libmpvkt", str("round trip ✓")))
            val (error, payload) = envelope(MpvNative.getPropertyNode(h, "user-data/libmpvkt"))
            assertEquals(0, error)
            assertTrue(payload.contentEquals(str("round trip ✓")))

            // A command that returns a value.
            val (cmdError, cmdPayload) = envelope(MpvNative.commandNode(h, arr(str("expand-text"), str("\${mpv-version}"))))
            assertEquals(0, cmdError)
            assertEquals(1, cmdPayload[0].toInt(), "a STRING node")
        } finally {
            MpvNative.terminateDestroy(h)
        }
    }

    /** Every node kind crosses the C++ codec both ways: numbers, a map, a byte array and an array, stored by mpv and read back. */
    @Test
    fun numbersMapsAndBytesRoundTrip() {
        val h = start()
        try {
            val node = map(
                "count" to int64(-7),
                "ratio" to double(2.5),
                "raw" to bytes(byteArrayOf(0, 1, 2, -1)),
                "names" to arr(str("a"), str("b")),
            )
            assertEquals(0, MpvNative.setPropertyNode(h, "user-data/libmpvkt-kinds", node))
            val (error, payload) = envelope(MpvNative.getPropertyNode(h, "user-data/libmpvkt-kinds"))
            assertEquals(0, error)
            assertTrue(payload.contentEquals(node), "the node came back changed")
        } finally {
            MpvNative.terminateDestroy(h)
        }
    }

    /**
     * mpv speaks standard UTF-8. 🎬 must arrive as one 4-byte sequence, and bytes that are not
     * UTF-8 (an old Latin-1 tag, say) must come back as U+FFFD, not abort a debuggable app.
     */
    @Test
    fun stringsCrossAsStandardUtf8() {
        val h = start()
        try {
            val text = "ü 🎬"
            assertEquals(0, MpvNative.setPropertyString(h, "user-data/libmpvkt-set", text))
            val (error, payload) = envelope(MpvNative.getPropertyNode(h, "user-data/libmpvkt-set"))
            assertEquals(0, error)
            assertTrue(payload.contentEquals(str(text)), "mpv stored ${payload.joinToString(" ") { "%02X".format(it) }}")

            assertEquals(0, MpvNative.command(h, arrayOf("set", "user-data/libmpvkt-command", text)))
            val (_, viaCommand) = envelope(MpvNative.getPropertyNode(h, "user-data/libmpvkt-command"))
            assertTrue(viaCommand.contentEquals(str(text)), "the string command path stored something else")

            // A STRING node holding Latin-1 "é" then "x". user-data reads back as JSON, hence the quotes.
            val latin1 = byteArrayOf(1, 2, 0, 0, 0, 0xE9.toByte(), 'x'.code.toByte())
            assertEquals(0, MpvNative.setPropertyNode(h, "user-data/libmpvkt-latin1", latin1))
            assertEquals("\"\uFFFDx\"", MpvNative.getPropertyString(h, "user-data/libmpvkt-latin1"))
        } finally {
            MpvNative.terminateDestroy(h)
        }
    }

    @Test
    fun observeAsyncHookAndLogArriveThroughWaitEvent() {
        val h = start()
        try {
            assertEquals(0, MpvNative.requestLogMessages(h, "v"))
            assertEquals(0, MpvNative.observeProperty(h, 42L, "pause", 3))
            assertEquals(0, MpvNative.hookAdd(h, 77L, "on_load", 0))
            assertEquals(0, MpvNative.commandNodeAsync(h, 99L, arr(str("expand-text"), str("x"))))
            assertEquals(0, MpvNative.setPropertyString(h, "pause", "yes"))

            val seen = mutableSetOf<String>()
            val deadline = System.currentTimeMillis() + 10_000
            while (System.currentTimeMillis() < deadline && !seen.containsAll(listOf("log", "observe", "reply"))) {
                val raw = MpvNative.waitEvent(h, 0.5)
                val buf = ByteBuffer.wrap(raw).order(ByteOrder.LITTLE_ENDIAN)
                val id = buf.int
                val reply = buf.long
                when {
                    id == 2 -> seen += "log"
                    id == 22 && reply == 42L -> seen += "observe"
                    id == 5 && reply == 99L -> seen += "reply"
                }
            }
            assertEquals(setOf("log", "observe", "reply"), seen)
            // mpv_unobserve_property answers with how many it removed, and one was observed.
            assertEquals(1, MpvNative.unobserveProperty(h, 42L))
        } finally {
            MpvNative.terminateDestroy(h)
        }
    }
}
