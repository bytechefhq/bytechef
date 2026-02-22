dependencies {
    implementation("org.apache.commons:commons-lang3")
    implementation("org.springframework:spring-context")

    implementation(project(":server:ee:libs:embedded:embedded-configuration:embedded-configuration-api"))
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))
}
