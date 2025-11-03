plugins {
    id("komvi.android.library")
    id("komvi.android.library.publishing")
    id("komvi.kover")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "io.github.wooongyee.komvi.compose"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

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

    // No tests needed - simple wrapper functions
}