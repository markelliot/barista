plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jreleaser:org.jreleaser.gradle.plugin:1.18.0")
}
