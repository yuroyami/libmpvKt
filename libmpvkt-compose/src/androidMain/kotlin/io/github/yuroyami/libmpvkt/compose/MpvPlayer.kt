package io.github.yuroyami.libmpvkt.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import io.github.yuroyami.libmpvkt.view.MpvOptions
import io.github.yuroyami.libmpvkt.view.MpvView

/**
 * [MpvView] inside a composition, for an app that wants the view's conveniences (`paused`,
 * `seek`, `tracks`) rather than a bare surface. [onReady] receives the view once, after it has
 * started its core. The view is destroyed when the composition leaves.
 *
 * Use [MpvSurface] instead when the app drives an [io.github.yuroyami.libmpvkt.Mpv] itself: it
 * needs no View at all.
 */
@Composable
public fun MpvPlayer(
    modifier: Modifier = Modifier,
    options: MpvOptions = MpvOptions(),
    onReady: (MpvView) -> Unit = {},
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            MpvView(context).also { view ->
                view.initialize(options)
                onReady(view)
            }
        },
        onRelease = { it.destroy() },
    )
}
