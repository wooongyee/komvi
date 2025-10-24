plugins {
    id("komvi.android.library")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.wooongyee.komvi.compose"

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":komvi-core"))

    api(platform(libs.compose.bom))
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.lifecycle.runtime.compose)
}