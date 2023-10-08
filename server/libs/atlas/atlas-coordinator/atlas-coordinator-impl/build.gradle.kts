dependencies {
    api("org.springframework:spring-context")
    api(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    api(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-core")
    implementation("org.springframework:spring-expression")
    implementation("org.springframework:spring-web")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator"))

    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-converter"))
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc"))
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-resource"))
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-service"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-jdbc"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-service"))
    testImplementation(project(":server:libs:atlas:atlas-sync-executor"))
    testImplementation(project(":server:libs:atlas:atlas-worker:atlas-worker-impl"))
    testImplementation(project(":server:libs:core:message:message-broker:message-broker-sync"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    testImplementation(project(":server:libs:core:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
