#!/usr/bin/env bash
# Packages one ABI's native libraries into dist/libmpvkt-natives-<abi>.zip plus a .sha256, with a
# BUILD-INFO.txt naming every pinned version and commit. The same input gives the same bytes:
# fixed timestamps, sorted entries, no date inside.
#
#   .github/scripts/package-natives.sh arm64-v8a
set -euo pipefail

abi="${1:?abi, e.g. arm64-v8a}"
cd "$(dirname "$0")/../.."
. buildscripts/include/depinfo.sh

src="libmpvkt/native-libs/$abi"
[ -d "$src" ] || { echo "::error::$src does not exist; run buildscripts/buildall.sh first" >&2; exit 1; }
for lib in libavcodec libavdevice libavfilter libavformat libavutil libswresample libswscale libmpv libmpvkt_jni libc++_shared; do
  [ -f "$src/$lib.so" ] || { echo "::error::$src/$lib.so is missing" >&2; exit 1; }
done

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/$abi" dist
cp "$src"/*.so "$work/$abi/"

{
  echo "libmpvkt=$(grep '^VERSION=' gradle.properties | cut -d= -f2)"
  echo "abi=$abi"
  echo "ndk=$v_ndk"
  echo "api=21"
  for dep in mpv ffmpeg libass libplacebo dav1d mbedtls harfbuzz freetype2 fribidi unibreak lua; do
    ver_key="v_${dep/freetype2/freetype}"
    commit="$(cat "buildscripts/deps/$dep/.libmpvkt-commit" 2>/dev/null || echo unknown)"
    echo "$dep=${!ver_key} $commit"
  done
} > "$work/BUILD-INFO.txt"

find "$work" -exec touch -t 200001010000 {} +
out="$PWD/dist/libmpvkt-natives-$abi.zip"
rm -f "$out"
( cd "$work" && { echo BUILD-INFO.txt; find "$abi" -type f | sort; } | zip -X -q "$out" -@ )
( cd dist && shasum -a 256 "libmpvkt-natives-$abi.zip" > "libmpvkt-natives-$abi.zip.sha256" )
ls -l dist/libmpvkt-natives-$abi.zip*
