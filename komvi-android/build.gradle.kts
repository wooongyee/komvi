plugins {
    id("komvi.android.library")
    id("komvi.kover")
}

android {
    namespace = "io.github.wooongyee.komvi.android"
}

dependencies {
    api(project(":komvi-core"))
    api(libs.lifecycle.viewmodel.ktx)
    api(libs.kotlinx.coroutines.android)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.turbine)
    testImplementation(libs.lifecycle.runtime.testing)
}