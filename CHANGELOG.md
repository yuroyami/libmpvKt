# Changelog

All notable changes to libmpvKt are documented here.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), and the project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html). libmpvKt is pre-1.0: a minor version may change the public API, and this file says so when it happens.

## [Unreleased]

Nothing yet.

## [0.1.0] - unreleased

First release, served from `https://yuroyami.github.io/maven`.

```kotlin
implementation("io.github.yuroyami:libmpvkt:0.1.0")
```

- `MPVLib`, the mpv-android JNI surface, under `io.github.yuroyami.libmpvkt`: one core per process, options, properties, commands, observers, log observers, thumbnails.
- `BuildInfo`, the versions inside, readable before the core exists.
- mpv 0.41.0, FFmpeg 9.0.1 built with `--enable-gpl --enable-version3`, libass 0.17.5, libplacebo 7.360.1, dav1d 1.5.4, Mbed TLS 3.6.7, HarfBuzz 14.4.0, FreeType 2.14.3, FriBidi 1.0.16, libunibreak 7.0, Lua 5.2.4. Compiled with NDK 29.0.14206865 for API 21.
- Four ABIs in one AAR, every library aligned to 16 KB pages, `libc++_shared.so` included.
- Consumer keep rules, so a shrunk build needs no ProGuard lines of its own.
