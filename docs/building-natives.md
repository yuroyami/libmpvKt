# Building the natives

Nobody has to do this to use the library. It is here for changing a flag, bumping a version, or checking that the published bytes come from the published scripts.

## Prerequisites

- Linux or macOS. Windows is not supported, WSL included.
- Android NDK 29.0.14206865, and `ANDROID_NDK_HOME` pointing at it.
- macOS: `brew install automake autoconf libtool pkg-config coreutils gnu-sed meson ninja nasm`. If libplacebo's meson step fails naming `jinja2`: `python3 -m pip install --break-system-packages jinja2`.
- Ubuntu: `sudo apt-get install autoconf automake libtool pkg-config ninja-build meson nasm python3-jinja2`.
- A checkout path without a `#` in it. FFmpeg's configure cannot handle one.

## The commands

```bash
cd buildscripts
./download-deps.sh                  # every upstream at its pinned tag, into deps/
./buildall.sh --arch arm64          # dependencies, mpv, then the JNI library, for one ABI
```

Architectures: `arm64`, `armv7l`, `x86`, `x86_64`. Each takes between ten minutes and an hour the first time, depending on the machine; a CI runner is at the slow end. The output lands in `libmpvkt-native/native-libs/<abi>/`, ten files per ABI. Then:

```bash
./gradlew :libmpvkt-native:checkNativeLibs -Plibmpvkt.abis=arm64-v8a
./gradlew :libmpvkt-native:publishToMavenLocal -Plibmpvkt.abis=arm64-v8a --no-configuration-cache
```

Without `-Plibmpvkt.abis` the check wants all four ABIs.

Useful flags: `--clean` rebuilds a target's build directory, `-n` skips dependencies, `--deps-only` builds everything below mpv and stops, and a target name (`mpv`, `ffmpeg`, `libass`...) builds that one thing.

## Where the versions live

`buildscripts/include/depinfo.sh` is the only place. Every entry is a release tag. `download-deps.sh --refresh` re-fetches after a change, and `BuildInfo` in the library is generated from the same file, so the code and the docs cannot disagree.

## What the scripts do

- `download-deps.sh` clones or downloads each source at its tag and records the commit in `.libmpvkt-commit`, which ends up in the release's `BUILD-INFO.txt`. A checkout counts as done only once that file exists, so an interrupted clone is fetched again.
- `buildall.sh` sets up one prefix per architecture under `buildscripts/prefix/`, writes a meson cross file, and builds each target's dependencies first. Everything below mpv is a static library; mpv and FFmpeg are shared.
- `scripts/jni.sh` runs `ndk-build` over `libmpvkt-native/native/jni/Android.mk`, which compiles `libmpvkt_jni.so` and copies the prebuilt shared libraries plus `libc++_shared.so` into `libmpvkt-native/native-libs/<abi>/`.
- Every shared library is linked with `-Wl,-z,max-page-size=16384 -Wl,-z,common-page-size=16384`, and `checkNativeLibs` reads the ELF headers back to prove it.

## What CI does

`ci.yml` runs one job per ABI on Ubuntu. The prefix (everything below mpv) is cached under a key made of the pins, so a push that touches only Kotlin rebuilds mpv and the JNI library in a few minutes and nothing else. A pin change rebuilds the chain, about half an hour per ABI, in parallel.

`release-natives.yml` builds the same four ABIs on demand and attaches `libmpvkt-natives-<abi>.zip`, its `.sha256`, and the mpv and FFmpeg source tarballs to the GitHub release for the tag. `publish.yml` downloads those zips, verifies them, and assembles the AAR from them, so the published AAR is made of the files anyone can inspect on the release page.

`.github/scripts/fetch-natives.sh v0.1.0` puts a release's libraries into `libmpvkt-native/native-libs/` on any machine, which is the quickest way to work on the Kotlin side without building anything.
