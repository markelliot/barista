plugins {
    `java-library`
    `maven-publish`
    signing
}

// apply from: "${rootDir}/gradle/generate-test-certs.gradle"

dependencies {
    api("com.palantir.tokens:auth-tokens")
    api("jakarta.servlet:jakarta.servlet-api")
    api("jakarta.ws.rs:jakarta.ws.rs-api")
    api("com.palantir.dialogue:dialogue-target")

    implementation("com.fasterxml.jackson.core:jackson-annotations")
    implementation("com.fasterxml.jackson.core:jackson-core")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.palantir.conjure.java.runtime:conjure-java-jackson-optimizations")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("com.google.code.findbugs:jsr305")
    implementation("com.google.errorprone:error_prone_annotations")
    implementation("com.google.guava:guava")
    implementation("com.palantir.dialogue:dialogue-clients")
    implementation("com.palantir.safe-logging:safe-logging")
    implementation("org.apache.httpcomponents.core5:httpcore5")
    implementation("org.slf4j:slf4j-api")
    implementation("io.undertow:undertow-core")
    implementation("io.undertow:undertow-servlet") {
        // Use jakarta.servlet:jakarta.servlet-api instead
        exclude(group = "org.jboss.spec.javax.servlet", module = "jboss-servlet-api_4.0_spec")
        exclude(group = "org.jboss.spec.javax.annotation", module = "jboss-annotations-api_1.2_spec")
    }
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.apache.commons:commons-lang3")
    implementation(project(":barista-annotations"))
    implementation(project(":barista"))

    testImplementation(platform("org.junit:junit-bom"))
    testImplementation("org.junit.jupiter:junit-jupiter-engine")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")

    annotationProcessor(project(":barista-processor"))

    annotationProcessor("org.immutables:value")
    compileOnly("org.immutables:value::annotations")

    annotationProcessor("com.palantir.dialogue:dialogue-annotations-processor")
    api("com.palantir.dialogue:dialogue-annotations")
}
