plugins {
    `java-library`
    `maven-publish`
    `signing`
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

java {
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("mavenJava") {
            from(components["java"])
            suppressPomMetadataWarningsFor("sourcesElements")
            suppressPomMetadataWarningsFor("javadocElements")
            pom {
                name.set("barista")
                description.set("an opinionated java server library.")
                url.set("https://github.com/markelliot/barista")
                licenses {
                    license {
                        name.set("Apache License, Version 2.0")
                        url.set("https://www.apache.org/licenses/LICENSE-2.0")
                    }
                }
                developers {
                    developer {
                        id.set("markelliot")
                        name.set("Mark Elliot")
                        email.set("markelliot@users.noreply.github.com")
                    }
                }
                scm {
                    connection.set("scm:git:https://github.com/markelliot/barista.git")
                    developerConnection.set("scm:git:https://github.com/markelliot/barista.git")
                    url.set("https://github.com/markelliot/barista")
                }
            }
        }
    }
    repositories {
        maven {
            name = "staging"
            url = uri(rootProject.layout.buildDirectory.dir("staging-deploy"))
        }
    }
}

configure<SigningExtension> {
    val key = System.getenv("SIGNING_SECRET_KEY")
    val password = System.getenv("SIGNING_PASSWORD")
    val publishing: PublishingExtension by project
    useInMemoryPgpKeys(key, password)
    sign(publishing.publications)
}
