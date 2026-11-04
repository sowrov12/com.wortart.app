# Gradle setup for Task 1.1 (Room + KSP)

Adapt these to your version catalog if you use one.

## Project-level `build.gradle.kts`

```kotlin
plugins {
    // ...existing plugins...
    // The KSP version must match your Kotlin version (see the KSP releases page).
    id("com.google.devtools.ksp") version "<kotlin-version>-<ksp-version>" apply false
}
```

## App-level `app/build.gradle.kts`

```kotlin
plugins {
    // ...existing plugins...
    id("com.google.devtools.ksp")
}

android {
    defaultConfig {
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
}

// Exports Room schema JSON files (commit the schemas/ folder to Git).
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    val roomVersion = "2.6.1"
    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
}
```

## Run the tests

Needs an emulator or device:

```
./gradlew connectedAndroidTest
```
