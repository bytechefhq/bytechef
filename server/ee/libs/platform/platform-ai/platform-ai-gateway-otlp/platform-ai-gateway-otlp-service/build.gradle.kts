dependencies {
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway-otlp:platform-ai-gateway-otlp-api"))

    implementation("io.micrometer:micrometer-core")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
