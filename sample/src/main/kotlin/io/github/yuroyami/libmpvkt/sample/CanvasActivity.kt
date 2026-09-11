package io.github.yuroyami.libmpvkt.sample

import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Slider
import androidx.compose.material.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import io.github.yuroyami.libmpvkt.MpvCommands
import io.github.yuroyami.libmpvkt.MpvProperties
import io.github.yuroyami.libmpvkt.getOrNull
import io.github.yuroyami.libmpvkt.canvas.MpvCanvas
import io.github.yuroyami.libmpvkt.canvas.rememberMpvRenderer
import io.github.yuroyami.libmpvkt.compose.rememberMpv
import io.github.yuroyami.libmpvkt.view.MpvOptions

/**
 * mpv drawn as a plain Compose image: rotated by a slider, and blurred under a panel. Neither is
 * possible over a SurfaceView, which is the point of the canvas renderer.
 */
class CanvasActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var rotation by remember { mutableFloatStateOf(0f) }
            val mpv = rememberMpv(MpvOptions.forCanvas().copy(configDir = filesDir, cacheDir = cacheDir))
            val renderer = rememberMpvRenderer(mpv)
            val stats by renderer.stats.collectAsState()
            // scripts/measure-canvas.sh passes a local file; without one the sample plays its URL.
            val target = intent?.getStringExtra("video") ?: SampleOptions.DEFAULT_URL
            LaunchedEffect(mpv) { mpv.command(MpvCommands.loadFile(target)) }
            // One line a second, in the shape the measuring script reads.
            LaunchedEffect(renderer) {
                var lastShown = 0L
                while (true) {
                    delay(1000)
                    val s = renderer.stats.value
                    // The speed is frames shown: a frame published over one Compose never drew is skipped, not shown.
                    val shown = s.framesRendered - s.framesSkipped
                    Log.i(
                        "libmpvKt",
                        "CANVAS-MEASUREMENT api=${Build.VERSION.SDK_INT} run=sample ok=${shown > lastShown} " +
                            "readback=${s.readback} size=${s.width}x${s.height} frames=${s.framesRendered} " +
                            "shown=$shown fps=${shown - lastShown} skipped=${s.framesSkipped} " +
                            "hwdec=${mpv[MpvProperties.HwdecCurrent].getOrNull()} " +
                            "dropped=${mpv[MpvProperties.FrameDropCount].getOrNull()}",
                    )
                    lastShown = shown
                }
            }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                MpvCanvas(renderer, Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation })
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .blur(16.dp)
                        .background(Color.White.copy(alpha = 0.35f))
                        .padding(12.dp),
                ) {
                    Text("${stats.width}x${stats.height}, ${stats.framesRendered} frames, ${stats.framesSkipped} skipped, readback ${stats.readback}")
                    Slider(value = rotation, onValueChange = { rotation = it }, valueRange = 0f..360f)
                }
            }
        }
    }
}
