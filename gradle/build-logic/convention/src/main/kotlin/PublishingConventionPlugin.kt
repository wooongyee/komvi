import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.create

class PublishingConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            with(pluginManager) {
                apply("maven-publish")
            }

            val libraryGroup = property("library.group").toString()
            val libraryVersion = property("library.version").toString()

            extensions.configure<PublishingExtension> {
                publications {
                    create<MavenPublication>("release") {
                        groupId = libraryGroup
                        artifactId = project.name
                        version = libraryVersion

                        // Android 라이브러리 또는 Java 라이브러리에 따라 컴포넌트 설정
                        afterEvaluate {
                            if (pluginManager.hasPlugin("com.android.library")) {
                                from(components["release"])
                            } else if (pluginManager.hasPlugin("java-library")) {
                                from(components["java"])
                            }
                        }

                        pom {
                            name.set(project.name)
                            description.set("Komvi MVI library for Android")
                            url.set("https://github.com/wooongyee/komvi")

                            licenses {
                                license {
                                    name.set("The Apache License, Version 2.0")
                                    url.set("http://www.apache.org/licenses/LICENSE-2.0.txt")
                                }
                            }

                            developers {
                                developer {
                                    id.set("wooongyee")
                                    name.set("Wooongyee")
                                }
                            }

                            scm {
                                connection.set("scm:git:git://github.com/wooongyee/komvi.git")
                                developerConnection.set("scm:git:ssh://github.com/wooongyee/komvi.git")
                                url.set("https://github.com/wooongyee/komvi")
                            }
                        }
                    }
                }
            }
        }
    }
}
