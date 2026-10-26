dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation(project(":server:libs:test:test-support"))
}
