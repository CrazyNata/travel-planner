package com.odyssey.travelplanner.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.GZIPInputStream

// Keep the gzip payload under a neutral extension: Android's asset packaging
// strips .gz and transparently expands it, which would break GZIPInputStream.
private const val CITY_CATALOG_ASSET = "city-catalog.tsv.bin"
private const val SEARCH_LIMIT = 36
private const val SEARCH_SEPARATOR = '\u0001'

class CityCatalogRepository(private val assets: AssetManager) {
    private data class BundledEntry(
        val rawLine: String,
        val normalizedLocalizedNames: List<String>,
        val normalizedSearchText: String,
        val population: Long,
    )

    private data class ScoredEntry(
        val score: Int,
        val population: Long,
        val normalizedName: String,
        val entry: CityCatalogEntry? = null,
        val bundledEntry: BundledEntry? = null,
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

    // The bundled catalog is immutable for the lifetime of the app. Loading and
    // its compact search index once avoids reopening, decompressing, and parsing
    // 150k+ rows every time the user types another character.
    private val bundledEntries: List<BundledEntry> by lazy(LazyThreadSafetyMode.SYNCHRONIZED) {
        loadBundledEntries()
    }

    suspend fun preload() = withContext(Dispatchers.IO) {
        bundledEntries.size
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
                .map { it.resolve() }
        }

        val localNames = localMatches
            .map { it.normalizedName }
            .toSet()
        val remoteMatches = ArrayList<ScoredEntry>(limit * 2)
        bundledEntries.forEach { bundledEntry ->
            val normalizedName = bundledEntry.localizedName(language)
            if (normalizedName !in localNames) {
                cityCatalogSearchScore(bundledEntry.normalizedSearchText, normalizedQuery)?.let { value ->
                    remoteMatches += ScoredEntry(
                        score = value,
                        population = bundledEntry.population,
                        normalizedName = normalizedName,
                        bundledEntry = bundledEntry,
                    )
                }
            }
        }

        (localMatches + remoteMatches)
            .sortedWith(compareByDescending<ScoredEntry> { it.score }
                .thenByDescending { it.population }
                .thenBy { it.normalizedName })
            .take(limit)
            .map { it.resolve() }
    }

    private fun loadBundledEntries(): List<BundledEntry> {
        val entries = ArrayList<BundledEntry>()
        assets.open(CITY_CATALOG_ASSET).use { compressed ->
            GZIPInputStream(compressed).use { gzip ->
                BufferedReader(InputStreamReader(gzip, Charsets.UTF_8)).useLines { lines ->
                    lines.forEach { line ->
                        index(line)?.let { entries += it }
                    }
                }
            }
        }
        return entries
    }

    private fun index(line: String): BundledEntry? {
        val parts = line.split('\t')
        if (parts.size < 12) return null
        val name = parts[1].ifBlank { return null }
        if (parts[4].toDoubleOrNull() == null || parts[5].toDoubleOrNull() == null) return null

        val localizedNames = listOf(
            parts[7].ifBlank { name },
            parts[8].ifBlank { name },
            parts[9].ifBlank { name },
            parts[10].ifBlank { name },
        )
        val searchText = cityCatalogSearchText(
            listOf(parts[1], parts[6], parts[3]) + localizedNames,
        )
        return BundledEntry(
            rawLine = line,
            normalizedLocalizedNames = localizedNames.map(::normalizeCityAlias),
            normalizedSearchText = searchText,
            population = parts[11].toLongOrNull() ?: 0L,
        )
    }

    private fun BundledEntry.localizedName(language: String): String = normalizedLocalizedNames[
        when (language.trim().uppercase(Locale.ROOT).substringBefore('-')) {
            "EN" -> 1
            "ES" -> 2
            "DE" -> 3
            else -> 0
        },
    ]

    private fun ScoredEntry.resolve(): CityCatalogEntry = entry ?: bundledEntry
        ?.let { parse(it.rawLine) }
        ?: error("City catalog result has no source")

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
