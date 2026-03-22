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
    implementation(compose.components.resources)

    // Навигация и lifecycle
    implementation(libs.decompose)
    implementation(libs.decompose.extensions.compose)
    implementation(libs.essenty.lifecycle)
    implementation(libs.essenty.lifecycle.coroutines)
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
    implementation(project(":feature:file-system"))
    implementation(project(":feature:file-explorer"))
    implementation(project(":feature:contacts"))
    implementation(project(":feature:screen-tools"))
    implementation(project(":feature:apk-install"))
    implementation(project(":feature:deep-links"))
    implementation(project(":feature:notifications"))
    implementation(project(":feature:device-info"))
    implementation(project(":feature:quick-toggles"))
    implementation(project(":feature:scrcpy"))
}

val appDisplayName = "ADB Deck"
val appPackageName = "ADBDeck"
val appVersion = "1.1.1"
val generatedAppBuildInfoDir = layout.buildDirectory.dir("generated/source/appBuildInfo/kotlin")

val generateAppBuildInfo by tasks.registering {
    outputs.dir(generatedAppBuildInfoDir)
    doLast {
        val outputFile = generatedAppBuildInfoDir.get()
            .file("com/adbdeck/app/AppBuildInfo.kt")
            .asFile

        outputFile.parentFile.mkdirs()
        outputFile.writeText(
            """
            package com.adbdeck.app

            internal const val APP_DISPLAY_NAME: String = "$appDisplayName"
            internal const val APP_VERSION: String = "$appVersion"
            """.trimIndent(),
        )
    }
}

kotlin {
    sourceSets.main {
        kotlin.srcDir(generatedAppBuildInfoDir)
    }
}

tasks.named("compileKotlin").configure {
    dependsOn(generateAppBuildInfo)
}

compose.desktop {
    application {
        mainClass = "com.adbdeck.app.MainKt"

        buildTypes.release.proguard {
            // Compose 1.7.0 defaults to ProGuard 7.2.2 (up to Java 18).
            // Java 21 bytecode (class version 65) requires newer ProGuard.
            version.set("7.4.2")
        }

        nativeDistributions {
            targetFormats(TargetFormat.Dmg, TargetFormat.Msi, TargetFormat.Deb, TargetFormat.Rpm)
            packageName = appPackageName
            packageVersion = appVersion
            description = "$appDisplayName desktop tool for ADB"
            copyright = "© 2025 ADB Deck"

            macOS {
                bundleID = "com.adbdeck.app"
                iconFile.set(project.file("src/main/resources/icons/adbdeck.icns"))
            }

            windows {
                iconFile.set(project.file("src/main/resources/icons/adbdeck.ico"))
            }

            linux {
                iconFile.set(project.file("src/main/resources/icons/adbdeck.png"))
            }
        }
    }
}
