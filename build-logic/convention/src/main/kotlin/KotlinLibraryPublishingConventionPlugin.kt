import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.get
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.register

class KotlinLibraryPublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("maven-publish")
            }

            val sourcesJar = tasks.register<Jar>("sourcesJar") {
                archiveClassifier.set("sources")
                from(project.extensions.getByName("sourceSets").getProperty("main").getProperty("allSource"))
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
