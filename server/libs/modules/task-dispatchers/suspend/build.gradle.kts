version="1.0"

dependencies {
    api(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))

    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    testImplementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-converter"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-api"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-jdbc"))
    testImplementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(testFixtures(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api")))
}
