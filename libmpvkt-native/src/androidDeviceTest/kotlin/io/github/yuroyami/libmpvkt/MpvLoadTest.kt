package io.github.yuroyami.libmpvkt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith

/** Loads the real libraries on a device or emulator. This is the only proof that the AAR works. */
@RunWith(AndroidJUnit4::class)
class MpvLoadTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A core with no window and no audio device, so it runs on any emulator. */
    private fun startCore() {
        MPVLib.create(context)
        MPVLib.setOptionString("config", "no")
        MPVLib.setOptionString("vo", "null")
        MPVLib.setOptionString("ao", "null")
        MPVLib.setOptionString("idle", "yes")
        MPVLib.init()
    }

    @Test
    fun theCoreStartsAndReportsThePinnedVersions() {
        startCore()
        try {
            val mpv = assertNotNull(MPVLib.getPropertyString("mpv-version"))
            assertTrue(BuildInfo.MPV in mpv, "mpv reports '$mpv' but BuildInfo says ${BuildInfo.MPV}")
            val ffmpeg = assertNotNull(MPVLib.getPropertyString("ffmpeg-version"))
            assertTrue(BuildInfo.FFMPEG in ffmpeg, "FFmpeg reports '$ffmpeg' but BuildInfo says ${BuildInfo.FFMPEG}")
        } finally {
            MPVLib.destroy()
        }
    }

    @Test
    fun aWavFilePlaysToTheEnd() {
        val wav = File(context.cacheDir, "silence.wav").apply { writeBytes(silentWav(seconds = 1)) }
        val loaded = CountDownLatch(1)
        val ended = CountDownLatch(1)
        val observer = object : MPVLib.EventObserver {
            override fun eventProperty(property: String) {}
            override fun eventProperty(property: String, value: Long) {}
            override fun eventProperty(property: String, value: Boolean) {}
            override fun eventProperty(property: String, value: String) {}
            override fun eventProperty(property: String, value: Double) {}
            override fun event(eventId: Int) {
                when (eventId) {
                    MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED -> loaded.countDown()
                    MPVLib.MpvEvent.MPV_EVENT_END_FILE -> ended.countDown()
                }
            }
        }
        startCore()
        MPVLib.addObserver(observer)
        try {
            MPVLib.command(arrayOf("loadfile", wav.absolutePath))
            assertTrue(loaded.await(15, TimeUnit.SECONDS), "the file never loaded")
            assertTrue(ended.await(15, TimeUnit.SECONDS), "playback never reached the end")
        } finally {
            MPVLib.removeObserver(observer)
            MPVLib.destroy()
        }
    }

    /** A valid 8 kHz mono 16-bit PCM WAV of silence: a 44 byte header and zeroed samples. */
    private fun silentWav(seconds: Int): ByteArray {
        val sampleRate = 8000
        val dataSize = sampleRate * 2 * seconds
        val buf = ByteBuffer.allocate(44 + dataSize).order(ByteOrder.LITTLE_ENDIAN)
        buf.put("RIFF".toByteArray()).putInt(36 + dataSize).put("WAVE".toByteArray())
        buf.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(1)
            .putInt(sampleRate).putInt(sampleRate * 2).putShort(2).putShort(16)
        buf.put("data".toByteArray()).putInt(dataSize)
        return buf.array()
    }
}
