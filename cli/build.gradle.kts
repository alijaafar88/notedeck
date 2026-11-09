plugins {
    alias(libs.plugins.kotlin.jvm)
    application
}

application {
    mainClass = "dev.notedeck.cli.MainKt"
}

dependencies {
    implementation(project(":core:domain"))
}
