/** Модуль core:i18n — общие строковые ресурсы и локализация для всего приложения. */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    implementation(compose.runtime)
    implementation(compose.components.resources)
}
