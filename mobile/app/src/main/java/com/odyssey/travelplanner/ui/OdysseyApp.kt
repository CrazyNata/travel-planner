package com.odyssey.travelplanner.ui

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.odyssey.travelplanner.data.SupabaseProvider
import com.odyssey.travelplanner.data.RememberedAccount
import com.odyssey.travelplanner.data.AuthRestoreResult
import com.odyssey.travelplanner.data.AccountRepository
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import com.odyssey.travelplanner.ui.common.RamingoSplash
import com.odyssey.travelplanner.ui.i18n.localized
import com.odyssey.travelplanner.ui.i18n.normalizeLanguage
import com.odyssey.travelplanner.ui.screen.account.AccountSettingsScreen
import com.odyssey.travelplanner.ui.screen.auth.AuthScreen
import com.odyssey.travelplanner.ui.screen.auth.ResetPasswordScreen
import com.odyssey.travelplanner.ui.screen.createtrip.CreateTripScreen
import com.odyssey.travelplanner.ui.screen.trip.TripOverviewScreen
import com.odyssey.travelplanner.ui.screen.trips.MyTripsScreen
import com.odyssey.travelplanner.ui.theme.LocalDarkTheme
import com.odyssey.travelplanner.ui.theme.LocalLanguage
import com.odyssey.travelplanner.ui.theme.OdysseyBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDarkBackground
import com.odyssey.travelplanner.ui.theme.OdysseyDarkColors
import com.odyssey.travelplanner.ui.theme.OdysseyLightColors


@Composable
fun OdysseyApp(
    onThemeChanged: (Boolean) -> Unit = {},
    onSplashVisibleChanged: (Boolean) -> Unit = {},
    pendingTripId: String? = null,
    pendingPasswordReset: Boolean = false,
    onPendingDeepLinkHandled: () -> Unit = {},
) {
    val navController = rememberNavController()
    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = currentBackStackEntry?.destination?.route
    var darkTheme by remember { mutableStateOf(false) }
    var language by remember { mutableStateOf("RU") }
    var languageSelectedBeforeAuth by remember { mutableStateOf<String?>(null) }
    var authReady by remember { mutableStateOf(false) }
    var hasSession by remember { mutableStateOf(false) }
    var authRestoreError by remember { mutableStateOf(false) }
    var authRestoreAttempt by remember { mutableStateOf(0) }
    var sessionRestoreVersion by remember { mutableStateOf(0) }
    var rememberCredentials by remember { mutableStateOf(false) }
    var rememberedAccounts by remember { mutableStateOf<List<RememberedAccount>>(emptyList()) }
    val lifecycleOwner = LocalLifecycleOwner.current
    val authScope = rememberCoroutineScope()

    LaunchedEffect(darkTheme, authReady) {
        if (authReady) {
            onSplashVisibleChanged(false)
            onThemeChanged(darkTheme)
        } else {
            onSplashVisibleChanged(true)
        }
    }

    LaunchedEffect(authRestoreAttempt) {
        authReady = false
        authRestoreError = false
        var result = AuthRestoreResult.FAILED
        for (attempt in 0 until 3) {
            result = SupabaseProvider.restorePersistentSession()
            if (result != AuthRestoreResult.FAILED) break
            if (attempt < 2) delay(250)
        }
        when (result) {
            AuthRestoreResult.RESTORED -> {
                hasSession = true
                rememberedAccounts = emptyList()
                runCatching { AccountRepository(SupabaseProvider.clientForCurrentAuthFlow()).loadProfile() }
                    .getOrNull()
                    ?.let { profile ->
                        darkTheme = profile.darkTheme
                        language = normalizeLanguage(profile.language)
                    }
            }
            AuthRestoreResult.NO_SESSION -> {
                hasSession = false
                rememberedAccounts = SupabaseProvider.loadRememberedAccounts()
            }
            AuthRestoreResult.FAILED -> {
                hasSession = false
                authRestoreError = true
            }
        }
        authReady = true
    }

    LaunchedEffect(authReady, hasSession, authRestoreError) {
        if (!authReady || authRestoreError) return@LaunchedEffect
        if (!hasSession) {
            rememberedAccounts = SupabaseProvider.loadRememberedAccounts()
        } else {
            rememberedAccounts = emptyList()
        }
    }

    DisposableEffect(lifecycleOwner, authReady, authRestoreError) {
        if (!authReady || authRestoreError) {
            onDispose { }
        } else {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_RESUME) {
                    authScope.launch {
                        // Re-read the persisted auth session after returning
                        // from background/process recreation. A failed restore
                        // must not turn an explicit logout into a login.
                        val restoreResult = SupabaseProvider.restorePersistentSession()
                        if (restoreResult == AuthRestoreResult.RESTORED) {
                            hasSession = true
                            sessionRestoreVersion += 1
                        }
                    }
                }
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
        }
    }

    LaunchedEffect(authReady, hasSession) {
        if (!authReady) return@LaunchedEffect
        if (!hasSession) return@LaunchedEffect
        val repository = AccountRepository(SupabaseProvider.clientForCurrentAuthFlow())
        runCatching { repository.loadProfile() }.getOrNull()?.let { profile ->
            darkTheme = profile.darkTheme
            val languageChosenBeforeAuth = languageSelectedBeforeAuth
            if (languageChosenBeforeAuth != null) {
                language = languageChosenBeforeAuth
                if (normalizeLanguage(profile.language) != languageChosenBeforeAuth) {
                    runCatching {
                        repository.updateAppearance(languageChosenBeforeAuth, profile.darkTheme)
                    }
                }
                languageSelectedBeforeAuth = null
            } else {
                language = normalizeLanguage(profile.language)
            }
        }
    }

    fun handleLanguageChange(value: String) {
        val normalized = normalizeLanguage(value)
        language = normalized
        if (!hasSession) languageSelectedBeforeAuth = normalized
    }

    LaunchedEffect(authReady, hasSession, currentRoute, authRestoreError) {
        if (!authReady || authRestoreError || currentRoute == null) return@LaunchedEffect
        if (hasSession && currentRoute == "foundation") {
            navController.navigate("trips") { popUpTo("foundation") { inclusive = true } }
        } else if (!hasSession && currentRoute != "foundation") {
            navController.navigate("foundation") { popUpTo(0) { inclusive = true } }
        }
    }

    LaunchedEffect(authReady, authRestoreError) {
        if (!authReady || authRestoreError) return@LaunchedEffect
        val authClients = SupabaseProvider.authClients()
        authClients.forEach { client ->
            launch {
                client.auth.sessionStatus.collect { status ->
                    val isActiveClient = SupabaseProvider.clientForCurrentAuthFlow() === client
                    if (isActiveClient && status is SessionStatus.Authenticated) {
                        hasSession = true
                    }
                    // The auth library can briefly publish NotAuthenticated
                    // while Android pauses/resumes its refresh job. Do not
                    // turn that transient state into a navigation logout;
                    // explicit UI logout handlers are the source of truth.
                }
            }
        }
    }

    val currentTripId = currentBackStackEntry?.arguments?.getString("tripId")

    LaunchedEffect(authReady, hasSession, currentRoute, currentTripId, pendingTripId, pendingPasswordReset) {
        if (!authReady || !hasSession || currentRoute == null) return@LaunchedEffect
        when {
            pendingPasswordReset && currentRoute != "reset-password" -> {
                navController.navigate("reset-password")
                onPendingDeepLinkHandled()
            }
            !pendingTripId.isNullOrBlank() -> {
                if (pendingTripId == currentTripId) {
                    onPendingDeepLinkHandled()
                } else {
                    navController.navigate("trip/$pendingTripId")
                    onPendingDeepLinkHandled()
                }
            }
        }
    }

    CompositionLocalProvider(LocalDarkTheme provides darkTheme, LocalLanguage provides language) {
    MaterialTheme(colorScheme = if (darkTheme) OdysseyDarkColors else OdysseyLightColors) {
        Surface(color = if (darkTheme) OdysseyDarkBackground else OdysseyBackground) {
            if (!authReady) {
                RamingoSplash()
            } else if (authRestoreError) {
                RamingoSplash(
                    message = localized(
                        "Не удалось восстановить вход. Проверьте соединение и попробуйте ещё раз.",
                        "Could not restore your session. Check your connection and try again.",
                        "No se pudo restaurar la sesión. Compruebe la conexión e inténtelo de nuevo.",
                        "Die Sitzung konnte nicht wiederhergestellt werden. Prüfen Sie die Verbindung und versuchen Sie es erneut.",
                    ),
                    onRetry = {
                        authRestoreAttempt += 1
                    },
                )
            } else NavHost(navController = navController, startDestination = if (hasSession) "trips" else "foundation") {
                composable("foundation") {
                    AuthScreen(
                        rememberCredentials = rememberCredentials,
                        rememberedAccounts = rememberedAccounts,
                        onRememberedAccountSelected = { account ->
                            SupabaseProvider.activateRememberedAccount(account.id)
                        },
                        onRememberedAccountRemoved = { account ->
                            SupabaseProvider.removeRememberedAccount(account.id)
                            rememberedAccounts = SupabaseProvider.loadRememberedAccounts()
                        },
                        onRememberCredentialsChange = { rememberCredentials = it },
                        onLanguageChange = ::handleLanguageChange,
                        onAuthenticated = {
                            rememberedAccounts = emptyList()
                            hasSession = true
                            navController.navigate("trips")
                        },
                    )
                }
                composable("trips") {
                    MyTripsScreen(
                        onTripClick = { navController.navigate("trip/$it") },
                        onNewTrip = { navController.navigate("create-trip") },
                        onLogout = {
                            hasSession = false
                            navController.navigate("foundation") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme },
                        onThemeSet = { darkTheme = it },
                        language = language,
                        onLanguageChange = ::handleLanguageChange,
                        sessionRestoreVersion = sessionRestoreVersion,
                    )
                }
                composable("settings") {
                    AccountSettingsScreen(
                        onBack = { navController.popBackStack() },
                        onLogout = {
                            hasSession = false
                            navController.navigate("foundation") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                        darkTheme = darkTheme,
                        onThemeToggle = { darkTheme = !darkTheme },
                        onThemeSet = { darkTheme = it },
                        language = language,
                        onLanguageChange = { language = normalizeLanguage(it) },
                    )
                }
                composable("create-trip") {
                    CreateTripScreen(
                        onBack = { navController.popBackStack() },
                        onCreated = { created ->
                            navController.navigate("trip/${created.id}") {
                                popUpTo("trips") { inclusive = false }
                            }
                        },
                        onAuthRequired = {
                            hasSession = false
                            navController.navigate("foundation") {
                                popUpTo(0) { inclusive = true }
                            }
                        },
                    )
                }
                composable("reset-password") {
                    ResetPasswordScreen(
                        onFinished = {
                            navController.navigate("trips") {
                                popUpTo("reset-password") { inclusive = true }
                            }
                        },
                        onCancel = { navController.popBackStack() },
                    )
                }
                composable("trip/{tripId}") { entry ->
                    TripOverviewScreen(
                        tripId = entry.arguments?.getString("tripId").orEmpty(),
                        onBack = { navController.popBackStack() },
                        onSettings = { navController.navigate("settings") },
                    )
                }
            }
        }
    }
    }
}

