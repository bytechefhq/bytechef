group = "com.bytechef.WebhookApplication"
description = ""

springBoot {
    mainClass.set("com.bytechef.webhook.WebhookApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-redis")
    implementation("org.springframework.boot:spring-boot-starter-aspectj")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.boot:spring-boot-starter-opentelemetry")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:cache-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:logback-config"))
    implementation(project(":server:libs:config:messages-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:evaluator:evaluator-impl"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-instance-impl"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-coordinator"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-impl"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-rest:platform-webhook-rest-impl"))

    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api"))

    implementation(project(":server:ee:libs:atlas:atlas-execution:atlas-execution-remote-client"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-remote-client"))
    implementation(project(":server:ee:libs:config:observability-config"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-instance-impl"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-remote-client"))
    implementation(project(":server:ee:libs:embedded:embedded-webhook:embedded-webhook-rest"))
    implementation(project(":server:ee:libs:embedded:embedded-workflow:embedded-workflow-coordinator"))
    implementation(project(":server:ee:libs:platform:platform-component:platform-component-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-notification:platform-notification-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-remote-client"))

    testImplementation(project(":server:libs:core:message:message-broker:message-broker-memory"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
