dependencies {
    api(project(":server:ee:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-registry:platform-workflow-task-dispatcher-registry-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))

    testImplementation(project(":server:libs:modules:task-dispatchers:branch"))
    testImplementation(project(":server:libs:modules:task-dispatchers:condition"))
    testImplementation(project(":server:libs:modules:task-dispatchers:each"))
    testImplementation(project(":server:libs:modules:task-dispatchers:loop"))
    testImplementation(project(":server:libs:modules:task-dispatchers:map"))
    testImplementation(project(":server:libs:modules:task-dispatchers:parallel"))
    testImplementation(project(":server:libs:test:test-support"))
}
