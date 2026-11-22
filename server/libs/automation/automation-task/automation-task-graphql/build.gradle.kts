dependencies {
    api(project(":server:libs:automation:automation-task:automation-task-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.security:spring-security-config")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework:spring-webflux")
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:automation:automation-task:automation-task-service"))
    testImplementation(project(":server:libs:core:graphql:graphql-impl"))
    testImplementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))
    testImplementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-token-service"))
}
