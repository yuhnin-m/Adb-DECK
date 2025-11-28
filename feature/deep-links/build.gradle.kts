/** Модуль feature:deep-links — запуск Deep Link и Intent через ADB. */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(compose.runtime)
    implementation(compose.foundation)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.ui)
    implementation(compose.components.uiToolingPreview)

    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.coroutines.core)
    implementation(libs.essenty.lifecycle.coroutines)
    implementation(libs.serialization.json)

    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:adb-api"))
    implementation(project(":core:settings"))
    implementation(project(":core:utils"))
}
