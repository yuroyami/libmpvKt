package io.github.yuroyami.libmpvkt

/**
 * One mpv command, in the node form `mpv_command_node` takes: a list with the command name first,
 * or a map of named arguments with the command under `name`.
 */
public class MpvCommand internal constructor(private val node: MpvNode) {
    /** A positional command: the command name first, then its arguments. */
    public constructor(args: List<MpvNode>) : this(MpvNode.Arr(args))

    /** The positional arguments, command name first. Empty for a command with named arguments. */
    public val args: List<MpvNode> get() = (node as? MpvNode.Arr)?.values.orEmpty()

    public fun toNode(): MpvNode = node
    override fun toString(): String = "MpvCommand(${(node as? MpvNode.Arr)?.values?.joinToString(" ") ?: node})"

    public companion object {
        public fun of(vararg args: String): MpvCommand = MpvCommand(args.map { MpvNode.Str(it) })

        /** A command with named arguments. mpv fills in every argument left out, wherever it sits; nulls are left out. */
        public fun named(name: String, vararg args: Pair<String, Any?>): MpvCommand = MpvCommand(
            MpvNode.Dict(
                buildMap {
                    put("name", MpvNode.Str(name))
                    for ((key, value) in args) if (value != null) put(key, MpvNode.of(value))
                },
            ),
        )

        /** A positional command. Trailing nulls are left out; a null in the middle would shift the rest, so it throws. */
        internal fun build(vararg parts: Any?): MpvCommand {
            val present = parts.indexOfLast { it != null } + 1
            require(parts.take(present).none { it == null }) { "null in the middle of ${parts.toList()}; use buildNamed" }
            return MpvCommand(parts.take(present).map { MpvNode.of(it) })
        }

        /** Positional while nothing in the middle is missing, named otherwise. The keys are mpv's own argument names. */
        internal fun buildNamed(name: String, vararg args: Pair<String, Any?>): MpvCommand {
            val present = args.indexOfLast { it.second != null } + 1
            if (args.take(present).any { it.second == null }) return named(name, *args)
            return MpvCommand(listOf(MpvNode.Str(name)) + args.take(present).map { MpvNode.of(it.second) })
        }
    }
}
