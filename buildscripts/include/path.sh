#!/bin/bash

DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && cd .. && pwd )"
ROOT="$( cd "$DIR/.." && pwd )"
export ROOT

. "$DIR/include/depinfo.sh"

os=linux
[[ "$OSTYPE" == "darwin"* ]] && os=mac
export os

if [ "$os" == "mac" ]; then
	[ -z "$cores" ] && cores=$(sysctl -n hw.ncpu)
	# lua's makefile and a few configure scripts rely on GNU install and sed
	export INSTALL=$(which ginstall)
	export SED=gsed
else
	[ -z "$cores" ] && cores=$(grep -c ^processor /proc/cpuinfo)
fi
cores=${cores:-4}
export cores

# The NDK: ANDROID_NDK_HOME wins, else <ANDROID_HOME>/ndk/<pinned version>.
if [ -z "$ANDROID_NDK_HOME" ]; then
	if [ -n "$ANDROID_HOME" ] && [ -d "$ANDROID_HOME/ndk/$v_ndk" ]; then
		export ANDROID_NDK_HOME="$ANDROID_HOME/ndk/$v_ndk"
	else
		echo "Error: set ANDROID_NDK_HOME to an NDK $v_ndk directory, or ANDROID_HOME to an SDK with ndk/$v_ndk installed." >&2
		exit 255
	fi
fi
if ! grep -qF "$v_ndk" "$ANDROID_NDK_HOME/source.properties"; then
	echo "Error: $ANDROID_NDK_HOME is not NDK $v_ndk (see its source.properties)." >&2
	exit 255
fi

toolchain=$(echo "$ANDROID_NDK_HOME/toolchains/llvm/prebuilt/"*)
export PATH="$toolchain/bin:$ANDROID_NDK_HOME:$PATH"

# pkg-config must only see the prefix being built
if [ -n "$ndk_triple" ]; then
	export PKG_CONFIG_SYSROOT_DIR="$prefix_dir"
	export PKG_CONFIG_LIBDIR="$PKG_CONFIG_SYSROOT_DIR/lib/pkgconfig"
	unset PKG_CONFIG_PATH
fi
