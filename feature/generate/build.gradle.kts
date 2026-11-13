plugins {
    alias(libs.plugins.notedeck.android.feature)
}

android {
    namespace = "dev.notedeck.feature.generate"
}

dependencies {
    // The Photo Picker's activity-result contract.
    implementation(libs.androidx.activity.compose)
}
