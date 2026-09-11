#!/usr/bin/env bash
# The local commit gate. Usage: scripts/check-gate.sh tier1|tier2
#   tier1: every change, seconds. Host tests, the ABI dump, the build-logic tests, the dash scan.
#   tier2: any change under buildscripts/, libmpvkt-native/native/, libmpvkt-canvas/src/androidMain/cpp/,
#          buildSrc/ or a build file, about an hour. tier1, then the arm64 natives, the packaging
#          check, the sample, and every module's device tests on one connected device.
set -euo pipefail

TIER="${1:-}"
case "$TIER" in tier1|tier2) ;; *) echo "usage: $0 tier1|tier2" >&2; exit 2 ;; esac
cd "$(dirname "${BASH_SOURCE[0]}")/.."

run() {
    printf 'gate:'
    printf ' %q' "$@"
    printf '\n'
    "$@"
}

scan_dashes() {
    # Em dash and en dash, in every tracked text file. git grep exits 1 on no match, which is the pass.
    if git grep -n -I -e "$(printf '\342\200\224')" -e "$(printf '\342\200\223')" -- . ; then
        echo 'An em dash or an en dash is in a tracked file.' >&2
        return 1
    fi
}

run ./gradlew --console=plain testAndroidHostTest checkKotlinAbi
run ./gradlew --console=plain -p buildSrc test
run scan_dashes

if [ "$TIER" = tier2 ]; then
    # connectedAndroidTest runs on every attached device, so with more than one, ANDROID_SERIAL picks the one.
    # Checked before the hour of building, not after it.
    if [ -z "${ANDROID_SERIAL:-}" ]; then
        attached=$(adb devices | awk 'NR > 1 && $2 == "device" { print $1 }')
        count=$(printf '%s' "$attached" | grep -c . || true)
        if [ "$count" -ne 1 ]; then
            echo "tier2 needs exactly one device, or ANDROID_SERIAL set to one; attached: ${attached:-none}" >&2
            exit 1
        fi
    fi
    run buildscripts/download-deps.sh
    run buildscripts/buildall.sh --arch arm64
    run ./gradlew --console=plain -Plibmpvkt.abis=arm64-v8a :libmpvkt-native:checkNativeLibs bundleAndroidMainAar :sample:assembleDebug
    run ./gradlew --console=plain -Plibmpvkt.abis=arm64-v8a connectedAndroidTest
fi

echo "gate $TIER passed"
