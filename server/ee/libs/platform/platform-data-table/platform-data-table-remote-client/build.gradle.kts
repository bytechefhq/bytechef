dependencies {
    implementation("org.springframework:spring-context")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:libs:platform:platform-data-table:platform-data-table-api"))
    implementation(project(":server:libs:platform:platform-api"))

    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
    implementation(project(":server:ee:libs:platform:platform-data-table:platform-data-table-remote-grpc"))

    testImplementation("io.grpc:grpc-inprocess")
    testImplementation("io.micrometer:micrometer-core")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc")
    testImplementation("org.springframework.grpc:spring-grpc-core")
    testImplementation("org.springframework.security:spring-security-core")
    testImplementation(testFixtures(project(":server:ee:libs:core:remote:remote-grpc")))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:config:jackson-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))
    testImplementation(project(":server:libs:platform:platform-data-table:platform-data-table-service"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
    testImplementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
    testImplementation(project(":server:libs:test:test-int-support"))

    testRuntimeOnly("org.postgresql:postgresql")
    testRuntimeOnly(project(":server:libs:platform:platform-tag:platform-tag-service"))
}
