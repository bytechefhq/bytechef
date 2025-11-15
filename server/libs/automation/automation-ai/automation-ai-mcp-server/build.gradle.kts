dependencies {
    compileOnly("jakarta.servlet:jakarta.servlet-api")

    implementation("io.modelcontextprotocol.sdk:mcp-spring-webmvc:0.14.0")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.ai:spring-ai-mcp")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:automation:automation-execution:automation-execution-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-mcp:platform-mcp-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
}
