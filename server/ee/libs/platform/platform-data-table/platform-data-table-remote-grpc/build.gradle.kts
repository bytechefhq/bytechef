dependencies {
    api(project(":server:ee:libs:platform:platform-data-table:platform-data-table-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-data-table:platform-data-table-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
