dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-validator:platform-workflow-validator-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
}
