package io.github.yuroyami.libmpvkt.buildtools

import java.nio.ByteBuffer
import java.nio.ByteOrder

/** Reads the alignment of every PT_LOAD segment of an ELF file. Little-endian only, which every Android ABI is. */
object ElfAlignment {
    private const val PT_LOAD = 1L

    /** Every PT_LOAD program header's p_align, in file order. Throws [IllegalArgumentException] on a non-ELF file. */
    fun loadAlignments(bytes: ByteArray): List<Long> {
        require(
            bytes.size >= 0x40 && bytes[0] == 0x7f.toByte() && bytes[1] == 'E'.code.toByte() &&
                bytes[2] == 'L'.code.toByte() && bytes[3] == 'F'.code.toByte(),
        ) { "not an ELF file" }
        val is64 = bytes[4] == 2.toByte()
        val buf = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        val phoff: Long
        val phentsize: Int
        val phnum: Int
        if (is64) {
            phoff = buf.getLong(0x20)
            phentsize = buf.getShort(0x36).toInt() and 0xffff
            phnum = buf.getShort(0x38).toInt() and 0xffff
        } else {
            phoff = buf.getInt(0x1c).toLong() and 0xffffffffL
            phentsize = buf.getShort(0x2a).toInt() and 0xffff
            phnum = buf.getShort(0x2c).toInt() and 0xffff
        }
        val out = mutableListOf<Long>()
        for (i in 0 until phnum) {
            val base = (phoff + i.toLong() * phentsize).toInt()
            val type = buf.getInt(base).toLong() and 0xffffffffL
            if (type != PT_LOAD) continue
            out += if (is64) buf.getLong(base + 0x30) else (buf.getInt(base + 0x1c).toLong() and 0xffffffffL)
        }
        return out
    }
}
