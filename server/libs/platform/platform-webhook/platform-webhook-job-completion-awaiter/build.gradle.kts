dependencies {
    implementation("com.github.ben-manes.caffeine:caffeine")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
}
