dependencies {
    implementation(project(":server:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-eval:platform-ai-agent-eval-api"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-eval:platform-ai-agent-eval-file-storage:platform-ai-agent-eval-file-storage-api"))

    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation("org.springframework.graphql:spring-graphql")
}
