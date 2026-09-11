# Choosing a surface

mpv draws into an Android surface either way. What differs is what Android does with the pixels afterwards, and that decides power, latency, what you can draw over the video, and what content is allowed at all.

## The two choices

| | `SurfaceType.Surface` (SurfaceView) | `SurfaceType.Texture` (TextureView) |
|---|---|---|
| Who composites it | The system compositor, often on a hardware overlay plane | Your app's own GPU pass, then the system |
| Power and latency | Lowest. The app never touches the video pixels | One extra copy per frame, about one frame of latency, more memory bandwidth |
| Transforms and animation | No. It is a hole in the view hierarchy | Yes: alpha, scale, rotation, view animations, rounded corners |
| Effects that sample the video (blur, glass) | See nothing where the video is | Work, because the video is a texture in your own rendering |
| Capture (`View.draw`, screenshots of the hierarchy) | Empty | Works |
| HDR and DRM | mpv tone-maps HDR in its own renderer and has no DRM path | The same |
| Z-order | A surface property (`setZOrderMediaOverlay`), not the view tree | The view's place in the tree, like any other view |
| Resize and rotation | Can show a black or stale frame for a moment | Follows the view |
| Requires | Nothing | A hardware accelerated window (the default) |

## The rule

Use `Surface` unless you transform, blur, animate, list or capture the video. Then use `Texture`.

A full-screen player with controls beside the video wants `Surface`, and so do picture-in-picture and TV. A video in a scrolling list, a video that shrinks into a corner, or a player with a frosted panel over the picture wants `Texture`.

## In a View

```kotlin
val view = MpvView(context)
view.initialize(MpvOptions(surfaceType = SurfaceType.Texture))
view.playFile(url)
```

## In Compose

```kotlin
val mpv = rememberMpv(MpvOptions())
MpvSurface(mpv, Modifier.fillMaxSize(), surfaceType = SurfaceType.Texture)
```

`MpvSurface` uses Compose Foundation's own surface composables, so there is no `AndroidView` in between: `SurfaceType.Surface` is `AndroidExternalSurface`, `SurfaceType.Texture` is `AndroidEmbeddedExternalSurface`. `zOrder` applies to the first only, because a TextureView's place in the tree is its z-order; `isOpaque` applies to both.

`MpvPlayer` is the other Compose entry point: it hosts `MpvView` in an `AndroidView`, for an app that wants the view's conveniences (`paused`, `seek`, `tracks`) rather than a bare surface.

## What both do for you

mpv needs three things right, and getting any of them wrong is a black picture rather than an error:

- The surface size on every change, through `android-surface-size`.
- `vo` set back to `null` before the surface is destroyed, or the renderer draws into a dead window.
- No window before one exists: `force-window=no` until the surface is created, or mpv aborts.

`MpvView` and `MpvSurface` both do all three, through the same code. Only the attached surface can resize or detach, so an old surface destroyed during a screen change leaves the new one alone. The sample shows the difference between the two types on a device: the panel at the bottom of each screen is blurred, and the video shows through it only in `Texture` mode.
