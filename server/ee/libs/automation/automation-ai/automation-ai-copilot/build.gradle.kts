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
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-tool"))
}
