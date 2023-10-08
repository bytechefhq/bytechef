dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:hermes:hermes-component:hermes-component-api"))
    implementation(project(":server:libs:hermes:hermes-connection:hermes-connection-api"))

    testImplementation("com.fasterxml.jackson.core:jackson-databind")
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:encryption:encryption-impl"))
    testImplementation(project(":server:libs:core:tag:tag-service"))
    testImplementation(project(":server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-service"))
    testImplementation(project(":server:libs:core:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
