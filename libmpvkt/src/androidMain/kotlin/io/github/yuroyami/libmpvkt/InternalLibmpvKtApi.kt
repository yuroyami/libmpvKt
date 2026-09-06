package io.github.yuroyami.libmpvkt

/** Public for the library's own modules. An app that opts in is on its own when it changes. */
@RequiresOptIn(message = "Internal to libmpvKt; not a stable API.", level = RequiresOptIn.Level.ERROR)
@Retention(AnnotationRetention.BINARY)
public annotation class InternalLibmpvKtApi
