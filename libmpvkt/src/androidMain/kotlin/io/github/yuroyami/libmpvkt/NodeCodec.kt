package io.github.yuroyami.libmpvkt

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The byte form nodes take across JNI. Tags are `mpv_format` values, integers are little-endian;
 * the C++ side (`node_codec.cpp`) implements the same table. One buffer per call, no reflection.
 */
public object NodeCodec {
    private const val NONE = 0
    private const val STRING = 1
    private const val FLAG = 3
    private const val INT64 = 4
    private const val DOUBLE = 5
    private const val ARRAY = 7
    private const val MAP = 8
    private const val BYTES = 9

    /** An error code and a node, as `commandNode` and `getPropertyNode` return them. */
    public data class ResultEnvelope(val error: Int, val node: MpvNode)

    /** What `waitEvent` returns: the event id, the reply userdata, the error, and the event's node. */
    public data class EventEnvelope(val eventId: Int, val replyId: Long, val error: Int, val node: MpvNode)

    public fun encode(node: MpvNode): ByteArray {
        val out = Writer()
        out.node(node)
        return out.toByteArray()
    }

    public fun decode(bytes: ByteArray): MpvNode = Reader(bytes).node()

    public fun decodeEnvelope(bytes: ByteArray): ResultEnvelope {
        val r = Reader(bytes)
        return ResultEnvelope(r.i32(), r.node())
    }

    public fun decodeEvent(bytes: ByteArray): EventEnvelope {
        val r = Reader(bytes)
        return EventEnvelope(r.i32(), r.i64(), r.i32(), r.node())
    }

    private class Writer {
        private var buf = ByteBuffer.allocate(256).order(ByteOrder.LITTLE_ENDIAN)

        private fun ensure(n: Int) {
            if (buf.remaining() >= n) return
            val bigger = ByteBuffer.allocate(maxOf(buf.capacity() * 2, buf.position() + n)).order(ByteOrder.LITTLE_ENDIAN)
            buf.flip()
            bigger.put(buf)
            buf = bigger
        }

        private fun u8(v: Int) {
            ensure(1)
            buf.put(v.toByte())
        }

        private fun u32(v: Int) {
            ensure(4)
            buf.putInt(v)
        }

        private fun i64(v: Long) {
            ensure(8)
            buf.putLong(v)
        }

        private fun str(s: String) {
            val b = s.toByteArray(Charsets.UTF_8)
            u32(b.size)
            ensure(b.size)
            buf.put(b)
        }

        fun node(n: MpvNode) {
            when (n) {
                MpvNode.None -> u8(NONE)
                is MpvNode.Str -> { u8(STRING); str(n.value) }
                is MpvNode.Flag -> { u8(FLAG); u8(if (n.value) 1 else 0) }
                is MpvNode.Int64 -> { u8(INT64); i64(n.value) }
                is MpvNode.Dbl -> { u8(DOUBLE); i64(java.lang.Double.doubleToRawLongBits(n.value)) }
                is MpvNode.Arr -> { u8(ARRAY); u32(n.values.size); n.values.forEach { node(it) } }
                is MpvNode.Dict -> { u8(MAP); u32(n.values.size); n.values.forEach { (k, v) -> str(k); node(v) } }
                is MpvNode.Bytes -> { u8(BYTES); u32(n.value.size); ensure(n.value.size); buf.put(n.value) }
            }
        }

        fun toByteArray(): ByteArray = ByteArray(buf.position()).also { buf.flip(); buf.get(it) }
    }

    private class Reader(bytes: ByteArray) {
        private val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)

        private fun need(n: Int) {
            require(buf.remaining() >= n) { "truncated node buffer: needed $n bytes at ${buf.position()}, ${buf.remaining()} left" }
        }

        fun u8(): Int {
            need(1)
            return buf.get().toInt() and 0xff
        }

        fun i32(): Int {
            need(4)
            return buf.getInt()
        }

        fun i64(): Long {
            need(8)
            return buf.getLong()
        }

        private fun u32(): Int {
            val v = i32()
            require(v >= 0) { "negative length" }
            return v
        }

        private fun str(): String {
            val n = u32()
            need(n)
            val b = ByteArray(n)
            buf.get(b)
            return String(b, Charsets.UTF_8)
        }

        fun node(): MpvNode = when (val tag = u8()) {
            NONE -> MpvNode.None
            STRING -> MpvNode.Str(str())
            FLAG -> MpvNode.Flag(u8() != 0)
            INT64 -> MpvNode.Int64(i64())
            DOUBLE -> MpvNode.Dbl(java.lang.Double.longBitsToDouble(i64()))
            ARRAY -> MpvNode.Arr(List(u32()) { node() })
            MAP -> MpvNode.Dict(LinkedHashMap<String, MpvNode>().also { m -> repeat(u32()) { m[str()] = node() } })
            BYTES -> MpvNode.Bytes(ByteArray(u32()).also { need(it.size); buf.get(it) })
            else -> throw IllegalArgumentException("unknown node tag $tag at ${buf.position() - 1}")
        }
    }
}
