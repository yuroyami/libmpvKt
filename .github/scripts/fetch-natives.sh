#!/usr/bin/env bash
# Downloads the native library zips of a libmpvKt GitHub release into libmpvkt-native/native-libs/,
# verifying each against its .sha256. Default: all four ABIs.
#
#   .github/scripts/fetch-natives.sh v0.1.0
#   .github/scripts/fetch-natives.sh v0.1.0 x86_64
#
# LIBMPVKT_NATIVES_BASE overrides where the files come from; a file:// URL works, which is how
# CI feeds its own artifacts through the same path as a release.
set -euo pipefail

tag="${1:?release tag, e.g. v0.1.0}"
shift
abis=("$@")
[ ${#abis[@]} -eq 0 ] && abis=(arm64-v8a armeabi-v7a x86 x86_64)

cd "$(dirname "$0")/../.."
base="${LIBMPVKT_NATIVES_BASE:-https://github.com/yuroyami/libmpvKt/releases/download/$tag}"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT

for abi in "${abis[@]}"; do
  asset="libmpvkt-natives-$abi.zip"
  curl -fL --retry 3 -o "$work/$asset" "$base/$asset"
  curl -fL --retry 3 -o "$work/$asset.sha256" "$base/$asset.sha256"
  ( cd "$work" && shasum -a 256 -c "$asset.sha256" )
  mkdir -p "$work/$abi.unpacked"
  unzip -q "$work/$asset" -d "$work/$abi.unpacked"
  rm -rf "libmpvkt-native/native-libs/$abi"
  mkdir -p libmpvkt-native/native-libs
  mv "$work/$abi.unpacked/$abi" "libmpvkt-native/native-libs/$abi"
  test -f "libmpvkt-native/native-libs/$abi/libmpv.so"
  # The render library publishes from libmpvkt-canvas, not from the core module.
  if [ -f "libmpvkt-native/native-libs/$abi/libmpvkt_render.so" ]; then
    mkdir -p "libmpvkt-canvas/native-libs/$abi"
    mv "libmpvkt-native/native-libs/$abi/libmpvkt_render.so" "libmpvkt-canvas/native-libs/$abi/"
  fi
  echo "fetched $abi: $(ls "libmpvkt-native/native-libs/$abi" | wc -l | tr -d ' ') files"
done
