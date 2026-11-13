plugins {
    alias(libs.plugins.kotlin.jvm)
}

// Shared by every module's tests: the MainDispatcherRule and what it needs.
dependencies {
    api(kotlin("test-junit"))
    api(libs.kotlinx.coroutines.test)
}
