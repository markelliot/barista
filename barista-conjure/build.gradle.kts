plugins {
    id("barista.java-conventions")
}

dependencies {
    api(project(":barista"))

    // required Conjure runtime elements
    api("com.palantir.conjure.java:conjure-java-undertow-runtime")

    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.assertj:assertj-core")
}
