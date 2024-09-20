dependencies {
    implementation("org.springframework:spring-context-support")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("software.amazon.awssdk:scheduler")
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-scheduler:platform-scheduler-api"))
    implementation(project(":server:ee:libs:core:cloud:cloud-aws"))
    implementation(project(":server:libs:config:app-config"))
//    implementation(project(":server:ee:libs:core:message:message-broker:message-broker-aws"))

//    testImplementation("org.testcontainers:junit-jupiter")
//    testImplementation("org.testcontainers:localstack")
//    testImplementation("io.awspring.cloud:spring-cloud-aws-dynamodb")
//    testImplementation("io.awspring.cloud:spring-cloud-aws-s3")
//    testImplementation(project(":server:libs:config:app-config"))

//    testImplementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
//    testImplementation("io.awspring.cloud:spring-cloud-aws-autoconfigure")
//    testImplementation(project(":server:ee:libs:core:message:message-broker:message-broker-aws"))
}
