dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation("org.quartz-scheduler:quartz")
    testImplementation("org.springframework.boot:spring-boot-quartz")
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-impl"))
    testImplementation(project(":server:libs:test:test-support"))
}
