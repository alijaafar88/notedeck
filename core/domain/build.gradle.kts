plugins {
    alias(libs.plugins.kotlin.jvm)
    // The reply parser lives here, shared by the app, the backend and the evals.
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    // Only for @Qualifier on IoDispatcher; no DI framework in the domain.
    api(libs.javax.inject)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
