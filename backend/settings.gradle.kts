pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "aura-backend"

include(":services:auth-service")
include(":services:user-service")
include(":services:product-service")
include(":services:recommendation-service")
include(":services:tracker-service")