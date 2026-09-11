@file:Suppress("DEPRECATION")

package io.github.yuroyami.libmpvkt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.runner.RunWith

/** Loads the real libraries on a device or emulator. This is the only proof that the AAR works. */
@RunWith(AndroidJUnit4::class)
class MPVLibFacadeTest {

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
        try {
            startCore()
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
        val wav = File(context.cacheDir, "silence.wav").apply { writeBytes(SilentWav.bytes(seconds = 1)) }
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
        try {
            startCore()
            MPVLib.addObserver(observer)
            MPVLib.command(arrayOf("loadfile", wav.absolutePath))
            assertTrue(loaded.await(15, TimeUnit.SECONDS), "the file never loaded")
            assertTrue(ended.await(15, TimeUnit.SECONDS), "playback never reached the end")
        } finally {
            MPVLib.removeObserver(observer)
            MPVLib.destroy()
        }
    }

    /** Every callback, with the thread it came on. */
    private class Recorder : MPVLib.EventObserver, MPVLib.LogObserver {
        val calls: MutableList<String> = Collections.synchronizedList(mutableListOf())
        val threads: MutableSet<String> = Collections.synchronizedSet(mutableSetOf())
        val logLevels: MutableList<Int> = Collections.synchronizedList(mutableListOf())
        val ended = CountDownLatch(1)
        private fun note(call: String) { calls += call; threads += Thread.currentThread().name }
        override fun eventProperty(property: String) = note(property)
        override fun eventProperty(property: String, value: Long) = note("$property=$value")
        override fun eventProperty(property: String, value: Boolean) = note("$property=$value")
        override fun eventProperty(property: String, value: String) = note("$property=$value")
        override fun eventProperty(property: String, value: Double) = note("$property=$value")
        override fun event(eventId: Int) {
            note("event $eventId")
            if (eventId == MPVLib.MpvEvent.MPV_EVENT_END_FILE) ended.countDown()
        }
        override fun logMessage(prefix: String, level: Int, text: String) { logLevels += level }
    }

    /** 0.1.0 asked mpv for INT64, which truncates a double property. */
    @Test
    fun getPropertyIntTruncatesADouble() {
        try {
            startCore()
            MPVLib.setPropertyDouble("speed", 1.75)
            assertEquals(1, MPVLib.getPropertyInt("speed"))
        } finally {
            MPVLib.destroy()
        }
    }

    /** 0.1.0 called back on mpv's event thread only, in mpv's order. */
    @Test
    fun callbacksArriveInOrderOnOneThread() {
        val wav = File(context.cacheDir, "ordered.wav").apply { writeBytes(SilentWav.bytes(seconds = 1)) }
        val recorder = Recorder()
        try {
            startCore()
            MPVLib.addObserver(recorder)
            MPVLib.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.command(arrayOf("loadfile", wav.absolutePath))
            assertTrue(recorder.ended.await(15, TimeUnit.SECONDS), "playback never reached the end")
            assertEquals(1, recorder.threads.size, "callbacks came on ${recorder.threads}")
            val calls = recorder.calls.toList()
            val started = calls.indexOf("event ${MPVLib.MpvEvent.MPV_EVENT_START_FILE}")
            val loaded = calls.indexOf("event ${MPVLib.MpvEvent.MPV_EVENT_FILE_LOADED}")
            assertTrue(started in 0 until loaded, "start-file came after file-loaded: $calls")
        } finally {
            MPVLib.removeObserver(recorder)
            MPVLib.destroy()
        }
    }

    /** A throwing observer is logged and skipped, and a format mpv refuses is logged, as 0.1.0 did. */
    @Test
    fun aThrowingObserverAndARefusedFormatDoNotEndTheApp() {
        val recorder = Recorder()
        val thrower = object : MPVLib.EventObserver {
            override fun eventProperty(property: String) = throw IllegalStateException("a broken observer")
            override fun eventProperty(property: String, value: Long) = throw IllegalStateException("a broken observer")
            override fun eventProperty(property: String, value: Boolean) = throw IllegalStateException("a broken observer")
            override fun eventProperty(property: String, value: String) = throw IllegalStateException("a broken observer")
            override fun eventProperty(property: String, value: Double) = throw IllegalStateException("a broken observer")
            override fun event(eventId: Int) = throw IllegalStateException("a broken observer")
        }
        try {
            startCore()
            MPVLib.addObserver(thrower)
            MPVLib.addObserver(recorder)
            MPVLib.observeProperty("pause", 42)
            MPVLib.observeProperty("pause", MPVLib.MpvFormat.MPV_FORMAT_FLAG)
            MPVLib.setPropertyBoolean("pause", true)
            val deadline = System.currentTimeMillis() + 10_000
            while ("pause=true" !in recorder.calls && System.currentTimeMillis() < deadline) Thread.sleep(20)
            assertTrue("pause=true" in recorder.calls, "the observer after the throwing one got ${recorder.calls}")
        } finally {
            MPVLib.removeObserver(thrower)
            MPVLib.removeObserver(recorder)
            MPVLib.destroy()
        }
    }

    /** The functions 0.1.0's native code called; an app built against 0.1.0 may call them too. */
    @Test
    fun theZeroOneCallbackEntryPointsStillReachTheObservers() {
        val recorder = Recorder()
        MPVLib.addObserver(recorder)
        MPVLib.addLogObserver(recorder)
        try {
            MPVLib.eventProperty("a")
            MPVLib.eventProperty("b", 2L)
            MPVLib.eventProperty("c", true)
            MPVLib.eventProperty("d", 1.5)
            MPVLib.eventProperty("e", "x")
            MPVLib.event(MPVLib.MpvEvent.MPV_EVENT_SEEK)
            MPVLib.logMessage("cplayer", MPVLib.MpvLogLevel.MPV_LOG_LEVEL_INFO, "hello")
            assertEquals(listOf("a", "b=2", "c=true", "d=1.5", "e=x", "event 20"), recorder.calls.toList())
            assertEquals(listOf(MPVLib.MpvLogLevel.MPV_LOG_LEVEL_INFO), recorder.logLevels.toList())
        } finally {
            MPVLib.removeObserver(recorder)
            MPVLib.removeLogObserver(recorder)
        }
    }

    /** 0.1.0 asked for terminal-default logs, so msg-level still decides which lines arrive. */
    @Test
    fun logLinesFollowMsgLevel() {
        val wav = File(context.cacheDir, "quiet.wav").apply { writeBytes(SilentWav.bytes(seconds = 1)) }
        val recorder = Recorder()
        try {
            MPVLib.create(context)
            MPVLib.setOptionString("config", "no")
            MPVLib.setOptionString("vo", "null")
            MPVLib.setOptionString("ao", "null")
            MPVLib.setOptionString("idle", "yes")
            MPVLib.setOptionString("msg-level", "all=warn")
            MPVLib.init()
            MPVLib.addObserver(recorder)
            MPVLib.addLogObserver(recorder)
            MPVLib.command(arrayOf("loadfile", wav.absolutePath))
            assertTrue(recorder.ended.await(15, TimeUnit.SECONDS), "playback never reached the end")
            val tooChatty = recorder.logLevels.filter { it > MPVLib.MpvLogLevel.MPV_LOG_LEVEL_WARN }.distinct()
            assertTrue(tooChatty.isEmpty(), "lines above warn arrived, at levels $tooChatty")
        } finally {
            MPVLib.removeObserver(recorder)
            MPVLib.removeLogObserver(recorder)
            MPVLib.destroy()
        }
    }
}
