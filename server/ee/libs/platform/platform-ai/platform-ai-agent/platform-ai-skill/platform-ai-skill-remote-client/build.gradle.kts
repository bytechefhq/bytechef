dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:libs:platform:platform-ai:platform-ai-agent:platform-ai-skill:platform-ai-skill-api"))

    implementation(project(":server:ee:libs:core:remote:remote-client"))
}
