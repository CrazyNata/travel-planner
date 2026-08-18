package com.odyssey.travelplanner.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import io.github.jan.supabase.storage.storage
import io.github.jan.supabase.functions.functions
import io.ktor.http.Headers
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
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
import java.time.LocalDate
import java.util.Locale
import java.util.UUID

@Serializable
data class TripRow(
    val id: String,
    val payload: JsonObject,
    @SerialName("owner_id") val ownerId: String? = null,
    val revision: Long = 0,
)

@Serializable
private data class TripInsert(
    val id: String,
    @SerialName("owner_id") val ownerId: String,
    val payload: JsonObject,
)

data class TripCard(
    val id: String,
    val title: String,
    val dates: String,
    val status: String,
    val progress: Int,
    val cities: String,
    val coverImage: String?,
    val isOwner: Boolean,
    val canEdit: Boolean = isOwner,
)

@Serializable
private data class TripCollaboratorRow(
    @SerialName("trip_id") val tripId: String,
    @SerialName("user_id") val userId: String,
    val role: String,
)

data class CoverPhoto(val id: String, val imageUrl: String, val city: String)
data class RouteLeg(
    val dayId: String,
    val from: String,
    val to: String,
    val date: String,
    val dateDay: String = "",
    val dateMonth: String = "",
    val weekday: String = "",
    val distance: String = "",
    val travelTime: String = "",
    val checkIn: String,
    val checkOut: String,
    val notes: String,
    val mapsUrl: String,
    val completed: List<String>,
    val dayNumber: Int = 0,
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
    // Optional fields extend the existing JSON payload without requiring a new table.
    val deadline: String = "",
    val rating: Double? = null,
    val source: String = "manual",
    val googlePlaceId: String = "",
    val bookingPropertyId: String = "",
    val externalUrl: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val reviewCount: Int? = null,
    val photoReference: String = "",
    val website: String = "",
    val phone: String = "",
    val type: String = "",
    val tripCityId: String = "",
)
data class BudgetExpense(
    val id: String,
    val name: String,
    val amount: Double,
    val category: String,
    val scope: String,
    val paidBy: String,
    val date: String = "",
)
data class BudgetGroup(val name: String, val people: Int)
data class TripMember(val id: String, val name: String, val email: String, val role: String, val initials: String, val tone: String)
data class Sight(
    val id: String,
    val name: String,
    val city: String,
    val photo: String,
    val category: String,
    val done: Boolean,
    val walkDay: Int,
    val walkOrder: Int,
    val description: String,
    val longitude: Double?,
    val latitude: Double?,
    val rating: Double? = null,
    val link: String = "",
    val photoUnavailable: Boolean = false,
)

private fun jsonText(element: JsonElement?): String =
    runCatching { element?.jsonPrimitive?.contentOrNull?.trim().orEmpty() }.getOrDefault("")

private fun photoReferences(element: JsonElement?): List<String> = when (element) {
    is JsonObject -> listOf("url", "image", "photo", "imageUrl", "photoUrl", "src", "path")
        .flatMap { key -> photoReferences(element[key]) }
        .distinct()
    is kotlinx.serialization.json.JsonArray -> element.flatMap(::photoReferences).distinct()
    is kotlinx.serialization.json.JsonPrimitive -> listOfNotNull(element.contentOrNull?.trim()?.takeIf(String::isNotBlank))
    else -> emptyList()
}

private fun accommodationPhotoReferences(accommodation: JsonObject): List<String> =
    listOf("photos", "photo", "image", "imageUrl", "photoUrl")
        .flatMap { key -> photoReferences(accommodation[key]) }
        .distinct()

private fun sightPhotoUrl(sight: JsonObject): String {
    listOf("photo", "image", "photoUrl", "imageUrl").forEach { key ->
        jsonText(sight[key]).takeIf { it.isNotBlank() }?.let { return it }
    }
    return sight["photos"]?.let { element ->
        runCatching {
            element.jsonArray.firstNotNullOfOrNull { photo ->
                jsonText(photo).takeIf { it.isNotBlank() }
                    ?: runCatching { jsonText(photo.jsonObject["url"]) }.getOrDefault("").takeIf { it.isNotBlank() }
                    ?: runCatching { jsonText(photo.jsonObject["image"]) }.getOrDefault("").takeIf { it.isNotBlank() }
            }.orEmpty()
        }.getOrDefault("")
    }.orEmpty()
}
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
    val priority: Boolean = false,
)
data class TripOverview(
    val id: String,
    val title: String,
    val dates: String,
    val status: String,
    val coverPhotos: List<CoverPhoto>,
    val overviewMapPoints: List<String>,
    val overviewWeatherCities: List<String> = emptyList(),
    val overviewBlocks: List<String> = emptyList(),
    val routeLegs: List<RouteLeg>,
    val accommodations: List<Accommodation>,
    val budgetCurrency: String,
    val budgetManualRates: Map<String, Double> = emptyMap(),
    val budgetExpenses: List<BudgetExpense>,
    val budgetGroups: List<BudgetGroup>,
    val members: List<TripMember>,
    val sights: List<Sight>,
    val restaurants: List<Restaurant>,
    val cities: List<String> = emptyList(),
    val cityCoordinates: Map<String, CityLocation> = emptyMap(),
    val routeDayCount: Int = 0,
    val currentUserRole: String = "",
    val canEdit: Boolean = false,
)

private data class ResolvedTripOverviewPhotos(
    val covers: List<CoverPhoto>,
    val accommodations: List<Accommodation>,
    val sights: List<Sight>,
    val restaurants: List<Restaurant>,
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
    val priority: Boolean = false,
)

private fun normalizeRestaurantStatus(status: String): String = when (status.trim().lowercase(Locale.ROOT)) {
    "want", "хочу" -> "хочу"
    "reserve", "reserved", "бронь" -> "бронь"
    "visited", "были" -> "были"
    else -> status.trim()
}

data class AccommodationInput(
    val name: String,
    val city: String,
    val dates: String,
    val price: String,
    val status: String,
    val details: String = "",
    val bookingUrl: String = "",
    val deadline: String = "",
    val source: String = "",
    val googlePlaceId: String = "",
    val bookingPropertyId: String = "",
    val externalUrl: String = "",
    val address: String = "",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val rating: Double? = null,
    val reviewCount: Int? = null,
    val photoReference: String = "",
    val website: String = "",
    val phone: String = "",
    val type: String = "",
    val tripCityId: String = "",
)

data class ExpenseInput(
    val name: String,
    val amount: Double,
    val category: String,
    val scope: String = "общий",
    val paidBy: String = "Не указано",
    val date: String = "",
)

private fun collectTripPhotoPaths(payload: JsonObject): Set<String> {
    val paths = linkedSetOf<String>()

    fun visit(element: JsonElement) {
        when (element) {
            is JsonObject -> element.values.forEach(::visit)
            is kotlinx.serialization.json.JsonArray -> element.forEach(::visit)
            is kotlinx.serialization.json.JsonPrimitive -> {
                tripPhotoPath(element.contentOrNull)?.let { paths += it }
            }
        }
    }

    visit(payload)
    return paths
}

interface TripRepository {
    suspend fun loadTrips(): List<TripCard>
    suspend fun loadTripOverview(id: String): TripOverview?
    suspend fun createTrip(
        title: String,
        startDate: String,
        endDate: String,
        cities: String,
        cityCoordinates: Map<String, CityLocation> = emptyMap(),
    ): TripCard
    suspend fun deleteTrip(id: String)
    suspend fun leaveTrip(id: String)
    suspend fun updateTripSection(id: String, key: String, value: JsonElement)
    suspend fun addRouteLeg(
        id: String,
        from: String,
        to: String,
        checkIn: String = "",
        checkOut: String = "",
        notes: String = "",
        mapsUrl: String = "",
        dateDay: String = "",
        dateMonth: String = "",
        weekday: String = "",
        distance: String = "",
        travelTime: String = "",
        date: String = "",
    )
    suspend fun reorderRouteLegs(id: String, orderedDayIds: List<String>)
    suspend fun addBudgetExpense(id: String, name: String, amount: Double, category: String)
    suspend fun updateMemberRole(id: String, memberId: String, role: String)
    suspend fun updateAccommodationStatus(id: String, accommodationId: String, status: String)
    suspend fun updateSightDone(id: String, sightId: String, done: Boolean)
    suspend fun addRestaurant(id: String, name: String, city: String, status: String)
    suspend fun addAccommodation(id: String, name: String, city: String, dates: String, price: String, status: String)
    suspend fun addSight(id: String, name: String, city: String, category: String)
    suspend fun updateRouteChecklist(id: String, dayId: String, itemId: String, completed: Boolean)
    suspend fun addMember(id: String, name: String, email: String, role: String)
    suspend fun addCoverPhoto(id: String, bytes: ByteArray, city: String = "")
    suspend fun updateTripDetails(id: String, title: String, dates: String, cities: String)
    suspend fun updateRestaurantStatus(id: String, restaurantId: String, status: String)
    suspend fun updateBudgetExpense(id: String, expenseId: String, name: String, amount: Double, category: String)
    suspend fun updateRouteLegCities(id: String, dayId: String, from: String, to: String)
    suspend fun updateRouteLegDetails(
        id: String,
        dayId: String,
        from: String,
        to: String,
        checkIn: String,
        checkOut: String,
        notes: String,
        mapsUrl: String,
        dateDay: String = "",
        dateMonth: String = "",
        weekday: String = "",
        distance: String = "",
        travelTime: String = "",
        date: String = "",
    )
    suspend fun updateRestaurantDetails(id: String, restaurantId: String, name: String, city: String, note: String)
    suspend fun updateAccommodationDetails(id: String, accommodationId: String, name: String, city: String, dates: String, price: String)
    suspend fun updateSightDetails(id: String, sightId: String, name: String, city: String, category: String)
    suspend fun addBudgetGroup(id: String, name: String, people: Int)
    suspend fun addAccommodationPhoto(id: String, accommodationId: String, bytes: ByteArray): String
    suspend fun replaceAccommodationCoverPhoto(id: String, accommodationId: String, bytes: ByteArray): String
    suspend fun addSightPhoto(id: String, sightId: String, bytes: ByteArray)
    suspend fun moveAccommodationPhoto(id: String, accommodationId: String, photoIndex: Int, direction: Int)
    suspend fun deleteAccommodationPhoto(id: String, accommodationId: String, photoIndex: Int)
    suspend fun deleteAccommodation(id: String, accommodationId: String)
    suspend fun moveRestaurantPhoto(id: String, restaurantId: String, photoIndex: Int, direction: Int)
    suspend fun addRestaurantPhoto(id: String, restaurantId: String, bytes: ByteArray)
    suspend fun replaceRestaurantCoverPhoto(id: String, restaurantId: String, bytes: ByteArray)
    suspend fun deleteTripItem(id: String, section: String, itemId: String)
    suspend fun deleteSightDay(id: String, walkDay: Int)
    suspend fun addSightDetails(
        id: String,
        name: String,
        city: String,
        category: String,
        description: String,
        walkDay: Int,
        longitude: Double? = null,
        latitude: Double? = null,
        link: String = "",
    ): String
    suspend fun addCatalogSights(
        id: String,
        city: String,
        language: String,
        walkDay: Int,
        entries: List<SightCatalogEntry>,
    )
    suspend fun updateSightDetailsRich(
        id: String,
        sightId: String,
        name: String,
        city: String,
        category: String,
        description: String,
        walkDay: Int,
        longitude: Double? = null,
        latitude: Double? = null,
        locationChanged: Boolean = false,
        link: String = "",
    )
    suspend fun reorderSights(id: String, orderedSightIds: List<String>)
    suspend fun addRestaurantDetails(input: RestaurantInput, tripId: String): String
    suspend fun updateRestaurantDetailsRich(tripId: String, restaurantId: String, input: RestaurantInput)
    suspend fun addAccommodationDetails(input: AccommodationInput, tripId: String): String
    suspend fun updateAccommodationDetailsRich(tripId: String, accommodationId: String, input: AccommodationInput)
    suspend fun addBudgetExpenseDetails(tripId: String, input: ExpenseInput)
    suspend fun updateBudgetExpenseDetails(tripId: String, expenseId: String, input: ExpenseInput)
}

class AuthSessionRequiredException : IllegalStateException()

class SupabaseTripRepository(private val client: SupabaseClient) : TripRepository {
    private fun roleCanEdit(role: String): Boolean = role == "Владелец" || role == "Редактор"

    private suspend fun currentUserRole(row: TripRow, currentUserId: String?): String {
        if (currentUserId.isNullOrBlank()) return ""
        if (row.ownerId == currentUserId) return "Владелец"

        val collaboratorRole = runCatching {
            client.from("trip_collaborators").select {
                filter {
                    eq("trip_id", row.id)
                    eq("user_id", currentUserId)
                }
            }.decodeList<TripCollaboratorRow>().firstOrNull()?.role.orEmpty()
        }.getOrDefault("")
        if (collaboratorRole.isNotBlank()) return collaboratorRole

        // Keep older trips usable until their member list is rewritten by the
        // invitation RPC and contains userId.
        val currentEmail = client.auth.currentUserOrNull()?.email?.trim()?.lowercase(Locale.ROOT).orEmpty()
        return row.payload["members"]?.jsonArray.orEmpty()
            .firstOrNull { member ->
                val memberObject = member.jsonObject
                memberObject["userId"]?.jsonPrimitive?.contentOrNull == currentUserId ||
                    (currentEmail.isNotBlank() && memberObject["email"]?.jsonPrimitive?.contentOrNull?.trim()?.lowercase(Locale.ROOT) == currentEmail)
            }
            ?.jsonObject
            ?.get("role")
            ?.jsonPrimitive
            ?.contentOrNull
            .orEmpty()
    }

    override suspend fun loadTrips(): List<TripCard> {
        val currentUserId = client.auth.currentUserOrNull()?.id?.toString()
        val rows = client.from("trips").select().decodeList<TripRow>()
        return supervisorScope {
            rows.map { row ->
                async {
                    fun text(key: String) = row.payload[key]?.jsonPrimitive?.contentOrNull.orEmpty()
                    val role = currentUserRole(row, currentUserId)
                    TripCard(
                        id = row.id,
                        title = text("title").ifBlank { "Путешествие" },
                        dates = text("dates"),
                        status = text("status").ifBlank { "Черновик" },
                        progress = row.payload["progress"]?.jsonPrimitive?.intOrNull?.coerceIn(0, 100) ?: 0,
                        cities = text("cities"),
                        coverImage = client.resolveTripPhotoReference(text("coverImage")),
                        isOwner = row.ownerId == currentUserId,
                        canEdit = roleCanEdit(role),
                    )
                }
            }.awaitAll()
        }
    }

    override suspend fun loadTripOverview(id: String): TripOverview? {
        val row = client.from("trips").select {
            filter { eq("id", id) }
        }.decodeList<TripRow>().firstOrNull() ?: return null
        val currentUserId = client.auth.currentUserOrNull()?.id?.toString()
        val resolvedUserRole = currentUserRole(row, currentUserId)
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
        val overviewWeatherCities = row.payload["overviewWeatherCities"]?.jsonArray.orEmpty()
            .mapNotNull { it.jsonPrimitive.contentOrNull }
        val overviewBlocks = row.payload["overviewBlocks"]?.jsonArray.orEmpty()
            .mapNotNull { it.jsonPrimitive.contentOrNull }
        val cityCoordinates = row.payload["cityCoordinates"]?.jsonObject.orEmpty().mapNotNull { (city, value) ->
            val coordinates = value.jsonObject
            val latitude = coordinates["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            val longitude = coordinates["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@mapNotNull null
            city to CityLocation(latitude = latitude, longitude = longitude)
        }.toMap()
        val days = row.payload["days"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
        val routeStartDate = tripStartDate(row.payload)
        val orderedRouteIds = routeDayIdsInDateOrder(days, routeStartDate)
        val legs = routeDayObjectsInDateOrder(days, routeStartDate).mapIndexedNotNull { routeIndex, day ->
            val dayData = day
            val roadLeg = dayData["roadLeg"]?.jsonObject ?: return@mapIndexedNotNull null
            val from = roadLeg["from"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
            val to = roadLeg["to"]?.jsonPrimitive?.contentOrNull ?: return@mapIndexedNotNull null
            val dayNumber = routeIndex + 1
            fun roadText(key: String) = roadLeg[key]?.jsonPrimitive?.contentOrNull.orEmpty()
            RouteLeg(
                dayId = dayData["id"]?.jsonPrimitive?.contentOrNull?.takeIf { it.isNotBlank() }
                    ?: orderedRouteIds.getOrNull(routeIndex)
                    ?: "legacy-route-$routeIndex",
                from = from,
                to = to,
                date = dayData["date"]?.jsonPrimitive?.contentOrNull.orEmpty(),
                checkIn = listOf(roadText("checkInFrom"), roadText("checkInTo")).filter(String::isNotBlank).joinToString(" - "),
                checkOut = listOf(roadText("checkOutFrom"), roadText("checkOutTo")).filter(String::isNotBlank).joinToString(" - "),
                notes = roadText("notes"),
                mapsUrl = roadText("mapsUrl"),
                completed = roadLeg["completed"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull },
                dayNumber = dayNumber,
                dateDay = roadText("dateDay").ifBlank { roadText("day") },
                dateMonth = roadText("dateMonth").ifBlank { roadText("month") },
                weekday = roadText("weekday").ifBlank { roadText("weekDay") },
                distance = roadText("distance").ifBlank { roadText("km") },
                travelTime = roadText("travelTime").ifBlank { roadText("hr") },
            )
        }
        val routeDayCount = days.mapIndexed { dayIndex, day ->
            day.jsonObject["dayNumber"]?.jsonPrimitive?.intOrNull?.takeIf { it > 0 } ?: (dayIndex + 1)
        }.maxOrNull() ?: 0
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
                details = accommodationText("details").ifBlank { accommodationText("address") },
                photos = accommodationPhotoReferences(accommodation),
                bookingUrl = accommodationText("bookingUrl").ifBlank { accommodationText("externalUrl") },
                deadline = accommodationText("deadline"),
                rating = accommodation["rating"]?.jsonPrimitive?.doubleOrNull
                    ?: accommodation["hotelRating"]?.jsonPrimitive?.doubleOrNull,
                source = accommodationText("source").ifBlank {
                    if (accommodationText("googlePlaceId").isNotBlank()) "google" else "manual"
                },
                googlePlaceId = accommodationText("googlePlaceId").ifBlank { accommodationText("placeId") },
                bookingPropertyId = accommodationText("bookingPropertyId"),
                externalUrl = accommodationText("externalUrl"),
                address = accommodationText("address").ifBlank { accommodationText("details") },
                latitude = accommodation["latitude"]?.jsonPrimitive?.doubleOrNull,
                longitude = accommodation["longitude"]?.jsonPrimitive?.doubleOrNull,
                reviewCount = accommodation["reviewCount"]?.jsonPrimitive?.intOrNull
                    ?: accommodation["userRatingCount"]?.jsonPrimitive?.intOrNull,
                photoReference = accommodationText("photoReference").ifBlank { accommodationText("googlePhotoName") },
                website = accommodationText("website"),
                phone = accommodationText("phone"),
                type = accommodationText("type").ifBlank { accommodationText("category") },
                tripCityId = accommodationText("tripCityId"),
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
                date = expenseText("date"),
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
                photo = sightPhotoUrl(sight),
                category = sightText("subcategory").ifBlank { sightText("group") },
                done = sight["done"]?.jsonPrimitive?.booleanOrNull ?: false,
                walkDay = sight["walkDay"]?.jsonPrimitive?.intOrNull ?: 0,
                walkOrder = sight["walkOrder"]?.jsonPrimitive?.intOrNull ?: 0,
                description = sightText("description"),
                longitude = lngLat.getOrNull(0)?.jsonPrimitive?.doubleOrNull,
                latitude = lngLat.getOrNull(1)?.jsonPrimitive?.doubleOrNull,
                rating = sight["rating"]?.jsonPrimitive?.doubleOrNull ?: sight["googleRating"]?.jsonPrimitive?.doubleOrNull,
                link = sightText("link").ifBlank { sightText("url") },
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
                status = normalizeRestaurantStatus(restaurantText("status")),
                photos = restaurant["photos"]?.jsonArray.orEmpty().mapNotNull { it.jsonPrimitive.contentOrNull },
                rating = restaurant["googleRating"]?.jsonPrimitive?.doubleOrNull,
                reviews = restaurantText("googleReviews"),
                price = restaurantText("price"),
                note = restaurantText("note"),
                link = restaurantText("link"),
                date = restaurantText("date").ifBlank { restaurantText("dateTime") },
                priority = restaurant["priority"]?.jsonPrimitive?.booleanOrNull ?: false,
            )
        }
        val rawCovers = (covers.ifEmpty {
            text("coverImage").takeIf(String::isNotBlank)?.let { listOf(CoverPhoto("legacy", it, "")) }.orEmpty()
        })
        val resolvedPhotos = supervisorScope {
            val coverValues = async {
                rawCovers.mapNotNull { photo ->
                    client.resolveTripPhotoReference(photo.imageUrl)?.let { url -> photo.copy(imageUrl = url) }
                }
            }
            val accommodationValues = async {
                accommodations.map { accommodation ->
                    async {
                        accommodation.copy(photos = client.resolveTripPhotoReferences(accommodation.photos))
                    }
                }.awaitAll()
            }
            val sightValues = async {
                sights.map { sight ->
                    async {
                        val resolvedPhoto = client.resolveTripPhotoReference(sight.photo)
                        sight.copy(
                            photo = resolvedPhoto.orEmpty(),
                            photoUnavailable = resolvedPhoto == null && sight.photo.isNotBlank() && tripPhotoPath(sight.photo) != null,
                        )
                    }
                }.awaitAll()
            }
            val restaurantValues = async {
                restaurants.map { restaurant ->
                    async {
                        restaurant.copy(photos = client.resolveTripPhotoReferences(restaurant.photos))
                    }
                }.awaitAll()
            }
            ResolvedTripOverviewPhotos(
                covers = coverValues.await(),
                accommodations = accommodationValues.await(),
                sights = sightValues.await(),
                restaurants = restaurantValues.await(),
            )
        }
        val resolvedCovers = resolvedPhotos.covers
        val resolvedAccommodations = resolvedPhotos.accommodations
        val resolvedSights = resolvedPhotos.sights
        val resolvedRestaurants = resolvedPhotos.restaurants
        val budgetManualRates = row.payload["budgetManualRates"]?.jsonObject.orEmpty()
            .mapNotNull { (code, value) ->
                value.jsonPrimitive.doubleOrNull
                    ?.takeIf { it > 0.0 }
                    ?.let { code.uppercase(Locale.ROOT) to it }
            }
            .toMap()
        return TripOverview(
            id = row.id,
            title = text("title").ifBlank { "Путешествие" },
            dates = text("dates"),
            status = text("status"),
            coverPhotos = resolvedCovers,
            overviewMapPoints = mapPoints,
            overviewWeatherCities = overviewWeatherCities,
            overviewBlocks = overviewBlocks,
            routeLegs = legs,
            accommodations = resolvedAccommodations,
            budgetCurrency = text("budgetCurrency").ifBlank { "EUR" },
            budgetManualRates = budgetManualRates,
            budgetExpenses = expenses,
            budgetGroups = groups,
            members = members,
            sights = resolvedSights,
            restaurants = resolvedRestaurants,
            cities = text("cities").split(",").map(String::trim).filter(String::isNotBlank),
            cityCoordinates = cityCoordinates,
            routeDayCount = routeDayCount,
            currentUserRole = resolvedUserRole,
            canEdit = roleCanEdit(resolvedUserRole),
        )
    }

    override suspend fun createTrip(
        title: String,
        startDate: String,
        endDate: String,
        cities: String,
        cityCoordinates: Map<String, CityLocation>,
    ): TripCard {
        val id = UUID.randomUUID().toString()
        val owner = client.auth.currentUserOrNull() ?: throw AuthSessionRequiredException()
        val ownerId = owner.id.toString()
        val ownerEmail = owner.email.orEmpty()
        val ownerName = ownerEmail.substringBefore('@').ifBlank { "Владелец" }
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
            // Keep the initial payload compatible with every trip section.
            put("overviewMapPoints", buildJsonArray {
                cityList.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) }
            })
            put("cityCoordinates", buildJsonObject {
                cityCoordinates.forEach { (city, coordinates) ->
                    put(city, buildJsonObject {
                        put("latitude", coordinates.latitude)
                        put("longitude", coordinates.longitude)
                    })
                }
            })
            put("budgetCurrency", "EUR")
            put("budgetManualRates", buildJsonObject { })
            put("budgetSplit", buildJsonObject {
                put("groups", buildJsonArray { })
            })
            put("coverPhotos", buildJsonArray { })
            put("sights", buildJsonArray { })
            put("restaurants", buildJsonArray { })
            put("accommodations", buildJsonArray { })
            put("budgetExpenses", buildJsonArray { })
            put("members", buildJsonArray {
                add(buildJsonObject {
                    put("id", ownerId)
                    put("name", ownerName)
                    put("email", ownerEmail)
                    put("role", "Владелец")
                    put("initials", ownerName.take(2).uppercase())
                    put("tone", "purple")
                })
            })
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
                            put("checkInFrom", "")
                            put("checkInTo", "")
                            put("checkOutFrom", "")
                            put("checkOutTo", "")
                            put("notes", "")
                            put("mapsUrl", "")
                            put("dateDay", "")
                            put("dateMonth", "")
                            put("weekday", "")
                            put("distance", "")
                            put("travelTime", "")
                            put("completed", buildJsonArray { })
                        })
                        put("dayNumber", index + 1)
                    })
                }
            })
        }
        client.from("trips").insert(listOf(TripInsert(id, ownerId, payload)))
        return TripCard(id, resolvedTitle, dates, "Черновик", 0, cities.trim(), null, isOwner = true)
    }

    override suspend fun deleteTrip(id: String) {
        val currentUserId = client.auth.currentUserOrNull()?.id?.toString()
            ?: throw AuthSessionRequiredException()
        val current = loadTripRow(id)
        check(current.ownerId == currentUserId) {
            "Только владелец путешествия может его удалить"
        }

        collectTripPhotoPaths(current.payload).forEach { path ->
            client.storage.from(TRIP_PHOTO_BUCKET).delete(path)
        }
        client.from("trips").delete {
            filter {
                eq("id", id)
                eq("owner_id", currentUserId)
            }
        }
    }

    override suspend fun leaveTrip(id: String) {
        client.auth.currentUserOrNull()?.id?.toString()
            ?: throw AuthSessionRequiredException()
        client.postgrest.rpc(
            function = "leave_trip",
            parameters = buildJsonObject {
                put("p_trip_id", id)
            },
        )
    }

    private suspend fun loadTripRow(id: String): TripRow =
        client.from("trips").select {
            filter { eq("id", id) }
        }.decodeList<TripRow>().firstOrNull() ?: error("Путешествие не найдено")

    private suspend fun patchTripPayload(id: String, patch: JsonObject, expectedRevision: Long) {
        require(patch.isNotEmpty()) { "Изменения отсутствуют" }
        client.postgrest.rpc(
            function = "patch_trip_payload",
            parameters = buildJsonObject {
                put("p_trip_id", id)
                put("p_patch", patch)
                put("p_expected_revision", expectedRevision)
            },
        )
    }

    private suspend fun patchTripSectionFromPayload(
        id: String,
        key: String,
        payload: JsonObject,
        expectedRevision: Long,
    ) {
        val value = payload[key] ?: error("Раздел путешествия не найден")
        patchTripPayload(id, buildJsonObject { put(key, value) }, expectedRevision)
    }

    private suspend fun updateTripSection(id: String, key: String, value: JsonElement, expectedRevision: Long) {
        patchTripPayload(id, buildJsonObject { put(key, value) }, expectedRevision)
    }

    override suspend fun updateTripSection(id: String, key: String, value: JsonElement) {
        val current = loadTripRow(id)
        updateTripSection(id, key, value, current.revision)
    }

    override suspend fun addRouteLeg(
        id: String,
        from: String,
        to: String,
        checkIn: String,
        checkOut: String,
        notes: String,
        mapsUrl: String,
        dateDay: String,
        dateMonth: String,
        weekday: String,
        distance: String,
        travelTime: String,
        date: String,
    ) {
        require(from.isNotBlank() && to.isNotBlank()) { "Укажите оба города" }
        val current = loadTripRow(id)
        val day = buildJsonObject {
            put("id", UUID.randomUUID().toString())
            put("city", to.trim())
            put("date", date.trim())
            put("places", buildJsonArray { })
            put("roadLeg", buildJsonObject {
                put("from", from.trim())
                put("to", to.trim())
                put("checkInFrom", checkIn.trim())
                put("checkOutFrom", checkOut.trim())
                put("notes", notes.trim())
                put("mapsUrl", mapsUrl.trim())
                put("dateDay", dateDay.trim())
                put("dateMonth", dateMonth.trim())
                put("weekday", weekday.trim())
                put("distance", distance.trim())
                put("travelTime", travelTime.trim())
                put("completed", buildJsonArray { })
            })
        }
        val days = buildJsonArray {
            current.payload["days"]?.jsonArray.orEmpty().forEach { add(it) }
            add(day)
        }
        val startDate = tripStartDate(current.payload)
        val synchronizedDays = synchronizeRouteDayOrder(
            days = days,
            orderedRouteDayIds = routeDayIdsInDateOrder(days, startDate),
            startDate = startDate,
        )
        updateTripSection(id, "days", synchronizedDays, current.revision)
    }

    override suspend fun reorderRouteLegs(id: String, orderedDayIds: List<String>) {
        require(orderedDayIds.isNotEmpty()) { "Маршрут пуст" }
        val current = loadTripRow(id)
        val days = current.payload["days"]?.jsonArray ?: kotlinx.serialization.json.JsonArray(emptyList())
        val startDate = tripStartDate(current.payload)
        val synchronizedDays = synchronizeRouteDayOrder(days, orderedDayIds, startDate)
        if (synchronizedDays != days) {
            updateTripSection(id, "days", synchronizedDays, current.revision)
        }
    }

    override suspend fun addBudgetExpense(id: String, name: String, amount: Double, category: String) {
        require(name.isNotBlank()) { "Укажите название траты" }
        require(amount > 0) { "Укажите сумму больше нуля" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "budgetExpenses", expenses, current.revision)
    }

    override suspend fun updateMemberRole(id: String, memberId: String, role: String) {
        require(role == "Редактор" || role == "Читатель") { "Недопустимая роль" }
        client.postgrest.rpc(
            function = "manage_trip_member",
            parameters = buildJsonObject {
                put("p_trip_id", id)
                put("p_member_id", memberId)
                put("p_role", role)
                put("p_delete", false)
            },
        )
    }

    override suspend fun updateAccommodationStatus(id: String, accommodationId: String, status: String) {
        val current = loadTripRow(id)
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
        updateTripSection(id, "accommodations", accommodations, current.revision)
    }

    override suspend fun updateSightDone(id: String, sightId: String, done: Boolean) {
        val current = loadTripRow(id)
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
        updateTripSection(id, "sights", sights, current.revision)
    }

    override suspend fun addRestaurant(id: String, name: String, city: String, status: String) {
        require(name.isNotBlank()) { "Укажите название ресторана" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "restaurants", restaurants, current.revision)
    }

    override suspend fun addAccommodation(id: String, name: String, city: String, dates: String, price: String, status: String) {
        require(name.isNotBlank()) { "Укажите название жилья" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "accommodations", accommodations, current.revision)
    }

    override suspend fun addSight(id: String, name: String, city: String, category: String) {
        require(name.isNotBlank()) { "Укажите название места" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "sights", sights, current.revision)
    }

    override suspend fun updateRouteChecklist(id: String, dayId: String, itemId: String, completed: Boolean) {
        val current = loadTripRow(id)
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
        updateTripSection(id, "days", days, current.revision)
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
                put("redirectTo", "https://ramingo.online/mobile/invite?tripId=$id")
            },
            headers = Headers.build { append(HttpHeaders.ContentType, "application/json") },
        )
    }

    override suspend fun addCoverPhoto(id: String, bytes: ByteArray, city: String) {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        val current = loadTripRow(id)
        val path = "$ownerId/$id/covers/${UUID.randomUUID()}.jpg"
        client.storage.from(TRIP_PHOTO_BUCKET).upload(path, bytes)
        val imageUrl = storedTripPhotoReference(path)
        val photos = buildJsonArray {
            current.payload["coverPhotos"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("image", imageUrl)
                put("city", city.trim())
            })
        }
        val patch = buildJsonObject {
            put("coverPhotos", photos)
            if (current.payload["coverImage"]?.jsonPrimitive?.contentOrNull.isNullOrBlank()) put("coverImage", kotlinx.serialization.json.JsonPrimitive(imageUrl))
        }
        runCatching { patchTripPayload(id, patch, current.revision) }
            .onFailure {
                runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(path) }
                throw it
            }
    }

    override suspend fun updateTripDetails(id: String, title: String, dates: String, cities: String) {
        require(title.isNotBlank()) { "Укажите название путешествия" }
        val current = loadTripRow(id)
        val cityList = cities.split(",").map(String::trim).filter(String::isNotBlank)
        val oldDays = current.payload["days"]?.jsonArray.orEmpty()
        fun parseTripDate(value: String): LocalDate? {
            val iso = Regex("\\d{4}-\\d{2}-\\d{2}").find(value)?.value
            if (iso != null) return runCatching { LocalDate.parse(iso) }.getOrNull()
            val dotted = Regex("(\\d{1,2})[./](\\d{1,2})[./](\\d{4})").find(value)
            if (dotted != null) {
                return runCatching {
                    LocalDate.of(
                        dotted.groupValues[3].toInt(),
                        dotted.groupValues[2].toInt(),
                        dotted.groupValues[1].toInt(),
                    )
                }.getOrNull()
            }
            return null
        }
        val parsedDates = Regex("(?:\\d{4}-\\d{2}-\\d{2}|\\d{1,2}[./]\\d{1,2}[./]\\d{4})")
            .findAll(dates)
            .mapNotNull { parseTripDate(it.value) }
            .toList()
        val startDate = parsedDates.firstOrNull()
        val endDate = parsedDates.getOrNull(1) ?: startDate
        val oldCoordinates = current.payload["cityCoordinates"]?.jsonObject.orEmpty()
        fun oldCoordinate(city: String): CityLocation? {
            val exact = oldCoordinates[city]?.jsonObject
            val exactLocation = exact?.get("latitude")?.jsonPrimitive?.doubleOrNull?.let { latitude ->
                exact["longitude"]?.jsonPrimitive?.doubleOrNull?.let { longitude -> CityLocation(latitude, longitude) }
            }
            if (exactLocation != null) return exactLocation
            val catalogKey = cityCatalogEntry(city)?.key ?: return null
            return oldCoordinates.entries.firstOrNull { (key, _) -> cityCatalogEntry(key)?.key == catalogKey }
                ?.value
                ?.jsonObject
                ?.let { coordinates ->
                    val latitude = coordinates["latitude"]?.jsonPrimitive?.doubleOrNull ?: return@let null
                    val longitude = coordinates["longitude"]?.jsonPrimitive?.doubleOrNull ?: return@let null
                    CityLocation(latitude, longitude)
                }
        }
        val nextCoordinates = linkedMapOf<String, CityLocation>()
        cityList.forEach { city ->
            val existingLocation = oldCoordinate(city)
            val catalogLocation = cityCatalogEntry(city)?.let { CityLocation(it.latitude, it.longitude) }
            (existingLocation ?: catalogLocation)?.let { nextCoordinates[city] = it }
        }
        val nextDays = buildJsonArray {
            cityList.zipWithNext().forEachIndexed { index, (from, to) ->
                val oldDay = oldDays.getOrNull(index)?.jsonObject
                val oldRoadLeg = oldDay?.get("roadLeg")?.jsonObject
                val dayDate = startDate?.plusDays(index.toLong())?.toString()
                    ?: oldDay?.get("date")?.jsonPrimitive?.contentOrNull.orEmpty()
                val nextRoadLeg = JsonObject((oldRoadLeg?.toMutableMap() ?: mutableMapOf()).apply {
                    put("from", kotlinx.serialization.json.JsonPrimitive(from))
                    put("to", kotlinx.serialization.json.JsonPrimitive(to))
                    if (startDate != null) {
                        // These are derived display fields. Let the Android
                        // reader calculate them from the new ISO date instead
                        // of retaining the previous day's labels.
                        remove("dateDay")
                        remove("dateMonth")
                        remove("weekday")
                    }
                })
                add(JsonObject((oldDay?.toMutableMap() ?: mutableMapOf()).apply {
                    put("id", oldDay?.get("id") ?: kotlinx.serialization.json.JsonPrimitive(UUID.randomUUID().toString()))
                    put("city", kotlinx.serialization.json.JsonPrimitive(to))
                    put("date", kotlinx.serialization.json.JsonPrimitive(dayDate))
                    put("dayNumber", kotlinx.serialization.json.JsonPrimitive(index + 1))
                    put("places", oldDay?.get("places") ?: buildJsonArray { })
                    put("roadLeg", nextRoadLeg)
                }))
            }
        }
        val preservedRouteDays = buildJsonArray {
            current.payload["archivedRouteDays"]?.jsonArray.orEmpty().forEach { add(it) }
            oldDays.drop(nextDays.size).forEach { add(it) }
        }
        val patch = buildJsonObject {
            put("title", kotlinx.serialization.json.JsonPrimitive(title.trim()))
            put("dates", kotlinx.serialization.json.JsonPrimitive(dates.trim()))
            put("cities", kotlinx.serialization.json.JsonPrimitive(cities.trim()))
            startDate?.let { put("startDate", kotlinx.serialization.json.JsonPrimitive(it.toString())) }
            endDate?.let { put("endDate", kotlinx.serialization.json.JsonPrimitive(it.toString())) }
            put("overviewMapPoints", buildJsonArray { cityList.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
            put("cityCoordinates", buildJsonObject {
                nextCoordinates.forEach { (city, coordinates) ->
                    put(city, buildJsonObject {
                        put("latitude", coordinates.latitude)
                        put("longitude", coordinates.longitude)
                    })
                }
            })
            put("days", nextDays)
            if (preservedRouteDays.isNotEmpty()) put("archivedRouteDays", preservedRouteDays)
        }
        patchTripPayload(id, patch, current.revision)
    }

    override suspend fun updateRestaurantStatus(id: String, restaurantId: String, status: String) {
        val current = loadTripRow(id)
        val restaurants = buildJsonArray {
            current.payload["restaurants"]?.jsonArray.orEmpty().forEach { item ->
                val restaurant = item.jsonObject
                if (restaurant["id"]?.jsonPrimitive?.contentOrNull == restaurantId) {
                    add(JsonObject(restaurant.toMutableMap().apply { put("status", kotlinx.serialization.json.JsonPrimitive(normalizeRestaurantStatus(status))) }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "restaurants", restaurants, current.revision)
    }

    override suspend fun updateBudgetExpense(id: String, expenseId: String, name: String, amount: Double, category: String) {
        require(name.isNotBlank()) { "Укажите название траты" }
        require(amount > 0) { "Укажите сумму больше нуля" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "budgetExpenses", expenses, current.revision)
    }

    override suspend fun updateRouteLegCities(id: String, dayId: String, from: String, to: String) {
        require(from.isNotBlank() && to.isNotBlank()) { "Укажите оба города" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "days", days, current.revision)
    }

    override suspend fun updateRouteLegDetails(
        id: String,
        dayId: String,
        from: String,
        to: String,
        checkIn: String,
        checkOut: String,
        notes: String,
        mapsUrl: String,
        dateDay: String,
        dateMonth: String,
        weekday: String,
        distance: String,
        travelTime: String,
        date: String,
    ) {
        require(from.isNotBlank() && to.isNotBlank()) { "Укажите оба города" }
        val current = loadTripRow(id)
        val days = buildJsonArray {
            current.payload["days"]?.jsonArray.orEmpty().forEach { item ->
                val day = item.jsonObject
                if (day["id"]?.jsonPrimitive?.contentOrNull == dayId) {
                    val leg = day["roadLeg"]?.jsonObject ?: run { add(item); return@forEach }
                    val nextLeg = JsonObject(leg.toMutableMap().apply {
                        put("from", kotlinx.serialization.json.JsonPrimitive(from.trim())); put("to", kotlinx.serialization.json.JsonPrimitive(to.trim()))
                        put("checkInFrom", kotlinx.serialization.json.JsonPrimitive(checkIn.trim())); put("checkOutFrom", kotlinx.serialization.json.JsonPrimitive(checkOut.trim()))
                        put("notes", kotlinx.serialization.json.JsonPrimitive(notes.trim())); put("mapsUrl", kotlinx.serialization.json.JsonPrimitive(mapsUrl.trim()))
                        put("dateDay", kotlinx.serialization.json.JsonPrimitive(dateDay.trim())); put("dateMonth", kotlinx.serialization.json.JsonPrimitive(dateMonth.trim()))
                        put("weekday", kotlinx.serialization.json.JsonPrimitive(weekday.trim())); put("distance", kotlinx.serialization.json.JsonPrimitive(distance.trim()))
                        put("travelTime", kotlinx.serialization.json.JsonPrimitive(travelTime.trim()))
                    })
                    add(JsonObject(day.toMutableMap().apply {
                        put("city", kotlinx.serialization.json.JsonPrimitive(to.trim()))
                        put("date", kotlinx.serialization.json.JsonPrimitive(date.trim()))
                        put("roadLeg", nextLeg)
                    }))
                } else add(item)
            }
        }
        val startDate = tripStartDate(current.payload)
        val synchronizedDays = synchronizeRouteDayOrder(
            days = days,
            orderedRouteDayIds = routeDayIdsInDateOrder(days, startDate),
            startDate = startDate,
        )
        updateTripSection(id, "days", synchronizedDays, current.revision)
    }

    override suspend fun updateRestaurantDetails(id: String, restaurantId: String, name: String, city: String, note: String) {
        require(name.isNotBlank()) { "Укажите название ресторана" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "restaurants", restaurants, current.revision)
    }

    override suspend fun updateAccommodationDetails(id: String, accommodationId: String, name: String, city: String, dates: String, price: String) {
        require(name.isNotBlank()) { "Укажите название жилья" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "accommodations", accommodations, current.revision)
    }

    override suspend fun updateSightDetails(id: String, sightId: String, name: String, city: String, category: String) {
        require(name.isNotBlank()) { "Укажите название места" }
        val current = loadTripRow(id)
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
        updateTripSection(id, "sights", sights, current.revision)
    }

    override suspend fun addBudgetGroup(id: String, name: String, people: Int) {
        require(name.isNotBlank()) { "Укажите название группы" }
        require(people > 0) { "Укажите количество участников" }
        val current = loadTripRow(id)
        val split = current.payload["budgetSplit"]?.jsonObject.orEmpty()
        val groups = buildJsonArray {
            split["groups"]?.jsonArray.orEmpty().forEach { add(it) }
            add(buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("name", name.trim())
                put("people", people)
            })
        }
        updateTripSection(id, "budgetSplit", JsonObject(split.toMutableMap().apply { put("groups", groups) }), current.revision)
    }

    override suspend fun addAccommodationPhoto(id: String, accommodationId: String, bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        val current = loadTripRow(id)
        require(current.payload["accommodations"]?.jsonArray.orEmpty().any {
            it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == accommodationId
        }) { "Жильё не найдено" }
        val path = "$ownerId/$id/accommodations/$accommodationId/${UUID.randomUUID()}.jpg"
        client.storage.from(TRIP_PHOTO_BUCKET).upload(path, bytes)
        val imageUrl = storedTripPhotoReference(path)
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
        runCatching { updateTripSection(id, "accommodations", accommodations, current.revision) }
            .onFailure {
                runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(path) }
                throw it
            }
        return imageUrl
    }

    override suspend fun replaceAccommodationCoverPhoto(id: String, accommodationId: String, bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val current = loadTripRow(id)
        val accommodation = current.payload["accommodations"]?.jsonArray.orEmpty()
            .map { it.jsonObject }
            .firstOrNull { it["id"]?.jsonPrimitive?.contentOrNull == accommodationId }
            ?: error("Жильё не найдено")
        val oldPhoto = accommodation["photos"]?.jsonArray?.firstOrNull()?.let(::jsonText)
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        val path = "$ownerId/$id/accommodations/$accommodationId/${UUID.randomUUID()}.jpg"
        client.storage.from(TRIP_PHOTO_BUCKET).upload(path, bytes)
        val imageReference = storedTripPhotoReference(path)
        val accommodations = buildJsonArray {
            current.payload["accommodations"]?.jsonArray.orEmpty().forEach { item ->
                val itemObject = item.jsonObject
                if (itemObject["id"]?.jsonPrimitive?.contentOrNull == accommodationId) {
                    val photos = buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive(imageReference))
                        itemObject["photos"]?.jsonArray.orEmpty().drop(1).forEach { add(it) }
                    }
                    add(JsonObject(itemObject.toMutableMap().apply { put("photos", photos) }))
                } else {
                    add(item)
                }
            }
        }
        runCatching {
            updateTripSection(id, "accommodations", accommodations, current.revision)
        }.onFailure {
            runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(path) }
            throw it
        }
        tripPhotoPath(oldPhoto)?.let { oldPath ->
            runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(oldPath) }
        }
        return imageReference
    }

    override suspend fun addSightPhoto(id: String, sightId: String, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        val current = loadTripRow(id)
        require(current.payload["sights"]?.jsonArray.orEmpty().any {
            it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == sightId
        }) { "Место не найдено" }
        val path = "$ownerId/$id/sights/$sightId/${UUID.randomUUID()}.jpg"
        client.storage.from(TRIP_PHOTO_BUCKET).upload(path, bytes)
        val imageUrl = storedTripPhotoReference(path)
        val sights = buildJsonArray {
            current.payload["sights"]?.jsonArray.orEmpty().forEach { item ->
                val sight = item.jsonObject
                if (sight["id"]?.jsonPrimitive?.contentOrNull == sightId) {
                    add(JsonObject(sight.toMutableMap().apply { put("photo", kotlinx.serialization.json.JsonPrimitive(imageUrl)) }))
                } else add(item)
            }
        }
        runCatching { updateTripSection(id, "sights", sights, current.revision) }
            .onFailure {
                runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(path) }
                throw it
            }
    }

    override suspend fun moveAccommodationPhoto(id: String, accommodationId: String, photoIndex: Int, direction: Int) {
        val current = loadTripRow(id)
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
        updateTripSection(id, "accommodations", accommodations, current.revision)
    }

    override suspend fun deleteAccommodationPhoto(id: String, accommodationId: String, photoIndex: Int) {
        val current = loadTripRow(id)
        val accommodation = current.payload["accommodations"]?.jsonArray.orEmpty()
            .map { it.jsonObject }
            .firstOrNull { it["id"]?.jsonPrimitive?.contentOrNull == accommodationId }
            ?: error("Жильё не найдено")
        val photos = accommodation["photos"]?.jsonArray.orEmpty().toMutableList()
        require(photoIndex in photos.indices) { "Фото не найдено" }

        val removedPhoto = photos.removeAt(photoIndex)

        val accommodations = buildJsonArray {
            current.payload["accommodations"]?.jsonArray.orEmpty().forEach { item ->
                val itemObject = item.jsonObject
                if (itemObject["id"]?.jsonPrimitive?.contentOrNull == accommodationId) {
                    add(JsonObject(itemObject.toMutableMap().apply {
                        put("photos", buildJsonArray { photos.forEach { add(it) } })
                    }))
                } else {
                    add(item)
                }
            }
        }
        updateTripSection(id, "accommodations", accommodations, current.revision)
        // Remove the object only after the database no longer references it.
        // Storage and Postgres cannot share a transaction, but this ordering
        // prevents a failed database write from leaving a broken photo link.
        tripPhotoPath(jsonText(removedPhoto))?.let { path ->
            runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(path) }
        }
    }

    override suspend fun deleteAccommodation(id: String, accommodationId: String) {
        val current = loadTripRow(id)
        val accommodation = current.payload["accommodations"]?.jsonArray.orEmpty()
            .map { it.jsonObject }
            .firstOrNull { it["id"]?.jsonPrimitive?.contentOrNull == accommodationId }
            ?: error("Жильё не найдено")

        val payload = TripPayloadCodec.removeArrayItem(current.payload, "accommodations", accommodationId)
        patchTripSectionFromPayload(id, "accommodations", payload, current.revision)
        accommodation["photos"]?.jsonArray.orEmpty().forEach { photo ->
            tripPhotoPath(jsonText(photo))?.let { path ->
                runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(path) }
            }
        }
    }

    override suspend fun moveRestaurantPhoto(id: String, restaurantId: String, photoIndex: Int, direction: Int) {
        val current = loadTripRow(id)
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
        updateTripSection(id, "restaurants", restaurants, current.revision)
    }

    override suspend fun addRestaurantPhoto(id: String, restaurantId: String, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        val current = loadTripRow(id)
        require(current.payload["restaurants"]?.jsonArray.orEmpty().any {
            it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == restaurantId
        }) { "Ресторан не найден" }
        val path = "$ownerId/$id/restaurants/$restaurantId/${UUID.randomUUID()}.jpg"
        client.storage.from(TRIP_PHOTO_BUCKET).upload(path, bytes)
        val imageUrl = storedTripPhotoReference(path)
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
        runCatching { updateTripSection(id, "restaurants", restaurants, current.revision) }
            .onFailure {
                runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(path) }
                throw it
            }
    }

    override suspend fun replaceRestaurantCoverPhoto(id: String, restaurantId: String, bytes: ByteArray) {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val current = loadTripRow(id)
        val restaurant = current.payload["restaurants"]?.jsonArray.orEmpty()
            .map { it.jsonObject }
            .firstOrNull { it["id"]?.jsonPrimitive?.contentOrNull == restaurantId }
            ?: error("Ресторан не найден")
        val oldPhoto = restaurant["photos"]?.jsonArray?.firstOrNull()?.let(::jsonText)
        val ownerId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        val path = "$ownerId/$id/restaurants/$restaurantId/${UUID.randomUUID()}.jpg"
        client.storage.from(TRIP_PHOTO_BUCKET).upload(path, bytes)
        val imageReference = storedTripPhotoReference(path)
        val restaurants = buildJsonArray {
            current.payload["restaurants"]?.jsonArray.orEmpty().forEach { item ->
                val itemObject = item.jsonObject
                if (itemObject["id"]?.jsonPrimitive?.contentOrNull == restaurantId) {
                    val photos = buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive(imageReference))
                        itemObject["photos"]?.jsonArray.orEmpty().drop(1).forEach { add(it) }
                    }
                    add(JsonObject(itemObject.toMutableMap().apply { put("photos", photos) }))
                } else {
                    add(item)
                }
            }
        }
        runCatching {
            updateTripSection(id, "restaurants", restaurants, current.revision)
        }.onFailure {
            runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(path) }
            throw it
        }
        tripPhotoPath(oldPhoto)?.let { oldPath ->
            runCatching { client.storage.from(TRIP_PHOTO_BUCKET).delete(oldPath) }
        }
    }

    override suspend fun deleteTripItem(id: String, section: String, itemId: String) {
        require(section in setOf("days", "sights", "restaurants", "accommodations", "budgetExpenses", "members", "coverPhotos")) {
            "Недопустимый раздел"
        }
        if (section == "members") {
            client.postgrest.rpc(
                function = "manage_trip_member",
                parameters = buildJsonObject {
                    put("p_trip_id", id)
                    put("p_member_id", itemId)
                    put("p_delete", true)
                },
            )
            return
        }
        val current = loadTripRow(id)
        val payload = TripPayloadCodec.removeArrayItem(current.payload, section, itemId)
        patchTripSectionFromPayload(id, section, payload, current.revision)
    }

    override suspend fun deleteSightDay(id: String, walkDay: Int) {
        val normalizedWalkDay = walkDay.coerceAtLeast(1)
        val current = loadTripRow(id)
        val nextPayload = TripPayloadCodec.removeArrayItems(current.payload, "sights") { sight ->
            (sight["walkDay"]?.jsonPrimitive?.intOrNull ?: 0).coerceAtLeast(1) == normalizedWalkDay
        }
        if (nextPayload["sights"] == current.payload["sights"]) return
        patchTripSectionFromPayload(id, "sights", nextPayload, current.revision)
    }

    override suspend fun addSightDetails(
        id: String,
        name: String,
        city: String,
        category: String,
        description: String,
        walkDay: Int,
        longitude: Double?,
        latitude: Double?,
        link: String,
    ): String {
        require(name.isNotBlank()) { "Укажите название места" }
        val current = loadTripRow(id)
        val sightId = UUID.randomUUID().toString()
        val normalizedWalkDay = walkDay.coerceAtLeast(1)
        val nextWalkOrder = current.payload["sights"]?.jsonArray.orEmpty()
            .mapNotNull { sight ->
                val sightObject = sight.jsonObject
                val sightDay = sightObject["walkDay"]?.jsonPrimitive?.intOrNull ?: 0
                if (sightDay.coerceAtLeast(1) == normalizedWalkDay) sightObject["walkOrder"]?.jsonPrimitive?.intOrNull else null
            }
            .maxOrNull()
            ?.plus(1)
            ?: 0
        val item = buildJsonObject {
            put("id", sightId)
            put("name", name.trim())
            put("city", city.trim())
            put("subcategory", category.trim())
            put("description", description.trim())
            put("walkDay", normalizedWalkDay)
            put("walkOrder", nextWalkOrder)
            put("done", false)
            if (link.isNotBlank()) put("link", link.trim())
            if (longitude != null && latitude != null) {
                put("lnglat", buildJsonArray {
                    add(kotlinx.serialization.json.JsonPrimitive(longitude))
                    add(kotlinx.serialization.json.JsonPrimitive(latitude))
                })
            }
        }
        patchTripSectionFromPayload(id, "sights", TripPayloadCodec.append(current.payload, "sights", item), current.revision)
        return sightId
    }

    override suspend fun addCatalogSights(
        id: String,
        city: String,
        language: String,
        walkDay: Int,
        entries: List<SightCatalogEntry>,
    ) {
        require(entries.isNotEmpty()) { "Выберите хотя бы одну достопримечательность" }
        val current = loadTripRow(id)
        val existing = current.payload["sights"]?.jsonArray.orEmpty()
        val nextItems = existing.toMutableList()
        val normalizedCity = normalizeCatalogText(catalogCityName(city))
        val selectedCatalogIds = existing.mapNotNull { item ->
            item.jsonObject["catalogId"]?.jsonPrimitive?.contentOrNull
        }.toMutableSet()
        val normalizedExistingNames = existing.mapNotNull { item ->
            val sight = item.jsonObject
            val itemCity = normalizeCatalogText(catalogCityName(sight["city"]?.jsonPrimitive?.contentOrNull.orEmpty()))
            val itemName = normalizeCatalogText(sight["name"]?.jsonPrimitive?.contentOrNull.orEmpty())
            if (itemCity.isBlank() || itemName.isBlank()) null else "$itemCity|$itemName"
        }.toMutableSet()
        var nextWalkOrder = existing.mapNotNull { item ->
            val sight = item.jsonObject
            val sightDay = sight["walkDay"]?.jsonPrimitive?.intOrNull ?: 0
            if (sightDay.coerceAtLeast(1) == walkDay.coerceAtLeast(1)) sight["walkOrder"]?.jsonPrimitive?.intOrNull else null
        }.maxOrNull()?.plus(1) ?: 0
        val normalizedWalkDay = walkDay.coerceAtLeast(1)

        entries.forEach { entry ->
            val name = entry.name(language).trim()
            if (name.isBlank()) return@forEach
            val entryNames = entry.allNames().map(::normalizeCatalogText).filter(String::isNotBlank).toSet()
            val duplicateKey = "$normalizedCity|${normalizeCatalogText(name)}"
            val alreadyAdded = entry.id in selectedCatalogIds || duplicateKey in normalizedExistingNames || existing.any { item ->
                val sight = item.jsonObject
                val itemCity = normalizeCatalogText(catalogCityName(sight["city"]?.jsonPrimitive?.contentOrNull.orEmpty()))
                val itemName = normalizeCatalogText(sight["name"]?.jsonPrimitive?.contentOrNull.orEmpty())
                itemCity == normalizedCity && itemName in entryNames
            }
            if (alreadyAdded) return@forEach

            val mapUrl = entry.mapUrl.trim().ifBlank {
                if (entry.latitude != null && entry.longitude != null) {
                    "https://www.google.com/maps/search/?api=1&query=${entry.latitude},${entry.longitude}"
                } else {
                    ""
                }
            }
            val item = buildJsonObject {
                put("id", UUID.randomUUID().toString())
                put("catalogId", entry.id)
                put("name", name)
                put("city", city.trim())
                put("subcategory", entry.category.trim())
                put("description", entry.description(language).trim())
                put("walkDay", normalizedWalkDay)
                put("walkOrder", nextWalkOrder)
                put("done", false)
                if (!entry.photoUrl.isNullOrBlank()) put("photo", entry.photoUrl.trim())
                if (entry.rating != null) put("rating", entry.rating)
                if (mapUrl.isNotBlank()) put("link", mapUrl)
                if (entry.longitude != null && entry.latitude != null) {
                    put("lnglat", buildJsonArray {
                        add(kotlinx.serialization.json.JsonPrimitive(entry.longitude))
                        add(kotlinx.serialization.json.JsonPrimitive(entry.latitude))
                    })
                }
            }
            nextItems += item
            selectedCatalogIds += entry.id
            normalizedExistingNames += duplicateKey
            nextWalkOrder += 1
        }

        require(nextItems.size > existing.size) { "Выбранные достопримечательности уже добавлены" }
        patchTripSectionFromPayload(id, "sights", TripPayloadCodec.withSection(current.payload, "sights", kotlinx.serialization.json.JsonArray(nextItems)), current.revision)
    }

    override suspend fun updateSightDetailsRich(
        id: String,
        sightId: String,
        name: String,
        city: String,
        category: String,
        description: String,
        walkDay: Int,
        longitude: Double?,
        latitude: Double?,
        locationChanged: Boolean,
        link: String,
    ) {
        require(name.isNotBlank()) { "Укажите название места" }
        val current = loadTripRow(id)
        val payload = TripPayloadCodec.updateArrayItem(current.payload, "sights", sightId) { sight ->
            JsonObject(sight.toMutableMap().apply {
                put("name", kotlinx.serialization.json.JsonPrimitive(name.trim()))
                put("city", kotlinx.serialization.json.JsonPrimitive(city.trim()))
                put("subcategory", kotlinx.serialization.json.JsonPrimitive(category.trim()))
                put("description", kotlinx.serialization.json.JsonPrimitive(description.trim()))
                put("walkDay", kotlinx.serialization.json.JsonPrimitive(walkDay.coerceAtLeast(0)))
                if (link.isBlank()) {
                    remove("link")
                } else {
                    put("link", kotlinx.serialization.json.JsonPrimitive(link.trim()))
                }
                if (locationChanged) {
                    if (longitude != null && latitude != null) {
                        put("lnglat", buildJsonArray {
                            add(kotlinx.serialization.json.JsonPrimitive(longitude))
                            add(kotlinx.serialization.json.JsonPrimitive(latitude))
                        })
                    } else {
                        remove("lnglat")
                    }
                }
            })
        }
        patchTripSectionFromPayload(id, "sights", payload, current.revision)
    }

    override suspend fun reorderSights(id: String, orderedSightIds: List<String>) {
        if (orderedSightIds.isEmpty()) return
        val current = loadTripRow(id)
        val sights = current.payload["sights"]?.jsonArray.orEmpty()
        val requestedIds = orderedSightIds.distinct()
        val requestedSightObjects = requestedIds.mapNotNull { requestedId ->
            sights.firstOrNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == requestedId }
        }
        if (requestedSightObjects.isEmpty()) return
        val targetDay = (requestedSightObjects.first().jsonObject["walkDay"]?.jsonPrimitive?.intOrNull ?: 0).coerceAtLeast(1)
        val targetDayObjects = sights.filter { item ->
            val sight = item.jsonObject
            val sightDay = (sight["walkDay"]?.jsonPrimitive?.intOrNull ?: 0).coerceAtLeast(1)
            sightDay == targetDay
        }
        val requestedTargetIds = requestedIds.filter { requestedId ->
            targetDayObjects.any { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull == requestedId }
        }
        val remainingTargetIds = targetDayObjects.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
            .filterNot { it in requestedTargetIds }
        val orderById = (requestedTargetIds + remainingTargetIds)
            .withIndex()
            .associate { (index, sightId) -> sightId to index }
        val reorderedSights = buildJsonArray {
            sights.forEach { item ->
                val sight = item.jsonObject
                val sightId = sight["id"]?.jsonPrimitive?.contentOrNull
                val order = sightId?.let(orderById::get)
                if (order == null) {
                    add(item)
                } else {
                    add(JsonObject(sight.toMutableMap().apply {
                        put("walkOrder", kotlinx.serialization.json.JsonPrimitive(order))
                    }))
                }
            }
        }
        updateTripSection(id, "sights", reorderedSights, current.revision)
    }

    override suspend fun addRestaurantDetails(input: RestaurantInput, tripId: String): String {
        require(input.name.isNotBlank()) { "Укажите название ресторана" }
        val current = loadTripRow(tripId)
        val restaurantId = UUID.randomUUID().toString()
        val item = buildJsonObject {
            put("id", restaurantId)
            put("name", input.name.trim())
            put("city", input.city.trim())
            put("status", normalizeRestaurantStatus(input.status))
            put("note", input.note.trim())
            put("price", input.price.trim())
            put("link", input.link.trim())
            put("date", input.date.trim())
            put("priority", input.priority)
            put("photos", buildJsonArray { })
        }
        patchTripSectionFromPayload(tripId, "restaurants", TripPayloadCodec.append(current.payload, "restaurants", item), current.revision)
        return restaurantId
    }

    override suspend fun updateRestaurantDetailsRich(tripId: String, restaurantId: String, input: RestaurantInput) {
        require(input.name.isNotBlank()) { "Укажите название ресторана" }
        val current = loadTripRow(tripId)
        val payload = TripPayloadCodec.updateArrayItem(current.payload, "restaurants", restaurantId) { restaurant ->
            JsonObject(restaurant.toMutableMap().apply {
                put("name", kotlinx.serialization.json.JsonPrimitive(input.name.trim()))
                put("city", kotlinx.serialization.json.JsonPrimitive(input.city.trim()))
                put("status", kotlinx.serialization.json.JsonPrimitive(normalizeRestaurantStatus(input.status)))
                put("note", kotlinx.serialization.json.JsonPrimitive(input.note.trim()))
                put("price", kotlinx.serialization.json.JsonPrimitive(input.price.trim()))
                put("link", kotlinx.serialization.json.JsonPrimitive(input.link.trim()))
                put("date", kotlinx.serialization.json.JsonPrimitive(input.date.trim()))
                put("priority", kotlinx.serialization.json.JsonPrimitive(input.priority))
            })
        }
        patchTripSectionFromPayload(tripId, "restaurants", payload, current.revision)
    }

    override suspend fun addAccommodationDetails(input: AccommodationInput, tripId: String): String {
        require(input.name.isNotBlank()) { "Укажите название жилья" }
        val current = loadTripRow(tripId)
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
            put("deadline", input.deadline.trim())
            put("photos", buildJsonArray { })
            put("source", input.source.trim().ifBlank { "manual" })
            if (input.googlePlaceId.isNotBlank()) put("googlePlaceId", input.googlePlaceId.trim())
            if (input.bookingPropertyId.isNotBlank()) put("bookingPropertyId", input.bookingPropertyId.trim())
            if (input.externalUrl.isNotBlank()) put("externalUrl", input.externalUrl.trim())
            if (input.address.isNotBlank()) put("address", input.address.trim())
            if (input.latitude != null) put("latitude", input.latitude)
            if (input.longitude != null) put("longitude", input.longitude)
            if (input.rating != null) put("rating", input.rating)
            if (input.reviewCount != null) put("reviewCount", input.reviewCount)
            if (input.photoReference.isNotBlank()) put("photoReference", input.photoReference.trim())
            if (input.website.isNotBlank()) put("website", input.website.trim())
            if (input.phone.isNotBlank()) put("phone", input.phone.trim())
            if (input.type.isNotBlank()) put("type", input.type.trim())
            if (input.tripCityId.isNotBlank()) put("tripCityId", input.tripCityId.trim())
        }
        patchTripSectionFromPayload(tripId, "accommodations", TripPayloadCodec.append(current.payload, "accommodations", item), current.revision)
        return accommodationId
    }

    override suspend fun updateAccommodationDetailsRich(tripId: String, accommodationId: String, input: AccommodationInput) {
        require(input.name.isNotBlank()) { "Укажите название жилья" }
        val current = loadTripRow(tripId)
        val payload = TripPayloadCodec.updateArrayItem(current.payload, "accommodations", accommodationId) { accommodation ->
            JsonObject(accommodation.toMutableMap().apply {
                put("name", kotlinx.serialization.json.JsonPrimitive(input.name.trim()))
                put("city", kotlinx.serialization.json.JsonPrimitive(input.city.trim()))
                put("dates", kotlinx.serialization.json.JsonPrimitive(input.dates.trim()))
                put("price", kotlinx.serialization.json.JsonPrimitive(input.price.trim()))
                put("status", kotlinx.serialization.json.JsonPrimitive(input.status))
                put("details", kotlinx.serialization.json.JsonPrimitive(input.details.trim()))
                put("bookingUrl", kotlinx.serialization.json.JsonPrimitive(input.bookingUrl.trim()))
                put("deadline", kotlinx.serialization.json.JsonPrimitive(input.deadline.trim()))
                put("externalUrl", kotlinx.serialization.json.JsonPrimitive(input.externalUrl.trim()))
                put("address", kotlinx.serialization.json.JsonPrimitive(input.address.trim()))
            })
        }
        patchTripSectionFromPayload(tripId, "accommodations", payload, current.revision)
    }

    override suspend fun addBudgetExpenseDetails(tripId: String, input: ExpenseInput) {
        require(input.name.isNotBlank()) { "Укажите название траты" }
        require(input.amount > 0) { "Укажите сумму больше нуля" }
        val current = loadTripRow(tripId)
        val item = buildJsonObject {
            put("id", UUID.randomUUID().toString())
            put("name", input.name.trim())
            put("amount", input.amount)
            put("category", input.category)
            put("scope", input.scope)
            put("paidBy", input.paidBy)
            put("date", input.date.trim())
        }
        patchTripSectionFromPayload(tripId, "budgetExpenses", TripPayloadCodec.append(current.payload, "budgetExpenses", item), current.revision)
    }

    override suspend fun updateBudgetExpenseDetails(tripId: String, expenseId: String, input: ExpenseInput) {
        require(input.name.isNotBlank()) { "Укажите название траты" }
        require(input.amount > 0) { "Укажите сумму больше нуля" }
        val current = loadTripRow(tripId)
        val payload = TripPayloadCodec.updateArrayItem(current.payload, "budgetExpenses", expenseId) { expense ->
            JsonObject(expense.toMutableMap().apply {
                put("name", kotlinx.serialization.json.JsonPrimitive(input.name.trim()))
                put("amount", kotlinx.serialization.json.JsonPrimitive(input.amount))
                put("category", kotlinx.serialization.json.JsonPrimitive(input.category))
                put("scope", kotlinx.serialization.json.JsonPrimitive(input.scope))
                put("paidBy", kotlinx.serialization.json.JsonPrimitive(input.paidBy))
                put("date", kotlinx.serialization.json.JsonPrimitive(input.date.trim()))
            })
        }
        patchTripSectionFromPayload(tripId, "budgetExpenses", payload, current.revision)
    }
}
