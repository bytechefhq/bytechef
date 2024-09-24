dependencies {
    implementation("org.springframework:spring-context")
    implementation("org.springframework.data:spring-data-jdbc")
    implementation(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    implementation(project(":server:libs:core:commons:commons-util"))
    implementation(project(":server:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-automation:platform-code-workflow-configuration-automation-api"))
    implementation(project(":server:libs:platform:platform-code-workflow:platform-code-workflow-loader:platform-code-workflow-loader-automation"))

    testImplementation("org.springframework.data:spring-data-jdbc")
    testImplementation(project(":server:libs:config:liquibase-config"))
    testImplementation(project(":server:libs:test:test-int-support"))
}
