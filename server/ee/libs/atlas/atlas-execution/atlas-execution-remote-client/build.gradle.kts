dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-api"))

    implementation(project(":server:ee:libs:atlas:atlas-execution:atlas-execution-remote-grpc"))
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("org.springframework.grpc:spring-grpc-core")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation(project(":server:libs:test:test-support"))
}
