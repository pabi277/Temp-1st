// Version matrix pinned 2026-10-11 from official sources:
// AGP 9.4.0 requires Gradle >= 9.6.0, SDK Build Tools 36.0.0 and JDK 17.
// https://developer.android.com/build/releases/agp-9-4-0-release-notes (checked 2026-10-11)
plugins {
    id("com.android.application") version "9.4.0" apply false
    id("org.jetbrains.kotlin.android") version "2.4.21" apply false
}
