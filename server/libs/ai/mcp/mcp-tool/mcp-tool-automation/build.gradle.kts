dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:ee:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:exception:exception-api"))
    implementation(project(":server:libs:platform:platform-mcp:platform-mcp-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-api"))
}
