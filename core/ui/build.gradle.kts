/** Модуль core:ui — общие reusable composable-компоненты (состояния загрузки, ошибок и пр.). */
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
    implementation(project(":core:designsystem"))
}
