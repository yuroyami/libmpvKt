package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class NodeCodecTest {

    private fun roundTrip(node: MpvNode) {
        assertEquals(node, NodeCodec.decode(NodeCodec.encode(node)))
    }

    @Test
    fun everyScalarRoundTrips() {
        roundTrip(MpvNode.None)
        roundTrip(MpvNode.Str(""))
        roundTrip(MpvNode.Str("Grüße, 日本, 🎬"))
        roundTrip(MpvNode.Flag(true))
        roundTrip(MpvNode.Flag(false))
        roundTrip(MpvNode.Int64(Long.MIN_VALUE))
        roundTrip(MpvNode.Int64(Long.MAX_VALUE))
        roundTrip(MpvNode.Dbl(-0.0))
        roundTrip(MpvNode.Dbl(Double.NaN))
        roundTrip(MpvNode.Dbl(123.456))
        roundTrip(MpvNode.Bytes(byteArrayOf(0, 1, 2, -1, 127, -128)))
    }

    @Test
    fun containersRoundTripIncludingEmptyAndNested() {
        roundTrip(MpvNode.Arr(emptyList()))
        roundTrip(MpvNode.Dict(emptyMap()))
        roundTrip(
            MpvNode.Dict(
                mapOf(
                    "w" to MpvNode.Int64(1920),
                    "list" to MpvNode.Arr(listOf(MpvNode.Str("a"), MpvNode.Dict(mapOf("k" to MpvNode.Flag(true))))),
                    "none" to MpvNode.None,
                ),
            ),
        )
    }

    @Test
    fun theWireBytesAreTheDocumentedFormat() {
        // Tag 4 (INT64) then 1 as little-endian i64.
        assertEquals(listOf(4, 1, 0, 0, 0, 0, 0, 0, 0), NodeCodec.encode(MpvNode.Int64(1)).map { it.toInt() and 0xff })
        // Tag 1 (STRING), u32 length 2, "hi".
        assertEquals(listOf(1, 2, 0, 0, 0, 'h'.code, 'i'.code), NodeCodec.encode(MpvNode.Str("hi")).map { it.toInt() and 0xff })
    }

    @Test
    fun envelopesDecode() {
        val result = byteArrayOf(-4, -1, -1, -1) + NodeCodec.encode(MpvNode.None)
        assertEquals(NodeCodec.ResultEnvelope(-4, MpvNode.None), NodeCodec.decodeEnvelope(result))
        val event = byteArrayOf(22, 0, 0, 0) + byteArrayOf(7, 0, 0, 0, 0, 0, 0, 0) + byteArrayOf(0, 0, 0, 0) +
            NodeCodec.encode(MpvNode.Dict(mapOf("name" to MpvNode.Str("pause"), "data" to MpvNode.Flag(true))))
        val decoded = NodeCodec.decodeEvent(event)
        assertEquals(22, decoded.eventId)
        assertEquals(7L, decoded.replyId)
        assertEquals(0, decoded.error)
        assertEquals(MpvNode.Str("pause"), (decoded.node as MpvNode.Dict).values["name"])
    }

    @Test
    fun aTruncatedBufferFailsCleanly() {
        val bytes = NodeCodec.encode(MpvNode.Str("hello")).copyOf(4)
        assertFailsWith<IllegalArgumentException> { NodeCodec.decode(bytes) }
    }

    /** A length the buffer cannot hold fails before anything that size is allocated. */
    @Test
    fun aHugeCountFailsWithoutAllocating() {
        val count = byteArrayOf(-1, -1, -1, 0x7f) // 2147483647, little-endian
        assertFailsWith<IllegalArgumentException> { NodeCodec.decode(byteArrayOf(7) + count) }
        assertFailsWith<IllegalArgumentException> { NodeCodec.decode(byteArrayOf(8) + count) }
        assertFailsWith<IllegalArgumentException> { NodeCodec.decode(byteArrayOf(9) + count) }
    }

    @Test
    fun nestingIsBounded() {
        val deep = (1..100).fold<Int, MpvNode>(MpvNode.None) { inner, _ -> MpvNode.Arr(listOf(inner)) }
        assertFailsWith<IllegalArgumentException> { NodeCodec.decode(NodeCodec.encode(deep)) }
        val shallow = (1..10).fold<Int, MpvNode>(MpvNode.None) { inner, _ -> MpvNode.Arr(listOf(inner)) }
        roundTrip(shallow)
    }
}
