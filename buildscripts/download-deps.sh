#!/bin/bash -e
# Fetches every upstream source at the tag depinfo.sh pins, into buildscripts/deps/.
# Idempotent: an existing checkout is left alone. --refresh deletes deps/ first, which a version
# bump needs. Each checkout records what it is in .libmpvkt-commit, for BUILD-INFO.txt.

cd "$( dirname "${BASH_SOURCE[0]}" )"
. ./include/depinfo.sh

[ "${1:-}" == "--refresh" ] && rm -rf deps
mkdir -p deps && cd deps

# A checkout counts as done only once .libmpvkt-commit exists, so an interrupted clone or a
# submodule that failed to check out is fetched again instead of being trusted.
clone () { # <dir> <url> <tag> [extra git args]
	local dir=$1 url=$2 tag=$3
	shift 3
	[ -f "$dir/.libmpvkt-commit" ] && return 0
	rm -rf "$dir"
	git clone --depth 1 --branch "$tag" "$@" "$url" "$dir"
	git -C "$dir" submodule update --init --recursive --depth 1 3rdparty 2>/dev/null || true
	git -C "$dir" rev-parse HEAD > "$dir/.libmpvkt-commit"
}

fetch_tar () { # <dir> <url>
	local dir=$1 url=$2
	[ -f "$dir/.libmpvkt-commit" ] && return 0
	rm -rf "$dir"
	mkdir "$dir"
	curl -fL --retry 3 "$url" | tar -xz -C "$dir" --strip-components=1
	echo "$url" > "$dir/.libmpvkt-commit"
}

clone mbedtls    https://github.com/Mbed-TLS/mbedtls.git             "v$v_mbedtls" --recurse-submodules --shallow-submodules
clone dav1d      https://code.videolan.org/videolan/dav1d.git         "$v_dav1d"
clone ffmpeg     https://github.com/FFmpeg/FFmpeg.git                 "n$v_ffmpeg"
clone freetype2  https://gitlab.freedesktop.org/freetype/freetype.git "VER-${v_freetype//./-}" --recurse-submodules --shallow-submodules
clone fribidi    https://github.com/fribidi/fribidi.git               "v$v_fribidi"
clone harfbuzz   https://github.com/harfbuzz/harfbuzz.git             "$v_harfbuzz"
fetch_tar unibreak "https://github.com/adah1972/libunibreak/releases/download/libunibreak_${v_unibreak//./_}/libunibreak-${v_unibreak}.tar.gz"
clone libass     https://github.com/libass/libass.git                 "$v_libass"
fetch_tar lua    "https://www.lua.org/ftp/lua-$v_lua.tar.gz"
clone libplacebo https://code.videolan.org/videolan/libplacebo.git    "v$v_libplacebo" --recurse-submodules --shallow-submodules
clone mpv        https://github.com/mpv-player/mpv.git                "v$v_mpv"

cd ..
echo "deps ready"
