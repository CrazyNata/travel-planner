package com.odyssey.travelplanner.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.functions.functions
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.storage.storage
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import java.util.UUID

@Serializable
private data class UserDataRow(
    @SerialName("user_id") val userId: String,
    val key: String,
    val value: JsonObject,
)

data class AccountProfile(
    val avatarUrl: String?,
    val notificationsEnabled: Boolean,
    val language: String = "RU",
    val themePreference: ThemePreference = ThemePreference.SYSTEM,
    val tripRemindersEnabled: Boolean = true,
    val cancellationRemindersEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val onboardingCompleted: Boolean = false,
    val createTripHintSeen: Boolean = false,
    val addPlaceHintSeen: Boolean = false,
    /** True when the account_profile row already existed before this read. */
    val hasStoredProfile: Boolean = false,
) {
    val darkTheme: Boolean
        get() = themePreference == ThemePreference.DARK
}

class AccountRepository(private val client: SupabaseClient) {
    suspend fun loadProfile(): AccountProfile {
        val userId = client.auth.currentUserOrNull()?.id?.toString() ?: return AccountProfile(null, false)
        val row = client.from("user_data").select {
            filter { eq("user_id", userId); eq("key", "account_profile") }
        }.decodeList<UserDataRow>().firstOrNull()
        val storedTheme = row?.value?.get("theme")?.jsonPrimitive?.contentOrNull
        val themePreference = when {
            storedTheme != null -> ThemePreference.fromStorage(storedTheme)
            row?.value?.get("dark_theme")?.jsonPrimitive?.content == "true" -> ThemePreference.DARK
            row != null -> ThemePreference.LIGHT
            else -> ThemePreference.SYSTEM
        }
        return AccountProfile(
            avatarUrl = client.resolveTripPhotoReference(row?.value?.get("avatar_url")?.jsonPrimitive?.contentOrNull),
            notificationsEnabled = row?.value?.get("notifications_enabled")?.jsonPrimitive?.content == "true",
            language = row?.value?.get("language")?.jsonPrimitive?.contentOrNull ?: "RU",
            themePreference = themePreference,
            tripRemindersEnabled = row?.value?.get("trip_reminders_enabled")?.jsonPrimitive?.content?.let { it == "true" } ?: true,
            cancellationRemindersEnabled = row?.value?.get("cancellation_reminders_enabled")?.jsonPrimitive?.content?.let { it == "true" } ?: true,
            reminderHour = row?.value?.get("reminder_hour")?.jsonPrimitive?.content?.toIntOrNull()?.coerceIn(0, 23) ?: 9,
            onboardingCompleted = row?.value?.get("onboarding_completed")?.jsonPrimitive?.content == "true",
            createTripHintSeen = row?.value?.get("create_trip_hint_seen")?.jsonPrimitive?.content == "true",
            addPlaceHintSeen = row?.value?.get("add_place_hint_seen")?.jsonPrimitive?.content == "true",
            hasStoredProfile = row != null,
        )
    }

    suspend fun updateProfile(
        avatarUrl: String?,
        notificationsEnabled: Boolean,
        language: String? = null,
        darkTheme: Boolean? = null,
        tripRemindersEnabled: Boolean? = null,
        cancellationRemindersEnabled: Boolean? = null,
        reminderHour: Int? = null,
        themePreference: ThemePreference? = null,
        onboardingCompleted: Boolean? = null,
        createTripHintSeen: Boolean? = null,
        addPlaceHintSeen: Boolean? = null,
    ) {
        val userId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        val existing = client.from("user_data").select {
            filter { eq("user_id", userId); eq("key", "account_profile") }
        }.decodeList<UserDataRow>().firstOrNull()?.value
        val value = existing?.toMutableMap() ?: mutableMapOf()
        canonicalTripPhotoReference(avatarUrl)?.let { value["avatar_url"] = JsonPrimitive(it) }
        value["notifications_enabled"] = JsonPrimitive(notificationsEnabled)
        language?.let { value["language"] = JsonPrimitive(it) }
        themePreference?.let {
            value["theme"] = JsonPrimitive(it.storageValue)
            value["dark_theme"] = JsonPrimitive(it == ThemePreference.DARK)
        } ?: darkTheme?.let {
            value["dark_theme"] = JsonPrimitive(it)
            if (value["theme"] == null) {
                value["theme"] = JsonPrimitive(if (it) ThemePreference.DARK.storageValue else ThemePreference.LIGHT.storageValue)
            }
        }
        tripRemindersEnabled?.let { value["trip_reminders_enabled"] = JsonPrimitive(it) }
        cancellationRemindersEnabled?.let { value["cancellation_reminders_enabled"] = JsonPrimitive(it) }
        reminderHour?.coerceIn(0, 23)?.let { value["reminder_hour"] = JsonPrimitive(it) }
        onboardingCompleted?.let { value["onboarding_completed"] = JsonPrimitive(it) }
        createTripHintSeen?.let { value["create_trip_hint_seen"] = JsonPrimitive(it) }
        addPlaceHintSeen?.let { value["add_place_hint_seen"] = JsonPrimitive(it) }
        client.from("user_data").upsert(
            UserDataRow(
                userId = userId,
                key = "account_profile",
                value = JsonObject(value),
            ),
        ) { onConflict = "user_id,key" }
    }

    suspend fun updateAppearance(language: String, themePreference: ThemePreference) {
        val profile = loadProfile()
        updateProfile(
            profile.avatarUrl,
            profile.notificationsEnabled,
            language = language,
            themePreference = themePreference,
        )
    }

    suspend fun updateAppearance(language: String, darkTheme: Boolean) {
        updateAppearance(
            language,
            if (darkTheme) ThemePreference.DARK else ThemePreference.LIGHT,
        )
    }

    suspend fun updateOnboardingState(
        onboardingCompleted: Boolean? = null,
        createTripHintSeen: Boolean? = null,
        addPlaceHintSeen: Boolean? = null,
    ) {
        val profile = loadProfile()
        updateProfile(
            avatarUrl = profile.avatarUrl,
            notificationsEnabled = profile.notificationsEnabled,
            language = profile.language,
            themePreference = profile.themePreference,
            tripRemindersEnabled = profile.tripRemindersEnabled,
            cancellationRemindersEnabled = profile.cancellationRemindersEnabled,
            reminderHour = profile.reminderHour,
            onboardingCompleted = onboardingCompleted,
            createTripHintSeen = createTripHintSeen,
            addPlaceHintSeen = addPlaceHintSeen,
        )
    }

    /**
     * The web app stores its first-run tutorial state in this generic user_data
     * record. Reading the same record keeps the Android and web onboarding in
     * sync without introducing another table or column.
     */
    suspend fun loadWebOnboardingCompleted(): Boolean? {
        val userId = client.auth.currentUserOrNull()?.id?.toString() ?: return null
        val row = client.from("user_data").select {
            filter { eq("user_id", userId); eq("key", "web-onboarding") }
        }.decodeList<UserDataRow>().firstOrNull() ?: return null
        return when (row.value["completed"]?.jsonPrimitive?.contentOrNull?.lowercase()) {
            "true" -> true
            "false" -> false
            else -> null
        }
    }

    suspend fun updateWebOnboardingState(completed: Boolean) {
        val userId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        client.from("user_data").upsert(
            UserDataRow(
                userId = userId,
                key = "web-onboarding",
                value = buildJsonObject {
                    put("completed", completed)
                    put("completedAt", java.time.Instant.now().toString())
                },
            ),
        ) { onConflict = "user_id,key" }
    }

    suspend fun uploadProfilePhoto(bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val userId = client.auth.currentUserOrNull()?.id?.toString() ?: throw AuthSessionRequiredException()
        val path = "$userId/profile/${UUID.randomUUID()}.jpg"
        client.storage.from("trip-photos").upload(path, bytes)
        val reference = storedTripPhotoReference(path)
        return client.resolveTripPhotoReference(reference) ?: error("Не удалось открыть изображение")
    }

    suspend fun changePassword(password: String) {
        require(password.length >= 6) { "Пароль должен содержать минимум 6 символов" }
        client.auth.updateUser { this.password = password }
    }

    suspend fun deleteAccount() {
        if (client.auth.currentUserOrNull() == null) throw AuthSessionRequiredException()
        client.functions.invoke(
            function = "delete-account",
            body = buildJsonObject { },
        )
    }
}
