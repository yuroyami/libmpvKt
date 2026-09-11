#!/bin/bash -e
# Compiles libmpvkt_jni.so and copies every prebuilt library, plus the NDK's libc++_shared.so,
# for every architecture whose prefix holds a libmpv.so, into libmpvkt-native/native-libs/<abi>/.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD="$DIR/.."

. "$BUILD"/include/path.sh
. "$BUILD"/include/depinfo.sh

if [ "$1" == "clean" ]; then
	rm -rf "$ROOT/build/ndk-obj" "$ROOT/build/ndk-render-libs" "$ROOT/libmpvkt-native/native-libs"
	exit 0
fi
[ "$1" == "build" ] || exit 255

nativeprefix () {
	if [ -f "$BUILD/prefix/$1/lib/libmpv.so" ]; then
		echo "$BUILD/prefix/$1"
	fi
}

prefix32=$(nativeprefix armv7l)
prefix64=$(nativeprefix arm64)
prefix_x64=$(nativeprefix x86_64)
prefix_x86=$(nativeprefix x86)

if [[ -z "$prefix32" && -z "$prefix64" && -z "$prefix_x64" && -z "$prefix_x86" ]]; then
	echo >&2 "Error: no libmpv.so under buildscripts/prefix/; build mpv first"
	exit 255
fi

export PREFIX32=$prefix32 PREFIX64=$prefix64 PREFIX_X64=$prefix_x64 PREFIX_X86=$prefix_x86

# First run: the core libraries, at the AAR's minSdk (APP_PLATFORM in Application.mk).
ndk-build -C "$ROOT/libmpvkt-native/native" \
	NDK_LIBS_OUT="$ROOT/libmpvkt-native/native-libs" NDK_OUT="$ROOT/build/ndk-obj/core" \
	-j${cores:-4}

# Second run: libmpvkt_render.so, at API 26 for AHardwareBuffer. It gets its own output
# directory, because ndk-build empties every ABI directory of NDK_LIBS_OUT it installs into.
render_out="$ROOT/build/ndk-render-libs"
rm -rf "$render_out"
ndk-build -C "$ROOT/libmpvkt-native/native" LIBMPVKT_RENDER=1 APP_PLATFORM=android-26 \
	NDK_LIBS_OUT="$render_out" NDK_OUT="$ROOT/build/ndk-obj/render" \
	-j${cores:-4}

# libmpvkt_render.so belongs to libmpvkt-canvas, which publishes it. Leaving it beside the core
# libraries puts the same file in two AARs, and AGP refuses to merge them.
for abi_dir in "$render_out"/*/; do
	abi=$(basename "$abi_dir")
	mkdir -p "$ROOT/libmpvkt-canvas/native-libs/$abi"
	cp "$abi_dir/libmpvkt_render.so" "$ROOT/libmpvkt-canvas/native-libs/$abi/"
done
