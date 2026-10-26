dependencies {
    api(project(":server:ee:libs:atlas:atlas-execution:atlas-execution-remote-grpc"))
    api(project(":server:ee:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))

    testImplementation(project(":server:libs:test:test-support"))
}
