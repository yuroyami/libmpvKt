# The render library registers MpvRenderNative's methods by name.
-keepclasseswithmembernames,includedescriptorclasses class io.github.yuroyami.libmpvkt.canvas.MpvRenderNative { native <methods>; }
# render.cpp throws this class by name, so an app that never names it must still keep it and its constructor.
-keep class io.github.yuroyami.libmpvkt.canvas.MpvRendererException { <init>(java.lang.String); }
