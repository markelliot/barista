plugins {
    `java-library`
    `maven-publish`
    `signing`
}

dependencies {
    api(project(":barista"))

    // required Conjure runtime elements
    api("com.palantir.conjure.java:conjure-java-undertow-runtime")

    testImplementation(platform("org.junit:junit-bom"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")
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
}

configure<SigningExtension> {
    val key = System.getenv("SIGNING_KEY")
    val password = System.getenv("SIGNING_PASSWORD")
    val publishing: PublishingExtension by project
    useInMemoryPgpKeys(key, password)
    sign(publishing.publications)
}
