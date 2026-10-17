dependencies {
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.ai:spring-ai-commons")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:platform:platform-api"))

    testImplementation(project(":server:libs:test:test-support"))
    testImplementation("org.testcontainers:postgresql")
}
