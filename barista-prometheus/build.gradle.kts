plugins {
    id("barista.java-conventions")
}

dependencies {
    compileOnlyApi("io.undertow:undertow-core")
    api("io.prometheus:prometheus-metrics-core")
    implementation("io.prometheus:prometheus-metrics-exporter-common")
}
