group = "com.bytechef.coordinator"
description = ""

springBoot {
    mainClass.set("com.bytechef.coordinator.CoordinatorApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-config"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-service"))
    implementation(project(":server:libs:core:async-config"))
    implementation(project(":server:libs:core:environment-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:jackson-config"))
    implementation(project(":server:libs:core:rest:rest-impl"))
    implementation(project(":server:libs:helios:helios-configuration:helios-configuration-instance-impl"))
    implementation(project(":server:libs:helios:helios-coordinator"))
    implementation(project(":server:libs:hermes:hermes-coordinator:hermes-coordinator-impl"))
    implementation(project(":server:libs:hermes:hermes-file-storage:hermes-file-storage-service"))
    implementation(project(":server:libs:hermes:hermes-task-dispatcher:hermes-task-dispatcher-registry:hermes-task-dispatcher-registry-service"))

    implementation(project(":ee:server:libs:atlas:atlas-execution:atlas-execution-remote-client"))
    implementation(project(":ee:server:libs:core:discovery:discovery-redis"))
    implementation(project(":ee:server:libs:helios:helios-configuration:helios-configuration-remote-client"))
    implementation(project(":ee:server:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-remote-client"))
    implementation(project(":ee:server:libs:hermes:hermes-configuration:hermes-configuration-remote-client"))
    implementation(project(":ee:server:libs:hermes:hermes-execution:hermes-execution-remote-client"))
    implementation(project(":ee:server:libs:hermes:hermes-task-dispatcher-registry:hermes-task-dispatcher-registry-remote-rest"))

    implementation(project(":server:libs:modules:task-dispatchers:branch"))
    implementation(project(":server:libs:modules:task-dispatchers:condition"))
    implementation(project(":server:libs:modules:task-dispatchers:each"))
    implementation(project(":server:libs:modules:task-dispatchers:forkjoin"))
    implementation(project(":server:libs:modules:task-dispatchers:loop"))
    implementation(project(":server:libs:modules:task-dispatchers:map"))
    implementation(project(":server:libs:modules:task-dispatchers:parallel"))
    implementation(project(":server:libs:modules:task-dispatchers:sequence"))
    implementation(project(":server:libs:modules:task-dispatchers:subflow"))

    runtimeOnly("org.springframework.boot:spring-boot-starter-amqp")
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-aop")
    runtimeOnly("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.springframework.kafka:spring-kafka")

    testImplementation(project(":server:libs:test:test-int-support"))
}
