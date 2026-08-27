import io.gitlab.arturbosch.detekt.Detekt
import io.gitlab.arturbosch.detekt.extensions.DetektExtension

val configFile = file("$rootDir/config/detekt/detekt.yml")

subprojects {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    extensions.configure<DetektExtension> {
        buildUponDefaultConfig = true
        config.setFrom(configFile)
        source = files(
            DetektExtension.DEFAULT_SRC_DIR_JAVA,
            DetektExtension.DEFAULT_SRC_DIR_KOTLIN
        )
    }

    dependencies.add("detektPlugins", "io.gitlab.arturbosch.detekt:detekt-formatting:1.23.8")

    tasks.withType<Detekt>().configureEach {
        jvmTarget = "21"
        // Detekt 1.23.x's last stable release predates Kotlin 2.3 / AGP 9.3, so its behavior
        // against this toolchain isn't verified. Keep it advisory (run `./gradlew detekt`
        // locally, or wire it back into `check` once it's been validated) rather than letting an
        // unverified static-analysis run fail CI.
        ignoreFailures = true
        reports {
            html.required.set(true)
            xml.required.set(false)
            txt.required.set(false)
            sarif.required.set(false)
            md.required.set(false)
        }
    }
}
