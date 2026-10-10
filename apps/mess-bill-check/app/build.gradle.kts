import java.io.File

// Keystore material is never in Git: the path and the three secret values come from
// GitHub Actions (see .github/workflows/ci-build.yml). Missing credentials must fail a
// release build rather than silently produce an unsigned artifact.
val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")
val wantsRelease = gradle.startParameter.taskNames.any {
    val t = it.lowercase()
    t.contains("release") || t.contains("bundle")
}
if (wantsRelease && keystorePath.isNullOrBlank()) {
    throw GradleException(
        "Release build requested without signing credentials. Set ANDROID_KEYSTORE_PATH, " +
            "ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD in the " +
            "repository's Actions secrets. A throwaway keystore is deliberately not generated: " +
            "the upload key must be created once and kept."
    )
}

plugins {
    id("com.android.application")
}

android {
    namespace = "com.pabi277.messbillcheck"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.pabi277.messbillcheck"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "0.1.0"
    }
}

// Signing is only configured when CI supplied a keystore, so the ordinary build path
// never touches signing DSL at all.
if (!keystorePath.isNullOrBlank()) {
    android.signingConfigs.create("release") {
        storeFile = File(keystorePath)
        storePassword = System.getenv("ANDROID_KEYSTORE_PASSWORD")
        keyAlias = System.getenv("ANDROID_KEY_ALIAS")
        keyPassword = System.getenv("ANDROID_KEY_PASSWORD")
    }
    android.buildTypes.getByName("release").signingConfig = android.signingConfigs.getByName("release")
}

dependencies {
    // No androidx and no SDKs in the skeleton: v1 is offline by construction, and the
    // fewer third-party coordinates here, the fewer privacy/licensing claims to keep
    // truthful later. R8/minify stays off until a library needs keep rules
    // (templates/proguard-note.md).
    testImplementation("junit:junit:4.13.2")
}
