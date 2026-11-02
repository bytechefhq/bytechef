dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:tenant:tenant-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:core:tenant:tenant-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("org.springframework.boot:spring-boot-jdbc")
    testImplementation("org.springframework.boot:spring-boot-liquibase")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation(project(":server:ee:libs:core:tenant:tenant-multi-service"))
    testImplementation(project(":server:libs:test:test-int-support"))

    testRuntimeOnly("com.zaxxer:HikariCP")
    testRuntimeOnly("org.postgresql:postgresql")
}
