import org.jetbrains.compose.desktop.application.dsl.TargetFormat

/** Модуль :app — точка входа, главное окно, инициализация DI и навигации. */
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

dependencies {
    // Desktop runtime — тянет за собой весь compose stack
    implementation(compose.desktop.currentOs)
    implementation(compose.material3)
    implementation(compose.materialIconsExtended)
    implementation(compose.components.uiToolingPreview)

    // Навигация и lifecycle
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.essenty.lifecycle)
    implementation(libs.coroutines.core)
    // Provides Dispatchers.Main backed by Swing EDT — required for desktop coroutines
    implementation(libs.coroutines.swing)

    // Dependency Injection
    implementation(libs.koin.core)

    // Core-модули
    implementation(project(":core:designsystem"))
    implementation(project(":core:ui"))
    implementation(project(":core:utils"))
    implementation(project(":core:process"))
    implementation(project(":core:adb-api"))
    implementation(project(":core:adb-impl"))
    implementation(project(":core:settings"))

    // Feature-модули
    implementation(project(":feature:dashboard"))
    implementation(project(":feature:devices"))
    implementation(project(":feature:logcat"))
    implementation(project(":feature:settings"))
    implementation(project(":feature:packages"))
    implementation(project(":feature:system-monitor"))
    implementation(project(":feature:file-explorer"))
    implementation(project(":feature:contacts"))
    implementation(project(":feature:screen-tools"))
}

compose.desktop {
    application {
        mainClass = "com.adbdeck.app.MainKt"

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb)
            packageName = "AdbDeck"
            packageVersion = "1.0.0"
            description = "Desktop-инструмент для работы с ADB"
            copyright = "© 2025 ADB Deck"

            macOS {
                bundleID = "com.adbdeck.app"
            }
        }
    }
}
