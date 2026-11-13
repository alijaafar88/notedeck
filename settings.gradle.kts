pluginManagement {
    // Convention plugins shared by the modules below.
    includeBuild("build-logic")
    repositories {
        google()
        gradlePluginPortal()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "notedeck"

include(":core:model")
include(":core:domain")
include(":core:data")
include(":core:ui")
include(":core:testing")
include(":feature:decks")
include(":feature:study")
include(":feature:playground")
include(":feature:generate")
include(":feature:assistant")
include(":backend")
include(":cli")
include(":app")
include(":baselineprofile")
