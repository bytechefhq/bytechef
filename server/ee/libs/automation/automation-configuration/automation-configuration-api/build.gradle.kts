dependencies {
    api(project(":server:libs:automation:automation-configuration:automation-configuration-api"))
    api(project(":server:ee:libs:platform:platform-code-workflow:platform-code-workflow-configuration:platform-code-workflow-configuration-api"))

    implementation("org.springframework.data:spring-data-jdbc")
}
