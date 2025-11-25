/**
 * Корневые настройки Gradle-проекта ADB Deck.
 * Объявляет все модули и репозитории для разрешения зависимостей.
 */
rootProject.name = "AdbDeck"

pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

// Точка входа приложения
include(":app")

// Core-модули — общие утилиты, сервисы и дизайн-система
include(":core:ui")
include(":core:designsystem")
include(":core:utils")
include(":core:process")
include(":core:adb-api")
include(":core:adb-impl")
include(":core:settings")

// Feature-модули — отдельные экраны и бизнес-логика
include(":feature:dashboard")
include(":feature:devices")
include(":feature:logcat")
include(":feature:settings")
include(":feature:packages")
include(":feature:system-monitor")
include(":feature:file-explorer")
include(":feature:contacts")
include(":feature:screen-tools")
include(":feature:apk-install")
