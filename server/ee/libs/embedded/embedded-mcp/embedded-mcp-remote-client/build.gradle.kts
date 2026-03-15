dependencies {
    implementation("org.springframework:spring-context")

    implementation(project(":server:ee:libs:embedded:embedded-mcp:embedded-mcp-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
