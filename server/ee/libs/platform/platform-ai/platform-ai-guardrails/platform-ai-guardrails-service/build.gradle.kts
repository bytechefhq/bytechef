dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("io.micrometer:micrometer-core")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    // Resolves a job principal id (project deployment id) to its workspace for AiGuardrailsAdvisorProviderImpl.
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    // Provides the injection-classifier SPI + exception (AiGatewayInjectionClassifier, AiGatewayGuardrailException) and
    // the model/provider/chat-model-factory types the prompt-based classifiers call through.
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-gateway:platform-ai-gateway-api"))
    // Implements the CE SPI seam (AiGuardrailsAdvisorProvider) so non-EE components can obtain this advisor.
    api(project(":server:libs:platform:platform-ai:platform-ai-api"))
    // AiGuardrailsAdvisor implements Spring AI's CallAdvisor/StreamAdvisor and takes ChatClientRequest/ChatResponse
    // types on its public surface, so callers registering it need these transitively too.
    api("org.springframework.ai:spring-ai-client-chat")
    api("org.springframework.ai:spring-ai-model")

    api(project(":server:ee:libs:platform:platform-ai:platform-ai-guardrails:platform-ai-guardrails-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation(project(":server:libs:test:test-support"))
}
