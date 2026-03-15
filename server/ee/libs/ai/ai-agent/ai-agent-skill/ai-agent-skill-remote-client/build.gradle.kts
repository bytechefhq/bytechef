dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:ai:ai-agent:ai-agent-skill:ai-agent-skill-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
