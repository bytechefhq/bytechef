dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("io.modelcontextprotocol.sdk:mcp:0.10.0")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:ai:mcp:mcp-commons"))
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-platform"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
