dependencies {
    implementation("org.springframework:spring-context")

    implementation(project(":server:ee:libs:embedded:embedded-ai:embedded-ai-mcp-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
