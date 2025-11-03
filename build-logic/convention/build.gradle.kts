import org.jetbrains.kotlin.gradle.dsl.JvmTarget
plugins {
    `kotlin-dsl`
}

group = "io.github.wooongyee.komvi.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.kover.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "komvi.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "komvi.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "komvi.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("kover") {
            id = "komvi.kover"
            implementationClass = "KoverConventionPlugin"
        }
        register("kotlinLibraryPublishing") {
            id = "komvi.kotlin.library.publishing"
            implementationClass = "KotlinLibraryPublishingConventionPlugin"
        }
        register("androidLibraryPublishing") {
            id = "komvi.android.library.publishing"
            implementationClass = "AndroidLibraryPublishingConventionPlugin"
        }
    }
}
