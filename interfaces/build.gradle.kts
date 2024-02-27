plugins {
    alias(libs.plugins.dokka)
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
