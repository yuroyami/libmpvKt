package io.github.yuroyami.libmpvkt

/** One mpv command as its argument list: the command name first, then its arguments, as nodes. */
public class MpvCommand(public val args: List<MpvNode>) {
    public fun toNode(): MpvNode = MpvNode.Arr(args)
    override fun toString(): String = "MpvCommand(${args.joinToString(" ")})"

    public companion object {
        public fun of(vararg args: String): MpvCommand = MpvCommand(args.map { MpvNode.Str(it) })
        /** Builds an argument list, skipping nulls so optional trailing arguments can be left out. */
        internal fun build(vararg parts: Any?): MpvCommand = MpvCommand(parts.filterNotNull().map { MpvNode.of(it) })
    }
}
