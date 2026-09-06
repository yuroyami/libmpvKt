#!/bin/bash -e
# Compiles libmpvkt_jni.so and copies every prebuilt library, plus the NDK's libc++_shared.so,
# for every architecture whose prefix holds a libmpv.so, into libmpvkt-native/native-libs/<abi>/.

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
BUILD="$DIR/.."

. "$BUILD"/include/path.sh
. "$BUILD"/include/depinfo.sh

if [ "$1" == "clean" ]; then
	rm -rf "$ROOT/build/ndk-obj" "$ROOT/libmpvkt-native/native-libs"
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

PREFIX32=$prefix32 PREFIX64=$prefix64 PREFIX_X64=$prefix_x64 PREFIX_X86=$prefix_x86 \
ndk-build -C "$ROOT/libmpvkt/native" \
	NDK_LIBS_OUT="$ROOT/libmpvkt-native/native-libs" NDK_OUT="$ROOT/build/ndk-obj" \
	-j${cores:-4}
