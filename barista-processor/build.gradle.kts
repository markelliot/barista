plugins {
    id("barista.java-conventions")
}

dependencies {
    annotationProcessor("com.google.auto.service:auto-service")
    compileOnly("com.google.auto.service:auto-service")

    implementation(project(":barista"))
    implementation(project(":barista-annotations"))
    implementation("com.google.googlejavaformat:google-java-format")
    implementation("com.google.guava:guava")
    implementation("com.markelliot.result:result")
    implementation("com.palantir.javapoet:javapoet")

    testAnnotationProcessor(project(":barista-processor"))
    // TODO(markelliot): figure out why the rest of these are necessary for intellij
    // (for now, keep in sync with implementation deps)
    testAnnotationProcessor(project(":barista"))
    testAnnotationProcessor(project(":barista-annotations"))
    testAnnotationProcessor("com.google.googlejavaformat:google-java-format")
    testAnnotationProcessor("com.google.guava:guava")
    testAnnotationProcessor("com.markelliot.result:result")
    testAnnotationProcessor("com.palantir.javapoet:javapoet")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core")
}
