dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("io.modelcontextprotocol.sdk:mcp:0.10.0")
    implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.10.0")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.ai:spring-ai-mcp")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.security:spring-security-web")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:mcp:mcp-tool"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
}
