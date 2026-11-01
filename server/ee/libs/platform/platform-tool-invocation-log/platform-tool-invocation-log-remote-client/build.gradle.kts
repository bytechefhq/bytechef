dependencies {
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("org.awaitility:awaitility")
    testImplementation("org.springframework.grpc:spring-grpc-core")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:tenant:tenant-api"))
    testImplementation(project(":server:libs:core:tenant:tenant-single-service"))
    testImplementation(project(":server:libs:platform:platform-api"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-service"))

    testRuntimeOnly("org.postgresql:postgresql")
}
