rootProject.name = "barista-root"

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.10.0"
}

include("barista")
include("barista-annotations")
include("barista-conjure")
include("barista-processor")
