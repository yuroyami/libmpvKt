package io.github.yuroyami.libmpvkt

import android.graphics.Bitmap
import java.nio.ByteBuffer

/**
 * The current frame as a bitmap, through `screenshot-raw`. No video, and a format that does not fit an
 * ARGB_8888 bitmap ([ScreenshotRawFormat.Rgba64]), come back as [MpvResult.Fail].
 */
public fun Mpv.screenshot(mode: ScreenshotMode = ScreenshotMode.Subtitles, format: ScreenshotRawFormat = ScreenshotRawFormat.Rgba): MpvResult<Bitmap> {
    val node = when (val r = command(MpvCommands.screenshotRaw(mode, format))) {
        is MpvResult.Fail -> return r
        is MpvResult.Ok -> r.value
    }
    val m = node.asMap()
    val w = m?.get("w")?.asLong()?.toInt()
    val h = m?.get("h")?.asLong()?.toInt()
    val stride = m?.get("stride")?.asLong()?.toInt()
    val data = m?.get("data")?.asBytes()
    val pixelFormat = m?.get("format")?.asString()
    if (w == null || h == null || stride == null || data == null || pixelFormat == null) {
        return MpvResult.Fail(MpvError.PROPERTY_FORMAT, "screenshot-raw answered $node")
    }
    val pixels = rgbaPixels(data, w, h, stride, pixelFormat)
        ?: return MpvResult.Fail(MpvError.UNSUPPORTED, "screenshot-raw format $pixelFormat does not fit an ARGB_8888 bitmap")
    val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    bitmap.copyPixelsFromBuffer(ByteBuffer.wrap(pixels))
    return MpvResult.Ok(bitmap)
}

/** A square [size] by [size] thumbnail of the current frame, cropped to the centre. What `MPVLib.grabThumbnail` did. */
public fun Mpv.thumbnail(size: Int): MpvResult<Bitmap> = screenshot(ScreenshotMode.Video).map { full ->
    val side = minOf(full.width, full.height)
    val square = Bitmap.createBitmap(full, (full.width - side) / 2, (full.height - side) / 2, side, side)
    Bitmap.createScaledBitmap(square, size, size, true)
}

public fun Mpv.expandText(text: String): MpvResult<String> = command(MpvCommands.expandText(text)).map { it.asString().orEmpty() }
public fun Mpv.expandPath(path: String): MpvResult<String> = command(MpvCommands.expandPath(path)).map { it.asString().orEmpty() }

/**
 * A `screenshot-raw` image in the byte order an ARGB_8888 bitmap stores (R, G, B, A), without row
 * padding. `rgba` with no padding is returned as it is. Null for a format that does not fit: `rgba64`.
 */
internal fun rgbaPixels(data: ByteArray, width: Int, height: Int, stride: Int, format: String): ByteArray? {
    val swap = when (format) {
        "rgba" -> false
        "bgra", "bgr0" -> true
        else -> return null
    }
    val row = width * 4
    require(stride >= row && data.size >= stride * (height - 1) + row) { "screenshot-raw data is too short for ${width}x$height" }
    if (!swap && stride == row && data.size == row * height) return data
    val out = ByteArray(row * height)
    for (y in 0 until height) System.arraycopy(data, y * stride, out, y * row, row)
    if (swap) {
        val opaque = format == "bgr0"
        for (i in out.indices step 4) {
            val blue = out[i]
            out[i] = out[i + 2]
            out[i + 2] = blue
            if (opaque) out[i + 3] = -1
        }
    }
    return out
}
