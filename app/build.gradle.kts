import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.notedeck.android.hilt)
    alias(libs.plugins.androidx.baselineprofile)
}

// Signing secrets come from a gitignored keystore.properties, or from environment variables in CI. Never from Git.
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun secret(property: String, env: String): String? = keystoreProperties.getProperty(property) ?: System.getenv(env)

// AI settings from the gitignored local.properties (see README). None of them is a secret: Firebase's values
// identify the project, and App Check, not hiding them, is what protects it. Provider API keys never come here.
val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) file.inputStream().use(::load)
}

fun local(name: String, default: String = ""): String = localProperties.getProperty(name) ?: default

android {
    namespace = "dev.notedeck.app"
    compileSdk = 37

    defaultConfig {
        applicationId = "dev.notedeck"
        minSdk = 26
        targetSdk = 37
        versionCode = 1
        versionName = "0.1"

        buildConfigField("String", "FIREBASE_APP_ID", "\"${local("notedeck.firebase.appId")}\"")
        buildConfigField("String", "FIREBASE_API_KEY", "\"${local("notedeck.firebase.apiKey")}\"")
        buildConfigField("String", "FIREBASE_PROJECT_ID", "\"${local("notedeck.firebase.projectId")}\"")
        // Your backend for Claude. Empty means none, and Claude shows as unavailable.
        buildConfigField("String", "PROXY_URL", "\"${local("notedeck.proxyUrl")}\"")
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    signingConfigs {
        // Only when a key is present, so CI and contributors without one can still build an unsigned release.
        val store = secret("storeFile", "NOTEDECK_KEYSTORE")
        if (store != null) {
            create("release") {
                storeFile = file(store)
                storePassword = secret("storePassword", "NOTEDECK_KEYSTORE_PASSWORD")
                keyAlias = secret("keyAlias", "NOTEDECK_KEY_ALIAS")
                keyPassword = secret("keyPassword", "NOTEDECK_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        // The emulator reaches the backend on your computer at 10.0.2.2. Other builds need an https URL.
        debug {
            buildConfigField("String", "PROXY_URL", "\"${local("notedeck.proxyUrl", "http://10.0.2.2:8080")}\"")
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.findByName("release")
        }
    }

    // Robolectric runs the Hilt tests on the JVM and needs the app's resources.
    testOptions.unitTests.isIncludeAndroidResources = true

    // BuildConfig.DEBUG shows the test reminder button and the Prompt Playground in debug builds only.
    buildFeatures.buildConfig = true
}

dependencies {
    implementation(project(":core:data"))
    implementation(project(":core:ui"))
    implementation(project(":feature:decks"))
    implementation(project(":feature:study"))
    // Reachable only from debug builds' Settings; R8 drops it from release builds as unused.
    implementation(project(":feature:playground"))
    implementation(project(":feature:generate"))
    implementation(project(":feature:assistant"))
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.appcheck.playintegrity)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.navigation3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.androidx.compose.material3.adaptive)
    implementation(libs.kotlinx.serialization.core)
    implementation(libs.androidx.hilt.lifecycle.viewmodel.compose)
    implementation(libs.androidx.hilt.work)
    // Applies the Baseline Profile on install, so the first launch is already compiled ahead of time.
    implementation(libs.androidx.profileinstaller)
    baselineProfile(project(":baselineprofile"))
    debugImplementation(libs.androidx.compose.ui.tooling)
    testImplementation(kotlin("test-junit"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
