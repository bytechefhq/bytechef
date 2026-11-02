dependencies {
    api(project(":server:ee:libs:automation:automation-task:automation-task-remote-grpc-proto"))
    api(project(":server:libs:automation:automation-task:automation-task-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
