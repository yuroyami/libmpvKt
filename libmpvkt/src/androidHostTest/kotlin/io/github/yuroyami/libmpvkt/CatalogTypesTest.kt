package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The catalog against the node shapes and values mpv 0.41 really uses, read from its option tables. */
class CatalogTypesTest {

    /** An object settings list, as mpv sends vo, ao, af, vf, gpu-api and gpu-context. */
    private fun settings(vararg names: String) = MpvNode.Arr(
        names.map { MpvNode.Dict(mapOf("name" to MpvNode.Str(it), "enabled" to MpvNode.Flag(true), "params" to MpvNode.Dict(emptyMap()))) },
    )

    @Test
    fun settingsListsDecode() {
        assertEquals(VideoOutput("gpu"), MpvProperties.Vo.decode(settings("gpu")))
        assertEquals("audiotrack,opensles", MpvProperties.Ao.decode(settings("audiotrack", "opensles")))
        assertEquals("android", MpvProperties.GpuContext.decode(settings("android")))
        assertEquals(GpuApi.Opengl, MpvProperties.GpuApi.decode(settings("opengl")))
        assertEquals(
            HwdecMode("mediacodec,mediacodec-copy"),
            MpvProperties.Hwdec.decode(MpvNode.Arr(listOf(MpvNode.Str("mediacodec"), MpvNode.Str("mediacodec-copy")))),
        )
    }

    @Test
    fun equalizerAndSubtitlePositionAreFloats() {
        assertEquals<Any?>(2.5, MpvProperties.Brightness.decode(MpvNode.Dbl(2.5)))
        assertEquals<Any?>(97.5, MpvProperties.SubPos.decode(MpvNode.Dbl(97.5)))
    }

    @Test
    fun anEmptyChromaScalerMeansInherit() {
        assertEquals<Any?>(null, MpvProperties.Cscale.decode(MpvNode.Str("")))
    }

    /** A float option whose default is NaN reads back as the string "default" (double_get in m_option.c). */
    @Test
    fun toneMappingParamReadsItsDefault() {
        assertEquals<Any?>(null, MpvProperties.ToneMappingParam.decode(MpvNode.Str("default")))
    }

    @Test
    fun videoRotateNoIsAString() {
        assertEquals<Any?>(VideoRotation.No, MpvProperties.VideoRotate.decode(MpvNode.Str("no")))
        assertEquals(MpvNode.Str("no"), MpvProperties.VideoRotate.encode(VideoRotation.No))
    }

    @Test
    fun loopValuesMpvAccepts() {
        assertEquals("Force", MpvProperties.LoopPlaylist.decode(MpvNode.Str("force")).toString())
        assertEquals(MpvNode.Int64(0), MpvProperties.AbLoopCount.encode(LoopCount.No))
    }

    @Test
    fun aspectOverrideUsesNo() {
        assertEquals(MpvNode.Str("no"), MpvProperties.VideoAspectOverride.encode(AspectOverride.Original))
        assertEquals<Any?>(AspectOverride.Original, MpvProperties.VideoAspectOverride.decode(MpvNode.Str("no")))
    }

    @Test
    fun cropFieldsAreWidthAndHeight() {
        val params = VideoParams.Codec.decode(MpvNode.Dict(mapOf("crop-w" to MpvNode.Int64(100), "crop-h" to MpvNode.Int64(50))))
        assertTrue("cropWidth=100" in params.toString() && "cropHeight=50" in params.toString(), params.toString())
    }

    @Test
    fun noDeprecatedAliases() {
        val names = MpvProperties.all.map { it.name }
        for (old in listOf("sub-text-ass", "sub-ass-hinting", "sub-ass-line-spacing", "sub-ass-shaper", "load-osd-console")) {
            assertFalse(old in names, "$old is a deprecated alias in mpv 0.41")
        }
        assertTrue("sub-text/ass" in names)
    }

    @Test
    fun playlistRemoveRefusesNone() {
        assertFailsWith<IllegalArgumentException> { MpvCommands.playlistRemove(PlaylistIndex.None) }
    }
}
