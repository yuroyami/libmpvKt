package io.github.yuroyami.libmpvkt

/** A value mpv writes as one of a fixed set of strings. Every enum in the catalogs implements it. */
public interface MpvChoice {
    public val mpvName: String
}

/** Converts between a Kotlin value and its node form. */
public interface MpvCodec<T> {
    public fun encode(value: T): MpvNode
    /** Throws [IllegalArgumentException] naming the expected and the actual shape. */
    public fun decode(node: MpvNode): T
}

/**
 * A key for an mpv property or option, with the type mpv gives it. Options are properties with
 * the same name (set before [Mpv.initialize] through [Mpv.setOption], after it through [Mpv.set]),
 * so one key serves both.
 */
public sealed class MpvProperty<T>(public val name: String) : MpvCodec<T> {
    protected fun mismatch(expected: String, node: MpvNode): Nothing =
        throw IllegalArgumentException("$name: expected $expected, mpv answered ${node::class.simpleName}($node)")

    public class Flag(name: String) : MpvProperty<Boolean>(name) {
        override fun encode(value: Boolean): MpvNode = MpvNode.Flag(value)
        override fun decode(node: MpvNode): Boolean = node.asBoolean() ?: mismatch("Boolean", node)
    }

    public class Int64(name: String) : MpvProperty<Long>(name) {
        override fun encode(value: Long): MpvNode = MpvNode.Int64(value)
        override fun decode(node: MpvNode): Long = node.asLong() ?: mismatch("Long", node)
    }

    public class Dbl(name: String) : MpvProperty<Double>(name) {
        override fun encode(value: Double): MpvNode = MpvNode.Dbl(value)
        override fun decode(node: MpvNode): Double = node.asDouble() ?: mismatch("Double", node)
    }

    public class Str(name: String) : MpvProperty<String>(name) {
        override fun encode(value: String): MpvNode = MpvNode.Str(value)
        override fun decode(node: MpvNode): String = node.asString() ?: mismatch("String", node)
    }

    public class Node(name: String) : MpvProperty<MpvNode>(name) {
        override fun encode(value: MpvNode): MpvNode = value
        override fun decode(node: MpvNode): MpvNode = node
    }

    public class Choice<E>(name: String, public val values: List<E>) : MpvProperty<E>(name) where E : Enum<E>, E : MpvChoice {
        override fun encode(value: E): MpvNode = MpvNode.Str(value.mpvName)
        override fun decode(node: MpvNode): E {
            val text = node.asString() ?: mismatch("one of ${values.map { it.mpvName }}", node)
            return values.firstOrNull { it.mpvName == text } ?: mismatch("one of ${values.map { it.mpvName }}", node)
        }
    }

    public class Typed<T>(name: String, private val codec: MpvCodec<T>) : MpvProperty<T>(name) {
        override fun encode(value: T): MpvNode = codec.encode(value)
        override fun decode(node: MpvNode): T = try {
            codec.decode(node)
        } catch (e: IllegalArgumentException) {
            throw IllegalArgumentException("$name: ${e.message}", e)
        }
    }

    override fun toString(): String = "MpvProperty($name)"
}
