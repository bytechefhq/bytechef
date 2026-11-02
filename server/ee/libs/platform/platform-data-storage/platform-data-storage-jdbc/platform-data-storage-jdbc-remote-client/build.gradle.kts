dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-api"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
