package io.github.yuroyami.libmpvkt.sample

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.github.yuroyami.libmpvkt.MpvCommands
import io.github.yuroyami.libmpvkt.compose.MpvPlayer
import io.github.yuroyami.libmpvkt.compose.MpvSurface
import io.github.yuroyami.libmpvkt.compose.rememberMpv
import io.github.yuroyami.libmpvkt.view.SurfaceType

/** mpv in Compose with no View in between, and a switch between the two surface types. */
class ComposeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var surfaceType by remember { mutableStateOf(SurfaceType.Surface) }
            val mpv = rememberMpv(SampleOptions.forApp(this))
            LaunchedEffect(mpv) { mpv.command(MpvCommands.loadFile(SampleOptions.DEFAULT_URL)) }
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                MpvSurface(mpv, Modifier.fillMaxSize(), surfaceType = surfaceType)
                Panel(
                    text = "$surfaceType: the blur below shows the video only in Texture mode",
                    buttonText = "Switch surface",
                    onClick = {
                        surfaceType = if (surfaceType == SurfaceType.Surface) SurfaceType.Texture else SurfaceType.Surface
                    },
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
        }
    }
}

/** The same picture through MpvView, for an app that wants the view's conveniences. */
class ComposePlayerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Box(Modifier.fillMaxSize().background(Color.Black)) {
                MpvPlayer(
                    modifier = Modifier.fillMaxSize(),
                    options = SampleOptions.forApp(this@ComposePlayerActivity),
                    onReady = { it.playFile(SampleOptions.DEFAULT_URL) },
                )
            }
        }
    }
}

@Composable
private fun Panel(text: String, buttonText: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier
            .fillMaxWidth()
            .height(140.dp)
            .blur(16.dp)
            .background(Color.White.copy(alpha = 0.4f))
            .padding(12.dp),
    ) {
        Text(text)
        Button(onClick = onClick) { Text(buttonText) }
    }
}
