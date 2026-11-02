dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:automation:automation-ai:automation-ai-auto-memory:automation-ai-auto-memory-api"))

    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.graphql:spring-graphql-test")
    testImplementation("org.springframework.security:spring-security-config")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation(project(":server:libs:automation:automation-ai:automation-ai-auto-memory:automation-ai-auto-memory-service"))
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    testImplementation(project(":server:libs:platform:platform-user:platform-user-api"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
