import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getByType
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.register

class KotlinLibraryPublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("maven-publish")
            }

            afterEvaluate {
                val sourceSets = extensions.getByType<SourceSetContainer>()

                val sourcesJar = tasks.register<Jar>("sourcesJar") {
                    archiveClassifier.set("sources")
                    from(sourceSets["main"].allSource)
                }

                extensions.configure<PublishingExtension> {
                    publications {
                        create<MavenPublication>("release") {
                            from(components["java"])
                            artifact(sourcesJar)

                            groupId = property("library.group").toString()
                            artifactId = project.name
                            version = property("library.version").toString()
                        }
                    }
                }
            }
        }
    }
}
