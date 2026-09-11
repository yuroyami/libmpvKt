package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/** `screenshot-raw` pixels into the RGBA byte order an ARGB_8888 bitmap stores. */
class ScreenshotPixelsTest {

    private fun bytes(vararg values: Int) = ByteArray(values.size) { values[it].toByte() }

    @Test
    fun rgbaRowsLoseTheirPadding() {
        // Two pixels a row and a stride of 12: the last four bytes of each row are padding.
        val data = bytes(1, 2, 3, 4, 5, 6, 7, 8, 0, 0, 0, 0, 9, 10, 11, 12, 13, 14, 15, 16, 0, 0, 0, 0)
        assertContentEquals(bytes(1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16), rgbaPixels(data, 2, 2, 12, "rgba"))
    }

    @Test
    fun bgraSwapsRedAndBlue() {
        assertContentEquals(bytes(3, 2, 1, 200), rgbaPixels(bytes(1, 2, 3, 200), 1, 1, 4, "bgra"))
    }

    @Test
    fun bgr0IsOpaque() {
        assertContentEquals(bytes(3, 2, 1, 255), rgbaPixels(bytes(1, 2, 3, 0), 1, 1, 4, "bgr0"))
    }

    @Test
    fun rgba64DoesNotFit() {
        assertNull(rgbaPixels(ByteArray(8), 1, 1, 8, "rgba64"))
    }

    @Test
    fun shortDataIsRefused() {
        assertFailsWith<IllegalArgumentException> { rgbaPixels(ByteArray(4), 2, 1, 8, "rgba") }
    }
}
