package com.odyssey.travelplanner.notifications

import android.Manifest
import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import com.odyssey.travelplanner.MainActivity
import com.odyssey.travelplanner.R
import com.odyssey.travelplanner.data.AccountRepository
import com.odyssey.travelplanner.data.Accommodation
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.TripCard
import com.odyssey.travelplanner.data.TripOverview
import com.odyssey.travelplanner.data.SupabaseTripRepository
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Locale

private const val ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED =
    "android.app.action.SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED"

internal enum class ReminderKind {
    TRIP,
    FREE_CANCELLATION,
}

internal data class ReminderTrip(
    val id: String,
    val title: String,
    val dates: String,
    val status: String,
    val accommodations: List<Accommodation>,
)

internal data class ReminderEvent(
    val key: String,
    val accountId: String,
    val tripId: String,
    val tripTitle: String,
    val accommodationId: String?,
    val kind: ReminderKind,
    val targetDate: LocalDate,
    val daysRemaining: Long,
    val triggerAtMillis: Long,
    val notificationTitle: String,
    val notificationText: String,
) {
    val requestCode: Int
        get() = (key.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
}

internal object ReminderPlanner {
    const val REMINDER_HOUR = 9

    private val tripReminderDays = listOf(30L, 14L, 7L, 3L, 1L)
    private val cancellationReminderDays = listOf(7L, 3L, 1L, 0L)

    fun plan(
        trips: List<TripCard>,
        language: String,
        accountId: String = "",
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        tripRemindersEnabled: Boolean = true,
        cancellationRemindersEnabled: Boolean = true,
        reminderHour: Int = REMINDER_HOUR,
    ): List<ReminderEvent> = planReminderTrips(
        trips = trips.map { trip ->
            ReminderTrip(
                id = trip.id,
                title = trip.title,
                dates = trip.dates,
                status = trip.status,
                accommodations = trip.accommodations,
            )
        },
        language = language,
        accountId = accountId,
        now = now,
        zone = zone,
        tripRemindersEnabled = tripRemindersEnabled,
        cancellationRemindersEnabled = cancellationRemindersEnabled,
        reminderHour = reminderHour,
    )

    private fun planReminderTrips(
        trips: List<ReminderTrip>,
        language: String,
        accountId: String = "",
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        tripRemindersEnabled: Boolean = true,
        cancellationRemindersEnabled: Boolean = true,
        reminderHour: Int = REMINDER_HOUR,
    ): List<ReminderEvent> = trips
        .flatMap {
            trip -> plan(
                trip = trip,
                language = language,
                accountId = accountId,
                now = now,
                zone = zone,
                tripRemindersEnabled = tripRemindersEnabled,
                cancellationRemindersEnabled = cancellationRemindersEnabled,
                reminderHour = reminderHour,
            )
        }
        .distinctBy(ReminderEvent::key)
        .sortedBy(ReminderEvent::triggerAtMillis)

    fun plan(
        trip: ReminderTrip,
        language: String,
        accountId: String = "",
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
        tripRemindersEnabled: Boolean = true,
        cancellationRemindersEnabled: Boolean = true,
        reminderHour: Int = REMINDER_HOUR,
    ): List<ReminderEvent> {
        if (isFinishedTrip(trip.status)) return emptyList()

        val result = buildList {
            if (tripRemindersEnabled) {
                parseDateRange(trip.dates)?.let { (start, end) ->
                    if (!end.isBefore(start)) {
                        tripReminderDays.forEach { daysBefore ->
                            reminderAt(
                                kind = ReminderKind.TRIP,
                                accountId = accountId,
                                trip = trip,
                                accommodation = null,
                                targetDate = start,
                                daysRemaining = daysBefore,
                                triggerDate = start.minusDays(daysBefore),
                                language = language,
                                now = now,
                                zone = zone,
                                reminderHour = reminderHour,
                            )?.let(::add)
                        }
                    }
                }
            }

            if (cancellationRemindersEnabled) {
                trip.accommodations
                    .filterNot { accommodation -> isStayedAccommodation(accommodation.status) }
                    .forEach { accommodation ->
                        val deadline = parseSingleDate(accommodation.deadline) ?: return@forEach
                        cancellationReminderDays.forEach { daysBefore ->
                            reminderAt(
                                kind = ReminderKind.FREE_CANCELLATION,
                                accountId = accountId,
                                trip = trip,
                                accommodation = accommodation,
                                targetDate = deadline,
                                daysRemaining = daysBefore,
                                triggerDate = deadline.minusDays(daysBefore),
                                language = language,
                                now = now,
                                zone = zone,
                                reminderHour = reminderHour,
                            )?.let(::add)
                        }
                    }
            }
        }
        return result
    }

    internal fun parseDateRange(value: String): Pair<LocalDate, LocalDate>? {
        val dates = extractDates(value)
        val start = dates.firstOrNull() ?: return null
        return start to dates.getOrElse(1) { start }
    }

    internal fun parseSingleDate(value: String): LocalDate? = extractDates(value).firstOrNull()

    private fun reminderAt(
        kind: ReminderKind,
        accountId: String,
        trip: ReminderTrip,
        accommodation: Accommodation?,
        targetDate: LocalDate,
        daysRemaining: Long,
        triggerDate: LocalDate,
        language: String,
        now: Instant,
        zone: ZoneId,
        reminderHour: Int,
    ): ReminderEvent? {
        val triggerAt = ZonedDateTime.of(
            triggerDate,
            LocalTime.of(reminderHour.coerceIn(0, 23), 0),
            zone,
        ).toInstant()
        if (!triggerAt.isAfter(now)) return null

        val title = trip.title.trim().ifBlank { localized(language, "Путешествие", "Trip", "Viaje", "Reise") }
        val accommodationName = accommodation?.name?.trim().orEmpty()
            .ifBlank { localized(language, "жилья", "lodging", "alojamiento", "Unterkunft") }
        val place = listOf(accommodationName, accommodation?.city?.trim().orEmpty())
            .filter(String::isNotBlank)
            .joinToString(" · ")
        val kindKey = when (kind) {
            ReminderKind.TRIP -> "trip"
            ReminderKind.FREE_CANCELLATION -> "cancellation"
        }
        val itemKey = accommodation?.id ?: "trip"
        val key = "$accountId:$kindKey:${trip.id}:$itemKey:$daysRemaining"
        val notificationTitle: String
        val notificationText: String
        when (kind) {
            ReminderKind.TRIP -> {
                notificationTitle = localized(
                    language,
                    "Скоро путешествие",
                    "Trip reminder",
                    "Recordatorio del viaje",
                    "Reiseerinnerung",
                )
                notificationText = if (daysRemaining == 0L) {
                    localized(
                        language,
                        "Путешествие «$title» начинается сегодня",
                        "Your trip «$title» starts today",
                        "Tu viaje «$title» comienza hoy",
                        "Ihre Reise «$title» beginnt heute",
                    )
                } else {
                    localized(
                        language,
                        "До путешествия «$title» осталось ${daysRemaining.toRussianDays()}",
                        "${daysRemaining.toEnglishDays()} left until «$title»",
                        "Faltan ${daysRemaining.toSpanishDays()} para «$title»",
                        "Noch ${daysRemaining.toGermanDays()} bis «$title»",
                    )
                }
            }

            ReminderKind.FREE_CANCELLATION -> {
                notificationTitle = localized(
                    language,
                    "Срок бесплатной отмены",
                    "Free cancellation deadline",
                    "Fecha límite de cancelación gratuita",
                    "Frist für kostenlose Stornierung",
                )
                notificationText = if (daysRemaining == 0L) {
                    localized(
                        language,
                        "Сегодня заканчивается бесплатная отмена «$place»",
                        "Free cancellation for «$place» ends today",
                        "La cancelación gratuita de «$place» termina hoy",
                        "Die kostenlose Stornierung für «$place» endet heute",
                    )
                } else {
                    localized(
                        language,
                        "До конца бесплатной отмены «$place» осталось ${daysRemaining.toRussianDays()}",
                        "${daysRemaining.toEnglishDays()} left to cancel «$place» for free",
                        "Quedan ${daysRemaining.toSpanishDays()} para cancelar «$place» gratis",
                        "Noch ${daysRemaining.toGermanDays()}, um «$place» kostenlos zu stornieren",
                    )
                }
            }
        }

        return ReminderEvent(
            key = key,
            accountId = accountId,
            tripId = trip.id,
            tripTitle = title,
            accommodationId = accommodation?.id,
            kind = kind,
            targetDate = targetDate,
            daysRemaining = daysRemaining,
            triggerAtMillis = triggerAt.toEpochMilli(),
            notificationTitle = notificationTitle,
            notificationText = notificationText,
        )
    }

    private fun extractDates(value: String): List<LocalDate> {
        val source = value.trim()
        if (source.isBlank()) return emptyList()

        val isoDates = Regex("""(?<!\d)(\d{4})-(\d{2})-(\d{2})(?!\d)""")
            .findAll(source)
            .mapNotNull { match ->
                runCatching {
                    LocalDate.of(
                        match.groupValues[1].toInt(),
                        match.groupValues[2].toInt(),
                        match.groupValues[3].toInt(),
                    )
                }.getOrNull()
            }
            .toList()
        if (isoDates.isNotEmpty()) return isoDates

        val dottedDates = Regex("""(?<!\d)(\d{1,2})[./](\d{1,2})[./](\d{4})(?!\d)""")
            .findAll(source)
            .mapNotNull { match ->
                runCatching {
                    LocalDate.of(
                        match.groupValues[3].toInt(),
                        match.groupValues[2].toInt(),
                        match.groupValues[1].toInt(),
                    )
                }.getOrNull()
            }
            .toList()
        if (dottedDates.isNotEmpty()) return dottedDates

        val monthPattern = monthNames.keys
            .sortedByDescending(String::length)
            .joinToString("|") { Regex.escape(it) }
        return Regex(
            """(?<!\d)(\d{1,2})\s+($monthPattern)\s+(\d{4})(?!\d)""",
            RegexOption.IGNORE_CASE,
        ).findAll(source).mapNotNull { match ->
            val month = monthNames[match.groupValues[2].lowercase(Locale.ROOT)] ?: return@mapNotNull null
            runCatching {
                LocalDate.of(match.groupValues[3].toInt(), month, match.groupValues[1].toInt())
            }.getOrNull()
        }.toList()
    }

    private fun isFinishedTrip(status: String): Boolean {
        val normalized = status.trim().lowercase(Locale.ROOT)
        return listOf("заверш", "прошед", "completed", "past", "finished").any(normalized::contains)
    }

    private fun isStayedAccommodation(status: String): Boolean {
        val normalized = status.trim().lowercase(Locale.ROOT)
        return listOf("пожил", "stayed", "visited", "past").any(normalized::contains)
    }

    private fun Long.toRussianDays(): String = "$this ${russianDayWord(toInt())}"

    private fun Long.toEnglishDays(): String = "$this ${if (this == 1L) "day" else "days"}"

    private fun Long.toSpanishDays(): String = "$this ${if (this == 1L) "día" else "días"}"

    private fun Long.toGermanDays(): String = "$this ${if (this == 1L) "Tag" else "Tage"}"

    private fun russianDayWord(value: Int): String = when {
        value % 10 == 1 && value % 100 != 11 -> "день"
        value % 10 in 2..4 && value % 100 !in 12..14 -> "дня"
        else -> "дней"
    }

    private fun localized(language: String, ru: String, en: String, es: String, de: String): String = when (language.trim().uppercase(Locale.ROOT)) {
        "EN" -> en
        "ES" -> es
        "DE" -> de
        else -> ru
    }

    private val monthNames = mapOf(
        "января" to 1, "январь" to 1, "янв" to 1,
        "февраля" to 2, "февраль" to 2, "фев" to 2,
        "марта" to 3, "март" to 3, "мар" to 3,
        "апреля" to 4, "апрель" to 4, "апр" to 4,
        "мая" to 5, "май" to 5,
        "июня" to 6, "июнь" to 6, "июн" to 6,
        "июля" to 7, "июль" to 7, "июл" to 7,
        "августа" to 8, "август" to 8, "авг" to 8,
        "сентября" to 9, "сентябрь" to 9, "сен" to 9, "сент" to 9,
        "октября" to 10, "октябрь" to 10, "окт" to 10,
        "ноября" to 11, "ноябрь" to 11, "ноя" to 11,
        "декабря" to 12, "декабрь" to 12, "дек" to 12,
        "january" to 1, "jan" to 1,
        "february" to 2, "feb" to 2,
        "march" to 3, "mar" to 3,
        "april" to 4, "apr" to 4,
        "may" to 5,
        "june" to 6, "jun" to 6,
        "july" to 7, "jul" to 7,
        "august" to 8, "aug" to 8,
        "september" to 9, "sep" to 9,
        "october" to 10, "oct" to 10,
        "november" to 11, "nov" to 11,
        "december" to 12, "dec" to 12,
        "enero" to 1, "ene" to 1,
        "febrero" to 2,
        "marzo" to 3,
        "abril" to 4,
        "mayo" to 5,
        "junio" to 6,
        "julio" to 7,
        "agosto" to 8,
        "septiembre" to 9, "setiembre" to 9,
        "octubre" to 10,
        "noviembre" to 11,
        "diciembre" to 12,
        "januar" to 1,
        "februar" to 2,
        "märz" to 3, "maerz" to 3,
        "april" to 4,
        "mai" to 5,
        "juni" to 6,
        "juli" to 7,
        "august" to 8,
        "september" to 9,
        "oktober" to 10,
        "november" to 11,
        "dezember" to 12,
    )
}

internal object ReminderScheduler {
    // The suffix also invalidates alarms created by releases that had no
    // persistent registry; their old broadcasts are ignored after upgrade.
    const val ACTION_DELIVER = "com.odyssey.travelplanner.action.DELIVER_REMINDER_V2"
    private const val ACTION_REFRESH = "com.odyssey.travelplanner.action.REFRESH_REMINDERS"
    private const val CHANNEL_ID = "trip_reminders"
    private const val MAINTENANCE_REQUEST_CODE = 0
    private const val EXTRA_ACCOUNT_ID = "account_id"
    private const val EXTRA_TRIP_ID = "trip_id"
    private const val EXTRA_TRIP_TITLE = "trip_title"
    private const val EXTRA_KIND = "kind"
    private const val EXTRA_ACCOMMODATION_ID = "accommodation_id"
    private const val EXTRA_TARGET_DATE = "target_date"
    private const val EXTRA_TITLE = "notification_title"
    private const val EXTRA_TEXT = "notification_text"
    private const val SCHEDULER_PREFERENCES = "reminder_scheduler"
    private const val SCHEDULED_ALARMS_KEY = "scheduled_alarms"
    private const val FALLBACK_ALARM_WINDOW_MILLIS = 10 * 60 * 1000L

    private val lock = Any()
    // Technical scheduler state only; no user-visible trip data is persisted here.
    private val scheduledCodesByTrip = mutableMapOf<String, Set<Int>>()

    fun sync(
        context: Context,
        trips: List<TripCard>,
        notificationsEnabled: Boolean,
        language: String,
        now: Instant = Instant.now(),
        tripRemindersEnabled: Boolean = true,
        cancellationRemindersEnabled: Boolean = true,
        reminderHour: Int = ReminderPlanner.REMINDER_HOUR,
    ) {
        val appContext = context.applicationContext
        val accountId = SupabaseProvider.clientForCurrentAuthFlow().auth.currentUserOrNull()?.id?.toString().orEmpty()
        val canSchedule = notificationsEnabled && canPostNotifications(appContext) && accountId.isNotBlank()
        val events = if (canSchedule) {
            ReminderPlanner.plan(
                trips.filter { it.deletedAt.isNullOrBlank() },
                language,
                accountId = accountId,
                now = now,
                tripRemindersEnabled = tripRemindersEnabled,
                cancellationRemindersEnabled = cancellationRemindersEnabled,
                reminderHour = reminderHour,
            )
        } else {
            emptyList()
        }

        val previousCodes = synchronized(lock) {
            val registry = mergedRegistryLocked(appContext)
            val codes = registry.values.flatten().toSet()
            scheduledCodesByTrip.clear()
            writePersistedRegistry(appContext, emptyMap())
            codes
        }
        previousCodes.forEach { cancelCode(appContext, it) }
        events.forEach { schedule(appContext, it) }
        synchronized(lock) {
            val registry = events.groupBy { event -> "$accountId:${event.tripId}" }
                .mapValues { (_, grouped) -> grouped.map(ReminderEvent::requestCode).toSet() }
            scheduledCodesByTrip.putAll(registry)
            writePersistedRegistry(appContext, registry)
        }

        if (canSchedule) {
            ensureChannel(appContext)
            scheduleMaintenance(appContext)
        } else {
            cancelMaintenance(appContext)
        }
    }

    fun syncTrip(
        context: Context,
        trip: TripOverview,
        notificationsEnabled: Boolean,
        language: String,
        now: Instant = Instant.now(),
        tripRemindersEnabled: Boolean = true,
        cancellationRemindersEnabled: Boolean = true,
        reminderHour: Int = ReminderPlanner.REMINDER_HOUR,
    ) {
        val appContext = context.applicationContext
        val accountId = SupabaseProvider.clientForCurrentAuthFlow().auth.currentUserOrNull()?.id?.toString().orEmpty()
        val tripKey = "$accountId:${trip.id}"
        val previousCodes = synchronized(lock) {
            val registry = mergedRegistryLocked(appContext)
            val codes = registry.remove(tripKey).orEmpty()
            scheduledCodesByTrip.clear()
            scheduledCodesByTrip.putAll(registry)
            writePersistedRegistry(appContext, registry)
            codes
        }
        previousCodes.forEach { cancelCode(appContext, it) }
        val canSchedule = notificationsEnabled && canPostNotifications(appContext) && accountId.isNotBlank()
        val events = if (canSchedule) {
            ReminderPlanner.plan(
                ReminderTrip(trip.id, trip.title, trip.dates, trip.status, trip.accommodations),
                language,
                accountId = accountId,
                now = now,
                tripRemindersEnabled = tripRemindersEnabled,
                cancellationRemindersEnabled = cancellationRemindersEnabled,
                reminderHour = reminderHour,
            )
        } else {
            emptyList()
        }
        events.forEach { schedule(appContext, it) }
        if (events.isNotEmpty()) {
            synchronized(lock) {
                val registry = mergedRegistryLocked(appContext)
                registry[tripKey] = events.map(ReminderEvent::requestCode).toMutableSet()
                scheduledCodesByTrip.clear()
                scheduledCodesByTrip.putAll(registry)
                writePersistedRegistry(appContext, registry)
            }
            ensureChannel(appContext)
            scheduleMaintenance(appContext)
        } else if (scheduledCodesByTrip.isEmpty()) {
            cancelMaintenance(appContext)
        }
    }

    fun cancelAll(context: Context) {
        val appContext = context.applicationContext
        val codes = synchronized(lock) {
            val registry = mergedRegistryLocked(appContext)
            val current = registry.values.flatten().toSet()
            scheduledCodesByTrip.clear()
            writePersistedRegistry(appContext, emptyMap())
            current
        }
        codes.forEach { cancelCode(appContext, it) }
        cancelMaintenance(appContext)
    }

    fun cancelTrip(context: Context, tripId: String) {
        val appContext = context.applicationContext
        val codes = synchronized(lock) {
            val registry = mergedRegistryLocked(appContext)
            val matchingKeys = registry.keys.filter { key -> key.endsWith(":$tripId") }
            val matchingCodes = matchingKeys.flatMap { key -> registry.remove(key).orEmpty() }.toSet()
            scheduledCodesByTrip.clear()
            scheduledCodesByTrip.putAll(registry)
            writePersistedRegistry(appContext, registry)
            matchingCodes
        }
        codes.forEach { cancelCode(appContext, it) }
        if (synchronized(lock) { scheduledCodesByTrip.isEmpty() }) {
            cancelMaintenance(appContext)
        }
    }

    suspend fun refreshFromSupabase(context: Context) {
        val appContext = context.applicationContext
        val restoreResult = SupabaseProvider.restorePersistentSession()
        if (restoreResult == com.odyssey.travelplanner.data.AuthRestoreResult.NO_SESSION ||
            !SupabaseProvider.ensureActiveSession()
        ) {
            cancelAll(appContext)
            return
        }
        val client = SupabaseProvider.clientForCurrentAuthFlow()
        val profile = runCatching { AccountRepository(client).loadProfile() }.getOrElse {
            cancelAll(appContext)
            return
        }
        if (!profile.notificationsEnabled) {
            cancelAll(appContext)
            return
        }
        val trips = runCatching { SupabaseTripRepository(client).loadTrips() }.getOrElse {
            cancelAll(appContext)
            return
        }
        sync(
            appContext,
            trips,
            notificationsEnabled = true,
            language = profile.language,
            tripRemindersEnabled = profile.tripRemindersEnabled,
            cancellationRemindersEnabled = profile.cancellationRemindersEnabled,
            reminderHour = profile.reminderHour,
        )
    }

    internal fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return false
        if (!manager.areNotificationsEnabled()) return false
        return manager.getNotificationChannel(CHANNEL_ID)?.importance != NotificationManager.IMPORTANCE_NONE
    }

    internal fun canScheduleExactAlarms(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return false
        return alarmManager.canScheduleExactAlarms()
    }

    internal suspend fun shouldDeliver(context: Context, intent: Intent): Boolean {
        if (intent.action != ACTION_DELIVER || !canPostNotifications(context)) return false
        val expectedAccountId = intent.getStringExtra(EXTRA_ACCOUNT_ID).orEmpty()
        val restoreResult = SupabaseProvider.restorePersistentSession()
        if (restoreResult == com.odyssey.travelplanner.data.AuthRestoreResult.NO_SESSION ||
            !SupabaseProvider.ensureActiveSession()
        ) return false
        val actualAccountId = SupabaseProvider.clientForCurrentAuthFlow().auth.currentUserOrNull()?.id?.toString().orEmpty()
        if (expectedAccountId.isNotBlank() && expectedAccountId != actualAccountId) return false

        // A preference change is persisted in Supabase. Check it when the
        // process was started by an alarm so an old alarm cannot resurrect
        // notifications after the user turns them off. Fail closed when the
        // profile cannot be confirmed within the short timeout.
        return withTimeoutOrNull(1_500L) {
            runCatching {
                val profile = AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadProfile()
                profile.notificationsEnabled && when (intent.getStringExtra(EXTRA_KIND)) {
                    ReminderKind.TRIP.name -> profile.tripRemindersEnabled
                    ReminderKind.FREE_CANCELLATION.name -> profile.cancellationRemindersEnabled
                    else -> true
                }
            }.getOrNull()
        } == true
    }

    internal fun post(context: Context, intent: Intent) {
        val tripId = intent.getStringExtra(EXTRA_TRIP_ID).orEmpty()
        val title = intent.getStringExtra(EXTRA_TITLE).orEmpty()
        val text = intent.getStringExtra(EXTRA_TEXT).orEmpty()
        if (tripId.isBlank() || title.isBlank() || text.isBlank()) return
        ensureChannel(context)
        val eventKey = listOf(
            intent.getStringExtra(EXTRA_ACCOUNT_ID).orEmpty(),
            intent.getStringExtra(EXTRA_KIND).orEmpty(),
            tripId,
            intent.getStringExtra(EXTRA_ACCOMMODATION_ID).orEmpty(),
            intent.getStringExtra(EXTRA_TARGET_DATE).orEmpty(),
            text,
        ).joinToString(":")
        val notificationId = (eventKey.hashCode() and Int.MAX_VALUE).coerceAtLeast(1)
        val openTripIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(MainActivity.EXTRA_NOTIFICATION_TRIP_ID, tripId)
        }
        val contentIntent = PendingIntent.getActivity(
            context,
            notificationId,
            openTripIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setContentIntent(contentIntent)
            .setAutoCancel(true)
            .setCategory(Notification.CATEGORY_REMINDER)
            .setPriority(Notification.PRIORITY_DEFAULT)
            .build()
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager)
            ?.notify(notificationId, notification)
    }

    private fun schedule(context: Context, event: ReminderEvent) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply {
            action = ACTION_DELIVER
            putExtra(EXTRA_ACCOUNT_ID, event.accountId)
            putExtra(EXTRA_TRIP_ID, event.tripId)
            putExtra(EXTRA_TRIP_TITLE, event.tripTitle)
            putExtra(EXTRA_KIND, event.kind.name)
            putExtra(EXTRA_ACCOMMODATION_ID, event.accommodationId)
            putExtra(EXTRA_TARGET_DATE, event.targetDate.toString())
            putExtra(EXTRA_TITLE, event.notificationTitle)
            putExtra(EXTRA_TEXT, event.notificationText)
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val exactScheduled = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            canScheduleExactAlarms(context)
        } else {
            true
        }
        if (exactScheduled) {
            runCatching {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    event.triggerAtMillis,
                    pendingIntent,
                )
            }.onSuccess { return }.onFailure { error ->
                if (error !is SecurityException) throw error
            }
        }
        alarmManager.setWindow(
            AlarmManager.RTC_WAKEUP,
            event.triggerAtMillis,
            FALLBACK_ALARM_WINDOW_MILLIS,
            pendingIntent,
        )
    }

    private fun cancelCode(context: Context, requestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderAlarmReceiver::class.java).apply { action = ACTION_DELIVER }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun ensureChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        if (manager.getNotificationChannel(CHANNEL_ID) == null) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    "Напоминания о поездках",
                    NotificationManager.IMPORTANCE_DEFAULT,
                ).apply {
                    description = "Сроки бесплатной отмены и даты путешествий"
                },
            )
        }
    }

    private fun scheduleMaintenance(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val now = ZonedDateTime.now()
        var firstRun = now.withHour(3).withMinute(15).withSecond(0).withNano(0)
        if (!firstRun.isAfter(now)) firstRun = firstRun.plusDays(1)
        val intent = Intent(context, ReminderRescheduleReceiver::class.java).apply { action = ACTION_REFRESH }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MAINTENANCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        alarmManager.setInexactRepeating(
            AlarmManager.RTC_WAKEUP,
            firstRun.toInstant().toEpochMilli(),
            AlarmManager.INTERVAL_DAY,
            pendingIntent,
        )
    }

    private fun cancelMaintenance(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderRescheduleReceiver::class.java).apply { action = ACTION_REFRESH }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            MAINTENANCE_REQUEST_CODE,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE,
        ) ?: return
        alarmManager.cancel(pendingIntent)
        pendingIntent.cancel()
    }

    private fun mergedRegistryLocked(context: Context): MutableMap<String, MutableSet<Int>> {
        val registry = readPersistedRegistry(context)
        scheduledCodesByTrip.forEach { (tripKey, codes) ->
            registry.getOrPut(tripKey) { mutableSetOf() }.addAll(codes)
        }
        return registry
    }

    private fun readPersistedRegistry(context: Context): MutableMap<String, MutableSet<Int>> {
        val encoded = context
            .getSharedPreferences(SCHEDULER_PREFERENCES, Context.MODE_PRIVATE)
            .getString(SCHEDULED_ALARMS_KEY, null)
            ?: return mutableMapOf()
        return runCatching {
            val result = mutableMapOf<String, MutableSet<Int>>()
            val records = JSONArray(encoded)
            for (index in 0 until records.length()) {
                val record = records.optJSONObject(index) ?: continue
                val tripKey = record.optString("tripKey").trim()
                val requestCode = record.optInt("requestCode", 0)
                if (tripKey.isNotBlank() && requestCode > 0) {
                    result.getOrPut(tripKey) { mutableSetOf() }.add(requestCode)
                }
            }
            result
        }.getOrDefault(mutableMapOf())
    }

    private fun writePersistedRegistry(
        context: Context,
        registry: Map<String, Set<Int>>,
    ) {
        val records = JSONArray()
        registry.forEach { (tripKey, requestCodes) ->
            requestCodes.forEach { requestCode ->
                records.put(
                    JSONObject()
                        .put("tripKey", tripKey)
                        .put("requestCode", requestCode),
                )
            }
        }
        context
            .getSharedPreferences(SCHEDULER_PREFERENCES, Context.MODE_PRIVATE)
            .edit()
            .putString(SCHEDULED_ALARMS_KEY, records.toString())
            .commit()
    }
}

class ReminderAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                if (ReminderScheduler.shouldDeliver(context.applicationContext, intent)) {
                    ReminderScheduler.post(context.applicationContext, intent)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}

class ReminderRescheduleReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action !in setOf(
                Intent.ACTION_BOOT_COMPLETED,
                Intent.ACTION_TIMEZONE_CHANGED,
                Intent.ACTION_TIME_CHANGED,
                Intent.ACTION_MY_PACKAGE_REPLACED,
                ACTION_EXACT_ALARM_PERMISSION_STATE_CHANGED,
                ACTION_REFRESH_FOR_RECEIVER,
            )
        ) return
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                ReminderScheduler.refreshFromSupabase(context.applicationContext)
            } finally {
                pendingResult.finish()
            }
        }
    }

    private companion object {
        const val ACTION_REFRESH_FOR_RECEIVER = "com.odyssey.travelplanner.action.REFRESH_REMINDERS"
    }
}
