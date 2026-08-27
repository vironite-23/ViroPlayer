plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
    `module-config`
}

dependencies {
    val composeBom = platform(Deps.Compose.bom)
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation(Deps.AndroidX.activityCompose)
    implementation(Deps.AndroidX.lifecycleRuntimeCompose)
    implementation(Deps.AndroidX.lifecycleViewModelCompose)
    implementation(Deps.Coroutines.android)

    implementation(Deps.Compose.material3)
    implementation(Deps.Compose.materialIconsExtended)
    implementation(Deps.Compose.foundation)
    implementation(Deps.Compose.ui)
    implementation(Deps.Compose.uiToolingPreview)
    debugImplementation(Deps.Compose.uiTooling)

    implementation(Deps.Media3.exoplayer)
    implementation(Deps.Media3.ui)
    implementation(Deps.Media3.common)
}
