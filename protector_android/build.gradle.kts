plugins {
    id("com.android.application") version "8.7.3" apply false
    id("org.jetbrains.kotlin.android") version "2.0.21" apply false
    id("com.google.gms.google-services") version "4.4.2" apply false
}

// On Windows, keep build outputs off OneDrive to avoid file locks.
// On macOS/Linux, use the default local build/ directory.
if (System.getProperty("os.name").orEmpty().lowercase().contains("windows")) {
    val externalBuildRoot = file("C:/AndroidTools/builds/fine_trade")
    layout.buildDirectory.set(externalBuildRoot.resolve("root"))
    subprojects {
        layout.buildDirectory.set(externalBuildRoot.resolve(name))
    }
}
