package com.odyssey.travelplanner.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.util.Locale
import java.util.zip.GZIPInputStream
import kotlin.math.abs

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
        val curated: Boolean,
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
                        curated = true,
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
            // Curated cities are the canonical choices for the app. Their
            // bundled population is intentionally optional, so an empty
            // population must not push them below a world-catalog duplicate.
            .thenByDescending { it.curated }
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
                            curated = false,
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

    /**
     * Resolves already-entered city names against the full catalog in one pass.
     * Route legs can contain legacy free-text values without a country suffix,
     * so the UI uses this only as a fallback when the local flag lookup is unknown.
     */
    suspend fun findExact(values: Collection<String>): Map<String, CityCatalogEntry> = withContext(Dispatchers.IO) {
        val requested = values
            .filter(String::isNotBlank)
            .groupBy(::cityLookupKey)
            .filterKeys(String::isNotBlank)
        if (requested.isEmpty()) return@withContext emptyMap()

        val resolved = mutableMapOf<String, CityCatalogEntry>()

        fun addIfBest(key: String, entry: CityCatalogEntry) {
            val current = resolved[key]
            if (current == null || entry.population > current.population) {
                resolved[key] = entry
            }
        }

        requested.keys.forEach { key ->
            cityCatalog.firstOrNull { entry -> key in entry.aliases }?.let { entry -> addIfBest(key, entry) }
        }

        val unresolved = requested.keys - resolved.keys
        if (unresolved.isNotEmpty()) {
            val catalog = catalogText
            var lineStart = 0
            var lineIndex = 0

            while (lineStart < catalog.length) {
                if (lineIndex % SEARCH_CHECK_INTERVAL == 0) currentCoroutineContext().ensureActive()

                var lineEnd = catalog.indexOf('\n', lineStart)
                if (lineEnd < 0) lineEnd = catalog.length
                if (lineEnd > lineStart && catalog[lineEnd - 1] == '\r') lineEnd--

                exactRowQuery(catalog, lineStart, lineEnd, unresolved)?.let { key ->
                    parse(catalog.substring(lineStart, lineEnd))?.let { entry -> addIfBest(key, entry) }
                }

                lineStart = if (lineEnd < catalog.length) lineEnd + 1 else catalog.length
                lineIndex++
            }
        }

        requested.flatMap { (key, originalValues) ->
            val entry = resolved[key] ?: return@flatMap emptyList()
            originalValues.map { it to entry }
        }.toMap()
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

    private fun exactRowQuery(text: String, lineStart: Int, lineEnd: Int, queries: Set<String>): String? {
        var fieldStart = lineStart
        var fieldIndex = 0

        while (fieldStart <= lineEnd && fieldIndex <= 10) {
            var fieldEnd = text.indexOf('\t', fieldStart)
            if (fieldEnd < 0 || fieldEnd > lineEnd) fieldEnd = lineEnd

            if (fieldIndex == 1 || fieldIndex in 6..10) {
                val fieldLength = fieldEnd - fieldStart
                queries.firstOrNull { query ->
                    fieldLength == query.length && text.regionMatches(fieldStart, query, 0, query.length, ignoreCase = true)
                }?.let { return it }
            }

            if (fieldEnd == lineEnd) break
            fieldStart = fieldEnd + 1
            fieldIndex++
        }
        return null
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

private fun cityLookupKey(value: String): String = normalizeCityAlias(
    value.substringBefore(",").substringBefore(" — ").trim(),
)

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

    // Keep typo tolerance limited to complete names. This catches common
    // transpositions such as “Salzbrug” → “Salzburg” without turning every
    // short substring search into a fuzzy match.
    if (query.length >= 4) {
        val field = text.substring(start, end).lowercase(Locale.ROOT)
        val maxDistance = fuzzyDistanceLimit(query.length)
        if (abs(field.length - query.length) <= maxDistance &&
            damerauDistanceWithin(field, query, maxDistance)
        ) {
            return 50
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
    val normalizedQuery = normalizeCityAlias(query)
    if (normalizedQuery.isBlank()) return 0
    val separator = SEARCH_SEPARATOR.toString()
    val exact = searchText.contains("$separator$normalizedQuery$separator")
    val starts = searchText.contains("$separator$normalizedQuery")
    val contains = searchText.contains(normalizedQuery)
    val fuzzy = normalizedQuery.length >= 4 && searchText
        .split(SEARCH_SEPARATOR)
        .any { candidate ->
            candidate.isNotBlank() &&
                abs(candidate.length - normalizedQuery.length) <= fuzzyDistanceLimit(normalizedQuery.length) &&
                damerauDistanceWithin(candidate, normalizedQuery, fuzzyDistanceLimit(normalizedQuery.length))
        }
    return when {
        exact -> 300
        starts -> 200
        contains -> 100
        fuzzy -> 50
        else -> null
    }
}

private fun fuzzyDistanceLimit(queryLength: Int): Int = when {
    queryLength < 4 -> 0
    queryLength <= 6 -> 1
    else -> 2
}

/** Returns whether the optimal-string-alignment distance is within [limit]. */
private fun damerauDistanceWithin(left: String, right: String, limit: Int): Boolean {
    if (left == right) return true
    if (abs(left.length - right.length) > limit) return false

    var previousPrevious = IntArray(right.length + 1) { it }
    var previous = IntArray(right.length + 1) { it }
    for (leftIndex in left.indices) {
        val current = IntArray(right.length + 1)
        current[0] = leftIndex + 1
        var rowMinimum = current[0]
        for (rightIndex in right.indices) {
            val insertion = current[rightIndex] + 1
            val deletion = previous[rightIndex + 1] + 1
            val substitution = previous[rightIndex] + if (left[leftIndex] == right[rightIndex]) 0 else 1
            var value = minOf(insertion, deletion, substitution)
            if (
                leftIndex > 0 && rightIndex > 0 &&
                left[leftIndex] == right[rightIndex - 1] &&
                left[leftIndex - 1] == right[rightIndex]
            ) {
                value = minOf(value, previousPrevious[rightIndex - 1] + 1)
            }
            current[rightIndex + 1] = value
            rowMinimum = minOf(rowMinimum, value)
        }
        if (rowMinimum > limit) return false
        previousPrevious = previous
        previous = current
    }
    return previous[right.length] <= limit
}
