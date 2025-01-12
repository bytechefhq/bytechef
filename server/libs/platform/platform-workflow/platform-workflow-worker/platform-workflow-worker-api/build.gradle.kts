dependencies {
    api(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    api(project(":server:libs:core:message:message-event:message-event-api"))
    api(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    implementation("org.apache.commons:commons-lang3")
    implementation(project(":server:libs:core:commons:commons-util"))
}
