package com.odyssey.travelplanner.data

enum class ThemePreference(val storageValue: String) {
    SYSTEM("system"),
    LIGHT("light"),
    DARK("dark");

    companion object {
        fun fromStorage(value: String?): ThemePreference = when {
            value.equals("light", ignoreCase = true) -> LIGHT
            value.equals("dark", ignoreCase = true) -> DARK
            else -> SYSTEM
        }
    }
}
