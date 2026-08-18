package com.odyssey.travelplanner.data

/**
 * A member's role in a trip.
 *
 * The wire form is a Russian display string, because that is what the shared
 * Supabase payload has always stored and what the web client reads from the
 * same rows. Those strings stay confined to [fromWire] and [toWire]; nothing
 * else in the app should compare roles by text.
 */
enum class TripRole {
    None,
    Owner,
    Editor,
    Reader,
    ;

    val isOwner: Boolean get() = this == Owner

    val canEdit: Boolean get() = this == Owner || this == Editor

    fun toWire(): String = when (this) {
        None -> ""
        Owner -> OWNER
        Editor -> EDITOR
        Reader -> READER
    }

    companion object {
        private const val OWNER = "Владелец"
        private const val EDITOR = "Редактор"
        private const val READER = "Читатель"

        fun fromWire(value: String?): TripRole = when (value?.trim()) {
            OWNER -> Owner
            EDITOR -> Editor
            READER -> Reader
            else -> None
        }

        /** Roles an owner may assign to somebody else. */
        val assignable = listOf(Editor, Reader)
    }
}
