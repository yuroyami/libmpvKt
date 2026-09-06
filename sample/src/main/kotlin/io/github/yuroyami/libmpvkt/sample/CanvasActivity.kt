package io.github.yuroyami.libmpvkt.sample

import android.os.Bundle
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
import io.github.yuroyami.libmpvkt.MpvCommands
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
            LaunchedEffect(mpv) { mpv.command(MpvCommands.loadFile(SampleOptions.DEFAULT_URL)) }
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
