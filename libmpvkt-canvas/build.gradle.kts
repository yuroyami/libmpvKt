import com.vanniktech.maven.publish.JavadocJar
import com.vanniktech.maven.publish.KotlinMultiplatform
import com.vanniktech.maven.publish.SourcesJar

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kmp.library)
    alias(libs.plugins.vanniktech.publish)
    alias(libs.plugins.dokka)
    alias(libs.plugins.kotlin.compose)
}

import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import java.io.File
import io.github.yuroyami.libmpvkt.buildtools.Elf
import io.github.yuroyami.libmpvkt.buildtools.NativeLibs

/*
 * :libmpvkt-canvas draws mpv as a plain Compose image, through mpv's render API. It carries one
 * native library of its own, libmpvkt_render.so, which links libmpv.so from the core AAR at load
 * time. An app that never draws into Compose does not carry it.
 */
kotlin {
    explicitApi()
    jvmToolchain(21)

    @OptIn(org.jetbrains.kotlin.gradle.dsl.abi.ExperimentalAbiValidation::class)
    abiValidation {
        // Declaring the block switches tracking on.
    }

    android {
        namespace = "io.github.yuroyami.libmpvkt.canvas"
        compileSdk = 37
        // 26: AHardwareBuffer and libnativewindow start there.
        minSdk = 26
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
        getByName("androidMain").dependencies {
            api(project(":libmpvkt-view"))
            implementation(libs.compose.runtime)
            implementation(libs.compose.ui)
            implementation(libs.compose.foundation)
        }
        getByName("androidHostTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.androidx.test.core)
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
        }
    }
}

/* The render library rides this module's jniLibs, beside the core AAR's own libraries. */
extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    onVariants { variant ->
        checkNotNull(variant.sources.jniLibs) { "AGP exposed no jniLibs sources for ${variant.name}" }
            .addStaticSourceDirectory("native-libs")
    }
}

/** Fails the build when the render library is missing for an ABI the app will ship. */
val checkRenderLib = tasks.register("checkRenderLib") {
    group = "verification"
    description = "Fails unless libmpvkt_render.so exists for every required ABI."
    val root = layout.projectDirectory.dir("native-libs").asFile
    val abis = providers.gradleProperty("libmpvkt.abis")
        .map { it.split(',').map(String::trim).filter(String::isNotEmpty) }
        .getOrElse(NativeLibs.abis.keys.toList())
    inputs.files(fileTree("native-libs") { include("*/*.so") })
    inputs.property("abis", abis)
    doLast {
        val missing = abis.filterNot { abi -> NativeLibs.render.all { lib -> File(root, "$abi/$lib").isFile } }
        check(missing.isEmpty()) {
            "libmpvkt_render.so is missing for ${missing.joinToString()}. Build it with " +
                "buildscripts/buildall.sh --arch <arch> -n jni."
        }
        val tooNew = abis.flatMap { abi ->
            NativeLibs.render.mapNotNull { lib ->
                Elf.androidApiLevel(File(root, "$abi/$lib").readBytes())
                    ?.takeIf { it > NativeLibs.RENDER_MIN_API }
                    ?.let { "$abi/$lib is linked for API $it" }
            }
        }
        check(tooNew.isEmpty()) { "${tooNew.joinToString()}, above libmpvkt-canvas's minSdk ${NativeLibs.RENDER_MIN_API}." }
        logger.lifecycle("[libmpvKt] the render library is present for ${abis.joinToString()}")
    }
}

tasks.matching { it.name == "mergeAndroidMainJniLibFolders" || it.name.startsWith("publish") }
    .configureEach { dependsOn(checkRenderLib) }

mavenPublishing {
    configure(
        KotlinMultiplatform(
            javadocJar = JavadocJar.Dokka("dokkaGeneratePublicationHtml"),
            sourcesJar = SourcesJar.Sources(),
        ),
    )
}
