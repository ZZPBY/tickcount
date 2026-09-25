// Top-level build file. Plugin versions live in gradle/libs.versions.toml.
//
// Note the absence of an "org.jetbrains.kotlin.android" plugin: AGP 9 has
// built-in Kotlin support and that plugin is incompatible with it.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.compiler) apply false
}
