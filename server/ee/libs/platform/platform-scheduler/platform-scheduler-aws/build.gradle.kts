dependencies {
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("software.amazon.awssdk:scheduler")
    implementation("io.awspring.cloud:spring-cloud-aws-sqs")

    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-coordinator:platform-workflow-coordinator-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-instance-api"))
    implementation(project(":server:ee:libs:core:cloud:cloud-aws"))
    implementation(project(":server:ee:libs:core:message:message-broker:message-broker-aws"))
    implementation(project(":server:libs:config:app-config"))
}
