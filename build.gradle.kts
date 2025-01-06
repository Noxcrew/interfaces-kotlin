import com.diffplug.gradle.spotless.SpotlessExtension
import com.diffplug.gradle.spotless.SpotlessPlugin
import org.jetbrains.kotlin.gradle.dsl.ExplicitApiMode
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import xyz.jpenilla.runpaper.task.RunServer
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    alias(libs.plugins.run.paper) apply false

    // Kotlin plugin prefers to be applied to parent when it's used in multiple sub-modules.
    kotlin("jvm") version "2.1.0" apply false
    alias(libs.plugins.spotless)
}

val javaVersion: Int = 21

allprojects {
    group = "com.noxcrew.interfaces"
    version = "1.4.0-SNAPSHOT"

    tasks.withType<JavaCompile> {
        sourceCompatibility = javaVersion.toString()
        targetCompatibility = javaVersion.toString()
    }
}

subprojects {
    apply(plugin = "kotlin")
    apply<SpotlessPlugin>()

    repositories {
        mavenCentral()
        maven("https://repo.papermc.io/repository/maven-public/")
    }

    configure<SpotlessExtension> {
        kotlin {
            ktlint("1.5.0")
        }
    }

    // Configure any existing RunServerTasks
    tasks.withType<RunServer> {
        minecraftVersion("1.21.4")
        jvmArgs("-Dio.papermc.paper.suppress.sout.nags=true")
    }

    tasks.withType<KotlinCompile> {
        explicitApiMode.set(ExplicitApiMode.Strict)

        compilerOptions {
            jvmTarget.set(JvmTarget.fromTarget(javaVersion.toString()))
        }
    }
}
