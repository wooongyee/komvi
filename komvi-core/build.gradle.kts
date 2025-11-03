plugins {
    id("komvi.kotlin.library")
    id("komvi.kotlin.library.publishing")
    id("komvi.kover")
}

dependencies {
    api(libs.kotlinx.coroutines.core)

    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.kotlin.test)
    testImplementation(libs.turbine)
}
