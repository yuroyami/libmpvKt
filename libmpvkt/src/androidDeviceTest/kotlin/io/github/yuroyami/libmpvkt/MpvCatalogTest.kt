package io.github.yuroyami.libmpvkt

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertTrue
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
            for (property in MpvProperties.all.filterIsInstance<MpvProperty.Choice<*>>()) {
                val info = mpv[MpvProperties.optionInfo(property.name)].getOrNull()?.asMap() ?: continue
                val choices = info["choices"]?.asList()?.mapNotNull { it.asString() }?.toSet() ?: continue
                for (value in property.values) if (value.mpvName !in choices) problems += "${property.name}: ${value.mpvName} not in $choices"
            }
            assertTrue(problems.isEmpty(), problems.joinToString("\n"))
        } finally { mpv.close() }
    }
}
