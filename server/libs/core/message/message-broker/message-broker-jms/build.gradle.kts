dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("tools.jackson.core:jackson-databind")
    implementation("jakarta.jms:jakarta.jms-api")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework:spring-jms")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.boot:spring-boot-jms")
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
}
