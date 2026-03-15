import org.gradle.api.tasks.SourceSetContainer
import org.gradle.api.tasks.testing.Test

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

    testImplementation(kotlin("test"))
}

val testSourceSet = extensions.getByType<SourceSetContainer>()["test"]
val adbIntegrationPattern = "**/*AdbShellSmokeIntegrationTest.class"

tasks.named<Test>("test") {
    exclude(adbIntegrationPattern)
}

tasks.register<Test>("adbIntegrationTest") {
    group = "verification"
    description = "Runs ADB shell smoke integration test against a connected device/emulator."
    testClassesDirs = testSourceSet.output.classesDirs
    classpath = testSourceSet.runtimeClasspath
    include(adbIntegrationPattern)
    shouldRunAfter(tasks.named("test"))
    systemProperty("adb.integration.enabled", "true")
}
