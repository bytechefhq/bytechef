dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.data:spring-data-redis")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.lettuce:lettuce-core")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
}
