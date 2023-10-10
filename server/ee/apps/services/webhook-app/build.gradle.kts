group = "com.bytechef.WebhookApplication"
description = ""

springBoot {
    mainClass.set("com.bytechef.webhook.WebhookApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:core:async-config"))
    implementation(project(":server:libs:core:environment-config"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:core:jackson-config"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:helios:helios-configuration:helios-configuration-instance-impl"))
    implementation(project(":server:libs:helios:helios-coordinator"))
    implementation(project(":server:libs:hermes:hermes-webhook:hermes-webhook-impl"))
    implementation(project(":server:libs:hermes:hermes-webhook:hermes-webhook-rest"))

    implementation(project(":server:ee:libs:atlas:atlas-execution:atlas-execution-remote-client"))
    implementation(project(":server:ee:libs:atlas:atlas-worker:atlas-worker-remote-client"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-remote-client"))
    implementation(project(":server:ee:libs:helios:helios-configuration:helios-configuration-remote-client"))
    implementation(project(":server:ee:libs:hermes:hermes-configuration:hermes-configuration-remote-client"))
    implementation(project(":server:ee:libs:hermes:hermes-execution:hermes-execution-remote-client"))

    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.springframework.boot:spring-boot-starter-aop")

    testImplementation(project(":server:libs:test:test-int-support"))
}
