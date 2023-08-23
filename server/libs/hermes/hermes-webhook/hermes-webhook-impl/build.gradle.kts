dependencies {
    api(project(":server:libs:hermes:hermes-webhook:hermes-webhook-api"))

    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-sync-executor"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:event:event-listener:event-listener-api"))
    implementation(project(":server:libs:core:message-broker:message-broker-sync"))
    implementation(project(":server:libs:hermes:hermes-coordinator:hermes-coordinator-impl"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-api"))
    implementation(project(":server:libs:modules:components:map"))
    implementation(project(":server:libs:modules:task-dispatchers:branch"))
    implementation(project(":server:libs:modules:task-dispatchers:condition"))
    implementation(project(":server:libs:modules:task-dispatchers:each"))
    implementation(project(":server:libs:modules:task-dispatchers:forkjoin"))
    implementation(project(":server:libs:modules:task-dispatchers:loop"))
    implementation(project(":server:libs:modules:task-dispatchers:map"))
    implementation(project(":server:libs:modules:task-dispatchers:parallel"))
    implementation(project(":server:libs:modules:task-dispatchers:sequence"))
    implementation(project(":server:libs:modules:task-dispatchers:subflow"))

}
