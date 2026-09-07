# The Compose canvas

Experimental. `MpvCanvas` draws mpv's output as an ordinary Compose image, with no Android view and no surface behind it. Anything Compose can do to a drawing it can do to the video: blur it, rotate it, animate it, clip it, put it in a graphics layer, capture it.

```kotlin
val mpv = rememberMpv(MpvOptions.forCanvas())
val renderer = rememberMpvRenderer(mpv)
LaunchedEffect(mpv) { mpv.command(MpvCommands.loadFile(url)) }

MpvCanvas(renderer, Modifier.fillMaxSize().graphicsLayer { rotationZ = angle })
```

`MpvOptions.forCanvas()` is the whole difference in configuration: it sets `vo=libmpv` and leaves out the window context options, which do not apply when there is no window.

## How a frame gets there

```text
mpv (vo=libmpv) --update callback--> render thread: mpv_render_context_update()
                                            | MPV_RENDER_UPDATE_FRAME
                                            v
                               FBO[slot] backed by EGLImage <-- AHardwareBuffer[slot]
                                            | mpv_render_context_render, glFinish, report_swap
                                            v
                               FrameRing.publish(slot) --> frameNumber (Compose State)
                                            v
                               MpvCanvas draws ImageBitmap[slot]
                                 (API 29+: Bitmap.wrapHardwareBuffer, no copy)
                                 (API 26..28: glReadPixels into an ARGB_8888 Bitmap)
```

Three slots: one Compose is drawing, one published and waiting, one free for the next render. The ring never hands the renderer the slot Compose is showing.

## What it costs

- **One extra GPU pass**, the same as a `TextureView`. The video is composited by the app, not by a hardware overlay plane.
- **A CPU copy per frame below API 29**, because `Bitmap.wrapHardwareBuffer` starts there. `MpvRendererStats.readback` says which path a device is on.
- **Three frames of memory at canvas size**, not at video size: the renderer renders to the size the canvas asks for.

## When a surface is the better choice

A full-screen player that draws nothing over the video should use `MpvView` or `MpvSurface` with `SurfaceType.Surface`: lower power, lower latency, HDR and protected content on the system path. [Choosing a surface](choosing-a-surface.md) compares them. The canvas earns its cost when the video has to be transformed, blurred, captured, or drawn inside a composition that moves.

## Renderer

`vo=libmpv` uses mpv's `gpu` renderer through the render API, and that is the only renderer the canvas has.

`gpu-next` does not drive it. Asked for `vo=gpu-next` with a render context attached, mpv ends up with no video output at all (`current-vo` reads null) and not a single frame arrives. Measured on Android 15 and Android 9. If you want libplacebo's newer renderer, use a surface, not the canvas.

## Hardware decoding

With a surface, `hwdec=mediacodec` renders straight into the window. With the render API there is no window, so mpv's image-reader interop imports each decoded frame as an `EGLImage` into the render context instead. `hwdec=auto` reaches MediaCodec, but as `mediacodec-copy` rather than the zero-copy path: with a render context and no window, that is what mpv picks. So a frame is copied out of the decoder before it reaches the renderer, on top of the canvas's own composition cost. Measured on an Android 15 emulator; a real device may choose differently, and the phone run below will say.

## Measured

The runs below are a 320x240 test pattern for four seconds on GitHub's emulators, which is what
CI can reach. They answer what the renderer does, not how fast it is on a phone: an emulator's
frame rate says nothing about an ASUS.

| Runtime | Path | hwdec-current | Frames | Rendered fps | Dropped |
|---|---|---|---|---|---|
| Android 15 | zero copy (`wrapHardwareBuffer`) | `mediacodec-copy` | 60 | 15.0 | 1 |
| Android 15 | zero copy, `hwdec=no` | `no` | 60 | 15.0 | 1 |
| Android 9 | readback (`glReadPixels`) | `no` | 60 | 15.0 | 1 |
| Android 9 | readback, `hwdec=no` | `no` | 59 | 14.8 | 2 |
| Android 15 or 9 | `vo=gpu-next` | none | 0 | 0 | no video output at all |

What this says: the pipeline works on both paths, the readback path costs nothing measurable on a
320x240 picture, and hardware decoding through the render API lands on `mediacodec-copy`. The
renderer skips almost every frame in these runs because nothing is composing: with no `MpvCanvas`
on screen, no slot is ever taken for display, which is the ring doing its job rather than a fault.

**Still missing, and the reason this module is experimental:** a 1080p30 file on a real phone. The
canvas stops being called experimental when that run holds at least 29 frames per second with
fewer than 10 dropped frames over 30 seconds. An emulator cannot answer that.

That run is one command with a device plugged in:

```bash
scripts/measure-canvas.sh /path/to/a-1080p30.mp4
```

It builds and installs the sample, plays the file on the canvas screen for 30 seconds, and prints
what the renderer and mpv report, one line a second.
