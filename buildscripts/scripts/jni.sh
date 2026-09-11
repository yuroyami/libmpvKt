#!/bin/bash -e
# Compiles libmpvkt_jni.so and copies every prebuilt library, plus the NDK's libc++_shared.so,
# into libmpvkt-native/native-libs/<abi>/, for the one architecture buildall.sh was given.
set -eo pipefail

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD="$DIR/.."

. "$BUILD"/include/path.sh
. "$BUILD"/include/depinfo.sh

# buildall.sh --arch <arch> sets prefix_dir; the Android ABI follows from it.
case "$(basename "${prefix_dir:-}")" in
	armv7l) abi=armeabi-v7a; export PREFIX32="$prefix_dir" ;;
	arm64) abi=arm64-v8a; export PREFIX64="$prefix_dir" ;;
	x86_64) abi=x86_64; export PREFIX_X64="$prefix_dir" ;;
	x86) abi=x86; export PREFIX_X86="$prefix_dir" ;;
	*) echo >&2 "Error: run this through buildall.sh --arch <arch>, which sets prefix_dir"; exit 255 ;;
esac

if [ "$1" == "clean" ]; then
	rm -rf "$ROOT/build/ndk-obj" "$ROOT/build/ndk-render-libs" "$ROOT/libmpvkt-native/native-libs/$abi" \
		"$ROOT/libmpvkt-canvas/native-libs/$abi"
	exit 0
fi
[ "$1" == "build" ] || exit 255

if [ ! -f "$prefix_dir/lib/libmpv.so" ]; then
	echo >&2 "Error: no libmpv.so in $prefix_dir; build mpv for this arch first"
	exit 255
fi

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
mkdir -p "$ROOT/libmpvkt-canvas/native-libs/$abi"
cp "$render_out/$abi/libmpvkt_render.so" "$ROOT/libmpvkt-canvas/native-libs/$abi/"

# The NDK's libc++_shared.so carries its full debug info, about 7.5 MB per ABI, and ndk-build keeps
# ours. The symbol tables stay, so a crash still names its functions; the NDK keeps unstripped
# copies with the same build ID for full symbolication.
for lib in "$ROOT/libmpvkt-native/native-libs/$abi/libc++_shared.so" "$ROOT/libmpvkt-native/native-libs/$abi/libmpvkt_jni.so" \
	"$ROOT/libmpvkt-canvas/native-libs/$abi/libmpvkt_render.so"; do
	llvm-strip --strip-debug "$lib"
done
