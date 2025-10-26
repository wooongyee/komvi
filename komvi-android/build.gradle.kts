plugins {
    id("komvi.android.library")
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
}