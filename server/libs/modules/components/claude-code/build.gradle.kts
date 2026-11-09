version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(libs.org.springaicommunity.agents.agent.claude)
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-workspace"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    testImplementation(project(":server:libs:core:commons:commons-util"))
}
