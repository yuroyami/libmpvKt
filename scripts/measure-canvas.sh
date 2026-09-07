#!/usr/bin/env bash
# The canvas renderer's speed gate, on a connected device. This is the measurement that decides
# whether libmpvkt-canvas stops calling itself experimental.
#
#   scripts/measure-canvas.sh /path/to/a-1080p30.mp4
#
# It installs the sample, plays the file on the canvas screen for 30 seconds, and prints what the
# renderer and mpv report. The threshold, from docs/compose-canvas.md: at least 29 frames per
# second with fewer than 10 dropped frames.
set -euo pipefail

VIDEO="${1:?a 1080p30 video file to play}"
SECONDS_TO_RUN="${SECONDS_TO_RUN:-30}"
PKG=io.github.yuroyami.libmpvkt.sample
cd "$(dirname "${BASH_SOURCE[0]}")/.."

[ -f "$VIDEO" ] || { echo "no such file: $VIDEO" >&2; exit 1; }
timeout 10 adb shell true >/dev/null 2>&1 || { echo "no device: plug one in and enable USB debugging" >&2; exit 1; }
model=$(adb shell getprop ro.product.model | tr -d '\r')
api=$(adb shell getprop ro.build.version.sdk | tr -d '\r')
echo "device: $model, API $api"

abi=$(adb shell getprop ro.product.cpu.abi | tr -d '\r')
echo "building the sample for $abi"
./gradlew :sample:assembleDebug -Plibmpvkt.abis="$abi" --console=plain -q

echo "installing"
adb install -r sample/build/outputs/apk/debug/sample-debug.apk >/dev/null

remote=/sdcard/Download/canvas-measure.mp4
echo "pushing the video"
adb push "$VIDEO" "$remote" >/dev/null

# The canvas screen takes a file path through its intent, falling back to its own sample URL.
adb logcat -c
adb shell am start -n "$PKG/.CanvasActivity" -e video "$remote" >/dev/null
echo "playing for ${SECONDS_TO_RUN}s"
sleep "$SECONDS_TO_RUN"

echo
echo "what the renderer and mpv reported:"
adb logcat -d -s libmpvKt:I | grep -o "CANVAS-MEASUREMENT.*" | tail -5 || echo "  no measurement lines: is the canvas screen on top?"
adb shell am force-stop "$PKG"
adb shell rm -f "$remote" >/dev/null 2>&1 || true

cat <<'NOTE'

The gate: at least 29 frames per second, fewer than 10 dropped, over the run. If it holds, drop
the word experimental from README.md, libmpvkt-canvas/Module.md and CHANGELOG.md, and put these
numbers in the table in docs/compose-canvas.md. If it does not, the label stays and the numbers
still go in the table.
NOTE
