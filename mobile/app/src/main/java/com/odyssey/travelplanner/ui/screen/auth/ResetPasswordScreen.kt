package com.odyssey.travelplanner.ui.screen.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.jan.supabase.auth.auth
import kotlinx.coroutines.launch
import com.odyssey.travelplanner.data.AccountRepository
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDanger
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBackground
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor

@Composable
internal fun ResetPasswordScreen(onFinished: () -> Unit, onCancel: () -> Unit) {
    val language = LocalLanguage.current
    val scope = rememberCoroutineScope()
    var password by remember { mutableStateOf("") }
    var repeatedPassword by remember { mutableStateOf("") }
    var saving by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }

    Column(
        verticalArrangement = Arrangement.Center,
        modifier = Modifier
            .fillMaxSize()
            .background(if (LocalDarkTheme.current) OdysseyDarkBackground else OdysseyBackground)
            .windowInsetsPadding(WindowInsets.statusBars)
            .windowInsetsPadding(WindowInsets.navigationBars)
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
    ) {
        Text(
            localized("Новый пароль", "New password", "Nueva contraseña", "Neues Passwort"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 28.sp,
        )
        Text(
            localized("Задайте новый пароль для аккаунта", "Choose a new password for your account", "Elija una nueva contraseña para su cuenta", "Legen Sie ein neues Passwort für Ihr Konto fest"),
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 22.dp),
        )
        AuthField(localized("Новый пароль", "New password", "Nueva contraseña", "Neues Passwort"), "••••••••", password, password = true) { password = it }
        Spacer(Modifier.height(12.dp))
        AuthField(localized("Повторите пароль", "Repeat password", "Repita la contraseña", "Passwort wiederholen"), "••••••••", repeatedPassword, password = true) { repeatedPassword = it }
        message?.let {
            Text(it, color = OdysseyDanger, fontFamily = Manrope, fontWeight = FontWeight.W700, fontSize = 12.sp, modifier = Modifier.padding(top = 10.dp))
        }
        Button(
            enabled = !saving,
            onClick = {
                when {
                    password.length < 6 -> message = localized(language, "Пароль должен содержать минимум 6 символов", "Password must contain at least 6 characters", "La contraseña debe tener al menos 6 caracteres", "Das Passwort muss mindestens 6 Zeichen enthalten")
                    password != repeatedPassword -> message = localized(language, "Пароли не совпадают", "Passwords do not match", "Las contraseñas no coinciden", "Passwörter stimmen nicht überein")
                    else -> scope.launch {
                        saving = true
                        message = null
                        runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).changePassword(password) }
                            .onSuccess { onFinished() }
                            .onFailure { message = it.message ?: localized(language, "Не удалось сменить пароль", "Could not change password", "No se pudo cambiar la contraseña", "Passwort konnte nicht geändert werden") }
                        saving = false
                    }
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = primaryColor(), contentColor = primaryContentColor()),
            shape = RoundedCornerShape(13.dp),
            modifier = Modifier.fillMaxWidth().height(52.dp).padding(top = 16.dp),
        ) {
            Text(if (saving) localized("Сохраняем…", "Saving…", "Guardando…", "Wird gespeichert…") else localized("Сохранить пароль", "Save password", "Guardar contraseña", "Passwort speichern"), fontFamily = Manrope, fontWeight = FontWeight.W800)
        }
        TextButton(onClick = onCancel, modifier = Modifier.align(Alignment.CenterHorizontally).padding(top = 8.dp)) {
            Text(localized("Отмена", "Cancel", "Cancelar", "Abbrechen"), color = secondaryTextColor(), fontFamily = Manrope, fontWeight = FontWeight.W700)
        }
    }
}

