plugins {
    alias(libs.plugins.notedeck.android.library)
    alias(libs.plugins.notedeck.android.hilt)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.androidx.room)
}

android {
    namespace = "dev.notedeck.core.data"
    // BuildConfig.DEBUG decides whether HTTP calls are logged.
    buildFeatures.buildConfig = true
}

// The schema history: commit it, so later versions can migrate learners' databases.
room3 {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    api(project(":core:domain"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.sqlite.framework)
    ksp(libs.androidx.room.compiler)
    api(libs.androidx.datastore.preferences)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.kotlinx.serialization)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging.interceptor)
    api(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)
    testImplementation(project(":core:testing"))
    testImplementation(libs.okhttp.mockwebserver3)
    testImplementation(libs.androidx.work.testing)
}

// The migration test rebuilds the version 1 database from its exported schema.
val schemasDir: File = file("schemas")
tasks.withType<Test>().configureEach {
    inputs.dir(schemasDir).withPropertyName("schemasDir").withPathSensitivity(PathSensitivity.RELATIVE)
    systemProperty("schemas.dir", schemasDir.absolutePath)
}
