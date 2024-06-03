plugins {
    alias(libs.plugins.shadow)
    alias(libs.plugins.run.paper)
}

dependencies {
    implementation(project(":interfaces"))
    implementation(libs.cloud.paper)
    implementation(libs.cloud.kotlin.extensions)
    implementation(libs.cloud.kotlin.coroutines)

    compileOnly(libs.paper.api)
}
