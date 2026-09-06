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

`vo=libmpv` uses mpv's `gpu` renderer through the render API. Whether this build's `gpu-next` works the same way through it has not been measured yet.

## Hardware decoding

With a surface, `hwdec=mediacodec` renders straight into the window. With the render API there is no window, so mpv's image-reader interop imports each decoded frame as an `EGLImage` into the render context instead. `hwdec=auto` is expected to reach MediaCodec that way, with `mediacodec-copy` as the fallback. Expected, not measured: the table below is empty until it runs on hardware.

## Measured

Nothing yet. The numbers go here whatever they say, from a 1080p30 H.264 file played for 30 seconds on a real device.

| Run | hwdec-current | Frames per second | Dropped | Skipped by the ring |
|---|---|---|---|---|
| hardware (`HwdecMode.Auto`) | | | | |
| hardware, copy (`HwdecMode.MediacodecCopy`) | | | | |
| software (`HwdecMode.No`) | | | | |
| readback path (API 26 to 28) | | | | |

The canvas stops being called experimental when the hardware run holds at least 29 frames per second with fewer than 10 dropped frames and fewer than 30 skipped over those 30 seconds.
