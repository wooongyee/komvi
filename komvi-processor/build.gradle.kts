plugins {
    id("komvi.kotlin.library")
    id("komvi.kover")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
    if (name.contains("Test")) {
        compilerOptions {
            freeCompilerArgs.add("-opt-in=org.jetbrains.kotlin.compiler.plugin.ExperimentalCompilerApi")
        }
    }
}

dependencies {
    implementation(project(":komvi-annotations"))
    implementation(libs.ksp.api)
    implementation(libs.kotlinpoet)
    implementation(libs.kotlinpoet.ksp)

    testImplementation(libs.kotlin.test)
    testImplementation(libs.kotlin.compile.testing)
    testImplementation(libs.kotlin.compile.testing.ksp)
    testImplementation(project(":komvi-core"))
    testImplementation(project(":komvi-annotations"))
}
