# Getting started

## The repository and the dependency

The artifacts live in a static Maven repository on GitHub Pages, not on Maven Central. Declare it once, in `settings.gradle.kts`, with a filter so every other `io.github.yuroyami` artifact keeps coming from Central:

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

Minimum SDK 21. Android only. In a Kotlin Multiplatform project, declare the dependency in `androidMain.dependencies`.

## The call order

1. `MPVLib.create(applicationContext)`.
2. `MPVLib.setOptionString(...)` for every option. Options are read at init; after that, change behaviour through properties.
3. `MPVLib.init()`.
4. When a `Surface` exists: `attachSurface`, then `force-window=yes`, then `vo=gpu`, then tell mpv the size through the `android-surface-size` property.
5. `MPVLib.command(arrayOf("loadfile", pathOrUrl))`.
6. Before the surface is destroyed: `vo=null`, `force-window=no`, `detachSurface`.
7. `MPVLib.destroy()` with no surface attached.

`sample/src/main/kotlin/io/github/yuroyami/libmpvkt/sample/MainActivity.kt` is this list as code.

## Options the sample sets

| Option | Value | Why |
|---|---|---|
| `vo` | `gpu` | The OpenGL ES renderer. `gpu-next` also works and uses libplacebo's newer path. |
| `gpu-context` | `android` | Render into an Android surface. |
| `opengl-es` | `yes` | The context is GLES. |
| `hwdec` | `auto` | MediaCodec when the codec allows it, software otherwise. |
| `ao` | `audiotrack,opensles` | AudioTrack first, OpenSL ES as the fallback. |
| `force-window` | `no` until a surface exists | mpv aborts if asked for a window it cannot create. |
| `tls-verify`, `tls-ca-file` | `yes`, a PEM path | mpv checks no certificate by default, and Mbed TLS has no access to Android's trust store. |

## Two things every app supplies

**A CA bundle for https.** mpv checks no https certificate unless `tls-verify=yes`, and Mbed TLS has no access to Android's trust store, so checking needs a bundle. Copy a PEM bundle (the sample ships curl's Mozilla bundle as `cacert.pem`) into `filesDir`, then set `tls-verify=yes` and `tls-ca-file` to its path before `init`. Without them, https URLs still play, with no certificate check.

**A subtitle font.** This libass has no system font provider. Put a TrueType font at `<config-dir>/subfont.ttf`, and start mpv with `config=yes` and `config-dir=<config-dir>`. mpv configures libass fonts at playback start, so the file has to exist before the first `loadfile`.

## Reading state

`observeProperty("time-pos", MPVLib.MpvFormat.MPV_FORMAT_DOUBLE)` plus an `EventObserver` registered with `addObserver` delivers every change of `time-pos` to `eventProperty(property, value: Double)`. `event(eventId)` delivers file loaded, end of file, seek and the other `MpvEvent` values. All of it arrives on mpv's event thread.

`getPropertyString("mpv-version")` and `getPropertyString("ffmpeg-version")` say what is running. `BuildInfo` says the same before `create`.
