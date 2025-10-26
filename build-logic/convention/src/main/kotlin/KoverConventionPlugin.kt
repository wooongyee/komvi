import kotlinx.kover.gradle.plugin.dsl.KoverProjectExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

class KoverConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("org.jetbrains.kotlinx.kover")

            extensions.configure<KoverProjectExtension> {
                reports {
                    filters {
                        excludes {
                            packages("*.sample", "*.test")
                            annotatedBy("*Generated*")
                        }
                    }

                    verify {
                        rule("Minimum coverage") {
                            disabled.set(true)  // TODO: Enable after writing comprehensive tests
                            bound {
                                minValue.set(when (project.name) {
                                    "komvi-core" -> 90
                                    "komvi-android", "komvi-compose" -> 80
                                    "komvi-processor" -> 70
                                    else -> 60
                                })
                            }
                        }
                    }
                }
            }
        }
    }
}
