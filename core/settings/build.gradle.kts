/** Модуль core:settings — хранение и чтение настроек приложения. */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(libs.serialization.json)
}
