import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import xyz.jpenilla.runpaper.task.RunServer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import io.papermc.paperweight.userdev.PaperweightUser
import io.papermc.paperweight.userdev.PaperweightUserDependenciesExtension

plugins {
    alias(libs.plugins.run.paper) apply false
    alias(libs.plugins.paper.userdev) apply false

    // Kotlin plugin prefers to be applied to parent when it's used in multiple sub-modules.
    kotlin("jvm") version "2.4.0" apply false

    alias(libs.plugins.spotless)
}

val javaVersion: Int = 25

allprojects {
    group = "com.noxcrew.interfaces"
    version = "2.2.0-SNAPSHOT"

    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply(plugin = "java-library")
    apply<SpotlessPlugin>()

    // Apply paperweight outside the API module
    if (name != "api") {
        apply<PaperweightUser>()

        dependencies {
            extensions.findByType<PaperweightUserDependenciesExtension>()?.paperDevBundle("26.2.build.65-beta")
        }
    }
    if (name != "examples") {
        apply(plugin = "maven-publish")
    }

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    configure<SpotlessExtension> {
        kotlin {
            ktlint("1.5.0")
            suppressLintsFor {
                step = "ktlint"
                shortCode = "standard:package-name"
            }
            suppressLintsFor {
                step = "ktlint"
                shortCode = "standard:annotation"
            }
            suppressLintsFor {
                step = "ktlint"
                shortCode = "standard:property-naming"
            }
        }
    }

    tasks.withType<RunServer> {
        minecraftVersion("26.2")
        jvmArgs("-Dio.papermc.paper.suppress.sout.nags=true")
    }

    tasks.withType<KotlinCompile> {
        explicitApiMode.set(ExplicitApiMode.Strict)

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        }
    }

    if (name != "examples") {
        val noxcrewRepository = "https://maven.noxcrew.com/public"

        configure<JavaPluginExtension> {
            withJavadocJar()
            withSourcesJar()
        }

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
            publications {
                create<MavenPublication>("maven") {
                    from(components["java"])
                    pom {
                        name = "interfaces-kotlin"
                        description = "A Kotlin Minecraft user-interface library."
                        url = "https://github.com/Noxcrew/interfaces-kotlin"
                        scm {
                            url = "https://github.com/Noxcrew/interfaces-kotlin"
                            connection = "scm:git:https://github.com/Noxcrew/interfaces-kotlin.git"
                            developerConnection = "scm:git:https://github.com/Noxcrew/interfaces-kotlin.git"
                        }
                        licenses {
                            license {
                                name = "MIT License"
                                url = "https://opensource.org/licenses/MIT"
                            }
                        }
                        developers {
                            developer {
                                id = "noxcrew"
                                name = "Noxcrew"
                                email = "contact@noxcrew.com"
                            }
                        }
                    }
                }
            }
        }
    }
}
