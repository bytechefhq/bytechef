dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-commons")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-remote-grpc"))
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))

    testImplementation("io.grpc:grpc-inprocess")
}
