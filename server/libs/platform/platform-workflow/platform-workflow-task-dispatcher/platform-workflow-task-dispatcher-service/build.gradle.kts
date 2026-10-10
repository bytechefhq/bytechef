dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))
}
