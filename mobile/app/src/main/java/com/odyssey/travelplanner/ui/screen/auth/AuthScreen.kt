package com.odyssey.travelplanner.ui.screen.auth

import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.NoCredentialException
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.odyssey.travelplanner.R
import com.odyssey.travelplanner.BuildConfig
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.RememberedAccount
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.providers.Google
import kotlinx.coroutines.launch
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.normalizeLanguage
import com.odyssey.travelplanner.ui.screen.trip.sights.isAlreadyRegisteredAuthError
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.Manrope
import com.odyssey.travelplanner.ui.theme.OdysseyBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBorder
import com.odyssey.travelplanner.ui.theme.OdysseyDarkText
import com.odyssey.travelplanner.ui.theme.cardSurfaceColor
import com.odyssey.travelplanner.ui.theme.contentBorderColor
import com.odyssey.travelplanner.ui.theme.contentTextColor
import com.odyssey.travelplanner.ui.theme.primaryColor
import com.odyssey.travelplanner.ui.theme.primaryContentColor
import com.odyssey.travelplanner.ui.theme.secondaryTextColor
import com.odyssey.travelplanner.ui.theme.tintedSurfaceColor

@Composable
internal fun AuthScreen(
    rememberCredentials: Boolean,
    rememberedAccounts: List<RememberedAccount>,
    onRememberCredentialsChange: (Boolean) -> Unit,
    onRememberedAccountSelected: suspend (RememberedAccount) -> Boolean = { false },
    onRememberedAccountRemoved: suspend (RememberedAccount) -> Unit = {},
    onLanguageChange: (String) -> Unit,
    onAuthenticated: () -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    val context = LocalContext.current
    val language = LocalLanguage.current
    var isRegistration by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var repeatPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var message by remember { mutableStateOf<String?>(null) }
    var languagePickerOpen by remember { mutableStateOf(false) }
    // Legacy saved accounts are shown only long enough to migrate them to the
    // single persistent auth session used by the current app. New logins do
    // not create account-picker entries.
    var accountPickerOpen by remember(rememberedAccounts) {
        mutableStateOf(rememberedAccounts.isNotEmpty())
    }
    val scope = rememberCoroutineScope()
    val languageCode = normalizeLanguage(language)
    val languageOptions = listOf(
        Triple("RU", "\uD83C\uDDF7\uD83C\uDDFA", "\u0420\u0443\u0441\u0441\u043A\u0438\u0439"),
        Triple("EN", "\uD83C\uDDEC\uD83C\uDDE7", "English"),
        Triple("ES", "\uD83C\uDDEA\uD83C\uDDF8", "Espa\u00F1ol"),
        Triple("DE", "\uD83C\uDDE9\uD83C\uDDEA", "Deutsch"),
    )
    fun messageText(ru: String, en: String, es: String, de: String) = localized(language, ru, en, es, de)

    LaunchedEffect(Unit) {
        SupabaseProvider.loadRememberedCredentials()?.let { credentials ->
            if (email.isBlank() && password.isBlank()) {
                email = credentials.email
                password = credentials.password
                onRememberCredentialsChange(true)
            }
        }
    }

    fun setRememberCredentials(checked: Boolean) {
        onRememberCredentialsChange(checked)
        if (!checked) {
            scope.launch { SupabaseProvider.clearRememberedCredentials() }
        }
    }

    fun submit() {
        if (email.isBlank() || password.isBlank() || (isRegistration && name.isBlank())) {
            message = messageText("Заполните обязательные поля", "Complete the required fields", "Complete los campos obligatorios", "Füllen Sie die Pflichtfelder aus")
            return
        }
        if (isRegistration && password != repeatPassword) {
            message = messageText("Пароли не совпадают", "Passwords do not match", "Las contraseñas no coinciden", "Passwörter stimmen nicht überein")
            return
        }
        if (isRegistration && password.length < 6) {
            message = messageText("Пароль должен содержать минимум 6 символов", "Password must contain at least 6 characters", "La contraseña debe tener al menos 6 caracteres", "Das Passwort muss mindestens 6 Zeichen enthalten")
            return
        }
        scope.launch {
            isLoading = true
            message = null
            runCatching {
                SupabaseProvider.prepareAuthentication()
                val auth = SupabaseProvider.clientForCurrentAuthFlow().auth
                if (isRegistration) {
                    auth.signUpWith(Email, "https://ramingo.online/mobile/auth") {
                        this.email = email.trim()
                        this.password = password
                        data = buildJsonObject { put("full_name", name.trim()) }
                    }
                    val authenticatedImmediately = auth.currentSessionOrNull() != null
                    if (authenticatedImmediately) {
                        SupabaseProvider.finalizeAuthentication()
                    }
                    SupabaseProvider.updateRememberedCredentials(email.trim(), password, rememberCredentials)
                    authenticatedImmediately
                } else {
                    auth.signInWith(Email) {
                        this.email = email.trim()
                        this.password = password
                    }
                    SupabaseProvider.finalizeAuthentication()
                    SupabaseProvider.updateRememberedCredentials(email.trim(), password, rememberCredentials)
                    true
                }
            }.onSuccess { authenticatedImmediately ->
                if (isRegistration && !authenticatedImmediately) {
                    message = messageText("Проверьте e-mail для подтверждения", "Check your email to confirm", "Revise su correo para confirmar", "Prüfen Sie Ihre E-Mail zur Bestätigung")
                } else {
                    onAuthenticated()
                }
            }.onFailure { error ->
                message = if (isRegistration && isAlreadyRegisteredAuthError(error)) {
                    messageText("Этот e-mail уже зарегистрирован. Войдите в аккаунт.", "This e-mail is already registered. Sign in instead.", "Este e-mail ya está registrado. Inicie sesión.", "Diese E-Mail ist bereits registriert. Melden Sie sich stattdessen an.")
                } else {
                    messageText("Не удалось выполнить запрос", "Could not complete the request", "No se pudo completar la solicitud", "Anfrage konnte nicht ausgeführt werden")
                }
            }
            isLoading = false
        }
    }

    fun signInWithGoogle() {
        if (BuildConfig.GOOGLE_WEB_CLIENT_ID.isBlank()) {
            message = messageText("Google OAuth не настроен", "Google OAuth is not configured", "Google OAuth no está configurado", "Google OAuth ist nicht eingerichtet")
            return
        }
        scope.launch {
            isLoading = true
            message = null
            runCatching {
                SupabaseProvider.prepareAuthentication()
                val option = GetSignInWithGoogleOption.Builder(
                    BuildConfig.GOOGLE_WEB_CLIENT_ID,
                ).build()
                val request = GetCredentialRequest.Builder().addCredentialOption(option).build()
                val credential = CredentialManager.create(context).getCredential(context, request).credential
                val googleCredential = GoogleIdTokenCredential.createFrom(credential.data)
                SupabaseProvider.clientForCurrentAuthFlow().auth.signInWith(IDToken) {
                    idToken = googleCredential.idToken
                    provider = Google
                }
                SupabaseProvider.finalizeAuthentication()
            }.onSuccess { onAuthenticated() }.onFailure { error ->
                message = if (error is NoCredentialException) {
                    messageText("Аккаунт Google не выбран", "No Google account was selected", "No se seleccionó ninguna cuenta de Google", "Kein Google-Konto ausgewählt")
                } else {
                    messageText("Не удалось войти через Google", "Google sign-in failed", "No se pudo iniciar sesión con Google", "Google-Anmeldung fehlgeschlagen")
                }
            }
            isLoading = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(if (darkTheme) OdysseyDarkBackground else OdysseyBackground)
            .padding(WindowInsets.statusBars.asPaddingValues())
            .verticalScroll(rememberScrollState())
            .imePadding()
            .padding(start = 24.dp, top = 40.dp, end = 24.dp, bottom = 28.dp + WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()),
    ) {
        Box(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(end = 84.dp),
            ) {
                Text(
                    text = "R",
                    color = primaryContentColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 19.sp,
                    modifier = Modifier
                        .background(
                            Brush.linearGradient(listOf(primaryColor(), Color(0xFF8E7BF5))),
                            RoundedCornerShape(12.dp),
                        )
                        .padding(horizontal = 12.dp, vertical = 7.dp),
                )
                Text(
                    text = "Ramingo",
                    color = contentTextColor(),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                    fontSize = 20.sp,
                    modifier = Modifier.padding(start = 11.dp),
                )
            }
            Box(modifier = Modifier.align(Alignment.TopEnd)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(tintedSurfaceColor())
                        .border(1.dp, contentBorderColor(), RoundedCornerShape(10.dp))
                        .clickable { languagePickerOpen = true }
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = localized("\u042f\u0437\u044b\u043a", "Language", "Idioma", "Sprache"),
                        tint = primaryColor(),
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = languageCode,
                        color = contentTextColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                        fontSize = 12.sp,
                    )
                    Icon(
                        imageVector = Icons.Outlined.KeyboardArrowDown,
                        contentDescription = null,
                        tint = secondaryTextColor(),
                        modifier = Modifier.size(16.dp),
                    )
                }
                DropdownMenu(
                    expanded = languagePickerOpen,
                    onDismissRequest = { languagePickerOpen = false },
                    modifier = Modifier.width(178.dp),
                    shape = RoundedCornerShape(14.dp),
                    containerColor = cardSurfaceColor(),
                    shadowElevation = 12.dp,
                ) {
                    languageOptions.forEach { (code, flag, label) ->
                        val selected = languageCode == code
                        DropdownMenuItem(
                            text = {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(9.dp),
                                ) {
                                    Text(flag, fontSize = 17.sp)
                                    Text(
                                        text = label,
                                        color = if (selected) primaryColor() else contentTextColor(),
                                        fontFamily = Manrope,
                                        fontWeight = if (selected) FontWeight.W800 else FontWeight.W700,
                                        fontSize = 13.sp,
                                    )
                                }
                            },
                            onClick = {
                                languagePickerOpen = false
                                onLanguageChange(code)
                            },
                            trailingIcon = {
                                if (selected) {
                                    Icon(
                                        imageVector = Icons.Filled.Check,
                                        contentDescription = null,
                                        tint = primaryColor(),
                                        modifier = Modifier.size(17.dp),
                                    )
                                }
                            },
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(34.dp))
        Text(
            text = if (isRegistration) localized("Создать аккаунт", "Create account", "Crear cuenta", "Konto erstellen") else localized("С возвращением", "Welcome back", "Bienvenido de nuevo", "Willkommen zurück"),
            color = contentTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 30.sp,
            lineHeight = 32.sp,
        )
        Text(
            text = if (isRegistration) localized("Пара шагов - и планируем поездку", "A few steps and you can plan your trip", "Unos pasos y podrá planificar su viaje", "Noch ein paar Schritte bis zur Reiseplanung") else localized("Войдите, чтобы продолжить планирование", "Sign in to continue planning", "Inicie sesión para continuar planificando", "Melden Sie sich an, um weiterzuplanen"),
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 14.sp,
            modifier = Modifier.padding(top = 8.dp),
        )

        val showRememberedAccounts = !isRegistration && accountPickerOpen && rememberedAccounts.isNotEmpty()
        if (showRememberedAccounts) {
            RememberedAccountsPanel(
                accounts = rememberedAccounts,
                isLoading = isLoading,
                onSelect = { account ->
                    scope.launch {
                        isLoading = true
                        message = null
                        val activated = runCatching {
                            onRememberedAccountSelected(account)
                        }.getOrDefault(false)
                        if (activated) {
                            onAuthenticated()
                        } else {
                            onRememberedAccountRemoved(account)
                            message = messageText(
                                "Не удалось войти под этим аккаунтом. Войдите снова.",
                                "Could not use this account. Sign in again.",
                                "No se pudo usar esta cuenta. Inicie sesión de nuevo.",
                                "Dieses Konto konnte nicht verwendet werden. Melden Sie sich erneut an.",
                            )
                            if (rememberedAccounts.size <= 1) accountPickerOpen = false
                        }
                        isLoading = false
                    }
                },
                onRemove = { account ->
                    scope.launch {
                        onRememberedAccountRemoved(account)
                        if (rememberedAccounts.size <= 1) accountPickerOpen = false
                    }
                },
                onOtherAccount = {
                    accountPickerOpen = false
                    message = null
                },
            )
        } else {
            Spacer(Modifier.height(28.dp))
        if (isRegistration) {
            AuthField(localized("Имя", "Name", "Nombre", "Name"), localized("Как вас зовут", "What is your name", "Cómo se llama", "Wie heißen Sie"), name) { name = it }
            Spacer(Modifier.height(14.dp))
        }
        AuthField(localized("E-mail", "E-mail", "Correo electrónico", "E-Mail"), "you@example.com", email) { email = it }
        Spacer(Modifier.height(14.dp))
        AuthField(localized("Пароль", "Password", "Contraseña", "Passwort"), "••••••••", password, password = true) { password = it }
        if (isRegistration) {
            Spacer(Modifier.height(14.dp))
            AuthField(localized("Повторите пароль", "Repeat password", "Repita la contraseña", "Passwort wiederholen"), "••••••••", repeatPassword, password = true) { repeatPassword = it }
        } else {
            Text(
                text = localized("Забыли пароль?", "Forgot password?", "¿Olvidó su contraseña?", "Passwort vergessen?"),
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 13.sp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clickable {
                        if (email.isBlank()) {
                            message = messageText("Введите e-mail для восстановления", "Enter your email to reset the password", "Introduzca su e-mail para restablecer la contraseña", "Geben Sie Ihre E-Mail zum Zurücksetzen des Passworts ein")
                        } else {
                            scope.launch {
                                isLoading = true
                                runCatching {
                                    SupabaseProvider.clientForCurrentAuthFlow().auth.resetPasswordForEmail(email.trim(), redirectUrl = "https://ramingo.online/mobile/reset")
                                }.onSuccess {
                                    message = messageText("Письмо для восстановления отправлено", "Password reset email sent", "Correo de restablecimiento enviado", "E-Mail zum Zurücksetzen gesendet")
                                }.onFailure {
                                    message = it.message ?: messageText("Не удалось отправить письмо", "Could not send reset email", "No se pudo enviar el correo", "E-Mail konnte nicht gesendet werden")
                                }
                                isLoading = false
                            }
                        }
                    },
            )
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(top = 14.dp).clickable { setRememberCredentials(!rememberCredentials) },
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(20.dp).background(if (rememberCredentials) primaryColor() else Color.Transparent, RoundedCornerShape(6.dp)).drawBehind {
                    if (!rememberCredentials) drawRoundRect(Color(0xFFBDBCC6), style = Stroke(width = 1.dp.toPx()), cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()))
                },
            ) {
                if (rememberCredentials) Text("✓", color = primaryContentColor(), fontWeight = FontWeight.W800, fontSize = 13.sp)
            }
            Text(
                text = localized("Запомнить данные входа", "Remember login details", "Recordar datos de acceso", "Anmeldedaten speichern"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 13.sp,
                modifier = Modifier.padding(start = 9.dp),
            )
        }

        Button(
            onClick = ::submit,
            enabled = !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = primaryContentColor()),
            shape = RoundedCornerShape(15.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(56.dp)
                .background(
                    Brush.linearGradient(listOf(primaryColor(), Color(0xFF7D6CF0))),
                    RoundedCornerShape(15.dp),
                ),
        ) {
            Text(
                text = if (isLoading) localized("Подождите…", "Please wait…", "Espere…", "Bitte warten…") else if (isRegistration) localized("Создать аккаунт", "Create account", "Crear cuenta", "Konto erstellen") else localized("Войти", "Sign in", "Iniciar sesión", "Anmelden"),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 16.sp,
            )
        }
        if (message != null) {
            Text(
                text = message!!,
                color = if (message == "Вход выполнен" || message?.startsWith("Проверьте") == true || message?.contains("отправлено", true) == true || message?.contains("sent", true) == true || message?.contains("enviado", true) == true || message?.contains("gesendet", true) == true) Color(0xFF22B07D) else Color(0xFFE0524B),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 13.sp,
                modifier = Modifier.padding(top = 10.dp),
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 22.dp),
        ) {
            Spacer(Modifier.weight(1f).height(1.dp).background(contentBorderColor()))
            Text(
                text = localized("или", "or", "o", "oder"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f).height(1.dp).background(contentBorderColor()))
        }
        Button(
            onClick = ::signInWithGoogle,
            shape = RoundedCornerShape(15.dp),
            colors = ButtonDefaults.buttonColors(containerColor = cardSurfaceColor(), contentColor = contentTextColor()),
            modifier = Modifier.fillMaxWidth().height(53.dp),
        ) {
            Text("G", color = Color(0xFF4285F4), fontWeight = FontWeight.W800, fontSize = 18.sp)
            Text(
                text = localized("Продолжить с Google", "Continue with Google", "Continuar con Google", "Mit Google fortfahren"),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 15.sp,
                modifier = Modifier.padding(start = 10.dp),
            )
        }
        Row(
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().padding(top = 26.dp),
        ) {
            Text(
                text = if (isRegistration) localized("Уже есть аккаунт?", "Already have an account?", "¿Ya tiene una cuenta?", "Bereits ein Konto?") else localized("Нет аккаунта?", "No account?", "¿No tiene cuenta?", "Noch kein Konto?"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W600,
                fontSize = 14.sp,
            )
            Text(
                text = if (isRegistration) " " + localized("Войти", "Sign in", "Iniciar sesión", "Anmelden") else " " + localized("Зарегистрироваться", "Sign up", "Registrarse", "Registrieren"),
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                modifier = Modifier.clickable {
                    isRegistration = !isRegistration
                    name = ""
                    password = ""
                    repeatPassword = ""
                    message = null
                },
            )
        }
        }
    }
}

@Composable
internal fun RememberedAccountsPanel(
    accounts: List<RememberedAccount>,
    isLoading: Boolean,
    onSelect: (RememberedAccount) -> Unit,
    onRemove: suspend (RememberedAccount) -> Unit,
    onOtherAccount: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var selectedId by remember(accounts) { mutableStateOf(accounts.firstOrNull()?.id) }
    var menuAccount by remember { mutableStateOf<RememberedAccount?>(null) }
    var removeAccount by remember { mutableStateOf<RememberedAccount?>(null) }
    val selectedAccount = accounts.firstOrNull { it.id == selectedId } ?: accounts.firstOrNull()

    Column(modifier = Modifier.padding(top = 27.dp)) {
        Text(
            text = localized("СОХРАНЁННЫЕ АККАУНТЫ", "SAVED ACCOUNTS", "CUENTAS GUARDADAS", "GESPEICHERTE KONTEN"),
            color = primaryColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W800,
            fontSize = 11.sp,
            letterSpacing = 1.2.sp,
        )
        Text(
            text = localized(
                "Выберите аккаунт, под которым хотите продолжить.",
                "Choose the account you want to continue with.",
                "Elija la cuenta con la que desea continuar.",
                "Wählen Sie das Konto, mit dem Sie fortfahren möchten.",
            ),
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 13.sp,
            lineHeight = 19.sp,
            modifier = Modifier.padding(top = 8.dp, bottom = 17.dp),
        )

        Column(verticalArrangement = Arrangement.spacedBy(11.dp)) {
            accounts.forEachIndexed { index, account ->
                val selected = selectedAccount?.id == account.id
                val cardShape = RoundedCornerShape(14.dp)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(cardShape)
                        .background(if (selected) tintedSurfaceColor() else cardSurfaceColor())
                        .border(
                            width = if (selected) 1.5.dp else 1.dp,
                            color = if (selected) primaryColor() else contentBorderColor(),
                            shape = cardShape,
                        )
                        .clickable {
                            selectedId = account.id
                        }
                        .padding(start = 13.dp, top = 13.dp, bottom = 13.dp, end = 42.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val avatarColors = if (index % 2 == 0) {
                            listOf(Color(0xFFF6CDA5), Color(0xFFE88E77))
                        } else {
                            listOf(Color(0xFFACCBFF), Color(0xFF7384E9))
                        }
                        Text(
                            text = account.displayName.trim().take(1).uppercase().ifBlank { "R" },
                            color = Color.White,
                            fontFamily = Manrope,
                            fontWeight = FontWeight.W800,
                            fontSize = 17.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .size(42.dp)
                                .background(Brush.linearGradient(avatarColors), RoundedCornerShape(12.dp))
                                .padding(top = 10.dp),
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(start = 12.dp),
                        ) {
                            Text(
                                text = account.displayName,
                                color = contentTextColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W800,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = account.email,
                                color = secondaryTextColor(),
                                fontFamily = Manrope,
                                fontWeight = FontWeight.W600,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 3.dp),
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(top = 5.dp),
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .background(Color(0xFF22B07D), CircleShape),
                                )
                                Text(
                                    text = localized(
                                        "Сохранён на этом устройстве",
                                        "Saved on this device",
                                        "Guardado en este dispositivo",
                                        "Auf diesem Gerät gespeichert",
                                    ),
                                    color = Color(0xFF22B07D),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W800,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(start = 5.dp),
                                )
                            }
                        }
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(20.dp)
                                .background(if (selected) primaryColor() else Color.Transparent, RoundedCornerShape(6.dp))
                                .drawBehind {
                                    if (!selected) {
                                        drawRoundRect(
                                            Color(0xFFBDBCC6),
                                            style = Stroke(width = 1.dp.toPx()),
                                            cornerRadius = androidx.compose.ui.geometry.CornerRadius(6.dp.toPx()),
                                        )
                                    }
                                },
                        ) {
                            if (selected) {
                                Text("✓", color = primaryContentColor(), fontWeight = FontWeight.W800, fontSize = 13.sp)
                            }
                        }
                    }

                    IconButton(
                        onClick = { menuAccount = account },
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(34.dp)
                            .padding(top = 2.dp, end = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MoreVert,
                            contentDescription = localized("Настройки аккаунта", "Account options", "Opciones de cuenta", "Kontenoptionen"),
                            tint = Color(0xFFB1B1BB),
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    DropdownMenu(
                        expanded = menuAccount?.id == account.id,
                        onDismissRequest = { menuAccount = null },
                        shape = RoundedCornerShape(14.dp),
                        containerColor = cardSurfaceColor(),
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text(
                                    text = localized("Удалить с устройства", "Remove from device", "Eliminar del dispositivo", "Vom Gerät entfernen"),
                                    color = contentTextColor(),
                                    fontFamily = Manrope,
                                    fontWeight = FontWeight.W700,
                                    fontSize = 13.sp,
                                )
                            },
                            onClick = {
                                removeAccount = account
                                menuAccount = null
                            },
                        )
                    }
                }
            }
        }

        Button(
            onClick = { selectedAccount?.let(onSelect) },
            enabled = selectedAccount != null && !isLoading,
            colors = ButtonDefaults.buttonColors(containerColor = Color.Transparent, contentColor = primaryContentColor()),
            shape = RoundedCornerShape(15.dp),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 18.dp)
                .height(56.dp)
                .background(
                    Brush.linearGradient(listOf(primaryColor(), Color(0xFF7D6CF0))),
                    RoundedCornerShape(15.dp),
                ),
        ) {
            Text(
                text = if (isLoading) {
                    localized("Подождите…", "Please wait…", "Espere…", "Bitte warten…")
                } else {
                    localized("Продолжить как ${selectedAccount?.displayName.orEmpty()}", "Continue as ${selectedAccount?.displayName.orEmpty()}", "Continuar como ${selectedAccount?.displayName.orEmpty()}", "Fortfahren als ${selectedAccount?.displayName.orEmpty()}")
                },
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 16.sp,
            )
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(vertical = 21.dp),
        ) {
            Spacer(Modifier.weight(1f).height(1.dp).background(contentBorderColor()))
            Text(
                text = localized("или", "or", "o", "oder"),
                color = secondaryTextColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W700,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 12.dp),
            )
            Spacer(Modifier.weight(1f).height(1.dp).background(contentBorderColor()))
        }
        TextButton(
            onClick = onOtherAccount,
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(23.dp)
                    .border(1.dp, Color(0xFFCFCBFA), RoundedCornerShape(8.dp)),
            ) {
                Text("+", color = primaryColor(), fontSize = 17.sp, lineHeight = 17.sp)
            }
            Text(
                text = localized("Войти под другим аккаунтом", "Sign in with another account", "Iniciar sesión con otra cuenta", "Mit einem anderen Konto anmelden"),
                color = primaryColor(),
                fontFamily = Manrope,
                fontWeight = FontWeight.W800,
                fontSize = 14.sp,
                modifier = Modifier.padding(start = 8.dp),
            )
        }
        Text(
            text = localized(
                "Если сохранённых аккаунтов нет, откроется обычная форма входа.",
                "If there are no saved accounts, the standard sign-in form opens.",
                "Si no hay cuentas guardadas, se abrirá el formulario de inicio de sesión.",
                "Wenn keine Konten gespeichert sind, wird das normale Anmeldeformular geöffnet.",
            ),
            color = secondaryTextColor(),
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 10.sp,
            lineHeight = 14.sp,
            modifier = Modifier.padding(top = 24.dp),
        )
    }

    removeAccount?.let { account ->
        AlertDialog(
            onDismissRequest = { removeAccount = null },
            title = {
                Text(
                    text = localized("Удалить аккаунт?", "Remove account?", "¿Eliminar la cuenta?", "Konto entfernen?"),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W800,
                )
            },
            text = {
                Text(
                    text = localized(
                        "Аккаунт будет удалён только с этого устройства.",
                        "The account will only be removed from this device.",
                        "La cuenta solo se eliminará de este dispositivo.",
                        "Das Konto wird nur von diesem Gerät entfernt.",
                    ),
                    fontFamily = Manrope,
                    fontWeight = FontWeight.W600,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val target = removeAccount
                        removeAccount = null
                        if (target != null) {
                            scope.launch { onRemove(target) }
                        }
                    },
                ) {
                    Text(
                        text = localized("Удалить", "Remove", "Eliminar", "Entfernen"),
                        color = Color(0xFFE0524B),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { removeAccount = null }) {
                    Text(
                        text = localized("Отмена", "Cancel", "Cancelar", "Abbrechen"),
                        color = primaryColor(),
                        fontFamily = Manrope,
                        fontWeight = FontWeight.W800,
                    )
                }
            },
        )
    }
}

@Composable
internal fun AuthField(
    label: String,
    placeholder: String,
    value: String,
    password: Boolean = false,
    onValueChange: (String) -> Unit,
) {
    val darkTheme = LocalDarkTheme.current
    val surface = cardSurfaceColor()
    val border = if (darkTheme) OdysseyDarkBorder else contentBorderColor()
    val text = contentTextColor()
    var passwordVisible by remember { mutableStateOf(false) }
    Text(
        text = label,
        color = if (darkTheme) OdysseyDarkText else Color(0xFF3A3A42),
        fontFamily = Manrope,
        fontWeight = FontWeight.W800,
        fontSize = 13.sp,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text(placeholder, fontFamily = Manrope, color = secondaryTextColor()) },
        singleLine = true,
        visualTransformation = if (password && !passwordVisible) PasswordVisualTransformation() else androidx.compose.ui.text.input.VisualTransformation.None,
        trailingIcon = if (password) {
            {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                    Icon(
                        imageVector = if (passwordVisible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (passwordVisible) {
                            localized("Скрыть пароль", "Hide password", "Ocultar contraseña", "Passwort ausblenden")
                        } else {
                            localized("Показать пароль", "Show password", "Mostrar contraseña", "Passwort anzeigen")
                        },
                        tint = secondaryTextColor(),
                    )
                }
            }
        } else null,
        shape = RoundedCornerShape(14.dp),
        textStyle = androidx.compose.ui.text.TextStyle(
            color = text,
            fontFamily = Manrope,
            fontWeight = FontWeight.W600,
            fontSize = 15.sp,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = primaryColor(),
            unfocusedBorderColor = border,
            focusedContainerColor = surface,
            unfocusedContainerColor = surface,
            cursorColor = primaryColor(),
        ),
        modifier = Modifier.fillMaxWidth(),
    )
}

