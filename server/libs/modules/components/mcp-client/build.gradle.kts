version = "1.0"

dependencies {
    implementation("io.modelcontextprotocol.sdk:mcp:${libs.versions.io.modelcontextprotocol.sdk.get()}")
    implementation("org.springframework.ai:spring-ai-mcp")
    implementation(project(":server:libs:ai:ai-tool-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
}
