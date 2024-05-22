group = "com.bytechef.WebhookApplication"
description = ""

springBoot {
    mainClass.set("com.bytechef.webhook.WebhookApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:messages-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-noop-service"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-instance-impl"))
    implementation(project(":server:libs:automation:automation-workflow:automation-workflow-coordinator"))
    implementation(project(":server:libs:embedded:embedded-configuration:embedded-configuration-instance-impl"))
    implementation(project(":server:libs:embedded:embedded-workflow:embedded-workflow-coordinator"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-impl"))
    implementation(project(":server:libs:platform:platform-webhook:platform-webhook-rest"))

    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-worker:platform-workflow-worker-api"))

    implementation(project(":server:ee:libs:atlas:atlas-execution:atlas-execution-remote-client"))
    implementation(project(":server:ee:libs:atlas:atlas-worker:atlas-worker-remote-config"))
    implementation(project(":server:ee:libs:automation:automation-configuration:automation-configuration-remote-client"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-component:platform-component-registry:platform-component-registry-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-configuration:platform-configuration-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-remote-client"))

    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.springframework.boot:spring-boot-starter-aop")

    testImplementation(project(":server:libs:test:test-int-support"))
}
