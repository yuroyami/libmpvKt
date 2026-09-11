package io.github.yuroyami.libmpvkt.sample

import android.app.Activity
import android.graphics.RenderEffect
import android.graphics.Shader
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import io.github.yuroyami.libmpvkt.view.MpvView
import io.github.yuroyami.libmpvkt.view.SurfaceType

/**
 * One screen per surface type. The panel over the video is blurred: over a TextureView it blurs
 * the video, over a SurfaceView it blurs nothing, which is the difference SurfaceType documents.
 */
abstract class SurfaceActivityBase(private val surfaceType: SurfaceType) : Activity() {

    private lateinit var view: MpvView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this)
        view = MpvView(this)
        root.addView(view, FrameLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        // The video runs under the system bars; the panel keeps its controls above the navigation bar.
        root.addView(blurredPanel().apply { padForSystemBars(top = false) }, FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT, Gravity.BOTTOM))
        setContentView(root)

        view.initialize(SampleOptions.forApp(this, surfaceType))
        view.playFile(SampleOptions.DEFAULT_URL)
    }

    /** A half-transparent panel with a blur behind it, which only a TextureView can feed. */
    private fun blurredPanel(): View = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setBackgroundColor(0x66FFFFFF)
        alpha = 0.85f
        addView(
            TextView(this@SurfaceActivityBase).apply {
                text = "$surfaceType: the blur behind this panel shows the video only over a TextureView"
            },
            LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT),
        )
        addView(
            Button(this@SurfaceActivityBase).apply {
                text = "Pause"
                setOnClickListener { view.paused = !view.paused }
            },
            LinearLayout.LayoutParams(WRAP_CONTENT, WRAP_CONTENT),
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            setRenderEffect(RenderEffect.createBlurEffect(24f, 24f, Shader.TileMode.CLAMP))
        }
    }

    override fun onDestroy() {
        view.destroy()
        super.onDestroy()
    }
}

/** mpv on a SurfaceView: the cheap path, and the video cannot be sampled by anything above it. */
class ViewActivity : SurfaceActivityBase(SurfaceType.Surface)

/** mpv on a TextureView: a real view, so the panel above it can blur the video. */
class TextureActivity : SurfaceActivityBase(SurfaceType.Texture)
