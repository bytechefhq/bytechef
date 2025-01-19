dependencies {
    api(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    api(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    api(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-impl"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-impl"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-sync"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-api"))
}
