dependencies {
    api(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-remote-grpc-proto"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
}
