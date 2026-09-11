package io.github.yuroyami.libmpvkt.sample

import android.os.Build
import android.view.View
import android.view.WindowInsets

/**
 * Pads this view by the system bars, which Android 15 and later draw over an app that targets SDK 35
 * or higher. On older versions the window already sits between the bars, and the insets are zero.
 */
internal fun View.padForSystemBars(top: Boolean = true) {
    setOnApplyWindowInsetsListener { view, insets ->
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bars = insets.getInsets(WindowInsets.Type.systemBars())
            view.setPadding(bars.left, if (top) bars.top else 0, bars.right, bars.bottom)
        } else {
            @Suppress("DEPRECATION")
            view.setPadding(
                insets.systemWindowInsetLeft,
                if (top) insets.systemWindowInsetTop else 0,
                insets.systemWindowInsetRight,
                insets.systemWindowInsetBottom,
            )
        }
        insets
    }
}
