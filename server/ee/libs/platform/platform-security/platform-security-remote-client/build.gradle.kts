dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-security:platform-security-remote-grpc"))
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))

    testImplementation("io.grpc:grpc-inprocess")
}
