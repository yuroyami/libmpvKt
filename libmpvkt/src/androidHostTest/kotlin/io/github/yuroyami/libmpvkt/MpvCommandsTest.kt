package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals

class MpvCommandsTest {
    private fun args(cmd: MpvCommand) = cmd.args

    @Test
    fun seek() {
        assertEquals(listOf(MpvNode.Str("seek"), MpvNode.Dbl(10.0), MpvNode.Str("relative")), args(MpvCommands.seek(10.0)))
        assertEquals(listOf(MpvNode.Str("seek"), MpvNode.Dbl(90.0), MpvNode.Str("absolute+exact")), args(MpvCommands.seek(90.0, SeekMode.Absolute, SeekPrecision.Exact)))
    }

    @Test
    fun loadFile() {
        assertEquals(listOf(MpvNode.Str("loadfile"), MpvNode.Str("a.mkv"), MpvNode.Str("replace")), args(MpvCommands.loadFile("a.mkv")))
        assertEquals(
            listOf(MpvNode.Str("loadfile"), MpvNode.Str("a.mkv"), MpvNode.Str("insert-at"), MpvNode.Int64(2), MpvNode.Dict(mapOf("start" to MpvNode.Str("10")))),
            args(MpvCommands.loadFile("a.mkv", LoadFileMode.InsertAt, index = 2, options = mapOf("start" to "10"))),
        )
    }

    @Test
    fun subAdd() {
        assertEquals(listOf(MpvNode.Str("sub-add"), MpvNode.Str("x.srt"), MpvNode.Str("cached"), MpvNode.Str("Title")), args(MpvCommands.subAdd("x.srt", SubAddMode.Cached, title = "Title")))
    }
}
