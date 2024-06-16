dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.cloud:spring-cloud-commons")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-registry:platform-workflow-task-dispatcher-registry-api"))

    implementation(project(":server:ee:libs:core:discovery:discovery-util"))
    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
