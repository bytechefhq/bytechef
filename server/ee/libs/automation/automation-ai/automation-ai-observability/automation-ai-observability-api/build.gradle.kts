dependencies {
    api("org.springframework.data:spring-data-commons")
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway-otlp:platform-ai-gateway-otlp-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api"))

    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
}
