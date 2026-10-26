dependencies {
    api(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
