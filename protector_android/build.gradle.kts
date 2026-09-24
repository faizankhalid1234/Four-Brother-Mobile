plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
}

// Keep build outputs off OneDrive to avoid Windows file locks.
val externalBuildRoot = file("C:/AndroidTools/builds/protector_android")
layout.buildDirectory.set(externalBuildRoot.resolve("root"))
subprojects {
    layout.buildDirectory.set(externalBuildRoot.resolve(name))
}
