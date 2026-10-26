dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("org.springframework.grpc:spring-grpc-core")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation(project(":server:libs:test:test-support"))
}
