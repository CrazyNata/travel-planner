import org.gradle.authentication.http.BasicAuthentication
import java.util.Properties

val localProperties = Properties().apply {
    val file = file("supabase.properties")
    if (file.exists()) file.inputStream().use(::load)
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
                password = localProperties.getProperty("MAPBOX_DOWNLOADS_TOKEN", "")
            }
        }
    }
}

rootProject.name = "OdysseyAndroid"
include(":app")
