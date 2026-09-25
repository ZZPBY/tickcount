import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Optional release signing. Create a `keystore.properties` file in the project
// root (it is .gitignore'd) with:
//   storeFile=release.jks
//   storePassword=...
//   keyAlias=...
//   keyPassword=...
// Without it, `assembleDebug` works out of the box and `assembleRelease`
// produces an unsigned APK.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties().apply {
    if (keystorePropertiesFile.exists()) {
        FileInputStream(keystorePropertiesFile).use { load(it) }
    }
}
val hasReleaseKeystore = keystorePropertiesFile.exists()

android {
    namespace = "io.github.ginko.tickcount"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.ginko.tickcount"
        // java.time and the modern notification APIs are all available from 29,
        // so there is no desugaring and no compatibility shim anywhere.
        minSdk = 29
        // Google Play requires new apps to target API 36+ since 2026-08-31.
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
    }

    signingConfigs {
        if (hasReleaseKeystore) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            // With no keystore.properties the release build falls back to the
            // debug key. That still yields a minified, non-debuggable, directly
            // installable APK — which is what makes `assembleRelease` and the
            // GitHub Actions workflow work with zero setup — but it is NOT
            // suitable for publishing to a store. See README.
            signingConfig = if (hasReleaseKeystore) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        compose = true
    }

    lint {
        abortOnError = false
    }
}

kotlin {
    compilerOptions {
        // Defaults to compileOptions.targetCompatibility, stated here for clarity.
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)

    debugImplementation(libs.androidx.compose.ui.tooling)

    testImplementation(libs.junit)
}
