dependencies {
    api(project(":server:ee:libs:platform:platform-external-invocation-log:platform-external-invocation-log-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-external-invocation:platform-external-invocation-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
