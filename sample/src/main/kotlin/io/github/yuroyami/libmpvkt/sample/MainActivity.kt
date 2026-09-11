package io.github.yuroyami.libmpvkt.sample

import android.app.Activity
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import io.github.yuroyami.libmpvkt.MPVLib
import java.io.File

/** Plays one URL through libmpv on a SurfaceView. Everything a consumer has to do is in this file. */
class MainActivity : Activity(), SurfaceHolder.Callback {

    private lateinit var url: EditText
    private var surfaceReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        url = EditText(this).apply { setText(DEFAULT_URL) }
        val play = Button(this).apply {
            text = "Play"
            setOnClickListener { play() }
        }
        val surface = SurfaceView(this).apply { holder.addCallback(this@MainActivity) }
        root.addView(url, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(play, LinearLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT))
        root.addView(surface, LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT))
        setContentView(root)

        // 1. Create the core and set options. Options are read at init; properties can change later.
        MPVLib.create(applicationContext)
        MPVLib.setOptionString("config", "no")
        MPVLib.setOptionString("vo", "null") // opens with no window; the surface sets gpu
        MPVLib.setOptionString("gpu-context", "android")
        MPVLib.setOptionString("opengl-es", "yes")
        MPVLib.setOptionString("hwdec", "auto")
        MPVLib.setOptionString("ao", "audiotrack,opensles")
        // No window until a surface exists, or mpv aborts.
        MPVLib.setOptionString("force-window", "no")
        MPVLib.setOptionString("idle", "yes")
        // mpv's TLS is Mbed TLS, which cannot see Android's trust store: hand it a CA bundle.
        MPVLib.setOptionString("tls-verify", "yes")
        MPVLib.setOptionString("tls-ca-file", installCaBundle().absolutePath)
        // 2. Start it.
        MPVLib.init()
    }

    private fun play() {
        if (!surfaceReady) return
        MPVLib.command(arrayOf("loadfile", url.text.toString()))
    }

    // 3. Hand mpv the window, then let it draw.
    override fun surfaceCreated(holder: SurfaceHolder) {
        MPVLib.attachSurface(holder.surface)
        MPVLib.setOptionString("force-window", "yes")
        MPVLib.setPropertyString("vo", "gpu")
        surfaceReady = true
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        MPVLib.setPropertyString("android-surface-size", "${width}x$height")
    }

    // 4. Stop drawing before the window goes away.
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        MPVLib.setPropertyString("vo", "null")
        MPVLib.setOptionString("force-window", "no")
        MPVLib.detachSurface()
    }

    // 5. Destroy the core with no surface attached.
    override fun onDestroy() {
        MPVLib.destroy()
        super.onDestroy()
    }

    private fun installCaBundle(): File {
        val dest = File(filesDir, "cacert.pem")
        if (!dest.exists()) {
            assets.open("cacert.pem").use { input -> dest.outputStream().use { input.copyTo(it) } }
        }
        return dest
    }

    private companion object {
        const val DEFAULT_URL = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
    }
}
