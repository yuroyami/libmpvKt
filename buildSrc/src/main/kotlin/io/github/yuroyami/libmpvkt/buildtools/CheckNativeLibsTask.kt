package io.github.yuroyami.libmpvkt.buildtools

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Refuses to package native libraries that are incomplete, misaligned, compiled for another Kotlin
 * package, or carrying an old libc++. Each rule is a failure that reached a device once.
 */
abstract class CheckNativeLibsTask : DefaultTask() {

    /** libmpvkt/native-libs. Internal because it may not exist yet, and a missing directory must produce our message. */
    @get:Internal
    abstract val nativeLibsDir: DirectoryProperty

    /** The files under [nativeLibsDir], declared so the task re-runs when any of them changes. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val libraryFiles: ConfigurableFileCollection

    /** ABI directory names that must be complete. */
    @get:Input
    abstract val abis: ListProperty<String>

    @TaskAction
    fun check() {
        val problems = findProblems(nativeLibsDir.get().asFile, abis.get())
        if (problems.isNotEmpty()) {
            throw GradleException(
                "Native libraries are not ready to ship:\n" +
                    problems.joinToString("\n") { "  - $it" } +
                    "\nBuild them with buildscripts/buildall.sh --arch <arch>, or fetch a release's " +
                    "with .github/scripts/fetch-natives.sh <tag>. A laptop with one architecture " +
                    "passes -Plibmpvkt.abis=<abi>.",
            )
        }
        logger.lifecycle("[libmpvKt] native libraries complete for ${abis.get().joinToString()}")
    }

    companion object {
        const val MIN_ALIGN: Long = 16384

        /** Every problem in [root] for [abis], as one sentence each. Empty means ready. */
        fun findProblems(root: File, abis: List<String>): List<String> {
            val problems = mutableListOf<String>()
            for (abi in abis) {
                val dir = File(root, abi)
                for (lib in NativeLibs.expectedLibs) {
                    val file = File(dir, lib)
                    if (!file.isFile) {
                        problems += "$abi/$lib is missing"
                        continue
                    }
                    val bytes = file.readBytes()
                    val aligns = runCatching { ElfAlignment.loadAlignments(bytes) }.getOrNull()
                    if (aligns == null) {
                        problems += "$abi/$lib is not an ELF file"
                        continue
                    }
                    val smallest = aligns.minOrNull() ?: 0L
                    if (smallest < MIN_ALIGN && abi in NativeLibs.abisRequiring16k) {
                        problems += "$abi/$lib has a LOAD segment aligned to $smallest bytes; 16384 is required"
                    }
                    if (lib == NativeLibs.JNI_LIB && !bytes.containsAscii(NativeLibs.JNI_PROBE_SYMBOL)) {
                        problems += "$abi/$lib does not export ${NativeLibs.JNI_PROBE_SYMBOL}; the C side was compiled for another package"
                    }
                    if (lib == "libc++_shared.so" && !bytes.containsAscii(NativeLibs.LIBCXX_MARKER)) {
                        problems += "$abi/$lib is an older libc++ than the NDK r29 one mpv needs"
                    }
                }
            }
            return problems
        }

        private fun ByteArray.containsAscii(text: String): Boolean =
            String(this, Charsets.ISO_8859_1).contains(text)
    }
}
