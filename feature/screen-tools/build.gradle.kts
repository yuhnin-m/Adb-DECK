/** Модуль feature:screen-tools — screenshot и screenrecord для активного устройства. */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

val javafxVersion = "21.0.6"
val osName = System.getProperty("os.name").lowercase()
val osArch = System.getProperty("os.arch").lowercase()
val javafxPlatform = when {
    osName.contains("mac") && (osArch.contains("aarch64") || osArch.contains("arm")) -> "mac-aarch64"
    osName.contains("mac") -> "mac"
    osName.contains("win") -> "win"
    osArch.contains("aarch64") || osArch.contains("arm") -> "linux-aarch64"
    else -> "linux"
}

dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)
    implementation(compose.components.resources)

    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.coroutines.core)
    implementation(libs.essenty.lifecycle.coroutines)

    implementation(project(":core:designsystem"))
    implementation(project(":core:i18n"))
    implementation(project(":core:ui"))
    implementation(project(":core:utils"))
    implementation(project(":core:adb-api"))
    implementation(project(":core:settings"))

    // Встроенный video preview/player через JavaFX MediaView.
    implementation("org.openjfx:javafx-base:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-graphics:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-controls:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-media:$javafxVersion:$javafxPlatform")
    implementation("org.openjfx:javafx-swing:$javafxVersion:$javafxPlatform")
}
