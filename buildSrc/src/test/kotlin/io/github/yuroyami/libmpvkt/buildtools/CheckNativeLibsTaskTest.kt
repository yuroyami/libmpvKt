package io.github.yuroyami.libmpvkt.buildtools

import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CheckNativeLibsTaskTest {

    /**
     * A minimal little-endian ELF: one PT_LOAD per alignment given, a PT_NOTE holding the NDK's
     * Android note when [apiLevel] is set, then [payload] as trailing bytes.
     */
    private fun fakeElf(is64: Boolean, aligns: List<Long>, payload: String = "", apiLevel: Int? = null): ByteArray {
        val headerSize = if (is64) 0x40 else 0x34
        val phentsize = if (is64) 0x38 else 0x20
        val phnum = aligns.size + if (apiLevel != null) 1 else 0
        val note = apiLevel?.let(::androidNote) ?: ByteArray(0)
        val noteOffset = headerSize + phentsize * phnum
        val buf = ByteBuffer.allocate(noteOffset + note.size + payload.length).order(ByteOrder.LITTLE_ENDIAN)
        buf.put(0, 0x7f.toByte()).put(1, 'E'.code.toByte()).put(2, 'L'.code.toByte()).put(3, 'F'.code.toByte())
        buf.put(4, (if (is64) 2 else 1).toByte()) // EI_CLASS
        buf.put(5, 1.toByte()) // little endian
        if (is64) {
            buf.putLong(0x20, headerSize.toLong())
            buf.putShort(0x36, phentsize.toShort())
            buf.putShort(0x38, phnum.toShort())
        } else {
            buf.putInt(0x1c, headerSize)
            buf.putShort(0x2a, phentsize.toShort())
            buf.putShort(0x2c, phnum.toShort())
        }
        aligns.forEachIndexed { i, align ->
            val base = headerSize + i * phentsize
            buf.putInt(base, 1) // PT_LOAD
            if (is64) buf.putLong(base + 0x30, align) else buf.putInt(base + 0x1c, align.toInt())
        }
        if (apiLevel != null) {
            val base = headerSize + aligns.size * phentsize
            buf.putInt(base, 4) // PT_NOTE
            if (is64) {
                buf.putLong(base + 0x08, noteOffset.toLong()).putLong(base + 0x20, note.size.toLong())
            } else {
                buf.putInt(base + 0x04, noteOffset).putInt(base + 0x10, note.size)
            }
        }
        buf.position(noteOffset)
        buf.put(note)
        buf.put(payload.toByteArray(Charsets.ISO_8859_1))
        return buf.array()
    }

    /** NT_ANDROID_TYPE_IDENT: name "Android", then the API level as the first word of the description. */
    private fun androidNote(apiLevel: Int): ByteArray =
        ByteBuffer.allocate(24).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(8).putInt(4).putInt(1).put("Android\u0000".toByteArray(Charsets.ISO_8859_1)).putInt(apiLevel)
            .array()

    private fun completeAbi(root: File, abi: String, is64: Boolean, align: Long) {
        val dir = File(root, abi).apply { mkdirs() }
        for (lib in NativeLibs.expectedLibs) {
            val payload = when (lib) {
                NativeLibs.JNI_LIB -> NativeLibs.JNI_PROBE_SYMBOL
                "libc++_shared.so" -> NativeLibs.LIBCXX_MARKER
                else -> ""
            }
            File(dir, lib).writeBytes(fakeElf(is64, listOf(align, align), payload, apiLevel = NativeLibs.MIN_API))
        }
    }

    private fun root(): File = createTempDirectory("libmpvkt-check").toFile()

    @Test
    fun aCompleteAlignedTreePasses() {
        val root = root()
        completeAbi(root, "arm64-v8a", is64 = true, align = 16384)
        completeAbi(root, "armeabi-v7a", is64 = false, align = 4096)
        assertEquals(emptyList(), CheckNativeLibsTask.findProblems(root, listOf("arm64-v8a", "armeabi-v7a")))
    }

    @Test
    fun aMissingLibraryIsNamed() {
        val root = root()
        completeAbi(root, "arm64-v8a", is64 = true, align = 16384)
        File(root, "arm64-v8a/libmpv.so").delete()
        assertEquals(listOf("arm64-v8a/libmpv.so is missing"), CheckNativeLibsTask.findProblems(root, listOf("arm64-v8a")))
    }

    @Test
    fun aFourKilobyteLoadSegmentFailsOnlyOnThe64BitAbis() {
        val root = root()
        completeAbi(root, "arm64-v8a", is64 = true, align = 4096)
        completeAbi(root, "x86", is64 = false, align = 4096)
        val problems = CheckNativeLibsTask.findProblems(root, listOf("arm64-v8a", "x86"))
        assertEquals(NativeLibs.expectedLibs.size, problems.size)
        assertTrue(problems.all { it.startsWith("arm64-v8a/") && "aligned to 4096" in it }, problems.toString())
    }

    @Test
    fun aJniLibraryBuiltForAnotherPackageIsRefused() {
        val root = root()
        completeAbi(root, "x86_64", is64 = true, align = 16384)
        File(root, "x86_64/${NativeLibs.JNI_LIB}").writeBytes(fakeElf(true, listOf(16384), "Java_is_xyz_mpv_MPVLib_create"))
        val problems = CheckNativeLibsTask.findProblems(root, listOf("x86_64"))
        assertEquals(1, problems.size)
        assertTrue(NativeLibs.JNI_PROBE_SYMBOL in problems.single(), problems.single())
    }

    @Test
    fun anOldLibcxxIsRefused() {
        val root = root()
        completeAbi(root, "x86_64", is64 = true, align = 16384)
        File(root, "x86_64/libc++_shared.so").writeBytes(fakeElf(true, listOf(16384), "nothing useful"))
        val problems = CheckNativeLibsTask.findProblems(root, listOf("x86_64"))
        assertEquals(listOf("x86_64/libc++_shared.so is an older libc++ than the NDK r29 one mpv needs"), problems)
    }

    @Test
    fun aCoreLibraryLinkedAboveTheMinSdkIsRefused() {
        val root = root()
        completeAbi(root, "arm64-v8a", is64 = true, align = 16384)
        File(root, "arm64-v8a/${NativeLibs.JNI_LIB}")
            .writeBytes(fakeElf(true, listOf(16384), NativeLibs.JNI_PROBE_SYMBOL, apiLevel = 26))
        assertEquals(
            listOf("arm64-v8a/${NativeLibs.JNI_LIB} is linked for API 26, above the AAR's minSdk 21"),
            CheckNativeLibsTask.findProblems(root, listOf("arm64-v8a")),
        )
    }

    @Test
    fun theApiLevelComesFromTheAndroidNote() {
        assertEquals(21, Elf.androidApiLevel(fakeElf(is64 = false, aligns = listOf(4096), apiLevel = 21)))
        assertEquals(26, Elf.androidApiLevel(fakeElf(is64 = true, aligns = listOf(16384), apiLevel = 26)))
        assertEquals(null, Elf.androidApiLevel(fakeElf(is64 = true, aligns = listOf(16384))))
    }

    @Test
    fun aFileThatIsNotElfIsRefused() {
        val root = root()
        completeAbi(root, "x86_64", is64 = true, align = 16384)
        File(root, "x86_64/libavutil.so").writeText("not an object file")
        assertEquals(listOf("x86_64/libavutil.so is not an ELF file"), CheckNativeLibsTask.findProblems(root, listOf("x86_64")))
    }
}
