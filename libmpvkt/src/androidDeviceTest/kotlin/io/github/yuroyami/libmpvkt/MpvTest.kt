package io.github.yuroyami.libmpvkt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MpvTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

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
    fun observeAsyncHookAndShutdown() = runBlocking {
        val mpv = start()
        val wav = File(context.cacheDir, "silence.wav").apply { writeBytes(SilentWav.bytes(1)) }
        try {
            var hookRan = false
            mpv.hook("on_load") { hookRan = true }.getOrThrow()
            val ended = withTimeout(15_000) {
                mpv.command(MpvCommand.of("loadfile", wav.absolutePath)).getOrThrow()
                mpv.events.filterIsInstance<MpvEvent.EndFile>().first()
            }
            assertEquals(EndFileReason.Eof, ended.reason)
            assertTrue(hookRan, "the on_load hook ran and continued, or playback could not have started")
            assertEquals(MpvResult.Ok(MpvNode.Str("y")), withTimeout(5_000) { mpv.commandAsync(MpvCommand.of("expand-text", "y")) })
            val observed = withTimeout(5_000) { mpv.observe(MpvProperty.Flag("pause")).first() }
            assertEquals(false, observed)
            mpv.command("quit")
            withTimeout(5_000) { mpv.events.filterIsInstance<MpvEvent.Shutdown>().first() }
        } finally {
            mpv.close()
        }
    }

}
