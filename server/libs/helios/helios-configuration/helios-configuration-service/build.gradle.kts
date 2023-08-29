dependencies {
    implementation(project(":server:libs:helios:helios-configuration:helios-configuration-api"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-repository:atlas-configuration-repository-jdbc"))
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-service"))
    testImplementation(project(":server:libs:configs:liquibase-config"))
    testImplementation(project(":server:libs:core:message-broker:message-broker-api"))
    testImplementation(project(":server:libs:core:category:category-service"))
    testImplementation(project(":server:libs:core:tag:tag-service"))
    testImplementation(project(":server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-service"))
    testImplementation(project(":server:libs:hermes:hermes-connection:hermes-connection-api"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
