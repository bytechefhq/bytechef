dependencies {
    api(project(":server:ee:libs:automation:automation-configuration:automation-configuration-remote-grpc-proto"))
    api(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    api(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:atlas:atlas-execution:atlas-execution-remote-grpc"))
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-remote-grpc"))

    testImplementation(project(":server:libs:test:test-support"))
}
