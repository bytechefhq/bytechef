dependencies {
    api(project(":server:ee:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-remote-grpc-proto"))
    api(project(":server:libs:automation:automation-workflow:automation-workflow-execution:automation-workflow-execution-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-commons")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-remote-grpc"))
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-remote-grpc"))
}
