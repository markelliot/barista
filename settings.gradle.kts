rootProject.name = "barista-root"

include("barista")

pluginManagement {
    repositories {
        maven {
            url = uri("https://maven.pkg.github.com/markelliot/gradle-versions")
            credentials {
                username = System.getenv("GITHUB_ACTOR")
                password = System.getenv("GH_READ_PACKAGES_TOKEN")
            }
        }
        gradlePluginPortal()
    }

    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.markelliot.versions") {
                 useModule("com.markelliot.gradle.versions:gradle-versions:${requested.version}")
            }
        }
    }
}
