# libmpvkt_jni.so resolves this class with FindClass, its static callbacks (eventProperty, event,
# logMessage) with GetStaticMethodID, and its native methods by name. None of that is visible to
# R8's reachability analysis, so the whole class and its nested types stay as written.
-keep class io.github.yuroyami.libmpvkt.MPVLib { *; }
-keep class io.github.yuroyami.libmpvkt.MPVLib$* { *; }
