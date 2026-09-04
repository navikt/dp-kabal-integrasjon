plugins {
    id("common")
    application
    alias(libs.plugins.shadow.jar)
}

dependencies {
    val dpBibliotekerVersion = "2026.09.04-18.18.19debaae7137"
    implementation(libs.rapids.and.rivers)
    implementation("io.prometheus:prometheus-metrics-core:1.8.0")

    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
    implementation(libs.bundles.ktor.client)
    implementation("com.fasterxml.uuid:java-uuid-generator:5.2.0")
    implementation("io.ktor:ktor-serialization-jackson:${libs.versions.ktor.get()}")
    implementation("no.nav.dagpenger:ktor-client-metrics:$dpBibliotekerVersion")
    implementation("no.nav.dagpenger:oauth2-klient:$dpBibliotekerVersion")

    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.mockk)
    testImplementation(libs.bundles.kotest.assertions)
    testImplementation(libs.bundles.naisful.rapid.and.rivers.test)
}

application {
    mainClass.set("no.nav.dagpenger.klageinstans.AppKt")
}
