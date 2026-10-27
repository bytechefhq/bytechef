dependencies {
    api(project(":server:ee:libs:platform:platform-notification:platform-notification-remote-grpc-proto"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-notification:platform-notification-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
