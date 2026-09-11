import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar
import io.github.yuroyami.libmpvkt.buildtools.CheckNativeLibsTask
import io.github.yuroyami.libmpvkt.buildtools.GenerateBuildInfoTask
import io.github.yuroyami.libmpvkt.buildtools.NativeLibs

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.dokka)
}

/*
 * :libmpvkt is the whole library: the MPVLib wrapper, the JNI glue and the prebuilt mpv chain.
 * Gradle compiles no native file here. buildscripts/ writes the libraries into native-libs per
 * ABI, and this build checks and packages them.
 */

/** ABI directories that must be complete. All four by default; a laptop with one arch passes -Plibmpvkt.abis=arm64-v8a. */
val requiredAbis: List<String> = providers.gradleProperty("libmpvkt.abis")
    .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
    .getOrElse(NativeLibs.abis.keys.toList())

val generateBuildInfo = tasks.register<GenerateBuildInfoTask>("generateBuildInfo") {
    depinfo.set(rootProject.layout.projectDirectory.file("buildscripts/include/depinfo.sh"))
    libraryVersion.set(providers.gradleProperty("VERSION"))
    minSdk.set(NativeLibs.MIN_API)
    outputDir.set(layout.buildDirectory.dir("generated/buildinfo"))
}

val checkNativeLibs = tasks.register<CheckNativeLibsTask>("checkNativeLibs") {
    group = "verification"
    description = "Fails unless every required ABI has its ten libraries, 16 KB aligned and built for this package."
    nativeLibsDir.set(layout.projectDirectory.dir("native-libs"))
    libraryFiles.from(fileTree("native-libs") { include("*/*.so") })
    abis.set(requiredAbis)
    report.set(layout.buildDirectory.file("reports/checkNativeLibs.txt"))
}

kotlin {
    explicitApi()
    jvmToolchain(21)

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        // Declaring the block switches tracking on: updateKotlinAbi writes api/, checkKotlinAbi compares.
    }

    android {
        namespace = "io.github.yuroyami.libmpvkt.jni"
        compileSdk = 37
        // What an app needs: compile SDK 35 and Java 11 bytecode, not this build's SDK 37 and JDK 21.
        aarMetadata { minCompileSdk = 35 }
        compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11) }
        // The natives are linked for this API level (buildall.sh, Application.mk), and
        // checkNativeLibs refuses any that ask for more.
        minSdk = NativeLibs.MIN_API
        withHostTest {}
        withDeviceTestBuilder {
            sourceSetTreeName = "test"
        }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
        optimization {
            consumerKeepRules.publish = true
            consumerKeepRules.file("consumer-rules.pro")
        }
    }

    sourceSets {
        getByName("androidMain") {
            kotlin.srcDir(generateBuildInfo.map { it.outputDir })
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

/* The prebuilt libraries ride the jniLibs source of the one Android variant. */
extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    onVariants { variant ->
        checkNotNull(variant.sources.jniLibs) { "AGP exposed no jniLibs sources for ${variant.name}" }
            .addStaticSourceDirectory("native-libs")
    }
}

/*
 * Nothing that packages the libraries runs before the check: the main variant's jniLibs merge
 * (which the AAR, the device test APK and the sample all consume) and every publish task.
 */
tasks.named { it == "mergeAndroidMainJniLibFolders" || it.startsWith("publish") }
    .configureEach { dependsOn(checkNativeLibs) }

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources(),
        ),
    )
}
