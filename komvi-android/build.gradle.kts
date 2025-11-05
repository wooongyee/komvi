plugins {
    id("komvi.android.library")
    id("komvi.android.library.publishing")
    id("komvi.kover")
    id("kotlin-parcelize")
}

android {
    namespace = "com.github.wooongyee.komvi.android"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }
}

dependencies {
    api(project(":komvi-core"))
    api(project(":komvi-annotations"))
    api(libs.androidx.lifecycle.viewmodel.ktx)
    api(libs.androidx.lifecycle.viewmodel.savedstate)
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.turbine)
    testImplementation(libs.androidx.lifecycle.runtime.testing)
}