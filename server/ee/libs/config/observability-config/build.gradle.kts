dependencies {
    implementation("ch.qos.logback:logback-core")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.24.0-alpha")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
