package io.github.yuroyami.libmpvkt

/** Turns a `waitEvent` envelope into an [MpvEvent]. Keys are the ones `mpv_event_to_node` writes. */
internal object EventDecoder {
    fun decode(envelope: ByteArray): MpvEvent? {
        val e = NodeCodec.decodeEvent(envelope)
        val map = e.node.asMap().orEmpty()
        fun str(key: String) = map[key]?.asString()
        fun long(key: String) = map[key]?.asLong() ?: 0L
        val error = MpvError.of(e.error)
        return when (e.eventId) {
            0 -> null
            1 -> MpvEvent.Shutdown
            2 -> MpvEvent.LogMessage(str("prefix").orEmpty(), MpvLogLevel.entries.firstOrNull { it.mpvName == str("level") } ?: MpvLogLevel.None, str("text").orEmpty())
            3 -> MpvEvent.GetPropertyReply(e.replyId, error, str("name").orEmpty(), map["data"] ?: MpvNode.None)
            4 -> MpvEvent.SetPropertyReply(e.replyId, error)
            5 -> MpvEvent.CommandReply(e.replyId, error, map["result"] ?: MpvNode.None)
            6 -> MpvEvent.StartFile(long("playlist_entry_id"))
            7 -> MpvEvent.EndFile(
                EndFileReason.entries.firstOrNull { it.mpvName == str("reason") } ?: EndFileReason.Unknown,
                str("file_error"), long("playlist_entry_id"), long("playlist_insert_id"), long("playlist_insert_num_entries").toInt(),
            )
            8 -> MpvEvent.FileLoaded
            16 -> MpvEvent.ClientMessage(map["args"]?.asList().orEmpty().mapNotNull { it.asString() })
            17 -> MpvEvent.VideoReconfig
            18 -> MpvEvent.AudioReconfig
            20 -> MpvEvent.Seek
            21 -> MpvEvent.PlaybackRestart
            22 -> MpvEvent.PropertyChange(e.replyId, str("name").orEmpty(), map["data"] ?: MpvNode.None)
            24 -> MpvEvent.QueueOverflow
            25 -> MpvEvent.Hook(e.replyId, str("name").orEmpty(), long("hook_id"))
            else -> MpvEvent.Unknown(e.eventId, e.node)
        }
    }
}
