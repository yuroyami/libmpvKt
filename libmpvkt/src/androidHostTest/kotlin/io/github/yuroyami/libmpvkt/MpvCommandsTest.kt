package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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

    /** A command whose middle argument is left out goes by name, so the next argument keeps its meaning. */
    @Test
    fun aMissingMiddleArgumentSwitchesToNamedArguments() {
        assertEquals(
            MpvNode.Dict(mapOf("name" to MpvNode.Str("sub-add"), "url" to MpvNode.Str("x.srt"), "flags" to MpvNode.Str("select"), "lang" to MpvNode.Str("en"))),
            MpvCommands.subAdd("x.srt", lang = "en").toNode(),
        )
        assertEquals(
            MpvNode.Dict(
                mapOf(
                    "name" to MpvNode.Str("loadfile"), "url" to MpvNode.Str("a.mkv"), "flags" to MpvNode.Str("replace"),
                    "options" to MpvNode.Dict(mapOf("start" to MpvNode.Str("10"))),
                ),
            ),
            MpvCommands.loadFile("a.mkv", options = mapOf("start" to "10")).toNode(),
        )
        assertEquals(
            MpvNode.Dict(mapOf("name" to MpvNode.Str("show-text"), "text" to MpvNode.Str("hi"), "level" to MpvNode.Int64(1))),
            MpvCommands.showText("hi", level = 1).toNode(),
        )
        // With nothing left out in the middle, the positional form stays.
        assertEquals(listOf(MpvNode.Str("show-text"), MpvNode.Str("hi"), MpvNode.Int64(500)), args(MpvCommands.showText("hi", 500)))
    }

    @Test
    fun osdOverlayUsesNamedArguments() {
        val node = MpvCommands.osdOverlay(1, OsdOverlayFormat.AssEvents, "{\\an7}hi").toNode()
        assertTrue(node is MpvNode.Dict, "osd-overlay takes named arguments only, got $node")
        assertEquals(MpvNode.Str("osd-overlay"), node.values["name"])
        assertEquals(MpvNode.Int64(1), node.values["id"])
        assertEquals(MpvNode.Str("ass-events"), node.values["format"])
    }

    /** set and cycle-values take strings, whatever the property's own type is. */
    @Test
    fun setAndCycleValuesSendStrings() {
        assertEquals(listOf(MpvNode.Str("set"), MpvNode.Str("pause"), MpvNode.Str("yes")), args(MpvCommands.set(MpvProperties.Pause, true)))
        assertEquals(listOf(MpvNode.Str("set"), MpvNode.Str("speed"), MpvNode.Str("1.5")), args(MpvCommands.set("speed", MpvNode.Dbl(1.5))))
        assertEquals(
            listOf(MpvNode.Str("cycle-values"), MpvNode.Str("speed"), MpvNode.Str("1.0"), MpvNode.Str("2.0")),
            args(MpvCommands.cycleValues("speed", listOf(MpvNode.Dbl(1.0), MpvNode.Dbl(2.0)))),
        )
    }
}
