# libmpvKt

libmpv for Android as one dependency: mpv, FFmpeg, libass, libplacebo and their dependencies, prebuilt for four ABIs, behind a small Kotlin wrapper.

[![Release](https://img.shields.io/github/v/release/yuroyami/libmpvKt?label=Release)](https://github.com/yuroyami/libmpvKt/releases)
[![CI](https://img.shields.io/github/actions/workflow/status/yuroyami/libmpvKt/ci.yml?label=CI)](https://github.com/yuroyami/libmpvKt/actions/workflows/ci.yml)
[![License](https://img.shields.io/badge/License-Apache%202.0%20wrapper%2C%20GPL%20binaries-blue)](NOTICE)

**[Documentation](https://yuroyami.github.io/libmpvKt/)** · [Changelog](CHANGELOG.md) · [Contributing](CONTRIBUTING.md)

## What you get

An app that wants mpv on Android has had to cross-compile mpv and eleven other projects for every ABI inside its own build. libmpvKt does that build once, in public CI, from pinned release tags, and publishes the result. You add one dependency and call `Mpv`: a typed Kotlin API where every property, command and event has a type, errors are values, and events arrive as flows.

Inside the AAR: `libmpv.so`, the seven FFmpeg libraries, `libmpvkt_jni.so` and the NDK's `libc++_shared.so`, for `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`, all aligned to 16 KB pages. Subtitles come through libass, rendering through libplacebo, AV1 through dav1d, TLS through Mbed TLS, scripting through Lua 5.2.

## A View, or Compose

```kotlin
// A View, on either surface type; SurfaceType documents which to pick and why.
val view = MpvView(context)
view.initialize(MpvOptions(surfaceType = SurfaceType.Surface))
view.playFile(url)

// Or Compose, with no View in between:
val mpv = rememberMpv(MpvOptions())
MpvSurface(mpv, Modifier.fillMaxSize())
```

The view and the Compose surfaces are separate artifacts, so an app that wants neither carries neither:

```kotlin
implementation("io.github.yuroyami:libmpvkt-view:0.2.0")      // MpvView, MpvOptions
implementation("io.github.yuroyami:libmpvkt-compose:0.2.0")   // MpvSurface, MpvPlayer
```

[Choosing a surface](docs/choosing-a-surface.md) is the whole comparison: power, effects, HDR, capture.

There is a third way to draw, experimental: `libmpvkt-canvas` renders mpv into a plain Compose image, so the video can be rotated, blurred and captured like any other drawing. [The Compose canvas](docs/compose-canvas.md) says what it costs and what has not been measured yet.

## Install

Add the repository once, in `settings.gradle.kts`:

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven("https://yuroyami.github.io/maven") {
            content { includeModuleByRegex("io\\.github\\.yuroyami", "libmpvkt.*") }
        }
    }
}
```

Then the dependency, in the app's `build.gradle.kts`:

```kotlin
dependencies {
    implementation("io.github.yuroyami:libmpvkt:0.1.0")
}
```

The repository is a static Maven repository on GitHub Pages; the artifact is not on Maven Central. Minimum SDK 21. In a Kotlin Multiplatform project the line goes in `androidMain.dependencies`. The AAR's consumer rules keep everything the JNI layer looks up by name, so a shrunk release build needs no extra ProGuard rules.

## Play a URL

```kotlin
val mpv = Mpv.create(applicationContext)
mpv.setOption(MpvProperties.Vo, VideoOutput.Gpu)
mpv.setOption("gpu-context", "android")
mpv.setOption("opengl-es", "yes")
mpv.setOption(MpvProperties.Hwdec, HwdecMode.Auto)
mpv.setOption(MpvProperties.TlsVerify, true)           // mpv's default is no: https plays unchecked
mpv.setOption(MpvProperties.TlsCaFile, caBundlePath)   // Mbed TLS cannot see Android's trust store
mpv.initialize().getOrThrow()

// When the SurfaceView has a surface:
mpv.attachSurface(holder.surface)
mpv[MpvProperties.AndroidSurfaceSize] = "${width}x$height"
mpv.command(MpvCommands.loadFile(url))

// Watch what it is doing:
mpv.playback.collect { state -> render(state.status, state.positionSeconds) }

// Before the surface goes away, and when done:
mpv.detachSurface()
mpv.close()
```

[The typed API](docs/typed-api.md) covers properties, commands, events, hooks, streams and the migration from `MPVLib`.

### Coming from mpv-android

`MPVLib` is still here, deprecated, calling `Mpv` underneath, so a 0.1.0 app moves by bumping the version:

```kotlin
MPVLib.create(applicationContext)
MPVLib.setOptionString("vo", "gpu")
MPVLib.init()
MPVLib.command(arrayOf("loadfile", url))
MPVLib.destroy()
```

## What you can call

| What you want | Call |
|---|---|
| Start and stop a core | `Mpv.create(context)`, `initialize()`, `close()`, or `use { }` |
| Give mpv a window | `attachSurface(surface)`, `detachSurface()` |
| Read or write a property | `mpv[MpvProperties.Duration].getOrNull()`, `mpv[MpvProperties.Pause] = true` |
| Set an option before init | `setOption(MpvProperties.Hwdec, HwdecMode.Auto)` |
| Run a command | `command(MpvCommands.seek(10.0))`, `commandAsync(...)` |
| Watch one property | `observe(MpvProperties.TimePos).collect { }` |
| Receive events | `events.filterIsInstance<MpvEvent.EndFile>().collect { }` |
| Follow playback | `playback.collect { it.status }` |
| Receive mpv's log | `requestLogMessages(MpvLogLevel.Info)`, then `logs.collect { }` |
| Rewrite a URL before it opens | `hook("on_load") { }` |
| Feed mpv your own bytes | `addStreamProtocol("content", ContentResolverStreamProvider(context))` |
| Ask what is inside | `BuildInfo.MPV`, `BuildInfo.FFMPEG`, `BuildInfo.ABIS` |

The catalogs name 399 properties and 71 commands. Anything not in them still works by name: `mpv.getNode("some-property")`, `mpv.command(MpvCommand.of("some-command", "arg"))`. Every property, option and command is documented in the [mpv manual](https://mpv.io/manual/stable/).

## What is inside

| Library | Version | Licence |
|---|---|---|
| mpv | 0.41.0 | GPL-2.0-or-later |
| FFmpeg | 9.0.1, built with `--enable-gpl --enable-version3` | GPL-3.0-or-later as configured |
| libass | 0.17.5 | ISC |
| libplacebo | 7.360.1 | LGPL-2.1-or-later |
| dav1d | 1.5.4 | BSD-2-Clause |
| Mbed TLS | 3.6.7 | Apache-2.0 |
| HarfBuzz | 14.4.0 | MIT |
| FreeType | 2.14.3 | FTL |
| FriBidi | 1.0.16 | LGPL-2.1-or-later |
| libunibreak | 7.0 | zlib |
| Lua | 5.2.4 | MIT |
| Android NDK | 29.0.14206865, API 21 | |

FFmpeg is built with decoders and demuxers, hardware decoding through MediaCodec, and no encoders or muxers except the ones mpv's screenshot and cache-dump features use.

## Limits

- **One core per `Mpv`, and as many as you make.** The deprecated `MPVLib` still allows only one per process. An Android View, Compose surfaces and a pure Compose renderer are in development.
- **The binaries are GPL.** An app that ships this AAR is bound by GPL-3.0-or-later for the whole app. NOTICE lists every library.
- **No system fonts.** This libass has no system font provider, so it draws nothing unless it finds a font. Put a TrueType font at `<config-dir>/subfont.ttf`, start mpv with `config=yes` and `config-dir` pointing there.
- **https is not checked by default.** mpv starts with `tls-verify=no`, so an https URL plays with no certificate check. mpv's TLS is Mbed TLS, which has no access to Android's trust store: to check certificates, ship a PEM bundle in your assets and set both `tls-verify=yes` and `tls-ca-file`. The sample does both, and so does `MpvOptions(tlsCaFile = ...)`.
- **Size.** The AAR is about 59 MB. Each ABI adds 31 to 41 MB of libraries to an APK before compression, so ship an app bundle and each phone downloads only its own ABI.
- **libc++_shared.so travels inside.** If another dependency also ships one, AGP refuses to merge them. That refusal is worth keeping: an older libc++ crashes libmpv at load. If you must pick one, `packaging { jniLibs { pickFirsts += "**/libc++_shared.so" } }` picks the first in resolution order, so make sure the winner is at least the NDK r29 copy.
- **Android only.** There is no desktop or iOS artifact.

## Building the libraries yourself

You do not have to. The AAR is assembled from the zips attached to the matching GitHub release, and every release carries the source of mpv and FFmpeg beside them. If you want to change a build flag or a version, [Building the natives](docs/building-natives.md) has the whole procedure; it needs an NDK, meson, ninja, autotools, and between ten minutes and an hour per ABI depending on the machine.

## License

The Kotlin wrapper, the JNI sources and the build scripts are Apache-2.0. The native libraries inside the published AAR are the upstream projects' own, and the combination is GPL-3.0-or-later. `MPVLib.kt` and the JNI sources derive from [mpv-android](https://github.com/mpv-android/mpv-android), MIT. See [NOTICE](NOTICE).
