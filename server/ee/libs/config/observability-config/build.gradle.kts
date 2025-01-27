dependencies {
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation(libs.loki.logback.appender)
    implementation("io.micrometer:micrometer-tracing-bridge-otel")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
