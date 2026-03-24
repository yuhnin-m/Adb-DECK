/** Модуль feature:update — проверка обновлений приложения и диалог установки. */
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
    implementation(libs.serialization.json)

    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:utils"))
}
