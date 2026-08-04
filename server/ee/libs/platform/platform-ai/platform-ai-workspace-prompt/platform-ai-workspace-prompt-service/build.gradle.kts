dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    // Resolves a job principal id (project deployment id) to its workspace for WorkspaceSystemPromptAdvisorProviderImpl.
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    // Implements the CE SPI seam (WorkspaceSystemPromptAdvisorProvider) so non-EE components can obtain this advisor.
    api(project(":server:libs:platform:platform-ai:platform-ai-api"))
    // WorkspaceSystemPromptAdvisor implements Spring AI's CallAdvisor/StreamAdvisor and takes
    // ChatClientRequest types on its public surface, so callers registering it need these transitively too.
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.ai:spring-ai-model")

    api(project(":server:ee:libs:platform:platform-ai:platform-ai-workspace-prompt:platform-ai-workspace-prompt-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
