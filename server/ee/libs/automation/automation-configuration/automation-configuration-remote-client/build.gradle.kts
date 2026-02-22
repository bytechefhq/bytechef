dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-task-dispatcher:platform-workflow-task-dispatcher-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
