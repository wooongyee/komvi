plugins {
    id("komvi.android.application")
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.ksp)
    id("kotlin-parcelize")
}

android {
    namespace = "io.github.wooongyee.komvi.sample"
    compileSdk = property("android.compileSdk").toString().toInt()

    defaultConfig {
        applicationId = "io.github.wooongyee.komvi.sample"
        targetSdk = property("android.compileSdk").toString().toInt()
        versionCode = 1
        versionName = "1.0"
    }

    buildFeatures {
        compose = true
    }
}

dependencies {
    // Komvi libraries
    implementation(project(":komvi-core"))
    implementation(project(":komvi-android"))
    implementation(project(":komvi-compose"))
    implementation(project(":komvi-annotations"))
    ksp(project(":komvi-processor"))

    // Compose BOM
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.runtime)

    // Material3 & other Compose
    val composeBom = platform(libs.compose.bom)
    implementation(composeBom)
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // AndroidX
    implementation(libs.androidx.core.ktx)
    implementation(libs.lifecycle.viewmodel.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.activity:activity-compose:1.9.3")
}