plugins {
    id("komvi.android.library")
    id("komvi.android.library.publishing")
    id("komvi.kover")
    id("kotlin-parcelize")
}

android {
    namespace = "io.github.wooongyee.komvi.android"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(project(":komvi-core"))
    api(project(":komvi-annotations"))
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.lifecycle.viewmodel.savedstate)
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.turbine)
    testImplementation(libs.lifecycle.runtime.testing)
}