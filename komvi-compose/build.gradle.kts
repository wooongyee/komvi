plugins {
    id("komvi.android.library")
}

android {
    namespace = "io.github.wooongyee.komvi.compose"

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.15"
    }
}

dependencies {
    api(project(":komvi-core"))

    api(platform(libs.compose.bom))
    api(libs.compose.runtime)
    api(libs.compose.ui)
    api(libs.lifecycle.runtime.compose)
}