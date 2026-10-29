dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-registry:platform-workflow-task-dispatcher-registry-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("org.springframework.grpc:spring-grpc-core")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:modules:task-dispatchers:condition"))
    testImplementation(project(":server:libs:modules:task-dispatchers:loop"))
    testImplementation(project(":server:libs:modules:task-dispatchers:subflow"))
    testImplementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-service"))
    testImplementation(project(":server:libs:test:test-support"))
}
