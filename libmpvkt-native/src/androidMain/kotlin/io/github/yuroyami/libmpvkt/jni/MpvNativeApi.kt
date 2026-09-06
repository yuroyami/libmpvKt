package io.github.yuroyami.libmpvkt.jni

/** Marks the raw binding. Apps use `io.github.yuroyami.libmpvkt.Mpv`; the higher modules of this library opt in here. */
@RequiresOptIn(
    level = RequiresOptIn.Level.WARNING,
    message = "MpvNative is the raw JNI binding: handles are Longs, values are byte arrays, nothing is checked. Use Mpv.",
)
@Retention(AnnotationRetention.BINARY)
@Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.PROPERTY)
public annotation class MpvNativeApi
