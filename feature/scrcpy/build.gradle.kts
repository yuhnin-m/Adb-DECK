/** Модуль feature:scrcpy — экран управления и запуска scrcpy для зеркалирования экрана. */
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
    implementation(project(":core:adb-api"))
    implementation(project(":core:settings"))
    implementation(project(":core:utils"))
    implementation(project(":core:i18n"))

    testImplementation(kotlin("test"))
    testImplementation(libs.coroutines.test)
}
