dependencies {
    implementation(project(":server:libs:modules:components:ai:agent:guardrails"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-sensitive-data:platform-ai-sensitive-data-service"))

    testImplementation("org.springframework.ai:spring-ai-client-chat")
    testImplementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
}
