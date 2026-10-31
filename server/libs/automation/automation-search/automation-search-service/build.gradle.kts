dependencies {
    api(project(":server:libs:automation:automation-search:automation-search-api"))

    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:tenant:tenant-api"))
    implementation(project(":server:libs:platform:platform-user:platform-user-api"))

    testImplementation("org.springframework.boot:spring-boot-starter-data-jdbc-test")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:config:cache-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:platform:platform-user:platform-user-service"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
