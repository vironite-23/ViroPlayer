/**
 * Single source of truth for dependency versions and coordinates.
 *
 * The original template used the `de.fayard.refreshVersions` plugin for this. That plugin's
 * generated accessors (`AndroidX.*`, `Kotlin.*`, etc.) are pinned to a 2022-era release with no
 * verified compatibility against Gradle 9.3 / AGP 9.3 / Kotlin 2.3, so it was replaced here with
 * plain `const val` coordinates that do the same job (one place to bump a version) without an
 * extra, unverified plugin on the build classpath.
 */
object Versions {
    const val composeBom = "2026.08.00"
    const val activityCompose = "1.13.0"
    const val lifecycle = "2.10.0"
    const val coroutines = "1.11.0"
    const val media3 = "1.11.0"
}

object Deps {
    object Compose {
        const val bom = "androidx.compose:compose-bom:${Versions.composeBom}"
        const val material3 = "androidx.compose.material3:material3"
        const val materialIconsExtended = "androidx.compose.material:material-icons-extended"
        const val foundation = "androidx.compose.foundation:foundation"
        const val ui = "androidx.compose.ui:ui"
        const val uiToolingPreview = "androidx.compose.ui:ui-tooling-preview"
        const val uiTooling = "androidx.compose.ui:ui-tooling"
    }

    object AndroidX {
        const val activityCompose = "androidx.activity:activity-compose:${Versions.activityCompose}"
        const val lifecycleRuntimeCompose =
            "androidx.lifecycle:lifecycle-runtime-compose:${Versions.lifecycle}"
        const val lifecycleViewModelCompose =
            "androidx.lifecycle:lifecycle-viewmodel-compose:${Versions.lifecycle}"
    }

    object Coroutines {
        const val android = "org.jetbrains.kotlinx:kotlinx-coroutines-android:${Versions.coroutines}"
    }

    object Media3 {
        const val exoplayer = "androidx.media3:media3-exoplayer:${Versions.media3}"
        const val ui = "androidx.media3:media3-ui:${Versions.media3}"
        const val common = "androidx.media3:media3-common:${Versions.media3}"
    }
}
