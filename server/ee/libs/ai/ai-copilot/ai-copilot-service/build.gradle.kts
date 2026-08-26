dependencies {
    implementation("io.micrometer:micrometer-core")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))

    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-api"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-tool"))

    implementation(project(":server:ee:libs:ai:ai-copilot:ai-copilot-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
