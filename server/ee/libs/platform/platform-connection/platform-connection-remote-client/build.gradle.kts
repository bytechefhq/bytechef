dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-connection:platform-connection-api"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-connection:platform-connection-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("org.springframework.grpc:spring-grpc-core")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:encryption:encryption-impl"))
    testImplementation(project(":server:libs:platform:platform-connection:platform-connection-service"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
    testImplementation(project(":server:libs:test:test-support"))
}
