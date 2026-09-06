package io.github.yuroyami.libmpvkt.buildtools

/** What one complete ABI directory under libmpvkt/native-libs holds. */
object NativeLibs {
    /** Android ABI directory name to the architecture name buildscripts/buildall.sh takes. */
    val abis: Map<String, String> = linkedMapOf(
        "arm64-v8a" to "arm64",
        "armeabi-v7a" to "armv7l",
        "x86_64" to "x86_64",
        "x86" to "x86",
    )

    /** Google Play requires 16 KB page alignment on 64-bit devices; the 32-bit ABIs are reported only. */
    val abisRequiring16k: Set<String> = setOf("arm64-v8a", "x86_64")

    /** The library the JNI sources compile to. MPVLib loads it after libmpv. */
    const val JNI_LIB: String = "libmpvkt_jni.so"

    /** Every file each ABI directory must hold before the AAR may be assembled. */
    val expectedLibs: List<String> = listOf(
        "libavcodec.so", "libavdevice.so", "libavfilter.so", "libavformat.so", "libavutil.so",
        "libswresample.so", "libswscale.so", "libmpv.so", JNI_LIB, "libc++_shared.so",
    )

    /** One exported JNI symbol. Its presence proves the C side was compiled for this Kotlin package. */
    const val JNI_PROBE_SYMBOL: String = "Java_io_github_yuroyami_libmpvkt_MPVLib_create"

    /**
     * A fragment of a symbol only the NDK r29 libc++ carries. An older libc++ lacks
     * __from_chars_floating_point and crashes libmpv at load; Synkplay shipped that once.
     */
    const val LIBCXX_MARKER: String = "from_chars_floating"
}
