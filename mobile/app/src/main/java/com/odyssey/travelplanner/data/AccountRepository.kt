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
    val darkTheme: Boolean = false,
)

class AccountRepository(private val client: SupabaseClient) {
    suspend fun loadProfile(): AccountProfile {
        val userId = client.auth.currentUserOrNull()?.id?.toString() ?: return AccountProfile(null, false)
        val row = client.from("user_data").select {
            filter { eq("user_id", userId); eq("key", "account_profile") }
        }.decodeList<UserDataRow>().firstOrNull()
        return AccountProfile(
            avatarUrl = row?.value?.get("avatar_url")?.jsonPrimitive?.contentOrNull,
            notificationsEnabled = row?.value?.get("notifications_enabled")?.jsonPrimitive?.content == "true",
            language = row?.value?.get("language")?.jsonPrimitive?.contentOrNull ?: "RU",
            darkTheme = row?.value?.get("dark_theme")?.jsonPrimitive?.content == "true",
        )
    }

    suspend fun updateProfile(
        avatarUrl: String?,
        notificationsEnabled: Boolean,
        language: String? = null,
        darkTheme: Boolean? = null,
    ) {
        val userId = client.auth.currentUserOrNull()?.id?.toString() ?: error("Необходимо войти в аккаунт")
        val existing = client.from("user_data").select {
            filter { eq("user_id", userId); eq("key", "account_profile") }
        }.decodeList<UserDataRow>().firstOrNull()?.value
        val value = existing?.toMutableMap() ?: mutableMapOf()
        avatarUrl?.let { value["avatar_url"] = JsonPrimitive(it) }
        value["notifications_enabled"] = JsonPrimitive(notificationsEnabled)
        language?.let { value["language"] = JsonPrimitive(it) }
        darkTheme?.let { value["dark_theme"] = JsonPrimitive(it) }
        client.from("user_data").upsert(
            UserDataRow(
                userId = userId,
                key = "account_profile",
                value = JsonObject(value),
            ),
        ) { onConflict = "user_id,key" }
    }

    suspend fun updateAppearance(language: String, darkTheme: Boolean) {
        val profile = loadProfile()
        updateProfile(profile.avatarUrl, profile.notificationsEnabled, language = language, darkTheme = darkTheme)
    }

    suspend fun uploadProfilePhoto(bytes: ByteArray): String {
        require(bytes.isNotEmpty()) { "Не удалось прочитать изображение" }
        val userId = client.auth.currentUserOrNull()?.id?.toString() ?: error("Необходимо войти в аккаунт")
        val path = "$userId/profile/${UUID.randomUUID()}.jpg"
        client.storage.from("trip-photos").upload(path, bytes)
        return client.storage.from("trip-photos").publicUrl(path)
    }

    suspend fun changePassword(password: String) {
        require(password.length >= 6) { "Пароль должен содержать минимум 6 символов" }
        client.auth.updateUser { this.password = password }
    }

    suspend fun deleteAccount() {
        check(client.auth.currentUserOrNull() != null) { "Необходимо войти в аккаунт" }
        client.functions.invoke(
            function = "delete-account",
            body = buildJsonObject { },
        )
    }
}
