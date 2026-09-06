# libmpvKt, for whoever works in this tree

libmpv for Android as one Maven artifact. `libmpvkt-native/` holds the natives and the binding, `libmpvkt/` the typed API,
`buildscripts/` compiles the natives, `buildSrc/` holds the checks. `CONTRIBUTING.md` has the ground rules and the gate.
This file has only what reading the code would not teach you.

## How work happens here

- Work on `main`. Never create a branch without asking. Commit locally, never push. The owner
  pushes and runs the release workflows.
- Commit subject is one imperative sentence about the outcome. Short prose body. No trailers.
- Every change starts with an issue, and the commit that closes one says `Fixes #n` in its body.
- Plain words with the owner. No internal codes, no jargon walls.

## Gotchas

Each line is something that bit someone. Delete a line when it stops being true.

- The Kotlin package and the JNI library name are hardcoded in C: `jni_utils.h` builds the
  `Java_io_github_yuroyami_libmpvkt_MPVLib_` symbol prefix and `jni_utils.cpp` does
  `FindClass("io/github/yuroyami/libmpvkt/MPVLib")`. Renaming the Kotlin package alone loads a
  library that crashes at the first call; `checkNativeLibs` looks for one exported symbol to
  catch it.
- `MPVLib` loads native code in its static initialiser, so a JVM host test cannot touch it.
  Anything about `MPVLib` is a device test.
- One `mpv_handle` per process. `create` while a core exists calls `die()`, which is `exit(1)`.
  So do most misuses in the C layer: a wrong call order is a process exit, not an exception.
- libmpv needs the NDK r29 `libc++_shared.so`. An older one lacks `__from_chars_floating_point`
  and crashes at load; `checkNativeLibs` greps for the symbol. Two AARs both shipping libc++
  make AGP refuse the merge, which is the right outcome.
- Kotlin block comments nest, so a `/*` inside one (`native-libs/<abi>/*.so` written in a build
  file comment) opens a second comment and swallows the rest of the file. The error lands far
  from the cause, on the first `*/` in a string.
- Kotlin's `abiValidation` dumps nothing for a module whose only target is Android, so
  `checkKotlinAbi` passes trivially today. The block stays because it starts working the moment
  a jvm or native target appears; do not read a green `checkKotlinAbi` as proof the API is
  unchanged. Review `MPVLib.kt` by eye.
- Meson's prefix wants a flat layout: `buildall.sh` symlinks `usr` and `local` to `.` inside each
  prefix. Delete `buildscripts/prefix/<arch>` whole rather than parts of it.
- `lua.sh` and `mbedtls.sh` always clean and rebuild; that is upstream's makefile, not a bug.
- On macOS the scripts need GNU `install` and `sed` (`ginstall`, `gsed` from coreutils and
  gnu-sed); `path.sh` exports them.
- FFmpeg's configure fails on a path with `#` in it. The checkout must not live under
  `StudioProjects/#Kite`.
- `-Plibmpvkt.abis=arm64-v8a` is for laptops with one architecture. The publish path never
  passes it, so a publish with a missing ABI fails, on purpose.
- `verifyExoOnlyApk` and the exoOnly exclusion list live in Synkplay, not here, but they name
  this AAR's ten files. Adding or renaming a library here means updating that list there.
- The `mpv-version` property answers `mpv v0.41.0`; `BuildInfo.MPV` is `0.41.0`. The device
  test compares by containment for that reason.
- The Maven repository is the `main` branch of `yuroyami/maven`, served by GitHub Pages under a
  1 GB site cap. One release is about 59 MB (measured on 0.1.0), so it holds about sixteen
  versions: prune the oldest version directories when it nears the cap, never rewrite a published
  one. The native zips stay on every release page for ever, and `fetch-natives.sh` plus
  `publishToMavenLocal` rebuilds any version on any machine.
- Everything above `MPVLib` is Phase 2 (`PLAN-2-typed-api.md`, `PLAN-3-surfaces.md`,
  `PLAN-4-canvas.md`). Do not grow `MPVLib` into a player API; it is the compatibility surface
  and the typed `Mpv` class is where new capability goes.
