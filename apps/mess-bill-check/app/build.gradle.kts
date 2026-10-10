import java.io.File

// Keystore material is never in Git: the path and the three secret values come
// from GitHub Actions (see .github/workflows/ci-build.yml). Missing credentials
// must fail a release build rather than silently produce an unsigned artifact.
val keystorePath: String? = System.getenv("ANDROID_KEYSTORE_PATH")
val keystorePassword: String? = System.getenv("ANDROID_KEYSTORE_PASSWORD")
val keyAlias: String? = System.getenv("ANDROID_KEY_ALIAS")
val keyPassword: String? = System.getenv("ANDROID_KEY_PASSWORD")

val wantsRelease = gradle.startParameter.taskNames.any {
    val t = it.lowercase()
    t.contains("release") || t.contains("bundle")
}
if (wantsRelease && keystorePath.isNullOrBlank()) {
    throw GradleException(
        "Release build requested without signing credentials. Set ANDROID_KEYSTORE_PATH, " +
            "ANDROID_KEYSTORE_PASSWORD, ANDROID_KEY_ALIAS and ANDROID_KEY_PASSWORD in the " +
            "repository's Actions secrets. A throwaway keystore is deliberately not generated here: " +
            "the upload key must be created once and kept."
    )
}

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
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

    signingConfigs {
        if (!keystorePath.isNullOrBlank()) {
            create("release") {
                storeFile = File(keystorePath)
                storePassword = keystorePassword
                keyAlias = keyAlias
                keyPassword = keyPassword
            }
        }
    }

    buildTypes {
        release {
            // R8 off until a library actually needs keep rules: see
            // templates/proguard-note.md ("one section per library" law).
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = if (keystorePath.isNullOrBlank()) {
                null
            } else {
                signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    testOptions {
        unitTests.isReturnDefaultValues = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    // No androidx, no SDKs in the skeleton: v1 is offline by construction and the
    // fewer third-party coordinates here, the fewer privacy/licensing declarations
    // to keep truthful later.
    testImplementation("junit:junit:4.13.2")
}
