/** Модуль core:utils — общие вспомогательные утилиты, не зависящие от UI. */
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.coroutines.core)
}
