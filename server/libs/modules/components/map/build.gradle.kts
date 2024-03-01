version="1.0"

dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-service"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-memory"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-impl"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-sync"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:modules:task-dispatchers:map"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-service"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))

    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation(project(":server:libs:core:commons:commons-util"))
}
