dependencies {
    api(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    api(project(":server:libs:core:message:message-event:message-event-api"))
    api(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    implementation("org.apache.commons:commons-lang3")
    api("org.springframework:spring-expression")
    implementation(project(":server:libs:core:commons:commons-util"))

    testImplementation(project(":server:libs:core:evaluator:evaluator-api"))

    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:core:evaluator:evaluator-impl"))
    testImplementation(project(":server:libs:test:test-support"))
}
