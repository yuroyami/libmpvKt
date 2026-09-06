package io.github.yuroyami.libmpvkt

/**
 * The values libmpv passes and returns: the `mpv_node` tree, as Kotlin data.
 *
 * Readers such as [asLong] answer null on a shape mismatch, with the leniency mpv itself has:
 * an [Int64] reads as a double, a [Dbl] with no fraction reads as a long, a [Flag] reads from an
 * integer 0 or 1 and from the strings `yes` and `no`.
 */
public sealed interface MpvNode {
    public data object None : MpvNode
    public data class Str(val value: String) : MpvNode
    public data class Flag(val value: Boolean) : MpvNode
    public data class Int64(val value: Long) : MpvNode
    public data class Dbl(val value: Double) : MpvNode
    public data class Arr(val values: List<MpvNode>) : MpvNode
    public data class Dict(val values: Map<String, MpvNode>) : MpvNode

    /** A byte array, compared by content. */
    public class Bytes(public val value: ByteArray) : MpvNode {
        override fun equals(other: Any?): Boolean = other is Bytes && value.contentEquals(other.value)
        override fun hashCode(): Int = value.contentHashCode()
        override fun toString(): String = "Bytes(${value.size} bytes)"
    }

    public companion object {
        /** A node from a Kotlin value: null, String, Boolean, Int, Long, Float, Double, ByteArray, List, Map with String keys. */
        public fun of(value: Any?): MpvNode = when (value) {
            null -> None
            is MpvNode -> value
            is String -> Str(value)
            is Boolean -> Flag(value)
            is Int -> Int64(value.toLong())
            is Long -> Int64(value)
            is Float -> Dbl(value.toDouble())
            is Double -> Dbl(value)
            is ByteArray -> Bytes(value)
            is List<*> -> Arr(value.map(::of))
            is Map<*, *> -> Dict(value.entries.associate { (k, v) -> k.toString() to of(v) })
            else -> throw IllegalArgumentException("no mpv node for ${value::class.simpleName}")
        }
    }
}

public fun MpvNode.asString(): String? = when (this) {
    is MpvNode.Str -> value
    is MpvNode.Int64 -> value.toString()
    is MpvNode.Dbl -> value.toString()
    is MpvNode.Flag -> if (value) "yes" else "no"
    else -> null
}

public fun MpvNode.asLong(): Long? = when (this) {
    is MpvNode.Int64 -> value
    is MpvNode.Dbl -> if (value % 1.0 == 0.0) value.toLong() else null
    is MpvNode.Flag -> if (value) 1L else 0L
    else -> null
}

public fun MpvNode.asDouble(): Double? = when (this) {
    is MpvNode.Dbl -> value
    is MpvNode.Int64 -> value.toDouble()
    else -> null
}

public fun MpvNode.asBoolean(): Boolean? = when (this) {
    is MpvNode.Flag -> value
    is MpvNode.Int64 -> when (value) {
        0L -> false
        1L -> true
        else -> null
    }
    is MpvNode.Str -> when (value) {
        "yes", "true" -> true
        "no", "false" -> false
        else -> null
    }
    else -> null
}

public fun MpvNode.asList(): List<MpvNode>? = (this as? MpvNode.Arr)?.values

public fun MpvNode.asMap(): Map<String, MpvNode>? = (this as? MpvNode.Dict)?.values

public fun MpvNode.asBytes(): ByteArray? = (this as? MpvNode.Bytes)?.value
