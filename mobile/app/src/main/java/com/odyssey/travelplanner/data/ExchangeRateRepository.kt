package com.odyssey.travelplanner.data

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.Locale

data class ExchangeRateSnapshot(
    val rates: Map<String, Double>,
    val date: String,
)

@Serializable
private data class FrankfurterRate(
    val date: String,
    val base: String,
    val quote: String,
    val rate: Double,
)

class ExchangeRateRepository {
    private val http = HttpClient(OkHttp) {
        install(ContentNegotiation) { json(Json { ignoreUnknownKeys = true }) }
    }

    fun close() {
        http.close()
    }

    suspend fun loadRubRates(quotes: Set<String>): ExchangeRateSnapshot {
        val normalizedQuotes = quotes
            .map { it.uppercase(Locale.ROOT) }
            .filter { it != "RUB" }
            .distinct()
            .sorted()
        if (normalizedQuotes.isEmpty()) {
            return ExchangeRateSnapshot(mapOf("RUB" to 1.0), "")
        }

        val response: List<FrankfurterRate> = http.get(
            "https://api.frankfurter.dev/v2/rates?base=RUB&quotes=${normalizedQuotes.joinToString(",")}",
        ).body()
        val rates = buildMap {
            put("RUB", 1.0)
            response.forEach { item ->
                if (item.rate > 0.0) put(item.quote.uppercase(Locale.ROOT), item.rate)
            }
        }
        require(rates.keys.containsAll(normalizedQuotes)) { "Не удалось получить полный курс валют" }
        return ExchangeRateSnapshot(rates, response.map(FrankfurterRate::date).distinct().singleOrNull().orEmpty())
    }
}
