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
    )

    private fun planReminderTrips(
        trips: List<ReminderTrip>,
        language: String,
        accountId: String = "",
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ReminderEvent> = trips
        .flatMap { trip -> plan(trip, language, accountId, now, zone) }
        .distinctBy(ReminderEvent::key)
        .sortedBy(ReminderEvent::triggerAtMillis)

    fun plan(
        trip: ReminderTrip,
        language: String,
        accountId: String = "",
        now: Instant = Instant.now(),
        zone: ZoneId = ZoneId.systemDefault(),
    ): List<ReminderEvent> {
        if (isFinishedTrip(trip.status)) return emptyList()

        val result = buildList {
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
                        )?.let(::add)
                    }
                }
            }

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
                        )?.let(::add)
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
    ): ReminderEvent? {
        val triggerAt = ZonedDateTime.of(
            triggerDate,
            LocalTime.of(REMINDER_HOUR, 0),
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
    const val ACTION_DELIVER = "com.odyssey.travelplanner.action.DELIVER_REMINDER"
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

    private val lock = Any()
    private val scheduledCodesByTrip = mutableMapOf<String, Set<Int>>()

    fun sync(
        context: Context,
        trips: List<TripCard>,
        notificationsEnabled: Boolean,
        language: String,
        now: Instant = Instant.now(),
    ) {
        val appContext = context.applicationContext
        val accountId = SupabaseProvider.clientForCurrentAuthFlow().auth.currentUserOrNull()?.id?.toString().orEmpty()
        val canSchedule = notificationsEnabled && canPostNotifications(appContext) && accountId.isNotBlank()
        val events = if (canSchedule) {
            ReminderPlanner.plan(trips, language, accountId = accountId, now = now)
        } else {
            emptyList()
        }

        val previousCodes = synchronized(lock) {
            val codes = scheduledCodesByTrip.values.flatten().toSet()
            scheduledCodesByTrip.clear()
            codes
        }
        previousCodes.forEach { cancelCode(appContext, it) }
        events.forEach { schedule(appContext, it) }
        synchronized(lock) {
            events.groupBy { event -> "$accountId:${event.tripId}" }
                .mapValues { (_, grouped) -> grouped.map(ReminderEvent::requestCode).toSet() }
                .forEach { (tripKey, codes) -> scheduledCodesByTrip[tripKey] = codes }
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
    ) {
        val appContext = context.applicationContext
        val accountId = SupabaseProvider.clientForCurrentAuthFlow().auth.currentUserOrNull()?.id?.toString().orEmpty()
        val tripKey = "$accountId:${trip.id}"
        val previousCodes = synchronized(lock) { scheduledCodesByTrip.remove(tripKey).orEmpty() }
        previousCodes.forEach { cancelCode(appContext, it) }
        val canSchedule = notificationsEnabled && canPostNotifications(appContext) && accountId.isNotBlank()
        val events = if (canSchedule) {
            ReminderPlanner.plan(
                ReminderTrip(trip.id, trip.title, trip.dates, trip.status, trip.accommodations),
                language,
                accountId = accountId,
                now = now,
            )
        } else {
            emptyList()
        }
        events.forEach { schedule(appContext, it) }
        if (events.isNotEmpty()) {
            synchronized(lock) {
                scheduledCodesByTrip[tripKey] = events.map(ReminderEvent::requestCode).toSet()
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
            val current = scheduledCodesByTrip.values.flatten().toSet()
            scheduledCodesByTrip.clear()
            current
        }
        codes.forEach { cancelCode(appContext, it) }
        cancelMaintenance(appContext)
    }

    fun cancelTrip(context: Context, tripId: String) {
        val appContext = context.applicationContext
        val codes = synchronized(lock) {
            val matchingKeys = scheduledCodesByTrip.keys.filter { key -> key.endsWith(":$tripId") }
            val matchingCodes = matchingKeys.flatMap { key -> scheduledCodesByTrip.remove(key).orEmpty() }.toSet()
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
        val profile = runCatching { AccountRepository(client).loadProfile() }.getOrElse { return }
        if (!profile.notificationsEnabled) {
            cancelAll(appContext)
            return
        }
        val trips = runCatching { SupabaseTripRepository(client).loadTrips() }.getOrElse { return }
        sync(appContext, trips, notificationsEnabled = true, language = profile.language)
    }

    internal fun canPostNotifications(context: Context): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return false
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        return manager?.areNotificationsEnabled() != false
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
        // notifications after the user turns them off. A short timeout keeps
        // reminders useful when the device is offline.
        return withTimeoutOrNull(1_500L) {
            runCatching {
                AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadProfile().notificationsEnabled
            }.getOrNull()
        } ?: true
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
        alarmManager.setAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            event.triggerAtMillis,
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
