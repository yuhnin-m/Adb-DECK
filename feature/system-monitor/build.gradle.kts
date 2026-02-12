/** Модуль feature:system-monitor — Task Manager и Storage Monitor для Android-устройства. */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
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
    implementation(project(":core:ui"))
    implementation(project(":core:utils"))
    implementation(project(":core:adb-api"))
    implementation(project(":core:settings"))
    // Переиспользуем PackageClient для действий force-stop из панели деталей процесса
    // (интерфейс находится в :core:adb-api, реализация — в :core:adb-impl через DI в :app)
}
