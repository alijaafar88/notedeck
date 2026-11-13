import com.android.build.api.dsl.LibraryExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies

/** Every Android library module: the same SDK levels, Java version and unit-test setup. A module can still override any of it. */
class AndroidLibraryConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply(libs.pluginId("android-library"))
            extensions.configure<LibraryExtension> {
                compileSdk = 37
                defaultConfig.minSdk = 26
                compileOptions {
                    sourceCompatibility = JavaVersion.VERSION_17
                    targetCompatibility = JavaVersion.VERSION_17
                }
                // Robolectric tests need the module's resources.
                testOptions.unitTests.isIncludeAndroidResources = true
            }
            dependencies {
                add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit:${libs.version("kotlin")}")
                add("testImplementation", libs.lib("kotlinx-coroutines-test"))
                add("testImplementation", libs.lib("turbine"))
                add("testImplementation", libs.lib("robolectric"))
            }
        }
    }
}
