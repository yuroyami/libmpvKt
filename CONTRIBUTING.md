# Contributing to libmpvKt

libmpvKt packages a prebuilt libmpv for Android. The Kotlin side is small; most of the work is in the build scripts and in keeping what ships true to what the docs say.

Open work lives in GitHub Issues. There is no private planning file.

## Ground rules

- **Red first.** A new check or test is seen failing before it is seen passing. `CheckNativeLibsTask` was proved by removing a library and watching the AAR refuse to build; keep that habit.
- **A claim carries the strength of its evidence.** A green host test says nothing about a device. The device tests and the sample on a real phone are the proof that the libraries load and play.
- **No em dashes and no en dashes in any file.** The gate scans for them.
- **No new dependency without asking first**: not a library, not a plugin, not a GitHub Action, not a change of an upstream pin.
- **Explicit API mode is on.** A public API change runs `./gradlew :libmpvkt-native:updateKotlinAbi` in the same commit, and every public declaration has KDoc.
- **The pins are the truth.** `buildscripts/include/depinfo.sh` is the only place a version is written. README tables, `BuildInfo` and CHANGELOG follow it.
- **When the tree contradicts an issue or a document, stop and say so.** Do not improvise the document back into truth.

## Getting a build

- JDK 21 and an Android SDK (`ANDROID_HOME` or `sdk.dir` in `local.properties`).
- For the natives: NDK 29.0.14206865 in `ANDROID_NDK_HOME`, plus the tools in `docs/building-natives.md`.
- To work on the Kotlin side without compiling anything native: `.github/scripts/fetch-natives.sh v0.1.0` downloads a release's libraries into place.

## The gate before every commit

Two tiers, selected by the paths that changed, never by confidence.

**Tier 1, every change, seconds:** `scripts/check-gate.sh tier1`. Host tests, the ABI dump check, the build-logic tests and the dash scan.

**Tier 2, any change under `buildscripts/`, `libmpvkt-native/native/`, `libmpvkt-canvas/src/androidMain/cpp/`, `buildSrc/`, or to a build file, about an hour:** `scripts/check-gate.sh tier2`. Tier 1, then the arm64 natives, the packaging check, the sample, and every module's device tests. `connectedAndroidTest` runs on every attached device, so the script refuses to start with more than one attached unless `ANDROID_SERIAL` names the device to use.

`./gradlew ... | tail` reports the exit code of `tail`; read the log for `BUILD FAILED`.

## Pull requests

- One change per pull request, with a test where the change is testable.
- Say which gate tier ran.
- A commit that fixes an issue says `Fixes #n` in its body.
