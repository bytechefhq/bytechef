dependencies {
    api(project(":server:libs:automation:automation-task:automation-task-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.graphql:spring-graphql")

    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.graphql:spring-graphql-test")
}
