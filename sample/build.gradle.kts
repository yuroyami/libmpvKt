plugins {
    // AGP 9 compiles Kotlin itself; a separate Kotlin Android plugin is refused.
    alias(libs.plugins.android.application)
}

/*
 * The shortest consumer of :libmpvkt: one Activity, one SurfaceView, one URL. It proves the AAR
 * plays and it is what the README quotes. No Compose and no libraries, on purpose.
 */
android {
    namespace = "io.github.yuroyami.libmpvkt.sample"
    compileSdk = 37

    defaultConfig {
        applicationId = "io.github.yuroyami.libmpvkt.sample"
        minSdk = 21
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

dependencies {
    implementation(project(":libmpvkt"))
}
