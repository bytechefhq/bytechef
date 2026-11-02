dependencies {
    api(project(":server:ee:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-remote-grpc-proto"))
    api(project(":server:libs:platform:platform-ai:platform-ai-skill:platform-ai-skill-api"))
    api(project(":server:libs:platform:platform-tag:platform-tag-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework:spring-context")
    implementation("tools.jackson.core:jackson-databind")
    implementation(project(":server:ee:libs:core:remote:remote-grpc"))
}
