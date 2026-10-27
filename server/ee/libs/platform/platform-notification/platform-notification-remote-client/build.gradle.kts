dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-notification:platform-notification-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-notification:platform-notification-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
}
