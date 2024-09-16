dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("io.awspring.cloud:spring-cloud-aws-autoconfigure")
    implementation("io.awspring.cloud:spring-cloud-aws-sqs")
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
    implementation(project(":server:libs:config:app-config"))

    testImplementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    testImplementation("io.awspring.cloud:spring-cloud-aws-dynamodb")
    testImplementation("io.awspring.cloud:spring-cloud-aws-s3")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation(project(":server:libs:config:app-config"))
}
