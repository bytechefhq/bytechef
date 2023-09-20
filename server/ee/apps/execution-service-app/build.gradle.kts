group = "com.bytechef.execution"
description = ""

springBoot {
    mainClass.set("com.bytechef.execution.ExecutionApplication")
}

dependencies {
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("org.springframework:spring-webflux")
    implementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation("org.springframework.retry:spring-retry")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-converter"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-repository:atlas-execution-repository-jdbc"))
    implementation(project(":server:libs:atlas:atlas-execution:atlas-execution-service"))
    implementation(project(":server:libs:atlas:atlas-file-storage:atlas-file-storage-service"))
    implementation(project(":server:libs:atlas:atlas-worker:atlas-worker-api"))
    implementation(project(":server:libs:configs:liquibase-config"))
    implementation(project(":server:libs:core:commons:commons-data"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:core:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:event:event-impl"))
    implementation(project(":server:libs:core:rest:rest-impl"))
    implementation(project(":server:libs:helios:helios-execution:helios-execution-rest"))
    implementation(project(":server:libs:helios:helios-execution:helios-execution-service"))
    implementation(project(":server:libs:core:data-storage:data-storage-db:data-storage-db-service"))
    implementation(project(":server:libs:hermes:hermes-execution:hermes-execution-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-base64-service"))
    implementation(project(":server:libs:core:file-storage:file-storage-filesystem-service"))
    implementation(project(":server:libs:hermes:hermes-test-executor:hermes-test-executor-impl"))
    implementation(project(":server:libs:hermes:hermes-worker:hermes-worker-api"))

    implementation(project(":server:ee:libs:atlas:atlas-execution:atlas-execution-remote-rest"))
    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:hermes:hermes-component:hermes-component-registry:hermes-component-registry-remote-client"))
    implementation(project(":server:ee:libs:helios:helios-configuration:helios-configuration-remote-client"))
    implementation(project(":server:ee:libs:hermes:hermes-configuration:hermes-configuration-remote-client"))
    implementation(project(":server:ee:libs:core:data-storage:data-storage-db:data-storage-db-remote-rest"))
    implementation(project(":server:ee:libs:hermes:hermes-execution:hermes-execution-remote-rest"))
    implementation(project(":server:ee:libs:hermes:hermes-scheduler:hermes-scheduler-remote-client"))

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly(libs.org.springdoc.springdoc.openapi.starter.webmvc.ui)
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-aop")
    runtimeOnly("org.springframework.boot:spring-boot-starter-data-redis")

    testImplementation(project(":server:libs:test:test-int-support"))
}
