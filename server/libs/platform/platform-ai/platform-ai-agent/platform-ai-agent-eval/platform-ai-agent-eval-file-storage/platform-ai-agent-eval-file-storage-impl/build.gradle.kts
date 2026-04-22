dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:platform:platform-ai:platform-ai-agent:platform-ai-agent-eval:platform-ai-agent-eval-file-storage:platform-ai-agent-eval-file-storage-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
}
