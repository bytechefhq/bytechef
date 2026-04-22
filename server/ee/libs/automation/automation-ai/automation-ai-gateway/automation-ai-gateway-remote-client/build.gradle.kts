dependencies {
    implementation("org.springframework:spring-context")
    implementation(project(":server:ee:libs:automation:automation-ai:automation-ai-gateway:automation-ai-gateway-api"))
    implementation(project(":server:ee:libs:core:remote:remote-client"))
    implementation(project(":server:libs:platform:platform-api"))
}
