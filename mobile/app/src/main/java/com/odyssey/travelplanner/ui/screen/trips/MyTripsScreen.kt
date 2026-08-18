package com.odyssey.travelplanner.ui.screen.trips

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.data.AccountRepository
import com.odyssey.travelplanner.data.AuthRestoreResult
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripCard
import com.odyssey.travelplanner.ui.common.EmptyStateCard
import com.odyssey.travelplanner.ui.common.NewTripCard
import com.odyssey.travelplanner.ui.common.TripListCard
import com.odyssey.travelplanner.ui.common.TripsLoadingCard
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.screen.account.AccountSettingsSheet
import com.odyssey.travelplanner.ui.screen.auth.RamingoBrand
import com.odyssey.travelplanner.ui.screen.tripedit.EditTripPanel
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDanger
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBackground
import com.odyssey.travelplanner.ui.theme.OdysseySheetScrim
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun MyTripsScreen(onTripClick: (String) -> Unit, onNewTrip: () -> Unit, onLogout: () -> Unit, darkTheme: Boolean, onThemeToggle: () -> Unit, onThemeSet: (Boolean) -> Unit, language: String, onLanguageChange: (String) -> Unit, sessionRestoreVersion: Int) {
    var filter by remember { mutableStateOf("all") }
    var loading by remember { mutableStateOf(true) }
    var trips by remember { mutableStateOf<List<TripCard>>(emptyList()) }
    var loadFailed by remember { mutableStateOf(false) }
    var lastTripsReloadAt by remember { mutableStateOf(0L) }
    var editingTrip by remember { mutableStateOf<TripCard?>(null) }
    var accountMenuOpen by remember { mutableStateOf(false) }
    var profileEmail by remember { mutableStateOf("") }
    var profileAvatarUrl by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var passwordEditorOpen by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var repeatedNewPassword by remember { mutableStateOf("") }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    var accountDeleteDialogOpen by remember { mutableStateOf(false) }
    var accountDeleting by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    fun reloadTrips(force: Boolean = false) {
        val now = System.currentTimeMillis()
        if (!force && (loading || now - lastTripsReloadAt < 1_500L)) return
        lastTripsReloadAt = now
        scope.launch {
            loading = true
            loadFailed = false
            val restoreResult = SupabaseProvider.restorePersistentSession()
            if (restoreResult != AuthRestoreResult.RESTORED) {
                loadFailed = trips.isEmpty()
                loading = false
                return@launch
            }
            val client = SupabaseProvider.clientForCurrentAuthFlow()
            runCatching { SupabaseTripRepository(client).loadTrips() }
                .onSuccess {
                    if (it.isNotEmpty() || trips.isEmpty()) {
                        trips = it
                    }
                }
                .onFailure {
                    loadFailed = trips.isEmpty()
                }
            loading = false
        }
    }
    val photoPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            accountMessage = null
            runCatching {
                val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() } ?: error("Не удалось прочитать изображение")
                val repository = AccountRepository(SupabaseProvider.clientForCurrentAuthFlow())
                val url = repository.uploadProfilePhoto(bytes)
                repository.updateProfile(url, notificationsEnabled)
                url
            }.onSuccess { profileAvatarUrl = it; accountMessage = localized(language, "Фото профиля обновлено", "Profile photo updated", "Foto de perfil actualizada", "Profilbild aktualisiert") }
                .onFailure { accountMessage = it.message ?: localized(language, "Не удалось загрузить фото", "Could not upload photo", "No se pudo cargar la foto", "Foto konnte nicht hochgeladen werden") }
        }
    }

    LaunchedEffect(Unit) {
        profileEmail = runCatching {
            SupabaseProvider.clientForCurrentAuthFlow().auth.currentSessionOrNull()?.user?.email.orEmpty()
        }.getOrDefault("")
        runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadProfile() }.getOrNull()?.let { profile ->
            profileAvatarUrl = profile.avatarUrl
            notificationsEnabled = profile.notificationsEnabled
            onThemeSet(profile.darkTheme)
        }
    }

    LaunchedEffect(Unit) { reloadTrips(force = true) }

    LaunchedEffect(sessionRestoreVersion) {
        if (sessionRestoreVersion > 0) reloadTrips()
    }

    val upcoming = trips.filter {
        !it.status.contains("чернов", ignoreCase = true) &&
            !it.status.contains("заверш", ignoreCase = true) &&
            !it.status.contains("прошед", ignoreCase = true)
    }
    val drafts = trips.filter { it.status.contains("чернов", ignoreCase = true) }
    val completed = trips.filter {
        it.status.contains("заверш", ignoreCase = true) ||
            it.status.contains("прошед", ignoreCase = true)
    }
    val visibleTrips = when (filter) {
        "upcoming" -> upcoming
        "drafts" -> drafts
        "completed" -> completed
        else -> trips
    }
    fun tripCountLabel(count: Int): String = if (loading) "…" else count.toString()
    val filters = listOf(
        "all" to localized("Все · ${tripCountLabel(trips.size)}", "All · ${tripCountLabel(trips.size)}", "Todos · ${tripCountLabel(trips.size)}", "Alle · ${tripCountLabel(trips.size)}"),
        "upcoming" to localized("Предстоящие · ${tripCountLabel(upcoming.size)}", "Upcoming · ${tripCountLabel(upcoming.size)}", "Próximos · ${tripCountLabel(upcoming.size)}", "Bevorstehend · ${tripCountLabel(upcoming.size)}"),
        "drafts" to localized("Черновики · ${tripCountLabel(drafts.size)}", "Drafts · ${tripCountLabel(drafts.size)}", "Borradores · ${tripCountLabel(drafts.size)}", "Entwürfe · ${tripCountLabel(drafts.size)}"),
        "completed" to localized("Завершённые · ${tripCountLabel(completed.size)}", "Completed · ${tripCountLabel(completed.size)}", "Completados · ${tripCountLabel(completed.size)}", "Abgeschlossen · ${tripCountLabel(completed.size)}"),
    )

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(if (darkTheme) OdysseyDarkBackground else OdysseyBackground)
                .padding(WindowInsets.statusBars.asPaddingValues()),
        ) {
        Box(modifier = Modifier.fillMaxWidth().height(54.dp)) {
            RamingoBrand(modifier = Modifier.align(Alignment.Center))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 4.dp)
                    .size(48.dp)
                    .semantics {
                        contentDescription = localized(language, "Открыть настройки", "Open settings", "Abrir ajustes", "Einstellungen öffnen")
                        role = Role.Button
                    }
                    .clickable { accountMenuOpen = true },
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = null,
                    tint = contentTextColor(),
                    modifier = Modifier.size(23.dp),
                )
            }
        }

        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(start = 18.dp, end = 18.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item {
                Text(
                    text = localized("Мои путешествия", "My trips", "Mis viajes", "Meine Reisen"),
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 32.sp,
                    lineHeight = 33.sp,
                    modifier = Modifier.padding(top = 22.dp),
                )
            }
            item {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                ) {
                    filters.forEach { (key, label) ->
                        val selected = filter == key
                        Button(
                            onClick = { filter = key },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (selected) primaryColor() else cardSurfaceColor(),
                                contentColor = if (selected) primaryContentColor() else contentTextColor(),
                            ),
                            border = if (selected) null else androidx.compose.foundation.BorderStroke(1.dp, contentBorderColor()),
                            shape = RoundedCornerShape(20.dp),
                            elevation = ButtonDefaults.buttonElevation(defaultElevation = if (selected) 4.dp else 0.dp),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 15.dp, vertical = 9.dp),
                            modifier = Modifier.height(38.dp),
                        ) {
                            Text(label, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 13.sp, maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
            if (loading) {
                item { TripsLoadingCard() }
            } else if (loadFailed) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Explore,
                        title = localized("Не удалось загрузить путешествия", "Could not load trips", "No se pudieron cargar los viajes", "Reisen konnten nicht geladen werden"),
                        body = localized("Проверьте соединение и попробуйте ещё раз", "Check your connection and try again", "Compruebe la conexión e inténtelo de nuevo", "Prüfen Sie die Verbindung und versuchen Sie es erneut"),
                        action = localized("Повторить", "Retry", "Reintentar", "Erneut versuchen"),
                        onAction = { reloadTrips(force = true) },
                    )
                }
            } else if (visibleTrips.isEmpty()) {
                item {
                    EmptyStateCard(
                        icon = Icons.Outlined.Explore,
                        title = localized("Здесь появятся ваши путешествия", "Your trips will appear here", "Aquí aparecerán sus viajes", "Hier erscheinen Ihre Reisen"),
                        body = localized("Создайте первую поездку с нуля или выберите готовый маршрут", "Create your first trip from scratch or choose a ready route", "Cree su primer viaje desde cero o elija una ruta", "Erstellen Sie Ihre erste Reise oder wählen Sie eine fertige Route"),
                        action = localized("Создать путешествие", "Create trip", "Crear viaje", "Reise erstellen"),
                        onAction = onNewTrip,
                    )
                }
            } else {
                items(visibleTrips, key = { it.id }) { trip -> TripListCard(trip, onTripClick) { editingTrip = trip } }
            }
            item { NewTripCard(onNewTrip) }
        }
        }

        if (editingTrip != null) {
            androidx.compose.ui.window.Dialog(
                onDismissRequest = { editingTrip = null },
                properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(OdysseySheetScrim),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 18.dp)
                            .heightIn(max = 740.dp)
                            .clip(RoundedCornerShape(22.dp))
                            .background(cardSurfaceColor())
                            .verticalScroll(rememberScrollState()),
                    ) {
                        EditTripPanel(
                            editingTrip!!,
                            onClose = { editingTrip = null },
                            onSaved = { updated ->
                                trips = trips.map { if (it.id == updated.id) updated else it }
                                editingTrip = null
                            },
                            onDeleted = { deletedId ->
                                trips = trips.filterNot { it.id == deletedId }
                                editingTrip = null
                            },
                        )
                    }
                }
            }
        }

        if (accountMenuOpen) {
            AccountSettingsSheet(
                profileEmail = profileEmail,
                profileAvatarUrl = profileAvatarUrl,
                trips = trips,
                language = language,
                darkTheme = darkTheme,
                passwordEditorOpen = passwordEditorOpen,
                newPassword = newPassword,
                repeatedNewPassword = repeatedNewPassword,
                accountMessage = accountMessage,
                onDismiss = { accountMenuOpen = false },
                onPhotoPick = { photoPicker.launch("image/*") },
                onLanguageChange = { code ->
                    onLanguageChange(code)
                    scope.launch {
                        runCatching {
                            AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateProfile(
                                profileAvatarUrl,
                                notificationsEnabled,
                                language = code,
                                darkTheme = darkTheme,
                            )
                        }.onFailure { accountMessage = it.message ?: localized(language, "Не удалось сохранить язык", "Could not save language", "No se pudo guardar el idioma", "Sprache konnte nicht gespeichert werden") }
                    }
                },
                onThemeToggle = {
                    val nextTheme = !darkTheme
                    onThemeToggle()
                    scope.launch {
                        runCatching {
                            AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).updateProfile(
                                profileAvatarUrl,
                                notificationsEnabled,
                                language = language,
                                darkTheme = nextTheme,
                            )
                        }.onFailure { accountMessage = it.message ?: localized(language, "Не удалось сохранить тему", "Could not save theme", "No se pudo guardar el tema", "Thema konnte nicht gespeichert werden") }
                    }
                },
                onPasswordEditorToggle = {
                    passwordEditorOpen = !passwordEditorOpen
                    accountMessage = null
                },
                onNewPasswordChange = { newPassword = it },
                onRepeatedPasswordChange = { repeatedNewPassword = it },
                onSavePassword = {
                    when {
                        newPassword.length < 6 -> accountMessage = localized(language, "Пароль должен содержать минимум 6 символов", "Password must contain at least 6 characters", "La contraseña debe tener al menos 6 caracteres", "Das Passwort muss mindestens 6 Zeichen enthalten")
                        newPassword != repeatedNewPassword -> accountMessage = localized(language, "Пароли не совпадают", "Passwords do not match", "Las contraseñas no coinciden", "Passwörter stimmen nicht überein")
                        else -> scope.launch {
                            runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).changePassword(newPassword) }
                                .onSuccess { newPassword = ""; repeatedNewPassword = ""; passwordEditorOpen = false; accountMessage = localized(language, "Пароль обновлён", "Password updated", "Contraseña actualizada", "Passwort aktualisiert") }
                                .onFailure { accountMessage = it.message ?: localized(language, "Не удалось сменить пароль", "Could not change password", "No se pudo cambiar la contraseña", "Passwort konnte nicht geändert werden") }
                        }
                    }
                },
                onDeleteAccount = {
                    accountMessage = null
                    accountDeleteDialogOpen = true
                },
                onSignOut = {
                    scope.launch {
                        SupabaseProvider.signOutForAccountPicker()
                        accountMenuOpen = false
                        onLogout()
                    }
                },
            )
        }
        if (accountDeleteDialogOpen) {
            AlertDialog(
                onDismissRequest = { if (!accountDeleting) accountDeleteDialogOpen = false },
                title = {
                    Text(
                        localized("Удалить аккаунт?", "Delete account?", "¿Eliminar cuenta?", "Konto löschen?"),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            localized(
                                "Будут удалены профиль, поездки, участники и загруженные фотографии. Это действие нельзя отменить.",
                                "Your profile, trips, collaborators, and uploaded photos will be deleted. This cannot be undone.",
                                "Se eliminarán su perfil, viajes, colaboradores y fotos subidas. Esta acción no se puede deshacer.",
                                "Ihr Profil, Reisen, Mitreisende und hochgeladene Fotos werden gelöscht. Das kann nicht rückgängig gemacht werden.",
                            ),
                            fontFamily = Manrope,
                            fontSize = 13.sp,
                        )
                        if (accountMessage != null) {
                            Text(
                                accountMessage!!,
                                color = OdysseyDanger,
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W700,
                                fontSize = 12.sp,
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        enabled = !accountDeleting,
                        onClick = {
                            scope.launch {
                                accountDeleting = true
                                accountMessage = null
                                runCatching {
                                    AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).deleteAccount()
                                    SupabaseProvider.clearActiveSessionLocally()
                                }.onSuccess {
                                    accountDeleteDialogOpen = false
                                    accountMenuOpen = false
                                    onLogout()
                                }.onFailure {
                                    accountMessage = it.message ?: localized(language, "Не удалось удалить аккаунт", "Could not delete account", "No se pudo eliminar la cuenta", "Konto konnte nicht gelöscht werden")
                                }
                                accountDeleting = false
                            }
                        },
                    ) {
                        Text(
                            if (accountDeleting) localized("Удаляем…", "Deleting…", "Eliminando…", "Wird gelöscht…") else localized("Удалить", "Delete", "Eliminar", "Löschen"),
                            color = OdysseyDanger,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                        )
                    }
                },
                dismissButton = {
                    TextButton(enabled = !accountDeleting, onClick = { accountDeleteDialogOpen = false }) {
                        Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), fontFamily = Manrope)
                    }
                },
            )
        }
    }
}

