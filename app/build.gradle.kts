import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

val localPropertiesFile = rootProject.file("local.properties")
val localProperties = Properties().apply {
    if (localPropertiesFile.exists()) {
        localPropertiesFile.inputStream().use { load(it) }
    }
}

val releaseStoreFilePath = System.getenv("GOLD_EX_RELEASE_STORE_FILE")
    ?: localProperties.getProperty("goldex.release.storeFile")
val releaseStorePassword = System.getenv("GOLD_EX_RELEASE_STORE_PASSWORD")
    ?: localProperties.getProperty("goldex.release.storePassword")
val releaseKeyAlias = System.getenv("GOLD_EX_RELEASE_KEY_ALIAS")
    ?: localProperties.getProperty("goldex.release.keyAlias")
val releaseKeyPassword = System.getenv("GOLD_EX_RELEASE_KEY_PASSWORD")
    ?: localProperties.getProperty("goldex.release.keyPassword")
val hasReleaseSigningConfig = !releaseStoreFilePath.isNullOrBlank()
    && !releaseStorePassword.isNullOrBlank()
    && !releaseKeyAlias.isNullOrBlank()
    && !releaseKeyPassword.isNullOrBlank()

if (System.getenv("CI") == "true" && !hasReleaseSigningConfig) {
    throw GradleException("Release signing is not configured in CI; refusing to produce an unsigned APK")
}

android {
    namespace = "com.goldex.companion"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.goldex.companion"
        minSdk = 24
        targetSdk = 34
        versionCode = 53
        versionName = "0.20.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    if (hasReleaseSigningConfig) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseStoreFilePath)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigningConfig) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.11"
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    val composeBom = platform("androidx.compose:compose-bom:2024.04.01")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("org.json:json:20240303")

    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")

    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
