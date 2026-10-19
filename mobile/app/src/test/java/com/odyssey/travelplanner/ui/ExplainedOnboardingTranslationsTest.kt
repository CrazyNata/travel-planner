package com.odyssey.travelplanner.ui

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExplainedOnboardingTranslationsTest {
    @Test
    fun everyLanguageIncludesAllActionsAndExamplesIncludingPhotos() {
        for (language in listOf("RU", "EN", "ES", "DE")) {
            val topics = explainedTopicsForLanguage(language)
            assertEquals(8, topics.size, language)
            assertEquals(listOf(5, 4, 4, 4, 3, 4, 4, 4), topics.map { it.steps.size }, language)
            assertEquals(32, topics.sumOf { it.steps.size }, language)
            topics.forEachIndexed { topicIndex, topic ->
                assertTrue(topic.name.isNotBlank(), language)
                topic.steps.forEachIndexed { stepIndex, step ->
                    assertEquals(explainedTopics[topicIndex].steps[stepIndex].exampleRows.size, step.exampleRows.size)
                    val text = listOf(step.title, step.why, step.action, step.result, step.exampleTitle) + step.exampleRows + topic.name
                    assertTrue(text.all { it.isNotBlank() }, "$language/$topicIndex/$stepIndex")
                    if (language != "RU") assertFalse(text.any { it.contains(Regex("[А-Яа-яЁё]")) }, "$language/$topicIndex/$stepIndex")
                }
            }
        }
        assertEquals("Photos", explainedTopicsForLanguage("EN").last().name)
        assertEquals("Fotos", explainedTopicsForLanguage("ES").last().name)
        assertEquals("Fotos", explainedTopicsForLanguage("DE").last().name)
    }

    @Test
    fun localeTagsAndFallbackKeepTheTutorialAvailable() {
        assertEquals(explainedTopicsForLanguage("EN"), explainedTopicsForLanguage("en-GB"))
        assertEquals(explainedTopicsForLanguage("ES"), explainedTopicsForLanguage(" es-ES "))
        assertEquals(explainedTopics, explainedTopicsForLanguage("RU"))
        assertEquals(explainedTopics, explainedTopicsForLanguage("unknown"))
    }

    @Test
    fun photoCountsUseSingularOnlyForOne() {
        for ((language, words) in mapOf("EN" to ("photo" to "photos"), "ES" to ("foto" to "fotos"), "DE" to ("Foto" to "Fotos"))) {
            assertEquals("1 ${words.first}", localizedPhotoCount(1, language))
            for (count in listOf(0, 2, 5, 11, 21)) assertEquals("$count ${words.second}", localizedPhotoCount(count, language))
        }
        for (count in listOf(0, 1, 2, 5, 11, 21)) assertEquals("$count фото", localizedPhotoCount(count, "RU"))
    }

    @Test
    fun tripCountHandlesRussianTeensAndCompoundNumbers() {
        for (count in listOf(1, 21, 101)) assertEquals("поездка", localizedTripCountWord(count, "RU"))
        for (count in listOf(2, 4, 22)) assertEquals("поездки", localizedTripCountWord(count, "RU"))
        for (count in listOf(0, 5, 10, 11, 12, 14, 111)) assertEquals("поездок", localizedTripCountWord(count, "RU"))
        for ((language, words) in mapOf("EN" to ("trip" to "trips"), "ES" to ("viaje" to "viajes"), "DE" to ("Reise" to "Reisen"))) {
            assertEquals(words.first, localizedTripCountWord(1, language))
            assertEquals(words.second, localizedTripCountWord(2, language))
        }
    }
}
