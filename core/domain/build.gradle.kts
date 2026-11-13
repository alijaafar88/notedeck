plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    api(project(":core:model"))
    api(libs.kotlinx.coroutines.core)
    // Only for @Qualifier on IoDispatcher; no DI framework in the domain.
    api(libs.javax.inject)
    testImplementation(kotlin("test"))
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
