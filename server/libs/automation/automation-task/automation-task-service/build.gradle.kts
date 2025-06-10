dependencies {
    api(project(":server:libs:automation:automation-task:automation-task-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.data:spring-data-commons")
    implementation("org.springframework.data:spring-data-jdbc")

    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation("org.springframework.boot:spring-boot-starter-test")
}
