import org.gradle.api.tasks.testing.Test
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

// Compose UI integration tests are deliberately outside the default "test" source set.
// They are expensive (real Compose windows / rendering) and run only with :desktop:uiTest.
val uiTestSourceSet = sourceSets.create("uiTest") {
    java.srcDir("src/uiTest/kotlin")
    compileClasspath += sourceSets["main"].output + sourceSets["test"].output
    runtimeClasspath += output + compileClasspath
}
configurations[uiTestSourceSet.implementationConfigurationName]
    .extendsFrom(configurations["testImplementation"])
configurations[uiTestSourceSet.runtimeOnlyConfigurationName]
    .extendsFrom(configurations["testRuntimeOnly"])

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
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    add("uiTestImplementation", compose.desktop.uiTestJUnit4)
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

// Unlike the built-in test compilation, uiTest must explicitly be a friend
// of main to access module-internal Compose primitives in existing UI tests.
tasks.named<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>("compileUiTestKotlin") {
    val mainOutput = sourceSets["main"].output.classesDirs.files.joinToString(",") { it.absolutePath.replace("\\", "/") }
    kotlinOptions.freeCompilerArgs += "-Xfriend-paths=$mainOutput"
}

tasks.test {
    // Pure JVM / domain / storage contract tests; no Compose windows or screenshot output.
    timeout.set(Duration.ofMinutes(10))
    systemProperty("java.awt.headless", "true")
    testLogging { events("failed", "skipped") }
}

tasks.register<Test>("uiTest") {
    group = "verification"
    description = "Optional interactive Compose UI regression tests. Never runs as part of check/test."
    testClassesDirs = uiTestSourceSet.output.classesDirs
    classpath = uiTestSourceSet.runtimeClasspath
    useJUnit()
    shouldRunAfter(tasks.test)
    timeout.set(Duration.ofMinutes(25))
    systemProperty("java.awt.headless", "false")
    testLogging { events("failed", "skipped") }
}