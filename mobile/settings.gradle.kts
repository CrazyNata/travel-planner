import org.gradle.authentication.http.BasicAuthentication
import java.util.Properties

val localProperties = Properties().apply {
    val file = file("supabase.properties")
    if (file.exists()) file.inputStream().use(::load)
}
val releaseTasksRequested = gradle.startParameter.taskNames.any { it.contains("Release", ignoreCase = true) }
val mapboxDownloadsToken = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN").orEmpty().trim()
    .ifBlank { providers.environmentVariable("MAPBOX_DOWNLOADS_TOKEN").orNull.orEmpty().trim() }
if (releaseTasksRequested && mapboxDownloadsToken.isBlank()) {
    error("MAPBOX_DOWNLOADS_TOKEN is required for release dependency resolution. Set it in mobile/supabase.properties or the CI environment.")
}

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://api.mapbox.com/downloads/v2/releases/maven") {
            authentication { create<BasicAuthentication>("basic") }
            credentials {
                username = "mapbox"
                password = mapboxDownloadsToken
            }
        }
    }
}

rootProject.name = "RamingoAndroid"
include(":app")
