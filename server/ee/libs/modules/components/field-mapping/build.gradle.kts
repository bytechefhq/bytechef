version="1.0"

dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:atlas:atlas-configuration:atlas-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-api"))
    implementation(project(":server:libs:platform:platform-component:platform-component-api"))
    implementation(project(":server:libs:platform:platform-configuration:platform-configuration-api"))

    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))

    testImplementation("org.springframework.boot:spring-boot-jdbc")
    testImplementation("org.springframework.boot:spring-boot-test")
    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation("org.springframework:spring-jdbc")
    testImplementation(project(":server:libs:automation:automation-configuration:automation-configuration-service"))
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:core:commons:commons-data"))
    testImplementation(project(":server:libs:core:encryption:encryption-api"))
    testImplementation(project(":server:libs:core:encryption:encryption-impl"))
    testImplementation(project(":server:libs:platform:platform-category:platform-category-service"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-context:platform-component-context-api"))
    testImplementation(project(":server:libs:platform:platform-component:platform-component-test-int-support"))
    testImplementation(project(":server:libs:platform:platform-configuration:platform-configuration-service"))
    testImplementation(project(":server:libs:platform:platform-connection:platform-connection-service"))
    testImplementation(project(":server:libs:platform:platform-security:platform-security-service"))
    testImplementation(project(":server:libs:platform:platform-tag:platform-tag-service"))
    testImplementation(project(":server:libs:test:test-int-support"))

    testImplementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-service"))
    testImplementation(project(":server:ee:libs:embedded:embedded-connected-user:embedded-connected-user-service"))
}
