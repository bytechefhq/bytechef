dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation("org.slf4j:slf4j-api")
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))

    api(project(":server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework:spring-test")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.testcontainers:postgresql")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))

    testRuntimeOnly("org.postgresql:postgresql")
}
