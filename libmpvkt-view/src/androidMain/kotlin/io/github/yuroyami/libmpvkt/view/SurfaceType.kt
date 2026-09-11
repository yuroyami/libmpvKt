package io.github.yuroyami.libmpvkt.view

/**
 * Which Android view carries mpv's output. mpv renders into an `ANativeWindow` on its own thread
 * either way; what differs is what Android does with the pixels afterwards.
 *
 * ## [Surface]: a `SurfaceView`
 *
 * The video gets its own window, composited by the system compositor (SurfaceFlinger), which can
 * put it on a hardware overlay plane. That means:
 *
 * - **Lowest power and latency.** The app's own GPU pass never touches the video pixels; the
 *   display controller blends the plane. On a phone playing a film this is the difference that
 *   shows up in battery figures.
 * - **HDR looks the same either way.** mpv tone-maps every frame in its own renderer before either
 *   view sees it, and it has no path for protected content.
 * - **The video is a hole in the view hierarchy.** It cannot be rotated, scaled with a view
 *   animation, faded with `alpha`, clipped to rounded corners, or captured with `View.draw`. Blur
 *   and other effects that sample the app's own rendering (a frosted glass over the video) see
 *   nothing where the video is. Z-order is a surface property (`setZOrderMediaOverlay`), not the
 *   view's place in the tree.
 * - **Transitions can flash.** Creating, resizing and destroying the surface happen outside the
 *   app's frame, so a rotation or a size change can show a black frame or a stale one.
 *
 * Use it for the normal case: a player screen where the video fills a rectangle and nothing is
 * drawn over it that needs to sample it. Picture-in-picture and TV want this one.
 *
 * ## [Texture]: a `TextureView`
 *
 * The video becomes a GPU texture that the app's own renderer draws like any other view. That
 * means:
 *
 * - **It is a real view.** Alpha, scale, rotation, translation, view animations, rounded corners
 *   through the outline, scrolling inside a list, and being drawn into a bitmap all work. Effects
 *   that sample the app's rendering can sample the video; this is what a blur over the video
 *   needs.
 * - **It costs a copy per frame.** Every frame is composited by the app's GPU pass in addition to
 *   the system's, with about one frame of extra latency and more memory bandwidth, which is more
 *   power. On a 4K stream on a mid-range phone this is measurable.
 * - **The window must be hardware accelerated** (the default), or the view never gets a surface.
 *
 * Use it when the video is part of a composition: transformed, animated, blurred, shown in a
 * list, or captured. If none of that applies, use [Surface].
 *
 * ## Both
 *
 * mpv needs the surface size on every change (`android-surface-size`), needs `vo` switched to
 * `null` before the surface is destroyed (or it renders into a dead window and stops), and must
 * not be asked for a window before one exists (`force-window=no` until the surface is created).
 * [MpvView] and `MpvSurface` do all three for either type; the type only changes which view is
 * created and whether the [android.view.Surface] object is the app's to release.
 */
public enum class SurfaceType { Surface, Texture }
