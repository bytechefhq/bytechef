dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    implementation(project(":ee:server:libs:core:commons:commons-rest-client"))
}
