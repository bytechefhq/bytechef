dependencies {
    api(project(":server:ee:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))

    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-service"))
    testImplementation(project(":server:libs:test:test-support"))
}
