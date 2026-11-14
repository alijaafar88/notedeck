// The card generator's eval suite. On every push, CI checks the stored reference replies (no key, no cost);
// with ANTHROPIC_API_KEY set, `./gradlew :evals:run --args=live` checks the real model instead.
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    application
}

application {
    mainClass.set("dev.notedeck.evals.MainKt")
}

// Run from this folder, so data/ resolves the same way for `run` and for the tests.
tasks.named<JavaExec>("run") {
    workingDir = projectDir
}

dependencies {
    // The prompt, the parser and the rules: the same code the app and the backend use.
    implementation(project(":core:domain"))
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.anthropic.java)
    testImplementation(kotlin("test"))
}
