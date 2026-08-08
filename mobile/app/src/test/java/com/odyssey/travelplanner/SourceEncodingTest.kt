package com.odyssey.travelplanner

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue

class SourceEncodingTest {
    @Test
    fun `android sources do not contain common UTF-8 mojibake markers`() {
        val sourceRoot = File("src/main")
        val markers = listOf("Рџ", "Рµ", "Сѓ", "С‚", "Рё", "РЅ", "Рѕ", "С€", "С‡")
        val affectedFiles = sourceRoot
            .walkTopDown()
            .filter { it.isFile && it.extension in setOf("kt", "kts", "xml") }
            .filter { file ->
                val text = file.readText(Charsets.UTF_8)
                markers.any(text::contains)
            }
            .map { it.relativeTo(sourceRoot).path }
            .toList()

        assertTrue(affectedFiles.isEmpty(), "Mojibake found in: ${affectedFiles.joinToString()}")
    }
}
