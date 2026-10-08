dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")

    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:encryption:encryption-impl"))
    testImplementation(project(":server:libs:platform:platform-category:platform-category-service"))
    testImplementation(project(":server:libs:platform:platform-security:platform-security-service"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))

    testImplementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-service"))
}
