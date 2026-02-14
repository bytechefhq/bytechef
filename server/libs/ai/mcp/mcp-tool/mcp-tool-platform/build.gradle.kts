dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-web")
    implementation(project(":server:libs:ai:mcp:mcp-tool:mcp-tool-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-validator"))
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
}
