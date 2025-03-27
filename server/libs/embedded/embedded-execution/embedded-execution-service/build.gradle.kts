dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:0.7.0")
    implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.7.0")
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.ai:spring-ai-mcp:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
    implementation(project(":server:libs:embedded:embedded-execution:embedded-execution-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
