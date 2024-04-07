group = "com.bytechef.scheduler"
description = ""

springBoot {
    mainClass.set("com.bytechef.scheduler.SchedulerApplication")
}

dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation(libs.org.openapitools.jackson.databind.nullable)
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.cloud:spring-cloud-starter-config")
    implementation("org.springframework.cloud:spring-cloud-starter-loadbalancer")
    implementation(project(":server:libs:config:async-config"))
    implementation(project(":server:libs:config:environment-config"))
    implementation(project(":server:libs:config:jackson-config"))
    implementation(project(":server:libs:config:liquibase-config"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-amqp"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-kafka"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-redis"))
    implementation(project(":server:libs:core:message:message-event:message-event-impl"))
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-impl"))

    implementation(project(":server:ee:libs:core:discovery:discovery-redis"))
    implementation(project(":server:ee:libs:platform:platform-component:platform-component-registry:platform-component-registry-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-remote-client"))
    implementation(project(":server:ee:libs:platform:platform-scheduler:platform-scheduler-remote-rest"))

    runtimeOnly("org.postgresql:postgresql")
    runtimeOnly("com.zaxxer:HikariCP")
    runtimeOnly("org.springframework.boot:spring-boot-starter-actuator")
    runtimeOnly("org.springframework.boot:spring-boot-starter-aop")
    runtimeOnly("org.springframework.boot:spring-boot-starter-data-redis")
    runtimeOnly("org.springframework.boot:spring-boot-starter-quartz")

    testImplementation(project(":server:libs:test:test-int-support"))
}
