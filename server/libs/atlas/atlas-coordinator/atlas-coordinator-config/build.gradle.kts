dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot")
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    implementation(project(":server:libs:core:autoconfigure-annotations"))
    implementation(project(":server:libs:core:message-broker:message-broker-api"))
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-impl"))
    implementation(project(":server:libs:core:commons:commons-util"))
}
