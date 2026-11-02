dependencies {
    api(project(":server:libs:automation:automation-ai:automation-ai-auto-memory:automation-ai-auto-memory-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-config")
    testImplementation(project(":server:libs:test:test-support"))
}
