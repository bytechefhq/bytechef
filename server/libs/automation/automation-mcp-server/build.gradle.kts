dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:0.10.0")
    implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.10.0")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.ai:spring-ai-mcp")
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
}
