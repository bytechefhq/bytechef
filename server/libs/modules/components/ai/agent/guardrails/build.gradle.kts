dependencies {
    compileOnly("com.github.spotbugs:spotbugs-annotations")
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.springframework.ai:spring-ai-client-chat")

    testImplementation("ch.qos.logback:logback-classic")
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework.ai:spring-ai-client-chat")
    testImplementation("org.springframework.ai:spring-ai-retry")
    testImplementation(project(":server:libs:modules:components:ai:agent:guardrails:check-for-violations"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
    testImplementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
}
