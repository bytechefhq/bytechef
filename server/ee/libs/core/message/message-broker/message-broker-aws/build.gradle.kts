dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("tools.jackson.core:jackson-databind")
    implementation("io.awspring.cloud:spring-cloud-aws-autoconfigure")
    implementation("io.awspring.cloud:spring-cloud-aws-sqs")
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
    implementation(project(":server:libs:config:app-config"))

    testImplementation(rootProject.libs.loki.logback.appender)
    testImplementation("io.awspring.cloud:spring-cloud-aws-starter-sqs")
    testImplementation("io.awspring.cloud:spring-cloud-aws-s3")
    testImplementation("org.testcontainers:junit-jupiter")
    testImplementation("org.testcontainers:localstack")
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:config:jackson-config"))
}
