APP_ABI :=
ifneq ($(PREFIX32),)
APP_ABI += armeabi-v7a
endif
ifneq ($(PREFIX64),)
APP_ABI += arm64-v8a
endif
ifneq ($(PREFIX_X64),)
APP_ABI += x86_64
endif
ifneq ($(PREFIX_X86),)
APP_ABI += x86
endif

# 26, not 21: libmpvkt_render.so needs AHardwareBuffer and libnativewindow. The other
# libraries still run on 21; only the canvas module's AAR asks for 26.
APP_PLATFORM := android-26
APP_STL := c++_shared
APP_SUPPORT_FLEXIBLE_PAGE_SIZES := true
