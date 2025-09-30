rootProject.name = "barista-root"

plugins {
    // Apply the foojay-resolver plugin to allow automatic download of JDKs
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include("barista")
include("barista-annotations")
include("barista-conjure")
include("barista-lifecycle")
include("barista-processor")
include("barista-prometheus")
