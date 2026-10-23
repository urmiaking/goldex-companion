import org.jetbrains.compose.desktop.application.dsl.TargetFormat
import java.util.Properties
import java.time.Duration

val desktopVersion = Properties().apply { file("version.properties").inputStream().use { load(it) } }.getProperty("version")

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
    implementation(compose.materialIconsExtended)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
    implementation("com.google.zxing:core:3.5.3")
    implementation("org.apache.pdfbox:pdfbox:3.0.7")
    implementation("de.rototor.pdfbox:graphics2d:3.0.5")
    implementation("org.xerial:sqlite-jdbc:3.45.2.0")
    testImplementation(kotlin("test-junit"))
    testImplementation(compose.desktop.uiTestJUnit4)
}

compose.desktop {
    application {
        mainClass = "com.goldex.companion.desktop.MainKt"
        nativeDistributions {
            targetFormats(TargetFormat.Msi, TargetFormat.Exe)
            packageName = "Qirato"
            packageVersion = desktopVersion
            description = "Qirato gold calculator"
            vendor = "Qirato"
            // PDFBox uses the JDK's buffer cleaner when closing mapped document files. SQLite uses java.sql.
            modules("java.desktop", "jdk.unsupported", "java.sql")
            windows {
                iconFile.set(project.file("icons/qirato.ico"))
                menuGroup = "Qirato"
                shortcut = true
                upgradeUuid = "a6b3cf2a-a8f7-4cde-a49a-f8935c6b8306"
            }
        }
    }
}

tasks.processResources {
    from("../app/src/main/res") { include("mipmap-xxxhdpi/ic_launcher.png") }
    from("version.properties")
}

tasks.test {
    timeout.set(Duration.ofMinutes(15))
    systemProperty("java.awt.headless", "false")
    systemProperty("qirato.screenshotDir", layout.buildDirectory.dir("screenshots").get().asFile.absolutePath)
    testLogging {
        events("started", "passed", "skipped", "failed")
        showExceptions = true
        showCauses = true
        showStackTraces = true
    }
}

