/** Модуль core:adb-impl — реализация ADB-клиента через системный adb. */
plugins {
    alias(libs.plugins.kotlin.jvm)
}

dependencies {
    implementation(libs.coroutines.core)
    implementation(project(":core:process"))
    implementation(project(":core:adb-api"))
    implementation(project(":core:settings"))
    implementation(project(":core:utils"))
}
