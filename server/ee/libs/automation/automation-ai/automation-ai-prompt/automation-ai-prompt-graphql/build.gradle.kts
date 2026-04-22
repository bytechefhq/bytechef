dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-observability:platform-ai-observability-service"))

    api(project(":server:ee:libs:automation:automation-ai:automation-ai-prompt:automation-ai-prompt-api"))
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-prompt:platform-ai-prompt-api"))
}
