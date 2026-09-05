version="1.0"

dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:modules:task-dispatchers:subflow"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))

    testImplementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-impl"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-memory"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    testImplementation(project(":server:libs:atlas:atlas-worker:atlas-worker-impl"))
    testImplementation(project(":server:libs:core:message:message-broker:message-broker-memory"))
    testImplementation(project(":server:libs:core:tenant:tenant-api"))
    testImplementation(project(":server:libs:modules:task-dispatchers:parallel"))
    testImplementation(project(":server:libs:modules:task-dispatchers:suspend"))
    testImplementation(project(":server:libs:platform:platform-job-sync"))
    testImplementation(project(":server:libs:platform:platform-worker"))
    testImplementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
    testImplementation(project(":server:libs:test:test-support"))
}
