import com.vanniktech.maven.publish.MavenPublishBaseExtension
import org.gradle.api.publish.PublishingExtension

plugins {
    // Declared with apply false so the publish plugin's build service loads once for the build.
    alias(libs.plugins.vanniktech.publish).apply(false)
    alias(libs.plugins.kotlin.multiplatform).apply(false)
    alias(libs.plugins.android.kmp.library).apply(false)
    alias(libs.plugins.android.application).apply(false)
    // Applied at the root so dokkaGenerate aggregates the modules into one API site.
    alias(libs.plugins.dokka)
}

/**
 * Where a publish lands: a checkout of yuroyami/maven, which GitHub Pages serves at
 * https://yuroyami.github.io/maven/. publish.yml passes the checkout; a laptop passes
 * -Plibmpvkt.mavenRepoDir=/path/to/maven. Without the property it is a scratch directory under
 * build/, so the task always has a target and a mistake cannot reach the real repository.
 */
val staticRepoDir: String = providers.gradleProperty("libmpvkt.mavenRepoDir")
    .getOrElse(layout.buildDirectory.dir("static-maven").get().asFile.absolutePath)

allprojects {
    group = providers.gradleProperty("GROUP").get()
    version = providers.gradleProperty("VERSION").get()
}

/** True when this build holds a signing key. Blank counts as absent. */
val hasSigningKey: Boolean = !providers.gradleProperty("signingInMemoryKey").orNull.isNullOrBlank()

subprojects {
    val publishingProject = this
    pluginManager.withPlugin("com.vanniktech.maven.publish") {
        extensions.configure<MavenPublishBaseExtension> {
            // No publishToMavenCentral(): the artifacts go to the static repository declared below.
            // Only when a key exists. An unconditional signAllPublications() makes a keyless
            // publishToMavenLocal fail with "no configured signatory".
            if (hasSigningKey) {
                signAllPublications()
            } else {
                logger.lifecycle(
                    "[libmpvKt] no signingInMemoryKey: publications are UNSIGNED. Fine for " +
                        "mavenLocal and CI smoke tests; a release run supplies the key.",
                )
            }
            pom {
                name.set(publishingProject.name)
                description.set(rootProject.providers.gradleProperty("DESCRIPTION"))
                url.set("https://github.com/yuroyami/libmpvKt")
                licenses {
                    license {
                        name.set("Apache License 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0.txt")
                        comments.set("The Kotlin wrapper, the JNI sources and the build scripts.")
                    }
                    license {
                        name.set("GNU General Public License v3.0 or later")
                        url.set("https://www.gnu.org/licenses/gpl-3.0.txt")
                        comments.set(
                            "The native libraries inside the AAR. mpv is GPL-2.0-or-later and its " +
                                "FFmpeg is built with --enable-gpl --enable-version3, so the combination " +
                                "is GPL-3.0-or-later. NOTICE lists every library and its own licence.",
                        )
                    }
                }
                // Identity only, no address: a published POM is public for ever.
                developers {
                    developer {
                        id.set("yuroyami")
                        name.set("yuroyami")
                        url.set("https://github.com/yuroyami")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/yuroyami/libmpvKt.git")
                    developerConnection.set("scm:git:ssh://git@github.com/yuroyami/libmpvKt.git")
                    url.set("https://github.com/yuroyami/libmpvKt")
                }
            }
        }
        // The static repository. The task this creates is publishAllPublicationsToStaticRepository.
        extensions.configure<PublishingExtension> {
            repositories {
                maven {
                    name = "static"
                    url = uri(staticRepoDir)
                }
            }
        }
    }
}

dokka {
    moduleName.set("libmpvKt")
}

dependencies {
    dokka(project(":libmpvkt"))
}

// The Kite documentation theme, copied into this repository so it builds from a fresh clone.
// Applied to every project that has Dokka: under aggregation each module renders its own pages.
allprojects {
    plugins.withId("org.jetbrains.dokka") {
        extensions.configure<org.jetbrains.dokka.gradle.DokkaExtension> {
            pluginsConfiguration.html {
                val themeCss = rootProject.layout.projectDirectory.file("docs/api-theme/kite.css")
                if (themeCss.asFile.exists()) {
                    customStyleSheets.from(themeCss)
                }
                val templates = rootProject.layout.projectDirectory.dir("dokka-templates")
                if (templates.asFile.exists()) {
                    templatesDir.set(templates)
                }
                footerMessage.set("Apache-2.0 wrapper over GPL binaries. libmpvKt by yuroyami.")
            }
            dokkaSourceSets.configureEach {
                val moduleDoc = layout.projectDirectory.file("Module.md")
                if (moduleDoc.asFile.exists()) {
                    includes.from(moduleDoc)
                }
            }
        }
    }
}
