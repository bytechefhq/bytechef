dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    implementation("io.awspring.cloud:spring-cloud-aws-s3")
    implementation("io.awspring.cloud:spring-cloud-aws-dynamodb")
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
//    implementation(project(":server:ee:libs:core:file-storage:file-storage-aws"))
    implementation(project(":server:libs:config:app-config"))

    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation(project(":server:libs:config:app-config"))
}
