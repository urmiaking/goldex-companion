plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.multiplatform")
    id("org.jetbrains.compose")
}

// Keep the verified Android Kotlin/compiler pair; a toolchain upgrade is a separate migration.
compose { kotlinCompilerPlugin.set("androidx.compose.compiler:compiler:1.5.11") }

kotlin {
    androidTarget { compilations.all { kotlinOptions.jvmTarget = "17" } }
    jvm { compilations.all { kotlinOptions.jvmTarget = "17" } }
    sourceSets {
        val commonMain by getting {
            dependencies {
                api(compose.runtime)
                api(compose.foundation)
                api(compose.material3)
                api(compose.ui)
            }
        }
        val jvmMain by getting {
            resources.srcDir("../app/src/main/res")
            resources.include("font/*.ttf")
        }
    }
}

// The existing font files remain the single asset source for both hosts.
val prepareAndroidFonts by tasks.registering(Sync::class) {
    from("../app/src/main/res") { include("font/*.ttf") }
    into(layout.buildDirectory.dir("generated/fontResources"))
}

android {
    namespace = "com.goldex.companion.sharedui"
    compileSdk = 34
    defaultConfig { minSdk = 24 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    sourceSets.getByName("main").res.srcDir(layout.buildDirectory.dir("generated/fontResources"))
}

tasks.named("preBuild") { dependsOn(prepareAndroidFonts) }
