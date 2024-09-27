dependencies {
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("software.amazon.awssdk:scheduler")

    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))
    implementation(project(":server:ee:libs:core:cloud:cloud-aws"))
    implementation(project(":server:ee:libs:core:message:message-broker:message-broker-aws"))
    implementation(project(":server:libs:config:app-config"))
}
