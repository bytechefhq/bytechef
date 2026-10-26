dependencies {
    api(project(":server:ee:libs:platform:platform-user:platform-user-remote-grpc-proto"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))
}
