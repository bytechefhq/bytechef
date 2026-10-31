dependencies {
    implementation("org.springframework.graphql:spring-graphql")
    implementation("org.springframework.data:spring-data-commons")
    implementation(project(":server:libs:atlas:atlas-coordinator:atlas-coordinator-api"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-tool-execution:platform-tool-execution-api"))
    implementation(project(":server:ee:libs:platform:platform-tool-invocation-log:platform-tool-invocation-log-api"))

    testImplementation("org.assertj:assertj-core")
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework:spring-webflux")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testImplementation("org.springframework.boot:spring-boot-starter-graphql-test")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation(project(":server:libs:test:test-int-support"))
}
