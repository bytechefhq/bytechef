dependencies {
    api(project(":server:ee:libs:embedded:embedded-ai:embedded-ai-auto-memory-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-config")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:test:test-support"))
}
