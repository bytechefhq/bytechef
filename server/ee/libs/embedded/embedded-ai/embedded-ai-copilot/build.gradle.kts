dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))
    implementation(project(":spring-ai:spring-ag-ui:packages:server"))
    implementation(project(":spring-ai:spring-ag-ui:integrations:spring-ai"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-api"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-service"))
    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-tool"))
    implementation(project(":server:libs:ai:ai-mcp:ai-mcp-server-api"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-tool"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-tool"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:ee:libs:embedded:embedded-ai:embedded-ai-tool"))

    // CopilotAgentType (ai-copilot-tool) implements AgentType, which ai-copilot-tool depends on as
    // implementation-only; expose it here for EmbeddedIntelligentToolContributorConfiguration's CopilotAgentType.key() calls
    // and for tests that assert against them.
    implementation(project(":server:libs:ai:ai-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
}
