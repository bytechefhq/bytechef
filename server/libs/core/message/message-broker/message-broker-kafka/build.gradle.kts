dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.kafka:spring-kafka")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-kafka")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
}
