package io.github.yuroyami.libmpvkt.sample

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ScrollView

/** What the launcher opens: one button per screen. Each screen runs in its own process, with its own core. */
class LauncherActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val list = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        for ((title, screen) in SCREENS) {
            list.addView(
                Button(this).apply {
                    text = title
                    setOnClickListener { startActivity(Intent(this@LauncherActivity, screen)) }
                },
                LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
            )
        }
        setContentView(ScrollView(this).apply { addView(list); padForSystemBars() })
    }

    private companion object {
        val SCREENS = listOf(
            "MPVLib, the 0.1.0 API, on a SurfaceView" to MainActivity::class.java,
            "MpvView on a SurfaceView" to ViewActivity::class.java,
            "MpvView on a TextureView" to TextureActivity::class.java,
            "MpvSurface in Compose" to ComposeActivity::class.java,
            "MpvPlayer in Compose" to ComposePlayerActivity::class.java,
            "MpvCanvas: the video as a Compose image" to CanvasActivity::class.java,
        )
    }
}
