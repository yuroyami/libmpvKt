# libmpvKt

libmpv for Android as one Maven artifact: mpv, FFmpeg, libass, libplacebo and their dependencies, prebuilt for `arm64-v8a`, `armeabi-v7a`, `x86` and `x86_64`, behind a typed Kotlin API.

```kotlin
// settings.gradle.kts
maven("https://yuroyami.github.io/maven") {
    content { includeModuleByRegex("io\\.github\\.yuroyami", "libmpvkt.*") }
}
// the app
implementation("io.github.yuroyami:libmpvkt:0.1.0")
```

- [The typed API](typed-api.md): `Mpv`, the catalogs, events as flows, playback state, streams and hooks.
- [Getting started](getting-started.md): the repository, the dependency, the call order, the two things every app must supply (a CA bundle and a subtitle font).
- [Building the natives](building-natives.md): the scripts, the pins, the cache, and how CI runs them.
- [Releasing](releasing.md): how a version reaches the repository.
- [Licensing](licensing.md): what is Apache-2.0, what is GPL, and what that means for an app.
- [API reference](https://yuroyami.github.io/libmpvKt/api/): every public declaration.
