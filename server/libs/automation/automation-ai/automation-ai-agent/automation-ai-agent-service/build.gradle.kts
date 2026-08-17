dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:automation:automation-ai:automation-ai-agent:automation-ai-agent-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))

    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework.boot:spring-boot-starter-cache-test")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-service"))
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc"))
    // AiAgentWorkflowExecutionIntTest: drives branch_in (a slice of the generated agent workflow) through the same
    // JobSyncExecutor-based harness the branch task-dispatcher module uses for its own IntTests.
    testImplementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    testImplementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    testImplementation(project(":server:libs:core:evaluator:evaluator-impl"))
    testImplementation(project(":server:libs:modules:task-dispatchers:branch"))
    // AiAgentWorkflowExecutionIntTest's Slack echo-loop guard tests: the generated slack case nests a condition/v1
    // (bot-message detection) whose TRUE arm is a terminate/v1 task.
    testImplementation(project(":server:libs:modules:task-dispatchers:condition"))
    testImplementation(project(":server:libs:modules:task-dispatchers:terminate"))
    testImplementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-test-int-support"))
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    // Not wired as Spring beans (this slice mocks/skips the facade layer they belong to) — testImplementation
    // deps purely so their Liquibase changelogs (category, tag, api_key, connection tables that Project's FKs
    // point at) are on the classpath for schema creation.
    testImplementation(project(":server:libs:platform:platform-category:platform-category-service"))
    testImplementation(project(":server:libs:platform:platform-connection:platform-connection-service"))
    // Real WorkflowTestConfigurationService/WorkflowNodeTestOutputService beans (interfaces are already on the main
    // classpath via platform-configuration-api) — publishAgent's replicated ProjectFacadeImpl.publishProject loop
    // calls their updateWorkflowId(...) methods.
    testImplementation(project(":server:libs:platform:platform-configuration:platform-configuration-service"))
    testImplementation(project(":server:libs:platform:platform-security:platform-security-service"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
