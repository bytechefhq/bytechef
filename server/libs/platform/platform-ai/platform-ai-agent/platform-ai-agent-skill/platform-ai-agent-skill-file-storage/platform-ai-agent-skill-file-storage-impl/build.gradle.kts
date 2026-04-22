dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-skill:platform-ai-agent-skill-file-storage:platform-ai-agent-skill-file-storage-api"))
}
