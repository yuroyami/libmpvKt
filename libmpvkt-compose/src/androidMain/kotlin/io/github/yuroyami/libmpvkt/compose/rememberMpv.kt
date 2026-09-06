package io.github.yuroyami.libmpvkt.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.yuroyami.libmpvkt.Mpv
import io.github.yuroyami.libmpvkt.getOrThrow
import io.github.yuroyami.libmpvkt.view.MpvOptions

/** One initialised core for the life of the composition; closed when it leaves. */
@Composable
public fun rememberMpv(options: MpvOptions = MpvOptions()): Mpv {
    val context = LocalContext.current.applicationContext
    val mpv = remember { Mpv.create(context).also { options.applyTo(it); it.initialize().getOrThrow() } }
    DisposableEffect(mpv) { onDispose { mpv.close() } }
    return mpv
}
