package io.github.yuroyami.libmpvkt

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CatalogShapeTest {
    @Test
    fun propertyNamesAreUniqueAndNonEmpty() {
        val names = MpvProperties.all.map { it.name }
        assertTrue(names.all { it.isNotBlank() && !it.contains(' ') })
        assertEquals(names.size, names.toSet().size, "duplicate: ${names.groupBy { it }.filter { it.value.size > 1 }.keys}")
        assertTrue(names.size >= 300, "the catalog has ${names.size} entries")
    }

    @Test
    fun commandNamesAreUnique() {
        val names = MpvCommands.all.map { it.first }
        assertEquals(names.size, names.toSet().size)
        assertTrue(names.size >= 70)
    }
}
