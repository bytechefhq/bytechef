dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.data:spring-data-redis")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("io.lettuce:lettuce-core")
    implementation("tools.jackson.core:jackson-databind")
    api(project(":server:libs:core:message:message-broker:message-broker-redis-control"))
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))
}

dependencies {
    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter-api")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.testcontainers:junit-jupiter")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}
