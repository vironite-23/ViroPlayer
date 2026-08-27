import com.android.build.api.dsl.ApplicationExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

// Applied by app/build.gradle.kts alongside the AGP/Kotlin plugins themselves. Using
// pluginManager.withPlugin (rather than applying com.android.application here) means this
// convention plugin doesn't care whether it's declared before or after those plugins in the
// consumer's `plugins { }` block.
pluginManager.withPlugin("com.android.application") {
    extensions.configure<ApplicationExtension> {
        compileSdk = AndroidSdk.compileSdk

        defaultConfig {
            applicationId = AppCoordinates.APP_ID
            minSdk = AndroidSdk.minSdk
            targetSdk = AndroidSdk.targetSdk
            versionCode = AppCoordinates.VERSION_CODE
            versionName = AppCoordinates.VERSION_NAME
        }

        namespace = AppCoordinates.APP_ID

        compileOptions {
            sourceCompatibility = JavaVersion.VERSION_21
            targetCompatibility = JavaVersion.VERSION_21
        }

        buildFeatures {
            compose = true
            buildConfig = true
        }

        packaging {
            resources.excludes += listOf("META-INF/AL2.0", "META-INF/LGPL2.1")
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    compilerOptions.jvmTarget.set(JvmTarget.JVM_21)
}
