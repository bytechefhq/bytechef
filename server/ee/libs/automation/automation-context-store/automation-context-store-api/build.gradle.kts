dependencies {
    api("org.springframework.ai:spring-ai-model")
    api(project(":server:ee:libs:platform:platform-context-store:platform-context-store-api"))
    api(project(":server:libs:automation:automation-ai:automation-ai-mcp-server-api"))

    implementation("org.springframework.data:spring-data-jdbc")
}
