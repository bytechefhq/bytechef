dependencies {
    api(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    testImplementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-impl"))
    testImplementation(project(":server:libs:atlas:atlas-worker:atlas-worker-config"))
    testImplementation(project(":server:libs:atlas:atlas-worker:atlas-worker-impl"))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:core:evaluator:evaluator-impl"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    testImplementation(project(":server:libs:test:test-support"))
}
