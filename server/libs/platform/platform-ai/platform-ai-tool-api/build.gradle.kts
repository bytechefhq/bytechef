dependencies {
    api(project(":server:libs:automation:automation-mcp:automation-mcp-api"))
    api(project(":server:libs:platform:platform-component:platform-component-api"))
    api(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    api(project(":server:libs:platform:platform-mcp:platform-mcp-api"))

    api("org.springframework:spring-expression")

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.ai:spring-ai-model")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-api"))

    testImplementation(project(":server:libs:core:evaluator:evaluator-impl"))
    testImplementation(project(":server:libs:test:test-support"))
}
