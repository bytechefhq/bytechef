dependencies {
    api(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))
    api(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-api"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-jdbc"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
