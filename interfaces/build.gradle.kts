plugins {
    id("maven-publish")
    id("java-library")
}

dependencies {
    compileOnlyApi(libs.adventure.api)
    compileOnlyApi(libs.paper.api) {
        isTransitive = false
    }
    compileOnlyApi(libs.guava)

    api(libs.kotlin.coroutines)
    api(libs.slf4j)
    api(libs.caffeine)
}

val noxcrewRepository: String = "https://maven.noxcrew.com/public"

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
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
