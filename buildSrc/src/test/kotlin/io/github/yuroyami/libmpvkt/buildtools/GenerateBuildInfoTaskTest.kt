package io.github.yuroyami.libmpvkt.buildtools

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class GenerateBuildInfoTaskTest {
    private val depinfo = """
        #!/bin/bash -e
        v_ndk=29.0.14206865
        v_mpv=0.41.0
        v_ffmpeg=9.0.1
        v_libass=0.17.5
        v_libplacebo=7.360.1
        v_dav1d=1.5.4
        v_mbedtls=3.6.7
        v_harfbuzz=14.4.0
        v_freetype=2.14.3
        v_fribidi=1.0.16
        v_unibreak=7.0
        v_lua=5.2.4
        dep_mpv=(ffmpeg libass lua libplacebo)
    """.trimIndent()

    @Test
    fun pinsAreReadFromTheShellFile() {
        val pins = GenerateBuildInfoTask.parsePins(depinfo)
        assertEquals("0.41.0", pins["mpv"])
        assertEquals("9.0.1", pins["ffmpeg"])
        assertEquals(12, pins.size)
    }

    @Test
    fun theRenderedObjectCarriesEveryPin() {
        val source = GenerateBuildInfoTask.render(GenerateBuildInfoTask.parsePins(depinfo), "0.1.0", 21)
        assertTrue("package io.github.yuroyami.libmpvkt" in source)
        assertTrue("public object BuildInfo" in source)
        assertTrue("public const val LIBMPVKT: String = \"0.1.0\"" in source)
        assertTrue("public const val MPV: String = \"0.41.0\"" in source)
        assertTrue("public const val NDK: String = \"29.0.14206865\"" in source)
        assertTrue("public const val MIN_SDK: Int = 21" in source)
    }

    @Test
    fun aMissingPinFailsNamingIt() {
        val pins = GenerateBuildInfoTask.parsePins(depinfo).minus("libass")
        val failure = assertFailsWith<IllegalStateException> { GenerateBuildInfoTask.render(pins, "0.1.0", 21) }
        assertTrue("v_libass" in failure.message.orEmpty())
    }
}
