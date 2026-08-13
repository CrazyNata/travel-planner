package com.odyssey.travelplanner.data

import android.content.res.AssetManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.Locale
import java.util.zip.GZIPInputStream

// Keep the gzip payload under a neutral extension: Android's asset packaging
// strips .gz and transparently expands it, which would break GZIPInputStream.
private const val CITY_CATALOG_ASSET = "city-catalog.tsv.bin"
private const val SEARCH_LIMIT = 36

class CityCatalogRepository(private val assets: AssetManager) {
    suspend fun search(query: String, language: String, limit: Int = SEARCH_LIMIT): List<CityCatalogEntry> = withContext(Dispatchers.IO) {
        val normalizedQuery = normalizeCityAlias(query)
        val localMatches = cityCatalog
            .mapNotNull { entry -> score(entry, normalizedQuery)?.let { it to entry } }

        if (normalizedQuery.length < 2) {
            return@withContext localMatches
                .sortedByDescending { it.first }
                .take(limit)
                .map { it.second }
        }

        val localNames = localMatches
            .map { normalizeCityAlias(it.second.localized(language)) }
            .toSet()
        val remoteMatches = ArrayList<Pair<Int, CityCatalogEntry>>(limit * 2)
        assets.open(CITY_CATALOG_ASSET).use { compressed ->
            GZIPInputStream(compressed).use { gzip ->
                BufferedReader(InputStreamReader(gzip, Charsets.UTF_8)).useLines { lines ->
                    lines.forEachIndexed { index, line ->
                        if (index % 256 == 0) currentCoroutineContext().ensureActive()
                        parse(line)?.let { entry ->
                            if (normalizeCityAlias(entry.localized(language)) !in localNames) {
                                score(entry, normalizedQuery)?.let { value ->
                                    remoteMatches += value to entry
                                }
                            }
                        }
                    }
                }
            }
        }

        (localMatches + remoteMatches)
            .sortedWith(compareByDescending<Pair<Int, CityCatalogEntry>> { it.first }
                .thenByDescending { it.second.population }
                .thenBy { normalizeCityAlias(it.second.localized(language)) })
            .take(limit)
            .map { it.second }
    }

    private fun score(entry: CityCatalogEntry, query: String): Int? {
        if (query.isBlank()) return 0
        val aliases = entry.aliases + entry.countryName + entry.localized("RU") + entry.localized("EN") + entry.localized("ES") + entry.localized("DE")
        val normalizedAliases = aliases.map(::normalizeCityAlias)
        val exact = normalizedAliases.any { it == query }
        val starts = normalizedAliases.any { it.startsWith(query) }
        val contains = normalizedAliases.any { it.contains(query) }
        return when {
            exact -> 300
            starts -> 200
            contains -> 100
            else -> null
        }
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
