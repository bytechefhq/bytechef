dependencies {
    implementation("io.micrometer:micrometer-core")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("tools.jackson.core:jackson-databind")

    api(project(":server:ee:libs:platform:platform-ai:platform-ai-llm-usage:platform-ai-llm-usage-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
