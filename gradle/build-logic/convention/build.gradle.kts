plugins {
    `kotlin-dsl`
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("androidLibrary") {
            id = "komvi.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("kotlinLibrary") {
            id = "komvi.kotlin.library"
            implementationClass = "KotlinLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "komvi.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("publishing") {
            id = "komvi.publishing"
            implementationClass = "PublishingConventionPlugin"
        }
    }
}
