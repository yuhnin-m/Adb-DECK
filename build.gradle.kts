import org.jetbrains.kotlin.gradle.dsl.KotlinJvmProjectExtension

/**
 * Корневой build-скрипт проекта ADB Deck.
 * Объявляет плагины для всех модулей, не применяя их здесь напрямую.
 */
plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.compose.compiler) apply false
}

subprojects {
    plugins.withId("org.jetbrains.kotlin.jvm") {
        extensions.configure<KotlinJvmProjectExtension> {
            jvmToolchain(21)
        }
    }
}
