plugins {
    `java-library`
    `maven-publish`
    `signing`
    id("org.jreleaser")
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
            url = uri(layout.buildDirectory.dir("staging-deploy"))
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

jreleaser {
    signing {
        active.set(org.jreleaser.model.Active.ALWAYS)
        armored.set(true)
        publicKey.set(System.getenv("SIGNING_PUBLIC_KEY"))
        secretKey.set(System.getenv("SIGNING_SECRET_KEY"))
        passphrase.set(System.getenv("SIGNING_PASSWORD"))
    }
    deploy {
        maven {
            mavenCentral {
                create("sonatype") {
                    active.set(org.jreleaser.model.Active.ALWAYS)
                    url.set("https://central.sonatype.com/api/v1/publisher")
                    username.set(System.getenv("SONATYPE_USERNAME"))
                    password.set(System.getenv("SONATYPE_PASSWORD"))
                    stagingRepository("build/staging-deploy")

                    // wait 30s between status checks
                    retryDelay.set(30)

                    // up to 100 status checks
                    maxRetries.set(100)
                }
            }
        }
    }
}