package io.github.yuroyami.libmpvkt.buildtools

import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Refuses to package native libraries that are incomplete, misaligned, linked for a newer API than
 * the AAR's minSdk, compiled for another Kotlin package, or carrying an old libc++. Each rule is a
 * failure that reached a device once, or would have.
 */
abstract class CheckNativeLibsTask : DefaultTask() {

    /** libmpvkt-native/native-libs. Internal because it may not exist yet, and a missing directory must produce our message. */
    @get:Internal
    abstract val nativeLibsDir: DirectoryProperty

    /** The files under [nativeLibsDir], declared so the task re-runs when any of them changes. */
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val libraryFiles: ConfigurableFileCollection

    /** ABI directory names that must be complete. */
    @get:Input
    abstract val abis: ListProperty<String>

    /** One line saying the last check passed. Declaring it lets Gradle skip the check when nothing changed. */
    @get:OutputFile
    abstract val report: RegularFileProperty

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
        val message = "native libraries complete for ${abis.get().joinToString()}"
        report.get().asFile.writeText("$message\n")
        logger.lifecycle("[libmpvKt] $message")
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
                    // Only the headers, a few hundred bytes: the libraries are 142 MB together.
                    val header = runCatching { Elf.headerBytes(file) }.getOrNull()
                    val aligns = header?.let { runCatching { Elf.loadAlignments(it) }.getOrNull() }
                    if (header == null || aligns == null) {
                        problems += "$abi/$lib is not an ELF file"
                        continue
                    }
                    val smallest = aligns.minOrNull() ?: 0L
                    if (smallest < MIN_ALIGN && abi in NativeLibs.abisRequiring16k) {
                        problems += "$abi/$lib has a LOAD segment aligned to $smallest bytes; 16384 is required"
                    }
                    val api = Elf.androidApiLevel(header)
                    if (api != null && api > NativeLibs.MIN_API) {
                        problems += "$abi/$lib is linked for API $api, above the AAR's minSdk ${NativeLibs.MIN_API}"
                    }
                    // The two text searches read their whole file; both libraries are small once stripped.
                    if (lib == NativeLibs.JNI_LIB && !file.readBytes().containsAscii(NativeLibs.JNI_PROBE_SYMBOL)) {
                        problems += "$abi/$lib does not export ${NativeLibs.JNI_PROBE_SYMBOL}; the C side was compiled for another package"
                    }
                    if (lib == "libc++_shared.so" && !file.readBytes().containsAscii(NativeLibs.LIBCXX_MARKER)) {
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
