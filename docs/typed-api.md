# The typed API

`Mpv` is the class an app uses. Every property, command and event has a Kotlin type, errors come back as values, and events arrive as flows. The older `MPVLib` still works and is deprecated.

## Creating and closing

One `Mpv` is one mpv core. Several can exist in one process.

```kotlin
val mpv = Mpv.create(context)
mpv.setOption(MpvProperties.Vo, VideoOutput.Null)  // opens with no window; attachSurface sets gpu
mpv.setOption("gpu-context", "android")
mpv.setOption("opengl-es", "yes")
mpv.setOption(MpvProperties.Hwdec, HwdecMode.Auto)
mpv.initialize().getOrThrow()
// ... play something ...
mpv.close()
```

`Mpv` is `AutoCloseable`, so `use { }` closes it on the way out. Options are read at `initialize`; after that the same keys work as properties.

## Reading and writing properties

```kotlin
mpv[MpvProperties.Pause] = true
val duration: Double? = mpv[MpvProperties.Duration].getOrNull()
val tracks: List<MpvTrack> = mpv[MpvProperties.TrackList].getOrNull().orEmpty()
```

The index operator returns an `MpvResult`: `getOrNull()` for the value, `getOrThrow()` to fail loudly, or a `when` over `Ok` and `Fail` when the error matters. A property mpv has no value for (no file loaded, for example) is a `Fail` with `MpvError.PROPERTY_UNAVAILABLE`.

## Watching a property change

```kotlin
lifecycleScope.launch {
    mpv.observe(MpvProperties.TimePos).collect { seconds ->
        if (seconds != null) updateSeekBar(seconds)
    }
}
```

The flow starts with the current value and is conflated: a slow collector sees the newest value, never a backlog. It emits null while mpv reports the property unavailable.

## Commands

```kotlin
mpv.command(MpvCommands.loadFile(url))
mpv.command(MpvCommands.seek(10.0))
mpv.command(MpvCommands.seek(90.0, SeekMode.Absolute, SeekPrecision.Exact))

// A command that takes a while, without blocking the caller:
val result = mpv.commandAsync(MpvCommands.screenshotRaw())
```

`MpvCommands` names every command with the arguments the mpv manual lists. Anything not in the catalog goes through `mpv.command(MpvCommand.of("some-command", "arg"))`.

## Events

```kotlin
lifecycleScope.launch {
    mpv.events.filterIsInstance<MpvEvent.EndFile>().collect { end ->
        if (end.reason == EndFileReason.Error) showError(end.fileError)
    }
}
```

`events` has no replay. Subscribe before the command that produces the event, or the event is gone.

## Playback state

Twelve properties reduced to one value, which is usually what a player screen wants:

```kotlin
mpv.playback.collect { state ->
    when (state.status) {
        MpvPlaybackState.Status.Buffering -> showSpinner(state.bufferingPercent)
        MpvPlaybackState.Status.Playing -> showControls(state.positionSeconds, state.durationSeconds)
        else -> Unit
    }
}
```

## Logs

```kotlin
mpv.requestLogMessages(MpvLogLevel.Info)
lifecycleScope.launch { mpv.logs.collect { Log.i("mpv", "${it.prefix}: ${it.text}") } }
```

## Hooks

A hook pauses mpv at a point in its own work until the handler returns, which is how a file's URL is rewritten before it opens:

```kotlin
mpv.hook("on_load") {
    val url = mpv[MpvProperties.StreamOpenFilename].getOrNull()
    if (url != null && url.startsWith("myscheme://")) {
        mpv[MpvProperties.StreamOpenFilename] = resolve(url)
    }
}
```

## Feeding mpv your own bytes

A stream provider answers for one URI scheme, so a file the app can open but mpv cannot gets played anyway:

```kotlin
mpv.addStreamProtocol("content", ContentResolverStreamProvider(context))
mpv.command(MpvCommands.loadFile("content://media/external/video/media/42"))
```

`ContentResolverStreamProvider` ships with the library. Any other source is an `MpvStreamProvider` of your own: `open` returns an `MpvStream` with `read`, `seek`, `size` and `close`.

## The window

```kotlin
override fun surfaceCreated(holder: SurfaceHolder) {
    mpv.attachSurface(holder.surface, VideoOutput.Gpu.value)
}

override fun surfaceDestroyed(holder: SurfaceHolder) {
    mpv.detachSurface()
}
```

`attachSurface` sets `wid`, `force-window` and `vo` together; `detachSurface` puts `vo` back to `null` before the surface dies. Tell mpv about a size change through `mpv[MpvProperties.AndroidSurfaceSize] = "${w}x$h"`.

## Errors

```kotlin
when (val r = mpv[MpvProperties.Duration]) {
    is MpvResult.Ok -> use(r.value)
    is MpvResult.Fail -> if (r.error == MpvError.PROPERTY_UNAVAILABLE) waitForFile() else report(r)
}
```

`MpvError` is the `mpv_error` table as an enum. `getOrThrow()` throws `MpvException` carrying the same error.

## Coming from MPVLib

`MPVLib` still compiles and still works; it now calls `Mpv` underneath. The moves are one line each:

| MPVLib | Mpv |
|---|---|
| `MPVLib.create(context)` | `val mpv = Mpv.create(context)` |
| `MPVLib.init()` | `mpv.initialize().getOrThrow()` |
| `MPVLib.destroy()` | `mpv.close()` |
| `MPVLib.setOptionString("vo", "gpu")` | `mpv.setOption(MpvProperties.Vo, VideoOutput.Gpu)` |
| `MPVLib.setPropertyBoolean("pause", true)` | `mpv[MpvProperties.Pause] = true` |
| `MPVLib.getPropertyDouble("duration")` | `mpv[MpvProperties.Duration].getOrNull()` |
| `MPVLib.command(arrayOf("loadfile", url))` | `mpv.command(MpvCommands.loadFile(url))` |
| `MPVLib.observeProperty("time-pos", MPV_FORMAT_DOUBLE)` plus an observer | `mpv.observe(MpvProperties.TimePos).collect { }` |
| `MPVLib.addLogObserver { }` | `mpv.logs.collect { }` |
| `MPVLib.attachSurface(surface)` | `mpv.attachSurface(surface)` |

The one difference that is not a rename: `MPVLib` allows one core per process, `Mpv` allows as many as you make.
