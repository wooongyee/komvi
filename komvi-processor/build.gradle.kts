plugins {
    id("komvi.kotlin.library")
}

dependencies {
    implementation(project(":komvi-annotations"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)
}
