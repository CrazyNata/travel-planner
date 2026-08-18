import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.compose.compiler)
}

val supabaseProperties = Properties().apply {
    val file = rootProject.file("supabase.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val configuredValue: (String) -> String = { name ->
    supabaseProperties.getProperty(name).orEmpty().trim()
        .ifBlank { providers.environmentVariable(name).orNull.orEmpty().trim() }
}

val releaseKeystorePath = providers.environmentVariable("ANDROID_KEYSTORE_PATH").orNull
val releaseKeystorePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").orNull
val releaseKeyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").orNull
val releaseKeyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(
    releaseKeystorePath,
    releaseKeystorePassword,
    releaseKeyAlias,
    releaseKeyPassword,
).all { !it.isNullOrBlank() } && rootProject.file(releaseKeystorePath ?: "").isFile
val releaseTasksRequested = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
if (releaseTasksRequested && !hasReleaseSigning) {
    error("Release signing is not configured. Set ANDROID_KEYSTORE_PATH, ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS, and ANDROID_KEY_PASSWORD.")
}
val requiredReleaseProperties = listOf(
    "SUPABASE_URL",
    "SUPABASE_PUBLISHABLE_KEY",
    "MAPBOX_ACCESS_TOKEN",
    "GOOGLE_WEB_CLIENT_ID",
)
val missingReleaseProperties = requiredReleaseProperties.filter {
    configuredValue(it).isBlank()
}
if (releaseTasksRequested && missingReleaseProperties.isNotEmpty()) {
    error("Release configuration is incomplete. Missing: ${missingReleaseProperties.joinToString()}. Set these values in mobile/supabase.properties or CI secrets.")
}
val versionCodeFromEnvironment = providers.environmentVariable("ANDROID_VERSION_CODE").orNull?.toIntOrNull() ?: 10028
val versionNameFromEnvironment = providers.environmentVariable("ANDROID_VERSION_NAME").orNull ?: "0.2.18"

android {
    namespace = "com.odyssey.travelplanner"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.odyssey.travelplanner"
        minSdk = 26
        targetSdk = 36
        versionCode = versionCodeFromEnvironment
        versionName = versionNameFromEnvironment

        buildConfigField("String", "SUPABASE_URL", "\"${configuredValue("SUPABASE_URL")}\"")
        buildConfigField("String", "SUPABASE_PUBLISHABLE_KEY", "\"${configuredValue("SUPABASE_PUBLISHABLE_KEY")}\"")
        buildConfigField("String", "MAPBOX_ACCESS_TOKEN", "\"${configuredValue("MAPBOX_ACCESS_TOKEN")}\"")
        resValue("string", "mapbox_access_token", configuredValue("MAPBOX_ACCESS_TOKEN"))
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"${configuredValue("GOOGLE_WEB_CLIENT_ID")}\"")
    }

    buildFeatures {
        buildConfig = true
        compose = true
    }

    bundle {
        language {
            enableSplit = false
        }
    }

    signingConfigs {
        create("release") {
            if (hasReleaseSigning) {
                storeFile = rootProject.file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        getByName("debug") {
            applicationIdSuffix = ".debug"
        }
        getByName("release") {
            isDebuggable = false
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
}

dependencies {
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.appcompat)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.json)
    implementation(libs.supabase.auth)
    implementation(libs.supabase.postgrest)
    implementation(libs.supabase.storage)
    implementation(libs.supabase.functions)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services)
    implementation(libs.googleid)
    implementation(libs.mapbox.maps)
    implementation(libs.coil.compose)
    implementation(libs.coil.network.okhttp)
    implementation(libs.androidx.lifecycle.runtime.compose)
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(kotlin("test"))
}
