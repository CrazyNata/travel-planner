package com.odyssey.travelplanner.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.functions.functions
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
data class TripRow(
    val id: String,
    val payload: JsonObject,
    @SerialName("owner_id") val ownerId: String? = null,
)

@Serializable
private data class TripInsert(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val payload: JsonObject,
)

@Serializable
private data class TripPayloadUpdate(val payload: JsonObject)

data class TripCard(
    val id: String,
    val title: String,
    val dates: String,
    val status: String,
    val progress: Int,
    val cities: String,
    val coverImage: String?,
)

data class CoverPhoto(val id: String, val imageUrl: String, val city: String)
data class RouteLeg(
    val dayId: String,
    val from: String,
    val to: String,
    val date: String,
    val checkIn: String,
    val checkOut: String,
    val notes: String,
    val mapsUrl: String,
    val completed: List<String>,
)
data class Accommodation(
    val id: String,
    val city: String,
    val name: String,
    val dates: String,
    val price: String,
    val status: String,
    val details: String,
    val photos: List<String>,
    val bookingUrl: String,
    // Optional fields already used by the existing trip payload. They are read-only
    // here so the Supabase schema and persisted shape remain unchanged.
    val deadline: String = "",
    val rating: Double? = null,
)
data class BudgetExpense(val id: String, val name: String, val amount: Double, val category: String, val scope: String, val paidBy: String)
data class BudgetGroup(val name: String, val people: Int)
data class TripMember(val id: String, val name: String, val email: String, val role: String, val initials: String, val tone: String)
data class Sight(val id: String, val name: String, val city: String, val photo: String, val category: String, val done: Boolean, val walkDay: Int, val walkOrder: Int, val description: String, val longitude: Double?, val latitude: Double?, val rating: Double? = null)
data class Restaurant(
    val id: String,
    val name: String,
    val city: String,
    val status: String,
    val photos: List<String>,
    val rating: Double?,
    val reviews: String,
    val price: String,
    val note: String,
    val link: String,
    val date: String = "",
)
data class TripOverview(
    val id: String,
    val title: String,
    val dates: String,
    val status: String,
    val coverPhotos: List<CoverPhoto>,
    val overviewMapPoints: List<String>,
    val routeLegs: List<RouteLeg>,
    val accommodations: List<Accommodation>,
    val budgetCurrency: String,
    val budgetExpenses: List<BudgetExpense>,
    val budgetGroups: List<BudgetGroup>,
    val members: List<TripMember>,
    val sights: List<Sight>,
    val restaurants: List<Restaurant>,
    val cities: List<String> = emptyList(),
)

enum class TripSection {
    OVERVIEW,
    ROUTE,
    SIGHTS,
    RESTAURANTS,
    ACCOMMODATION,
    BUDGET,
    MEMBERS,
    PHOTOS,
}

data class CreateTripInput(
    val title: String,
    val startDate: String,
    val endDate: String,
    val cities: String,
)

data class RestaurantInput(
    val name: String,
    val city: String,
    val status: String,
    val note: String = "",
    val price: String = "",
    val link: String = "",
    val date: String = "",
)

data class AccommodationInput(
    val name: String,
    val city: String,
    val dates: String,
    val price: String,
    val status: String,
    val details: String = "",
    val bookingUrl: String = "",
)

data class ExpenseInput(
    val name: String,
    val amount: Double,
    val category: String,
    val scope: String = "общий",
    val paidBy: String = "Не указано",
)

interface TripRepository {
    suspend fun loadTrips(): List<TripCard>
    suspend fun loadTripOverview(id: String): TripOverview?
    suspend fun createTrip(title: String, startDate: String, endDate: String, cities: String): TripCard
    suspend fun updateTripSection(id: String, key: String, value: JsonElement)
    suspend fun addRouteLeg(id: String, from: String, to: String)
    suspend fun addBudgetExpense(id: String, name: String, amount: Double, category: String)
    suspend fun updateMemberRole(id: String, memberId: String, role: String)
    suspend fun updateAccommodationStatus(id: String, accommodationId: String, status: String)
    suspend fun updateSightDone(id: String, sightId: String, done: Boolean)
    suspend fun addRestaurant(id: String, name: String, city: String, status: String)
    suspend fun addAccommodation(id: String, name: String, city: String, dates: String, price: String, status: String)
    suspend fun addSight(id: String, name: String, city: String, category: String)
    suspend fun updateRouteChecklist(id: String, dayId: String, itemId: String, completed: Boolean)
    suspend fun addMember(id: String, name: String, email: String, role: String)
    suspend fun addCoverPhoto(id: String, bytes: ByteArray)
    suspend fun updateTripDetails(id: String, title: String, dates: String, cities: String)
    suspend fun updateRestaurantStatus(id: String, restaurantId: String, status: String)
    suspend fun updateBudgetExpense(id: String, expenseId: String, name: String, amount: Double, category: String)
    suspend fun updateRouteLegCities(id: String, dayId: String, from: String, to: String)
    suspend fun updateRouteLegDetails(id: String, dayId: String, from: String, to: String, checkIn: String, checkOut: String, notes: String, mapsUrl: String)
    suspend fun updateRestaurantDetails(id: String, restaurantId: String, name: String, city: String, note: String)
    suspend fun updateAccommodationDetails(id: String, accommodationId: String, name: String, city: String, dates: String, price: String)
    suspend fun updateSightDetails(id: String, sightId: String, name: String, city: String, category: String)
    suspend fun addBudgetGroup(id: String, name: String, people: Int)
    suspend fun addAccommodationPhoto(id: String, accommodationId: String, bytes: ByteArray)
    suspend fun addSightPhoto(id: String, sightId: String, bytes: ByteArray)
    suspend fun moveAccommodationPhoto(id: String, accommodationId: String, photoIndex: Int, direction: Int)
    suspend fun moveRestaurantPhoto(id: String, restaurantId: String, photoIndex: Int, direction: Int)
    suspend fun addRestaurantPhoto(id: String, restaurantId: String, bytes: ByteArray)
    suspend fun deleteTripItem(id: String, section: String, itemId: String)
    suspend fun addSightDetails(id: String, name: String, city: String, category: String, description: String, walkDay: Int): String
    suspend fun updateSightDetailsRich(id: String, sightId: String, name: String, city: String, category: String, description: String, walkDay: Int)
    suspend fun addRestaurantDetails(input: RestaurantInput, tripId: String): String
    suspend fun updateRestaurantDetailsRich(tripId: String, restaurantId: String, input: RestaurantInput)
    suspend fun addAccommodationDetails(input: AccommodationInput, tripId: String): String
    suspend fun updateAccommodationDetailsRich(tripId: String, accommodationId: String, input: AccommodationInput)
    suspend fun addBudgetExpenseDetails(tripId: String, input: ExpenseInput)
    suspend fun updateBudgetExpenseDetails(tripId: String, expenseId: String, input: ExpenseInput)
}

class SupabaseTripRepository(private val client: SupabaseClient) : TripRepository {
    override suspend fun loadTrips(): List<TripCard> = client
        .from("trips")
        .select()
        .decodeList<TripRow>()
        .map { row ->
            fun text(key: String) = row.payload[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            TripCard(
                id = row.id,
                title = text("title").ifBlank { "Путешествие" },
                dates = text("dates"),
                status = text("status").ifBlank { "Черновик" },
                progress = row.payload["progress"]?.jsonPrimitive?.intOrNull?.coerceIn(0, 100) ?: 0,
                cities = text("cities"),
                coverImage = text("coverImage").takeIf(String::isNotBlank),
            )
        }

    override suspend fun loadTripOverview(id: String): TripOverview? {
        val row = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id } ?: return null
        fun text(key: String) = row.payload[key]?.jsonPrimitive?.contentOrNull.orEmpty()
        val covers = row.payload["coverPhotos"]?.jsonArray.orEmpty().mapNotNull { item ->
            val photo = item.jsonObject
            val imageUrl = photo["image"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            CoverPhoto(
                id = photo["id"]?.jsonPrimitive?.contentOrNull ?: imageUrl,
                imageUrl = imageUrl,
                city = photo["city"]?.jsonPrimitive?.contentOrNull.orEmpty(),
            )
        }
        val mapPoints = row.payload["overviewMapPoints"]?.jsonArray.orEmpty()
            .mapNotNull { it.jsonPrimitive.contentOrNull }
        val legs = row.payload["days"]?.jsonArray.orEmpty().mapNotNull { day ->
            val dayData = day.jsonObject
            val roadLeg = dayData["roadLeg"]?.jsonObject ?: return@mapNotNull null
            val from = roadLeg["from"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            val to = roadLeg["to"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            fun roadText(key: String) = roadLeg[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            RouteLeg(
                dayId = dayData["id"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                from = from,
                to = to,
                date = dayData["date"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                checkIn = listOf(roadText("checkInFrom"), roadText("checkInTo")).filter(String::isNotBlank).joinToString(" - "),
                checkOut = listOf(roadText("checkOutFrom"), roadText("checkOutTo")).filter(String::isNotBlank).joinToString(" - "),
                notes = roadText("notes"),
                mapsUrl = roadText("mapsUrl"),
                completed = roadLeg["completed"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull },
            )
        }
        val accommodations = row.payload["accommodations"]?.jsonArray.orEmpty().mapNotNull { item ->
            val accommodation = item.jsonObject
            val name = accommodation["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            fun accommodationText(key: String) = accommodation[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            Accommodation(
                id = accommodationText("id").ifBlank { name },
                city = accommodationText("city"),
                name = name,
                dates = accommodationText("dates"),
                price = accommodationText("price"),
                status = accommodationText("status"),
                details = accommodationText("details"),
                photos = accommodation["photos"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull },
                bookingUrl = accommodationText("bookingUrl"),
                deadline = accommodationText("deadline"),
                rating = accommodation["rating"]?.jsonPrimitive?.doubleOrNull
                    ?: accommodation["hotelRating"]?.jsonPrimitive?.doubleOrNull,
            )
        }
        val expenses = row.payload["budgetExpenses"]?.jsonArray.orEmpty().mapNotNull { item ->
            val expense = item.jsonObject
            val name = expense["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            fun expenseText(key: String) = expense[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            BudgetExpense(
                id = expenseText("id").ifBlank { name },
                name = name,
                amount = expense["amount"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                category = expenseText("category").ifBlank { "Прочее" },
                scope = expenseText("scope"),
                paidBy = expenseText("paidBy"),
            )
        }
        val groups = row.payload["budgetSplit"]?.jsonObject?.get("groups")?.jsonArray.orEmpty().mapNotNull { item ->
            val group = item.jsonObject
            val name = group["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            BudgetGroup(name, group["people"]?.jsonPrimitive?.intOrNull?.coerceAtLeast(1) ?: 1)
        }
        val members = row.payload["members"]?.jsonArray.orEmpty().mapNotNull { item ->
            val member = item.jsonObject
            val name = member["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            fun memberText(key: String) = member[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            TripMember(
                id = memberText("id").ifBlank { name },
                name = name,
                email = memberText("email"),
                role = memberText("role"),
                initials = memberText("initials").ifBlank { name.take(2).uppercase() },
                tone = memberText("tone"),
            )
        }
        val sights = row.payload["sights"]?.jsonArray.orEmpty().mapNotNull { item ->
            val sight = item.jsonObject
            val name = sight["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            fun sightText(key: String) = sight[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            val lngLat = sight["lnglat"]?.jsonArray.orEmpty()
            Sight(
                id = sightText("id").ifBlank { name },
                name = name,
                city = sightText("city"),
                photo = sightText("photo"),
                category = sightText("subcategory").ifBlank { sightText("group") },
                done = sight["done"]?.jsonPrimitive?.booleanOrNull ?: false,
                walkDay = sight["walkDay"]?.jsonPrimitive?.intOrNull ?: 0,
                walkOrder = sight["walkOrder"]?.jsonPrimitive?.intOrNull ?: 0,
                description = sightText("description"),
                longitude = lngLat.getOrNull(0)?.jsonPrimitive?.doubleOrNull,
                latitude = lngLat.getOrNull(1)?.jsonPrimitive?.doubleOrNull,
                rating = sight["rating"]?.jsonPrimitive?.doubleOrNull ?: sight["googleRating"]?.jsonPrimitive?.doubleOrNull,
            )
        }
        val restaurants = row.payload["restaurants"]?.jsonArray.orEmpty().mapNotNull { item ->
            val restaurant = item.jsonObject
            val name = restaurant["name"]?.jsonPrimitive?.contentOrNull ?: return@mapNotNull null
            fun restaurantText(key: String) = restaurant[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            Restaurant(
                id = restaurantText("id").ifBlank { name },
                name = name,
                city = restaurantText("city"),
                status = restaurantText("status"),
                photos = restaurant["photos"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull },
                rating = restaurant["googleRating"]?.jsonPrimitive?.doubleOrNull,
                reviews = restaurantText("googleReviews"),
                price = restaurantText("price"),
                note = restaurantText("note"),
                link = restaurantText("link"),
                date = restaurantText("date").ifBlank { restaurantText("dateTime") },
            )
        }
        return TripOverview(
            id = row.id,
            title = text("title").ifBlank { "Путешествие" },
            dates = text("dates"),
            status = text("status"),
            coverPhotos = covers.ifEmpty {
                text("coverImage").takeIf(String::isNotBlank)?.let { listOf(CoverPhoto("legacy", it, "")) }.orEmpty()
            },
            overviewMapPoints = mapPoints,
            routeLegs = legs,
            accommodations = accommodations,
            budgetCurrency = text("budgetCurrency").ifBlank { "EUR" },
            budgetExpenses = expenses,
            budgetGroups = groups,
            members = members,
            sights = sights,
            restaurants = restaurants,
            cities = text("cities").split(",").map(String::trim).filter(String::isNotBlank),
        )
    }

    override suspend fun createTrip(title: String, startDate: String, endDate: String, cities: String): TripCard {
        val id = UUID.randomUUID().toString()
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: error("Необходимо войти в аккаунт")
        val resolvedTitle = title.trim().ifBlank { "Без названия" }
        val dates = listOf(startDate, endDate).filter(String::isNotBlank).joinToString(" — ")
        val cityList = cities.split(",").map(String::trim).filter(String::isNotBlank)
        val payload = buildJsonObject {
            put("id", id)
            put("title", resolvedTitle)
            put("startDate", startDate)
            put("endDate", endDate)
            put("dates", dates)
            put("cities", cities.trim())
            put("status", "Черновик")
            put("progress", 0)
            put("isDraft", true)
            put("tone", "purple")
            put("coverPhotos", buildJsonArray { })
            put("sights", buildJsonArray { })
            put("restaurants", buildJsonArray { })
            put("accommodations", buildJsonArray { })
            put("budgetExpenses", buildJsonArray { })
            put("members", buildJsonArray { })
            if (cityList.size > 1) {
                put("days", buildJsonArray {
                    cityList.zipWithNext().forEachIndexed { index, (from, to) ->
                        add(buildJsonObject {
                            put("id", UUID.randomUUID().toString())
                            put("city", to)
                            put("date", "")
                            put("places", buildJsonArray { })
                            put("roadLeg", buildJsonObject {
                                put("from", from)
                                put("to", to)
                                put("completed", buildJsonArray { })
                            })
                            put("dayNumber", index + 1)
                        })
                    }
                })
            }
        }
        client.from("trips").insert(listOf(TripInsert(id, ownerId, payload)))
        return TripCard(id, resolvedTitle, dates, "Черновик", 0, cities.trim(), null)
    }

    override suspend fun updateTripSection(id: String, key: String, value: JsonElement) {
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val payload = TripPayloadCodec.withSection(current.payload, key, value)
        client.from("trips").update(TripPayloadUpdate(payload)) {
            filter { eq("id", id) }
        }
    }

    override suspend fun addRouteLeg(id: String, from: String, to: String) {
        require(from.isNotBlank() && to.isNotBlank()) { "Укажите оба города" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val day = buildJsonObject {
            put("id", UUID.randomUUID().toString())
            put("city", to.trim())
            put("places", buildJsonArray { })
            put("roadLeg", buildJsonObject {
                put("from", from.trim())
                put("to", to.trim())
                put("completed", buildJsonArray { })
            })
        }
        val days = buildJsonArray {
            current.payload["days"]?.jsonArray.orEmpty().forEach { add(it) }
            add(day)
        }
        updateTripSection(id, "days", days)
    }

    override suspend fun addBudgetExpense(id: String, name: String, amount: Double, category: String) {
        require(name.isNotBlank()) { "Укажите название траты" }
        require(amount > 0) { "Укажите сумму больше нуля" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val expenses = buildJsonArray {
            current.payload["budgetExpenses"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("name", name.trim())
                put("amount", amount)
                put("category", category)
                put("scope", "общий")
                put("paidBy", "Не указано")
            })
        }
        updateTripSection(id, "budgetExpenses", expenses)
    }

    override suspend fun updateMemberRole(id: String, memberId: String, role: String) {
        require(role == "Редактор" || role == "Читатель") { "Недопустимая роль" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val members = buildJsonArray {
            current.payload["members"]?.jsonArray.orEmpty().forEach { item ->
                val member = item.jsonObject
                if (member["id"]?.jsonPrimitive?.contentOrNull == memberId) {
                    add(JsonObject(member.toMutableMap().apply { put("role", kotlinx.serialization.json.JsonPrimitive(role)) }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "members", members)
    }

    override suspend fun updateAccommodationStatus(id: String, accommodationId: String, status: String) {
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val accommodations = buildJsonArray {
            current.payload["accommodations"]?.jsonArray.orEmpty().forEach { item ->
                val accommodation = item.jsonObject
                if (accommodation["id"]?.jsonPrimitive?.contentOrNull == accommodationId) {
                    add(JsonObject(accommodation.toMutableMap().apply { put("status", kotlinx.serialization.json.JsonPrimitive(status)) }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "accommodations", accommodations)
    }

    override suspend fun updateSightDone(id: String, sightId: String, done: Boolean) {
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val sights = buildJsonArray {
            current.payload["sights"]?.jsonArray.orEmpty().forEach { item ->
                val sight = item.jsonObject
                if (sight["id"]?.jsonPrimitive?.contentOrNull == sightId) {
                    add(JsonObject(sight.toMutableMap().apply { put("done", kotlinx.serialization.json.JsonPrimitive(done)) }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "sights", sights)
    }

    override suspend fun addRestaurant(id: String, name: String, city: String, status: String) {
        require(name.isNotBlank()) { "Укажите название ресторана" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val restaurants = buildJsonArray {
            current.payload["restaurants"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("name", name.trim())
                put("city", city.trim())
                put("status", status)
                put("photos", buildJsonArray { })
            })
        }
        updateTripSection(id, "restaurants", restaurants)
    }

    override suspend fun addAccommodation(id: String, name: String, city: String, dates: String, price: String, status: String) {
        require(name.isNotBlank()) { "Укажите название жилья" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val accommodations = buildJsonArray {
            current.payload["accommodations"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("name", name.trim())
                put("city", city.trim())
                put("dates", dates.trim())
                put("price", price.trim())
                put("status", status)
                put("photos", buildJsonArray { })
            })
        }
        updateTripSection(id, "accommodations", accommodations)
    }

    override suspend fun addSight(id: String, name: String, city: String, category: String) {
        require(name.isNotBlank()) { "Укажите название места" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val sights = buildJsonArray {
            current.payload["sights"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("name", name.trim())
                put("city", city.trim())
                put("subcategory", category)
                put("done", false)
            })
        }
        updateTripSection(id, "sights", sights)
    }

    override suspend fun updateRouteChecklist(id: String, dayId: String, itemId: String, completed: Boolean) {
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val days = buildJsonArray {
            current.payload["days"]?.jsonArray.orEmpty().forEach { item ->
                val day = item.jsonObject
                if (day["id"]?.jsonPrimitive?.contentOrNull == dayId) {
                    val roadLeg = day["roadLeg"]?.jsonObject ?: run { add(item); return@forEach }
                    val existing = roadLeg["completed"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull }
                    val next = if (completed) (existing + itemId).distinct() else existing - itemId
                    val nextRoadLeg = JsonObject(roadLeg.toMutableMap().apply {
                        put("completed", buildJsonArray { next.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
                    })
                    add(JsonObject(day.toMutableMap().apply { put("roadLeg", nextRoadLeg) }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "days", days)
    }

    override suspend fun addMember(id: String, name: String, email: String, role: String) {
        require(name.isNotBlank()) { "Укажите имя участника" }
        require(email.contains("@")) { "Укажите корректный e-mail" }
        require(role == "Редактор" || role == "Читатель") { "Недопустимая роль" }
        client.functions.invoke(
            function = "send-invite",
            body = buildJsonObject {
                put("email", email.trim().lowercase())
                put("name", name.trim())
                put("role", role)
                put("tripId", id)
                put("redirectTo", "https://travelplanner.muntim.ru")
            },
            headers = Headers.build { append(HttpHeaders.ContentType, "application/json") },
        )
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val members = buildJsonArray {
            current.payload["members"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("name", name.trim())
                put("email", email.trim().lowercase())
                put("role", role)
                put("initials", name.trim().take(2).uppercase())
                put("tone", "blue")
            })
        }
        updateTripSection(id, "members", members)
    }

    override suspend fun addCoverPhoto(id: String, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: error("Необходимо войти в аккаунт")
        val path = "$ownerId/$id/covers/${UUID.randomUUID()}.jpg"
        client.storage.from("trip-photos").upload(path, bytes)
        val imageUrl = client.storage.from("trip-photos").publicUrl(path)
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val photos = buildJsonArray {
            current.payload["coverPhotos"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("image", imageUrl)
            })
        }
        val payload = JsonObject(current.payload.toMutableMap().apply {
            put("coverPhotos", photos)
            if (this["coverImage"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) put("coverImage", kotlinx.serialization.json.JsonPrimitive(imageUrl))
        })
        client.from("trips").update(TripPayloadUpdate(payload)) { filter { eq("id", id) } }
    }

    override suspend fun updateTripDetails(id: String, title: String, dates: String, cities: String) {
        require(title.isNotBlank()) { "Укажите название путешествия" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val payload = JsonObject(current.payload.toMutableMap().apply {
            put("title", kotlinx.serialization.json.JsonPrimitive(title.trim()))
            put("dates", kotlinx.serialization.json.JsonPrimitive(dates.trim()))
            put("cities", kotlinx.serialization.json.JsonPrimitive(cities.trim()))
        })
        client.from("trips").update(TripPayloadUpdate(payload)) { filter { eq("id", id) } }
    }

    override suspend fun updateRestaurantStatus(id: String, restaurantId: String, status: String) {
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val restaurants = buildJsonArray {
            current.payload["restaurants"]?.jsonArray.orEmpty().forEach { item ->
                val restaurant = item.jsonObject
                if (restaurant["id"]?.jsonPrimitive?.contentOrNull == restaurantId) {
                    add(JsonObject(restaurant.toMutableMap().apply { put("status", kotlinx.serialization.json.JsonPrimitive(status)) }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "restaurants", restaurants)
    }

    override suspend fun updateBudgetExpense(id: String, expenseId: String, name: String, amount: Double, category: String) {
        require(name.isNotBlank()) { "Укажите название траты" }
        require(amount > 0) { "Укажите сумму больше нуля" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val expenses = buildJsonArray {
            current.payload["budgetExpenses"]?.jsonArray.orEmpty().forEach { item ->
                val expense = item.jsonObject
                if (expense["id"]?.jsonPrimitive?.contentOrNull == expenseId) {
                    add(JsonObject(expense.toMutableMap().apply {
                        put("name", kotlinx.serialization.json.JsonPrimitive(name.trim()))
                        put("amount", kotlinx.serialization.json.JsonPrimitive(amount))
                        put("category", kotlinx.serialization.json.JsonPrimitive(category))
                    }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "budgetExpenses", expenses)
    }

    override suspend fun updateRouteLegCities(id: String, dayId: String, from: String, to: String) {
        require(from.isNotBlank() && to.isNotBlank()) { "Укажите оба города" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val days = buildJsonArray {
            current.payload["days"]?.jsonArray.orEmpty().forEach { item ->
                val day = item.jsonObject
                if (day["id"]?.jsonPrimitive?.contentOrNull == dayId) {
                    val roadLeg = day["roadLeg"]?.jsonObject ?: run { add(item); return@forEach }
                    val nextRoadLeg = JsonObject(roadLeg.toMutableMap().apply {
                        put("from", kotlinx.serialization.json.JsonPrimitive(from.trim()))
                        put("to", kotlinx.serialization.json.JsonPrimitive(to.trim()))
                    })
                    add(JsonObject(day.toMutableMap().apply {
                        put("city", kotlinx.serialization.json.JsonPrimitive(to.trim()))
                        put("roadLeg", nextRoadLeg)
                    }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "days", days)
    }

    override suspend fun updateRouteLegDetails(id: String, dayId: String, from: String, to: String, checkIn: String, checkOut: String, notes: String, mapsUrl: String) {
        require(from.isNotBlank() && to.isNotBlank()) { "Укажите оба города" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id } ?: error("Путешествие не найдено")
        val days = buildJsonArray {
            current.payload["days"]?.jsonArray.orEmpty().forEach { item ->
                val day = item.jsonObject
                if (day["id"]?.jsonPrimitive?.contentOrNull == dayId) {
                    val leg = day["roadLeg"]?.jsonObject ?: run { add(item); return@forEach }
                    val nextLeg = JsonObject(leg.toMutableMap().apply {
                        put("from", kotlinx.serialization.json.JsonPrimitive(from.trim())); put("to", kotlinx.serialization.json.JsonPrimitive(to.trim()))
                        put("checkInFrom", kotlinx.serialization.json.JsonPrimitive(checkIn.trim())); put("checkOutFrom", kotlinx.serialization.json.JsonPrimitive(checkOut.trim()))
                        put("notes", kotlinx.serialization.json.JsonPrimitive(notes.trim())); put("mapsUrl", kotlinx.serialization.json.JsonPrimitive(mapsUrl.trim()))
                    })
                    add(JsonObject(day.toMutableMap().apply { put("city", kotlinx.serialization.json.JsonPrimitive(to.trim())); put("roadLeg", nextLeg) }))
                } else add(item)
            }
        }
        updateTripSection(id, "days", days)
    }

    override suspend fun updateRestaurantDetails(id: String, restaurantId: String, name: String, city: String, note: String) {
        require(name.isNotBlank()) { "Укажите название ресторана" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val restaurants = buildJsonArray {
            current.payload["restaurants"]?.jsonArray.orEmpty().forEach { item ->
                val restaurant = item.jsonObject
                if (restaurant["id"]?.jsonPrimitive?.contentOrNull == restaurantId) {
                    add(JsonObject(restaurant.toMutableMap().apply {
                        put("name", kotlinx.serialization.json.JsonPrimitive(name.trim()))
                        put("city", kotlinx.serialization.json.JsonPrimitive(city.trim()))
                        put("note", kotlinx.serialization.json.JsonPrimitive(note.trim()))
                    }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "restaurants", restaurants)
    }

    override suspend fun updateAccommodationDetails(id: String, accommodationId: String, name: String, city: String, dates: String, price: String) {
        require(name.isNotBlank()) { "Укажите название жилья" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val accommodations = buildJsonArray {
            current.payload["accommodations"]?.jsonArray.orEmpty().forEach { item ->
                val accommodation = item.jsonObject
                if (accommodation["id"]?.jsonPrimitive?.contentOrNull == accommodationId) {
                    add(JsonObject(accommodation.toMutableMap().apply {
                        put("name", kotlinx.serialization.json.JsonPrimitive(name.trim()))
                        put("city", kotlinx.serialization.json.JsonPrimitive(city.trim()))
                        put("dates", kotlinx.serialization.json.JsonPrimitive(dates.trim()))
                        put("price", kotlinx.serialization.json.JsonPrimitive(price.trim()))
                    }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "accommodations", accommodations)
    }

    override suspend fun updateSightDetails(id: String, sightId: String, name: String, city: String, category: String) {
        require(name.isNotBlank()) { "Укажите название места" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val sights = buildJsonArray {
            current.payload["sights"]?.jsonArray.orEmpty().forEach { item ->
                val sight = item.jsonObject
                if (sight["id"]?.jsonPrimitive?.contentOrNull == sightId) {
                    add(JsonObject(sight.toMutableMap().apply {
                        put("name", kotlinx.serialization.json.JsonPrimitive(name.trim()))
                        put("city", kotlinx.serialization.json.JsonPrimitive(city.trim()))
                        put("subcategory", kotlinx.serialization.json.JsonPrimitive(category.trim()))
                    }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "sights", sights)
    }

    override suspend fun addBudgetGroup(id: String, name: String, people: Int) {
        require(name.isNotBlank()) { "Укажите название группы" }
        require(people > 0) { "Укажите количество участников" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val split = current.payload["budgetSplit"]?.jsonObject.orEmpty()
        val groups = buildJsonArray {
            split["groups"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("name", name.trim())
                put("people", people)
            })
        }
        updateTripSection(id, "budgetSplit", JsonObject(split.toMutableMap().apply { put("groups", groups) }))
    }

    override suspend fun addAccommodationPhoto(id: String, accommodationId: String, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: error("Необходимо войти в аккаунт")
        val path = "$ownerId/$id/accommodations/$accommodationId/${UUID.randomUUID()}.jpg"
        client.storage.from("trip-photos").upload(path, bytes)
        val imageUrl = client.storage.from("trip-photos").publicUrl(path)
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val accommodations = buildJsonArray {
            current.payload["accommodations"]?.jsonArray.orEmpty().forEach { item ->
                val accommodation = item.jsonObject
                if (accommodation["id"]?.jsonPrimitive?.contentOrNull == accommodationId) {
                    val photos = buildJsonArray {
                        accommodation["photos"]?.jsonArray.orEmpty().forEach { add(it) }
                        add(kotlinx.serialization.json.JsonPrimitive(imageUrl))
                    }
                    add(JsonObject(accommodation.toMutableMap().apply { put("photos", photos) }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "accommodations", accommodations)
    }

    override suspend fun addSightPhoto(id: String, sightId: String, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: error("Необходимо войти в аккаунт")
        val path = "$ownerId/$id/sights/$sightId/${UUID.randomUUID()}.jpg"
        client.storage.from("trip-photos").upload(path, bytes)
        val imageUrl = client.storage.from("trip-photos").publicUrl(path)
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id } ?: error("Путешествие не найдено")
        val sights = buildJsonArray {
            current.payload["sights"]?.jsonArray.orEmpty().forEach { item ->
                val sight = item.jsonObject
                if (sight["id"]?.jsonPrimitive?.contentOrNull == sightId) {
                    add(JsonObject(sight.toMutableMap().apply { put("photo", kotlinx.serialization.json.JsonPrimitive(imageUrl)) }))
                } else add(item)
            }
        }
        updateTripSection(id, "sights", sights)
    }

    override suspend fun moveAccommodationPhoto(id: String, accommodationId: String, photoIndex: Int, direction: Int) {
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id } ?: error("Путешествие не найдено")
        val accommodations = buildJsonArray {
            current.payload["accommodations"]?.jsonArray.orEmpty().forEach { item ->
                val accommodation = item.jsonObject
                if (accommodation["id"]?.jsonPrimitive?.contentOrNull == accommodationId) {
                    val photos = accommodation["photos"]?.jsonArray.orEmpty().toMutableList()
                    val target = photoIndex + direction
                    if (photoIndex in photos.indices && target in photos.indices) {
                        val moved = photos.removeAt(photoIndex)
                        photos.add(target, moved)
                    }
                    add(JsonObject(accommodation.toMutableMap().apply { put("photos", buildJsonArray { photos.forEach { add(it) } }) }))
                } else add(item)
            }
        }
        updateTripSection(id, "accommodations", accommodations)
    }

    override suspend fun moveRestaurantPhoto(id: String, restaurantId: String, photoIndex: Int, direction: Int) {
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id } ?: error("Путешествие не найдено")
        val restaurants = buildJsonArray {
            current.payload["restaurants"]?.jsonArray.orEmpty().forEach { item ->
                val restaurant = item.jsonObject
                if (restaurant["id"]?.jsonPrimitive?.contentOrNull == restaurantId) {
                    val photos = restaurant["photos"]?.jsonArray.orEmpty().toMutableList()
                    val target = photoIndex + direction
                    if (photoIndex in photos.indices && target in photos.indices) photos.add(target, photos.removeAt(photoIndex))
                    add(JsonObject(restaurant.toMutableMap().apply { put("photos", buildJsonArray { photos.forEach { add(it) } }) }))
                } else add(item)
            }
        }
        updateTripSection(id, "restaurants", restaurants)
    }

    override suspend fun addRestaurantPhoto(id: String, restaurantId: String, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: error("Необходимо войти в аккаунт")
        val path = "$ownerId/$id/restaurants/$restaurantId/${UUID.randomUUID()}.jpg"
        client.storage.from("trip-photos").upload(path, bytes)
        val imageUrl = client.storage.from("trip-photos").publicUrl(path)
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val restaurants = buildJsonArray {
            current.payload["restaurants"]?.jsonArray.orEmpty().forEach { item ->
                val restaurant = item.jsonObject
                if (restaurant["id"]?.jsonPrimitive?.contentOrNull == restaurantId) {
                    val photos = buildJsonArray {
                        restaurant["photos"]?.jsonArray.orEmpty().forEach { add(it) }
                        add(kotlinx.serialization.json.JsonPrimitive(imageUrl))
                    }
                    add(JsonObject(restaurant.toMutableMap().apply { put("photos", photos) }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "restaurants", restaurants)
    }

    override suspend fun deleteTripItem(id: String, section: String, itemId: String) {
        require(section in setOf("days", "sights", "restaurants", "accommodations", "budgetExpenses", "members", "coverPhotos")) {
            "Недопустимый раздел"
        }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val payload = TripPayloadCodec.removeArrayItem(current.payload, section, itemId)
        client.from("trips").update(TripPayloadUpdate(payload)) { filter { eq("id", id) } }
    }

    override suspend fun addSightDetails(
        id: String,
        name: String,
        city: String,
        category: String,
        description: String,
        walkDay: Int,
    ): String {
        require(name.isNotBlank()) { "Укажите название места" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val sightId = UUID.randomUUID().toString()
        val item = buildJsonObject {
            put("id", sightId)
            put("name", name.trim())
            put("city", city.trim())
            put("subcategory", category.trim())
            put("description", description.trim())
            put("walkDay", walkDay.coerceAtLeast(0))
            put("walkOrder", 0)
            put("done", false)
        }
        client.from("trips").update(TripPayloadUpdate(TripPayloadCodec.append(current.payload, "sights", item))) {
            filter { eq("id", id) }
        }
        return sightId
    }

    override suspend fun updateSightDetailsRich(
        id: String,
        sightId: String,
        name: String,
        city: String,
        category: String,
        description: String,
        walkDay: Int,
    ) {
        require(name.isNotBlank()) { "Укажите название места" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == id }
            ?: error("Путешествие не найдено")
        val payload = TripPayloadCodec.updateArrayItem(current.payload, "sights", sightId) { sight ->
            JsonObject(sight.toMutableMap().apply {
                put("name", kotlinx.serialization.json.JsonPrimitive(name.trim()))
                put("city", kotlinx.serialization.json.JsonPrimitive(city.trim()))
                put("subcategory", kotlinx.serialization.json.JsonPrimitive(category.trim()))
                put("description", kotlinx.serialization.json.JsonPrimitive(description.trim()))
                put("walkDay", kotlinx.serialization.json.JsonPrimitive(walkDay.coerceAtLeast(0)))
            })
        }
        client.from("trips").update(TripPayloadUpdate(payload)) { filter { eq("id", id) } }
    }

    override suspend fun addRestaurantDetails(input: RestaurantInput, tripId: String): String {
        require(input.name.isNotBlank()) { "Укажите название ресторана" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == tripId }
            ?: error("Путешествие не найдено")
        val restaurantId = UUID.randomUUID().toString()
        val item = buildJsonObject {
            put("id", restaurantId)
            put("name", input.name.trim())
            put("city", input.city.trim())
            put("status", input.status)
            put("note", input.note.trim())
            put("price", input.price.trim())
            put("link", input.link.trim())
            put("date", input.date.trim())
            put("photos", buildJsonArray { })
        }
        client.from("trips").update(TripPayloadUpdate(TripPayloadCodec.append(current.payload, "restaurants", item))) {
            filter { eq("id", tripId) }
        }
        return restaurantId
    }

    override suspend fun updateRestaurantDetailsRich(tripId: String, restaurantId: String, input: RestaurantInput) {
        require(input.name.isNotBlank()) { "Укажите название ресторана" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == tripId }
            ?: error("Путешествие не найдено")
        val payload = TripPayloadCodec.updateArrayItem(current.payload, "restaurants", restaurantId) { restaurant ->
            JsonObject(restaurant.toMutableMap().apply {
                put("name", kotlinx.serialization.json.JsonPrimitive(input.name.trim()))
                put("city", kotlinx.serialization.json.JsonPrimitive(input.city.trim()))
                put("status", kotlinx.serialization.json.JsonPrimitive(input.status))
                put("note", kotlinx.serialization.json.JsonPrimitive(input.note.trim()))
                put("price", kotlinx.serialization.json.JsonPrimitive(input.price.trim()))
                put("link", kotlinx.serialization.json.JsonPrimitive(input.link.trim()))
                put("date", kotlinx.serialization.json.JsonPrimitive(input.date.trim()))
            })
        }
        client.from("trips").update(TripPayloadUpdate(payload)) { filter { eq("id", tripId) } }
    }

    override suspend fun addAccommodationDetails(input: AccommodationInput, tripId: String): String {
        require(input.name.isNotBlank()) { "Укажите название жилья" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == tripId }
            ?: error("Путешествие не найдено")
        val accommodationId = UUID.randomUUID().toString()
        val item = buildJsonObject {
            put("id", accommodationId)
            put("name", input.name.trim())
            put("city", input.city.trim())
            put("dates", input.dates.trim())
            put("price", input.price.trim())
            put("status", input.status)
            put("details", input.details.trim())
            put("bookingUrl", input.bookingUrl.trim())
            put("photos", buildJsonArray { })
        }
        client.from("trips").update(TripPayloadUpdate(TripPayloadCodec.append(current.payload, "accommodations", item))) {
            filter { eq("id", tripId) }
        }
        return accommodationId
    }

    override suspend fun updateAccommodationDetailsRich(tripId: String, accommodationId: String, input: AccommodationInput) {
        require(input.name.isNotBlank()) { "Укажите название жилья" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == tripId }
            ?: error("Путешествие не найдено")
        val payload = TripPayloadCodec.updateArrayItem(current.payload, "accommodations", accommodationId) { accommodation ->
            JsonObject(accommodation.toMutableMap().apply {
                put("name", kotlinx.serialization.json.JsonPrimitive(input.name.trim()))
                put("city", kotlinx.serialization.json.JsonPrimitive(input.city.trim()))
                put("dates", kotlinx.serialization.json.JsonPrimitive(input.dates.trim()))
                put("price", kotlinx.serialization.json.JsonPrimitive(input.price.trim()))
                put("status", kotlinx.serialization.json.JsonPrimitive(input.status))
                put("details", kotlinx.serialization.json.JsonPrimitive(input.details.trim()))
                put("bookingUrl", kotlinx.serialization.json.JsonPrimitive(input.bookingUrl.trim()))
            })
        }
        client.from("trips").update(TripPayloadUpdate(payload)) { filter { eq("id", tripId) } }
    }

    override suspend fun addBudgetExpenseDetails(tripId: String, input: ExpenseInput) {
        require(input.name.isNotBlank()) { "Укажите название траты" }
        require(input.amount > 0) { "Укажите сумму больше нуля" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == tripId }
            ?: error("Путешествие не найдено")
        val item = buildJsonObject {
            put("id", UUID.randomUUID().toString())
            put("name", input.name.trim())
            put("amount", input.amount)
            put("category", input.category)
            put("scope", input.scope)
            put("paidBy", input.paidBy)
        }
        client.from("trips").update(TripPayloadUpdate(TripPayloadCodec.append(current.payload, "budgetExpenses", item))) {
            filter { eq("id", tripId) }
        }
    }

    override suspend fun updateBudgetExpenseDetails(tripId: String, expenseId: String, input: ExpenseInput) {
        require(input.name.isNotBlank()) { "Укажите название траты" }
        require(input.amount > 0) { "Укажите сумму больше нуля" }
        val current = client.from("trips").select().decodeList<TripRow>().firstOrNull { it.id == tripId }
            ?: error("Путешествие не найдено")
        val payload = TripPayloadCodec.updateArrayItem(current.payload, "budgetExpenses", expenseId) { expense ->
            JsonObject(expense.toMutableMap().apply {
                put("name", kotlinx.serialization.json.JsonPrimitive(input.name.trim()))
                put("amount", kotlinx.serialization.json.JsonPrimitive(input.amount))
                put("category", kotlinx.serialization.json.JsonPrimitive(input.category))
                put("scope", kotlinx.serialization.json.JsonPrimitive(input.scope))
                put("paidBy", kotlinx.serialization.json.JsonPrimitive(input.paidBy))
            })
        }
        client.from("trips").update(TripPayloadUpdate(payload)) { filter { eq("id", tripId) } }
    }
}
