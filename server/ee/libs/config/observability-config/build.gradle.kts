dependencies {
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation(libs.io.zipkin.contrib.otel.encoder.brave)
    implementation(libs.loki.logback.appender)
    implementation("org.springframework:spring-context")
}
