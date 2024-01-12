dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-registry:platform-workflow-task-dispatcher-registry-api"))

    implementation(project(":ee:server:libs:core:commons:commons-discovery"))
    implementation(project(":ee:server:libs:core:commons:commons-rest-client"))
}
