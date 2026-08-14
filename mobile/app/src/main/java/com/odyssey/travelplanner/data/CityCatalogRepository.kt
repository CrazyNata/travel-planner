package com.odyssey.travelplanner.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.zip.GZIPInputStream

// Keep the gzip payload under a neutral extension: Android's asset packaging
// strips .gz and transparently expands it, which would break GZIPInputStream.
private const val CITY_CATALOG_ASSET = "city-catalog.tsv.bin"
private const val SEARCH_LIMIT = 36
private const val SEARCH_SEPARATOR = '\u0001'
private const val SEARCH_CHECK_INTERVAL = 1024

class CityCatalogRepository(private val assets: AssetManager) {
    private data class ScoredEntry(
        val score: Int,
        val population: Long,
        val normalizedName: String,
        val entry: CityCatalogEntry,
    )

    private val localEntries = cityCatalog.map { entry ->
        entry to cityCatalogSearchText(
            entry.aliases +
                entry.countryName +
                entry.localized("RU") +
                entry.localized("EN") +
                entry.localized("ES") +
                entry.localized("DE"),
        )
    }

    // Keep only the decompressed text, not 150k Kotlin objects and strings.
    // Searching the text by field offsets is much cheaper on a mobile device.
    private val catalogText: String by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        GZIPInputStream(assets.open(CITY_CATALOG_ASSET)).use { it.readBytes().toString(Charsets.UTF_8) }
    }

    suspend fun search(query: String, language: String, limit: Int = SEARCH_LIMIT): List<CityCatalogEntry> = withContext(Dispatchers.IO) {
        val normalizedQuery = normalizeCityAlias(query)
        val localMatches = localEntries
            .mapNotNull { (entry, searchText) ->
                cityCatalogSearchScore(searchText, normalizedQuery)?.let { value ->
                    ScoredEntry(
                        score = value,
                        population = entry.population,
                        normalizedName = normalizeCityAlias(entry.localized(language)),
                        entry = entry,
                    )
                }
            }

        if (normalizedQuery.length < 2) {
            return@withContext localMatches
                .sortedByDescending { it.score }
                .take(limit)
                .map { it.entry }
        }

        val localNames = localMatches
            .map { it.normalizedName }
            .toSet()
        val remoteMatches = ArrayList<ScoredEntry>(limit * 2)
        val compareEntries = compareByDescending<ScoredEntry> { it.score }
            .thenByDescending { it.population }
            .thenBy { it.normalizedName }
        val catalog = catalogText
        var lineStart = 0
        var lineIndex = 0

        while (lineStart < catalog.length) {
            if (lineIndex % SEARCH_CHECK_INTERVAL == 0) currentCoroutineContext().ensureActive()

            var lineEnd = catalog.indexOf('\n', lineStart)
            if (lineEnd < 0) lineEnd = catalog.length
            if (lineEnd > lineStart && catalog[lineEnd - 1] == '\r') lineEnd--

            val score = scoreRow(catalog, lineStart, lineEnd, normalizedQuery)
            if (score != null) {
                val entry = parse(catalog.substring(lineStart, lineEnd))
                if (entry != null) {
                    val normalizedName = normalizeCityAlias(entry.localized(language))
                    if (normalizedName !in localNames) {
                        remoteMatches += ScoredEntry(
                            score = score,
                            population = entry.population,
                            normalizedName = normalizedName,
                            entry = entry,
                        )
                        // Keep broad two-letter searches bounded while the text is scanned.
                        if (remoteMatches.size > limit * 4) {
                            remoteMatches.sortWith(compareEntries)
                            remoteMatches.subList(limit, remoteMatches.size).clear()
                        }
                    }
                }
            }

            lineStart = if (lineEnd < catalog.length) lineEnd + 1 else catalog.length
            lineIndex++
        }

        (localMatches + remoteMatches)
            .sortedWith(compareEntries)
            .take(limit)
            .map { it.entry }
    }

    private fun scoreRow(text: String, lineStart: Int, lineEnd: Int, query: String): Int? {
        var fieldStart = lineStart
        var fieldIndex = 0
        var bestScore: Int? = null

        while (fieldStart <= lineEnd && fieldIndex <= 10) {
            var fieldEnd = text.indexOf('\t', fieldStart)
            if (fieldEnd < 0 || fieldEnd > lineEnd) fieldEnd = lineEnd

            when (fieldIndex) {
                1, 3, 6, 7, 8, 9, 10 -> {
                    val score = cityCatalogFieldScore(text, fieldStart, fieldEnd, query)
                    if (score != null && (bestScore == null || score > bestScore)) {
                        bestScore = score
                        if (score == 300) return score
                    }
                }
            }

            if (fieldEnd == lineEnd) break
            fieldStart = fieldEnd + 1
            fieldIndex++
        }
        return bestScore
    }

    private fun parse(line: String): CityCatalogEntry? {
        val parts = line.split('\t')
        if (parts.size < 12) return null
        val latitude = parts[4].toDoubleOrNull() ?: return null
        val longitude = parts[5].toDoubleOrNull() ?: return null
        val name = parts[1].ifBlank { return null }
        val aliases = listOf(parts[1], parts[6], parts[7], parts[8], parts[9], parts[10])
            .filter(String::isNotBlank)
            .map(::normalizeCityAlias)
            .toSet()
        return CityCatalogEntry(
            key = "world:${parts[0]}",
            russian = parts[7].ifBlank { name },
            english = parts[8].ifBlank { name },
            spanish = parts[9].ifBlank { name },
            german = parts[10].ifBlank { name },
            latitude = latitude,
            longitude = longitude,
            aliases = aliases,
            countryName = parts[3],
            countryCode = parts[2].uppercase(Locale.ROOT),
            population = parts[11].toLongOrNull() ?: 0L,
        )
    }
}

private fun cityCatalogFieldScore(text: String, start: Int, end: Int, query: String): Int? {
    if (start >= end) return null
    val fieldLength = end - start
    val exact = fieldLength == query.length && text.regionMatches(start, query, 0, query.length, ignoreCase = true)
    if (exact) return 300

    val starts = fieldLength >= query.length && text.regionMatches(start, query, 0, query.length, ignoreCase = true)
    if (starts) return 200

    val lastCandidate = end - query.length
    if (lastCandidate >= start) {
        for (candidate in start..lastCandidate) {
            if (text.regionMatches(candidate, query, 0, query.length, ignoreCase = true)) return 100
        }
    }
    return null
}

internal fun cityCatalogSearchText(values: Iterable<String>): String = values
    .asSequence()
    .map(::normalizeCityAlias)
    .filter(String::isNotBlank)
    .distinct()
    .joinToString(
        separator = SEARCH_SEPARATOR.toString(),
        prefix = SEARCH_SEPARATOR.toString(),
        postfix = SEARCH_SEPARATOR.toString(),
    )

internal fun cityCatalogSearchScore(searchText: String, query: String): Int? {
    if (query.isBlank()) return 0
    val separator = SEARCH_SEPARATOR.toString()
    val exact = searchText.contains("$separator$query$separator")
    val starts = searchText.contains("$separator$query")
    val contains = searchText.contains(query)
    return when {
        exact -> 300
        starts -> 200
        contains -> 100
        else -> null
    }
}
