import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.io.File
import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.compiler)
}

// Optional release signing. The properties file is looked up in two places:
//
//   keystore.properties                     written by CI on the runner
//   .tools/签名密钥文件/keystore.properties    the maintainer's local copy
//
// The second path is written with \u escapes to keep this script pure ASCII.
// Gradle reads the script as UTF-8, but the JVM here reports GBK for both
// file.encoding and sun.jnu.encoding, and a corrupted literal would fail
// *silently* by falling back to debug signing — which is the exact bug this
// lookup exists to prevent.
//
// `storeFile` is resolved relative to the properties file itself, so the
// keystore can sit next to its properties instead of needing a second copy at
// the repository root. Both paths are .gitignore'd. Without either file
// `assembleDebug` works out of the box and `assembleRelease` falls back to the
// debug key — see the README.
val keystorePropertiesFile: File? = listOf(
    rootProject.file("keystore.properties"),
    rootProject.file(".tools/\u7B7E\u540D\u5BC6\u94A5\u6587\u4EF6/keystore.properties"),
).firstOrNull { it.isFile }

val keystoreProperties = Properties()
keystorePropertiesFile?.let { file ->
    FileInputStream(file).use { stream -> keystoreProperties.load(stream) }
}
val hasReleaseKeystore = keystorePropertiesFile != null

/**
 * Copies the repository-root changelog into a directory the variant API adopts as
 * generated assets.
 *
 * A plain Copy task will not do. AGP 9 refuses Provider instances in the older
 * SourceSet API, and wiring a resolved directory into `sourceSets` leaves every
 * task that reads assets — the merge, and the lint model among them — with no
 * declared dependency on the producer. It is a whack-a-mole that
 * `addGeneratedSourceDirectory` exists to end.
 */
abstract class CopyChangelogTask : DefaultTask() {
    @get:InputFile
    abstract val sourceFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun copyChangelog() {
        val from = sourceFile.get().asFile
        val into = outputDir.get().asFile
        into.mkdirs()
        from.copyTo(into.resolve(from.name), overwrite = true)
    }
}

// The changelog lives at the repository root so it is readable on GitHub, and is
// copied into the APK's assets at build time so the in-app dialog and that file
// can never disagree. Generated into the build directory rather than committed a
// second time under src/main/assets. The file name is written with \u escapes to
// keep this script pure ASCII, for the same reason as the keystore path above.
val copyChangelog = tasks.register<CopyChangelogTask>("copyChangelog") {
    sourceFile.set(rootProject.file("\u66F4\u65B0\u65E5\u5FD7.txt"))
    outputDir.set(layout.buildDirectory.dir("generated/changelogAssets"))
}

android {
    namespace = "io.github.zzpby.tickcount"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.zzpby.tickcount"
        // java.time arrived in API 26 and the theme's windowLightNavigationBar in
        // 27, so 27 is the lowest floor that needs no desugaring, no compatibility
        // shim and no split of the theme files.
        minSdk = 27
        // Google Play requires new apps to target API 36+ since 2026-08-31.
        targetSdk = 36
        // Bumped together with versionName: 1.0.1 already shipped as version code
        // 2, and Play rejects a version code it has seen before.
        versionCode = 5
        versionName = "1.0.4"
    }

    signingConfigs {
        val propertiesFile = keystorePropertiesFile
        if (propertiesFile != null) {
            create("release") {
                // Relative to the properties file, which keeps the keystore next
                // to it rather than requiring a second copy at the root.
                storeFile = propertiesFile.parentFile
                    .resolve(keystoreProperties.getProperty("storeFile"))
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
        // The About box reads VERSION_NAME from the generated BuildConfig, so the
        // version can never drift from the one in the build script.
        buildConfig = true
    }

    lint {
        abortOnError = false
    }
}

// Declared through the variant API so AGP knows the directory is generated, and
// wires the dependency for every task that reads assets rather than for whichever
// ones happened to fail first.
androidComponents {
    onVariants { variant ->
        variant.sources.assets?.addGeneratedSourceDirectory(
            copyChangelog,
            CopyChangelogTask::outputDir,
        )
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
