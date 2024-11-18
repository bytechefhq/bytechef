dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-tag:platform-tag-api"))
    implementation(project(":server:libs:platform:platform-tenant:platform-tenant-api"))

    implementation(project(":server:ee:libs:automation:automation-api-platform:automation-api-platform-configuration:automation-api-platform-configuration-api"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
