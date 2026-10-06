import org.jetbrains.compose.desktop.application.dsl.TargetFormat

plugins {
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.compose")
}

compose { kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.5.11") }
kotlin { jvmToolchain(17) }

dependencies {
    implementation(project(":core"))
    implementation(project(":shared-ui"))
    implementation(compose.desktop.currentOs)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    testImplementation(kotlin("test-junit"))
    testImplementation(compose.desktop.uiTestJUnit4)
}

compose.desktop {
    application {
        mainClass = "com.goldex.companion.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Qirato"
            packageVersion = "0.56.36"
            description = "Qirato gold calculator"
            vendor = "Qirato"
            modules("java.desktop")
            windows {
                menuGroup = "Qirato"
                shortcut = true
                upgradeUuid = "a6b3cf2a-a8f7-4cde-a49a-f8935c6b8306"
            }
        }
    }
}

tasks.test {
    systemProperty("java.awt.headless", "false")
    systemProperty("qirato.screenshotDir", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
}
