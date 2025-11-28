group = "com.bytechef.worker"
description = ""

springBoot {
    mainClass.set("com.bytechef.worker.WorkerApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-starter-amqp")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-aop")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-impl"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-config"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:cache-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:logback-config"))
    implementation(project(":server:libs:config:messages-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-impl"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:platform:platform-data-storage:platform-data-storage-file-storage:platform-data-storage-file-storage-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:platform:platform-file-storage:platform-file-storage-impl"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-context:platform-component-context-service"))
    implementation(project(":server:libs:platform:platform-component:platform-component-service"))
    implementation(project(":server:libs:platform:platform-worker"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-impl"))

    run {
        rootProject.subprojects
            .asSequence()
            .filter { it.path.startsWith(":server:libs:modules:components") }
            .filterNot { it.path in setOf(":server:libs:modules:components:ai:llm:amazon-bedrock", ":server:libs:modules:components:data-stream", ":server:libs:modules:components:example") }
            .sortedBy { it.path }
            .forEach { implementation(project(it.path)) }
    }

    implementation(project(":server:ee:libs:config:observability-config"))
    implementation(project(":server:ee:libs:config:tenant-multi-message-event-config"))
    implementation(project(":server:ee:libs:core:discovery:discovery-metadata-api"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:core:remote:remote-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-component:platform-component-remote-rest"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-connection:platform-connection-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-data-storage:platform-data-storage-jdbc:platform-data-storage-jdbc-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-remote-rest"))

    implementation(project(":server:ee:libs:modules:components:app-event"))

    testImplementation(project(":server:libs:core:message:message-broker:message-broker-memory"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
