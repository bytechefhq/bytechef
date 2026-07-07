dependencies {
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":spring-ai:spring-ag-ui:packages:core"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))

    implementation(project(":server:libs:ai:ai-copilot:ai-copilot-api"))

    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-api"))
}
