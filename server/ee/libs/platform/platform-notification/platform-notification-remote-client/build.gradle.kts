dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-notification:platform-notification-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-notification:platform-notification-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation("org.eclipse.angus:angus-mail")
    testImplementation("org.springframework:spring-context-support")
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation("org.springframework.grpc:spring-grpc-core")
    testImplementation("org.thymeleaf:thymeleaf-spring6")
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:platform:platform-mail"))
    testImplementation(project(":server:libs:platform:platform-notification:platform-notification-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
