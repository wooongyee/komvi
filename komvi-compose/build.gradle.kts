plugins {
    id("komvi.android.library")
    id("komvi.android.library.publishing")
    id("komvi.kover")
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.github.wooongyee.komvi.compose"

    defaultConfig {
        consumerProguardFiles("consumer-rules.pro")
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":komvi-core"))
    api(platform(libs.androidx.compose.bom))
    api(libs.androidx.compose.runtime)
    api(libs.androidx.compose.ui)

}