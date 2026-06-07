dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.ai:spring-ai-client-chat")
    implementation("org.springframework.ai:spring-ai-model")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:ee:libs:ai:mcp:mcp-tool:mcp-tool-api"))
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-api"))
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework:spring-webflux")
}
