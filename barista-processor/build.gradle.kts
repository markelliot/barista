plugins {
    `java-library`
    `maven-publish`
    `signing`
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service")
    compileOnly("com.google.auto.service:auto-service")

    implementation(project(":barista"))
    implementation(project(":barista-annotations"))
    implementation("com.google.googlejavaformat:google-java-format")
    implementation("com.google.guava:guava")
    implementation("com.markelliot.result:result")
    implementation("com.squareup:javapoet")

    testAnnotationProcessor(project(":barista-processor"))
    // TODO(markelliot): figure out why the rest of these are necessary for intellij
    // (for now, keep in sync with implementation deps)
    testAnnotationProcessor(project(":barista"))
    testAnnotationProcessor(project(":barista-annotations"))
    testAnnotationProcessor("com.google.googlejavaformat:google-java-format")
    testAnnotationProcessor("com.google.guava:guava")
    testAnnotationProcessor("com.markelliot.result:result")
    testAnnotationProcessor("com.squareup:javapoet")

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
