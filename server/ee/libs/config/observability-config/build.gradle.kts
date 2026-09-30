dependencies {
    // opentelemetry.version is defined in the root gradle.properties; the platform lifts the Spring Boot BOM managed
    // version on the compile classpath of this module and of the modules depending on it
    implementation(platform("io.opentelemetry:opentelemetry-bom:${property("opentelemetry.version")}"))

    implementation("ch.qos.logback:logback-core")
    implementation("jakarta.servlet:jakarta.servlet-api")
    implementation("io.micrometer:micrometer-core")
    implementation("io.micrometer:micrometer-tracing")
    implementation("io.opentelemetry:opentelemetry-api")
    implementation("io.opentelemetry:opentelemetry-common")
    implementation("io.opentelemetry:opentelemetry-exporter-otlp")
    implementation("io.opentelemetry:opentelemetry-sdk-common")
    implementation("io.opentelemetry.instrumentation:opentelemetry-logback-appender-1.0:2.24.0-alpha")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-opentelemetry")
    implementation("software.amazon.awssdk:auth")
    implementation(project(":server:libs:config:app-config"))

    testImplementation("io.opentelemetry:opentelemetry-sdk-testing")
    testImplementation("software.amazon.awssdk:http-auth-aws")
}
