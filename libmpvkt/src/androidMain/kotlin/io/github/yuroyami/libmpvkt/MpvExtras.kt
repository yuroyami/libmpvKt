package io.github.yuroyami.libmpvkt

import android.graphics.Bitmap
import java.nio.ByteBuffer

/** The current frame as a bitmap, through `screenshot-raw`. Null-result cases (no video) come back as [MpvResult.Fail]. */
public fun Mpv.screenshot(mode: ScreenshotMode = ScreenshotMode.Subtitles, format: ScreenshotRawFormat = ScreenshotRawFormat.Rgba): MpvResult<Bitmap> =
    command(MpvCommands.screenshotRaw(mode, format)).map { node ->
        val m = node.asMap() ?: throw MpvException(MpvError.PROPERTY_FORMAT, "screenshot-raw answered $node")
        val w = m["w"]!!.asLong()!!.toInt()
        val h = m["h"]!!.asLong()!!.toInt()
        val stride = m["stride"]!!.asLong()!!.toInt()
        val data = m["data"]!!.asBytes()!!
        val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val pixels = IntArray(w * h)
        val bgr0 = m["format"]?.asString() == "bgr0"
        for (y in 0 until h) {
            val row = ByteBuffer.wrap(data, y * stride, w * 4).order(java.nio.ByteOrder.LITTLE_ENDIAN)
            for (x in 0 until w) {
                val v = row.getInt()
                pixels[y * w + x] = if (bgr0) (0xff shl 24) or (v and 0xffffff) else (v ushr 24 shl 24) or (v and 0xff shl 16) or (v and 0xff00) or (v ushr 16 and 0xff)
            }
        }
        bitmap.setPixels(pixels, 0, w, 0, 0, w, h)
        bitmap
    }

/** A square [size] by [size] thumbnail of the current frame, cropped to the centre. What `MPVLib.grabThumbnail` did. */
public fun Mpv.thumbnail(size: Int): MpvResult<Bitmap> = screenshot(ScreenshotMode.Video).map { full ->
    val side = minOf(full.width, full.height)
    val square = Bitmap.createBitmap(full, (full.width - side) / 2, (full.height - side) / 2, side, side)
    Bitmap.createScaledBitmap(square, size, size, true)
}

public fun Mpv.expandText(text: String): MpvResult<String> = command(MpvCommands.expandText(text)).map { it.asString().orEmpty() }
public fun Mpv.expandPath(path: String): MpvResult<String> = command(MpvCommands.expandPath(path)).map { it.asString().orEmpty() }
