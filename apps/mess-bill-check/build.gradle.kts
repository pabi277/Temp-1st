// Version matrix pinned 2026-10-11 from official sources:
// AGP 9.4.0 requires Gradle >= 9.6.0, SDK Build Tools 36.0.0 and JDK 17.
//   https://developer.android.com/build/releases/agp-9-4-0-release-notes
// Kotlin support is BUILT INTO AGP 9.x: applying org.jetbrains.kotlin.android is a
// hard error ("no longer required for Kotlin support since AGP 9.0") and the plugin
// is incompatible with AGP 9's new DSL interfaces, so there is no KGP here.
//   https://developer.android.com/build/releases/agp-9-0-0-release-notes#android-gradle-plugin-built-in-kotlin
plugins {
    id("com.android.application") version "9.4.0" apply false
}
