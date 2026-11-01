dependencies {
    api(project(":server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
