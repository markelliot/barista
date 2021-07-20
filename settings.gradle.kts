rootProject.name = "barista-root"

include("barista")

pluginManagement {
    repositories {
        maven { url = uri("https://dl.bintray.com/palantir/releases/") }
        jcenter()
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.palantir.consistent-versions") {
                useModule("com.palantir.gradle.consistentversions:gradle-consistent-versions:${requested.version}")
            }
        }
    }
}
