package io.github.yuroyami.libmpvkt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.fail
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MpvTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    /** A wait that says what it was waiting for when it runs out, and is patient on a slow emulator. */
    private suspend fun <T> awaiting(what: String, millis: Long = 20_000, block: suspend () -> T): T =
        withTimeoutOrNull(millis) { block() } ?: fail("timed out after ${millis}ms waiting for $what")

    private fun start(): Mpv = Mpv.create(context).apply {
        setOption("vo", "null").getOrThrow()
        setOption("ao", "null").getOrThrow()
        setOption("idle", "yes").getOrThrow()
        initialize().getOrThrow()
    }

    @Test
    fun propertiesCommandsAndTwoInstances() {
        val a = start()
        val b = start()
        try {
            assertTrue(a.getString("mpv-version")!!.startsWith("mpv "))
            assertEquals(MpvResult.Ok(true), a[MpvProperty.Flag("pause")].also { a[MpvProperty.Flag("pause")] = true }.let { a[MpvProperty.Flag("pause")] })
            assertEquals(MpvResult.Ok(false), b[MpvProperty.Flag("pause")], "the second core is independent")
            assertEquals(MpvResult.Ok(MpvNode.Str("x")), a.command(MpvCommand.of("expand-text", "x")))
            assertTrue(a.command(MpvCommand.of("no-such-command")) is MpvResult.Fail)
            assertEquals(MpvResult.Fail(MpvError.PROPERTY_NOT_FOUND, "no-such-property"), a.getNode("no-such-property"))
        } finally {
            a.close(); b.close(); a.close()
        }
    }

    @Test
    fun observeAsyncHookAndShutdown(): Unit = runBlocking {
        val mpv = start()
        val wav = File(context.cacheDir, "silence.wav").apply { writeBytes(SilentWav.bytes(1)) }
        try {
            var hookRan = false
            mpv.hook("on_load") { hookRan = true }.getOrThrow()
            val ended = awaiting("the file to end") {
                mpv.command(MpvCommand.of("loadfile", wav.absolutePath)).getOrThrow()
                mpv.events.filterIsInstance<MpvEvent.EndFile>().first()
            }
            assertEquals(EndFileReason.Eof, ended.reason)
            assertTrue(hookRan, "the on_load hook ran and continued, or playback could not have started")
            assertEquals(MpvResult.Ok(MpvNode.Str("y")), awaiting("the async command reply") { mpv.commandAsync(MpvCommand.of("expand-text", "y")) })
            val observed = awaiting("the first observed value") { mpv.observe(MpvProperty.Flag("pause")).first() }
            assertEquals(false, observed)
            // events has no replay, so the collector has to be attached before the quit, or the
            // shutdown is gone by the time anyone looks. On a slow emulator that race is lost.
            val attached = CompletableDeferred<Unit>()
            val shutdown = async {
                mpv.events.onSubscription { attached.complete(Unit) }
                    .filterIsInstance<MpvEvent.Shutdown>().first()
            }
            attached.await()
            mpv.command("quit")
            awaiting("the shutdown event") { shutdown.await() }
        } finally {
            mpv.close()
        }
    }

    /** The events mpv_event_to_node leaves empty: async property replies, INT64 changes, hook names. */
    @Test
    fun asyncReadsInt64ObserversAndHookNamesCarryTheirData(): Unit = runBlocking {
        val mpv = start()
        val wav = File(context.cacheDir, "hooked.wav").apply { writeBytes(SilentWav.bytes(1)) }
        try {
            assertEquals(MpvResult.Ok(100.0), awaiting("the async volume read") { mpv.getAsync(MpvProperties.Volume) })
            val int64 = awaiting("the first INT64 value") { mpv.observeNode("playlist-count", format = 4).first() }
            assertEquals(MpvNode.Int64(0), int64)
            val hookName = CompletableDeferred<String>()
            mpv.hook("on_load") { hookName.complete(it.name) }.getOrThrow()
            mpv.command(MpvCommand.of("loadfile", wav.absolutePath)).getOrThrow()
            assertEquals("on_load", awaiting("the on_load hook") { hookName.await() })
        } finally {
            mpv.close()
        }
    }

    /** close() waits for the calls inside native code; a reader on another thread then ends with IllegalStateException. */
    @Test
    fun closeWhileAnotherThreadReads() {
        val mpv = start()
        val reading = CountDownLatch(1)
        var ending: Throwable? = null
        val reader = thread {
            try {
                while (true) { mpv.getNode("time-pos"); mpv[MpvProperties.Volume]; reading.countDown() }
            } catch (t: Throwable) {
                ending = t
            }
        }
        assertTrue(reading.await(10, TimeUnit.SECONDS), "the reader never got going")
        mpv.close()
        reader.join(10_000)
        assertTrue(ending is IllegalStateException, "the reader ended with $ending")
    }
}
