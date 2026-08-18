package com.odyssey.travelplanner.ui.screen.account

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeleteForever
import androidx.compose.material.icons.filled.Close
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.BuildConfig
import com.odyssey.travelplanner.R
import com.odyssey.travelplanner.data.AccountRepository
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.SupabaseTripRepository
import com.odyssey.travelplanner.data.TripCard
import com.odyssey.travelplanner.ui.domain.cityFilterKey
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.localizedCountWord
import com.odyssey.travelplanner.ui.i18n.normalizeLanguage
import com.odyssey.travelplanner.ui.i18n.splitStoredCityList
import com.odyssey.travelplanner.ui.screen.auth.AuthField
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBorder
import com.odyssey.travelplanner.ui.theme.OdysseyDarkMuted
import com.odyssey.travelplanner.ui.theme.OdysseyDarkSurface
import com.odyssey.travelplanner.ui.theme.OdysseyDarkTint
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.dangerSurfaceColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondarySurfaceColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
@OptIn(ExperimentalMaterial3Api::class)
internal fun AccountSettingsScreen(
    onBack: () -> Unit,
    onLogout: () -> Unit,
    darkTheme: Boolean,
    onThemeToggle: () -> Unit,
    onThemeSet: (Boolean) -> Unit,
    language: String,
    onLanguageChange: (String) -> Unit,
) {
    var profileEmail by remember { mutableStateOf("") }
    var profileAvatarUrl by remember { mutableStateOf<String?>(null) }
    var notificationsEnabled by remember { mutableStateOf(false) }
    var passwordEditorOpen by remember { mutableStateOf(false) }
    var newPassword by remember { mutableStateOf("") }
    var repeatedNewPassword by remember { mutableStateOf("") }
    var accountMessage by remember { mutableStateOf<String?>(null) }
    var accountDeleteDialogOpen by remember { mutableStateOf(false) }
    var accountDeleting by remember { mutableStateOf(false) }
    var trips by remember { mutableStateOf<List<TripCard>>(emptyList()) }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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
            }.onSuccess {
                profileAvatarUrl = it
                accountMessage = localized(language, "Фото профиля обновлено", "Profile photo updated", "Foto de perfil actualizada", "Profilbild aktualisiert")
            }.onFailure {
                accountMessage = it.message ?: localized(language, "Не удалось загрузить фото", "Could not upload photo", "No se pudo cargar la foto", "Foto konnte nicht hochgeladen werden")
            }
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
        trips = runCatching { SupabaseTripRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadTrips() }.getOrDefault(emptyList())
    }

    Box(modifier = Modifier.fillMaxSize().background(if (darkTheme) OdysseyDarkBackground else OdysseyBackground)) {
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
            onDismiss = onBack,
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
                    }.onFailure {
                        accountMessage = it.message ?: localized(language, "Не удалось сохранить язык", "Could not save language", "No se pudo guardar el idioma", "Sprache konnte nicht gespeichert werden")
                    }
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
                    }.onFailure {
                        accountMessage = it.message ?: localized(language, "Не удалось сохранить тему", "Could not save theme", "No se pudo guardar el tema", "Thema konnte nicht gespeichert werden")
                    }
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
                            .onSuccess {
                                newPassword = ""
                                repeatedNewPassword = ""
                                passwordEditorOpen = false
                                accountMessage = localized(language, "Пароль обновлён", "Password updated", "Contraseña actualizada", "Passwort aktualisiert")
                            }
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
                    onLogout()
                }
            },
        )
    }

    if (accountDeleteDialogOpen) {
        AlertDialog(
            onDismissRequest = { if (!accountDeleting) accountDeleteDialogOpen = false },
            title = { Text(localized("Удалить аккаунт?", "Delete account?", "¿Eliminar cuenta?", "Konto löschen?"), fontFamily = Manrope, fontWeight = FontWeight.W800) },
            text = {
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
                                onLogout()
                            }.onFailure {
                                accountMessage = it.message ?: localized(language, "Не удалось удалить аккаунт", "Could not delete account", "No se pudo eliminar la cuenta", "Konto konnte nicht gelöscht werden")
                            }
                            accountDeleting = false
                        }
                    },
                ) {
                    Text(if (accountDeleting) localized("Удаляем…", "Deleting…", "Eliminando…", "Wird gelöscht…") else localized("Удалить", "Delete", "Eliminar", "Löschen"), color = Color(0xFFE85B56), fontFamily = Manrope, fontWeight = FontWeight.W800)
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AccountSettingsSheet(
    profileEmail: String,
    profileAvatarUrl: String?,
    trips: List<TripCard>,
    language: String,
    darkTheme: Boolean,
    passwordEditorOpen: Boolean,
    newPassword: String,
    repeatedNewPassword: String,
    accountMessage: String?,
    onDismiss: () -> Unit,
    onPhotoPick: () -> Unit,
    onLanguageChange: (String) -> Unit,
    onThemeToggle: () -> Unit,
    onPasswordEditorToggle: () -> Unit,
    onNewPasswordChange: (String) -> Unit,
    onRepeatedPasswordChange: (String) -> Unit,
    onSavePassword: () -> Unit,
    onDeleteAccount: () -> Unit,
    onSignOut: () -> Unit,
) {
    var languagePickerOpen by remember(language) { mutableStateOf(false) }
    val displayName = profileEmail.substringBefore("@").ifBlank { "Ramingo" }
    val languageName = when (normalizeLanguage(language)) {
        "EN" -> "English"
        "ES" -> "Español"
        "DE" -> "Deutsch"
        else -> "Русский"
    }
    val cityCount = remember(trips) {
        trips
            .flatMap { splitStoredCityList(it.cities) }
            .filter(String::isNotBlank)
            .distinctBy(::cityFilterKey)
            .size
    }
    val sheetBackground = if (darkTheme) OdysseyDarkSurface else Color(0xFFF7F5FF)
    val dividerColor = if (darkTheme) OdysseyDarkBorder else contentBorderColor()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = sheetBackground,
        tonalElevation = 0.dp,
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .width(43.dp)
                    .height(5.dp)
                    .clip(RoundedCornerShape(999.dp))
                    .background(if (darkTheme) OdysseyDarkMuted else Color(0xFF9996A5)),
            )
        },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .imePadding()
                .verticalScroll(rememberScrollState())
                .padding(start = 24.dp, top = 0.dp, end = 24.dp, bottom = 23.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                if (profileAvatarUrl != null) {
                    AsyncImage(
                        model = profileAvatarUrl,
                        contentDescription = localized("Фото профиля", "Profile photo", "Foto de perfil", "Profilbild"),
                        contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                        modifier = Modifier.size(54.dp).clip(RoundedCornerShape(17.dp)),
                    )
                } else {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(17.dp))
                            .background(Brush.linearGradient(listOf(primaryColor(), Color(0xFF9588F0)))),
                    ) {
                        Text(displayName.firstOrNull()?.uppercaseChar()?.toString() ?: "R", color = Color.White, fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 23.sp)
                    }
                }
                Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                    Text(displayName, color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 19.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(localized("Личный кабинет · Ramingo", "Personal account · Ramingo", "Cuenta personal · Ramingo", "Persönliches Konto · Ramingo"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 11.sp, maxLines = 1)
                }
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(if (darkTheme) OdysseyDarkTint else Color(0xFFEEEDF4))
                        .clickable(onClick = onDismiss),
                ) {
                    Icon(Icons.Filled.Close, contentDescription = localized("Закрыть", "Close", "Cerrar", "Schließen"), tint = secondaryTextColor(), modifier = Modifier.size(20.dp))
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(9.dp), modifier = Modifier.fillMaxWidth().padding(top = 18.dp)) {
                AccountStat(value = trips.size.toString(), label = localized("поездки", "trips", "viajes", "Reisen"), modifier = Modifier.weight(1f))
                AccountStat(
                    value = cityCount.toString(),
                    label = localizedCountWord(cityCount, language, "город", "города", "городов", "city", "cities", "ciudad", "ciudades", "Stadt", "Städte"),
                    modifier = Modifier.weight(1f),
                )
                AccountStat(value = "—", label = localized("км", "km", "km", "km"), modifier = Modifier.weight(1f))
            }

            Text(localized("НАСТРОЙКИ АККАУНТА", "ACCOUNT SETTINGS", "AJUSTES DE LA CUENTA", "KONTOEINSTELLUNGEN"), color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 10.sp, letterSpacing = 1.sp, modifier = Modifier.padding(top = 20.dp, bottom = 9.dp))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(15.dp))
                    .border(1.dp, dividerColor, RoundedCornerShape(15.dp))
                    .background(cardSurfaceColor()),
            ) {
                AccountMenuItem(Icons.Outlined.Language, localized("Языки", "Languages", "Idiomas", "Sprachen"), trailing = languageName) { languagePickerOpen = !languagePickerOpen }
                if (languagePickerOpen) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(5.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 13.dp, end = 13.dp, bottom = 10.dp)
                            .height(42.dp)
                            .clip(RoundedCornerShape(11.dp))
                            .background(secondarySurfaceColor())
                            .padding(4.dp),
                    ) {
                        listOf("RU", "EN", "ES", "DE").forEach { code ->
                            val selected = normalizeLanguage(language) == code
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (selected) primaryColor() else Color.Transparent)
                                    .clickable {
                                        languagePickerOpen = false
                                        onLanguageChange(code)
                                    },
                            ) {
                                Text(code, color = if (selected) primaryContentColor() else secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 12.sp)
                            }
                        }
                    }
                }
                AccountSettingsDivider(dividerColor)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().height(50.dp).padding(horizontal = 13.dp)) {
                    AccountIconTile(Icons.Outlined.DarkMode)
                    Text(localized("Тёмная тема", "Dark theme", "Tema oscuro", "Dunkles Thema"), color = contentTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
                    Box(
                        modifier = Modifier
                            .width(40.dp)
                            .height(24.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(if (darkTheme) primaryColor() else Color(0xFFE4E1EB))
                            .clickable(onClick = onThemeToggle),
                    ) {
                        Box(
                            modifier = Modifier
                                .align(if (darkTheme) Alignment.CenterEnd else Alignment.CenterStart)
                                .padding(3.dp)
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(Color.White)
                                .border(1.dp, if (darkTheme) OdysseyDarkBorder else Color(0xFFD6D2DE), CircleShape),
                        )
                    }
                }
                AccountSettingsDivider(dividerColor)
                AccountMenuItem(Icons.Outlined.Lock, localized("Сменить пароль", "Change password", "Cambiar contraseña", "Passwort ändern")) { onPasswordEditorToggle() }
                if (passwordEditorOpen) {
                    Column(modifier = Modifier.padding(start = 13.dp, top = 0.dp, end = 13.dp, bottom = 12.dp)) {
                        AuthField(localized("Новый пароль", "New password", "Nueva contraseña", "Neues Passwort"), "••••••••", newPassword, password = true, onValueChange = onNewPasswordChange)
                        Spacer(Modifier.height(8.dp))
                        AuthField(localized("Повторите пароль", "Repeat password", "Repita la contraseña", "Passwort wiederholen"), "••••••••", repeatedNewPassword, password = true, onValueChange = onRepeatedPasswordChange)
                        Button(onClick = onSavePassword, colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()), shape = RoundedCornerShape(10.dp), modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
                            Text(localized("Сохранить пароль", "Save password", "Guardar contraseña", "Passwort speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 13.sp)
                        }
                    }
                }
                AccountSettingsDivider(dividerColor)
                AccountMenuItem(Icons.Outlined.Image, localized("Сменить фото", "Change photo", "Cambiar foto", "Foto ändern")) { onPhotoPick() }
                AccountSettingsDivider(dividerColor)
                AccountMenuItem(Icons.Outlined.DeleteForever, localized("Удалить аккаунт", "Delete account", "Eliminar cuenta", "Konto löschen"), Color(0xFFE85B56)) { onDeleteAccount() }
            }
            accountMessage?.let {
                Text(it, color = if (it.contains("Не удалось") || it.contains("не совпадают") || it.contains("минимум")) Color(0xFFE85B56) else Color(0xFF249D72), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 11.sp, modifier = Modifier.padding(top = 8.dp))
            }
            Text(localized("Версия приложения · ${BuildConfig.VERSION_NAME}", "App version · ${BuildConfig.VERSION_NAME}", "Versión de la aplicación · ${BuildConfig.VERSION_NAME}", "App-Version · ${BuildConfig.VERSION_NAME}"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W500, fontSize = 10.sp, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 13.dp))
            TextButton(onClick = onSignOut, modifier = Modifier.align(Alignment.CenterHorizontally)) {
                Text(localized("Выйти из аккаунта", "Sign out", "Cerrar sesión", "Abmelden"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp)
            }
        }
    }
}

@Composable
internal fun AccountStat(value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(cardSurfaceColor())
            .border(1.dp, contentBorderColor(), RoundedCornerShape(14.dp))
            .padding(vertical = 11.dp, horizontal = 8.dp),
    ) {
        Text(value, color = primaryColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 20.sp)
        Text(label, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.sp, modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
internal fun AccountIconTile(icon: androidx.compose.ui.graphics.vector.ImageVector, danger: Boolean = false) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(28.dp)
            .clip(RoundedCornerShape(9.dp))
            .background(if (danger) dangerSurfaceColor() else tintedSurfaceColor()),
    ) {
        Icon(icon, contentDescription = null, tint = if (danger) Color(0xFFE85B56) else primaryColor(), modifier = Modifier.size(18.dp))
    }
}

@Composable
internal fun AccountSettingsDivider(color: Color) {
    Spacer(Modifier.fillMaxWidth().height(1.dp).background(color))
}

@Composable
internal fun AccountMenuItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color? = null, trailing: String? = null, onClick: (() -> Unit)? = null) {
    val isDanger = color != null
    val resolvedTextColor = color ?: contentTextColor()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp)
            .let { modifier -> if (onClick != null) modifier.clickable { onClick() } else modifier }
            .padding(horizontal = 13.dp),
    ) {
        AccountIconTile(icon, danger = isDanger)
        Text(label, color = resolvedTextColor, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 14.sp, modifier = Modifier.weight(1f).padding(start = 12.dp))
        if (trailing != null) {
            Text(trailing, color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W600, fontSize = 10.sp, modifier = Modifier.padding(end = 8.dp))
        }
        Text("›", color = color ?: secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W800, fontSize = 18.sp)
    }
}

