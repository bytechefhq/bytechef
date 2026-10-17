version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-api"))
}
