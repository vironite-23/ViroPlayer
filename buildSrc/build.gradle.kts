plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
    gradlePluginPortal()
}

dependencies {
    implementation("com.android.tools.build:gradle:9.3.0")
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.3.21")
    implementation("io.gitlab.arturbosch.detekt:detekt-gradle-plugin:1.23.8")
}
