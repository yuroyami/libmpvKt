#!/usr/bin/env bash
# Packages one ABI's native libraries into dist/libmpvkt-natives-<abi>.zip plus a .sha256, with a
# BUILD-INFO.txt naming every pinned version and commit. The same input gives the same bytes:
# fixed timestamps, sorted entries, no date inside.
#
# It also packages dist/libmpvkt-symbols-<abi>.zip: the unstripped copies of the same libraries,
# for a crash reporter or ndk-stack. Each one must carry the build id of its packaged copy.
#
#   .github/scripts/package-natives.sh arm64-v8a
set -euo pipefail

abi="${1:?abi, e.g. arm64-v8a}"
cd "$(dirname "$0")/../.."
. buildscripts/include/depinfo.sh

src="libmpvkt-native/native-libs/$abi"
# The canvas module's own library travels in the same zip, under the same ABI directory.
render="libmpvkt-canvas/native-libs/$abi"
[ -d "$src" ] || { echo "::error::$src does not exist; run buildscripts/buildall.sh first" >&2; exit 1; }
[ -f "$render/libmpvkt_render.so" ] || { echo "::error::$render/libmpvkt_render.so is missing" >&2; exit 1; }
for lib in libavcodec libavdevice libavfilter libavformat libavutil libswresample libswscale libmpv libmpvkt_jni libc++_shared; do
  [ -f "$src/$lib.so" ] || { echo "::error::$src/$lib.so is missing" >&2; exit 1; }
done

work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/$abi" dist
cp "$src"/*.so "$work/$abi/"
cp "$render"/libmpvkt_render.so "$work/$abi/"

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

# The unstripped copies: ndk-build keeps one of every library it installs, the prebuilt ones too,
# under its object directory, before it strips what goes into native-libs.
objects="build/ndk-obj/core/local/$abi"
render_objects="build/ndk-obj/render/local/$abi"
readelf_bin="$(command -v readelf || command -v llvm-readelf || true)"
[ -n "$readelf_bin" ] || { echo "::error::no readelf to read build ids with" >&2; exit 1; }
build_id() { "$readelf_bin" -n "$1" 2>/dev/null | awk '/Build ID/ { print $3; exit }'; }

symbols="$work/symbols"
mkdir -p "$symbols/$abi"
for packaged in "$work/$abi"/*.so; do
  name="$(basename "$packaged")"
  # The NDK itself keeps the unstripped libc++_shared.so, with the same build id.
  [ "$name" = "libc++_shared.so" ] && continue
  unstripped="$objects/$name"
  [ "$name" = "libmpvkt_render.so" ] && unstripped="$render_objects/$name"
  [ -f "$unstripped" ] || { echo "::error::no unstripped $name under $objects" >&2; exit 1; }
  id="$(build_id "$unstripped")"
  [ -n "$id" ] || { echo "::error::$name has no build id, so a crash could not be matched to its symbols" >&2; exit 1; }
  [ "$id" = "$(build_id "$packaged")" ] || { echo "::error::$name: the packaged and the unstripped copy differ in build id" >&2; exit 1; }
  cp "$unstripped" "$symbols/$abi/"
done
cp "$work/BUILD-INFO.txt" "$symbols/"

find "$symbols" -exec touch -t 200001010000 {} +
symbols_out="$PWD/dist/libmpvkt-symbols-$abi.zip"
rm -f "$symbols_out"
( cd "$symbols" && { echo BUILD-INFO.txt; find "$abi" -type f | sort; } | zip -X -q "$symbols_out" -@ )
( cd dist && shasum -a 256 "libmpvkt-symbols-$abi.zip" > "libmpvkt-symbols-$abi.zip.sha256" )
ls -l dist/libmpvkt-symbols-$abi.zip*
