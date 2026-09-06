# Changelog

All notable changes to libmpvKt are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). libmpvKt is pre-1.0: a minor version may change the public API, and this file says so when it happens.

## [Unreleased]

Nothing yet.

## [0.2.0] - unreleased

The typed Kotlin API. `MPVLib` still works, so a 0.1.0 app moves by bumping the version.

- `Mpv`: one class per mpv core, as many cores per process as you make. Properties through `mpv[key]`, commands through `command`, events and logs as flows, hooks, and `close` that ends the event thread with the core.
- The catalogs: 402 properties and 71 commands with the type mpv gives each one, 55 enums for the options that take a fixed set of strings, and typed values for tracks, chapters, the playlist, audio and video parameters, the demuxer cache and the rest. A device test checks every name against mpv's own `property-list` and `command-list`.
- `playback`: the twelve properties a player screen watches, reduced to one state with a status of Idle, Loading, Playing, Paused, Buffering or Ended.
- Stream providers: an app can answer for a URI scheme itself, so a `content://` file plays through `ContentResolverStreamProvider` without a path mpv can open.
- The module split: `libmpvkt-native` holds the libraries and the raw binding, `libmpvkt` the API. An app still names `io.github.yuroyami:libmpvkt` and gets both.
- `MPVLib` is deprecated and calls `Mpv` underneath.
- `libmpvkt-view`: `MpvView`, an Android View that carries mpv's output, with `SurfaceType` documenting the SurfaceView or TextureView choice where you make it, and `MpvOptions`, the startup options with a type each.
- `libmpvkt-compose`: `MpvSurface`, mpv in Compose with no View in between, and `MpvPlayer` for apps that want the view inside a composition. Compose is a floor, not a pin: Gradle raises it to whatever the app uses.
- `libmpvkt-canvas`, experimental: `MpvCanvas` draws mpv through mpv's own render API into a ring of hardware buffers, so the video is an ordinary Compose image that can be rotated, blurred or captured. Zero copies from API 29; a readback below it. Nothing about its speed has been measured on hardware yet, and the docs say so.
- The JNI symbols moved with the binding: they are `Java_io_github_yuroyami_libmpvkt_jni_MpvNative_*` now. Only an app that looked the old ones up by hand would notice.

## [0.1.0] - 2026-09-06

First release, served from `https://yuroyami.github.io/maven`.

```kotlin
implementation("io.github.yuroyami:libmpvkt:0.1.0")
```

- `MPVLib`, the mpv-android JNI surface, under `io.github.yuroyami.libmpvkt`: one core per process, options, properties, commands, observers, log observers, thumbnails.
- `BuildInfo`, the versions inside, readable before the core exists.
- mpv 0.41.0, FFmpeg 9.0.1 built with `--enable-gpl --enable-version3`, libass 0.17.5, libplacebo 7.360.1, dav1d 1.5.4, Mbed TLS 3.6.7, HarfBuzz 14.4.0, FreeType 2.14.3, FriBidi 1.0.16, libunibreak 7.0, Lua 5.2.4. Compiled with NDK 29.0.14206865 for API 21.
- Four ABIs in one AAR, every library aligned to 16 KB pages, `libc++_shared.so` included.
- Consumer keep rules, so a shrunk build needs no ProGuard lines of its own.
