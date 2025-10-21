plugins {
    id("komvi.android.library")
    id("komvi.publishing")
}

android {
    namespace = "io.github.wooongyee.komvi.android"
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}