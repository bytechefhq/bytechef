dependencies {
    api(project(":server:ee:libs:core:tenant:tenant-remote-grpc-proto"))
    api(project(":server:libs:core:tenant:tenant-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
