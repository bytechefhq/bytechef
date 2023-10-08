dependencies {
    api(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    api(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8")
    testImplementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    testImplementation("org.springframework:spring-jdbc")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    testImplementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-service"))
    testImplementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    testImplementation(project(":server:libs:core:message:message-broker:message-broker-sync"))
}
