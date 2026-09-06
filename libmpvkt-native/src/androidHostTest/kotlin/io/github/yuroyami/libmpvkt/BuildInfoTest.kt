package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** BuildInfo is generated from depinfo.sh; this pins its shape. MPVLib itself loads native code and is tested on a device. */
class BuildInfoTest {
    private val release = Regex("""\d+\.\d+\.\d+""")

    @Test
    fun theVersionsAreReleaseNumbers() {
        assertTrue(release.matches(BuildInfo.LIBMPVKT), BuildInfo.LIBMPVKT)
        assertTrue(release.matches(BuildInfo.MPV), BuildInfo.MPV)
        assertTrue(release.matches(BuildInfo.FFMPEG), BuildInfo.FFMPEG)
        assertTrue(release.matches(BuildInfo.LIBASS), BuildInfo.LIBASS)
    }

    @Test
    fun theAarPromisesFourAbisFromApi21() {
        assertEquals(listOf("arm64-v8a", "armeabi-v7a", "x86", "x86_64"), BuildInfo.ABIS)
        assertEquals(21, BuildInfo.MIN_SDK)
        assertEquals("29.0.14206865", BuildInfo.NDK)
    }
}
