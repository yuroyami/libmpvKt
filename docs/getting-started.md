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
    implementation("io.github.yuroyami:libmpvkt:0.2.0")
    // Only what the app draws with:
    implementation("io.github.yuroyami:libmpvkt-view:0.2.0")      // MpvView, MpvOptions
    implementation("io.github.yuroyami:libmpvkt-compose:0.2.0")   // MpvSurface, MpvPlayer
}
```

Minimum SDK 21, and 26 for `libmpvkt-canvas`. The app needs compile SDK 35 or newer, and Kotlin targeting JVM 11 or newer. Android only. In a Kotlin Multiplatform project, declare the dependency in `androidMain.dependencies`.

An app that plays network streams declares `<uses-permission android:name="android.permission.INTERNET" />`; the library declares no permission of its own. Rotation recreates an activity, and a recreated activity closes its core, so playback starts over. Declare `android:configChanges="orientation|screenSize|screenLayout|keyboardHidden"` on the player activity, as the sample does, or keep the core somewhere that outlives the activity.

## The call order

`MpvView` and `MpvSurface` do the surface steps for you (see [Choosing a surface](choosing-a-surface.md)). This is what they do:

1. `val mpv = Mpv.create(applicationContext)`.
2. Options through `mpv.setOption(...)`, `vo=null` among them: `null` always opens, so a file loaded before the surface keeps its video. Options are read at `initialize`; after that, change behaviour through properties. `MpvOptions(...).applyTo(mpv)` sets the ones below in one call.
3. `mpv.initialize().getOrThrow()`.
4. When a `Surface` exists: `mpv.attachSurface(surface)`, which sets `wid`, `force-window=yes` and `vo=gpu`, then the size through `mpv[MpvProperties.AndroidSurfaceSize] = "${w}x$h"`.
5. `mpv.command(MpvCommands.loadFile(pathOrUrl))`. This works before the surface exists too.
6. Before the surface is destroyed: `mpv.detachSurface()`.
7. `mpv.close()`. It waits for calls still running on other threads, then for mpv to stop, which a stuck stream can make slow, so call it off the main thread.

The deprecated `MPVLib` follows the same order with its own calls; `sample/src/main/kotlin/io/github/yuroyami/libmpvkt/sample/MainActivity.kt` is that list as code.

## Options a phone player sets

`MpvOptions` sets all of these by default.

| Option | Value | Why |
|---|---|---|
| `vo` | `null` at start, `gpu` once a surface exists | `null` always opens. `gpu` is the OpenGL ES renderer; `gpu-next` also works and uses libplacebo's newer path. |
| `gpu-context` | `android` | Render into an Android surface. |
| `opengl-es` | `yes` | The context is GLES. |
| `hwdec` | `auto` | MediaCodec when the codec allows it, software otherwise. |
| `ao` | `audiotrack,opensles` | AudioTrack first, OpenSL ES as the fallback. |
| `force-window` | `no` until a surface exists | mpv aborts if asked for a window it cannot create. |
| `tls-verify`, `tls-ca-file` | `yes`, a PEM path | mpv checks no certificate by default, and Mbed TLS has no access to Android's trust store. |

## Two things every app supplies

**A CA bundle for https.** mpv checks no https certificate unless `tls-verify=yes`, and Mbed TLS has no access to Android's trust store, so checking needs a bundle. Copy a PEM bundle (the sample ships curl's Mozilla bundle as `cacert.pem`) into `filesDir`, then set `tls-verify=yes` and `tls-ca-file` to its path before `initialize`. `MpvOptions(tlsCaFile = ...)` sets both. Without them, https URLs still play, with no certificate check.

**A subtitle font.** This libass has no system font provider. Put a TrueType font at `<config-dir>/subfont.ttf`, and start mpv with `config=yes` and `config-dir=<config-dir>`. mpv configures libass fonts at playback start, so the file has to exist before the first `loadfile`.

## Reading state

`mpv.observe(MpvProperties.TimePos)` is a flow of every change of `time-pos`, and `mpv.events` is a flow of file loaded, end of file, seek and the rest; [The typed API](typed-api.md) shows both. `mpv.playback` reduces twelve properties to one state for a player screen. Collect the flows on whatever dispatcher suits you. `MPVLib`'s observers get their callbacks on the core's event thread, in mpv's order, as in 0.1.0.

`mpv.getString("mpv-version")` and `mpv.getString("ffmpeg-version")` say what is running. `BuildInfo` says the same before `create`.
