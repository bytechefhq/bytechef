dependencies {
    implementation("io.micrometer:micrometer-registry-prometheus")
    implementation("io.micrometer:micrometer-tracing-bridge-brave")
    implementation("io.zipkin.contrib.otel:encoder-brave:0.1.0")
    implementation(libs.loki.logback.appender)
    implementation("org.springframework:spring-context")
}
