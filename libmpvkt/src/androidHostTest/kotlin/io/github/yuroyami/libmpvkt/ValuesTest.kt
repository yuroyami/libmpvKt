package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals

class ValuesTest {
    @Test
    fun trackSelection() {
        assertEquals(MpvNode.Int64(2), TrackSelection.Codec.encode(TrackSelection.Id(2)))
        assertEquals(MpvNode.Str("no"), TrackSelection.Codec.encode(TrackSelection.No))
        assertEquals(TrackSelection.Id(2), TrackSelection.Codec.decode(MpvNode.Int64(2)))
        assertEquals(TrackSelection.Auto, TrackSelection.Codec.decode(MpvNode.Str("auto")))
        assertEquals(TrackSelection.No, TrackSelection.Codec.decode(MpvNode.Flag(false)))
    }

    @Test
    fun loopCount() {
        assertEquals(MpvNode.Str("inf"), LoopCount.Codec.encode(LoopCount.Inf))
        assertEquals(LoopCount.Times(3), LoopCount.Codec.decode(MpvNode.Int64(3)))
        assertEquals(LoopCount.No, LoopCount.Codec.decode(MpvNode.Flag(false)))
    }

    @Test
    fun trackListDecodesTheMpvShape() {
        val entry = MpvNode.Dict(
            mapOf(
                "id" to MpvNode.Int64(1), "type" to MpvNode.Str("audio"), "src-id" to MpvNode.Int64(0),
                "title" to MpvNode.Str("Commentary"), "lang" to MpvNode.Str("eng"), "default" to MpvNode.Flag(true),
                "selected" to MpvNode.Flag(false), "codec" to MpvNode.Str("aac"), "demux-channel-count" to MpvNode.Int64(2),
            ),
        )
        val track = MpvTrack.ListCodec.decode(MpvNode.Arr(listOf(entry))).single()
        assertEquals(1, track.id)
        assertEquals(TrackType.Audio, track.type)
        assertEquals("Commentary", track.title)
        assertEquals(true, track.isDefault)
        assertEquals(false, track.isForced)
        assertEquals(2, track.demuxChannelCount)
    }

    @Test
    fun stringListsAcceptBothForms() {
        assertEquals(listOf("en", "ja"), StringListCodec.decode(MpvNode.Str("en,ja")))
        assertEquals(listOf("en", "ja"), StringListCodec.decode(MpvNode.Arr(listOf(MpvNode.Str("en"), MpvNode.Str("ja")))))
        assertEquals(MpvNode.Arr(listOf(MpvNode.Str("en"))), StringListCodec.encode(listOf("en")))
    }
}
