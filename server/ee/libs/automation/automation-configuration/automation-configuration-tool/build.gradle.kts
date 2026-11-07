dependencies {
    implementation("org.springframework.ai:spring-ai-model")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-configuration:platform-custom-component-configuration-api"))
    implementation(project(":server:ee:libs:platform:platform-custom-component:platform-custom-component-tool"))
    implementation(project(":server:libs:ai:ai-mcp:ai-mcp-server-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:exception:exception-api"))
    testImplementation("org.springframework:spring-webflux")
}
