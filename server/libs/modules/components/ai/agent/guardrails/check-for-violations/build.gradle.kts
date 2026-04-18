dependencies {
    implementation(project(":server:libs:modules:components:ai:agent:guardrails"))
    implementation("org.springframework.ai:spring-ai-client-chat")

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation(project(":server:libs:platform:platform-component:platform-component-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
    testImplementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
}
