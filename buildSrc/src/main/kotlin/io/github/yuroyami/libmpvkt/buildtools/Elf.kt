package io.github.yuroyami.libmpvkt.buildtools

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads the parts of an ELF file the native checks need. Little-endian only, which every Android ABI is. */
object Elf {
    private const val PT_LOAD = 1L
    private const val PT_NOTE = 4L
    private const val NT_ANDROID_TYPE_IDENT = 1

    /** Every PT_LOAD program header's p_align, in file order. Throws [IllegalArgumentException] on a non-ELF file. */
    fun loadAlignments(bytes: ByteArray): List<Long> =
        programHeaders(bytes).filter { it.type == PT_LOAD }.map { it.align }

    /**
     * The API level in the NDK's `Android` note, which is the level the library was linked for.
     * Null when the file has no such note.
     */
    fun androidApiLevel(bytes: ByteArray): Int? {
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        for (ph in programHeaders(bytes).filter { it.type == PT_NOTE }) {
            val end = minOf(ph.offset + ph.fileSize, bytes.size.toLong())
            var pos = ph.offset
            while (pos + 12 <= end) {
                val nameSize = buf.getInt(pos.toInt())
                val descSize = buf.getInt(pos.toInt() + 4)
                val type = buf.getInt(pos.toInt() + 8)
                val descStart = pos + 12 + padded(nameSize)
                if (nameSize < 0 || descSize < 0 || descStart + descSize > end) break
                val name = String(bytes, (pos + 12).toInt(), maxOf(nameSize - 1, 0), Charsets.ISO_8859_1)
                if (name == "Android" && type == NT_ANDROID_TYPE_IDENT && descSize >= 4) return buf.getInt(descStart.toInt())
                pos = descStart + padded(descSize)
            }
        }
        return null
    }

    private fun padded(size: Int): Long = ((size + 3) and 3.inv()).toLong()

    private class ProgramHeader(val type: Long, val offset: Long, val fileSize: Long, val align: Long)

    private fun programHeaders(bytes: ByteArray): List<ProgramHeader> {
        require(
            bytes.size >= 0x40 && bytes[0] == 0x7f.toByte() && bytes[1] == 'E'.code.toByte() &&
                bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte(),
        ) { "not an ELF file" }
        val is64 = bytes[4] == 2.toByte()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        fun u32(at: Int): Long = buf.getInt(at).toLong() and 0xffffffffL
        val phoff = if (is64) buf.getLong(0x20) else u32(0x1c)
        val phentsize = buf.getShort(if (is64) 0x36 else 0x2a).toInt() and 0xffff
        val phnum = buf.getShort(if (is64) 0x38 else 0x2c).toInt() and 0xffff
        return (0 until phnum).map { i ->
            val base = (phoff + i.toLong() * phentsize).toInt()
            if (is64) {
                ProgramHeader(u32(base), buf.getLong(base + 0x08), buf.getLong(base + 0x20), buf.getLong(base + 0x30))
            } else {
                ProgramHeader(u32(base), u32(base + 0x04), u32(base + 0x10), u32(base + 0x1c))
            }
        }
    }
}
