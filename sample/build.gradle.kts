plugins {
    id("komvi.android.application")
}

android {
    namespace = "io.github.wooongyee.komvi.sample"

    defaultConfig {
        applicationId = "io.github.wooongyee.komvi.sample"
        versionCode = 1
        versionName = "1.0"
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}