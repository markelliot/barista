plugins {
    `java-library`
    `maven-publish`
    `signing`
}

dependencies {
    implementation("com.auth0:java-jwt")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-guava")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    implementation("com.google.guava:guava")
    implementation("com.markelliot.barista.tracing:barista-tracing")
    implementation("io.undertow:undertow-core")

    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-jul")
    implementation("org.apache.logging.log4j:log4j-slf4j-impl")
    // users of this library should have access to the slf4j api for their own logging
    api("org.slf4j:slf4j-api")

    // register @Plugin annotation processor
    annotationProcessor("org.apache.logging.log4j:log4j-core")

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
