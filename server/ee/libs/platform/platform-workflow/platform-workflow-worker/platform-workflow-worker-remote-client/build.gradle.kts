dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:core:discovery:discovery-util"))
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("org.springframework.grpc:spring-grpc-core")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation(project(":server:libs:test:test-support"))
}
