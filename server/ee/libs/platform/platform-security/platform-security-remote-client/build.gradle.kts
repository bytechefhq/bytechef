dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-security:platform-security-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-security:platform-security-remote-grpc"))
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    testImplementation(project(":server:libs:platform:platform-security:platform-security-service"))
    testImplementation(project(":server:libs:platform:platform-user:platform-user-api"))
    testImplementation(project(":server:libs:platform:platform-user:platform-user-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
