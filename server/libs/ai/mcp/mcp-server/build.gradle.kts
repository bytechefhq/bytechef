dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.14.0")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.ai:spring-ai-mcp")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.security:spring-security-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-automation-impl"))
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-platform"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
