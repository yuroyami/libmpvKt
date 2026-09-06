package io.github.yuroyami.libmpvkt.sample

import android.content.Context
import io.github.yuroyami.libmpvkt.view.MpvOptions
import io.github.yuroyami.libmpvkt.view.SurfaceType
import java.io.File

/** The options every sample screen starts from. Only the surface type differs between them. */
object SampleOptions {
    const val DEFAULT_URL: String = "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"

    fun forApp(context: Context, surfaceType: SurfaceType = SurfaceType.Surface): MpvOptions = MpvOptions(
        configDir = context.filesDir,
        cacheDir = context.cacheDir,
        surfaceType = surfaceType,
        // mpv's TLS is Mbed TLS, which cannot see Android's trust store.
        tlsCaFile = installCaBundle(context),
    )

    private fun installCaBundle(context: Context): File {
        val dest = File(context.filesDir, "cacert.pem")
        if (!dest.exists()) {
            context.assets.open("cacert.pem").use { input -> dest.outputStream().use { input.copyTo(it) } }
        }
        return dest
    }
}
