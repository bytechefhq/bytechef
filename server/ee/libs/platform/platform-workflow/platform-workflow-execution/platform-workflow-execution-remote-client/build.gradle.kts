dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-workflow:platform-workflow-execution:platform-workflow-execution-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
