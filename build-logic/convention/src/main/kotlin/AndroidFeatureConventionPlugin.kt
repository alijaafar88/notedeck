import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.project

/**
 * A feature module: an Android library with Compose and Hilt that sees only :core:domain and :core:ui.
 * Features never depend on each other; :app connects them.
 */
class AndroidFeatureConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("notedeck-android-library"))
            pluginManager.apply(libs.pluginId("notedeck-android-compose"))
            pluginManager.apply(libs.pluginId("notedeck-android-hilt"))
            dependencies {
                add("implementation", project(":core:domain"))
                add("implementation", project(":core:ui"))
                add("implementation", libs.lib("androidx-lifecycle-runtime-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-compose"))
                add("implementation", libs.lib("androidx-lifecycle-viewmodel-savedstate"))
                add("implementation", libs.lib("androidx-hilt-lifecycle-viewmodel-compose"))
                add("testImplementation", project(":core:testing"))
            }
        }
    }
}
