dependencies {
    api(project(":server:libs:automation:automation-task:automation-task-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
    // ApprovalTaskFacadeImpl requires a real ApprovalTokens signer (resolveInnerToken on every approval-task
    // creation, form URLs for the pending-approvals inbox), so the module carrying its auto-configuration has to
    // reach the runtime classpath of every app hosting this service (monolith and configuration-app alike).
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-token-service"))

    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
