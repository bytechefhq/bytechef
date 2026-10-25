dependencies {
    implementation("io.lettuce:lettuce-core")
    implementation("org.jspecify:jspecify")
    implementation("org.slf4j:slf4j-api")
    implementation("org.springframework.data:spring-data-redis")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:core:message:message-broker:message-broker-api"))

    testImplementation("org.testcontainers:junit-jupiter")
}
