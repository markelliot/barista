plugins {
    id("barista.java-conventions")
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
    implementation("com.markelliot.result:result")

    // users of this library may need to implement HttpHandler
    api("io.undertow:undertow-core")

    implementation("org.apache.logging.log4j:log4j-core")
    implementation("org.apache.logging.log4j:log4j-jul")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl")
    // users of this library should have access to the slf4j api for their own logging
    api("org.slf4j:slf4j-api")

    // register @Plugin annotation processor
    annotationProcessor("org.apache.logging.log4j:log4j-core")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core")
}
