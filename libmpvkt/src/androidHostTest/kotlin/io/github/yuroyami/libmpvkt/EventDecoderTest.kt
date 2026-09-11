package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class EventDecoderTest {
    private fun envelope(id: Int, replyId: Long = 0, error: Int = 0, node: MpvNode = MpvNode.None): ByteArray {
        val head = java.nio.ByteBuffer.allocate(16).order(java.nio.ByteOrder.LITTLE_ENDIAN).putInt(id).putLong(replyId).putInt(error).array()
        return head + NodeCodec.encode(node)
    }

    @Test
    fun everyEventKindDecodes() {
        assertNull(EventDecoder.decode(envelope(0)))
        assertEquals(MpvEvent.Shutdown, EventDecoder.decode(envelope(1)))
        assertEquals(
            MpvEvent.LogMessage("cplayer", MpvLogLevel.Verbose, "hello\n"),
            EventDecoder.decode(envelope(2, node = MpvNode.Dict(mapOf("prefix" to MpvNode.Str("cplayer"), "level" to MpvNode.Str("v"), "text" to MpvNode.Str("hello\n"))))),
        )
        assertEquals(
            MpvEvent.PropertyChange(42, "pause", MpvNode.Flag(true)),
            EventDecoder.decode(envelope(22, replyId = 42, node = MpvNode.Dict(mapOf("name" to MpvNode.Str("pause"), "data" to MpvNode.Flag(true))))),
        )
        assertEquals(
            MpvEvent.EndFile(EndFileReason.Error, "Failed to recognize file format.", 3, 0, 0),
            EventDecoder.decode(envelope(7, node = MpvNode.Dict(mapOf("reason" to MpvNode.Str("error"), "file_error" to MpvNode.Str("Failed to recognize file format."), "playlist_entry_id" to MpvNode.Int64(3), "playlist_insert_id" to MpvNode.Int64(0), "playlist_insert_num_entries" to MpvNode.Int64(0))))),
        )
        assertEquals(MpvEvent.CommandReply(9, MpvError.SUCCESS, MpvNode.Str("x")), EventDecoder.decode(envelope(5, replyId = 9, node = MpvNode.Dict(mapOf("result" to MpvNode.Str("x"))))))
        assertEquals(MpvEvent.CommandReply(9, MpvError.COMMAND, MpvNode.None), EventDecoder.decode(envelope(5, replyId = 9, error = -12)))
        assertEquals(MpvEvent.Hook(5, "on_load", 2), EventDecoder.decode(envelope(25, replyId = 5, node = MpvNode.Dict(mapOf("name" to MpvNode.Str("on_load"), "hook_id" to MpvNode.Int64(2))))))
        assertEquals(MpvEvent.ClientMessage(listOf("a", "b")), EventDecoder.decode(envelope(16, node = MpvNode.Dict(mapOf("args" to MpvNode.Arr(listOf(MpvNode.Str("a"), MpvNode.Str("b"))))))))
        assertEquals(MpvEvent.Unknown(14, MpvNode.None), EventDecoder.decode(envelope(14)))
    }

    /** The other nine ids, with the keys mpv_event_to_node writes: an `event` name on every event, `id` and `error` on replies. */
    @Test
    fun theRemainingEventIdsDecode() {
        fun named(event: String, vararg extra: Pair<String, MpvNode>) = MpvNode.Dict(mapOf("event" to MpvNode.Str(event)) + extra)
        // A get-property reply comes from the binding's own encoder, which writes the name and the data.
        assertEquals(
            MpvEvent.GetPropertyReply(11, MpvError.SUCCESS, "volume", MpvNode.Dbl(100.0)),
            EventDecoder.decode(envelope(3, replyId = 11, node = MpvNode.Dict(mapOf("name" to MpvNode.Str("volume"), "data" to MpvNode.Dbl(100.0))))),
        )
        assertEquals(
            MpvEvent.SetPropertyReply(12, MpvError.PROPERTY_FORMAT),
            EventDecoder.decode(envelope(4, replyId = 12, error = -9, node = named("set-property-reply", "id" to MpvNode.Int64(12), "error" to MpvNode.Str("unsupported format for accessing property")))),
        )
        assertEquals(MpvEvent.StartFile(4), EventDecoder.decode(envelope(6, node = named("start-file", "playlist_entry_id" to MpvNode.Int64(4)))))
        assertEquals(MpvEvent.FileLoaded, EventDecoder.decode(envelope(8, node = named("file-loaded"))))
        assertEquals(MpvEvent.VideoReconfig, EventDecoder.decode(envelope(17, node = named("video-reconfig"))))
        assertEquals(MpvEvent.AudioReconfig, EventDecoder.decode(envelope(18, node = named("audio-reconfig"))))
        assertEquals(MpvEvent.Seek, EventDecoder.decode(envelope(20, node = named("seek"))))
        assertEquals(MpvEvent.PlaybackRestart, EventDecoder.decode(envelope(21, node = named("playback-restart"))))
        assertEquals(MpvEvent.QueueOverflow, EventDecoder.decode(envelope(24, node = named("event-queue-overflow"))))
    }
}
