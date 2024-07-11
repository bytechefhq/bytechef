dependencies {
    implementation("io.awspring.cloud:spring-cloud-aws-sqs")
    implementation("org.apache.commons:commons-lang3")
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
}
