package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class MpvPropertyTest {
    private enum class Mode(override val mpvName: String) : MpvChoice { A("a"), B("b-c") }

    @Test
    fun eachKindDecodesItsOwnNodeAndTheLenientForms() {
        assertEquals(true, MpvProperty.Flag("x").decode(MpvNode.Flag(true)))
        assertEquals(true, MpvProperty.Flag("x").decode(MpvNode.Str("yes")))
        assertEquals(7L, MpvProperty.Int64("x").decode(MpvNode.Int64(7)))
        assertEquals(7L, MpvProperty.Int64("x").decode(MpvNode.Dbl(7.0)))
        assertEquals(1.5, MpvProperty.Dbl("x").decode(MpvNode.Dbl(1.5)))
        assertEquals(3.0, MpvProperty.Dbl("x").decode(MpvNode.Int64(3)))
        assertEquals("12", MpvProperty.Str("x").decode(MpvNode.Int64(12)))
        assertEquals(MpvNode.Arr(emptyList()), MpvProperty.Node("x").decode(MpvNode.Arr(emptyList())))
        assertEquals(Mode.B, MpvProperty.Choice("x", Mode.entries).decode(MpvNode.Str("b-c")))
    }

    @Test
    fun aShapeMismatchNamesBothTypes() {
        val failure = assertFailsWith<IllegalArgumentException> { MpvProperty.Dbl("time-pos").decode(MpvNode.Str("soon")) }
        assertTrue("time-pos" in failure.message!! && "Double" in failure.message!! && "Str" in failure.message!!, failure.message)
    }

    @Test
    fun encodingIsTheNaturalNode() {
        assertEquals(MpvNode.Flag(false), MpvProperty.Flag("x").encode(false))
        assertEquals(MpvNode.Int64(4), MpvProperty.Int64("x").encode(4))
        assertEquals(MpvNode.Dbl(0.5), MpvProperty.Dbl("x").encode(0.5))
        assertEquals(MpvNode.Str("b-c"), MpvProperty.Choice("x", Mode.entries).encode(Mode.B))
    }
}
