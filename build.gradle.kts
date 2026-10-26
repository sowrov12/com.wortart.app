// Top-level build file: declares plugin versions for all modules.
plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("org.jetbrains.kotlin.plugin.compose") version "2.0.21" apply false
    // The KSP version must match the Kotlin version (2.0.21-<ksp-version>).
    id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false
}
