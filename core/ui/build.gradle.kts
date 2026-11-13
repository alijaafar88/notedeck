plugins {
    alias(libs.plugins.notedeck.android.library)
    alias(libs.plugins.notedeck.android.compose)
}

android {
    namespace = "dev.notedeck.core.ui"
}

dependencies {
    api(project(":core:model"))
}
