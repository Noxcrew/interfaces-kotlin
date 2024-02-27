import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import java.io.ByteArrayOutputStream
import net.kyori.indra.IndraPlugin
import net.kyori.indra.IndraPublishingPlugin
import xyz.jpenilla.runpaper.task.RunServer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.indra)
    alias(libs.plugins.indra.publishing) apply false
    alias(libs.plugins.run.paper) apply false

    // Kotlin plugin prefers to be applied to parent when it's used in multiple sub-modules.
    kotlin("jvm") version "1.8.21" apply false
    alias(libs.plugins.spotless)
}

group = "com.noxcrew.interfaces"
version = "1.0.0"

description = "A Kotlin Minecraft user-interface library."

val noxcrewRepository: String = "https://maven.noxcrew.com/public"
val javaVersion: Int = 17

subprojects {
    apply(plugin = "kotlin")
    apply<IndraPlugin>()
    apply<SpotlessPlugin>()

    // Don't publish examples
    if (name != "examples") {
        apply<IndraPublishingPlugin>()

        configure<PublishingExtension> {
            repositories {
                maven {
                    name = "noxcrew-public"
                    url = uri(noxcrewRepository)
                    credentials {
                        username = System.getenv("NOXCREW_MAVEN_PUBLIC_USERNAME")
                        password = System.getenv("NOXCREW_MAVEN_PUBLIC_PASSWORD")
                    }
                    authentication {
                        create<BasicAuthentication>("basic")
                    }
                }
            }
        }
    }

    repositories {
        mavenCentral()
        maven("https://papermc.io/repo/repository/maven-public/")
    }

    dependencies {
        compileOnlyApi(rootProject.libs.checker.qual)
    }

    indra {
        mitLicense()

        javaVersions {
            minimumToolchain(javaVersion)
            target(javaVersion)
        }

        publishAllTo("noxcrew-public", noxcrewRepository)

        github("noxcrew", "interfaces-kotlin") {
            ci(true)
        }

        configurePublications {
            pom {
                developers {
                    developer {
                        id.set("noxcrew")
                        email.set("contact@noxcrew.com")
                    }
                }
            }
        }
    }

    configure<SpotlessExtension> {
        kotlin {
            ktlint("0.47.1")
        }
    }

    // Configure any existing RunServerTasks
    tasks.withType<RunServer> {
        minecraftVersion("1.19.4")
        jvmArgs("-Dio.papermc.paper.suppress.sout.nags=true")
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = javaVersion.toString()
            freeCompilerArgs += listOf("-Xexplicit-api=strict")
        }
    }
}
