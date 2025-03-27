dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:0.7.0")
    implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.7.0")
    implementation("org.springframework.ai:spring-ai-mcp:${rootProject.libs.versions.spring.ai.get()}")
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:embedded:embedded-execution:embedded-execution-api"))
}
