import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    androidTarget {
        compilations.all { kotlinOptions.jvmTarget = "17" }
    }
    jvm {
        compilations.all { kotlinOptions.jvmTarget = "17" }
    }

    sourceSets {
        val commonMain by getting {
            dependencies { implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3") }
        }
        val commonTest by getting {
            dependencies { implementation(kotlin("test")) }
        }
        // Android and desktop share Java-backed adapters, never business rules.
        val jvmSharedMain by creating { dependsOn(commonMain) }
        val androidMain by getting { dependsOn(jvmSharedMain) }
        val jvmMain by getting {
            dependsOn(jvmSharedMain)
            dependencies { api("org.json:json:20240303") }
        }

        val jvmSharedTest by creating {
            dependsOn(commonTest)
            dependencies { implementation("junit:junit:4.13.2") }
        }
        val androidUnitTest by getting { dependsOn(jvmSharedTest) }
        val jvmTest by getting { dependsOn(jvmSharedTest) }
    }
}

android {
    namespace = "com.goldex.companion.core"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

// A typed task avoids capturing the Gradle script in a doLast closure.
// It is safe for the configuration cache and can reuse prior verification inputs.
abstract class VerifyCoreBoundaries : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val commonSources: ConfigurableFileCollection

    @TaskAction
    fun verify() {
        val platformImport = Regex("""^\s*import\s+(java\.|javax\.|android\.|androidx\.|org\.json\.)""")
        val violations = commonSources.files.sorted().flatMap { file ->
            file.readLines().mapIndexedNotNull { index, line ->
                if (platformImport.containsMatchIn(line)) "${file.name}:${index + 1}" else null
            }
        }
        check(violations.isEmpty()) { "Platform dependencies in commonMain: ${violations.joinToString()}" }
    }
}

val verifyCoreBoundaries by tasks.registering(VerifyCoreBoundaries::class) {
    group = "verification"
    description = "Compiles common metadata and rejects platform imports in common business code."
    dependsOn("compileCommonMainKotlinMetadata")
    commonSources.from(fileTree("src/commonMain") { include("**/*.kt") })
}
tasks.named("check") { dependsOn(verifyCoreBoundaries) }
