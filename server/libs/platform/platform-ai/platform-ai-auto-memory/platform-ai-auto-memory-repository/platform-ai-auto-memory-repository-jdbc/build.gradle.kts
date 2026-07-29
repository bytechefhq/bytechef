dependencies {
    api(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")

    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:platform:platform-ai:platform-ai-auto-memory:platform-ai-auto-memory-repository:platform-ai-auto-memory-repository-test-support"))
}
