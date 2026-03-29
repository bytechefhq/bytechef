dependencies {
    implementation("org.springframework.boot:spring-boot-autoconfigure")
    implementation(project(":server:libs:ai:ai-agent:ai-agent-eval:ai-agent-eval-file-storage:ai-agent-eval-file-storage-api"))
    implementation(project(":server:libs:config:app-config"))
    implementation(project(":server:libs:core:file-storage:file-storage-api"))
}
