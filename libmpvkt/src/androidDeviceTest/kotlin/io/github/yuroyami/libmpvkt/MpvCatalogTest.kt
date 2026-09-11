package io.github.yuroyami.libmpvkt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.runner.RunWith

/** The catalog against the real mpv: every name exists, every choice value is one mpv accepts. */
@RunWith(AndroidJUnit4::class)
class MpvCatalogTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    private fun start(): Mpv = Mpv.create(context).apply {
        setOption("vo", "null").getOrThrow(); setOption("ao", "null").getOrThrow(); setOption("idle", "yes").getOrThrow()
        initialize().getOrThrow()
    }

    @Test
    fun everyPropertyExists() {
        val mpv = start()
        try {
            val known = mpv[MpvProperties.PropertyList].getOrThrow().toSet()
            val missing = MpvProperties.all.map { it.name.substringBefore('/') }.filter { it !in known }
            assertTrue(missing.isEmpty(), "unknown to mpv ${mpv.getString("mpv-version")}: $missing")
        } finally { mpv.close() }
    }

    @Test
    fun everyCommandExists() {
        val mpv = start()
        try {
            val known = mpv[MpvProperties.CommandList].getOrThrow().map { it.name }.toSet()
            val missing = MpvCommands.all.map { it.first }.filter { it !in known }
            assertTrue(missing.isEmpty(), "unknown commands: $missing")
        } finally { mpv.close() }
    }

    @Test
    fun everyChoiceValueIsAccepted() {
        val mpv = start()
        try {
            val problems = mutableListOf<String>()
            var checked = 0
            for (property in MpvProperties.all.filterIsInstance<MpvProperty.Choice<*>>()) {
                val info = mpv[MpvProperties.optionInfo(property.name)].getOrNull()?.asMap() ?: continue
                val choices = info["choices"]?.asList()?.mapNotNull { it.asString() }?.toSet() ?: continue
                checked++
                for (value in property.values) if (value.mpvName !in choices) problems += "${property.name}: ${value.mpvName} not in $choices"
            }
            // cscale and dscale are typed Scaler? rather than Choice: they take every Scaler plus "" for inherit.
            for (name in listOf("cscale", "dscale")) {
                val info = mpv[MpvProperties.optionInfo(name)].getOrNull()?.asMap() ?: continue
                val choices = info["choices"]?.asList()?.mapNotNull { it.asString() }?.toSet() ?: continue
                checked++
                for (value in Scaler.entries.map { it.mpvName } + "") if (value !in choices) problems += "$name: '$value' not in $choices"
            }
            assertTrue(problems.isEmpty(), problems.joinToString("\n"))
            // Without this, an option-info that answers nothing would pass after checking nothing.
            assertTrue(checked >= 20, "only $checked choice properties could be checked")
        } finally { mpv.close() }
    }

    /** Every property reads back in the type the catalog gives it, with a file loaded. Unavailable is fine; a type mismatch is not. */
    @Test
    fun everyPropertyDecodesWhatMpvSends(): Unit = runBlocking {
        val mpv = start()
        val wav = File(context.cacheDir, "catalog.wav").apply { writeBytes(SilentWav.bytes(seconds = 2)) }
        try {
            val attached = CompletableDeferred<Unit>()
            val loaded = async { mpv.events.onSubscription { attached.complete(Unit) }.first { it == MpvEvent.FileLoaded } }
            attached.await()
            mpv.command(MpvCommands.loadFile(wav.absolutePath)).getOrThrow()
            withTimeout(20_000) { loaded.await() }
            val problems = MpvProperties.all.mapNotNull { property ->
                val result = mpv[property]
                if (result is MpvResult.Fail && result.error == MpvError.PROPERTY_FORMAT) "${property.name}: ${result.detail}" else null
            }
            assertTrue(problems.isEmpty(), problems.joinToString("\n"))
        } finally { mpv.close() }
    }
}
