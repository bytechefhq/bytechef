dependencies {
    implementation("org.apache.commons:commons-csv:1.14.1")
    implementation("org.apache.commons:commons-lang3")
    implementation("jakarta.validation:jakarta.validation-api")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.security:spring-security-core")
    implementation(project(":server:libs:core:commons:commons-util"))

    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-api"))

    testImplementation("org.mockito:mockito-core")
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation("org.springframework.security:spring-security-config")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:config:app-config"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:platform:platform-api"))
    testImplementation(project(":server:libs:test:test-int-support"))

    testImplementation(project(":server:ee:libs:platform:platform-audit:platform-audit-api"))
    testImplementation(project(":server:ee:libs:platform:platform-audit:platform-audit-service"))

    testRuntimeOnly("com.h2database:h2")
}
