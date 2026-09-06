plugins {
    // AGP 9 compiles Kotlin itself; a separate Kotlin Android plugin is refused.
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

/*
 * One screen per layer: MPVLib, MpvView on each surface type, MpvSurface in Compose, and
 * MpvPlayer. Each screen runs in its own process, because each holds a core.
 */
android {
    namespace = "io.github.yuroyami.libmpvkt.sample"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.yuroyami.libmpvkt.sample"
        // 23, not the library's 21: activity-compose pulls androidx.navigationevent, which asks
        // for 23. The library itself still runs on 21; only this demo app moves.
        minSdk = 23
        targetSdk = 37
        versionCode = 1
        versionName = providers.gradleProperty("VERSION").get()
    }

    buildTypes {
        release {
            // Shrunk on purpose: it is the check that the AAR's consumer keep rules are enough.
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"))
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

android {
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(project(":libmpvkt"))
    implementation(project(":libmpvkt-view"))
    implementation(project(":libmpvkt-compose"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.compose.material)
}
