dependencies {
    implementation("io.grpc:grpc-api")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))
    implementation(project(":server:libs:platform:platform-security-web:platform-security-web-api"))

    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
}
