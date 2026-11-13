// NoteDeck's backend: a small Ktor server that holds the Anthropic key, so the app never does.
// Run it with the key in its environment, never in a file:  ANTHROPIC_API_KEY=... ./gradlew :backend:run
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("dev.notedeck.backend.MainKt")
}

dependencies {
    // Shares the prompt and the card rules with the app.
    implementation(project(":core:domain"))
    implementation(libs.ktor.server.core)
    implementation(libs.ktor.server.netty)
    implementation(libs.anthropic.java)
    implementation(libs.kotlinx.serialization.json)
    testImplementation(kotlin("test"))
    testImplementation(libs.ktor.server.test.host)
    testImplementation(libs.kotlinx.coroutines.test)
}
