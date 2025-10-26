plugins {
    id("komvi.kotlin.library")
    id("komvi.kover")
}

dependencies {
    implementation(project(":komvi-annotations"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.kotlin.test)
}
